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

import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.BOTH_ON_FILE_DELETE_METHODS_NOT_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MULTIPLE_CONTENT_METHODS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.NO_VALID_REMOTE_METHOD;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_CHANGE_DEPRECATED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CHANGE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CSV_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_DELETE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_DELETED_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_JSON_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_TEXT_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_XML_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;

/**
 * FTP service compilation validator.
 */
public class FtpServiceValidator {

    public void validate(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        NodeList<Node> memberNodes = serviceDeclarationNode.members();

        boolean hasRemoteFunction = serviceDeclarationNode.members().stream().anyMatch(child ->
                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION &&
                        isRemoteFunction(context, (FunctionDefinitionNode) child));

        if (serviceDeclarationNode.members().isEmpty() || !hasRemoteFunction) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getErrorCode(),
                    PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getError(),
                    DiagnosticSeverity.INTERNAL);
            context.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    serviceDeclarationNode.location()));
        }

        FunctionDefinitionNode onFileChange = null;
        FunctionDefinitionNode onFileDelete = null;
        FunctionDefinitionNode onFileDeleted = null;
        List<FunctionDefinitionNode> contentMethods = new ArrayList<>();
        List<String> contentMethodNames = new ArrayList<>();

        for (Node node : memberNodes) {
            if (node.kind() == RESOURCE_ACCESSOR_DEFINITION) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(RESOURCE_FUNCTION_NOT_ALLOWED,
                        DiagnosticSeverity.ERROR, node.location()));
                continue;
            }

            if (node.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
                continue;
            }

            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            MethodSymbol methodSymbol = PluginUtils.getMethodSymbol(context, functionDefinitionNode);
            Optional<String> functionName = methodSymbol.getName();
            if (functionName.isEmpty()) {
                continue;
            }

            String funcName = functionName.get();
            switch (funcName) {
                case ON_FILE_CHANGE_FUNC:
                    onFileChange = functionDefinitionNode;
                    break;
                case ON_FILE_DELETE_FUNC:
                    onFileDelete = functionDefinitionNode;
                    break;
                case ON_FILE_DELETED_FUNC:
                    onFileDeleted = functionDefinitionNode;
                    break;
                case ON_FILE_FUNC:
                case ON_FILE_TEXT_FUNC:
                case ON_FILE_JSON_FUNC:
                case ON_FILE_XML_FUNC:
                case ON_FILE_CSV_FUNC:
                    contentMethods.add(functionDefinitionNode);
                    contentMethodNames.add(funcName);
                    break;
                default:
                    // Invalid remote function name
                    if (isRemoteFunction(context, functionDefinitionNode)) {
                        context.reportDiagnostic(getDiagnostic(INVALID_REMOTE_FUNCTION,
                                DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                    }
                    break;
            }
        }

        // Check if both onFileDelete and onFileDeleted are present
        if (onFileDelete != null && onFileDeleted != null) {
            context.reportDiagnostic(getDiagnostic(BOTH_ON_FILE_DELETE_METHODS_NOT_ALLOWED,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
            return;
        }

        // Validate method exclusivity
        if (onFileChange != null) {
            if (!contentMethods.isEmpty() || onFileDelete != null || onFileDeleted != null) {
                context.reportDiagnostic(getDiagnostic(MULTIPLE_CONTENT_METHODS,
                        DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
                return;
            }
            context.reportDiagnostic(getDiagnostic(ON_FILE_CHANGE_DEPRECATED, DiagnosticSeverity.WARNING,
                    onFileChange.location()));
            new FtpFunctionValidator(context, onFileChange).validate();
            return;
        }

        if (contentMethods.isEmpty() && onFileDelete == null && onFileDeleted == null) {
            context.reportDiagnostic(getDiagnostic(NO_VALID_REMOTE_METHOD, DiagnosticSeverity.ERROR,
                    serviceDeclarationNode.location()));
            return;
        }

        // Validate format-specific handlers
        if (!contentMethods.isEmpty()) {
            for (int i = 0; i < contentMethods.size(); i++) {
                new FtpContentFunctionValidator(context, contentMethods.get(i),
                        contentMethodNames.get(i)).validate();
            }
        }

        // Validate deletion handlers
        if (onFileDelete != null) {
            new FtpFileDeletedValidator(context, onFileDelete,
                    FtpFileDeletedValidator.DeletionMethodType.ON_FILE_DELETE).validate();
        }
        if (onFileDeleted != null) {
            new FtpFileDeletedValidator(context, onFileDeleted,
                    FtpFileDeletedValidator.DeletionMethodType.ON_FILE_DELETED).validate();
        }
    }
}
