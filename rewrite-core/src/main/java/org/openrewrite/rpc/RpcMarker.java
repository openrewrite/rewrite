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
package org.openrewrite.rpc;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Value
public class RpcMarker implements Marker {
    @With
    UUID id;

    @JsonIgnore
    Map<String, Object> data = new HashMap<>();

    /**
     * The fields of the marker, which are written beside its id, as they were read.
     */
    @JsonAnyGetter
    public Map<String, Object> getData() {
        // They were once written under "data" instead, which is how LSTs stored then still hold them.
        Object stored = data.size() == 1 ? data.get("data") : null;
        //noinspection unchecked
        return stored instanceof Map ? (Map<String, Object>) stored : data;
    }

    public void addData(String key, Object value) {
        getData().put(key, value);
    }

    @JsonAnySetter
    private void read(String key, Object value) {
        data.put(key, value);
    }

    // Ignoring the field would otherwise have a "data" property skipped rather than read.
    @JsonProperty("data")
    private void readData(Object value) {
        data.put("data", value);
    }
}
