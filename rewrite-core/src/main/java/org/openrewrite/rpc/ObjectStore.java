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
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * A store for objects that maintains both forward (ID to object) and reverse (object to ID) mappings.
 * Uses identity semantics for object comparison.
 */
public class ObjectStore {
    private final Map<String, Object> objects;
    private final Map<Object, String> objectIds;

    public ObjectStore() {
        this.objects = new HashMap<>();
        this.objectIds = new IdentityHashMap<>();
    }

    /**
     * Store an object with an optional ID. If no ID is provided, a new one will be generated.
     * @param obj The object to store
     * @param id The optional ID to use
     * @return The ID used to store the object
     */
    public String store(Object obj, @Nullable String id) {
        String currentId = objectIds.get(obj);
        
        if (currentId == null) {
            // Object not yet stored
            if (id == null) {
                id = SnowflakeId.generateId();
            }
            objects.put(id, obj);
            objectIds.put(obj, id);
        } else if (!currentId.equals(id) && id != null) {
            // Object already stored with different ID, update it
            objects.remove(currentId);
            objects.put(id, obj);
            objectIds.put(obj, id);
        } else {
            // Object already stored with same ID or no new ID provided
            id = currentId;
        }
        
        return id;
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
    @Nullable
    public Object get(String id) {
        return objects.get(id);
    }

    /**
     * Get the ID associated with an object.
     * @param obj The object
     * @return The ID or null if the object is not stored
     */
    @Nullable
    public String getId(Object obj) {
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
    }

    /**
     * Get the number of stored objects.
     * @return The number of objects
     */
    public int size() {
        return objects.size();
    }
}