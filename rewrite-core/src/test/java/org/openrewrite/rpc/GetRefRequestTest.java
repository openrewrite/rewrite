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

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.marker.Markers;
import org.openrewrite.rpc.request.GetObject;
import org.openrewrite.rpc.request.GetRef;
import org.openrewrite.rpc.request.GetRefResponse;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused tests for GetRef functionality and independent cache management.
 * These tests specifically verify the new GetRef and lastKnownId features
 * without triggering broader RPC deserialization issues.
 */
class GetRefRequestTest {
    Environment env = Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.text")
            .build();

    RewriteRpc client;
    RewriteRpc server;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        client = RewriteRpc.from(new JsonRpc(new HeaderDelimitedMessageHandler(clientIn, clientOut)), env)
                .timeout(Duration.ofMinutes(10))
                .build()
                .batchSize(1);

        server = RewriteRpc.from(new JsonRpc(new HeaderDelimitedMessageHandler(serverIn, serverOut)), env)
                .timeout(Duration.ofMinutes(10))
                .build()
                .batchSize(1);
    }

    @AfterEach
    void after() {
        client.close();
        server.close();
    }

    @Test
    void getRefRequestForMissingReference() {
        // Request a reference that doesn't exist
        GetRef getRefRequest = new GetRef(999);
        GetRefResponse response = server.send("GetRef", getRefRequest, GetRefResponse.class);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.DELETE);
        assertThat(response.get(1).getState()).isEqualTo(RpcObjectData.State.END_OF_OBJECT);
    }

    @Test
    void getObjectConstructorAcceptsLastKnownId() {
        // Test that GetObject constructor accepts lastKnownId parameter
        GetObject request1 = new GetObject("object-id", null);
        assertThat(request1.getId()).isEqualTo("object-id");
        assertThat(request1.getLastKnownId()).isNull();

        GetObject request2 = new GetObject("object-id", "last-known-id");
        assertThat(request2.getId()).isEqualTo("object-id");
        assertThat(request2.getLastKnownId()).isEqualTo("last-known-id");
    }

    @Test
    void cacheManagementMethods() {
        // Test that cache maps are accessible for testing
        assertThat(server.localObjects).isNotNull();
        assertThat(server.remoteObjects).isNotNull();
        assertThat(server.localRefs).isNotNull();
        assertThat(server.remoteRefs).isNotNull();

        // Test that we can manipulate caches independently
        server.remoteObjects.put("test", "value");
        assertThat(server.remoteObjects).containsKey("test");

        server.remoteObjects.clear();
        assertThat(server.remoteObjects).isEmpty();

        // Client should be unaffected
        assertThat(client.remoteObjects).isEmpty();
    }

    @Test
    void localRefsManagement() {
        // Create a marker and assign it a ref ID
        Markers markers = Markers.build(java.util.List.of());
        server.localRefs.put(markers, 456);

        // Verify it's stored
        assertThat(server.localRefs).containsKey(markers);
        assertThat(server.localRefs.get(markers)).isEqualTo(456);

        // Clear and verify independence
        server.localRefs.clear();
        assertThat(server.localRefs).isEmpty();
        assertThat(client.localRefs).isEmpty();
    }
}
