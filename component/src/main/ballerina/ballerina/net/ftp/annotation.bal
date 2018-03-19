package ballerina.net.ftp;

///////////////////////////
/// Service Annotations ///
///////////////////////////
@Description {value: "Configuration for remote FTP monitor service"}
@Field {value: "endpoints: An array of endpoints the service would be attached to"}
public struct FTPServiceConfiguration {
    Service[] endpoints;
}

@Description {value:"Configurations annotation for remote FTP monitor service"}
public annotation <service> serviceConfig FTPServiceConfiguration;
