package ballerina.net.fs;

public struct FileSystemEvent {
    string name;
    string operation;
}

@Description {value:"Move event triggered file to a given location"}
@Param {value:"fs: A file event"}
@Param {value:"destination: A new local file system location to move the file"}
public native function <FileSystemEvent fs> move (string destination);

@Description {value:"Delete event triggered file"}
@Param {value:"fs: A file event"}
public native function <FileSystemEvent fs> delete ();