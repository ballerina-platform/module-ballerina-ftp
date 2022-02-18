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

import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * FTP service compilation validator.
 */
public class FTPServiceValidator {

    public void validate(SyntaxNodeAnalysisContext context) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                PluginConstants.CompilationErrors.NO_ON_CONSUMER_RECORD.getErrorCode(),
                PluginConstants.CompilationErrors.NO_ON_CONSUMER_RECORD.getError(),
                DiagnosticSeverity.ERROR);
        context.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                serviceDeclarationNode.location()));

    }
}
