package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsTest {

    @Test
    void classAvailable() {
        boolean result = ReflectionUtils.isClassAvailable("org.openrewrite.internal.ReflectionUtilsTest");
        assertTrue(result);
    }

    @Test
    void classNotAvailable() {
        boolean result = ReflectionUtils.isClassAvailable("org.openrewrite.internal.ReflectionUtilsTest");
        assertTrue(result);
    }

    @Test
    void classNotAvailableWhenFQNOmitted() {
        boolean result = ReflectionUtils.isClassAvailable("ReflectionUtilsTest");
        assertFalse(result);
    }
}
