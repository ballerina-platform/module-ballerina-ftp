import ballerinax/java;
import ballerina/io;

public function initEndpoint(Client clientEndpoint, map<anydata> config) returns error? = @java:Method {
    name: "initEndpoint",
    class: "org.ballerinalang.ftp.client.endpoint.InitEndpoint"
} external;

public function get(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "get",
    class: "org.ballerinalang.ftp.client.actions.Get"
} external;

public function append(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "append",
    class: "org.ballerinalang.ftp.client.actions.Append"
} external;

public function delete(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "delete",
    class: "org.ballerinalang.ftp.client.actions.Delete"
} external;

public function put(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "put",
    class: "org.ballerinalang.ftp.client.actions.Put"
} external;

public function isDirectory(Client clientEndpoint, handle path) returns boolean|error = @java:Method{
    name: "isDirectory",
    class: "org.ballerinalang.ftp.client.actions.IsDirectory"
} external;
