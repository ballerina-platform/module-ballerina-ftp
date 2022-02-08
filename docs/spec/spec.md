# Specification: Ballerina FTP Library

_Owners_: @shafreenAnfar @dilanSachi @Bhashinee    
_Reviewers_: @shafreenAnfar @Bhashinee  
_Created_: 2020/10/28   
_Updated_: 2022/02/07  
_Edition_: Swan Lake    
_Issue_: [#2202](https://github.com/ballerina-platform/ballerina-standard-library/issues/2202)

# Introduction
This is the specification for the FTP standard library of [Ballerina language](https://ballerina.io/), which provides FTP client/listener functionalities to send and receive files by connecting to a FTP/SFTP server.

The FTP library specification has evolved and may continue to evolve in the future. Released versions of the specification can be found under the relevant Github tag.

If you have any feedback or suggestions about the library, start a discussion via a Github issue or in the [Slack channel](https://ballerina.io/community/). Based on the outcome specification and implementation can be updated. Community feedback is always welcome. Any accepted proposal which affects the specification is stored under `/docs/proposals`. Proposals under discussion can be found with the label `type/proposal` in Github.

Conforming implementation of the specification is released and included in the distribution. Any deviation from the specification is considered a bug.

# Contents
1. [Overview](#1-overview)
2. [Configurations](#2-security-configurations)
3. [Client](#3-client)
    *  3.1. [Configurations](#31-configurations)
    *  3.2. [Initialization](#32-initialization)
        *    3.2.1. [Insecure Client](#321-insecure-client)
        *    3.2.2. [Secure Client](#322-secure-client)
    *  3.3. [Functions](#33-functions)
4. [Listener](#4-listener)
    *  4.1. [Configurations](#41-configurations)
    *  4.2. [Initialization](#42-initialization)
        *  4.2.1. [Insecure Listener](#421-insecure-listener)
        *  4.2.2. [Secure Listener](#422-secure-listener)
5. [Samples](#5-samples)
    *  5.1. [Sending Files](#51-sending-files)
    *  5.2. [Listening to File Changes](#52-listening-to-file-changes)

## 1. Overview
FTP is the traditional file transfer protocol. It’s a basic way of using the Internet to share files.
SFTP (or Secure File Transfer Protocol) is an alternative to FTP that also allows transferring files,
but adds a layer of security to the process. SFTP uses SSH (or secure shell) encryption to protect data as
it’s being transferred. This means data is not exposed to outside entities on the Internet when it is sent
to another party.

Ballerina FTP library contains two core apis:
* Client - The `ftp:Client` is used to connect to an FTP server and perform various operations on the files.
* Listener - The `ftp:Listener` is used to listen to a remote FTP location and notify if files are added or removed 
  from the FTP location.

## 2. Security Configurations
* PrivateKey record represents the configuration related to a private key.
```ballerina
public type PrivateKey record {|
    # Path to the private key file
    string path;
    # Private key password
    string password?;
|};
```
* Credentials record represents the username and password configurations.
```ballerina
public type Credentials record {|
    # Username of the user
    string username;
    # Password of the user
    string password;
|};
```
* AuthConfiguration record represents the configurations needed for facilitating secure communication with a remote FTP server.
```ballerina
public type AuthConfiguration record {|
    # Username and password to be used
    Credentials credentials?;
    # Private key to be used
    PrivateKey privateKey?;
|};
```
## 3. Client
The `ftp:Client` connects to an FTP server and performs various operations on the files. Currently, it supports the
generic FTP operations; `get`, `delete`, `put`, `append`, `mkdir`, `rmdir`, `isDirectory`, `rename`, `size`, and
`list`. An FTP client is defined using the `protocol` and `host` parameters and optionally, the `port` and `auth`.
Authentication configuration can be configured using the `auth` parameter for Basic Auth and
private key.
### 3.1. Configurations
* When initializing the `ftp:Client`, following configurations can be provided.
```ballerina
public type ClientConfiguration record {|
    # Supported FTP protocols
    Protocol protocol = FTP;
    # Target service URL
    string host = "127.0.0.1";
    # Port number of the remote service
    int port = 21;
    # Authentication options
    AuthConfiguration auth?;
|};
```
* InputContent record represents the configurations for the input given for `put` and `append` operations.
```ballerina
public type InputContent record{|
    # Path of the file to be created or appended
    string filePath;
    # `true` if the input type is a file
    boolean isFile = false;
    # The content read from the input file, if the input is a file
    stream<byte[] & readonly, io:Error?> fileContent?;
    # The input content, for other input types
    string textContent?;
    # If true, input will be compressed before uploading
    boolean compressInput = false;
|};
```
* Following Compression options can be used when adding a file to the FTP server. 
```ballerina
public enum Compression {
    # Zip compression
    ZIP,
    # No compression used
    NONE
}
```
### 3.2. Initialization
#### 3.2.1. Insecure Client
A simple insecure client can be initialized by providing `ftp:FTP` as the protocol and the host and port to the `ftp:ClientConfiguration`.
```ballerina
# Gets invoked during object initialization.
#
# + clientConfig - Configurations for FTP client
# + return - `ftp:Error` in case of errors or `()` otherwise
public isolated function init(ClientConfiguration clientConfig) returns Error?;
```
#### 3.2.2. Secure Client
A secure client can be initialized by providing `ftp:SFTP` as the protocol and by providing `ftp:Credentials`
and `ftp:PrivateKey` to `ftp:AuthConfiguration`.
```ballerina
// Provide secureSocket configuration to ProducerConfiguration
ftp:ClientConfiguration ftpConfig = {
    protocol: ftp:SFTP,
    host: "<The FTP host>",
    port: <The FTP port>,
    auth: {
        credentials: {
            username: "<The FTP username>",
            password: "<The FTP passowrd>"
        }
    }
};
```
### 3.3. Functions
* FTP Client API can be used to put files on the FTP server. For this, the `put()` method can be used.
```ballerina
# Adds a file to an FTP server.
# ```ballerina
# ftp:Error? response = client->put(path, channel);
# ```
#
# + path - The resource path
# + content - Content to be written to the file in server
# + compressionType - Type of the compression to be used, if
#                     the file should be compressed before
#                     uploading
# + return - `()` or else an `ftp:Error` if failed to establish
#            the communication with the FTP server
remote isolated function put(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content, Compression compressionType=NONE) returns Error?;
```
* `append()` can be used to append the content to an existing file in an FTP server.
```ballerina
# Appends the content to an existing file in an FTP server.
# ```ballerina
# ftp:Error? response = client->append(path, channel);
# ```
#
# + path - The resource path
# + content - Content to be written to the file in server
# + return - `()` or else an `ftp:Error` if failed to establish
#            the communication with the FTP server
remote isolated function append(string path, stream<byte[] & readonly, io:Error?>|string|xml|json content) returns Error?;
```
* To retrieve file content from an FTP server, `get()` can be used.
```ballerina
# Retrieves the file content from a remote resource.
# ```ballerina
# stream<byte[] & readonly, io:Error?>|ftp:Error channel
#   = client->get(path);
# ```
#
# + path - The resource path
# + return - A byte stream from which the file can be read or `ftp:Error` in case of errors
remote isolated function get(string path) returns stream<byte[] & readonly, io:Error?>|Error;
```
* `mkdir()` can be used to create a new directory in an FTP server.
```ballerina
# Creates a new directory in an FTP server.
# ```ballerina
# ftp:Error? response = client->mkdir(path);
# ```
#
# + path - The directory path
# + return - `()` or else an `ftp:Error` if failed to establish
#            the communication with the FTP server
remote isolated function mkdir(string path) returns Error?;
```
* `rmdir()` can be used to remove an empty directory in an FTP server.
```ballerina
# Deletes an empty directory in an FTP server.
# ```ballerina
# ftp:Error? response = client->rmdir(path);
# ```
#
# + path - The directory path
# + return - `()` or else an `ftp:Error` if failed to establish
#            the communication with the FTP server
remote isolated function rmdir(string path) returns Error?;
```
* To delete a file, `delete()` can be used.
```ballerina
# Deletes a file from an FTP server.
# ```ballerina
# ftp:Error? response = client->delete(path);
# ```
#
# + path - The resource path
# + return -  `()` or else an `ftp:Error` if failed to establish
#             the communication with the FTP server
remote isolated function delete(string path) returns Error?;
```
* To rename a file or move it to another directory, `rename()` can be used.
```ballerina
# Renames a file or moves it to a new location within the same FTP server.
# ```ballerina
# ftp:Error? response = client->rename(origin, destination);
# ```
#
# + origin - The source file location
# + destination - The destination file location
# + return - `()` or else an `ftp:Error` if failed to establish
#            the communication with the FTP server
remote isolated function rename(string origin, string destination) returns Error?;
```
* `size()` function can be used to get the size of a file.
```ballerina
# Gets the size of a file resource.
# ```ballerina
# int|ftp:Error response = client->size(path);
# ```
#
# + path - The resource path
# + return - The file size in bytes or an `ftp:Error` if
#            failed to establish the communication with the FTP server
remote isolated function size(string path) returns int|Error;
```
* To get the file list in a directory, `list()` can be used.
```ballerina
# Gets the file name list in a given folder.
# ```ballerina
# ftp:FileInfo[]|ftp:Error response = client->list(path);
# ```
#
# + path - The direcotry path
# + return - An array of file names or an `ftp:Error` if failed to
#            establish the communication with the FTP server
remote isolated function list(string path) returns FileInfo[]|Error;
```
* To check if a resource is a directory, `isDirectory()` can be used.
```ballerina
# Checks if a given resource is a directory.
# ```ballerina
# boolean|ftp:Error response = client->isDirectory(path);
# ```
#
# + path - The resource path
# + return - `true` if given resource is a directory or an `ftp:Error` if
#            an error occurred while checking the path
remote isolated function isDirectory(string path) returns boolean|Error;
```
## 4. Listener
The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new
files are added to or deleted from the directory. The `fileResource` function is invoked when a new file is added
and/or deleted.
### 4.1. Configurations
* When initializing the `ftp:Listener`, following configurations can be provided.
```ballerina
public type ListenerConfiguration record {|
    # Supported FTP protocols
    Protocol protocol = FTP;
    # Target service url
    string host = "127.0.0.1";
    # Port number of the remote service
    int port = 21;
    # Authentication options
    AuthConfiguration auth?;
    # Remote FTP directory location
    string path = "/";
    # File name pattern that event need to trigger
    string fileNamePattern?;
    # Periodic time interval to check new update
    decimal pollingInterval = 60;
|};
```
* `WatchEvent` record represents the latest status change of the server from the last status change.
```ballerina
public type WatchEvent readonly & record {|
    # Array of `ftp:FileInfo` that represents newly added files
    FileInfo[] addedFiles;
    # Array of strings that contains deleted file names
    string[] deletedFiles;
|};
```
* `FileInfo` record contains the metadata for the newly-added files.
```ballerina
public type FileInfo record {|
    # Relative file path for a newly-added file
    string path;
    # Size of the file
    int size;
    # Last-modified timestamp of the file in UNIX Epoch time
    int lastModifiedTimestamp;
    # File name
    string name;
    # `true` if the file is a folder
    boolean isFolder;
    # `true` if the file is a file
    boolean isFile;
    # Normalized absolute path of this file within its file system
    string pathDecoded;
    # Extension of the file name
    string extension;
    # Receiver as a URI String for public display
    string publicURIString;
    # Type of the file
    string fileType;
    # `true` if the `fileObject` is attached
    boolean isAttached;
    # `true` if someone reads/writes from/to this file
    boolean isContentOpen;
    # `true` if this file is executable
    boolean isExecutable;
    # `true` if this file is hidden
    boolean isHidden;
    # `true` if this file can be read
    boolean isReadable;
    # `true` if this file can be written
    boolean isWritable;
    # Depth of the file name within its file system
    int depth;
    # URI scheme of the file
    string scheme;
    # Absolute URI of the file
    string uri;
    # Root URI of the file system in which the file exists
    string rootURI;
    # A "friendly path" is a path, which can be accessed without a password
    string friendlyURI;
|};
```
### 4.2. Initialization
#### 4.2.1. Insecure Listener
An insecure FTP listener can be initialized using the mandatory `protocol`, `host`, and  `path` parameters and 
the polling interval can be configured using the `pollingInterval` parameter. The default polling interval is 
60 seconds. The `fileNamePattern` parameter can be used to define the type of files the FTP listener will listen to.
For instance, if the listener gets invoked for text files, the value `(.*).txt` can be given for the config.
```ballerina
# Gets invoked during object initialization.
#
# + listenerConfig - Configurations for FTP listener
# + return - `()` or else an `ftp:Error` upon failure to initialize the listener
public isolated function init(ListenerConfiguration listenerConfig) returns Error?;
```
#### 4.2.2. Secure Listener
A secure client can be initialized by providing either `ftp:SFTP` as the protocol and by providing `ftp:Credentials`
and `ftp:PrivateKey` to `ftp:AuthConfiguration`.
```ballerina
ftp:ListenerConfiguration ftpConfig = {
    protocol: ftp:SFTP,
    host: "<The SFTP host>",
    port: <The SFTP port>,
    path: "<The remote SFTP directory location>",
    pollingInterval: <Polling interval>,
    fileNamePattern: "<File name pattern>",
    auth: {
        credentials: {username: "<The SFTP username>", password: "<The SFTP password>"},
        privateKey: {
            path: "<The private key file path>",
            password: "<The private key file password>"
        }
    }
};
```
#### 4.3. Usage
After initializing the listener, a service must be attached to the listener. There are two ways for this.
1. Attach the service to the listener directly.
```ballerina
service ftp:Service on ftpListener {
    function onFileChange(ftp:WatchEvent event) {
        // process event
    }
}
```
2. Attach the service dynamically.
```ballerina
// Create a service object
ftp:Service ftpListener = service object {
    function onFileChange(ftp:WatchEvent event) {
        // process event
    }
};
```
The function `onFileChange()` is invoked when the listener notices a file change in the FTP server.

The Listener has following functions to manage a service.
* `attach()` - can be used to bind a service to the `ftp:Listener`.
```ballerina
# Binds a service to the `ftp:Listener`.
# ```ballerina
# error? response = listener.attach(service1);
# ```
#
# + ftpService - Service to be detached from the listener
# + name - Name of the service to be detached from the listener
# + return - `()` or else an `error` upon failure to register the listener
public isolated function attach(Service ftpService, string[]|string? name = ()) returns error?;
```
* `detach()` - can be used to detach a service from the `ftp:Listener`.
```ballerina
# Stops consuming messages and detaches the service from the `ftp:Listener`.
# ```ballerina
# error? response = listener.detach(service1);
# ```
#
# + ftpService - Service to be detached from the listener
# + return - `()` or else an `error` upon failure to detach the service
public isolated function detach(Service ftpService) returns error?;
```
* `start()` - needs to be called to start the listener.
```ballerina
# Starts the `ftp:Listener`.
# ```ballerina
# error? response = listener.'start();
# ```
#
# + return - `()` or else an `error` upon failure to start the listener
public isolated function 'start() returns error?;
```
* `gracefulStop()` - can be used to gracefully stop the listener.
```ballerina
# Stops the `ftp:Listener` gracefully.
# ```ballerina
# error? response = listener.gracefulStop();
# ```
#
# + return - `()` or else an `error` upon failure to stop the listener
public isolated function gracefulStop() returns error?;
```
* `immediateStop()` - can be used to forcefully stop the listener.
```ballerina
# Stops the `ftp:Listener` forcefully.
# ```ballerina
# error? response = listener.immediateStop();
# ```
#
# + return - `()` or else an `error` upon failure to stop the listener
public isolated function immediateStop() returns error?;
```
* poll()` - can be used to poll new files from the FTP server.
```ballerina
# Poll new files from a FTP server.
# ```ballerina
# error? response = listener.poll();
# ```
#
# + return - An `error` if failed to establish communication with the FTP
#            server
public isolated function poll() returns error?
```
* `register()` can be used to register an FTP service in an `ftp:listener`.
```ballerina
# Register a FTP service in an FTP listener server.
# ```ballerina
# error? response = listener.register(ftpService, name);
# ```
#
# + ftpService - The FTP service
# + name - Name of the FTP service
# + return - An `error` if failed to establish communication with the FTP
#            server
public isolated function register(Service ftpService, string? name) returns error?
```
## 5. Samples
### 5.1. Sending Files.
```ballerina
import ballerina/ftp;
import ballerina/io;
import ballerina/lang.'string as strings;

ftp:AuthConfiguration authConfig = {
    credentials: {username: "wso2", password: "wso2123"},
    privateKey: {
        path: "resources/keys/sftp.private.key",
        password: "password"
    }
};

ftp:ClientConfiguration sftpClientConfig = {
    protocol: ftp:SFTP,
    host: "ftp.example.com",
    port: 21,
    auth: authConfig
};

public function main() returns error? {
    ftp:Client clientEp = check new(sftpClientConfig);
    stream<byte[] & readonly, io:Error?> fileStream = check clientEp->get("/server/book.txt");
    check fileStream.forEach(isolated function(byte[] & readonly fileContent) {
        io:println("File content received: " + checkpanic strings:fromBytes(fileContent));
    });
    stream<io:Block, io:Error?> bStream = check io:fileReadBlocksAsStream("/local/logFile.txt", 1024);
    check clientEp->put("/server", bStream);
    check fileStream.close();
}
```
### 5.2. Listening to file changes.
```ballerina
import ballerina/ftp;
import ballerina/io;

listener ftp:Listener remoteServer = check new({
    protocol: ftp:FTP,
    host: "ftp.example.com",
    auth: {
        credentials: {
            username: "wso2",
            password: "wso2123"
        },
        privateKey: {
            path: "resources/keys/sftp.private.key",
            password: "password"
        }
    },
    port: 21,
    path: "/home/in",
    pollingInterval: 2,
    fileNamePattern: "(.*).txt"
});

service on remoteServer {

    remote function onFileChange(ftp:WatchEvent event) {
        foreach ftp:FileInfo addedFile in event.addedFiles {
            io:println("Added file path: " + addedFile.path);
        }

        foreach string deletedFile in event.deletedFiles {
            io:println("Deleted file path: " + deletedFile);
        }
    }
}
```
