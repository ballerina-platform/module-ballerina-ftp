import ballerina.net.ftp;
import ballerina.file;

function createFile (string url, boolean createFolder) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File newDir = {path:url};
    c.createFile(newDir, createFolder);
}

function isExist (string url) (boolean) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File textFile = {path:url};
    return c.exists(textFile);
}

function readContent (string url) (string) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File textFile = {path:url};
    blob contentB = c.read(textFile);
    return contentB.toString("UTF-8");
}

function copyFiles (string source, string destination) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File txtFile = {path:source};
    file:File copyOfTxt = {path:destination};
    c.copy(txtFile, copyOfTxt);
}

function moveFile (string source, string destination) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File txtFile = {path:source};
    file:File copyOfTxt = {path:destination};
    c.move(txtFile, copyOfTxt);
}

function write (string source, string content) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File wrt = {path:source};
    blob contentD = content.toBlob("UTF-8");
    c.write(contentD, wrt);
}

function fileDelete (string source) {
    ftp:ClientConnector c = create ftp:ClientConnector();
    file:File del = {path:source};
    c.delete(del);
}
