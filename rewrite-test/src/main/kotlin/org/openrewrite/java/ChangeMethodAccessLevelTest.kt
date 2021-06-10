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
    fun fromPublicToPrivate(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "private"),
            before = """
            package com.abc;
            class A {

                @Deprecated // comment
                public void aMethod(String s) {
                }

                // comment
                @Deprecated
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

                @Deprecated // comment
                private void aMethod(String s) {
                }

                // comment
                @Deprecated
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
    fun fromPackagePrivateToProtected(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected"),
            before = """
            package com.abc;
            class A {

                void aMethod(Double i) {
                }

                @Deprecated // comment
                void aMethod(String s) {
                }
            }
        """,
            after = """
            package com.abc;
            class A {

                protected void aMethod(Double i) {
                }

                @Deprecated // comment
                protected void aMethod(String s) {
                }
            }
        """
    )

    @Disabled
    @Test
    fun fromPackagePrivateToProtected_broken(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected"),
            before = """
            package com.abc;
            class A {

                // comment
                @Deprecated
                void aMethod() {
                }

                // comment
                void aMethod(Integer i) {
                }
            }
        """,
            after = """
            package com.abc;
            class A {

                // comment
                @Deprecated
                protected void aMethod() {
                }

                // comment
                protected void aMethod(Integer i) {
                }
            }
        """
    )

    @Test
    fun fromPackagePrivateToProtected_withOtherModifier(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "protected"),
            before = """
            package com.abc;
            class A {

                static void aMethod(Integer i) {
                }

                // comment
                static void aMethod(String s) {
                }

                @Deprecated // comment
                static void aMethod(Double d) {
                }
            }
        """,
            after = """
            package com.abc;
            class A {

                protected static void aMethod(Integer i) {
                }

                // comment
                protected static void aMethod(String s) {
                }

                @Deprecated // comment
                protected static void aMethod(Double d) {
                }
            }
        """
    )

    @Disabled
    @Test
    fun fromPackagePrivateToProtected_withOtherModifier_broken(jp: JavaParser) = assertChanged(
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
    fun fromPackagePrivateToProtected_withConstructor(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A A(..)", "protected"),
            before = """
            package com.abc;
            class A {

                A(Integer i) {
                }
            }
        """,
            after = """
            package com.abc;
            class A {

                protected A(Integer i) {
                }
            }
        """
    )

    @Disabled
    @Test
    fun fromPackagePrivateToProtected_withConstructor_broken(jp: JavaParser) = assertChanged(
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

    @Test
    fun fromPublicToPackagePrivate(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A *(..)", "package-private"),
            before = """
            package com.abc;
            class A {

                public A() {
                }

                // comment
                @Deprecated
                public void aMethod() {
                }

                public void aMethod(Double i) {
                }
            }
        """,
            after = """
            package com.abc;
            class A {

                A() {
                }

                // comment
                @Deprecated
                void aMethod() {
                }

                void aMethod(Double i) {
                }
            }
        """
    )

    // This test should be included in the previous one, when it's fixed
    @Test
    @Disabled
    fun fromPublicToPackagePrivate_broken(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A *(..)", "package-private"),
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

    @Test
    fun onMethodWithAlreadyCorrectAccessLevel(jp: JavaParser) = assertUnchanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "public"),
            before = """
            package com.abc;
            class A {
                // comment
                public void aMethod(String s) {
                }
            }
        """
    )
}