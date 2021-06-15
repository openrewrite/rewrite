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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

interface ChangeMethodAccessLevelTest : JavaRecipeTest {

    @Test
    fun changeMethodAccessLevelFromPublicToPrivate(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "private"),
        before = """
            package com.abc;
            class A {

                @SuppressWarnings("ALL") // comment
                public void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                public void aMethod() {
                }

                // comment
                public void aMethod(Integer i) {
                }

                public void aMethod(Double i) {
                }
            }
        """,
        after = """
            package com.abc;
            class A {

                @SuppressWarnings("ALL") // comment
                private void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                private void aMethod() {
                }

                // comment
                private void aMethod(Integer i) {
                }

                private void aMethod(Double i) {
                }
            }
        """
    )

    @Test
    fun changeMethodAccessLevelFromPackagePrivateToProtected(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected"),
        before = """
            package com.abc;
            class A {

                @SuppressWarnings("ALL") // comment
                static void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                void aMethod() {
                }

                // comment
                void aMethod(Integer i) {
                }

                void aMethod(Double i) {
                }
            }
        """,
        after = """
            package com.abc;
            class A {

                @SuppressWarnings("ALL") // comment
                protected static void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                protected void aMethod() {
                }

                // comment
                protected void aMethod(Integer i) {
                }

                protected void aMethod(Double i) {
                }
            }
        """
    )

    @Test
    fun changeMethodAccessLevelFromPublicToPackagePrivate(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "package"),
        before = """
            package com.abc;
            class A {

                @SuppressWarnings("ALL") // comment
                public void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                public void aMethod() {
                }

                // comment
                public void aMethod(Integer i) {
                }

                public void aMethod(Double i) {
                }
            }
        """,
        after = """
            package com.abc;
            class A {

                // comment
                @SuppressWarnings("ALL")
                void aMethod(String s) {
                }

                // comment
                @SuppressWarnings("ALL")
                void aMethod() {
                }

                // comment
                void aMethod(Integer i) {
                }

                void aMethod(Double i) {
                }
            }
        """
    )

    @Test
    fun changeMethodAccessLevelOnMethodWithAlreadyCorrectAccessLevel(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A aMethod(String)", "public"),
        before = """
            package com.abc;
            class A {
                // comment
                public void aMethod(String s) {
                }
            }
        """
    )

    @Test
    fun fromPackagePrivateToProtectedWithOtherModifier(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected"),
        before = """
            package com.abc;
            class A {
                // comment
                @Deprecated
                static void aMethod(Double d) {
                }
            }
        """,
        after = """
            package com.abc;
            class A {
                // comment
                @Deprecated
                protected static void aMethod(Double d) {
                }
            }
        """
    )

    @Test
    fun fromPackagePrivateToProtectedWithConstructor(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A A(..)", "protected"),
        before = """
            package com.abc;
            class A {
                A(Integer i) {
                }
                // comment
                A() {
                }
            }
        """,
        after = """
            package com.abc;
            class A {
                protected A(Integer i) {
                }
                // comment
                protected A() {
                }
            }
        """
    )

    @Disabled
    @Test
    fun fromPublicToPackagePrivate(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangeMethodAccessLevel("com.abc.A *(..)", "package"),
        before = """
            package com.abc;
            class A {
                // comment
                public A(Integer i) {
                }
                @Deprecated // comment
                public A(Float f) {
                }
                @Deprecated // comment
                public void aMethod(String s) {
                }
                // comment
                public void aMethod(Integer i) {
                }
            }
        """,
        after = """
            package com.abc;
            class A {
                // comment
                A(Integer i) {
                }
                @Deprecated // comment
                A(Float f) {
                }
                @Deprecated // comment
                void aMethod(String s) {
                }
                // comment
                void aMethod(Integer i) {
                }
            }
        """
    )
}
