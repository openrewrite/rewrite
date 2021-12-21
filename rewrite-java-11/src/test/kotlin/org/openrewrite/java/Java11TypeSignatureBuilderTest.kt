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
package org.openrewrite.java

import org.junit.jupiter.api.Test

class Java11TypeSignatureBuilderTest : JavaTypeSignatureBuilderTest, Java11TypeSignatureBuilderTestBase() {

    @Test
    override fun arraySignature() = methodFirstParameterSignatureEquals(
        """
            interface Test {
                void test(Integer[] n);
            }
        """,
        "java.lang.Integer[]"
        )

    @Test
    override fun classSignature() = methodFirstParameterSignatureEquals(
        """
            import java.io.File;
            interface Test {
                void test(File f);
            }
        """,
        "java.io.File"
    )

    @Test
    override fun primitiveSignature() = methodFirstParameterSignatureEquals(
        """
            import java.io.File;
            interface Test {
                void test(int n);
            }
        """,
        "int"
    )

    @Test
    override fun parameterizedSignature() = methodFirstParameterSignatureEquals(
        """
            import java.util.List;
            interface Test {
                void test(List<String> l);
            }
        """,
        "java.util.List<java.lang.String>"
    )

    @Test
    override fun genericTypeVariable() = methodFirstParameterSignatureEquals(
        """
            import java.util.List;
            interface Test {
                void test(List<? extends String> l);
            }
        """,
        "java.util.List<? extends java.lang.String>"
    )

    @Test
    override fun genericVariableContravariant() = methodFirstParameterSignatureEquals(
        """
            import java.util.List;
            interface Test {
                void test(List<? super Test> l);
            }
        """,
        "java.util.List<? super Test>"
    )

    @Test
    override fun traceySpecial() = classTypeParameterSignatureEquals(
        """
            import java.util.*;
            interface Test<T extends A<? extends T>> {}
            interface A<U> {}
        """,
        "T extends A<? extends T>")

    @Test
    override fun genericVariableMultipleBounds() = classTypeParameterSignatureEquals(
        """
            import java.util.*;
            interface Test<T extends A & B> {}
            interface A {}
            interface B {}
        """,
        "T extends A & B")

    @Test
    override fun genericTypeVariableUnbounded() = methodFirstParameterSignatureEquals(
        """
            import java.util.List;
            interface Test {
                <T> void test(List<T> l);
            }
        """,
        "java.util.List<T>"
    )
}
