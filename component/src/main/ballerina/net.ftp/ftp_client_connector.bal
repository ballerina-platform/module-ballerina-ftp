package ballerina.net.ftp;

import ballerina/file;
import ballerina/io;

@Description {value:"Represents an error which will occur while FTP client operations"}
@Field {value:"message:  An error message explaining about the error"}
@Field {value:"cause: The error(s) that caused FTPClientError to get thrown"}
public struct FTPClientError {
    string message;
    error[] cause;
}

@Description {value:"FTP client connector for outbound FTP file requests"}
@Field { value:"connectorOptions: connector options" }
public struct ClientConnector {
    ClientEndpointConfiguration config;
}

public function <ClientConnector conn> ClientConnector() {
}

@Description {value:"Retrieves blob value of a file"}
@Param {value:"file: The file to be read"}
@Return {value:"channel: A ByteChannel that represent the data source"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> read (file:File file) returns (io:ByteChannel| FTPClientError);

@Description {value:"Copies a file from a given location to another"}
@Param {value:"target: File/Directory that should be copied"}
@Param {value:"destination: Location where the File/Directory should be pasted"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> copy (file:File source, file:File destination) returns FTPClientError;

@Description {value:"Moves a file from a given location to another"}
@Param {value:"target: File/Directory that should be moved"}
@Param {value:"destination: Location where the File/Directory should be moved to"}
public native function <ClientConnector client> move (file:File target, file:File destination) returns FTPClientError;

@Description {value:"Deletes a file from a given location"}
@Param {value:"target: File/Directory that should be deleted"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> delete (file:File target) returns FTPClientError;

@Description {value:"Writes a file using the given blob"}
@Param {value:"blob: Content to be written"}
@Param {value:"file: Destination path of the file"}
@Param {value:"mode: Whether to append or overwrite the given content ['append' | 'a' or 'overwrite' | 'o']"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> write (blob content, file:File file, string mode) returns FTPClientError;

@Description {value:"Create a file or folder"}
@Param {value:"file: Path of the file"}
@Param {value:"isDir: Specify whether it is a file or a folder"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> createFile (file:File file, boolean isDir) returns FTPClientError;

@Description {value:"Checks the existence of a file"}
@Param {value:"file: File struct containing path information"}
@Return {value:"boolean: The availability of the file"}
public native function <ClientConnector client> exists (file:File file) returns (boolean | FTPClientError);

@Description {value:"Pipe the content to a file using the given ByteChannel"}
@Param {value:"channel: Content to be written"}
@Param {value:"file: Destination path of the file"}
@Param {value:"mode: Whether to append or overwrite the given content ['append' | 'a' or 'overwrite' | 'o']"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> pipe (io:ByteChannel channel, file:File file, string mode) returns FTPClientError;

