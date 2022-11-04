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
@file:Suppress("deprecation", "RedundantThrows")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface ObjectFinalizeCallsSuperTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ObjectFinalizeCallsSuper())
    }

    @Test
    fun hasSuperFinalizeInvocation() = rewriteRun(
        java("""
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                    super.finalize();
                }
            }
        """)
    )

    @Test
    fun addsSuperFinalizeInvocation() = rewriteRun(
        java("""
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                }
            }
        """,
        """
            class F {
                Object o = new Object();
                
                @Override
                protected void finalize() throws Throwable {
                    o = null;
                    super.finalize();
                }
            }
        """)
    )
}
