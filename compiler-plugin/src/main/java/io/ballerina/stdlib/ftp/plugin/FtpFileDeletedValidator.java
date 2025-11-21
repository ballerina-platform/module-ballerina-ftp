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

import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MANDATORY_PARAMETER_NOT_FOUND;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETED;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getParameterTypeSymbol;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.reportErrorDiagnostic;

/**
 * Validator for onFileDeleted function.
 */
public class FtpFileDeletedValidator {

    private final SyntaxNodeAnalysisContext context;
    private final FunctionDefinitionNode functionDefinitionNode;

    public FtpFileDeletedValidator(SyntaxNodeAnalysisContext context,
                                    FunctionDefinitionNode functionDefinitionNode) {
        this.context = context;
        this.functionDefinitionNode = functionDefinitionNode;
    }

    public void validate() {
        if (!isRemoteFunction(context, functionDefinitionNode)) {
            reportErrorDiagnostic(context, ON_FILE_DELETED_MUST_BE_REMOTE, functionDefinitionNode.location());
        }

        SeparatedNodeList<ParameterNode> parameters =
                functionDefinitionNode.functionSignature().parameters();
        validateOnFileDeletedParameters(parameters, functionDefinitionNode);
        PluginUtils.validateReturnTypeErrorOrNil(functionDefinitionNode, context);
    }

    private void validateOnFileDeletedParameters(SeparatedNodeList<ParameterNode> parameters,
                                                 FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.isEmpty()) {
            reportErrorDiagnostic(context, MANDATORY_PARAMETER_NOT_FOUND, functionDefinitionNode.location(),
                    PluginConstants.ON_FILE_DELETED_FUNC, "string[]");
            return;
        }

        if (parameters.size() > 2) {
            reportErrorDiagnostic(context, TOO_MANY_PARAMETERS_ON_FILE_DELETED, functionDefinitionNode.location());
            return;
        }

        // First parameter must be string[]
        ParameterNode firstParameter = parameters.get(0);
        if (!validateStringArrayParameter(firstParameter)) {
            reportErrorDiagnostic(context, INVALID_ON_FILE_DELETED_PARAMETER, firstParameter.location());
        }

        // Second parameter (if exists) must be Caller
        if (parameters.size() == 2) {
            ParameterNode secondParameter = parameters.get(1);
            if (!PluginUtils.validateCallerParameter(secondParameter, context)) {
                reportErrorDiagnostic(context, INVALID_ON_FILE_DELETED_CALLER_PARAMETER, secondParameter.location());
            }
        }
    }

    private boolean validateStringArrayParameter(ParameterNode parameterNode) {
        return getParameterTypeSymbol(parameterNode, context)
                .map(typeSymbol -> {
                    if (!(typeSymbol instanceof ArrayTypeSymbol arrayType)) {
                        return false;
                    }
                    return arrayType.memberTypeDescriptor().typeKind() == TypeDescKind.STRING;
                })
                .orElse(false);
    }

}
