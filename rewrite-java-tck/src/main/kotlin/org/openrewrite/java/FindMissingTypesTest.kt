/*
 * Copyright 2022 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.search.FindMissingTypes
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation

interface FindMissingTypesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindMissingTypes())
            .typeValidationOptions(TypeValidation.none())
    }

    @Test
    fun findsMissingAnnotationType(jp: JavaParser) {
        val cu = jp.parse("""
            import org.junit.Test;
            
            class A {
                @Test
                void foo() {}
            }
        """)[0]
        assertThat(FindMissingTypes.findMissingTypes(cu).size).isEqualTo(1)
    }
}
