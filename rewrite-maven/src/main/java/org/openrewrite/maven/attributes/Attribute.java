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

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;

/**
 * Represents metadata from build systems external to OpenRewrite.
 * Stored in a map of strings to strings for simplicity of
 */
public interface Attribute extends Serializable {

    /**
     * Subclasses are expected to have a key method with this signature.
     * Used to know which entries in a map correspond to elements of the type of the attribute.
     * When the attribute's origin is an external system, such as Gradle's HasAttributes class, the key returned by
     * this should be the same key used in the foreign attribute system.
     */
    static String key() {
        throw new IllegalStateException("Subclass expected to implement this method does not");
    }


    /**
     * Subclasses are expected to have a from() method which translates a string into that subtype of attribute, or null
     */
    static @Nullable Optional<Attribute> from(@Nullable String value) {
        throw new IllegalStateException("Subclass expected to implement this method does not");
    }
}
