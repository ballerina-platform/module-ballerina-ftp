/*
 * Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.compiler.api.symbols.TypeDescKind.ARRAY;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CALLER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETED;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getMethodSymbol;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.validateModuleId;

/**
 * Validator for onFileDeleted function.
 */
public class FtpFileDeletedValidator {

    private final SyntaxNodeAnalysisContext syntaxNodeAnalysisContext;
    private final FunctionDefinitionNode onFileDeletedFunctionDefinitionNode;

    public FtpFileDeletedValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                    FunctionDefinitionNode onFileDeletedFunctionDefinitionNode) {
        this.syntaxNodeAnalysisContext = syntaxNodeAnalysisContext;
        this.onFileDeletedFunctionDefinitionNode = onFileDeletedFunctionDefinitionNode;
    }

    public void validate() {
        if (Objects.isNull(onFileDeletedFunctionDefinitionNode)) {
            return;
        }

        if (!isRemoteFunction(syntaxNodeAnalysisContext, onFileDeletedFunctionDefinitionNode)) {
            reportErrorDiagnostic(ON_FILE_DELETED_MUST_BE_REMOTE, onFileDeletedFunctionDefinitionNode.location());
        }

        SeparatedNodeList<ParameterNode> parameters =
                onFileDeletedFunctionDefinitionNode.functionSignature().parameters();
        validateOnFileDeletedParameters(parameters, onFileDeletedFunctionDefinitionNode);
        validateReturnTypeErrorOrNil(onFileDeletedFunctionDefinitionNode);
    }

    private void validateOnFileDeletedParameters(SeparatedNodeList<ParameterNode> parameters,
                                                  FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.isEmpty()) {
            reportErrorDiagnostic(INVALID_ON_FILE_DELETED_PARAMETER, functionDefinitionNode.location());
            return;
        }

        if (parameters.size() > 2) {
            reportErrorDiagnostic(TOO_MANY_PARAMETERS_ON_FILE_DELETED, functionDefinitionNode.location());
            return;
        }

        // First parameter must be string[]
        ParameterNode firstParameter = parameters.get(0);
        if (!validateStringArrayParameter(firstParameter)) {
            reportErrorDiagnostic(INVALID_ON_FILE_DELETED_PARAMETER, firstParameter.location());
        }

        // Second parameter (if exists) must be Caller
        if (parameters.size() == 2) {
            ParameterNode secondParameter = parameters.get(1);
            if (!validateCallerParameter(secondParameter)) {
                reportErrorDiagnostic(INVALID_ON_FILE_DELETED_CALLER_PARAMETER, secondParameter.location());
            }
        }
    }

    private boolean validateStringArrayParameter(ParameterNode parameterNode) {
        if (!(parameterNode instanceof RequiredParameterNode)) {
            return false;
        }

        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = syntaxNodeAnalysisContext.semanticModel();
        Optional<Symbol> paramSymbolOpt = semanticModel.symbol(requiredParameterNode);

        if (paramSymbolOpt.isEmpty()) {
            return false;
        }

        Symbol symbol = paramSymbolOpt.get();
        if (!(symbol instanceof ParameterSymbol)) {
            return false;
        }

        ParameterSymbol parameterSymbol = (ParameterSymbol) symbol;
        TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
        if (typeSymbol == null) {
            return false;
        }

        // Check if it's string[]
        return typeSymbol.typeKind() == ARRAY && typeSymbol.signature().equals("string[]");
    }

    private boolean validateCallerParameter(ParameterNode parameterNode) {
        if (!(parameterNode instanceof RequiredParameterNode)) {
            return false;
        }

        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        if (requiredParameterNode.typeName().kind() != QUALIFIED_NAME_REFERENCE) {
            return false;
        }

        Node parameterTypeNode = requiredParameterNode.typeName();
        SemanticModel semanticModel = syntaxNodeAnalysisContext.semanticModel();
        Optional<Symbol> paramSymbol = semanticModel.symbol(parameterTypeNode);

        if (paramSymbol.isPresent()) {
            Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
            if (moduleSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().orElse("");
                return validateModuleId(moduleSymbol.get()) && paramName.equals(CALLER);
            }
        }
        return false;
    }

    private void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(syntaxNodeAnalysisContext, functionDefinitionNode);
        if (methodSymbol == null) {
            return;
        }

        Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
        if (returnTypeDesc.isEmpty()) {
            return;
        }

        TypeDescKind returnTypeKind = returnTypeDesc.get().typeKind();

        if (returnTypeKind == TypeDescKind.NIL) {
            return;
        }

        if (returnTypeKind == TypeDescKind.UNION) {
            List<TypeSymbol> returnTypeMembers =
                    ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
            for (TypeSymbol returnType : returnTypeMembers) {
                if (returnType.typeKind() != TypeDescKind.NIL) {
                    if (returnType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                        if (!returnType.signature().equals(PluginConstants.ERROR) &&
                                returnType.getModule().isPresent() &&
                                !validateModuleId(returnType.getModule().get())) {
                            reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                    functionDefinitionNode.functionSignature().location());
                            return;
                        }
                    } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                        reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                functionDefinitionNode.functionSignature().location());
                        return;
                    }
                }
            }
        } else {
            reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                    functionDefinitionNode.functionSignature().location());
        }
    }

    public void reportErrorDiagnostic(PluginConstants.CompilationErrors error, Location location) {
        syntaxNodeAnalysisContext.reportDiagnostic(getDiagnostic(error,
                DiagnosticSeverity.ERROR, location));
    }
}
