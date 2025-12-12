# FTPS Secure Publisher Example

[![Star on Github](https://img.shields.io/badge/-Star%20on%20Github-blue?style=social&logo=github)](https://github.com/ballerina-platform/module-ballerina-ftp)

_Authors_: WSO2 Inc.  
_Created_: 2024/12/11  
_Updated_: 2024/12/11

## Overview

This guide explains how to use FTPS (FTP over SSL/TLS) to securely transfer files using Ballerina. This example demonstrates:

- Setting up an FTPS server with SSL/TLS encryption
- Configuring a Ballerina FTPS listener to monitor remote directories
- Processing files securely over FTPS connections
- Using both IMPLICIT and EXPLICIT FTPS modes
- Configuring data channel protection levels

FTPS provides secure file transfer by encrypting both the control channel (commands) and optionally the data channel (file transfers) using SSL/TLS certificates.

## Prerequisites

- Java 11 or higher
- Ballerina Swan Lake or later
- Gradle (for building the FTPS server)
- Keystore files (included in `resources/resources/keys/`)

## FTPS Modes

### IMPLICIT Mode
- SSL/TLS connection is established immediately upon connection
- Typically uses port 990
- All communication is encrypted from the start
- Legacy mode, less commonly used

### EXPLICIT Mode (Default)
- Starts as regular FTP, then upgrades to SSL/TLS using AUTH TLS command
- Typically uses port 21 (or custom port like 21214 in this example)
- More flexible, allows fallback to unencrypted if needed
- Recommended for most use cases

## Directory Structure

```
ftps-secure-publisher/
├── ftps-server/              # Java FTPS server implementation
│   ├── src/main/java/
│   │   └── FtpsServer.java    # FTPS server main class
│   └── src/main/resources/
│       ├── keystore.jks       # Server keystore
│       ├── datafiles/         # Server data directory
│       └── input/             # Input directory for file processing
├── processor/                 # Ballerina FTPS client/listener
│   ├── processor.bal          # Main Ballerina service
│   └── Ballerina.toml         # Ballerina package configuration
└── resources/
    ├── files/                 # Sample data files
    └── resources/keys/         # SSL/TLS certificates
        ├── server-keystore.jks
        ├── client-keystore.jks
        └── truststore.jks
```

## Execution Steps

### Step 1: Build the FTPS Server

Navigate to the FTPS server directory and build it:

```bash
cd examples/ftps-secure-publisher/ftps-server
./gradlew build
```

### Step 2: Start the FTPS Server

Start the server in EXPLICIT mode (default):

```bash
./gradlew run
```

Or start in IMPLICIT mode:

```bash
./gradlew run --args="IMPLICIT"
```

The server will start and display:
```
INFO: Started FTPS server in EXPLICIT mode on port 21214
```

### Step 3: Start the Ballerina Processor

In a new terminal, navigate to the processor directory:

```bash
cd examples/ftps-secure-publisher/processor
bal run
```

The processor will initialize and start listening for files:
```
Initialized the FTPS processor job.
```

### Step 4: Add Files for Processing

Copy a CSV file to the server's input directory:

```bash
cp examples/ftps-secure-publisher/resources/files/owid-covid-data.csv \
   examples/ftps-secure-publisher/ftps-server/src/main/resources/input/
```

The FTPS listener will detect the new file, fetch it securely, and process it.

## Configuration

### FTPS Listener Configuration

The processor uses the following FTPS configuration:

```ballerina
ftp:AuthConfiguration authConfig = {
    credentials: {username: "wso2", password: "wso2123"},
    secureSocket: {
        key: {
            path: "../resources/resources/keys/client-keystore.jks",
            password: "changeit"
        },
        trustStore: {
            path: "../resources/resources/keys/truststore.jks",
            password: "changeit"
        },
        mode: ftp:EXPLICIT,  // or ftp:IMPLICIT
        dataChannelProtection: ftp:PRIVATE
    }
};
```

### Data Channel Protection Levels

- **PRIVATE** (default): Data channel is encrypted (PROT P) - Recommended
- **CLEAR**: Data channel is not encrypted (PROT C) - Not recommended
- **SAFE**: Data channel has integrity protection only (PROT S) - Rarely used
- **CONFIDENTIAL**: Data channel is encrypted (PROT E) - Similar to PRIVATE

## Certificate Setup

The example includes pre-generated keystores for testing. For production use:

1. Generate a server certificate and keystore
2. Export the server certificate
3. Import it into the client truststore
4. Optionally, generate client certificates for mutual TLS authentication

### Generating Certificates (Optional)

```bash
# Generate server keystore
keytool -genkeypair -alias ftps-server -keyalg RSA -keysize 2048 \
  -keystore server-keystore.jks -validity 365 -storepass changeit

# Export server certificate
keytool -export -alias ftps-server -file server-cert.cer \
  -keystore server-keystore.jks -storepass changeit

# Import into client truststore
keytool -import -alias ftps-server -file server-cert.cer \
  -keystore truststore.jks -storepass changeit -noprompt
```

## Troubleshooting

### Connection Refused
- Ensure the FTPS server is running
- Check that the port (21214 for EXPLICIT, 990 for IMPLICIT) is not blocked
- Verify firewall settings

### SSL/TLS Handshake Failures
- Verify keystore paths are correct
- Check keystore passwords match configuration
- Ensure truststore contains the server certificate
- Check certificate validity dates

### File Not Detected
- Verify the file matches the `fileNamePattern` (e.g., `(.*).csv`)
- Check file permissions on the server
- Ensure polling interval is appropriate (default: 2 seconds)

### Authentication Failures
- Verify username and password match server configuration
- Check that credentials are correctly specified in `auth.credentials`

## Security Considerations

1. **Use Strong Passwords**: Change default keystore passwords in production
2. **Certificate Management**: Regularly rotate certificates and update truststores
3. **Data Channel Protection**: Always use PRIVATE or CONFIDENTIAL for secure transfers
4. **Network Security**: Use FTPS over trusted networks or VPNs
5. **Access Control**: Implement proper user authentication and authorization

## Testing

Run the comprehensive FTPS test suite:

```bash
cd module-ballerina-ftp
bal test --tests ftps_test
```

The test suite covers:
- EXPLICIT and IMPLICIT mode connections
- Data channel protection levels
- Client certificate authentication
- Listener file detection
- Negative test cases (error handling)

## Additional Resources

- [Ballerina FTP Module Documentation](../../README.md)
- [FTPS Protocol Specification](https://tools.ietf.org/html/rfc4217)
- [Apache FtpServer Documentation](https://mina.apache.org/ftpserver-project/)

## License

This example is licensed under the Apache License 2.0.
