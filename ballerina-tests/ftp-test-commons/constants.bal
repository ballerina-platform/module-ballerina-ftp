// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

// ─── Server host ──────────────────────────────────────────────────────────────

public const string FTP_HOST = "127.0.0.1";

// ─── Server ports ─────────────────────────────────────────────────────────────

public const int ANON_FTP_PORT = 21210;
public const int AUTH_FTP_PORT = 21212;
public const int SFTP_PORT = 21213;
public const int FTPS_EXPLICIT_PORT = 21214;
public const int SLOW_FTP_PORT = 21215;
public const int FTPS_IMPLICIT_PORT = 21217;
public const int FTPS_MISMATCHED_PORT = 21220;

// ─── FTP credentials ──────────────────────────────────────────────────────────

public const string FTP_USERNAME = "wso2";
public const string FTP_PASSWORD = "wso2123";
public const string ANON_USERNAME = "anonymous";
public const string ANON_PASSWORD = "anything";

// ─── Remote paths ─────────────────────────────────────────────────────────────

public const string HOME_IN = "/home/in";
public const string FTPS_ROOT = "/ftps-client";
public const string FTPS_LISTENER_ROOT = "/ftps-listener";
public const string SFTP_LISTENER_ROOT = "/sftp-listener";

// ─── Local resource paths ─────────────────────────────────────────────────────
// All paths are relative from each test-package directory under ballerina-tests/

public const string RESOURCES_PATH = "../resources";
public const string PUT_FILE_PATH = RESOURCES_PATH + "/datafiles/file2.txt";
public const string APPEND_FILE_PATH = RESOURCES_PATH + "/datafiles/file1.txt";
public const string JSON_FILE_PATH = RESOURCES_PATH + "/datafiles/test_data.json";
public const string XML_FILE_PATH = RESOURCES_PATH + "/datafiles/test_data.xml";
public const string CSV_FILE_PATH = RESOURCES_PATH + "/datafiles/test_data.csv";
public const string CSV_ERROR_FILE_PATH = RESOURCES_PATH + "/datafiles/test_data_with_error.csv";
public const string TEXT_FILE_PATH = RESOURCES_PATH + "/datafiles/test_text.txt";

// ─── SFTP key paths ───────────────────────────────────────────────────────────

public const string SFTP_PRIVATE_KEY_PATH = RESOURCES_PATH + "/sftp.private.key";
public const string SFTP_PASSWORDLESS_KEY_PATH = RESOURCES_PATH + "/sftp.passwordless.private.key";
public const string SFTP_WRONG_KEY_PATH = RESOURCES_PATH + "/sftp.wrong.private.key";

// ─── FTPS keystore paths ──────────────────────────────────────────────────────

public const string KEYSTORE_PATH = RESOURCES_PATH + "/keystore.jks";
public const string MISMATCHED_KEYSTORE_PATH = RESOURCES_PATH + "/mismatched-keystore.jks";
public const string SERVER_PEM_PATH = RESOURCES_PATH + "/server.pem";

// ─── Common error messages ────────────────────────────────────────────────────

public const string CLIENT_ALREADY_CLOSED_MSG = "FTP Client is already closed, hence further operations are not allowed";
