import ballerinax/java;
import ballerina/io;

public function initEndpoint(Client clientEndpoint, map<anydata> config) returns error? = @java:Method {
    name: "initEndpoint",
    class: "org.ballerinalang.ftp.client.endpoint.InitEndpoint"
} external;

public function get(Client clientEndpoint, handle path) returns io:ReadableByteChannel|error = @java:Method{
    name: "get",
    class: "org.ballerinalang.ftp.client.actions.Get"
} external;

public function delete(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "delete",
    class: "org.ballerinalang.ftp.client.actions.Delete"
} external;

public function put(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "put",
    class: "org.ballerinalang.ftp.client.actions.Put"
} external;

public function append(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "append",
    class: "org.ballerinalang.ftp.client.actions.Append"
} external;

public function mkdir(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "mkdir",
    class: "org.ballerinalang.ftp.client.actions.Mkdir"
} external;

public function rmdir(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "rmdir",
    class: "org.ballerinalang.ftp.client.actions.Rmdir"
} external;

public function rename(Client clientEndpoint, handle origin, handle destination) returns error? = @java:Method{
    name: "rename",
    class: "org.ballerinalang.ftp.client.actions.Rename"
} external;

public function size(Client clientEndpoint, handle path) returns int|error = @java:Method{
    name: "size",
    class: "org.ballerinalang.ftp.client.actions.Size"
} external;

public function list(Client clientEndpoint, handle path) returns string[]|error = @java:Method{
    name: "list",
    class: "org.ballerinalang.ftp.client.actions.List"
} external;

public function isDirectory(Client clientEndpoint, handle path) returns boolean|error = @java:Method{
    name: "isDirectory",
    class: "org.ballerinalang.ftp.client.actions.IsDirectory"
} external;

