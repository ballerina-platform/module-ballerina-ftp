/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.ballerina.stdlib.ftp.plugin.CompilerPluginTestUtils.assertDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.CompilerPluginTestUtils.loadPackage;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.CONTENT_METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_CONTENT_PARAMETER_TYPE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_FILEINFO_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETED_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTION;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_WATCHEVENT_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MANDATORY_PARAMETER_NOT_FOUND;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MULTIPLE_CONTENT_METHODS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MUST_HAVE_WATCHEVENT;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.NO_VALID_REMOTE_METHOD;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_CHANGE_DEPRECATED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETE_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETE_CALLER_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_FILE_DELETE_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.BOTH_ON_FILE_DELETE_METHODS_NOT_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.INVALID_ON_ERROR_SECOND_PARAMETER;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_ERROR_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_ERROR;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS_ON_FILE_DELETED;

/**
 * Tests for FTP package compiler plugin.
 */
public class FtpServiceValidationTest {

    @Test(description = "Validation with multiple listeners on same service")
    public void testValidService1() {
        Package currentPackage = loadPackage("valid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with renamed ftp import")
    public void testValidService2() {
        Package currentPackage = loadPackage("valid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with service level variables defined")
    public void testValidService3() {
        Package currentPackage = loadPackage("valid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with other functions defined in the service")
    public void testValidService4() {
        Package currentPackage = loadPackage("valid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with multiple error return types")
    public void testValidService5() {
        Package currentPackage = loadPackage("valid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with service containing annotations")
    public void testValidService6() {
        Package currentPackage = loadPackage("valid_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Deprecation warning is emitted when onFileChange is used")
    public void testOnFileChangeDeprecationWarning() {
        Package currentPackage = loadPackage("valid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Diagnostic warning = diagnosticResult.diagnostics().stream()
                .filter(diagnostic -> diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.WARNING)
                .findFirst()
                .orElse(null);
        Assert.assertNotNull(warning, "Expected a deprecation warning for onFileChange usage.");
        assertDiagnostic(warning, ON_FILE_CHANGE_DEPRECATED,
                "onFileChange is deprecated. Use format-specific handlers (onFileJson, " +
                "onFileXml, onFileCsv, onFileText) for automatic type conversion, or onFileDelete for deletion " +
                "events.");
    }

    @Test(description = "Validation with content listener methods and onFileDeleted handler")
    public void testValidContentService1() {
        Package currentPackage = loadPackage("valid_content_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with generic onFile content handler")
    public void testValidContentService2() {
        Package currentPackage = loadPackage("valid_content_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with onFileCsv using record array (Employee[])")
    public void testValidContentService3() {
        Package currentPackage = loadPackage("valid_content_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with onFileCsv using stream<string[], error?>")
    public void testValidContentService4() {
        Package currentPackage = loadPackage("valid_content_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with onFile byte[], xml content, and inline record array content handlers")
    public void testValidContentService5() {
        Package currentPackage = loadPackage("valid_content_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with onFileCsv using stream<record[], error?>")
    public void testValidContentService6() {
        Package currentPackage = loadPackage("valid_content_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with valid onFileDelete method")
    public void testValidContentService7() {
        Package currentPackage = loadPackage("valid_content_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation when no valid remote function is defined")
    public void testInvalidService1() {
        Package currentPackage = loadPackage("invalid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, NO_VALID_REMOTE_METHOD,
                "Service must define at least one handler method: onFile, onFileText, onFileJson, " +
                "onFileXml, onFileCsv (format-specific) or onFileDelete.");
    }

    @Test(description = "Validation when 2 remote functions are defined")
    public void testInvalidService2() {
        Package currentPackage = loadPackage("invalid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_REMOTE_FUNCTION,
                "Invalid remote method. Allowed handlers: onFile, onFileText, onFileJson, " +
                "onFileXml, onFileCsv (format-specific) or onFileDelete.");
    }

    @Test(description = "Validation when onFileChange function is not remote")
    public void testInvalidService3() {
        Package currentPackage = loadPackage("invalid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, METHOD_MUST_BE_REMOTE);
    }

    @Test(description = "Validation when a resource function is added without a valid remote function")
    public void testInvalidService4() {
        Package currentPackage = loadPackage("invalid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, NO_VALID_REMOTE_METHOD);
    }

    @Test(description = "Validation when a resource function is added")
    public void testInvalidService5() {
        Package currentPackage = loadPackage("invalid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, RESOURCE_FUNCTION_NOT_ALLOWED);
    }

    @Test(description = "Validation when invalid method qualifiers are added " +
            "(resource/remote) without a valid remote function")
    public void testInvalidService6() {
        Package currentPackage = loadPackage("invalid_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, INVALID_REMOTE_FUNCTION);
        Diagnostic diagnostic3 = (Diagnostic) diagnostics[2];
        assertDiagnostic(diagnostic3, NO_VALID_REMOTE_METHOD);
    }

    @Test(description = "Validation when invalid method qualifiers are added (resource/remote) with onFileChange")
    public void testInvalidService7() {
        Package currentPackage = loadPackage("invalid_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, INVALID_REMOTE_FUNCTION);
        Diagnostic diagnostic3 = (Diagnostic) diagnostics[2];
        assertDiagnostic(diagnostic3, METHOD_MUST_BE_REMOTE);
    }

    @Test(description = "Validation when no arguments are added to method definition")
    public void testInvalidService8() {
        Package currentPackage = loadPackage("invalid_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MUST_HAVE_WATCHEVENT);
    }

    @Test(description = "Validation when a readonly WatchEvent argument and an invalid argument is added")
    public void testInvalidService9() {
        Package currentPackage = loadPackage("invalid_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 4);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_CALLER_PARAMETER);
        }
    }

    @Test(description = "Validation when 1 invalid argument is added to method definition")
    public void testInvalidService10() {
        Package currentPackage = loadPackage("invalid_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 4);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_WATCHEVENT_PARAMETER);
        }
    }

    @Test(description = "Validation when invalid return types are added in method definition")
    public void testInvalidService11() {
        Package currentPackage = loadPackage("invalid_service_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 6);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ERROR_OR_NIL);
        }
    }

    @Test(description = "Validation when a WatchEvent argument and invalid argument is added to method definition")
    public void testInvalidService12() {
        Package currentPackage = loadPackage("invalid_service_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_CALLER_PARAMETER);
        }
    }

    @Test(description = "Validation when 3 arguments are added to method definition")
    public void testInvalidService13() {
        Package currentPackage = loadPackage("invalid_service_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, ONLY_PARAMS_ALLOWED);
        }
    }

    @Test(description = "Validation when 2 WatchEvent arguments are added to method definition")
    public void testInvalidService14() {
        Package currentPackage = loadPackage("invalid_service_14");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 4);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_CALLER_PARAMETER);
        }
    }

    @Test(description = "Validation when 2 Caller arguments are added to method definition")
    public void testInvalidService15() {
        Package currentPackage = loadPackage("invalid_service_15");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_WATCHEVENT_PARAMETER);
    }

    @Test(description = "Validation when invalid qualified ref is added as first param and watchevent " +
            "is added as second param")
    public void testInvalidService16() {
        Package currentPackage = loadPackage("invalid_service_16");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_CALLER_PARAMETER);
        }
    }

    @Test(description = "Validation when 2 invalid qualified refs are added")
    public void testInvalidService17() {
        Package currentPackage = loadPackage("invalid_service_17");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, INVALID_CALLER_PARAMETER);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, INVALID_WATCHEVENT_PARAMETER);
    }

    @Test(description = "Validation when 1 invalid intersection param and 1 invalid param added")
    public void testInvalidService18() {
        Package currentPackage = loadPackage("invalid_service_18");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 4);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, INVALID_CALLER_PARAMETER);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, INVALID_WATCHEVENT_PARAMETER);
        Diagnostic diagnostic3 = (Diagnostic) diagnostics[2];
        assertDiagnostic(diagnostic3, INVALID_CALLER_PARAMETER);
        Diagnostic diagnostic4 = (Diagnostic) diagnostics[3];
        assertDiagnostic(diagnostic4, INVALID_WATCHEVENT_PARAMETER);
    }

    @Test(description = "Validation when content method is not remote")
    public void testInvalidContentService1() {
        Package currentPackage = loadPackage("invalid_content_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CONTENT_METHOD_MUST_BE_REMOTE,
                "onFileText handler must be declared as remote.");
    }

    @Test(description = "Validation when content method has invalid first parameter type")
    public void testInvalidContentService2() {
        Package currentPackage = loadPackage("invalid_content_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFileJson. Expected json or record{}, found int.");
    }

    @Test(description = "Validation when content method has invalid fileInfo parameter")
    public void testInvalidContentService3() {
        Package currentPackage = loadPackage("invalid_content_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_FILEINFO_PARAMETER,
                "Invalid parameter for onFileText. Optional second parameter must be FileInfo.");
    }

    @Test(description = "Validation when content method defines too many parameters")
    public void testInvalidContentService4() {
        Package currentPackage = loadPackage("invalid_content_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS,
                "Too many parameters for onFileCsv. Format-specific handlers " +
                        "accept at most 3 parameters: (content, fileInfo?, caller?).");
    }

    @Test(description = "Validation when content method caller parameter is invalid")
    public void testInvalidContentService5() {
        Package currentPackage = loadPackage("invalid_content_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CALLER_PARAMETER);
    }

    @Test(description = "Validation when onFileChange is used with content handlers")
    public void testInvalidContentService6() {
        Package currentPackage = loadPackage("invalid_content_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MULTIPLE_CONTENT_METHODS,
                "Cannot mix event-based handler (onFileChange) with " +
                "format-specific handlers (onFile, onFileText, onFileJson, onFileXml, onFileCsv, onFileDelete).");
    }

    @Test(description = "Validation when onFileDeleted method is not remote")
    public void testInvalidContentService7() {
        Package currentPackage = loadPackage("invalid_content_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ON_FILE_DELETED_MUST_BE_REMOTE,
                "onFileDeleted method must be remote.");
    }

    @Test(description = "Validation when onFileDeleted has invalid deleted files parameter")
    public void testInvalidContentService8() {
        Package currentPackage = loadPackage("invalid_content_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETED_PARAMETER,
                "Invalid parameter for onFileDeleted. First parameter must be " +
                "string[] (list of deleted file paths).");
    }

    @Test(description = "Validation when onFileDeleted has invalid caller parameter")
    public void testInvalidContentService9() {
        Package currentPackage = loadPackage("invalid_content_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETED_CALLER_PARAMETER,
                "Invalid second parameter for onFileDeleted. " +
                "Optional second parameter must be Caller.");
    }

    @Test(description = "Validation when onFileDeleted has too many parameters")
    public void testInvalidContentService10() {
        Package currentPackage = loadPackage("invalid_content_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS_ON_FILE_DELETED,
                "Too many parameters for onFileDeleted. Accepts at most 2 parameters: " +
                "(deletedFiles, caller?).");
    }

    @Test(description = "Validation when onFileJson has invalid return type")
    public void testInvalidContentService11() {
        Package currentPackage = loadPackage("invalid_content_service_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ERROR_OR_NIL,
                "Invalid return type. Expected error? or ftp:Error?.");
    }

    @Test(description = "Validation when onFileCsv uses stream<byte[], error?> instead of stream<string[], error?>")
    public void testInvalidContentService12() {
        Package currentPackage = loadPackage("invalid_content_service_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFileCsv. Expected string[][], record{}[], " +
                        "stream<string[], error?>, or stream<record{}, error?>, found stream<byte[], error?>.");
    }

    @Test(description = "Validation when onFileCsv uses stream<int[], error?> instead of stream<string[], error?>")
    public void testInvalidContentService13() {
        Package currentPackage = loadPackage("invalid_content_service_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFileCsv. Expected string[][], record{}[], " +
                        "stream<string[], error?>, or stream<record{}, error?>, found stream<int[], error?>.");
    }

    @Test(description = "Validation when a content handler omits the required content parameter")
    public void testInvalidContentService14() {
        Package currentPackage = loadPackage("invalid_content_service_14");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MANDATORY_PARAMETER_NOT_FOUND,
                "Mandatory parameter missing for onFileText. Expected string.");
    }

    @Test(description = "Validation when a three-parameter content handler uses an invalid fileInfo parameter")
    public void testInvalidContentService15() {
        Package currentPackage = loadPackage("invalid_content_service_15");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_FILEINFO_PARAMETER,
                "Invalid parameter for onFileText. Optional second parameter must be FileInfo.");
    }

    @Test(description = "Validation when onFileDeleted does not define any parameters")
    public void testInvalidContentService16() {
        Package currentPackage = loadPackage("invalid_content_service_16");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MANDATORY_PARAMETER_NOT_FOUND,
                "Mandatory parameter missing for onFileDeleted. Expected string[].");
    }

    @Test(description = "Validation when onFileChange and onFileDeleted are mixed in the same service")
    public void testInvalidContentService17() {
        Package currentPackage = loadPackage("invalid_content_service_17");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MULTIPLE_CONTENT_METHODS,
                "Cannot mix event-based handler (onFileChange) with " +
                "format-specific handlers (onFile, onFileText, onFileJson, onFileXml, onFileCsv, onFileDelete).");
    }

    @Test(description = "Validation when onFile handler uses invalid stream type (stream<byte, error?> " +
            "instead of stream<byte[], error?>)")
    public void testInvalidContentService18() {
        Package currentPackage = loadPackage("invalid_content_service_18");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFile. Expected byte[] or stream<byte[], error?>, " +
                        "found stream<byte, error?>.");
    }

    @Test(description = "Validation when onFileXml handler accepts stream type instead of xml")
    public void testInvalidContentService19() {
        Package currentPackage = loadPackage("invalid_content_service_19");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFileXml. " +
                        "Expected xml or record{}, found stream<xml, error?>.");
    }

    @Test(description = "Validation when service on multiple listeners uses incompatible content handler types")
    public void testInvalidContentService20() {
        Package currentPackage = loadPackage("invalid_content_service_20");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        assertDiagnostic(((Diagnostic) diagnostics[0]), INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFile. " +
                        "Expected byte[] or stream<byte[], error?>, found byte.");
        assertDiagnostic(((Diagnostic) diagnostics[1]), INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for handler onFileXml. Expected xml or record{}, found json.");
    }

    @Test(description = "Validation when content handlers use invalid return types instead of error?")
    public void testInvalidContentService21() {
        Package currentPackage = loadPackage("invalid_content_service_21");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ERROR_OR_NIL);
        }
    }

    @Test(description = "Validation when onFileDelete method is not remote")
    public void testInvalidContentService22() {
        Package currentPackage = loadPackage("invalid_content_service_22");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ON_FILE_DELETE_MUST_BE_REMOTE,
                "onFileDelete method must be remote.");
    }

    @Test(description = "Validation when onFileDelete has invalid deleted file parameter")
    public void testInvalidContentService23() {
        Package currentPackage = loadPackage("invalid_content_service_23");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETE_PARAMETER,
                "Invalid parameter for onFileDelete. First parameter must be " +
                "string (deleted file path).");
    }

    @Test(description = "Validation when onFileDelete has invalid caller parameter")
    public void testInvalidContentService24() {
        Package currentPackage = loadPackage("invalid_content_service_24");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETE_CALLER_PARAMETER,
                "Invalid second parameter for onFileDelete. " +
                "Optional second parameter must be Caller.");
    }

