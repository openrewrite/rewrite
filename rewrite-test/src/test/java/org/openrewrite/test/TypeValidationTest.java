/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TypeValidationTest {

    @Test
    void noneInvertsEveryFlagFromAll() throws IllegalAccessException {
        // the checks none() keeps, and one that all() leaves off
        Set<String> exempt = Set.of("addedMarkers", "minimalFormatting", "typeAttributedMatching");
        TypeValidation all = TypeValidation.all();
        TypeValidation none = TypeValidation.none();
        for (Field field : TypeValidation.class.getDeclaredFields()) {
            if (field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() &&
                !exempt.contains(field.getName())) {
                field.setAccessible(true);
                assertThat(field.getBoolean(none))
                  .as("%s is not skipped by none()", field.getName())
                  .isNotEqualTo(field.getBoolean(all));
            }
        }
    }

    @Test
    void noneKeepsChecksThatDoNotNeedTypeAttribution() {
        TypeValidation none = TypeValidation.none();
        assertThat(none.addedMarkers()).isTrue();
        assertThat(none.minimalFormatting()).isTrue();
    }

    @Test
    void noneSkipsChecksThatNeedTypeAttribution() {
        assertThat(TypeValidation.none().matchByTypeNotName()).isFalse();
        assertThat(TypeValidation.none().typeAttributedMatching()).isFalse();
        // until its false positives are measured
        assertThat(TypeValidation.all().typeAttributedMatching()).isFalse();
    }

    @Test
    void noneSkipsWhitespaceValidation() {
        assertThat(TypeValidation.none().allowNonWhitespaceInWhitespace()).isTrue();
        assertThat(TypeValidation.all().allowNonWhitespaceInWhitespace()).isFalse();
    }
}
