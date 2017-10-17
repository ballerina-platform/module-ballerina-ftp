/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.net.utils;

import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.codegen.ProgramFileReader;
import org.wso2.ballerinalang.programfile.ProgramFileWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Contains utility methods for executing a Ballerina program.
 *
 * @since 0.8.0
 */
public class LauncherUtils {

    /**
     * Get the executable program ({@link ProgramFile}) given the compiled program
     * ({@link org.wso2.ballerinalang.programfile.ProgramFile})
     *
     * @param programFile Compiled program
     * @return Executable program
     */
    public static ProgramFile getExecutableProgram(org.wso2.ballerinalang.programfile.ProgramFile programFile) {
        ByteArrayInputStream byteIS = null;
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try {
            ProgramFileWriter.writeProgram(programFile, byteOutStream);

            ProgramFileReader reader = new ProgramFileReader();
            byteIS = new ByteArrayInputStream(byteOutStream.toByteArray());
            return reader.readProgram(byteIS);
        } catch (Throwable e) {
        } finally {
            if (byteIS != null) {
                try {
                    byteIS.close();
                } catch (IOException ignore) {
                }
            }

            try {
                byteOutStream.close();
            } catch (IOException ignore) {
            }
        }
        return null;
    }
}
