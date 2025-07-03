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

import io.moderne.jsonrpc.internal.SnowflakeId;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A store for objects that maintains both forward (ID to object) and reverse (object to ID) mappings.
 * Uses identity semantics for object comparison.
 */
public class ObjectStore {
    private final Map<String, Object> objects;
    private final Map<Object, String> objectIds;
    private final Map<String, String> objectIdToVersion; // objectId -> currentVersionId

    public ObjectStore() {
        this.objects = new HashMap<>();
        this.objectIds = new IdentityHashMap<>();
        this.objectIdToVersion = new HashMap<>();
    }

    /**
     * Check if an object has an intrinsic ID property.
     */
    private boolean hasIntrinsicId(Object obj) {
        return obj instanceof Tree || obj instanceof Marker;
    }

    /**
     * Get the intrinsic ID from an object.
     */
    private @Nullable String getIntrinsicId(Object obj) {
        if (obj instanceof Tree) {
            return ((Tree) obj).getId().toString();
        } else if (obj instanceof Marker) {
            return ((Marker) obj).getId().toString();
        }
        return null;
    }

    /**
     * Create a composite ID from object ID and version ID.
     */
    private String createCompositeId(String objectId, String versionId) {
        return objectId + "@" + versionId;
    }

    /**
     * Parse a composite ID into object ID and version ID.
     * Returns null if not a composite ID.
     */
    private @Nullable CompositeId parseCompositeId(String compositeId) {
        int atIndex = compositeId.indexOf('@');
        if (atIndex == -1) {
            return null;
        }
        return new CompositeId(
            compositeId.substring(0, atIndex),
            compositeId.substring(atIndex + 1)
        );
    }

    /**
     * Get the current version ID for an object ID.
     */
    public @Nullable String getCurrentVersion(String objectId) {
        return objectIdToVersion.get(objectId);
    }

    /**
     * Helper class to hold parsed composite ID components.
     */
    @RequiredArgsConstructor
    private static class CompositeId {
        final String objectId;
        final String versionId;
    }

    /**
     * Store an object with an optional ID. If no ID is provided, a new one will be generated.
     * For objects with intrinsic IDs, creates composite IDs with versioning.
     * @param obj The object to store
     * @param id The optional ID to use
     * @return The ID used to store the object
     */
    public String store(Object obj, @Nullable String id) {
        String currentId = objectIds.get(obj);
        String intrinsicId = getIntrinsicId(obj);
        
        if (currentId == null) {
            // Object not yet stored
            String actualId;
            
            if (intrinsicId != null && id == null) {
                // Object has intrinsic ID, create composite ID with new version
                String versionId = SnowflakeId.generateId();
                actualId = createCompositeId(intrinsicId, versionId);
                objectIdToVersion.put(intrinsicId, versionId);
            } else if (intrinsicId != null) {
                // Object has intrinsic ID and specific ID provided
                CompositeId parsed = parseCompositeId(id);
                if (parsed != null && parsed.objectId.equals(intrinsicId)) {
                    actualId = id;
                    objectIdToVersion.put(intrinsicId, parsed.versionId);
                } else {
                    // Provided ID doesn't match intrinsic ID, create new composite
                    String versionId = SnowflakeId.generateId();
                    actualId = createCompositeId(intrinsicId, versionId);
                    objectIdToVersion.put(intrinsicId, versionId);
                }
            } else {
                // Object has no intrinsic ID, use simple Snowflake ID
                actualId = id != null ? id : SnowflakeId.generateId();
            }
            
            objects.put(actualId, obj);
            objectIds.put(obj, actualId);
            return actualId;
        } else if (!currentId.equals(id) && id != null) {
            // Object already stored with different ID, update it
            objects.remove(currentId);
            objects.put(id, obj);
            objectIds.put(obj, id);
            
            // Update version mapping if object has intrinsic ID
            if (intrinsicId != null) {
                CompositeId parsed = parseCompositeId(id);
                if (parsed != null && parsed.objectId.equals(intrinsicId)) {
                    objectIdToVersion.put(intrinsicId, parsed.versionId);
                }
            }
            
            return id;
        } else {
            // Object already stored with same ID or no new ID provided
            return currentId;
        }
    }

    /**
     * Store an object with an auto-generated ID.
     * @param obj The object to store
     * @return The generated ID
     */
    public String store(Object obj) {
        return store(obj, null);
    }

    /**
     * Get an object by its ID.
     * @param id The ID of the object
     * @return The object or null if not found
     */
    public @Nullable Object get(String id) {
        return objects.get(id);
    }

    /**
     * Get the ID associated with an object.
     * @param obj The object
     * @return The ID or null if the object is not stored
     */
    public @Nullable String getId(Object obj) {
        return objectIds.get(obj);
    }

    /**
     * Check if an object is stored.
     * @param obj The object to check
     * @return true if the object is stored
     */
    public boolean has(Object obj) {
        return objectIds.containsKey(obj);
    }

    /**
     * Check if an ID exists in the store.
     * @param id The ID to check
     * @return true if the ID exists
     */
    public boolean hasId(String id) {
        return objects.containsKey(id);
    }

    /**
     * Remove an object by its ID.
     * @param id The ID of the object to remove
     * @return true if the object was removed, false if not found
     */
    public boolean remove(String id) {
        Object obj = objects.remove(id);
        if (obj != null) {
            objectIds.remove(obj);
            return true;
        }
        return false;
    }

    /**
     * Clear all stored objects.
     */
    public void clear() {
        objects.clear();
        objectIds.clear();
        objectIdToVersion.clear();
    }

    /**
     * Get the number of stored objects.
     * @return The number of objects
     */
    public int size() {
        return objects.size();
    }
}
