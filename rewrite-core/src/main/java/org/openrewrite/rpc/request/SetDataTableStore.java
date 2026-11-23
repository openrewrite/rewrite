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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.moderne.jsonrpc.JsonRpcMethod;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.CsvDataTableStore;
import org.openrewrite.DataTableStore;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;

/**
 * Conveys a {@link DataTableStore}'s configuration to a remote runtime so recipes there
 * write data table rows where the host wants. Like {@link Print.MarkerPrinter}, only a
 * closed set of mirrored implementations crosses the wire; rows never do.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SetDataTableStore.Csv.class, name = "CSV"),
        @JsonSubTypes.Type(value = SetDataTableStore.NoOp.class, name = "NOOP")
})
public interface SetDataTableStore extends RpcRequest {

    DataTableStore toDataTableStore();

    /**
     * @return the wire form, or {@code null} when {@code store} is not RPC-conveyable
     * (the remote then keeps its own default store).
     */
    static @Nullable SetDataTableStore from(@Nullable DataTableStore store) {
        if (store == null) {
            return null;
        }
        if (store == DataTableStore.noop()) {
            return new NoOp();
        }
        if (store instanceof CsvDataTableStore) {
            CsvDataTableStore csv = (CsvDataTableStore) store;
            return new Csv(
                    csv.getOutputDir().toAbsolutePath().normalize().toString(),
                    new LinkedHashMap<>(csv.getPrefixColumns()),
                    new LinkedHashMap<>(csv.getSuffixColumns()));
        }
        return null;
    }

    @Value
    class Csv implements SetDataTableStore {
        String outputDir;

        @Nullable
        Map<String, String> prefixColumns;

        @Nullable
        Map<String, String> suffixColumns;

        @Override
        public DataTableStore toDataTableStore() {
            Map<String, String> prefix = prefixColumns != null ? prefixColumns : emptyMap();
            Map<String, String> suffix = suffixColumns != null ? suffixColumns : emptyMap();
            return new CsvDataTableStore(Paths.get(outputDir), prefix, suffix);
        }
    }

    @Value
    class NoOp implements SetDataTableStore {
        @Override
        public DataTableStore toDataTableStore() {
            return DataTableStore.noop();
        }
    }

    @RequiredArgsConstructor
    class Handler extends JsonRpcMethod<SetDataTableStore> {
        private final Consumer<DataTableStore> installStore;

        @Override
        protected Object handle(SetDataTableStore request) {
            installStore.accept(request.toDataTableStore());
            return true;
        }
    }
}
