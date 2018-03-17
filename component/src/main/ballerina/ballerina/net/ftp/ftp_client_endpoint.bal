package ballerina.net.ftp;

////////////////////////////
/// FTP Client Endpoint ///
///////////////////////////

@Description {value:"Represents an FTP client endpoint"}
@Field {value:"epName: The name of the endpoint"}
@Field {value:"config: The configurations associated with the endpoint"}
public struct Client {
    string epName;
    ClientEndpointConfiguration config;
}


@Description {value:"ClientEndpointConfiguration struct represents options to be used for HTTP client invocation"}
@Field {value:"protocol: Either ftp or sftp"}
@Field {value:"host: Target service url"}
@Field {value:"port: Port number of the remote service"}
@Field {value:"username: Username for authentication"}
@Field {value:"passPhrase: Password for authentication"}
public struct ClientEndpointConfiguration {
    string protocol;
    string host;
    int port;
    string username;
    string passPhrase;
}

@Description {value:"Initializes the ClientEndpointConfiguration struct with default values."}
@Param {value:"config: The ClientEndpointConfiguration struct to be initialized"}
public function <ClientEndpointConfiguration config> ClientEndpointConfiguration () {
}

@Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
@Param {value:"ep: The endpoint to be initialized"}
@Param {value:"epName: The endpoint name"}
@Param {value:"config: The ClientEndpointConfiguration of the endpoint"}
public function <Client ep> init (string epName, ClientEndpointConfiguration config) {
    ep.epName = epName;
    ep.config = config;
    ep.initEndpoint();
}

public native function <Client ep> initEndpoint ();

public function <Client ep> register (type serviceType) {
}

public function <Client ep> start () {
}

@Description {value:"Returns the connector that client code uses"}
@Return {value:"The connector that client code uses"}
public native function <Client ep> getConnector () returns (ClientConnector conn);

@Description {value:"Stops the registered service"}
public function <Client ep> stop () {
}