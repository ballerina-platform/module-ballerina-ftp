package ballerina.net.fs;

@Field {value:"name: Absolute file URI for triggerd event"}
@Field {value:"operation: Triggered event action. This can be create, delete or modify"}
public struct FileEvent {
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
