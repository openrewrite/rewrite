/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc.request;

import lombok.Value;

import java.util.List;

/**
 * Tells a remote RPC peer where (and in what format) to write the data table rows that
 * recipes it executes produce, so the peer's data tables land alongside the orchestrator's
 * in the same {@code datatables/} directory. Sent once per {@link org.openrewrite.ExecutionContext}
 * (identified by {@link #pId}) before any visit, and only to peers that
 * {@linkplain org.openrewrite.rpc.RewriteRpc#supportsDataTableConfig() advertise support}.
 * <p>
 * Columns are passed as parallel name/value lists to preserve their order across
 * language-specific JSON deserializers.
 */
@Value
public class ConfigureDataTables implements RpcRequest {
    String pId;

    /**
     * Absolute path of the directory the peer should write its CSV files into.
     */
    String outputDir;

    /**
     * File extension including the leading dot, e.g. {@code .csv} or {@code .csv.gz}.
     * A {@code .gz} suffix instructs the peer to GZIP its output.
     */
    String fileExtension;

    List<String> prefixColumnNames;
    List<String> prefixColumnValues;
    List<String> suffixColumnNames;
    List<String> suffixColumnValues;
}
