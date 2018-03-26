package ballerina.net.fs;

@Field {value:"name: Absolute file URI for triggerd event"}
@Field {value:"operation: Triggered event action. This can be create, delete or modify"}
public struct FileSystemEvent {
    string name;
    string operation;
}

@Description {value:"Represents an error which will occur while FTP client operations"}
@Field {value:"message:  An error message explaining about the error"}
@Field {value:"cause: The error(s) that caused FTPClientError to get thrown"}
public struct FSError {
    string message;
    error[] cause;
}

@Description {value:"Move event triggered file to a given location"}
@Param {value:"fs: A file event"}
@Param {value:"destination: A new local file system location to move the file"}
@Return {value:"The Error occured during the file move"}
public native function <FileSystemEvent fs> move (string destination) returns FSError;

@Description {value:"Delete event triggered file"}
@Param {value:"fs: A file event"}
@Return {value:"The Error occured during file delete"}
public native function <FileSystemEvent fs> delete () returns FSError;
