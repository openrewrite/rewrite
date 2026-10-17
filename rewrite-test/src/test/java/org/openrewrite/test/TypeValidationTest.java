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

import static org.assertj.core.api.Assertions.assertThat;

class TypeValidationTest {

    @Test
    void noneInvertsEveryFlagFromAll() throws IllegalAccessException {
        TypeValidation all = TypeValidation.all();
        TypeValidation none = TypeValidation.none();
        for (Field field : TypeValidation.class.getDeclaredFields()) {
            if (field.getType() == boolean.class && !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                field.setAccessible(true);
                assertThat(field.getBoolean(none))
                  .as("%s is not skipped by none()", field.getName())
                  .isNotEqualTo(field.getBoolean(all));
            }
        }
    }

    @Test
    void noneSkipsWhitespaceValidation() {
        assertThat(TypeValidation.none().allowNonWhitespaceInWhitespace()).isTrue();
        assertThat(TypeValidation.all().allowNonWhitespaceInWhitespace()).isFalse();
    }
}
