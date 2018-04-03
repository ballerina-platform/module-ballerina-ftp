import ballerina/io;
import ballerina/net.ftp;

function createDirectory (string host, string url) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    _ = clientEndpoint -> mkdir(url);
}

function removeDirectory (string host, string url) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    _ = clientEndpoint -> rmdir(url);
}

function readContent (string host, string url) returns string {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    io:ByteChannel channel =? clientEndpoint -> get(url);
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

//function copyFiles (string host, string source, string destination) {
//    endpoint ftp:ClientEndpoint clientEndpoint {
//        protocol: "ftp",
//        host:host
//    };
//
//    file:File txtFile = {path:source};
//    file:File copyOfTxt = {path:destination};
//    _ = clientEndpoint -> copy(txtFile, copyOfTxt);
//}

//function moveFile (string host, string source, string destination) {
//    endpoint ftp:ClientEndpoint clientEndpoint {
//        protocol: "ftp",
//        host:host
//    };
//
//    file:File txtFile = {path:source};
//    file:File copyOfTxt = {path:destination};
//    _ = clientEndpoint -> move(txtFile, copyOfTxt);
//}

function write (string host, string path, string content) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    blob contentD = content.toBlob("UTF-8");
    _ = clientEndpoint -> put(contentD, path);
}


function append (string host, string path, string content) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol:"ftp",
        host:host
    };

    blob contentD = content.toBlob("UTF-8");
    _ = clientEndpoint -> append(contentD, path);
}

function fileDelete (string host, string path) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };

    _ = clientEndpoint -> delete(path);
}

function size (string host, string path) returns int {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol: "ftp",
        host:host
    };
    int size = 0;
    size =? clientEndpoint -> size(path);
    return size;
}

function list (string host, string path) returns string[] {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol:"ftp",
        host:host
    };
    string[] list;
    list =? clientEndpoint -> list(path);
    return list;
}

function rename (string host, string source, string destination) {
    endpoint ftp:ClientEndpoint clientEndpoint {
        protocol:"ftp",
        host:host
    };

    _ = clientEndpoint -> rename(source, destination);
}
