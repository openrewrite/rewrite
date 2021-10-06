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
@file:Suppress("UnnecessaryTemporaryOnConversionToString", "UseCompareMethod")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("BooleanConstructorCall", "deprecation", "CachedNumberConstructorCall")
interface NoPrimitiveWrappersForToStringOrCompareToTest : JavaRecipeTest {

    override val recipe: Recipe
        get() = NoPrimitiveWrappersForToStringOrCompareTo()

    @Test
    fun noPrimitiveWrapperForToString() = assertChanged(
        before = """
            class T {
                 String a = new Integer(3).toString();
                 String b = Long.valueOf(3).toString();
                 String c = Double.valueOf(3.0).toString();
                 String d = Float.valueOf("4").toString();
                 String e = new Float("3").toString();
                 String f = Boolean.valueOf(true).toString();
                 String G = Boolean.valueOf("true").toString();
            }
        """,
        after = """
            class T {
                 String a = Integer.toString(3);
                 String b = Long.toString(3);
                 String c = Double.toString(3.0);
                 String d = Float.valueOf("4").toString();
                 String e = new Float("3").toString();
                 String f = Boolean.toString(true);
                 String G = Boolean.valueOf("true").toString();
            }
        """
    )

    @Test
    fun noPrimitiveWrapperForCompareTo() = assertChanged(
        before = """
            class T {
                int a = new Boolean(true).compareTo(false);
                int b = Boolean.valueOf(true).compareTo(false);
                int c = new Integer(3).compareTo(4);
                int d = new Long(4L).compareTo(5L);
            }
        """,
        after = """
            class T {
                int a = Boolean.compare(true, false);
                int b = Boolean.compare(true, false);
                int c = Integer.compare(3, 4);
                int d = Long.compare(4L, 5L);
            }
        """
    )
}
