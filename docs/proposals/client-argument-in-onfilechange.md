# Proposal: Passing `ftp:Client` as an argument to `onFileChange` method

_Owners_: @shafreenAnfar @dilanSachi @Bhashinee     
_Reviewers_: @shafreenAnfar @Bhashinee  
_Created_: 2022/02/23  
_Issues_: [#2525](https://github.com/ballerina-platform/ballerina-standard-library/issues/2525)

## Summary
The `onFileChange` method in `ftp:Service` gets invoked for any file changes in the FTP server.
The argument `ftp:WatchEvent` is passed to the method, and it contains the details of newly added files and deleted files.
When the user wants to do any task, an `ftp:Client` has to be created manually. In this proposal, we are going to 
pass `ftp:Client` as an argument to the `onFileChange` method to improve the usability.

## Goals
- To support passing `ftp:Client` as an argument to the `onFileChange` method.
- To allow adding `ftp:Client` as an argument in compiler plugin.
- To support code snippet generation with/without `ftp:Client`.

## Motivation
As mentioned in the summary, when a user wants to do any useful task(delete, rename, move, append files) once a file 
change event is received, user have to do it using an `ftp:Client`. This is somewhat of a burden, and a usability issue 
since the user has to create the `ftp:Client` by providing the same configurations provided to the `ftp:Listener`.
However, if the `ftp:Client` is passed as an argument to the `onFileChange` method, user can directly use it to do any
task needed. This would reduce lines of code, duplication as well as will ease the life of a developer.

## Description

## Testing

