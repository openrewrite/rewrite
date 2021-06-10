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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.java.ChangeMethodAccessLevelVisitor.MethodAccessLevel.*
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

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
    fun changeMethodAccessLevelFromPackagePrivateToPublic(jp: JavaParser) = assertChanged(
            jp,
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(String)", "protected"),
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
            recipe = ChangeMethodAccessLevel("com.abc.A aMethod(..)", "package-private"),
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

                // comment <-- FIXME: remove additional space in front of comment?
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

                 // comment <-- FIXME: remove additional space in front of comment?
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
}