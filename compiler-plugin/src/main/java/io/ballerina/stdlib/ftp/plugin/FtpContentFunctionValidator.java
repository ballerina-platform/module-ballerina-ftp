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
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.Objects;
import java.util.Optional;

import static io.ballerina.compiler.api.symbols.TypeDescKind.ARRAY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.BYTE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.JSON;
import static io.ballerina.compiler.api.symbols.TypeDescKind.RECORD;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STREAM;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STRING;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPE_REFERENCE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.XML;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.CONTENT_METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CONTENT_PARAMETER_TYPE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_FILEINFO_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CSV_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_JSON_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_TEXT_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_XML_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.getDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;

/**
 * FTP content function validator.
 */
public class FtpContentFunctionValidator {

    private final SyntaxNodeAnalysisContext syntaxNodeAnalysisContext;
    private final FunctionDefinitionNode contentFunctionDefinitionNode;
    private final String contentMethodName;

    public FtpContentFunctionValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                       FunctionDefinitionNode contentFunctionDefinitionNode,
                                       String contentMethodName) {
        this.syntaxNodeAnalysisContext = syntaxNodeAnalysisContext;
        this.contentFunctionDefinitionNode = contentFunctionDefinitionNode;
        this.contentMethodName = contentMethodName;
    }

    public void validate() {
        if (Objects.isNull(contentFunctionDefinitionNode)) {
            return;
        }

        if (!isRemoteFunction(syntaxNodeAnalysisContext, contentFunctionDefinitionNode)) {
            reportErrorDiagnostic(CONTENT_METHOD_MUST_BE_REMOTE, contentFunctionDefinitionNode.location(),
                    contentMethodName);
        }

        SeparatedNodeList<ParameterNode> parameters = contentFunctionDefinitionNode.functionSignature().parameters();
        validateContentFunctionParameters(parameters, contentFunctionDefinitionNode);
        PluginUtils.validateReturnTypeErrorOrNil(contentFunctionDefinitionNode, syntaxNodeAnalysisContext);
    }

    private void validateContentFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                    FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.isEmpty()) {
            String expectedType = getExpectedContentType();
            reportErrorDiagnostic(INVALID_CONTENT_PARAMETER_TYPE, functionDefinitionNode.location(),
                    contentMethodName, expectedType, "none");
            return;
        }

        if (parameters.size() > 3) {
            reportErrorDiagnostic(TOO_MANY_PARAMETERS, functionDefinitionNode.location(), contentMethodName);
            return;
        }

        // First parameter must be content parameter
        ParameterNode firstParameter = parameters.get(0);
        if (!validateContentParameter(firstParameter)) {
            String expectedType = getExpectedContentType();
            String actualType = PluginUtils.getParameterTypeSignature(firstParameter, syntaxNodeAnalysisContext);;
            reportErrorDiagnostic(INVALID_CONTENT_PARAMETER_TYPE, firstParameter.location(),
                    contentMethodName, expectedType, actualType);
        }

        if (parameters.size() == 1) {
            return;
        }

        if (parameters.size() == 2) {
            boolean isFileInfo = PluginUtils.validateFileInfoParameter(parameters.get(1), syntaxNodeAnalysisContext);
            boolean isCaller = PluginUtils.validateCallerParameter(parameters.get(1), syntaxNodeAnalysisContext);

            if (!isFileInfo && !isCaller) {
                reportErrorDiagnostic(INVALID_FILEINFO_PARAMETER, parameters.get(1).location(), contentMethodName);
            }
            return;
        }

        // parameters.size() == 3
        if (!PluginUtils.validateFileInfoParameter(parameters.get(1), syntaxNodeAnalysisContext)) {
            reportErrorDiagnostic(INVALID_FILEINFO_PARAMETER, parameters.get(1).location(), contentMethodName);
            return;
        }

        if (!PluginUtils.validateCallerParameter(parameters.get(2), syntaxNodeAnalysisContext)) {
            reportErrorDiagnostic(INVALID_CALLER_PARAMETER, parameters.get(2).location());
        }
    }

    private boolean validateContentParameter(ParameterNode parameterNode) {
        Optional<TypeSymbol> typeSymbolOpt = PluginUtils.getParameterTypeSymbol(parameterNode,
                syntaxNodeAnalysisContext);
        if (typeSymbolOpt.isEmpty()) {
            return false;
        }

        TypeSymbol typeSymbol = typeSymbolOpt.get();
        TypeDescKind typeKind = typeSymbol.typeKind();

        return switch (contentMethodName) {
            case ON_FILE_FUNC -> validateOnFileContentType(typeKind, typeSymbol);
            case ON_FILE_TEXT_FUNC -> typeKind == STRING;
            case ON_FILE_JSON_FUNC -> typeKind == JSON || typeKind == RECORD || isRecordTypeReference(typeSymbol);
            case ON_FILE_XML_FUNC -> typeKind == XML || typeKind == RECORD || isRecordTypeReference(typeSymbol);
            case ON_FILE_CSV_FUNC -> validateOnFileCsvContentType(typeKind, typeSymbol);
            default -> false;
        };
    }

    private boolean validateOnFileContentType(TypeDescKind typeKind, TypeSymbol typeSymbol) {
        // onFile accepts: byte[] or stream<byte[], error?>
        if (typeKind == ARRAY) {
            // Check if it's byte[]
            return isByteArray(typeSymbol);
        } else if (typeKind == STREAM) {
            // Check if it's stream<byte[], error?>
            return isByteStream(typeSymbol);
        }
        return false;
    }

    private boolean validateOnFileCsvContentType(TypeDescKind typeKind, TypeSymbol typeSymbol) {
        // onFileCsv accepts: string[][], record{}[], or stream<string[], error?>
        if (typeKind == ARRAY) {
            return isStringArrayArray(typeSymbol) || isRecordArray(typeSymbol);
        } else if (typeKind == STREAM) {
            return isStringArrayStream(typeSymbol);
        }
        return false;
    }

    private boolean isByteArray(TypeSymbol typeSymbol) {
        if (typeSymbol instanceof ArrayTypeSymbol arrayType) {
            TypeSymbol memberType = arrayType.memberTypeDescriptor();
            return memberType.typeKind() == BYTE;
        }
        return false;
    }


    private boolean isByteStream(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() != STREAM || !(typeSymbol instanceof StreamTypeSymbol streamType)) {
            return false;
        }
        TypeSymbol itemType = streamType.typeParameter();
        if (!(itemType instanceof ArrayTypeSymbol arrayType)) {
            return false;
        }
        TypeSymbol memberType = arrayType.memberTypeDescriptor();
        return memberType.typeKind() == BYTE;
    }

    private boolean isStringArrayStream(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() != STREAM || !(typeSymbol instanceof StreamTypeSymbol streamType)) {
            return false;
        }
        TypeSymbol itemType = streamType.typeParameter();
        if (!(itemType instanceof ArrayTypeSymbol arrayType)) {
            return false;
        }
        TypeSymbol memberType = arrayType.memberTypeDescriptor();
        return memberType.typeKind() == STRING;
    }


    private boolean isStringArrayArray(TypeSymbol typeSymbol) {
        if (!(typeSymbol instanceof ArrayTypeSymbol outerArray)) {
            return false;
        }
        TypeSymbol outerMember = outerArray.memberTypeDescriptor();

        if (!(outerMember instanceof ArrayTypeSymbol innerArray)) {
            return false;
        }
        TypeSymbol innerMember = innerArray.memberTypeDescriptor();
        return innerMember.typeKind() == STRING;
    }

    private boolean isRecordArray(TypeSymbol typeSymbol) {
        if (typeSymbol instanceof ArrayTypeSymbol arrayType) {
            TypeSymbol memberType = arrayType.memberTypeDescriptor();
            return memberType.typeKind() == RECORD;
        }
        return false;
    }

    private boolean isRecordTypeReference(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TYPE_REFERENCE) {
            // Get the referred type and check if it's a record
            TypeSymbol referredType = ((io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol) typeSymbol)
                    .typeDescriptor();
            return referredType != null && referredType.typeKind() == RECORD;
        }
        return false;
    }

    private String getExpectedContentType() {
        return switch (contentMethodName) {
            case ON_FILE_FUNC -> "byte[] or stream<byte[], error?>";
            case ON_FILE_TEXT_FUNC -> "string";
            case ON_FILE_JSON_FUNC -> "json or record type";
            case ON_FILE_XML_FUNC -> "xml or record type";
            case ON_FILE_CSV_FUNC -> "string[][], record{}[], or stream<string[], error?>";
            default -> "unknown";
        };
    }

    public void reportErrorDiagnostic(PluginConstants.CompilationErrors error, Location location) {
        syntaxNodeAnalysisContext.reportDiagnostic(getDiagnostic(error,
                DiagnosticSeverity.ERROR, location));
    }

    public void reportErrorDiagnostic(PluginConstants.CompilationErrors error, Location location, Object... args) {
        syntaxNodeAnalysisContext.reportDiagnostic(getDiagnostic(error,
                DiagnosticSeverity.ERROR, location, args));
    }
}
