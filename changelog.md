# Change Log
This file contains all the notable changes done to the Ballerina Email package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
 - [[#1438] Add SFTP and related security](https://github.com/ballerina-platform/ballerina-standard-library/issues/1438)
 - [[#1607] Add the support for anonymous user](https://github.com/ballerina-platform/ballerina-standard-library/issues/1607)

### Changed
 - [[#1345] Introduce byte stream related functionality to FTP module](https://github.com/ballerina-platform/ballerina-standard-library/issues/1345)
 - [[#1388] Add verification for tests](https://github.com/ballerina-platform/ballerina-standard-library/issues/1388)
 - [[#1606] Rename `BasicAuth` record to `Credentials`](https://github.com/ballerina-platform/ballerina-standard-library/issues/1606)
 - [[#1506] Migrate required content of `wso2/transport-file` repository into the module](https://github.com/ballerina-platform/ballerina-standard-library/issues/1506)
 - [[#1648] Migrate the dependency library `org.wso2.org.apache.commons:commons-vfs2` to the latest version of the `org.apache.commons:commons-vfs2` library](https://github.com/ballerina-platform/ballerina-standard-library/issues/1648)
 - [[#1749] Initialize `VfsClientConnector` once when the FTP Client is initialized](https://github.com/ballerina-platform/ballerina-standard-library/issues/1749)
 - [[#1782] Throw an error when file/directory does not exist in the `isDirectory` method](https://github.com/ballerina-platform/ballerina-standard-library/issues/1782)
 - [[#1703] Revamp the logic of the FTP Listener](https://github.com/ballerina-platform/ballerina-standard-library/issues/1703)
 - [[#1940] Remove `arraySize` parameter from the `get` method of the FTP Client API](https://github.com/ballerina-platform/ballerina-standard-library/issues/1940)
 - [[#1957] Change `boolean` typed `compressInput` parameter of the `put` method of FTP Client to an `enum` type with the name, `compressionType`](https://github.com/ballerina-platform/ballerina-standard-library/issues/1957)
 - [[#1955] Make the access to the `WatchEvent` as `readonly` in the FTP Listener](https://github.com/ballerina-platform/ballerina-standard-library/issues/1955)

### Fixed
 - [[#1518] Remove thrown exceptions and make then return](https://github.com/ballerina-platform/ballerina-standard-library/issues/1518)
 - [[#1588] Add missing SFTP related configuration to Module.md](https://github.com/ballerina-platform/ballerina-standard-library/issues/1588)
 - [[#1523] Fix the issue of created zip file being corrupted](https://github.com/ballerina-platform/ballerina-standard-library/issues/1523)
 - [[#1546] Correctly handle errors of `get` method](https://github.com/ballerina-platform/ballerina-standard-library/issues/1546)
 - [[#108] Implement the `stop` functionality to the FTP connector](https://github.com/ballerina-platform/ballerina-standard-library/issues/108)
 - [[#106] Correctly set the `lastModifiedTime` and `fileSize` to the `FileInfo` object](https://github.com/ballerina-platform/ballerina-standard-library/issues/106)
 - [[#2245] Remove warnings in both build and runtime](https://github.com/ballerina-platform/ballerina-standard-library/issues/2245)
