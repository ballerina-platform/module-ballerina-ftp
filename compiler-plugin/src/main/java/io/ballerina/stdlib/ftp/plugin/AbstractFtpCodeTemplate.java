/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.NODE_LOCATION;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.findNode;

/**
 * Abstract base class for format-specific handler code templates.
 * Provides common logic for generating code action templates for FTP handlers.
 */
public abstract class AbstractFtpCodeTemplate implements CodeAction {

    /**
     * Returns the remote function template text for this handler.
     * Subclasses should override this to provide the specific function signature.
     *
     * @return The template text with placeholders for line separators (LS)
     */
    protected abstract String getRemoteFunctionText();

    /**
     * Returns the display name for the code action.
     *
     * @return The name to show in the IDE code action menu
     */
    protected abstract String getActionName();

    /**
     * Returns the template name identifier for this code action.
     *
     * @return The constant name from PluginConstants
     */
    protected abstract String getTemplateName();

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getErrorCode());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext codeActionContext) {
        Diagnostic diagnostic = codeActionContext.diagnostic();
        if (diagnostic.location() == null) {
            return Optional.empty();
        }
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, diagnostic.location().lineRange());
        return Optional.of(CodeActionInfo.from(getActionName(), List.of(locationArg)));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext codeActionExecutionContext) {
        LineRange lineRange = null;
        for (CodeActionArgument argument : codeActionExecutionContext.arguments()) {
            if (NODE_LOCATION.equals(argument.key())) {
                lineRange = argument.valueAs(LineRange.class);
            }
        }

        if (lineRange == null) {
            return Collections.emptyList();
        }

        SyntaxTree syntaxTree = codeActionExecutionContext.currentDocument().syntaxTree();
        NonTerminalNode node = findNode(syntaxTree, lineRange);
        if (!(node instanceof ServiceDeclarationNode)) {
            return Collections.emptyList();
        }

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;

        List<TextEdit> textEdits = new ArrayList<>();
        TextRange resourceTextRange;
        if (serviceDeclarationNode.members().isEmpty()) {
            resourceTextRange = TextRange.from(serviceDeclarationNode.openBraceToken().textRange().endOffset(),
                    serviceDeclarationNode.closeBraceToken().textRange().startOffset() -
                            serviceDeclarationNode.openBraceToken().textRange().endOffset());
        } else {
            Node lastMember = serviceDeclarationNode.members().get(serviceDeclarationNode.members().size() - 1);
            resourceTextRange = TextRange.from(lastMember.textRange().endOffset(),
                    serviceDeclarationNode.closeBraceToken().textRange().startOffset() -
                            lastMember.textRange().endOffset());
        }
        textEdits.add(TextEdit.from(resourceTextRange, getRemoteFunctionText()));
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(new DocumentEdit(codeActionExecutionContext.fileUri(),
                SyntaxTree.from(syntaxTree, change)));
    }

    @Override
    public String name() {
        return getTemplateName();
    }
}
