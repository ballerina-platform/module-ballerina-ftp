/*
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CALLER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ERROR_PARAM;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_ERROR_FIRST_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_ERROR_SECOND_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_ERROR_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_ERROR;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.validateModuleId;

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
            if (!validateCallerParam((RequiredParameterNode) secondParamNode)) {
                context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_SECOND_PARAMETER,
                        DiagnosticSeverity.ERROR, secondParamNode.location()));
            }
        }

        // Validate return type - must be error?
        PluginUtils.validateReturnTypeErrorOrNil(functionDefinitionNode, context);
    }

    private void validateErrorParameter(ParameterNode parameterNode) {
        SyntaxKind paramSyntaxKind = ((RequiredParameterNode) parameterNode).typeName().kind();
        SemanticModel semanticModel = context.semanticModel();
        if (paramSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            Node parameterTypeNode = ((RequiredParameterNode) parameterNode).typeName();
            Optional<Symbol> paramSymbol = semanticModel.symbol(parameterTypeNode);
            if (paramSymbol.isEmpty() || paramSymbol.get().getName().isEmpty() ||
                    !paramSymbol.get().getName().get().equals(ERROR_PARAM) ||
                    paramSymbol.get().getModule().isEmpty() ||
                    !validateModuleId(paramSymbol.get().getModule().get())) {
                context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_FIRST_PARAMETER,
                        DiagnosticSeverity.ERROR, parameterNode.location()));
            }
        } else if (!paramSyntaxKind.equals(ERROR_TYPE_DESC)) {
            context.reportDiagnostic(getDiagnostic(INVALID_ON_ERROR_FIRST_PARAMETER,
                    DiagnosticSeverity.ERROR, parameterNode.location()));
        }
    }

    private boolean validateCallerParam(RequiredParameterNode requiredParameterNode) {
        Node parameterTypeNode = requiredParameterNode.typeName();
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> paramSymbol = semanticModel.symbol(parameterTypeNode);
        if (paramSymbol.isPresent()) {
            Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
            if (moduleSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().isPresent() ? paramSymbol.get().getName().get() : "";
                if (validateModuleId(moduleSymbol.get()) && paramName.equals(CALLER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
