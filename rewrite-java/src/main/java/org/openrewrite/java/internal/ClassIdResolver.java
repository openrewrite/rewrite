/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.internal;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ClassIdResolver implements ObjectIdResolver {
    protected Map<ObjectIdGenerator.IdKey, Object> items;

    @Override
    public void bindItem(ObjectIdGenerator.IdKey id, Object ob) {
        if (items == null) {
            items = new HashMap<>();
        }
        items.put(id, ob);
    }

    @Override
    public @Nullable Object resolveId(ObjectIdGenerator.IdKey id) {
        return (items == null) ? null : items.get(id);
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }

    @Override
    public ObjectIdResolver newForDeserialization(Object context) {
        return new ClassIdResolver();
    }
}
