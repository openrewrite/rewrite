package org.openrewrite.maven.attributes;

import java.util.Map;
import java.util.Optional;

public interface Attributed {

    Map<String, Attribute> getAttributes();

    /**
     * Look up an attribute by its class. null if no attribute with that class exists.
     */
    default <T extends Attribute> Optional<T> findAttribute(Class<T> clazz) {
        //noinspection unchecked
        return (Optional<T>) Optional.ofNullable(getAttributes().get(clazz.getName()));
    }
}
