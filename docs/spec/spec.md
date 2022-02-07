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
4. [Listener](#4-consumer)
    *  4.1. [Configurations](#41-configurations)
    *  4.2. [Consumer Client](#42-consumer-client)
        *  4.2.1. [Initialization](#421-initialization)
            *  4.2.1.1. [Insecure Client](#4211-insecure-client)
            *  4.2.1.2. [Secure Client](#4212-secure-client)
        *  4.2.2. [Consume Messages](#422-consume-messages)
        *  4.2.3. [Handle Offsets](#423-handle-offsets)
        *  4.2.4. [Handle Partitions](#424-handle-partitions)
        *  4.2.5. [Seeking](#425-seeking)
        *  4.2.6. [Handle subscriptions](#426-handle-subscriptions)
    *  4.3. [Listener](#43-listener)
        *  4.3.1. [Initialization](#431-initialization)
            *  4.3.1.1. [Insecure Listener](#4311-insecure-listener)
            *  4.3.1.2. [Secure Listener](#4312-secure-listener)
        *  4.3.2. [Usage](#432-usage)
        *  4.3.3. [Caller](#433-caller)
5. [Samples](#5-samples)
    *  5.1. [Produce Messages](#51-produce-messages)
    *  5.2. [Consume Messages](#52-consume-messages)
        *    5.2.1. [Using Consumer Client](#521-using-consumer-client)
        *    5.2.2. [Using Listener](#522-using-listener)

## 1. Overview
FTP is the traditional file transfer protocol. It’s a basic way of using the Internet to share files.
SFTP (or Secure File Transfer Protocol) is an alternative to FTP that also allows transferring files,
but adds a layer of security to the process. SFTP uses SSH (or secure shell) encryption to protect data as
it’s being transferred. This means data is not exposed to outside entities on the Internet when it is sent
to another party.

Ballerina FTP library contains two core apis:
* Client - The `ftp:Client` is used to connect to an FTP server and perform various operations on the files.
* Listener - The `ftp:Listener` is used to listen to a remote FTP location and trigger a `WatchEvent` type of event when new
  files are added to or deleted from the directory.

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
A secure client can be initialized by providing either `ftp:SFTP` as the protocol and by providing `ftp:Credentials` 
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
* 
## 4. Listener
The Consumer allows applications to read streams of data from topics in the Kafka cluster. Ballerina Kafka supports
two types of consumers, Consumer Client and Listener.
### 4.1. Configurations

