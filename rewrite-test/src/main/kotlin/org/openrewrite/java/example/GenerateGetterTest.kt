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
package org.openrewrite.java.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser

interface GenerateGetterTest : RecipeTest {

    @Test
    fun forField(jp: JavaParser) = assertChanged(
            jp,
            recipe = GenerateGetter("field"),
            before = """
                class A {
                    String field;
                }
            """,

            /*
            Note: if a human added the getter, there would be an indent on the blank line after field,
            from when the author hit enter after "String field;" but IntelliJ reformat does not
            add that indent into the blank line if it's missing, and if IntelliJ adds the blank line it lacks the indent
            so I think it's not a formatting behavior, it's an auto-indent while typing behavior.
            */

            after = """
                class A {
                    String field;
                
                    public String getField() {
                        return field;
                    }
                }
            """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = GenerateGetter(null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse()
        assertThat(valid.failures()).hasSize(1)
        assertThat(valid.failures()[0].property).isEqualTo("fieldName")

        recipe = GenerateGetter("foo")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue()
    }
}
