import ballerina/net.ftp;
import ballerina/file;
import ballerina/io;

function createFile (string host, string url, boolean createFolder) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File newDir = {path:url};
    _ = clientEndpoint -> createFile(newDir, createFolder);
}

function isExist (string host, string url) returns boolean {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File existFile = {path:url};
    boolean result;
    result =? clientEndpoint -> exists(existFile);
    return result;
}

function readContent (string host, string url) returns string {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File textFile = {path:url};
    io:ByteChannel channel =? clientEndpoint -> read(textFile);
    blob contentB;
    var result = channel.read(15);
    match result {
        (blob, int) content => {
            var (cont, readSize) = content;
            contentB = cont;
        }
        io:IOError readError => {
            throw readError;
        }
    }
    _ = channel.close();
    return contentB.toString("UTF-8");
}

function copyFiles (string host, string source, string destination) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File txtFile = {path:source};
    file:File copyOfTxt = {path:destination};
    _ = clientEndpoint -> copy(txtFile, copyOfTxt);
}

function moveFile (string host, string source, string destination) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File txtFile = {path:source};
    file:File copyOfTxt = {path:destination};
    _ = clientEndpoint -> move(txtFile, copyOfTxt);
}

function write (string host, string source, string content) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File wrt = {path:source};
    blob contentD = content.toBlob("UTF-8");
    _ = clientEndpoint -> write(contentD, wrt, "o");
}

function fileDelete (string host, string source) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File del = {path:source};
    _ = clientEndpoint -> delete(del);
}

function pipeContent (string host, string source, string destination) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    file:File sourceFile = {path:source};
    io:ByteChannel channel =? clientEndpoint -> read(sourceFile);
    file:File destinationFile = {path:destination};
    _ = clientEndpoint -> pipe(channel, destinationFile, "o");
    _ = channel.close();
}
