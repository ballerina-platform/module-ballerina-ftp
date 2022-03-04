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
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.ballerina.stdlib.ftp.plugin.CompilerPluginTestUtils.assertDiagnostic;
import static io.ballerina.stdlib.ftp.plugin.CompilerPluginTestUtils.loadPackage;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CompilationErrors;

/**
 * Tests for FTP package compiler plugin.
 */
public class FtpServiceValidationTest {

    @Test
    public void testValidService1() {
        Package currentPackage = loadPackage("valid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testValidService2() {
        Package currentPackage = loadPackage("valid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testValidService3() {
        Package currentPackage = loadPackage("valid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testValidService4() {
        Package currentPackage = loadPackage("valid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testValidService5() {
        Package currentPackage = loadPackage("valid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testInvalidService1() {
        Package currentPackage = loadPackage("invalid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CompilationErrors.NO_ON_FILE_CHANGE);
    }

    @Test
    public void testInvalidService2() {
        Package currentPackage = loadPackage("invalid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CompilationErrors.INVALID_REMOTE_FUNCTION);
    }

    @Test
    public void testInvalidService3() {
        Package currentPackage = loadPackage("invalid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CompilationErrors.METHOD_MUST_BE_REMOTE);
    }

    @Test
    public void testInvalidService4() {
        Package currentPackage = loadPackage("invalid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, CompilationErrors.NO_ON_FILE_CHANGE);
    }

    @Test
    public void testInvalidService5() {
        Package currentPackage = loadPackage("invalid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED);
    }

    @Test
    public void testInvalidService6() {
        Package currentPackage = loadPackage("invalid_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, CompilationErrors.INVALID_REMOTE_FUNCTION);
        Diagnostic diagnostic3 = (Diagnostic) diagnostics[2];
        assertDiagnostic(diagnostic3, CompilationErrors.NO_ON_FILE_CHANGE);
    }

    @Test
    public void testInvalidService7() {
        Package currentPackage = loadPackage("invalid_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();

        Diagnostic diagnostic1 = (Diagnostic) diagnostics[0];
        assertDiagnostic(diagnostic1, CompilationErrors.RESOURCE_FUNCTION_NOT_ALLOWED);
        Diagnostic diagnostic2 = (Diagnostic) diagnostics[1];
        assertDiagnostic(diagnostic2, CompilationErrors.INVALID_REMOTE_FUNCTION);
        Diagnostic diagnostic3 = (Diagnostic) diagnostics[2];
        assertDiagnostic(diagnostic3, CompilationErrors.METHOD_MUST_BE_REMOTE);
    }

    @Test
    public void testInvalidService8() {
        Package currentPackage = loadPackage("invalid_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, CompilationErrors.MUST_HAVE_WATCHEVENT);
    }

    @Test
    public void testInvalidService9() {
        Package currentPackage = loadPackage("invalid_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 5);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, CompilationErrors.ONLY_PARAMS_ALLOWED);
        }
    }

    @Test
    public void testInvalidService10() {
        Package currentPackage = loadPackage("invalid_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 4);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, CompilationErrors.INVALID_PARAMETER);
        }
    }

    @Test
    public void testInvalidService11() {
        Package currentPackage = loadPackage("invalid_service_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 6);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL);
        }
    }
}
