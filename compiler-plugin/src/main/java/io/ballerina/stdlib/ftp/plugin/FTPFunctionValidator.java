/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MUST_HAVE_WATCHEVENT;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.NO_ON_FILE_CHANGE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getMethodSymbol;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.validateModuleId;

/**
 * FTP function validator.
 */
public class FTPFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final ServiceDeclarationNode serviceDeclarationNode;
    FunctionDefinitionNode onFileChange;

    public FTPFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onFileChange) {
        this.context = context;
        this.serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        this.onFileChange = onFileChange;
    }

    public void validate() {
        if (Objects.isNull(onFileChange)) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(NO_ON_FILE_CHANGE,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
        } else {
            if (!isRemoteFunction(context, onFileChange)) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(METHOD_MUST_BE_REMOTE,
                        DiagnosticSeverity.ERROR, onFileChange.location()));
            }
            SeparatedNodeList<ParameterNode> parameters = onFileChange.functionSignature().parameters();
            validateFunctionParameters(parameters, onFileChange);
            validateReturnTypeErrorOrNil(onFileChange);
        }
    }

    private void validateFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                            FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 1) {
            ParameterNode paramNode = parameters.get(0);
            SyntaxKind paramSyntaxKind = ((RequiredParameterNode) paramNode).typeName().kind();
            if (paramSyntaxKind.equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
                validateWatchEventParam(paramNode);
            } else if (paramSyntaxKind.equals(SyntaxKind.INTERSECTION_TYPE_DESC)) {
                validateIntersectionParam(paramNode);
            } else {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        INVALID_PARAMETER,
                        DiagnosticSeverity.ERROR, paramNode.location()));
            }
        } else if (parameters.size() > 1) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(ONLY_PARAMS_ALLOWED,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(MUST_HAVE_WATCHEVENT,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateIntersectionParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isPresent()) {
            ParameterSymbol parameterSymbol = (ParameterSymbol) symbol.get();
            if (parameterSymbol.typeDescriptor() instanceof IntersectionTypeSymbol) {
                IntersectionTypeSymbol intersectionTypeSymbol =
                        (IntersectionTypeSymbol) parameterSymbol.typeDescriptor();
                boolean watchEventExists = intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .anyMatch(typeSymbol -> typeSymbol.nameEquals(PluginConstants.WATCHEVENT) &&
                                typeSymbol.getModule().isPresent() &&
                                validateModuleId(typeSymbol.getModule().get()));
                if (!watchEventExists) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETER,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else {
                context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETER,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETER,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        }
    }

    private void validateWatchEventParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> paramSymbol = semanticModel.symbol(parameterTypeNode);
        if (paramSymbol.isPresent()) {
            Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
            if (moduleSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().isPresent() ?
                        paramSymbol.get().getName().get() : "";
                if (!validateModuleId(moduleSymbol.get()) ||
                        !paramName.equals(PluginConstants.WATCHEVENT)) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            INVALID_PARAMETER,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        INVALID_PARAMETER,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    INVALID_PARAMETER,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        }
    }

    private void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol != null) {
            Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDesc.isPresent()) {
                if (returnTypeDesc.get().typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> returnTypeMembers =
                            ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
                    for (TypeSymbol returnType : returnTypeMembers) {
                        if (returnType.typeKind() != TypeDescKind.NIL) {
                            if (returnType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                                if (!returnType.signature().equals(PluginConstants.ERROR) &&
                                        !validateModuleId(returnType.getModule().get())) {
                                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                                            PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                            DiagnosticSeverity.ERROR,
                                            functionDefinitionNode.functionSignature().location()));
                                }
                            } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                                context.reportDiagnostic(PluginUtils.getDiagnostic(
                                    PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    context.reportDiagnostic(PluginUtils.getDiagnostic(
                            PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL,
                            DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
                }
            }
        }
    }
}
