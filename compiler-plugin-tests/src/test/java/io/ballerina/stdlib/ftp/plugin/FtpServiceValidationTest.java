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
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.METHOD_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MULTIPLE_CONTENT_METHODS;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.MUST_HAVE_WATCHEVENT;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.NO_VALID_REMOTE_METHOD;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_CHANGE_DEPRECATED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ON_FILE_DELETED_MUST_BE_REMOTE;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.ONLY_REMOTE_FUNCTION_ALLOWED;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors.TOO_MANY_PARAMETERS;
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
        assertDiagnostic(warning, ON_FILE_CHANGE_DEPRECATED);
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

    @Test(description = "Validation when no valid remote function is defined")
    public void testInvalidService1() {
        Package currentPackage = loadPackage("invalid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, NO_VALID_REMOTE_METHOD);
    }

    @Test(description = "Validation when 2 remote functions are defined")
    public void testInvalidService2() {
        Package currentPackage = loadPackage("invalid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_REMOTE_FUNCTION);
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
        assertDiagnostic(diagnostic1, ONLY_REMOTE_FUNCTION_ALLOWED);
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
        assertDiagnostic(diagnostic, ONLY_REMOTE_FUNCTION_ALLOWED);
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
        assertDiagnostic(diagnostic1, ONLY_REMOTE_FUNCTION_ALLOWED);
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
        assertDiagnostic(diagnostic1, ONLY_REMOTE_FUNCTION_ALLOWED);
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
                "Format-specific handler 'onFileText' must be declared as remote.");
    }

    @Test(description = "Validation when content method has invalid first parameter type")
    public void testInvalidContentService2() {
        Package currentPackage = loadPackage("invalid_content_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid first parameter type for onFileJson. Expected json or record type, found int.");
    }

    @Test(description = "Validation when content method has invalid fileInfo parameter")
    public void testInvalidContentService3() {
        Package currentPackage = loadPackage("invalid_content_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_FILEINFO_PARAMETER,
                "Invalid fileInfo parameter for onFileText. Only ftp:FileInfo is allowed.");
    }

    @Test(description = "Validation when content method defines too many parameters")
    public void testInvalidContentService4() {
        Package currentPackage = loadPackage("invalid_content_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS,
                "Too many parameters for 'onFileCsv'. Format-specific handlers accept at most 3 parameters: " +
                "(content, fileInfo?, caller?).");
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
        assertDiagnostic(diagnostic, MULTIPLE_CONTENT_METHODS);
    }

    @Test(description = "Validation when onFileDeleted method is not remote")
    public void testInvalidContentService7() {
        Package currentPackage = loadPackage("invalid_content_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ON_FILE_DELETED_MUST_BE_REMOTE);
    }

    @Test(description = "Validation when onFileDeleted has invalid deleted files parameter")
    public void testInvalidContentService8() {
        Package currentPackage = loadPackage("invalid_content_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETED_PARAMETER);
    }

    @Test(description = "Validation when onFileDeleted has invalid caller parameter")
    public void testInvalidContentService9() {
        Package currentPackage = loadPackage("invalid_content_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETED_CALLER_PARAMETER);
    }

    @Test(description = "Validation when onFileDeleted has too many parameters")
    public void testInvalidContentService10() {
        Package currentPackage = loadPackage("invalid_content_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, TOO_MANY_PARAMETERS_ON_FILE_DELETED);
    }

    @Test(description = "Validation when onFileJson has invalid return type")
    public void testInvalidContentService11() {
        Package currentPackage = loadPackage("invalid_content_service_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ERROR_OR_NIL);
    }

    @Test(description = "Validation when onFileCsv uses stream<byte[], error?> instead of stream<string[], error?>")
    public void testInvalidContentService12() {
        Package currentPackage = loadPackage("invalid_content_service_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid first parameter type for onFileCsv. Expected string[][], record{}[], " +
                        "or stream<string[], error?>, found stream<byte[], error?>.");
    }

    @Test(description = "Validation when onFileCsv uses stream<int[], error?> instead of stream<string[], error?>")
    public void testInvalidContentService13() {
        Package currentPackage = loadPackage("invalid_content_service_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid first parameter type for onFileCsv. Expected string[][], record{}[], " +
                        "or stream<string[], error?>, found stream<int[], error?>.");
    }

    @Test(description = "Validation when a content handler omits the required content parameter")
    public void testInvalidContentService14() {
        Package currentPackage = loadPackage("invalid_content_service_14");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid first parameter type for onFileText. Expected string, found none.");
    }

    @Test(description = "Validation when a three-parameter content handler uses an invalid fileInfo parameter")
    public void testInvalidContentService15() {
        Package currentPackage = loadPackage("invalid_content_service_15");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_FILEINFO_PARAMETER,
                "Invalid fileInfo parameter for onFileText. Only ftp:FileInfo is allowed.");
    }

    @Test(description = "Validation when onFileDeleted does not define any parameters")
    public void testInvalidContentService16() {
        Package currentPackage = loadPackage("invalid_content_service_16");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_ON_FILE_DELETED_PARAMETER);
    }

    @Test(description = "Validation when onFileChange and onFileDeleted are mixed in the same service")
    public void testInvalidContentService17() {
        Package currentPackage = loadPackage("invalid_content_service_17");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MULTIPLE_CONTENT_METHODS);
    }

    @Test(description = "Validation when onFileCsv uses stream<record[], error?> with valid record type")
    public void testValidContentService7() {
        Package currentPackage = loadPackage("valid_content_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test(description = "Validation when onFileCsv uses stream<record[], error?> with invalid stream item type")
    public void testInvalidContentService18() {
        Package currentPackage = loadPackage("invalid_content_service_18");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_CONTENT_PARAMETER_TYPE,
                "Invalid parameter type for format-specific handler 'onFileCsv'. " +
                "Expected string[][], record type array, stream<string[], error?>, or stream<record[], error?>, " +
                "found stream<int[], error?>.");
    }
}
