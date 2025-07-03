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
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class ObjectStoreTest {
    
    @Test
    void objectsWithIntrinsicIdCreateCompositeIds() {
        ObjectStore store = new ObjectStore();
        
        PlainText text = new PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, "UTF-8", false, null, null, "content", Collections.emptyList());
        String compositeId = store.store(text);
        
        // Should create composite ID: uuid@snowflake
        assertThat(compositeId).contains("@");
        assertThat(compositeId).startsWith(text.getId().toString() + "@");
        
        // Version should be tracked
        String version = store.getCurrentVersion(text.getId().toString());
        assertThat(version).isNotNull();
        assertThat(compositeId).isEqualTo(text.getId().toString() + "@" + version);
        
        // Can retrieve by composite ID
        assertThat(store.get(compositeId)).isSameAs(text);
    }
    
    @Test
    void objectsWithoutIntrinsicIdUseSimpleIds() {
        ObjectStore store = new ObjectStore();
        
        String simpleObject = "test data";
        String id = store.store(simpleObject);
        
        // Should not contain @ symbol
        assertThat(id).doesNotContain("@");
        
        // No version tracking for objects without intrinsic ID
        assertThat(store.getCurrentVersion("any-id")).isNull();
        
        // Can retrieve by ID
        assertThat(store.get(id)).isSameAs(simpleObject);
    }
    
    @Test
    void versionChangesWhenStoringNewInstanceWithSameIntrinsicId() {
        ObjectStore store = new ObjectStore();
        
        UUID intrinsicId = randomId();
        PlainText text1 = new PlainText(intrinsicId, Paths.get("test.txt"), Markers.EMPTY, "UTF-8", false, null, null, "version 1", Collections.emptyList());
        String id1 = store.store(text1);
        String version1 = store.getCurrentVersion(intrinsicId.toString());
        
        // Store new instance with same intrinsic ID
        PlainText text2 = new PlainText(intrinsicId, Paths.get("test.txt"), Markers.EMPTY, "UTF-8", false, null, null, "version 2", Collections.emptyList());
        String id2 = store.store(text2);
        String version2 = store.getCurrentVersion(intrinsicId.toString());
        
        // Versions should be different
        assertThat(version2).isNotEqualTo(version1);
        assertThat(id2).isNotEqualTo(id1);
        assertThat(id2).isEqualTo(intrinsicId.toString() + "@" + version2);
        
        // Both versions can be retrieved
        assertThat(store.get(id1)).isSameAs(text1);
        assertThat(store.get(id2)).isSameAs(text2);
    }
    
    @Test
    void markerObjectsUseCompositeIds() {
        ObjectStore store = new ObjectStore();
        
        TestMarker marker = new TestMarker(randomId(), "test marker");
        String compositeId = store.store(marker);
        
        // Should create composite ID for Marker objects
        assertThat(compositeId).contains("@");
        assertThat(compositeId).startsWith(marker.getId().toString() + "@");
        
        // Version should be tracked
        String version = store.getCurrentVersion(marker.getId().toString());
        assertThat(version).isNotNull();
        assertThat(compositeId).isEqualTo(marker.getId().toString() + "@" + version);
    }
    
    @Test
    void storeWithExplicitCompositeId() {
        ObjectStore store = new ObjectStore();
        
        UUID intrinsicId = randomId();
        PlainText text = new PlainText(intrinsicId, Paths.get("test.txt"), Markers.EMPTY, "UTF-8", false, null, null, "content", Collections.emptyList());
        
        // Store with explicit composite ID
        String explicitId = intrinsicId.toString() + "@custom-version-123";
        String storedId = store.store(text, explicitId);
        
        assertThat(storedId).isEqualTo(explicitId);
        assertThat(store.getCurrentVersion(intrinsicId.toString())).isEqualTo("custom-version-123");
        assertThat(store.get(explicitId)).isSameAs(text);
    }
    
    static class TestMarker implements Marker {
        private final UUID id;
        private final String value;
        
        TestMarker(UUID id, String value) {
            this.id = id;
            this.value = value;
        }
        
        @Override
        public UUID getId() {
            return id;
        }
        
        @Override
        public <M extends Marker> M withId(UUID id) {
            return (M) new TestMarker(id, this.value);
        }
        
        public String getValue() {
            return value;
        }
    }
}