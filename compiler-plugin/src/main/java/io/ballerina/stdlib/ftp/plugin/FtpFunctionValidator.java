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
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.INTERSECTION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CALLER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_PARAMETERS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_WATCHEVENT_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MUST_HAVE_WATCHEVENT;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.WATCHEVENT;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.validateModuleId;
import static io.ballerina.tools.diagnostics.DiagnosticSeverity.ERROR;

/**
 * FTP function validator.
 */
public class FtpFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    FunctionDefinitionNode onFileChange;

    public FtpFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onFileChange) {
        this.context = context;
        this.onFileChange = onFileChange;
    }

    public void validate() {
        if (!isRemoteFunction(context, onFileChange)) {
            reportErrorDiagnostic(METHOD_MUST_BE_REMOTE, onFileChange.location());
        }
        SeparatedNodeList<ParameterNode> parameters = onFileChange.functionSignature().parameters();
        validateFunctionArguments(parameters, onFileChange);
        PluginUtils.validateReturnTypeErrorOrNil(onFileChange, context);
    }

    private void validateFunctionArguments(SeparatedNodeList<ParameterNode> parameters,
                                           FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 1) {
            validateSingleArgumentScenario(parameters);
        } else if (parameters.size() == 2) {
            validateTwoArgumentScenario(parameters, functionDefinitionNode);
        } else if (parameters.size() > 2) {
            reportErrorDiagnostic(ONLY_PARAMS_ALLOWED, functionDefinitionNode.location());
        } else {
            reportErrorDiagnostic(MUST_HAVE_WATCHEVENT, functionDefinitionNode.location());
        }
    }

    private void validateTwoArgumentScenario(SeparatedNodeList<ParameterNode> parameters,
                                             FunctionDefinitionNode functionDefinitionNode) {
        ParameterNode firstParamNode = parameters.get(0);
        ParameterNode secondParamNode = parameters.get(1);
        SyntaxKind firstParamSyntaxKind = ((RequiredParameterNode) firstParamNode).typeName().kind();
        SyntaxKind secondParamSyntaxKind = ((RequiredParameterNode) secondParamNode).typeName().kind();

        if (firstParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE) &&
                secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateCallerAndWatchEvent(firstParamNode, secondParamNode);
        } else if (firstParamSyntaxKind.equals(INTERSECTION_TYPE_DESC) &&
                secondParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateInvalidReadonlyArguments(firstParamNode, secondParamNode);
        } else if (firstParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateCallerAndReadonlyWatchEvent(firstParamNode, secondParamNode, secondParamSyntaxKind);
        } else if (secondParamSyntaxKind.equals(INTERSECTION_TYPE_DESC)) {
            validateCallerAndReadonlyWatchEvent(secondParamNode, firstParamNode, firstParamSyntaxKind);
        } else if (firstParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateInvalidArgument(firstParamNode, secondParamNode);
        } else if (secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
            validateInvalidArgument(secondParamNode, firstParamNode);
        } else {
            reportErrorDiagnostic(INVALID_PARAMETERS, functionDefinitionNode.functionSignature().location());
        }
    }

    private void validateSingleArgumentScenario(SeparatedNodeList<ParameterNode> parameters) {
        ParameterNode paramNode = parameters.get(0);
        SyntaxKind paramSyntaxKind = ((RequiredParameterNode) paramNode).typeName().kind();
        if ((!paramSyntaxKind.equals(INTERSECTION_TYPE_DESC) || !validateIntersectionParam(paramNode)) &&
                (!paramSyntaxKind.equals(QUALIFIED_NAME_REFERENCE) || !validateWatchEventParam(paramNode))) {
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, paramNode.location());
        }
    }

    private void validateInvalidArgument(ParameterNode qualifiedRefParamNode, ParameterNode invalidParamNode) {
        if (validateCallerParam(qualifiedRefParamNode)) {
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, invalidParamNode.location());
        } else if (validateWatchEventParam(qualifiedRefParamNode)) {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, invalidParamNode.location());
        } else {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, qualifiedRefParamNode.location());
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, invalidParamNode.location());
        }
    }

    private void validateCallerAndReadonlyWatchEvent(ParameterNode intersectionParamNode, ParameterNode secondParamNode,
                                                     SyntaxKind secondParamSyntaxKind) {
        boolean intersectionParamWatchEvent = validateIntersectionParam(intersectionParamNode);
        boolean secondParamCaller = validateCallerParam(secondParamNode);
        if (!secondParamCaller && !intersectionParamWatchEvent) {
            if (secondParamSyntaxKind.equals(QUALIFIED_NAME_REFERENCE)) {
                boolean secondParamWatchEvent = validateWatchEventParam(secondParamNode);
                if (secondParamWatchEvent) {
                    reportErrorDiagnostic(INVALID_CALLER_PARAMETER, intersectionParamNode.location());
                    return;
                }
            }
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, secondParamNode.location());
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, intersectionParamNode.location());
        } else if (!intersectionParamWatchEvent) {
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, intersectionParamNode.location());
        } else if (!secondParamCaller) {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, secondParamNode.location());
        }
    }

    private void validateInvalidReadonlyArguments(ParameterNode firstParamNode, ParameterNode secondParamNode) {
        boolean firstParamWatchEvent = validateIntersectionParam(firstParamNode);
        if (!firstParamWatchEvent) {
            boolean secondParamWatchEvent = validateIntersectionParam(secondParamNode);
            if (!secondParamWatchEvent) {
                reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, secondParamNode.location());
            }
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, firstParamNode.location());
        } else {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, secondParamNode.location());
        }
    }

    private void validateCallerAndWatchEvent(ParameterNode firstParamNode, ParameterNode secondParamNode) {
        boolean firstParamCaller = validateCallerParam(firstParamNode);
        boolean secondParamWatchEvent = validateWatchEventParam(secondParamNode);
        if (firstParamCaller && secondParamWatchEvent) {
            return;
        }
        boolean firstParamWatchEvent = validateWatchEventParam(firstParamNode);
        boolean secondParamCaller = validateCallerParam(secondParamNode);
        if (secondParamCaller && firstParamWatchEvent) {
            return;
        }
        if (!firstParamCaller && secondParamWatchEvent) {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, firstParamNode.location());
        } else if (!secondParamCaller && firstParamWatchEvent) {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, secondParamNode.location());
        } else if (firstParamCaller) {
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, secondParamNode.location());
        } else if (secondParamCaller) {
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, firstParamNode.location());
        } else {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, firstParamNode.location());
            reportErrorDiagnostic(INVALID_WATCHEVENT_PARAMETER, secondParamNode.location());
        }
    }

    private boolean validateIntersectionParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> symbol = semanticModel.symbol(requiredParameterNode);
        if (symbol.isPresent()) {
            ParameterSymbol parameterSymbol = (ParameterSymbol) symbol.get();
            if (parameterSymbol.typeDescriptor() instanceof IntersectionTypeSymbol) {
                IntersectionTypeSymbol intersectionTypeSymbol =
                        (IntersectionTypeSymbol) parameterSymbol.typeDescriptor();
                boolean watchEventExists = intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .anyMatch(typeSymbol -> typeSymbol.nameEquals(WATCHEVENT) &&
                                typeSymbol.getModule().isPresent() &&
                                validateModuleId(typeSymbol.getModule().get()));
                if (watchEventExists) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validateWatchEventParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        SemanticModel semanticModel = context.semanticModel();
        Optional<Symbol> paramSymbol = semanticModel.symbol(parameterTypeNode);
        if (paramSymbol.isPresent()) {
            Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
            if (moduleSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().isPresent() ? paramSymbol.get().getName().get() : "";
                if (validateModuleId(moduleSymbol.get()) && paramName.equals(WATCHEVENT)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validateCallerParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
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

    public void reportErrorDiagnostic(PluginConstants.CompilationErrors error, Location location) {
        context.reportDiagnostic(getDiagnostic(error, ERROR, location));
    }
}
