/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.PACKAGE_ORG;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.PACKAGE_PREFIX;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ERROR_PARAM;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_ERROR_FIRST_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_ERROR_SECOND_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_ERROR_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_ERROR;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;

/**
 * Validates the onError remote function in FTP service.
 */
public class FtpOnErrorValidator {

    private final SyntaxNodeAnalysisContext context;
    private final FunctionDefinitionNode functionDefinitionNode;

    public FtpOnErrorValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode functionDefinitionNode) {
        this.context = context;
        this.functionDefinitionNode = functionDefinitionNode;
    }

    public void validate() {
        // Check if remote
        if (!isRemoteFunction(context, functionDefinitionNode)) {
            context.reportDiagnostic(getDiagnostic(ON_ERROR_MUST_BE_REMOTE,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            return;
        }

        SeparatedNodeList<ParameterNode> parameters = functionDefinitionNode.functionSignature().parameters();
        int paramCount = parameters.size();

        if (paramCount == 0) {
            context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_FIRST_PARAMETER,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            return;
        }

        // Validate parameter count (max 2: error, caller?)
        if (paramCount > 2) {
            context.reportDiagnostic(getDiagnostic(TOO_MANY_PARAMETERS_ON_ERROR,
                    DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
            return;
        }

        // Validate first parameter - must be ftp:Error or error
        ParameterNode firstParamNode = parameters.get(0);
        validateErrorParameter(firstParamNode);

        // Validate 2nd parameter if present - must be Caller
        if (paramCount == 2) {
            ParameterNode secondParamNode = parameters.get(1);
            if (!PluginUtils.validateCallerParameter(secondParamNode, context)) {
                context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_SECOND_PARAMETER,
                        DiagnosticSeverity.ERROR, secondParamNode.location()));
            }
        }

        // Validate return type - must be error?
        PluginUtils.validateReturnTypeErrorOrNil(functionDefinitionNode, context);
    }

    private void validateErrorParameter(ParameterNode parameterNode) {
        Optional<TypeSymbol> paramType = PluginUtils.getParameterTypeSymbol(parameterNode, context);
        if (paramType.isEmpty()) {
            context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_FIRST_PARAMETER,
                    DiagnosticSeverity.ERROR, parameterNode.location()));
            return;
        }

        SemanticModel semanticModel = context.semanticModel();
        TypeSymbol normalizedParamType = unwrapTypeReference(paramType.get());
        boolean isError = isSameType(normalizedParamType, semanticModel.types().ERROR);
        boolean isFtpErrorSubtype = findFtpErrorTypeSymbol(semanticModel, "")
                .map(this::unwrapTypeReference)
                .map(normalizedParamType::subtypeOf)
                .orElse(false);
        if (!isError && !isFtpErrorSubtype) {
            context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_FIRST_PARAMETER,
                    DiagnosticSeverity.ERROR, parameterNode.location()));
        }
    }

    private Optional<TypeSymbol> findFtpErrorTypeSymbol(SemanticModel semanticModel, String version) {
        Optional<Symbol> ftpErrorSymbol = semanticModel.types()
                .getTypeByName(PACKAGE_ORG, PACKAGE_PREFIX, version, ERROR_PARAM);
        if (ftpErrorSymbol.isEmpty()) {
            return Optional.empty();
        }
        Symbol symbol = ftpErrorSymbol.get();
        if (symbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
            return Optional.of(typeDefinitionSymbol.typeDescriptor());
        }
        if (symbol instanceof TypeSymbol typeSymbol) {
            return Optional.of(typeSymbol);
        }
        return Optional.empty();
    }

    private boolean isSameType(TypeSymbol left, TypeSymbol right) {
        return left.subtypeOf(right) && right.subtypeOf(left);
    }

    private TypeSymbol unwrapTypeReference(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.TYPE_REFERENCE &&
                typeSymbol instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol) {
            return typeReferenceTypeSymbol.typeDescriptor();
        }
        return typeSymbol;
    }

}
