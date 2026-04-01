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

import com.fasterxml.jackson.annotation.JsonAnySetter;
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

    Map<String, Object> data = new HashMap<>();

    @JsonAnySetter
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
}
