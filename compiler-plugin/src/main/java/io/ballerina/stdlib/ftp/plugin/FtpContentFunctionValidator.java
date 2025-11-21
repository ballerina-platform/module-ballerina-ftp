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
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

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
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MANDATORY_PARAMETER_NOT_FOUND;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_CSV_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_JSON_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_TEXT_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.ON_FILE_XML_FUNC;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.isRemoteFunction;
import static io.ballerina.stdlib.ftp.plugin.PluginUtils.reportErrorDiagnostic;

/**
 * FTP content function validator.
 */
public class FtpContentFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final FunctionDefinitionNode funcDefinitionNode;
    private final String contentMethodName;

    public FtpContentFunctionValidator(SyntaxNodeAnalysisContext context,
                                       FunctionDefinitionNode funcDefinitionNode,
                                       String contentMethodName) {
        this.context = context;
        this.funcDefinitionNode = funcDefinitionNode;
        this.contentMethodName = contentMethodName;
    }

    public void validate() {
        if (!isRemoteFunction(context, funcDefinitionNode)) {
            reportErrorDiagnostic(context, CONTENT_METHOD_MUST_BE_REMOTE,
                    funcDefinitionNode.location(), contentMethodName);
        }

        SeparatedNodeList<ParameterNode> parameters = funcDefinitionNode.functionSignature().parameters();
        validateContentFunctionParameters(parameters, funcDefinitionNode);
        PluginUtils.validateReturnTypeErrorOrNil(funcDefinitionNode, context);
    }

    private void validateContentFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                   FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.isEmpty()) {
            String expectedType = getExpectedContentType();
            reportErrorDiagnostic(context, MANDATORY_PARAMETER_NOT_FOUND,
                    functionDefinitionNode.location(),
                    contentMethodName, expectedType);
            return;
        }

        if (parameters.size() > 3) {
            reportErrorDiagnostic(context, TOO_MANY_PARAMETERS, functionDefinitionNode.location(),
                    contentMethodName);
            return;
        }

        // First parameter must be content parameter
        ParameterNode firstParameter = parameters.get(0);
        if (!validateContentParameter(firstParameter)) {
            String expectedType = getExpectedContentType();
            String actualType = PluginUtils.getParameterTypeSignature(firstParameter, context);
            reportErrorDiagnostic(context, INVALID_CONTENT_PARAMETER_TYPE, firstParameter.location(),
                    contentMethodName, expectedType, actualType);
        }

        if (parameters.size() == 1) {
            return;
        }

        if (parameters.size() == 2) {
            boolean isFileInfo = PluginUtils.validateFileInfoParameter(parameters.get(1), context);
            if (isFileInfo) {
                return;
            }
            boolean isCaller = PluginUtils.validateCallerParameter(parameters.get(1), context);
            if (!isCaller) {
                reportErrorDiagnostic(context, INVALID_FILEINFO_PARAMETER,
                        parameters.get(1).location(), contentMethodName);
            }
            return;
        }

        // parameters.size() == 3
        if (!PluginUtils.validateFileInfoParameter(parameters.get(1), context)) {
            reportErrorDiagnostic(context, INVALID_FILEINFO_PARAMETER, parameters.get(1).location(),
                    contentMethodName);
            return;
        }

        if (!PluginUtils.validateCallerParameter(parameters.get(2), context)) {
            reportErrorDiagnostic(context, INVALID_CALLER_PARAMETER, parameters.get(2).location());
        }
    }

    private boolean validateContentParameter(ParameterNode parameterNode) {
        Optional<TypeSymbol> typeSymbolOpt = PluginUtils.getParameterTypeSymbol(parameterNode,
                context);
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
            TypeSymbol memberType = ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor();
            return memberType.typeKind() == BYTE;
        } else if (typeKind == STREAM) {
            // Get the stream's item type - should be byte[]
            TypeSymbol itemType = ((StreamTypeSymbol) typeSymbol).typeParameter();
            if (!(itemType instanceof ArrayTypeSymbol arrayType)) {
                return false;
            }
            // Verify the array contains bytes
            TypeSymbol memberType = arrayType.memberTypeDescriptor();
            return memberType.typeKind() == BYTE;
        }
        return false;
    }

    private boolean validateOnFileCsvContentType(TypeDescKind typeKind, TypeSymbol typeSymbol) {
        if (typeKind == ARRAY) {
            // Array variant: string[][] or record[][]
            ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
            return isStringArrayOfArray(arrayTypeSymbol) || isRecordArray(arrayTypeSymbol);
        } else if (typeKind == STREAM) {
            // Stream variant: stream<string[], error?> or stream<record{}, error?>
            StreamTypeSymbol streamTypeSymbol = (StreamTypeSymbol) typeSymbol;
            return isStringArrayStream(streamTypeSymbol) || isRecordStream(streamTypeSymbol);
        }
        return false;
    }

    private boolean isRecordStream(StreamTypeSymbol streamType) {
        // Get the stream's item type - should be record{}
        TypeSymbol itemType = streamType.typeParameter();
        return itemType.typeKind() == RECORD || isRecordTypeReference(itemType);
    }

    private boolean isStringArrayStream(StreamTypeSymbol streamType) {
        // Get the stream's item type - should be string[]
        TypeSymbol itemType = streamType.typeParameter();
        if (!(itemType instanceof ArrayTypeSymbol arrayType)) {
            return false;
        }

        // Verify the array contains strings
        TypeSymbol memberType = arrayType.memberTypeDescriptor();
        return memberType.typeKind() == STRING;
    }

    private boolean isStringArrayOfArray(ArrayTypeSymbol outerArray) {
        TypeSymbol outerMember = outerArray.memberTypeDescriptor();
        if (!(outerMember instanceof ArrayTypeSymbol innerArray)) {
            return false;
        }

        TypeSymbol innerMember = innerArray.memberTypeDescriptor();
        return innerMember.typeKind() == STRING;
    }

    private boolean isRecordArray(ArrayTypeSymbol arrayType) {
        // Get the array's member type
        TypeSymbol memberType = arrayType.memberTypeDescriptor();
        TypeDescKind memberKind = memberType.typeKind();

        // Accept both direct record types and type references to records
        return memberKind == RECORD || isRecordTypeReference(memberType);
    }

    private boolean isRecordTypeReference(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() != TYPE_REFERENCE) {
            return false;
        }

        // Cast to TypeReferenceTypeSymbol and get the referred type
        TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeSymbol;
        TypeSymbol referredType = typeRef.typeDescriptor();

        // Check if the referred type is a record
        return referredType != null && referredType.typeKind() == RECORD;
    }

    private String getExpectedContentType() {
        return switch (contentMethodName) {
            case ON_FILE_FUNC -> "byte[] or stream<byte[], error?>";
            case ON_FILE_TEXT_FUNC -> "string";
            case ON_FILE_JSON_FUNC -> "json or record{}";
            case ON_FILE_XML_FUNC -> "xml or record{}";
            case ON_FILE_CSV_FUNC ->
                    "string[][], record{}[], stream<string[], error?>, or stream<record{}, error?>";
            default -> "unknown";
        };
    }
}
