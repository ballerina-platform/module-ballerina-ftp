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

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MIXED_GENERIC_AND_FORMAT_SPECIFIC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MULTIPLE_CONTENT_METHODS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MULTIPLE_GENERIC_CONTENT_METHODS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.NO_ON_FILE_CHANGE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CHANGE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CSV_FUNC;
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
        List<FunctionDefinitionNode> contentMethods = new ArrayList<>();
        List<String> contentMethodNames = new ArrayList<>();

        for (Node node : memberNodes) {
            if (node.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                MethodSymbol methodSymbol = PluginUtils.getMethodSymbol(context, functionDefinitionNode);
                Optional<String> functionName = methodSymbol.getName();
                if (functionName.isPresent()) {
                    String funcName = functionName.get();
                    if (funcName.equals(ON_FILE_CHANGE_FUNC)) {
                        onFileChange = functionDefinitionNode;
                    } else if (isContentMethod(funcName)) {
                        contentMethods.add(functionDefinitionNode);
                        contentMethodNames.add(funcName);
                    } else if (isRemoteFunction(context, functionDefinitionNode)) {
                        context.reportDiagnostic(getDiagnostic(INVALID_REMOTE_FUNCTION,
                                DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                    }
                }
            } else if (node.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(RESOURCE_FUNCTION_NOT_ALLOWED,
                        DiagnosticSeverity.ERROR, node.location()));
            }
        }

        // Validate method exclusivity rules
        validateMethodExclusivity(context, serviceDeclarationNode, onFileChange, contentMethods, contentMethodNames);

        // Validate parameters based on which method type is present
        if (onFileChange != null && contentMethods.isEmpty()) {
            // Traditional onFileChange validation
            new FtpFunctionValidator(context, onFileChange).validate();
        } else if (onFileChange == null && !contentMethods.isEmpty()) {
            // Content method validation
            for (int i = 0; i < contentMethods.size(); i++) {
                new FtpContentFunctionValidator(context, contentMethods.get(i),
                        contentMethodNames.get(i)).validate();
            }
        } else if (onFileChange == null && contentMethods.isEmpty()) {
            // No valid method found - maintain backward compatibility by reporting NO_ON_FILE_CHANGE
            context.reportDiagnostic(getDiagnostic(NO_ON_FILE_CHANGE,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
        }
    }

    private boolean isContentMethod(String methodName) {
        return methodName.equals(ON_FILE_FUNC) ||
                methodName.equals(ON_FILE_TEXT_FUNC) ||
                methodName.equals(ON_FILE_JSON_FUNC) ||
                methodName.equals(ON_FILE_XML_FUNC) ||
                methodName.equals(ON_FILE_CSV_FUNC);
    }

    private void validateMethodExclusivity(SyntaxNodeAnalysisContext context,
                                           ServiceDeclarationNode serviceDeclarationNode,
                                           FunctionDefinitionNode onFileChange,
                                           List<FunctionDefinitionNode> contentMethods,
                                           List<String> contentMethodNames) {
        // Rule 1: Cannot mix onFileChange with content methods
        if (onFileChange != null && !contentMethods.isEmpty()) {
            context.reportDiagnostic(getDiagnostic(MULTIPLE_CONTENT_METHODS,
                    DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
            return;
        }

        // Rule 2: If using content methods, validate strategy
        if (!contentMethods.isEmpty()) {
            boolean hasGenericOnFile = contentMethodNames.contains(ON_FILE_FUNC);
            boolean hasFormatSpecific = contentMethodNames.stream()
                    .anyMatch(name -> !name.equals(ON_FILE_FUNC));

            // Cannot mix generic onFile with format-specific methods
            if (hasGenericOnFile && hasFormatSpecific) {
                context.reportDiagnostic(getDiagnostic(MIXED_GENERIC_AND_FORMAT_SPECIFIC,
                        DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
                return;
            }

            // Cannot have multiple generic onFile methods
            long onFileCount = contentMethodNames.stream().filter(name -> name.equals(ON_FILE_FUNC)).count();
            if (onFileCount > 1) {
                context.reportDiagnostic(getDiagnostic(MULTIPLE_GENERIC_CONTENT_METHODS,
                        DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
            }
        }
    }
}
