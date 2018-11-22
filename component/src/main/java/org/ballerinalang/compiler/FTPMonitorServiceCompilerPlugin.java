/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.compiler;

import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportEndpointTypes;
import org.ballerinalang.ftp.util.FtpConstants;
import org.ballerinalang.model.tree.AnnotationAttachmentNode;
import org.ballerinalang.model.tree.EndpointNode;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.model.tree.expressions.ExpressionNode;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.ballerinalang.util.diagnostic.DiagnosticLog;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangResource;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;

import java.util.List;

import static org.ballerinalang.ftp.util.FtpConstants.FTP_SERVER_EVENT;

/**
 * Compiler plugin for validating FTP Listener.
 */
@SupportEndpointTypes(value = {
        @SupportEndpointTypes.EndpointType(orgName = "wso2",
                                           packageName = "ftp",
                                           name = "Listener")
})
public class FTPMonitorServiceCompilerPlugin extends AbstractCompilerPlugin {

    private DiagnosticLog dlog = null;

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        dlog = diagnosticLog;
    }

    @Override
    public void process(ServiceNode serviceNode, List<AnnotationAttachmentNode> annotations) {
        List<BLangResource> resources = (List<BLangResource>) serviceNode.getResources();
        if (resources.size() == 0) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, serviceNode.getPosition(),
                    "No resources define for service: " + serviceNode.getName().getValue());
        } else if (resources.size() >= 2) {
            dlog.logDiagnostic(Diagnostic.Kind.ERROR, serviceNode.getPosition(),
                    "Only one resource allows for service: " + serviceNode.getName().getValue());
        }
        if (resources.size() == 1) {
            final List<BLangSimpleVariable> parameters = resources.get(0).getParameters();
            if (parameters.size() != 1) {
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, resources.get(0).getPosition(),
                        "Invalid resource signature. A single " + FTP_SERVER_EVENT
                                + " parameter allow in the resource signature.");
            }
            final BType type = parameters.get(0).getTypeNode().type;
            if (type.getKind().equals(TypeKind.OBJECT)) {
                if (type instanceof BObjectType) {
                    BObjectType event = (BObjectType) type;
                    if (!"ftp".equals(event.tsymbol.pkgID.name.value) || !FTP_SERVER_EVENT
                            .equals(event.tsymbol.name.value)) {
                        dlog.logDiagnostic(Diagnostic.Kind.ERROR, parameters.get(0).getPosition(),
                                "Parameter should be of type - ftp:" + FTP_SERVER_EVENT);
                    }
                }
            }
        }
    }

    @Override
    public void process(EndpointNode endpointNode, List<AnnotationAttachmentNode> annotations) {
        final ExpressionNode configurationExpression = endpointNode.getConfigurationExpression();
        if (NodeKind.RECORD_LITERAL_EXPR.equals(configurationExpression.getKind())) {
            BLangRecordLiteral recordLiteral = (BLangRecordLiteral) configurationExpression;
            boolean isNonEmptyPath = false;
            boolean isNonEmptyHost = false;
            for (BLangRecordLiteral.BLangRecordKeyValue config : recordLiteral.getKeyValuePairs()) {
                final String key = ((BLangSimpleVarRef) config.getKey()).variableName.value;
                if (config.getValue() instanceof BLangLiteral) {
                    final Object value = ((BLangLiteral) config.getValue()).getValue();
                    if (value == null) {
                        throw new BallerinaException(key + " does not contains any value.");
                    }
                    switch (key) {
                        case FtpConstants.ENDPOINT_CONFIG_PATH:
                            if (!value.toString().isEmpty()) {
                                isNonEmptyPath = true;
                            }
                            break;
                        case FtpConstants.ENDPOINT_CONFIG_HOST:
                            if (!value.toString().isEmpty()) {
                                isNonEmptyHost = true;
                            }
                            break;
                        default:
                            // Do nothing.
                    }
                }
            }
            if (!isNonEmptyPath) {
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, endpointNode.getPosition(),
                        "Cannot create FTP Listener without path.");
            }
            if (!isNonEmptyHost) {
                dlog.logDiagnostic(Diagnostic.Kind.ERROR, endpointNode.getPosition(),
                        "Cannot create FTP Listener without host.");
            }
        }
    }
}
