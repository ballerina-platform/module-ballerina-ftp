import ballerina/test;

@test:Config { groups: ["ftpsValidation"] }
function testListenerFtpsWithPrivateKey() {
    // Hits validateServerAuthProtocolCombination: FTPS + PrivateKey
    Listener|error resultListener = new ({
        protocol: FTPS,
        host: "127.0.0.1",
        port: 21214,
        auth: {
            privateKey: { path: "tests/resources/id_rsa" }
        },
        path: "/ftps-listener"
    });

    if resultListener is error {
        test:assertTrue(resultListener.message().includes("privateKey can only be used with SFTP"), 
            "Failed to catch invalid auth combination in Listener");
    } else {
        test:assertFail("Listener init should have failed with FTPS + PrivateKey");
    }
}

@test:Config { groups: ["ftpsValidation"] }
function testClientFtpsWithPrivateKey() {
    // Hits FtpClient.validateAuthProtocolCombination
    ClientConfiguration invalidConfig = {
        protocol: FTPS,
        host: "127.0.0.1",
        auth: {
            privateKey: { path: "tests/resources/id_rsa" }
        }
    };
    
    Client|error clientEp = new (invalidConfig);
    if clientEp is error {
        test:assertTrue(clientEp.message().includes("privateKey can only be used with SFTP"), 
            "Failed to catch invalid auth combination in Client");
    } else {
        test:assertFail("Client init should have failed with FTPS + PrivateKey");
    }
}

@test:Config { groups: ["ftpsValidation"] }
function testClientSftpWithSecureSocket() {
    // Hits FtpClient.validateAuthProtocolCombination reverse case
    ClientConfiguration invalidConfig = {
        protocol: SFTP,
        host: "127.0.0.1",
        auth: {
            secureSocket: {
                key: { path: "tests/resources/keystore.jks", password: "changeit" }
            }
        }
    };
    
    Client|error clientEp = new (invalidConfig);
    if clientEp is error {
        test:assertTrue(clientEp.message().includes("secureSocket can only be used with FTPS"), 
            "Failed to catch invalid auth combination (SFTP + SecureSocket)");
    } else {
        test:assertFail("Client init should have failed with SFTP + SecureSocket");
    }
}