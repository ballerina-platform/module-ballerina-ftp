/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.ftp.plugin;

import static io.ballerina.stdlib.ftp.plugin.PluginConstants.CODE_TEMPLATE_XML;
import static io.ballerina.stdlib.ftp.plugin.PluginConstants.LS;

/**
 * Code action to add onFileXml format-specific handler template.
 */
public class FtpCodeTemplateXml extends AbstractFtpCodeTemplate {

    @Override
    protected String getRemoteFunctionText() {
        return LS + "\tremote function onFileXml(record{} content) returns ftp:Error? {" + LS +
                LS + "\t}" + LS;
    }

    @Override
    protected String getActionName() {
        return "Add onFileXml handler";
    }

    @Override
    protected String getTemplateName() {
        return CODE_TEMPLATE_XML;
    }
}
