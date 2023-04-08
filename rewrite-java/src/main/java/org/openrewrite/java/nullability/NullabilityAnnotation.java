package org.openrewrite.java.nullability;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.internal.lang.NonNull;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@RequiredArgsConstructor
class NullabilityAnnotation {

    static NullabilityAnnotation nullable(String fqn) {
        return new NullabilityAnnotation(fqn, Nullability.NULLABLE);
    }

    static NullabilityAnnotation nonNull(String fqn) {
        return new NullabilityAnnotation(fqn, Nullability.NONNULL);
    }

    enum Nullability {
        NULLABLE,
        NONNULL;
    }

    /**
     * Fully qualified name of this annotation
     */
    String fqn;
    /**
     * Whether this annotation indicates whether an element can be null or not.
     */
    Nullability nullability;
    /**
     * Defines on what elements this nullability annotation ca be used.
     * @see ElementType
     */
    Set<ElementType> targets = new HashSet<>();
    /**
     * Defines on what elements this nullability annotation applies.
     * Any of {@link ElementType#FIELD}, {@link ElementType#METHOD}, {@link ElementType#PARAMETER}.
     */
    Set<ElementType> scopes = new HashSet<>();

    public NullabilityAnnotation(String fqn, Nullability nullability, Set<ElementType> targets, Set<ElementType> scopes) {
        this(fqn, nullability);
        getTargets().addAll(targets);
        getScopes().addAll(scopes);
    }

    public boolean isNullable() {
        return nullability == Nullability.NULLABLE;
    }

    public boolean isNonNull() {
        return nullability == Nullability.NONNULL;
    }

    public NullabilityAnnotation withTargets(@NonNull ElementType... targets) {
        return new NullabilityAnnotation(getFqn(), getNullability(), Arrays.stream(targets).collect(Collectors.toSet()), getScopes());
    }

    public NullabilityAnnotation withAllTargets() {
        return withTargets(ElementType.values());
    }

    public NullabilityAnnotation withScopes(@NonNull ElementType... scopes) {
        return new NullabilityAnnotation(getFqn(), getNullability(), getTargets(), Arrays.stream(scopes).collect(Collectors.toSet()));
    }

    public NullabilityAnnotation withAllScopes() {
        return withScopes(ElementType.values());
    }
}