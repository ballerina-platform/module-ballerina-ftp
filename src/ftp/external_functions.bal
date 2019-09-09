import ballerinax/java;
import ballerina/io;

public function initEndpoint(Client clientEndpoint, map<anydata> config) returns error? = @java:Method {
    name: "initClientEndpoint",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function get(Client clientEndpoint, handle path) returns io:ReadableByteChannel|error = @java:Method{
    name: "get",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function delete(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "delete",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function put(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "put",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function append(Client clientEndpoint, handle path, io:ReadableByteChannel byteChannel) returns error? = @java:Method{
    name: "append",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function mkdir(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "mkdir",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function rmdir(Client clientEndpoint, handle path) returns error? = @java:Method{
    name: "rmdir",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function rename(Client clientEndpoint, handle origin, handle destination) returns error? = @java:Method{
    name: "rename",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function size(Client clientEndpoint, handle path) returns int|error = @java:Method{
    name: "size",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function list(Client clientEndpoint, handle path) returns string[]|error = @java:Method{
    name: "list",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function isDirectory(Client clientEndpoint, handle path) returns boolean|error = @java:Method{
    name: "isDirectory",
    class: "org.wso2.ei.ftp.client.FTPClient"
} external;

public function poll(ListenerConfig config) returns error? = @java:Method{
    name: "poll",
    class: "org.wso2.ei.ftp.server.FTPListenerHelper"
} external;

public function register(Listener listenerEndpoint, ListenerConfig config, service ftpService, handle name)
    returns handle|error = @java:Method{
    name: "register",
    class: "org.wso2.ei.ftp.server.FTPListenerHelper"
} external;

