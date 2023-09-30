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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Iterator;
import java.util.function.Predicate;

class ValidatedTest {

    @Test
    void andBadAnonymousValidatedMustNotRaiseException() {
        Validated<String> good = Validated.valid("foo", "bar");
        Validated<String> bad = new Validated<>() {
            @Override
            public boolean isValid() {
                throw new IllegalStateException("this exception must not be raised by the boolean AND");
            }

            @Override
            public Iterator<Validated<String>> iterator() {
                return null;
            }

            @Override
            public @Nullable String getValue() {
                return null;
            }
        };
        assertThatCode(() -> good.and(bad)).doesNotThrowAnyException();
    }

    @Test
    void andBadPropertyTestMustNotRaiseException() {
        Validated<String> good = Validated.valid("foo", "bar");
        Validated<String> bad = Validated.test("foo2", "bar2", null, t -> {
            throw new IllegalStateException("this exception must not be raised by the boolean AND");
        });

        assertThatCode(() -> good.and(bad)).doesNotThrowAnyException();
    }

    @Test
    void testProperty() {
        Validated<String> valid = Validated.test("foo", "bar", null, t -> true);
        Validated<String> invalid = Validated.test("foo2", "bar2", null, t -> false);

        assertThat(valid.isValid()).isTrue();
        assertThat(invalid.isValid()).isFalse();
    }

}
