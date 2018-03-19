package ballerina.net.ftp;

////////////////////////////
/// FTP Client Endpoint ///
///////////////////////////

@Description {value:"Represents an FTP client endpoint"}
@Field {value:"config: The configurations associated with the endpoint"}
public struct ClientEndpoint {
    ClientEndpointConfiguration config;
}


@Description {value:"ClientEndpointConfiguration struct represents options to be used for FTP client invocation"}
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
@Param {value:"config: The ClientEndpointConfiguration of the endpoint"}
public function <ClientEndpoint ep> init (ClientEndpointConfiguration config) {
    ep.config = config;
    ep.initEndpoint();
}

public native function <ClientEndpoint ep> initEndpoint ();

public function <ClientEndpoint ep> register (type serviceType) {
}

public function <ClientEndpoint ep> start () {
}

@Description {value:"Returns the connector that client code uses"}
@Return {value:"The connector that client code uses"}
public native function <ClientEndpoint ep> getClient () (ClientConnector);

@Description {value:"Stops the registered service"}
public function <ClientEndpoint ep> stop () {
}