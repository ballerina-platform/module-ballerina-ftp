import ballerina.net.ftp;
import ballerina.lang.files;
import ballerina.lang.blobs;
import ballerina.lang.strings;

function createFile (string url, boolean createFolder) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File newDir = {path:url};
    c.createFile(newDir, createFolder);
}

function isExist (string url) (boolean) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File textFile = {path:url};
    return c.exists(textFile);
}

function readContent (string url) (string) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File textFile = {path:url};
    blob contentB = c.read(textFile);
    return blobs:toString(contentB, "UTF-8");
}

function copyFiles (string source, string destination) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File txtFile = {path:source};
    files:File copyOfTxt = {path:destination};
    c.copy(txtFile, copyOfTxt);
}

function moveFile (string source, string destination) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File txtFile = {path:source};
    files:File copyOfTxt = {path:destination};
    c.move(txtFile, copyOfTxt);
}

function write (string source, string content) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File wrt = {path:source};
    blob contentD = strings:toBlob(content, "UTF-8");
    c.write(contentD, wrt);
}

function fileDelete (string source) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    files:File del = {path:source};
    c.delete(del);
}
