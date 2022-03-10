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

import static io.ballerina.compiler.syntax.tree.SyntaxKind.INTERSECTION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_PARAMETERS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_WATCHEVENT_PARAMETER;
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
public class FtpFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final ServiceDeclarationNode serviceDeclarationNode;
    FunctionDefinitionNode onFileChange;

    public FtpFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onFileChange) {
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
            validateFunctionArguments(parameters, onFileChange);
            validateReturnTypeErrorOrNil(onFileChange);
        }
    }

    private void validateFunctionArguments(SeparatedNodeList<ParameterNode> parameters,
                                           FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 1) {
            validateSingleArgumentScenario(parameters);
        } else if (parameters.size() == 2) {
            validateTwoArgumentScenario(parameters, functionDefinitionNode);
        } else if (parameters.size() > 2) {
            validateInvalidArgumentCountScenario(functionDefinitionNode, ONLY_PARAMS_ALLOWED);
        } else {
            validateInvalidArgumentCountScenario(functionDefinitionNode, MUST_HAVE_WATCHEVENT);
        }
    }

    private void validateInvalidArgumentCountScenario(FunctionDefinitionNode functionDefinitionNode,
                                                      PluginConstants.CompilationErrors compilationErrors) {
        context.reportDiagnostic(PluginUtils.getDiagnostic(compilationErrors,
                DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
    }

    private void validateTwoArgumentScenario(SeparatedNodeList<ParameterNode> parameters,
                                             FunctionDefinitionNode functionDefinitionNode) {
        ParameterNode firstParamNode = parameters.get(0);
        ParameterNode secondParamNode = parameters.get(1);
        SyntaxKind firstParamSyntaxKind = ((RequiredParameterNode) firstParamNode).typeName().kind();
        SyntaxKind secondParamSyntaxKind = ((RequiredParameterNode) secondParamNode).typeName().kind();

        if (firstParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE) &&
                secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateCallerAndWachEvent(firstParamNode, secondParamNode);
        } else if (firstParamSyntaxKind.equals(INTERSECTION_TYPE_DESC) &&
                secondParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateInvalidReaonlyArguments(firstParamNode, secondParamNode);
        } else if (firstParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateReadonlyWatchEventAndCaller(firstParamNode, secondParamNode, secondParamSyntaxKind);
        } else if (secondParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateCallerAndReadonlyWatchEvent(firstParamNode, secondParamNode, firstParamSyntaxKind);
        } else if (firstParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateInvalidArguments(firstParamNode, secondParamNode);
        } else if (secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateInvalidArguments(secondParamNode, firstParamNode);
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETERS,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.functionSignature().location()));
        }
    }

    private void validateSingleArgumentScenario(SeparatedNodeList<ParameterNode> parameters) {
        ParameterNode paramNode = parameters.get(0);
        SyntaxKind paramSyntaxKind = ((RequiredParameterNode) paramNode).typeName().kind();
        if (paramSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateWatchEventParam(paramNode);
        } else if (paramSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            boolean watchEventParam = validateIntersectionParam(paramNode);
            if (!watchEventParam) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        INVALID_WATCHEVENT_PARAMETER,
                        DiagnosticSeverity.ERROR, paramNode.location()));
            }
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    INVALID_WATCHEVENT_PARAMETER,
                    DiagnosticSeverity.ERROR, paramNode.location()));
        }
    }

    private void validateInvalidArguments(ParameterNode firstParamNode, ParameterNode secondParamNode) {
        boolean firstParamCaller = validateCallerParam(firstParamNode);
        if (!firstParamCaller) {
            validateWatchEventParam(firstParamNode);
        }
        context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETERS,
                DiagnosticSeverity.ERROR, secondParamNode.location()));
    }

    private void validateCallerAndReadonlyWatchEvent(ParameterNode firstParamNode, ParameterNode secondParamNode,
                                                     SyntaxKind firstParamSyntaxKind) {
        boolean firstParamCaller = validateCallerParam(firstParamNode);
        boolean secondParamWatchEvent = validateIntersectionParam(secondParamNode);
        if (!firstParamCaller && !secondParamWatchEvent) {
            if (firstParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
                validateWatchEventParam(firstParamNode);
            }
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETERS,
                    DiagnosticSeverity.ERROR, secondParamNode.location()));
        } else if (!secondParamWatchEvent) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_WATCHEVENT_PARAMETER,
                    DiagnosticSeverity.ERROR, firstParamNode.location()));
        } else if (!firstParamCaller) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_CALLER_PARAMETER,
                    DiagnosticSeverity.ERROR, secondParamNode.location()));
        }
    }

    private void validateReadonlyWatchEventAndCaller(ParameterNode firstParamNode, ParameterNode secondParamNode,
                                                     SyntaxKind secondParamSyntaxKind) {
        boolean firstParamWatchEvent = validateIntersectionParam(firstParamNode);
        boolean secondParamCaller = validateCallerParam(secondParamNode);
        if (!firstParamWatchEvent && !secondParamCaller) {
            if (secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
                validateWatchEventParam(secondParamNode);
            }
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_PARAMETERS,
                    DiagnosticSeverity.ERROR, firstParamNode.location()));
        } else if (!firstParamWatchEvent) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_WATCHEVENT_PARAMETER,
                    DiagnosticSeverity.ERROR, firstParamNode.location()));
        } else if (!secondParamCaller) {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_CALLER_PARAMETER,
                    DiagnosticSeverity.ERROR, secondParamNode.location()));
        }
    }

    private void validateInvalidReaonlyArguments(ParameterNode firstParamNode, ParameterNode secondParamNode) {
        boolean firstParamWatchEvent = validateIntersectionParam(firstParamNode);
        if (!firstParamWatchEvent) {
            boolean secondParamWatchEvent = validateIntersectionParam(secondParamNode);
            if (!secondParamWatchEvent) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_WATCHEVENT_PARAMETER,
                        DiagnosticSeverity.ERROR, secondParamNode.location()));
            }
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_CALLER_PARAMETER,
                    DiagnosticSeverity.ERROR, firstParamNode.location()));
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_CALLER_PARAMETER,
                    DiagnosticSeverity.ERROR, secondParamNode.location()));
        }
    }

    private void validateCallerAndWachEvent(ParameterNode firstParamNode, ParameterNode secondParamNode) {
        boolean firstParamCaller = validateCallerParam(firstParamNode);
        if (firstParamCaller) {
            validateWatchEventParam(secondParamNode);
        } else {
            validateWatchEventParam(firstParamNode);
            boolean secondParamCaller = validateCallerParam(secondParamNode);
            if (!secondParamCaller) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_CALLER_PARAMETER,
                        DiagnosticSeverity.ERROR, secondParamNode.location()));
            }
        }
    }

    private boolean validateIntersectionParam(ParameterNode parameterNode) {
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
                if (watchEventExists) {
                    return true;
                }
            }
        }
        return false;
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
                            INVALID_WATCHEVENT_PARAMETER,
                            DiagnosticSeverity.ERROR, requiredParameterNode.location()));
                }
            } else {
                context.reportDiagnostic(PluginUtils.getDiagnostic(
                        INVALID_WATCHEVENT_PARAMETER,
                        DiagnosticSeverity.ERROR, requiredParameterNode.location()));
            }
        } else {
            context.reportDiagnostic(PluginUtils.getDiagnostic(
                    INVALID_WATCHEVENT_PARAMETER,
                    DiagnosticSeverity.ERROR, requiredParameterNode.location()));
        }
    }

    private boolean validateCallerParam(ParameterNode parameterNode) {
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
                        !paramName.equals(PluginConstants.CALLER)) {
                    return false;
                }
                return true;
            }
        }
        return false;
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
