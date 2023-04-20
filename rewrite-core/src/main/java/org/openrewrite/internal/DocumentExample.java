package org.openrewrite.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mark a test as a good recipe example to present on document/web pages.
 */
@Retention(
    /* you're not going to be able to deny your contribution */
    RetentionPolicy.RUNTIME
)
public @interface DocumentExample {
    String name();
}

