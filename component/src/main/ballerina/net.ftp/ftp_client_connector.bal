package ballerina.net.ftp;

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

@Description {value:"Retrieves ByteChannel"}
@Param {value:"path: The file path to be read"}
@Return {value:"channel: A ByteChannel that represent the data source"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> get (string path) returns (io:ByteChannel|FTPClientError);

@Description {value:"Deletes a file from a given location"}
@Param {value:"path: File path that should be deleted"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> delete (string path) returns null | FTPClientError;

@Description {value:"Put a file using the given blob"}
@Param {value:"blob: Content to be written"}
@Param {value:"path: Destination path of the file"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> put (blob content, string path) returns null | FTPClientError;

@Description {value:"Append to a file using the given blob"}
@Param {value:"blob: Content to be written"}
@Param {value:"file: Destination path of the file"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> append (blob content, string path) returns null| FTPClientError;

@Description {value:"Create a directory in a given location"}
@Param {value:"path: Path that directory need to create"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> mkdir (string path) returns null | FTPClientError;

@Description {value:"Remove directory in a given location"}
@Param {value:"path: Path that directory need to remove"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> rmdir (string path) returns null | FTPClientError;

@Description {value:"Rename the existing file. This can also use to move file to a different location"}
@Param {value:"origin: Origin file path"}
@Param {value:"destination: Destination file path"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> rename (string origin, string destination) returns null| FTPClientError;

@Description {value:"Get the size of the given file"}
@Param {value:"path: File location"}
@Return {value:"Returns size of the given file"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> size (string path) returns int | FTPClientError;

@Description {value:"Get the list of folder/file names in the given location"}
@Param {value:"path: Directory location"}
@Return {value:"Returns size of the given file"}
@Return {value:"Error occured during FTP client invocation"}
public native function <ClientConnector client> list (string path) returns string[] | FTPClientError;