    @Test(description = "Validation when onFileDelete has too many parameters")
    public void testInvalidContentService25() {
        Package currentPackage = loadPackage("invalid_content_service_25");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS_ON_FILE_DELETE,
                "Too many parameters for onFileDelete. Accepts at most 2 parameters: " +
                "(deletedFile, caller?).");
    }

    @Test(description = "Validation when both onFileDelete and onFileDeleted are present")
    public void testInvalidContentService26() {
        Package currentPackage = loadPackage("invalid_content_service_26");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, BOTH_ON_FILE_DELETE_METHODS_NOT_ALLOWED,
                "Cannot use both onFileDelete and onFileDeleted methods. " +
                "Use only onFileDelete as onFileDeleted is deprecated.");
    }

    @Test(description = "Validation when onFileDelete has no parameters")
    public void testInvalidContentService27() {
        Package currentPackage = loadPackage("invalid_content_service_27");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MANDATORY_PARAMETER_NOT_FOUND,
                "Mandatory parameter missing for onFileDelete. Expected string.");
    }

    // ==================== onError Handler Tests ====================

    @Test(description = "Validation with valid onError handler (Error only)")
    public void testValidOnErrorService1() {
        Package currentPackage = loadPackage("valid_on_error_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with valid onError handler (Error and Caller)")
    public void testValidOnErrorService2() {
        Package currentPackage = loadPackage("valid_on_error_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with valid onError handler (error)")
    public void testValidOnErrorService3() {
        Package currentPackage = loadPackage("valid_on_error_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation with valid onError handler (ftp:ContentBindingError subtype)")
    public void testValidOnErrorService4() {
        Package currentPackage = loadPackage("valid_on_error_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation when onError method is not remote")
    public void testInvalidOnErrorService1() {
        Package currentPackage = loadPackage("invalid_on_error_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ON_ERROR_MUST_BE_REMOTE,
                "onError method must be remote.");
    }

    @Test(description = "Validation when onError has invalid second parameter (not Caller)")
    public void testInvalidOnErrorService3() {
        Package currentPackage = loadPackage("invalid_on_error_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_ERROR_SECOND_PARAMETER,
                "Invalid second parameter for onError. Second parameter must be ftp:Caller.");
    }

    @Test(description = "Validation when onError has too many parameters")
    public void testInvalidOnErrorService4() {
        Package currentPackage = loadPackage("invalid_on_error_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS_ON_ERROR,
                "Too many parameters for onError. Accepts at most 2 parameters: " +
                "(error, caller?).");
    }

}
