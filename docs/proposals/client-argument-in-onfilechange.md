# Proposal: Passing `ftp:Caller` as an argument to `onFileChange` method

_Owners_: @shafreenAnfar @dilanSachi @Bhashinee     
_Reviewers_: @shafreenAnfar @Bhashinee  
_Created_: 2022/02/23  
_Issues_: [#2525](https://github.com/ballerina-platform/ballerina-standard-library/issues/2525)

## Summary
The `onFileChange` method in `ftp:Service` gets invoked for any file changes in the FTP server. The argument `ftp:WatchEvent` is passed to the method, and it contains the details of newly added files and deleted files. When the user wants to do any task, an `ftp:Client` has to be created manually. In this proposal, we are going to pass `ftp:Caller` which contains the APIs of the `ftp:Client` as an argument to the `onFileChange` method to improve the usability.

## Goals
- To support passing `ftp:Caller` as an argument to the `onFileChange` method.
- To allow adding `ftp:Caller` as an argument in compiler plugin.
- To support code snippet generation with/without `ftp:Caller`.

## Motivation
As mentioned in the summary, when a user wants to do any useful task(delete, rename, move, append files) once a file change event is received, user have to do it using an `ftp:Client`. This is somewhat of a burden, and a usability issue since the user has to create the `ftp:Client` by providing the same configurations provided to the `ftp:Listener`. However, if the `ftp:Caller` is passed as an argument to the `onFileChange` method, user can directly use it to do any task needed. This would reduce lines of code, duplication as well as will ease the life of the developer.

## Description
`ftp:Caller` is basically an `ftp:Client` and will contain the same APIs of the `ftp:Client` and the only difference is that the user cannot create an instance of `ftp:Caller`. It will only be able to create internally.
Since the `ftp:ClientConfiguration` contains a subset of `ftp:ListenerConfiguration`, we can easily create an `ftp:Caller` when the `ftp:Listener` is created and pass the `ftp:Caller` as an argument if the user has added `ftp:Caller` in the function signature.

That way, both the following `onFileChange` implementations will be valid.
```ballerina
ftp:Service ftpService = service object {
    remote function onFileChange(ftp:Caller caller, ftp:WatchEvent & readonly event) {
        // process event
    }
};
```
```ballerina
ftp:Service ftpService = service object {
    remote function onFileChange(ftp:WatchEvent & readonly event) {
        // process event
    }
};
```

Along with this, compiler plugin changes related to validating the `onFileChange` method signature and code snippet generation will be updated.
## Testing
Testing will contain 3 main steps.
- Testing the runtime behaviour of the `onFileChange` method with/without `ftp:Caller`
- Testing compiler plugin validation with/without `ftp:Caller`
- Testing code snippet generation with/without `ftp:Caller`
