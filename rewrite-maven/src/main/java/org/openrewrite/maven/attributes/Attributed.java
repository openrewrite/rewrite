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
package org.openrewrite.maven.attributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Container for metadata mapped from foreign tools.
 * The slightly strange pattern of storing the data as a map of strings and retrieving it as something more strongly typed
 * is for ease of serialization/deserialization without having to reconfigure all of our jackson serializers to accept a polymorphic map of String -> Attribute
 */
public interface Attributed {

    Map<String, String> getAttributes();

    /**
     * Look up an attribute by class.
     * Only works for attributes which expose "key" and "from" static methods.
     */
    default <T extends Attribute> Optional<T> findAttribute(Class<T> clazz) {
        try {
            Method keyMethod = clazz.getMethod("key");
            String key = (String) keyMethod.invoke(null);
            String value = getAttributes().get(key);
            if (value == null) {
                return Optional.empty();
            }
            Method from = clazz.getMethod("from", String.class);
            //noinspection unchecked
            return Optional.ofNullable((T)from.invoke(null, value));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return Optional.empty();
        }
    }
}
