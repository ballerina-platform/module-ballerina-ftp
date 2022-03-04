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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.ftp.plugin.PluginUtils.validateModuleId;

/**
 * FTP service compilation analysis task.
 */
public class FtpServiceAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private final FtpServiceValidator serviceValidator;

    public FtpServiceAnalysisTask() {
        this.serviceValidator = new FtpServiceValidator();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        List<Diagnostic> diagnostics = context.semanticModel().diagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                return;
            }
        }
        if (!isFtpService(context)) {
            return;
        }
        this.serviceValidator.validate(context);
    }

    private boolean isFtpService(SyntaxNodeAnalysisContext context) {
        SemanticModel semanticModel = context.semanticModel();
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        Optional<Symbol> symbol = semanticModel.symbol(serviceDeclarationNode);
        if (symbol.isPresent()) {
            ServiceDeclarationSymbol serviceDeclarationSymbol = (ServiceDeclarationSymbol) symbol.get();
            List<TypeSymbol> listeners = serviceDeclarationSymbol.listenerTypes();
            for (TypeSymbol listener: listeners) {
                if (!isFtpListener(listener)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isFtpListener(TypeSymbol listener) {
        boolean isFtpListener = false;
        if (listener.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) listener;
            List<TypeSymbol> members = unionTypeSymbol.memberTypeDescriptors();
            for (TypeSymbol memberSymbol : members) {
                Optional<ModuleSymbol> module = memberSymbol.getModule();
                if (module.isPresent()) {
                    isFtpListener = validateModuleId(module.get());
                }
            }
        } else {
            Optional<ModuleSymbol> module = listener.getModule();
            if (module.isPresent()) {
                isFtpListener = validateModuleId(module.get());
            }
        }
        return isFtpListener;
    }
}
