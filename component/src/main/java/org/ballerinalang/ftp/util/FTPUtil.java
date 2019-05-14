/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.ftp.util;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMErrors;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.BallerinaConnectorException;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;

import java.net.URI;
import java.net.URISyntaxException;

import static org.ballerinalang.ftp.util.FtpConstants.FTP_PACKAGE_NAME;

/**
 * Utils class for FTP client operations.
 */
public class FTPUtil {

    private static final String FTP_ERROR_CODE = "{wso2/ftp}FTPError";
    private static final String FTP_ERROR = "FTPError";
    public static boolean notValidProtocol(String url) {
        return !url.startsWith("ftp") && !url.startsWith("sftp") && !url.startsWith("ftps");
    }

    public static boolean validProtocol(String url) {
        return url.startsWith("ftp://") || url.startsWith("sftp://") || url.startsWith("ftps://");
    }

    public static String createUrl(String protocol, String host, int port, String username, String passPhrase,
            String basePath) {
        String userInfo = username + ":" + passPhrase;
        URI uri = null;
        try {
            uri = new URI(protocol, userInfo, host, port, basePath, null, null);
        } catch (URISyntaxException e) {
            throw new BallerinaConnectorException("Error occurred while constructing a URI from the given components: "
                    + e.getMessage(), e);
        }
        return uri.toString();
    }

    /**
     * Creates an error message.
     *
     * @param context context which is invoked.
     * @param errMsg  the cause for the error.
     * @return an error which will be propagated to ballerina user.
     */
    public static BError createError(Context context, String errMsg) {
        BMap<String, BValue> ftpErrorRecord = BLangConnectorSPIUtil.createBStruct(context, FTP_PACKAGE_NAME, FTP_ERROR);
        ftpErrorRecord.put("message", new BString(errMsg));
        return BLangVMErrors.createError(context, true, BTypes.typeError, FTP_ERROR_CODE, ftpErrorRecord);
    }
}
