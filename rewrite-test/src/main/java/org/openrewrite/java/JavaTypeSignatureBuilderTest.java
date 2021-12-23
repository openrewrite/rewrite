/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.internal.JavaTypeSignatureBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public interface JavaTypeSignatureBuilderTest {

    /**
     * @param methodName The type of the first parameter of the method with this name.
     */
    Object firstMethodParameter(String methodName);

    /**
     * The type of the type variable of the last type parameter of {@link JavaTypeGoat}.
     */
    Object lastClassTypeParameter();

    JavaTypeSignatureBuilder signatureBuilder();

    @Test
    default void array() {
        assertThat(signatureBuilder().signature(firstMethodParameter("array")))
                .isEqualTo("org.openrewrite.java.C[][]");
    }

    @Test
    default void classSignature() {
        assertThat(signatureBuilder().signature(firstMethodParameter("clazz")))
                .isEqualTo("org.openrewrite.java.C");
    }

    @Test
    default void primitive() {
        assertThat(signatureBuilder().signature(firstMethodParameter("primitive")))
                .isEqualTo("int");
    }

    @Test
    default void parameterized() {
        assertThat(signatureBuilder().signature(firstMethodParameter("parameterized")))
                .isEqualTo("org.openrewrite.java.PT<org.openrewrite.java.C>");
    }

    @Test
    default void parameterizedRecursive() {
        assertThat(signatureBuilder().signature(firstMethodParameter("parameterizedRecursive")))
                .isEqualTo("org.openrewrite.java.PT<org.openrewrite.java.PT<org.openrewrite.java.C>>");
    }

    @Test
    default void generic() {
        assertThat(signatureBuilder().signature(firstMethodParameter("generic")))
                .isEqualTo("org.openrewrite.java.PT<Generic{? extends org.openrewrite.java.C}>");
    }

    @Test
    default void genericContravariant() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericContravariant")))
            .isEqualTo("org.openrewrite.java.PT<Generic{? super org.openrewrite.java.C}>");
    }

    @Test
    default void genericRecursiveInClassDefinition() {
        assertThat(signatureBuilder().signature(lastClassTypeParameter()))
                .isEqualTo("Generic{S extends org.openrewrite.java.JavaTypeGoat<Generic{S}, Generic{? extends Generic{T}}> & org.openrewrite.java.C}");
    }

    @Test
    default void genericRecursiveInMethodDeclaration() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericRecursive")))
                .isEqualTo("org.openrewrite.java.JavaTypeGoat<Generic{? extends Generic{U extends org.openrewrite.java.JavaTypeGoat<U, Generic{?}>}[]}, Generic{?}>");
    }

    @Test
    default void genericUnbounded() {
        assertThat(signatureBuilder().signature(firstMethodParameter("genericUnbounded")))
            .isEqualTo("org.openrewrite.java.PT<Generic{U}>");
    }

    @Test
    default void innerClass() {
        assertThat(signatureBuilder().signature(firstMethodParameter("inner")))
                .isEqualTo("org.openrewrite.java.C$Inner");
    }
}
