package org.openrewrite.java.internal;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;

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
    public Object resolveId(ObjectIdGenerator.IdKey id) {
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
