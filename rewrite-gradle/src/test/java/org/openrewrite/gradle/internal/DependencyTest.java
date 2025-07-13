package org.openrewrite.gradle.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void toStringNotationWithNullVersionAndExtensionButClassifierPresent() {
        Dependency dep = new Dependency("com.example", "artifact", null, "sources", null);
        assertEquals("com.example:artifact::sources", dep.toStringNotation());
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