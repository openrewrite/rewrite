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
package org.openrewrite.gradle.attributes;

import org.gradle.api.Named;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.DependencyAttribute;

/**
 * Attribute representing the technical elements of a  <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/LibraryElements.html">library variant</a>.
 */
public enum LibraryElements implements DependencyAttribute {
    /**
     * The JVM classes format
     */
    CLASSES,

    /**
     * The JVM class files and resources
     */
    CLASSES_AND_RESOURCES,

    /**
     * Dynamic libraries for native modules
     */
    DYNAMIC_LIB,

    /**
     * Header files for C++
     */
    HEADERS_CPLUSPLUS,

    /**
     * The JVM archive format
     */
    JAR,

    /**
     * Link archives for native modules
     */
    LINK_ARCHIVE,
    /**
     * Objects for native modules
     */
    OBJECTS,

    /**
     * JVM resources
     */
    RESOURCES;

    public static @Nullable LibraryElements from(org.gradle.api.artifacts.Dependency dependency) {
        if (!(dependency instanceof ModuleDependency)) {
            return null;
        }
        return from(((ModuleDependency) dependency).getAttributes());
    }

    public static @Nullable LibraryElements from(org.gradle.api.attributes.AttributeContainer attributes) {
        try {
            return from((Named) attributes.getAttribute(Attribute.of(Class.forName("org.gradle.api.attributes.LibraryElements"))));
        } catch (ClassCastException | ClassNotFoundException e) {
            return null;
        }
    }

    public static @Nullable LibraryElements from(@Nullable Named libraryElements) {
        if (libraryElements == null) {
            return null;
        }
        switch (libraryElements.getName()) {
            case "classes":
                return LibraryElements.CLASSES;
            case "classes+resources":
                return LibraryElements.CLASSES_AND_RESOURCES;
            case "dynamic-lib":
                return LibraryElements.DYNAMIC_LIB;
            case "headers-cplusplus":
                return LibraryElements.HEADERS_CPLUSPLUS;
            case "jar":
                return LibraryElements.JAR;
            case "link-archive":
                return LibraryElements.LINK_ARCHIVE;
            case "objects":
                return LibraryElements.OBJECTS;
            case "resources":
                return LibraryElements.RESOURCES;
            default:
                return null;
        }
    }
}
