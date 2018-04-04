package ballerina.net.fs;

///////////////////////////
/// Service Annotations ///
///////////////////////////
@Description {value: "Configuration for DirectoryListener service"}
@Field {value: "endpoints: An array of endpoints the service would be attached to"}
public struct DirectoryListenerConfiguration {
    DirectoryListener[] endpoints;
}

@Description {value:"Configurations annotation for a DirectoryListener service"}
public annotation <service> ServiceConfig DirectoryListenerConfiguration;
