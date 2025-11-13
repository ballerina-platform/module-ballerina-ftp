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
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.Optional;

import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPE_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CALLER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.FILE_INFO;

/**
 * Util class for the compiler plugin.
 */
public final class PluginUtils {

    private PluginUtils() {}

    public static Diagnostic getDiagnostic(CompilationErrors error, DiagnosticSeverity severity, Location location) {
        String errorMessage = error.getError();
        String diagnosticCode = error.getErrorCode();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticCode, errorMessage, severity);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
    }

    public static Diagnostic getDiagnostic(CompilationErrors error, DiagnosticSeverity severity, Location location,
                                           Object... args) {
        String errorTemplate = error.getError();
        String errorMessage = String.format(errorTemplate, args);
        String diagnosticCode = error.getErrorCode();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticCode, errorMessage, severity);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
    }

    public static boolean validateModuleId(ModuleSymbol moduleSymbol) {
        if (moduleSymbol != null) {
            String moduleName = moduleSymbol.id().moduleName();
            String orgName = moduleSymbol.id().orgName();
            return moduleName.equals(PluginConstants.PACKAGE_PREFIX) && orgName.equals(PluginConstants.PACKAGE_ORG);
        }
        return false;
    }

    public static boolean isRemoteFunction(SyntaxNodeAnalysisContext context,
                                           FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        return methodSymbol.qualifiers().contains(Qualifier.REMOTE);
    }

    public static MethodSymbol getMethodSymbol(SyntaxNodeAnalysisContext context,
                                               FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = null;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isPresent()) {
            methodSymbol = (MethodSymbol) symbol.get();
        }
        return methodSymbol;
    }

    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        if (lineRange == null) {
            return null;
        }
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }

    /**
     * Validates that a parameter is of type ftp:FileInfo.
     *
     * @param parameterNode The parameter node to validate
     * @param context The syntax node analysis context
     * @return true if the parameter is ftp:FileInfo, false otherwise
     */
    public static boolean validateFileInfoParameter(ParameterNode parameterNode,
                                                      SyntaxNodeAnalysisContext context) {
        return validateQualifiedFtpParameter(parameterNode, context, FILE_INFO);
    }

    /**
     * Validates that a parameter is of type ftp:Caller.
     *
     * @param parameterNode The parameter node to validate
     * @param context The syntax node analysis context
     * @return true if the parameter is ftp:Caller, false otherwise
     */
    public static boolean validateCallerParameter(ParameterNode parameterNode,
                                                    SyntaxNodeAnalysisContext context) {
        return validateQualifiedFtpParameter(parameterNode, context, CALLER);
    }

    private static boolean validateQualifiedFtpParameter(ParameterNode parameterNode,
                                                         SyntaxNodeAnalysisContext context,
                                                         String expectedTypeName) {
        if (!(parameterNode instanceof RequiredParameterNode requiredParameterNode)) {
            return false;
        }
        if (requiredParameterNode.typeName().kind() != QUALIFIED_NAME_REFERENCE) {
            return false;
        }
        Optional<TypeSymbol> typeSymbol = getParameterTypeSymbol(parameterNode, context);
        if (typeSymbol.isEmpty()) {
            return false;
        }
        Optional<ModuleSymbol> moduleSymbol = typeSymbol.get().getModule();
        if (moduleSymbol.isEmpty() || !validateModuleId(moduleSymbol.get())) {
            return false;
        }
        return typeSymbol.get().getName().map(expectedTypeName::equals).orElse(false);
    }

    public static Optional<TypeSymbol> getParameterTypeSymbol(ParameterNode parameterNode,
                                                              SyntaxNodeAnalysisContext context) {
        if (!(parameterNode instanceof RequiredParameterNode requiredParameterNode)) {
            return Optional.empty();
        }

        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isEmpty() || !(symbol.get() instanceof ParameterSymbol parameterSymbol)) {
            return Optional.empty();
        }

        return Optional.ofNullable(parameterSymbol.typeDescriptor());
    }

    public static String getParameterTypeSignature(ParameterNode parameterNode,
                                                   SyntaxNodeAnalysisContext context) {
        return getParameterTypeSymbol(parameterNode, context)
                .map(TypeSymbol::signature)
                .orElse("unknown");
    }

    /**
     * Validates that the return type of a function is error? or nil.
     * Reports a diagnostic error if the return type is invalid.
     *
     * @param functionDefinitionNode The function definition node to validate
     * @param context The syntax node analysis context
     */
    public static void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode,
                                                     SyntaxNodeAnalysisContext context) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol == null) {
            return;
        }

        Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
        if (returnTypeDesc.isEmpty()) {
            return;
        }

        TypeDescKind returnTypeKind = returnTypeDesc.get().typeKind();

        if (returnTypeKind == TypeDescKind.NIL) {
            return;
        }

        if (returnTypeKind != TypeDescKind.UNION) {
            return;
        }

        UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) returnTypeDesc.get();
        for (TypeSymbol memberType : unionTypeSymbol.memberTypeDescriptors()) {
            TypeDescKind memberKind = memberType.typeKind();
            if (memberKind == TypeDescKind.NIL) {
                continue;
            }
            if (memberKind == TypeDescKind.ERROR) {
                continue;
            }
            if (memberKind == TYPE_REFERENCE && isValidErrorTypeReference(memberType)) {
                continue;
            }
            context.reportDiagnostic(getDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL, DiagnosticSeverity.ERROR,
                    functionDefinitionNode.functionSignature().location()));
            return;
        }
    }

    private static boolean isValidErrorTypeReference(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() != TYPE_REFERENCE) {
            return false;
        }
        if (typeSymbol.signature().equals(PluginConstants.ERROR)) {
            return true;
        }
        Optional<ModuleSymbol> module = typeSymbol.getModule();
        return module.map(PluginUtils::validateModuleId).orElse(true);
    }
}
