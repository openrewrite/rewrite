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
package org.openrewrite.gradle.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyTest {
    @Test
    void toStringNotationWithAllFieldsPresent() {
        Dependency dep = new Dependency("com.example", "artifact", "1.0.0", "sources", "jar");
        assertEquals("com.example:artifact:1.0.0:sources@jar", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithOnlyGroupAndArtifact() {
        Dependency dep = new Dependency("com.example", "artifact", null, null, null);
        assertEquals("com.example:artifact", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithNullGroup() {
        Dependency dep = new Dependency(null, "artifact", "1.0.0", null, null);
        assertEquals(":artifact:1.0.0", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithNullVersionButClassifierPresent() {
        Dependency dep = new Dependency("com.example", "artifact", null, "sources", null);
        assertEquals("com.example:artifact::sources", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithNullVersionAndClassifierButExtensionPresent() {
        Dependency dep = new Dependency("com.example", "artifact", null, null, "jar");
        assertEquals("com.example:artifact@jar", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithNullClassifierButExtensionPresent() {
        Dependency dep = new Dependency("com.example", "artifact", "1.0.0", null, "jar");
        assertEquals("com.example:artifact:1.0.0@jar", dep.toStringNotation());
    }

    @Test
    void toStringNotationWithOnlyArtifact() {
        Dependency dep = new Dependency(null, "artifact", null, null, null);
        assertEquals(":artifact", dep.toStringNotation());
    }
}
