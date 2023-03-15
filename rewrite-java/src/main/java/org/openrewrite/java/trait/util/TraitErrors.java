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
package org.openrewrite.java.trait.util;

import org.openrewrite.Cursor;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Immutable
public final class TraitErrors implements Iterable<TraitError> {
    private final List<TraitError> errors;

    private TraitErrors(List<TraitError> errors) {
        // Defensive copy
        this.errors =
                Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(errors, "errors cannot be null")));
    }

    @Override
    public Iterator<TraitError> iterator() {
        return errors.iterator();
    }

    public <V> V doThrow() {
        throw new TraitErrorsException(this);
    }

    public String toString() {
        return "TraitErrors: " + errors.stream()
                .map(TraitError::getError)
                .collect(Collectors.joining("\n\t- ", "\n\t- ", ""));
    }

    public static TraitErrors fromSingle(TraitError error) {
        return new TraitErrors(Collections.singletonList(error));
    }

    public static TraitErrors fromSingleError(String error) {
        return fromSingle(new TraitError(error));
    }

    public static <V> Validation<TraitErrors, V> invalidTraitCreationError(String error) {
        return Validation.fail(TraitErrors.fromSingleError(error));
    }

    public static <V> Validation<TraitErrors, V> invalidTraitCreationType(Class<?> traitType, Cursor cursor, Class<?> expectedType) {
        return Validation.fail(TraitErrors.fromSingleError(
                traitType.getSimpleName() + " must be created from " + expectedType + " but was " + cursor.getValue().getClass()
        ));
    }

    public static <V> Validation<TraitErrors, V> invalidTraitCreationType(Class<?> traitType, Cursor cursor, Class<?> expectedTypeFirst, Class<?> expectedTypeSecond) {
        return Validation.fail(TraitErrors.fromSingleError(
                traitType.getSimpleName() + " must be created from " + expectedTypeFirst + " or " + expectedTypeSecond + " but was " + cursor.getValue().getClass()
        ));
    }

    public static Semigroup<TraitErrors> semigroup = Semigroup.semigroupDef((a, b) -> new TraitErrors(
            Stream.concat(a.errors.stream(), b.errors.stream()).collect(Collectors.toList())
    ));
}
