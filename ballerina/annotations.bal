// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

# Annotation to override default file extension routing for content methods.
# Use this to specify which file patterns should be handled by a particular content method.
#
# + pattern - File name pattern (regex) that should be routed to this method.
#             Must be a subset of the listener's `fileNamePattern`.
public annotation record {| string pattern; |} FileConfig on function;
