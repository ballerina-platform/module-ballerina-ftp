package ballerina.net.fs;

///////////////////////////
/// Service Annotations ///
///////////////////////////
@Description {value: "Configuration for File System service"}
@Field {value: "endpoints: An array of endpoints the service would be attached to"}
public struct FileSystemServiceConfiguration {
    Service[] endpoints;
}

@Description {value:"Configurations annotation for an File System service"}
public annotation <service> serviceConfig FileSystemServiceConfiguration;