package ballerina.net.fs;

import ballerina.doc;

public struct FileSystemEvent {
    string name;
    string operation;
}

@doc:Description {value:"Move event triggered file to a given location"}
@doc:Param {value:"fs: A file event"}
@doc:Param {value:"destination: A new local file system location to move the file"}
public native function <FileSystemEvent fs> move (string destination);

@doc:Description {value:"Delete event triggered file"}
@doc:Param {value:"fs: A file event"}
public native function <FileSystemEvent fs> delete ();