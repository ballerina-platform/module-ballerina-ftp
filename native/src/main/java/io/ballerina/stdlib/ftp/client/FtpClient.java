/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.ftp.client;

import io.ballerina.lib.data.jsondata.json.Native;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.stdlib.ftp.exception.BallerinaFtpException;
import io.ballerina.stdlib.ftp.exception.RemoteFileSystemConnectorException;
import io.ballerina.stdlib.ftp.transport.RemoteFileSystemConnectorFactory;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.FtpAction;
import io.ballerina.stdlib.ftp.transport.client.connector.contract.VfsClientConnector;
import io.ballerina.stdlib.ftp.transport.client.connector.contractimpl.VfsClientConnectorImpl;
import io.ballerina.stdlib.ftp.transport.impl.RemoteFileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.ftp.transport.message.RemoteFileSystemMessage;
import io.ballerina.stdlib.ftp.util.BufferHolder;
import io.ballerina.stdlib.ftp.util.CSVUtils;
import io.ballerina.stdlib.ftp.util.FtpConstants;
import io.ballerina.stdlib.ftp.util.FtpUtil;
import io.ballerina.stdlib.io.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.stdlib.ftp.util.FtpConstants.ENDPOINT_CONFIG_PREFERRED_METHODS;
import static io.ballerina.stdlib.ftp.util.FtpConstants.ENTITY_BYTE_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.FTP_ERROR;
import static io.ballerina.stdlib.ftp.util.FtpConstants.READ_INPUT_STREAM;
import static io.ballerina.stdlib.ftp.util.FtpConstants.VFS_CLIENT_CONNECTOR;
import static io.ballerina.stdlib.ftp.util.FtpUtil.ErrorType.Error;
import static io.ballerina.stdlib.ftp.util.FtpUtil.findRootCause;

/**
 * Contains functionality of FTP client.
 */
public class FtpClient {

    private static final Logger log = LoggerFactory.getLogger(FtpClient.class);

    private FtpClient() {
        // private constructor
    }

