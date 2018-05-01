import ballerina/io;
import wso2/ftp;
import ballerina/file;

function createDirectory(string host, string url) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->mkdir(url);
}

function removeDirectory(string host, string url) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->rmdir(url);
}

function readContent(string host, string url) returns string {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    var output = client->get(url);
    io:ByteChannel channel = check output;
    blob contentB;
    var result = channel.read(15);
    match result {
        (blob, int) content => {
            var (cont, readSize) = content;
            contentB = cont;
        }
        error readError => {
            throw readError;
        }
    }
    _ = channel.close();
    return contentB.toString("UTF-8");
}

function write(string host, string path, string filePath) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    io:ByteChannel bchannel = io:openFile(filePath, io:READ);
    _ = client->put(path, bchannel);
}


function append(string host, string path, string filePath) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    io:ByteChannel bchannel = io:openFile(filePath, io:READ);
    _ = client->append(path, bchannel);
}

function fileDelete(string host, string path) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->delete(path);
}

function size(string host, string path) returns int {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };
    int size = 0;
    var result = client->size(path);
    size = check result;
    return size;
}

function list(string host, string path) returns string[] {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };
    string[] list;
    var result = client->list(path);
    list = check result;
    return list;
}

function rename(string host, string source, string destination) {
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    _ = client->rename(source, destination);
}

function isDirectory(string host, string path) returns boolean{
    endpoint ftp:Client client {
        protocol: ftp:FTP,
        host: host
    };

    return check client->isDirectory(path);
}
