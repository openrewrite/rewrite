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

import org.junit.jupiter.api.Test;
import org.openrewrite.rpc.request.GetObject;
import org.openrewrite.rpc.request.GetRef;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.Markers.EMPTY;

class IndependentCacheManagementTest {

    @Test
    void getObjectWithoutLastKnownId_returnsADD() {
        // Setup local and remote objects
        Map<String, Object> remoteObjects = new HashMap<>();
        Map<String, Object> localObjects = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Create object on server
        String objectId = "test-object-id";
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath(Paths.get("test.txt"))
                .build();

        localObjects.put(objectId, text);

        // Simulate GetObject request without lastKnownId (null)
        TestableGetObjectHandler handler = new TestableGetObjectHandler(new AtomicInteger(1), remoteObjects,
                localObjects, localRefs, new AtomicBoolean(false));

        List<RpcObjectData> response = handler.handle(new GetObject(objectId, null));

        // First data should be ADD state
        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().getState()).isEqualTo(RpcObjectData.State.ADD);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasNoEntry_returnsADD() {
        // Setup local and remote objects
        Map<String, Object> remoteObjects = new HashMap<>();
        Map<String, Object> localObjects = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Create object on server
        String objectId = "test-object-id";
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath(Paths.get("test.txt"))
                .build();

        localObjects.put(objectId, text);
        // Intentionally don't put anything in remoteObjects to simulate cache miss

        // Simulate GetObject request with lastKnownId 
        TestableGetObjectHandler handler = new TestableGetObjectHandler(new AtomicInteger(1), remoteObjects,
                localObjects, localRefs, new AtomicBoolean(false));

        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));

        // Should return ADD since remote has no entry for this ID
        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().getState()).isEqualTo(RpcObjectData.State.ADD);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasSameEntry_returnsNOCHANGE() {
        // Setup local and remote objects
        Map<String, Object> remoteObjects = new HashMap<>();
        Map<String, Object> localObjects = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Create object on server
        String objectId = "test-object-id";
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Hello World")
                .markers(EMPTY)
                .sourcePath(Paths.get("test.txt"))
                .build();

        localObjects.put(objectId, text);
        remoteObjects.put(objectId, text); // Same object in remote

        // Simulate GetObject request with lastKnownId 
        TestableGetObjectHandler handler = new TestableGetObjectHandler(new AtomicInteger(1), remoteObjects,
                localObjects, localRefs, new AtomicBoolean(false));

        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));

        // Should return NO_CHANGE since objects are identical
        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().getState()).isEqualTo(RpcObjectData.State.NO_CHANGE);
    }

    @Test
    void getObjectWithLastKnownId_whenRemoteHasDifferentEntry_returnsCHANGE() {
        // Setup local and remote objects
        Map<String, Object> remoteObjects = new HashMap<>();
        Map<String, Object> localObjects = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Create original object on server
        String objectId = "test-object-id";
        PlainText originalText = PlainText.builder()
                .id(randomId())
                .text("Original Text")
                .markers(EMPTY)
                .sourcePath(Paths.get("test.txt"))
                .build();

        // Create modified object 
        PlainText modifiedText = originalText.withText("Modified Text");

        localObjects.put(objectId, modifiedText);
        remoteObjects.put(objectId, originalText); // Different object in remote

        // Simulate GetObject request with lastKnownId 
        TestableGetObjectHandler handler = new TestableGetObjectHandler(new AtomicInteger(1), remoteObjects,
                localObjects, localRefs, new AtomicBoolean(false));

        List<RpcObjectData> response = handler.handle(new GetObject(objectId, objectId));

        // Should return CHANGE since objects are different
        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().getState()).isEqualTo(RpcObjectData.State.CHANGE);
    }

    @Test
    void getRefRequest_whenRefExists_returnsCorrectObject() throws InterruptedException {
        Map<Integer, Object> remoteRefs = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Create a reference on server
        PlainText text = PlainText.builder()
                .id(randomId())
                .text("Referenced Text")
                .markers(EMPTY)
                .sourcePath(Paths.get("ref.txt"))
                .build();

        int refId = 42;
        localRefs.put(text, refId);

        // Test GetRef request
        GetRef.Handler handler = new GetRef.Handler(remoteRefs, localRefs, new AtomicInteger(1000), new AtomicBoolean(false));
        List<RpcObjectData> response = handler.handle(new GetRef(refId));

        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().getState()).isEqualTo(RpcObjectData.State.ADD);
        // For RpcCodec objects like PlainText, the first RpcObjectData has null value 
        // and the actual data is in subsequent RpcObjectData objects
        assertThat((Object) response.getFirst().getValue()).isNull();
        assertThat(response.getFirst().getValueType()).isEqualTo("org.openrewrite.text.PlainText");

        // Should end with END_OF_OBJECT
        assertThat(response.getLast().getState()).isEqualTo(RpcObjectData.State.END_OF_OBJECT);
    }

    @Test
    void getRefRequest_whenRefDoesNotExist_returnsDELETE() throws InterruptedException {
        Map<Integer, Object> remoteRefs = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Test GetRef request for non-existent reference
        GetRef.Handler handler = new GetRef.Handler(remoteRefs, localRefs, new AtomicInteger(1), new AtomicBoolean(false));
        List<RpcObjectData> response = handler.handle(new GetRef(999));

        assertThat(response).hasSize(2); // DELETE + END_OF_OBJECT
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.DELETE);
        assertThat(response.get(1).getState()).isEqualTo(RpcObjectData.State.END_OF_OBJECT);
    }

    @Test
    void getRefRequest_whenInvalidRefId_returnsDELETE() throws InterruptedException {
        Map<Integer, Object> remoteRefs = new HashMap<>();
        IdentityHashMap<Object, Integer> localRefs = new IdentityHashMap<>();

        // Test GetRef request with invalid ref ID format
        GetRef.Handler handler = new GetRef.Handler(remoteRefs, localRefs, new AtomicInteger(1), new AtomicBoolean(false));
        List<RpcObjectData> response = handler.handle(new GetRef(-1));

        assertThat(response).hasSize(2); // DELETE + END_OF_OBJECT
        assertThat(response.get(0).getState()).isEqualTo(RpcObjectData.State.DELETE);
        assertThat(response.get(1).getState()).isEqualTo(RpcObjectData.State.END_OF_OBJECT);
    }

    // Helper class to access protected method
    private static class TestableGetObjectHandler extends GetObject.Handler {

        TestableGetObjectHandler(AtomicInteger batchSize, Map<String, Object> remoteObjects,
                Map<String, Object> localObjects, IdentityHashMap<Object, Integer> localRefs,
                AtomicBoolean trace) {
            super(batchSize, remoteObjects, localObjects, localRefs, trace);
        }

        @Override
        public List<RpcObjectData> handle(GetObject request) {
            try {
                return super.handle(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
