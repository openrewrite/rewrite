/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FindFieldsOfTypeTest : JavaRecipeTest {

    @Test
    fun findPrivateNonInheritedField(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFieldsOfType("java.util.List"),
        before = """
            import java.util.*;
            public class A {
               private List list;
               private Set set;
            }
        """,
        after = """
            import java.util.*;
            public class A {
               /*~~>*/private List list;
               private Set set;
            }
        """,
    )

    @Test
    fun findArrayOfType(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFieldsOfType("java.lang.String"),
        before = """
            import java.util.*;
            public class A {
               private String[] s;
            }
        """,
        after = """
            import java.util.*;
            public class A {
               /*~~>*/private String[] s;
            }
        """,
    )

    @Test
    fun skipsMultiCatches(jp: JavaParser) = assertChanged(
        jp,
        recipe = FindFieldsOfType("java.io.File"),
        before = """
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """,
        after = """
            import java.io.*;
            public class A {
                /*~~>*/File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """,
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = FindFieldsOfType(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("fullyQualifiedTypeName")

        recipe = FindFieldsOfType("com.foo.Foo")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }

}
