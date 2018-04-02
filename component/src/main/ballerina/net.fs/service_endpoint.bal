package ballerina.net.fs;

//////////////////////////////////
/// DirectoryListener Endpoint ///
//////////////////////////////////
public struct DirectoryListener {
    Connection conn;
    ListenerEndpointConfiguration config;
}

@Description {value:"Configuration for local file system service endpoint"}
@Field {value:"path: Listener directory path"}
@Field {value: "recursive: Recursively monitor all folders in the given folder path"}
public struct ListenerEndpointConfiguration {
    string path;
    boolean recursive;
}

@Description {value:"Gets called when the endpoint is being initialized during the package initialization."}
@Param {value:"config: The ServiceEndpointConfiguration of the endpoint"}
@Return {value:"Error occured during initialization"}
public function <DirectoryListener ep> init (ListenerEndpointConfiguration config) {
    ep.config = config;
    var err = ep.initEndpoint();
    if (err != null) {
        throw err;
    }
}

public native function <DirectoryListener ep> initEndpoint () returns (error);

@Description {value:"Gets called every time a service attaches itself to this endpoint. Also happens at package initialization."}
@Param {value:"ep: The endpoint to which the service should be registered to"}
@Param {value:"serviceType: The type of the service to be registered"}
public native function <DirectoryListener ep> register (typedesc serviceType);

@Description {value:"Starts the registered service"}
public native function <DirectoryListener ep> start ();

@Description {value:"Returns the connector that client code uses"}
@Return {value:"The connector that client code uses"}
public native function <DirectoryListener ep> getClient () returns (Connection);

@Description {value:"Stops the registered service"}
public native function <DirectoryListener ep> stop ();

public struct Connection {}
