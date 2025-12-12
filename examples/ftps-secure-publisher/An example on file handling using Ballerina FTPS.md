# An Example on File Handling Using Ballerina FTPS

[![Star on Github](https://img.shields.io/badge/-Star%20on%20Github-blue?style=social&logo=github)](https://github.com/ballerina-platform/module-ballerina-ftp)

_Authors_: WSO2 Inc.  
_Created_: 2024/12/11  
_Updated_: 2024/12/11

## Overview

This guide explains how to listen to remotely located files over FTPS (FTP over SSL/TLS), receive them securely, process their content, and handle file events using Ballerina.

This example demonstrates:
- Setting up an FTPS server with SSL/TLS encryption
- Configuring a Ballerina FTPS listener to monitor remote directories
- Processing CSV files securely over FTPS connections
- Handling file change events (additions and deletions)

The figure below illustrates the high-level design diagram of the system.

```
┌─────────────┐         FTPS (SSL/TLS)        ┌──────────────┐
│   FTPS      │◄──────────────────────────────►│  Ballerina   │
│   Server    │                                 │  Processor   │
│             │                                 │              │
│  Port:      │                                 │  Listener:   │
│  21214      │                                 │  - onFileCsv │
│  (EXPLICIT) │                                 │  - onFileChange│
└─────────────┘                                 └──────────────┘
```

## Implementation

The implementation follows these steps:

1. **FTPS Server Setup**: An FTPS server is started with SSL/TLS encryption configured
2. **Listener Initialization**: The Ballerina FTPS listener connects to the server and monitors for new files
3. **File Detection**: When a CSV file is detected in the monitored directory, the listener triggers
4. **File Processing**: The file is fetched securely and processed row by row
5. **Event Handling**: File change events (additions/deletions) are logged and handled

## Execution Order

### Step 1 - Start the FTPS Server

Navigate to the FTPS server directory and start it:

```bash
cd examples/ftps-secure-publisher/ftps-server
./gradlew run
```

The server will start in EXPLICIT mode by default (port 21214). For IMPLICIT mode:

```bash
./gradlew run --args="IMPLICIT"
```

The successful start should show:
```shell
INFO: Started FTPS server in EXPLICIT mode on port 21214
```

### Step 2 - Start the Processor

In a separate terminal, navigate to the processor directory:

```bash
cd examples/ftps-secure-publisher/processor
bal run
```

The successful execution should show:
```shell
Initialized the FTPS processor job.
```

### Step 3 - Add Files for Processing

Copy a CSV file to the server's input directory to trigger processing:

```bash
cp examples/ftps-secure-publisher/resources/files/owid-covid-data.csv \
   examples/ftps-secure-publisher/ftps-server/src/main/resources/input/
```

The FTPS listener will:
1. Detect the new file matching the pattern `(.*).csv`
2. Fetch it securely over FTPS
3. Process each row in the `onFileCsv` handler
4. Log processing progress

## Configuration Details

### FTPS Listener Configuration

```ballerina
listener ftp:Listener secureRemoteServer = check new(
    protocol = ftp:FTPS,
    host = "localhost",
    auth = authConfig,
    port = 21214,  // EXPLICIT mode port
    pollingInterval = 2,
    fileNamePattern = "(.*).csv",
    laxDataBinding = true
);
```

### Secure Socket Configuration

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
        mode: ftp:EXPLICIT,  // Can be changed to ftp:IMPLICIT
        dataChannelProtection: ftp:PRIVATE
    }
};
```

### Service Implementation

The service implements two remote functions:

1. **onFileCsv**: Processes CSV files row by row
   - Receives a stream of CSV rows
   - Filters and processes valid rows
   - Logs progress and sample data

2. **onFileChange**: Handles file change events
   - Receives WatchEvent with added/deleted files
   - Logs file change information

## FTPS Modes Explained

### EXPLICIT Mode (Default)
- Connection starts as regular FTP
- Client sends `AUTH TLS` command to upgrade to SSL/TLS
- Control channel is encrypted after AUTH
- Data channel encryption depends on `dataChannelProtection` setting
- More flexible, allows fallback

### IMPLICIT Mode
- SSL/TLS connection established immediately
- All communication encrypted from the start
- Typically uses port 990
- Legacy mode, less commonly used

## Data Channel Protection

The `dataChannelProtection` setting controls whether file transfers are encrypted:

- **PRIVATE** (default): Data channel encrypted - Recommended for security
- **CLEAR**: Data channel not encrypted - Not recommended
- **SAFE**: Integrity protection only - Rarely used
- **CONFIDENTIAL**: Data channel encrypted - Similar to PRIVATE

## Security Features

1. **SSL/TLS Encryption**: All control channel communication is encrypted
2. **Certificate Validation**: Server certificate is validated using truststore
3. **Optional Client Certificates**: Can be configured for mutual TLS authentication
4. **Data Channel Protection**: File transfers can be encrypted
5. **Secure Authentication**: Credentials are transmitted over encrypted connection

## Testing

### Manual Testing

1. Start the FTPS server
2. Start the Ballerina processor
3. Add a CSV file to the server's input directory
4. Observe the processor logs for file processing

### Automated Testing

Run the comprehensive test suite:

```bash
cd module-ballerina-ftp
bal test --tests ftps_test
```

## Troubleshooting

### Server Won't Start
- Check if port 21214 (or 990 for IMPLICIT) is already in use
- Verify keystore.jks exists in `ftps-server/src/main/resources/`
- Check keystore password is correct ("changeit")

### Listener Can't Connect
- Verify server is running
- Check host and port configuration
- Verify truststore contains server certificate
- Check credentials (username: "wso2", password: "wso2123")

### Files Not Detected
- Verify file matches `fileNamePattern` (e.g., `(.*).csv`)
- Check file is in the correct directory
- Ensure polling interval allows time for detection
- Check file permissions

### SSL/TLS Errors
- Verify keystore and truststore paths are correct
- Check certificate validity dates
- Ensure truststore contains the server's certificate
- Verify passwords match configuration

## Additional Notes

- The example uses pre-generated keystores for simplicity
- In production, use proper certificate management
- Change default passwords before deployment
- Consider using certificate authorities (CAs) for production certificates
- Monitor certificate expiration dates

## Related Examples

- [COVID-19 Statistics Publisher](../covid19-stat-publisher/) - Similar example using SFTP
- [FTP Module Documentation](../../README.md) - Complete API documentation

## License

This example is licensed under the Apache License 2.0.
