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
import io.moderne.jsonrpc.MockConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.request.GetObject;
import org.openrewrite.rpc.request.GetRef;
import org.openrewrite.text.PlainText;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.Markers.EMPTY;

class IndependentCacheManagementTest {

    private RewriteRpc serverRpc;
    private RewriteRpc clientRpc;
    private MockConnection serverConnection;
    private MockConnection clientConnection;

    @BeforeEach
    void setUp() {
        serverConnection = new MockConnection();
        clientConnection = new MockConnection();
        
        // Connect the mock connections to each other
        serverConnection.setRemote(clientConnection);
        clientConnection.setRemote(serverConnection);
        
        Environment env = Environment.builder().build();
        serverRpc = RewriteRpc.from(new JsonRpc(serverConnection), env).build();
        clientRpc = RewriteRpc.from(new JsonRpc(clientConnection), env).build();
    }

    @Test
    void getObjectWithoutLastKnownId_returnsADD() {
        // Create object on server
        String objectId = UUID.randomUUID().toString();
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath("test.txt")
                .build();
        
        serverRpc.localObjects.put(objectId, text);

        // Simulate GetObject request without lastKnownId (null)
        GetObject.Handler handler = new GetObject.Handler(1, serverRpc.remoteObjects, 
                serverRpc.localObjects, serverRpc.localRefs, false);
        
        List<RpcObjectData> response = handler.handle(new GetObject(objectId, null));
        
        // First data should be ADD state
        assertThat(response).isNotEmpty();
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.ADD);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasNoEntry_returnsADD() {
        // Create object on server
        String objectId = UUID.randomUUID().toString();
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath("test.txt")
                .build();
        
        serverRpc.localObjects.put(objectId, text);
        // Intentionally don't put anything in remoteObjects to simulate cache miss

        // Simulate GetObject request with lastKnownId 
        GetObject.Handler handler = new GetObject.Handler(1, serverRpc.remoteObjects, 
                serverRpc.localObjects, serverRpc.localRefs, false);
        
        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));
        
        // Should return ADD since remote has no entry for this ID
        assertThat(response).isNotEmpty();
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.ADD);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasSameEntry_returnsNOCHANGE() {
        // Create object on server
        String objectId = UUID.randomUUID().toString();
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath("test.txt")
                .build();
        
        serverRpc.localObjects.put(objectId, text);
        serverRpc.remoteObjects.put(objectId, text); // Same object in remote

        // Simulate GetObject request with lastKnownId 
        GetObject.Handler handler = new GetObject.Handler(1, serverRpc.remoteObjects, 
                serverRpc.localObjects, serverRpc.localRefs, false);
        
        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));
        
        // Should return NO_CHANGE since objects are identical
        assertThat(response).isNotEmpty();
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.NO_CHANGE);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasDifferentEntry_returnsCHANGE() {
        // Create original object on server
        String objectId = UUID.randomUUID().toString();
        PlainText originalText = PlainText.builder()
                .id(randomId())
                .text("Original Text")
                .markers(EMPTY)
                .sourcePath("test.txt")
                .build();
        
        // Create modified object 
        PlainText modifiedText = originalText.withText("Modified Text");
        
        serverRpc.localObjects.put(objectId, modifiedText);
        serverRpc.remoteObjects.put(objectId, originalText); // Different object in remote

        // Simulate GetObject request with lastKnownId 
        GetObject.Handler handler = new GetObject.Handler(1, serverRpc.remoteObjects, 
                serverRpc.localObjects, serverRpc.localRefs, false);
        
        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));
        
        // Should return CHANGE since objects are different
        assertThat(response).isNotEmpty();
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.CHANGE);
    }

    @Test
    void getRefRequest_whenRefExists_returnsCorrectObject() {
        // Create a reference on server
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Referenced Text")
                .markers(EMPTY)
                .sourcePath("ref.txt")
                .build();
        
        Integer refId = 42;
        serverRpc.localRefs.put(text, refId);

        // Test GetRef request
        GetRef.Handler handler = new GetRef.Handler(serverRpc.localRefs);
        RpcObjectData response = handler.handle(new GetRef(refId.toString()));
        
        assertThat(response.getState()).isEqualTo(RpcObjectData.State.ADD);
        assertThat(response.getValue()).isEqualTo(text);
    }

    @Test
    void getRefRequest_whenRefDoesNotExist_returnsDELETE() {
        // Test GetRef request for non-existent reference
        GetRef.Handler handler = new GetRef.Handler(serverRpc.localRefs);
        RpcObjectData response = handler.handle(new GetRef("999"));
        
        assertThat(response.getState()).isEqualTo(RpcObjectData.State.DELETE);
        assertThat(response.getValue()).isNull();
    }

    @Test
    void independentCacheEviction_doesNotAffectOtherSide() {
        // Create objects on both sides
        String objectId1 = UUID.randomUUID().toString();
        String objectId2 = UUID.randomUUID().toString();
        
        PlainText text1 = PlainText.builder()
                .id(randomId())
                .text("Server Object")
                .markers(EMPTY)
                .sourcePath("server.txt")
                .build();
        
        PlainText text2 = PlainText.builder()
                .id(randomId())
                .text("Client Object")
                .markers(EMPTY)
                .sourcePath("client.txt")
                .build();
        
        serverRpc.localObjects.put(objectId1, text1);
        clientRpc.localObjects.put(objectId2, text2);

        // Establish some remote state
        serverRpc.remoteObjects.put(objectId2, text2);
        clientRpc.remoteObjects.put(objectId1, text1);

        // Client evicts its caches
        clientRpc.remoteObjects.clear();
        clientRpc.remoteRefs.clear();

        // Server should still have its objects and remote state
        assertThat(serverRpc.localObjects.get(objectId1)).isEqualTo(text1);
        assertThat(serverRpc.remoteObjects.get(objectId2)).isEqualTo(text2);
    }
}