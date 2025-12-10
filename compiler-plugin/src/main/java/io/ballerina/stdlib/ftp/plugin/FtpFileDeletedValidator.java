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
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETE_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETE_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MANDATORY_PARAMETER_NOT_FOUND;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_DEPRECATED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETE_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETED;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getParameterTypeSymbol;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.reportErrorDiagnostic;

/**
 * Combined validator for onFileDelete and onFileDeleted functions.
 */
public class FtpFileDeletedValidator {

    private final SyntaxNodeAnalysisContext context;
    private final FunctionDefinitionNode functionDefinitionNode;
    private final DeletionMethodType methodType;

    public enum DeletionMethodType {
        ON_FILE_DELETE,      // New method: onFileDelete(string deletedFile)
        ON_FILE_DELETED      // Deprecated method: onFileDeleted(string[] deletedFiles)
    }

    public FtpFileDeletedValidator(SyntaxNodeAnalysisContext context,
                                   FunctionDefinitionNode functionDefinitionNode,
                                   DeletionMethodType methodType) {
        this.context = context;
        this.functionDefinitionNode = functionDefinitionNode;
        this.methodType = methodType;
    }

    public void validate() {
        // Report deprecation warning for onFileDeleted
        if (methodType == DeletionMethodType.ON_FILE_DELETED) {
            context.reportDiagnostic(getDiagnostic(ON_FILE_DELETED_DEPRECATED, DiagnosticSeverity.WARNING,
                    functionDefinitionNode.location()));
        }

        // Check if remote
        if (!isRemoteFunction(context, functionDefinitionNode)) {
            PluginConstants.CompilationErrors remoteError = methodType == DeletionMethodType.ON_FILE_DELETE
                    ? ON_FILE_DELETE_MUST_BE_REMOTE
                    : ON_FILE_DELETED_MUST_BE_REMOTE;
            reportErrorDiagnostic(context, remoteError, functionDefinitionNode.location());
        }

        SeparatedNodeList<ParameterNode> parameters =
                functionDefinitionNode.functionSignature().parameters();
        validateParameters(parameters, functionDefinitionNode);
        PluginUtils.validateReturnTypeErrorOrNil(functionDefinitionNode, context);
    }

    private void validateParameters(SeparatedNodeList<ParameterNode> parameters,
                                    FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.isEmpty()) {
            String expectedType = methodType == DeletionMethodType.ON_FILE_DELETE ? "string" : "string[]";
            String funcName = methodType == DeletionMethodType.ON_FILE_DELETE
                    ? PluginConstants.ON_FILE_DELETE_FUNC
                    : PluginConstants.ON_FILE_DELETED_FUNC;
            reportErrorDiagnostic(context, MANDATORY_PARAMETER_NOT_FOUND, functionDefinitionNode.location(),
                    funcName, expectedType);
            return;
        }

        if (parameters.size() > 2) {
            PluginConstants.CompilationErrors tooManyParamsError = methodType == DeletionMethodType.ON_FILE_DELETE
                    ? TOO_MANY_PARAMETERS_ON_FILE_DELETE
                    : TOO_MANY_PARAMETERS_ON_FILE_DELETED;
            reportErrorDiagnostic(context, tooManyParamsError, functionDefinitionNode.location());
            return;
        }

        // Validate first parameter
        ParameterNode firstParameter = parameters.get(0);
        boolean isValidFirstParam = methodType == DeletionMethodType.ON_FILE_DELETE
                ? validateStringParameter(firstParameter)
                : validateStringArrayParameter(firstParameter);

        if (!isValidFirstParam) {
            PluginConstants.CompilationErrors paramError = methodType == DeletionMethodType.ON_FILE_DELETE
                    ? INVALID_ON_FILE_DELETE_PARAMETER
                    : INVALID_ON_FILE_DELETED_PARAMETER;
            reportErrorDiagnostic(context, paramError, firstParameter.location());
        }

        // Validate second parameter (if exists) must be Caller
        if (parameters.size() == 2) {
            ParameterNode secondParameter = parameters.get(1);
            if (!PluginUtils.validateCallerParameter(secondParameter, context)) {
                PluginConstants.CompilationErrors callerError = methodType == DeletionMethodType.ON_FILE_DELETE
                        ? INVALID_ON_FILE_DELETE_CALLER_PARAMETER
                        : INVALID_ON_FILE_DELETED_CALLER_PARAMETER;
                reportErrorDiagnostic(context, callerError, secondParameter.location());
            }
        }
    }

    private boolean validateStringParameter(ParameterNode parameterNode) {
        return getParameterTypeSymbol(parameterNode, context)
                .map(typeSymbol -> typeSymbol.typeKind() == TypeDescKind.STRING)
                .orElse(false);
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