    public static Object initClientEndpoint(BObject clientEndpoint, BMap<Object, Object> config) {
        String protocol = (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_PROTOCOL)))
                .getValue();

        // Keep databinding config for later
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING,
                config.getBooleanValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING)));

        Map<String, String> authMap = FtpUtil.getAuthMap(config, protocol);
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_USERNAME,
                authMap.get(FtpConstants.ENDPOINT_CONFIG_USERNAME));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PASS_KEY,
                authMap.get(FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_HOST,
                (config.getStringValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_HOST))).getValue());
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PORT,
                FtpUtil.extractPortValue(config.getIntValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PORT))));
        clientEndpoint.addNativeData(FtpConstants.ENDPOINT_CONFIG_PROTOCOL, protocol);
        Map<String, String> ftpConfig = new HashMap<>(6);
        BMap auth = config.getMapValue(StringUtils.fromString(FtpConstants.ENDPOINT_CONFIG_AUTH));
        if (auth != null) {
            final BMap privateKey = auth.getMapValue(StringUtils.fromString(
                    FtpConstants.ENDPOINT_CONFIG_PRIVATE_KEY));
            if (privateKey != null) {
                final BString privateKeyPath = privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_KEY_PATH));
                ftpConfig.put(FtpConstants.IDENTITY, privateKeyPath.getValue());
                final BString privateKeyPassword = privateKey.getStringValue(StringUtils.fromString(
                        FtpConstants.ENDPOINT_CONFIG_PASS_KEY));
                if (privateKeyPassword != null && !privateKeyPassword.getValue().isEmpty()) {
                    ftpConfig.put(FtpConstants.IDENTITY_PASS_PHRASE, privateKeyPassword.getValue());
                }
            }
            ftpConfig.put(ENDPOINT_CONFIG_PREFERRED_METHODS, FtpUtil.getPreferredMethodsFromAuthConfig(auth));
        }
        ftpConfig.put(FtpConstants.PASSIVE_MODE, String.valueOf(true));
        boolean userDirIsRoot = config.getBooleanValue(FtpConstants.USER_DIR_IS_ROOT_FIELD);
        ftpConfig.put(FtpConstants.USER_DIR_IS_ROOT, String.valueOf(userDirIsRoot));
        ftpConfig.put(FtpConstants.AVOID_PERMISSION_CHECK, String.valueOf(true));
        String url;
        try {
            url = FtpUtil.createUrl(clientEndpoint, "");
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), Error.errorType());
        }
        ftpConfig.put(FtpConstants.URI, url);
        clientEndpoint.addNativeData(FtpConstants.PROPERTY_MAP, ftpConfig);
        RemoteFileSystemConnectorFactory fileSystemConnectorFactory = new RemoteFileSystemConnectorFactoryImpl();
        try {
            VfsClientConnector connector = fileSystemConnectorFactory.createVfsClientConnector(ftpConfig);
            clientEndpoint.addNativeData(VFS_CLIENT_CONNECTOR, connector);
        } catch (RemoteFileSystemConnectorException e) {
            return FtpUtil.createError(e.getMessage(), findRootCause(e), Error.errorType());
        }
        return null;
    }

    /**
     * @deprecated : use typed getters like getBytes/getText/getJson/getXml/getCsv or their streaming variants.
     */
    @Deprecated
    public static Object getFirst(Environment env, BObject clientConnector, BString filePath) {
        clientConnector.addNativeData(ENTITY_BYTE_STREAM, null);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGetAction(remoteFileSystemBaseMessage,
                            balFuture, clientConnector));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    /**
     * @deprecated : use getBytesAsStream or getCsvAsStream instead of this legacy accessor.
     */
    @Deprecated
    public static Object get(BObject clientConnector) {
        return FtpClientHelper.generateInputStreamEntry((InputStream) clientConnector.getNativeData(READ_INPUT_STREAM));
    }

    public static Object getBytes(Environment env, BObject clientConnector, BString filePath) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }
        return ValueCreator.createArrayValue((byte[]) content);
    }

    public static Object getText(Environment env, BObject clientConnector, BString filePath) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }
        return StringUtils.fromString(new String((byte[]) content, StandardCharsets.UTF_8));
    }

    public static Object getJson(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }

        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(
                io.ballerina.lib.data.ModuleUtils.getModule(), "Options");
        if (laxDataBinding) {
            BMap allowDataProjection = mapValue.getMapValue(StringUtils.fromString("allowDataProjection"));
            allowDataProjection.put(StringUtils.fromString("nilAsOptionalField"), Boolean.TRUE);
            allowDataProjection.put(StringUtils.fromString("absentAsNilableType"), Boolean.TRUE);
            mapValue.put(StringUtils.fromString("allowDataProjection"), allowDataProjection);
        } else {
            mapValue.put(StringUtils.fromString("allowDataProjection"), Boolean.FALSE);
        }
        Object bJson = Native.parseBytes(ValueCreator.createArrayValue((byte[]) content), mapValue, typeDesc);
        if (bJson instanceof BError) {
            return FtpUtil.createError(((BError) bJson).getErrorMessage().getValue(), FTP_ERROR);
        }
        return bJson;
    }

    public static Object getXml(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }

        if (typeDesc.getDescribingType().getQualifiedName().equals("xml")) {
            Object bXml = XmlUtils.parse(StringUtils.fromString(new String((byte[]) content,
                    StandardCharsets.UTF_8)));
            if (bXml instanceof BError) {
                return FtpUtil.createError(((BError) bXml).getErrorMessage().getValue(), FTP_ERROR);
            }
            return bXml;
        }

        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(
                new Module("ballerina", "data.xmldata", "1"),
                "SourceOptions");
        mapValue.put(StringUtils.fromString("allowDataProjection"), laxDataBinding);

        Object bXml = io.ballerina.lib.data.xmldata.xml.Native.parseBytes(
                ValueCreator.createArrayValue((byte[]) content), mapValue, typeDesc);
        if (bXml instanceof BError) {
            return FtpUtil.createError(((BError) bXml).getErrorMessage().getValue(), FTP_ERROR);
        }
        return bXml;
    }

    public static Object getCsv(Environment env, BObject clientConnector, BString filePath, BTypedesc typeDesc) {
        Object content = getAllContent(env, clientConnector, filePath);
        if (!(content instanceof byte[])) {
            return content;
        }

        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        BMap<BString, Object> mapValue = ValueCreator.createRecordValue(
                io.ballerina.lib.data.csvdata.utils.ModuleUtils.getModule(), "ParseOptions");
        if (laxDataBinding) {
            BMap allowDataProjection = mapValue.getMapValue(StringUtils.fromString("allowDataProjection"));
            allowDataProjection.put(StringUtils.fromString("nilAsOptionalField"), Boolean.TRUE);
            allowDataProjection.put(StringUtils.fromString("absentAsNilableType"), Boolean.TRUE);
            mapValue.put(StringUtils.fromString("allowDataProjection"), allowDataProjection);
        } else {
            mapValue.put(StringUtils.fromString("allowDataProjection"), Boolean.FALSE);
        }

        Object csv = io.ballerina.lib.data.csvdata.csv.Native.parseBytes(
                ValueCreator.createArrayValue((byte[]) content), mapValue, typeDesc);
        if (csv instanceof BError) {
            return FtpUtil.createError(((BError) csv).getErrorMessage().getValue(), FTP_ERROR);
        }
        return csv;
    }

    public static Object getBytesAsStream(Environment env, BObject clientConnector, BString filePath) {
        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage ->
                            FtpClientHelper.executeStreamingAction(remoteFileSystemBaseMessage,
                                    balFuture, TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE), laxDataBinding));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object getCsvAsStream(Environment env, BObject clientConnector, BString filePath,
                                        BTypedesc typeDesc) {
        boolean laxDataBinding = (boolean) clientConnector.getNativeData(FtpConstants.ENDPOINT_CONFIG_LAX_DATABINDING);
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage ->
                            FtpClientHelper.executeStreamingAction(remoteFileSystemBaseMessage,
                                    balFuture, typeDesc.getDescribingType(), laxDataBinding));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    private static Object getAllContent(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGetAllAction(remoteFileSystemBaseMessage,
                            balFuture));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.GET_ALL, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object closeInputByteStream(BObject clientObject) {
        InputStream readInputStream = (InputStream) clientObject.getNativeData(READ_INPUT_STREAM);
        if (readInputStream != null) {
            try {
                readInputStream.close();
                clientObject.addNativeData(READ_INPUT_STREAM, null);
                clientObject.addNativeData(ENTITY_BYTE_STREAM, null);
                return null;
            } catch (IOException e) {
                return IOUtils.createError(e);
            }
        } else {
            return null;
        }
    }

    /**
     * @deprecated : use putBytes/putText/putJson/putXml/putCsv or the streaming variants with APPEND option.
     */
    @Deprecated
    public static Object append(Environment env, BObject clientConnector, BMap<Object, Object> inputContent) {
        boolean isFile = inputContent.getBooleanValue(StringUtils.fromString(
                FtpConstants.INPUT_CONTENT_IS_FILE_KEY));
        RemoteFileSystemMessage message;
        if (isFile) {
            InputStream stream = FtpClientHelper.getUploadStream(env, clientConnector, inputContent, true);
            message = new RemoteFileSystemMessage(stream);
        } else {
            String textContent = (inputContent.getStringValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_TEXT_CONTENT_KEY))).getValue();
            InputStream stream = new ByteArrayInputStream(textContent.getBytes());
            message = new RemoteFileSystemMessage(stream);
        }
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(message, FtpAction.APPEND, (inputContent.getStringValue(StringUtils.fromString(
                    FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(), null);
            return getResult(balFuture);
        });
    }

    /**
     * @deprecated : use typed put methods (putBytes/putText/putJson/putXml/putCsv) instead.
     */
    @Deprecated
    public static Object put(Environment env, BObject clientConnector, BMap<Object, Object> inputContent) {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        boolean isFile = inputContent.getBooleanValue(StringUtils.fromString(FtpConstants.INPUT_CONTENT_IS_FILE_KEY));
        boolean compressInput = inputContent.getBooleanValue(StringUtils.fromString(
                FtpConstants.INPUT_CONTENT_COMPRESS_INPUT_KEY));
        InputStream stream = FtpClientHelper.getUploadStream(env, clientConnector, inputContent, isFile);
        RemoteFileSystemMessage message;
        ByteArrayInputStream compressedStream = null;

        if (stream != null) {
            if (compressInput) {
                compressedStream = FtpUtil.compress(stream, (inputContent.getStringValue(StringUtils.fromString(
                        FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue());
                message = FtpClientHelper.getCompressedMessage(clientConnector, (inputContent.getStringValue(
                        StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                        propertyMap, compressedStream);
            } else {
                try {
                    message = FtpClientHelper.getUncompressedMessage(clientConnector, (inputContent.getStringValue(
                            StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue(),
                            propertyMap, stream);
                } catch (BallerinaFtpException e) {
                    return FtpUtil.createError(e.getMessage(), Error.errorType());
                }
            }
        } else {
            return FtpUtil.createError("Error while reading a file", Error.errorType());
        }
        Object result = env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            String filePath = (inputContent.getStringValue(
                    StringUtils.fromString(FtpConstants.INPUT_CONTENT_FILE_PATH_KEY))).getValue();
            if (compressInput) {
                filePath = FtpUtil.getCompressedFileName(filePath);
            }
            connector.send(message, FtpAction.PUT, filePath, null);
            return getResult(balFuture);
        });
        try {
            stream.close();
            if (compressedStream != null) {
                compressedStream.close();
            }
        } catch (IOException e) {
            log.error("Error in closing stream");
        }
        return result;
    }

    public static Object putBytes(Environment env, BObject clientConnector, BString path, BArray inputContent,
                                  BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getBytes());
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putText(Environment env, BObject clientConnector, BString path, BString inputContent,
                                 BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getValue().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putJson(Environment env, BObject clientConnector, BString path, BString inputContent,
                                 BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.getValue().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putXml(Environment env, BObject clientConnector, BString path, BXml inputContent,
                                BString options) {
        InputStream stream = new ByteArrayInputStream(inputContent.toString().getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putCsv(Environment env, BObject clientConnector, BString path, BArray inputContent,
                                BString options) {
        boolean addHeader = !options.getValue().equals(FtpConstants.WRITE_OPTION_APPEND);
        String convertToCsv = CSVUtils.convertToCsv(inputContent, addHeader);
        InputStream stream = new ByteArrayInputStream(convertToCsv.getBytes(StandardCharsets.UTF_8));
        RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
        return putGenericAction(env, clientConnector, path, options, message);
    }

    public static Object putBytesAsStream(Environment env, BObject clientConnector, BString path, BStream inputContent,
                                          BString options) {
        try {
            InputStream stream = createInputStreamFromIterator(env, inputContent.getIteratorObj());
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
            return putGenericAction(env, clientConnector, path, options, message);
        } catch (Exception e) {
            return FtpUtil.createError(e.getMessage(), FTP_ERROR);
        }
    }

    public static Object putCsvAsStream(Environment env, BObject clientConnector, BString path, BStream inputContent,
                                        BString options) {
        try {
            InputStream stream = createInputStreamFromIterator(env, inputContent.getIteratorObj());
            RemoteFileSystemMessage message = new RemoteFileSystemMessage(stream);
            return putGenericAction(env, clientConnector, path, options, message);
        } catch (Exception e) {
            return FtpUtil.createError(e.getMessage(), FTP_ERROR);
        }
    }

    /**
     * Creates an InputStream from a Ballerina iterator using SequenceInputStream.
     */
    private static InputStream createInputStreamFromIterator(Environment environment, BObject iterator) {
        IteratorToInputStream streamIterator = new IteratorToInputStream(environment, iterator);
        return new SequenceInputStream(asEnumeration(streamIterator));
    }

    /**
     * Converts an Iterator to an Enumeration for compatibility with SequenceInputStream.
     */
    private static <T> Enumeration<T> asEnumeration(Iterator<T> iterator) {
        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public T nextElement() {
                return iterator.next();
            }
        };
    }

    /**
     * Lightweight adapter that turns a Ballerina iterator into a sequence of InputStreams.
     * Keeps state minimal to reduce nesting and cognitive complexity in the outer method.
     */
    private static final class IteratorToInputStream implements Iterator<InputStream> {
        private final Environment env;
        private final BObject iterator;
        private InputStream nextStream;
        private boolean hasChecked;
        private boolean isFirstRow = true;

        IteratorToInputStream(Environment env, BObject iterator) {
            this.env = env;
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            if (hasChecked) {
                return nextStream != null;
            }
            nextStream = fetchNextStream();
            hasChecked = true;
            return nextStream != null;
        }

        @Override
        public InputStream next() {
            if (!hasChecked && !hasNext()) {
                throw new NoSuchElementException();
            }
            hasChecked = false;
            InputStream result = nextStream;
            nextStream = null;
            return result;
        }

        private InputStream fetchNextStream() {
            final Object next;
            try {
                next = env.getRuntime().callMethod(iterator, "next", null);
            } catch (Exception e) {
                throw FtpUtil.createError("Failed to read iterator", e, FTP_ERROR);
            }
            if (next == null) {
                return null;
            }
            if (next instanceof BError err) {
                throw FtpUtil.createError("Iterator error: " + err.getMessage(), FTP_ERROR);
            }

            byte[] bytes = toBytes(next);
            if (bytes.length == 0) {
                return null;
            }
            isFirstRow = false;
            return new ByteArrayInputStream(bytes);
        }

        private byte[] toBytes(Object value) {
            // Each element is a record with a 'value' field.
            @SuppressWarnings("unchecked")
            BMap<BString, Object> streamRecord = (BMap<BString, Object>) value;
            Object val = streamRecord.get(FtpConstants.FIELD_VALUE);

            if (val instanceof BArray array) {
                return bytesFromArray(array);
            }
            @SuppressWarnings("unchecked")
            BMap<BString, Object> recordValue = (BMap<BString, Object>) val;
            return bytesFromRecord(recordValue, isFirstRow);
        }

        private static byte[] bytesFromArray(BArray array) {
            // If it's a byte[] just return it; else it's CSV row from string[]
            if (array.getElementType().getTag() == io.ballerina.runtime.api.types.TypeTags.BYTE_TAG) {
                return array.getBytes();
            }
            String csvRow = CSVUtils.convertArrayToCsvRow(array) + System.lineSeparator();
            return csvRow.getBytes(StandardCharsets.UTF_8);
        }

        private static byte[] bytesFromRecord(BMap<BString, Object> balRecord, boolean includeHeader) {
            String csvRow = CSVUtils.convertRecordToCsvRow(balRecord, includeHeader) + System.lineSeparator();
            return csvRow.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static Object putGenericAction(Environment env, BObject clientConnector, BString path, BString options,
                                           RemoteFileSystemMessage message) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector
                    = (VfsClientConnectorImpl) clientConnector.getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            String filePath = path.getValue();
            if (options.getValue().equals(FtpConstants.WRITE_OPTION_OVERWRITE)) {
                connector.send(message, FtpAction.PUT, filePath, null);
            } else {
                connector.send(message, FtpAction.APPEND, filePath, null);
            }
            return getResult(balFuture);
        });
    }

    public static Object delete(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.DELETE, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object isDirectory(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false, remoteFileSystemBaseMessage ->
                    FtpClientHelper.executeIsDirectoryAction(remoteFileSystemBaseMessage, balFuture));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.ISDIR, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object list(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false, remoteFileSystemBaseMessage ->
                    FtpClientHelper.executeListAction(remoteFileSystemBaseMessage, balFuture));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.LIST, filePath.getValue(), null);
            return getResult(balFuture);
        });

    }

    public static Object mkdir(Environment env, BObject clientConnector, BString path) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.MKDIR, path.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object rename(Environment env, BObject clientConnector, BString origin, BString destination) {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        String destinationUrl;
        try {
            propertyMap.put(FtpConstants.URI, FtpUtil.createUrl(clientConnector, origin.getValue()));
            propertyMap.put(FtpConstants.DESTINATION, FtpUtil.createUrl(clientConnector,
                    destination.getValue()));
            destinationUrl = FtpUtil.createUrl(clientConnector, destination.getValue());
        } catch (BallerinaFtpException e) {
            return FtpUtil.createError(e.getMessage(), Error.errorType());
        }
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.RENAME, origin.getValue(), destinationUrl);
            return getResult(balFuture);
        });
    }

    public static Object rmdir(Environment env, BObject clientConnector, BString filePath) {
        return env.yieldAndRun(() -> {
            CompletableFuture balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, true,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeGenericAction());
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.RMDIR, filePath.getValue(), null);
            return getResult(balFuture);
        });

    }

    public static Object size(Environment env, BObject clientConnector, BString filePath) {
        Map<String, String> propertyMap = new HashMap<>(
                (Map<String, String>) clientConnector.getNativeData(FtpConstants.PROPERTY_MAP));
        propertyMap.put(FtpConstants.PASSIVE_MODE, Boolean.TRUE.toString());
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            FtpClientListener connectorListener = new FtpClientListener(balFuture, false,
                    remoteFileSystemBaseMessage -> FtpClientHelper.executeSizeAction(remoteFileSystemBaseMessage,
                            balFuture));
            VfsClientConnectorImpl connector = (VfsClientConnectorImpl) clientConnector.
                    getNativeData(VFS_CLIENT_CONNECTOR);
            connector.addListener(connectorListener);
            connector.send(null, FtpAction.SIZE, filePath.getValue(), null);
            return getResult(balFuture);
        });
    }

    public static Object getResult(CompletableFuture<Object> balFuture) {
        try {
            return balFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ErrorCreator.createError(e);
        } catch (Throwable throwable) {
            return ErrorCreator.createError(throwable);
        }
    }

    public static void handleStreamEnd(BObject entity, BufferHolder bufferHolder) {
        entity.addNativeData(ENTITY_BYTE_STREAM, null);
        bufferHolder.setTerminal(true);
    }
}
