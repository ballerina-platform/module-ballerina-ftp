# Change Log
This file contains all the notable changes done to the Ballerina Email package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- [Address `CVE-2025-27553` vulnerability in Apache Commons VFS](https://github.com/ballerina-platform/ballerina-library/issues/7740)

## [2.11.0] - 2024-08-20

### Fixed

- [Isolated the `ftp:Listener`](https://github.com/ballerina-platform/module-ballerina-ftp/pull/1287)

## [2.10.1] - 2024-07-24

## Added

- [Added customizable authentication methods in SFTP listener](https://github.com/ballerina-platform/ballerina-library/issues/6771)

## [2.10.0] - 2024-07-23

### Added

- [Added customizable authentication methods in SFTP client](https://github.com/ballerina-platform/ballerina-library/issues/6768)

## [2.9.1] - 2024-02-19

### Added

- [Added cause details to the errors](https://github.com/ballerina-platform/ballerina-library/issues/6064)

### Fixed

- [Fix NPE when reusing a file stream returned from a `ftp:get` operation](https://github.com/ballerina-platform/ballerina-library/issues/5990)

## [2.9.0] - 2023-09-15

### Changed

- [Changed disallowing service level annotations in the compiler plugin](https://github.com/ballerina-platform/ballerina-standard-library/issues/4741)
- [Make some of the Java classes proper utility classes](https://github.com/ballerina-platform/ballerina-standard-library/issues/5070)

## [2.7.1] - 2023-06-01

- [Fixed log manager getting reset by `ftp` module](https://github.com/ballerina-platform/ballerina-standard-library/issues/4478)

## [2.7.0] - 2023-04-10

### Changed
- [Exit the service when a panic occurs inside the service method](https://github.com/ballerina-platform/ballerina-standard-library/issues/4241)

## [2.5.1] - 2022-12-22

### Fixed

- [GraalVM build failure due to missing configs](https://github.com/ballerina-platform/ballerina-standard-library/issues/4483)

## [2.5.0] - 2022-11-29

### Changed
- [API docs updated](https://github.com/ballerina-platform/ballerina-standard-library/issues/3463)
- [Stopped Java client logs from appearing in the console](https://github.com/ballerina-platform/module-ballerina-ftp/pull/824)

## [2.4.1] - 2022-12-08

### Changed
- [Updated `commons-net` version to `3.9.0`](https://github.com/ballerina-platform/ballerina-standard-library/issues/3841)

## [2.3.0] - 2022-05-30

### Added
- [[#2525] Introduced `ftp:Caller` API and added as an optional parameter in `onFileChange` method](https://github.com/ballerina-platform/ballerina-standard-library/issues/2525)
- [[#1514] Added compiler plugin validation support for `ftp:Service`](https://github.com/ballerina-platform/ballerina-standard-library/issues/1514)
- [[#2704] Added code snippet generation for `ftp:Service`](https://github.com/ballerina-platform/ballerina-standard-library/issues/2704)

## [2.2.0] - 2022-01-28

### Changed
 - [[#2741] Remove `readonly` type of `ftp:WatchEvent` and support both `ftp:WatchEvent & readonly` and `ftp:WatchEvent` in `onFileChange` method](https://github.com/ballerina-platform/ballerina-standard-library/issues/2741)
 - [[#2549] Make `path` field to root and `fileNamePattern` field optional in `ftp:ListenerConfiguration`](https://github.com/ballerina-platform/ballerina-standard-library/issues/2549)

## [2.0.1] - 2021-11-20

### Changed
 - [[#2398] Mark Service type as distinct](https://github.com/ballerina-platform/ballerina-standard-library/issues/2398)

### Fixed
 - [[#2245] Remove warnings in both build and runtime](https://github.com/ballerina-platform/ballerina-standard-library/issues/2245)

## [2.0.0] - 2021-10-10

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
