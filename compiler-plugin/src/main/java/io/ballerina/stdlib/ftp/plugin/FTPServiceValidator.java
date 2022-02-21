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
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CHANGE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;

/**
 * FTP service compilation validator.
 */
public class FTPServiceValidator {

    public void validate(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        NodeList<Node> memberNodes = serviceDeclarationNode.members();

//        boolean hasRemoteFunction = serviceDeclarationNode.members().stream().anyMatch(child ->
//                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION &&
//                        isRemoteFunction(context, (FunctionDefinitionNode) child));
//
//        boolean hasResourceFunction = serviceDeclarationNode.members().stream().anyMatch(child ->
//                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION &&
//                        isResourceFunction(context, (FunctionDefinitionNode) child));

        boolean hasFunction = serviceDeclarationNode.members().stream().anyMatch(child ->
                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION &&
                        isFunction(context, (FunctionDefinitionNode) child));

//        if (hasRemoteFunction) {
//
//        }
//
//        if (hasResourceFunction) {
//
//        }

        if (serviceDeclarationNode.members().isEmpty() || !hasFunction) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getErrorCode(),
                    PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getError(),
                    DiagnosticSeverity.INTERNAL);
            context.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    serviceDeclarationNode.location()));
        }

        validateAnnotation(context);
        FunctionDefinitionNode onFileChange = null;

        for (Node node : memberNodes) {
            if (node.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION) {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                MethodSymbol methodSymbol = PluginUtils.getMethodSymbol(context, functionDefinitionNode);
                Optional<String> functionName = methodSymbol.getName();
                if (functionName.isPresent()) {
                    if (functionName.get().equals(ON_FILE_CHANGE_FUNC)) {
                        onFileChange = functionDefinitionNode;
                    } else if (isRemoteFunction(context, functionDefinitionNode)) {
                        context.reportDiagnostic(getDiagnostic(INVALID_FUNCTION,
                                DiagnosticSeverity.ERROR, functionDefinitionNode.location()));
                    }
                }
            } else if (node.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(INVALID_FUNCTION,
                        DiagnosticSeverity.ERROR, node.location()));
            }
        }
        new FTPFunctionValidator(context, onFileChange).validate();
    }

    private void validateAnnotation(SyntaxNodeAnalysisContext context) {
        SemanticModel semanticModel = context.semanticModel();
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> symbol = semanticModel.symbol(serviceDeclarationNode);
        if (symbol.isPresent()) {
            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) symbol.get();
            List<AnnotationSymbol> symbolList = serviceDeclarationSymbol.annotations();
            if (!symbolList.isEmpty()) {
                context.reportDiagnostic(PluginUtils.getDiagnostic(CompilationErrors.INVALID_ANNOTATION_NUMBER,
                        DiagnosticSeverity.ERROR, serviceDeclarationNode.location()));
            }
        }
    }
}
