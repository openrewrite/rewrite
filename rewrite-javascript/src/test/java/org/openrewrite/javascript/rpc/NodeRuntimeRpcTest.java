/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.rpc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeRuntime;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NodeRuntimeRpcTest {

    private static final NodeRuntime RUNTIME = new NodeRuntime(UUID.randomUUID(), "v24.19.0", "linux", "x64", "musl",
            PackageManager.Npm, "11.17.0");

    @Test
    void everyFieldSurvivesTheTrip() {
        assertThat(sendAndReceive(RUNTIME, null)).isEqualTo(RUNTIME);
    }

    @Test
    void absentFieldsStayAbsent() {
        NodeRuntime runtime = new NodeRuntime(UUID.randomUUID(), "v22.12.0", "darwin", "arm64", null, null, null);

        assertThat(sendAndReceive(runtime, null)).isEqualTo(runtime);
    }

    @Test
    void aChangeIsReceivedOverTheMarkerItChanges() {
        NodeRuntime changed = RUNTIME.withPackageManager(PackageManager.Pnpm).withPackageManagerVersion("10.18.3")
                .withLibc(null);

        assertThat(sendAndReceive(changed, RUNTIME)).isEqualTo(changed);
    }

    // LSTs are written with Jackson as well
    @Test
    void survivesJackson() throws Exception {
        ObjectMapper mapper = mapper();

        assertThat(mapper.readValue(mapper.writeValueAsString(RUNTIME), NodeRuntime.class)).isEqualTo(RUNTIME);
    }

    private static NodeRuntime sendAndReceive(NodeRuntime after, @Nullable NodeRuntime before) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(after, before, null);
        sq.flush();

        // The wire carries a UUID and an enum as strings, as the transport's JSON encoding would.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            for (RpcObjectData data : batches.removeFirst()) {
                Object value = data.getValue();
                all.add(value instanceof UUID || value instanceof Enum ?
                        new RpcObjectData(data.getState(), data.getValueType(), value instanceof Enum ?
                                ((Enum<?>) value).name() : value.toString(), data.getRef(), false) :
                        data);
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);

        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null).receive(before);
    }

    // the mapper configuration that serializes LSTs and recipe edits
    private static ObjectMapper mapper() {
        ObjectMapper m = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
                .build();
        m.registerModule(new ParameterNamesModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return m;
    }
}
