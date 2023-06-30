package org.openrewrite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedTest {

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3385")
    void lazyFailures() {
        Validated<String> validated = Validated.lazy("Hello", () -> null);
        assertFalse(validated.isValid());
        // throws ClassCastException: class org.openrewrite.Validated$LazyValidated cannot be cast to class org.openrewrite.Validated$Invalid
        assertDoesNotThrow(validated::failures);
    }

}