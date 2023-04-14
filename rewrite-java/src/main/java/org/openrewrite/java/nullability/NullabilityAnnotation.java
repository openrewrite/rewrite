package org.openrewrite.java.nullability;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.util.Set;

public interface NullabilityAnnotation {

    enum Nullability {
        NULLABLE,
        NONNULL,
        UNKNOWN;
    }

    enum Scope {
        FIELD,
        METHOD,
        PARAMETER;
    }

    /**
     * Fully qualified name of this annotation
     */
    String getFqn();

    default String getSimpleName() {
        return StringUtils.substringAfterLast(getFqn(), ".");
    }

    /**
     * Whether this annotation indicates whether an element can be null or not.
     */
    Nullability getNullability();

    /**
     * Defines on what elements this nullability annotation ca be used.
     * @see ElementType
     */
    Set<ElementType> getTargets();

    /**
     * Defines on what elements this nullability annotation applies.
     */
    Set<Scope> getScopes();
}
