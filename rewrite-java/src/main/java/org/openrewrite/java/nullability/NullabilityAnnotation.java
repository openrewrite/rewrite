/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.nullability;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
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
        NONNULL
    }

    enum Scope {
        FIELD,
        METHOD,
        PARAMETER
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
     */
    Set<Scope> scopes = new HashSet<>();

    public NullabilityAnnotation(String fqn, Nullability nullability, Set<ElementType> targets, Set<Scope> scopes) {
        this(fqn, nullability);
        getTargets().addAll(targets);
        getScopes().addAll(scopes);
    }

    public String getSimpleName() {
        return StringUtils.substringAfterLast(getFqn(), ".");
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

    public NullabilityAnnotation withScopes(@NonNull Scope... scopes) {
        return new NullabilityAnnotation(getFqn(), getNullability(), getTargets(), Arrays.stream(scopes).collect(Collectors.toSet()));
    }

    public NullabilityAnnotation withAllScopes() {
        return withScopes(Scope.values());
    }
}