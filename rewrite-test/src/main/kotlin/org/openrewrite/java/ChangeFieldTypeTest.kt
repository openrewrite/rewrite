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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.tree.JavaType
import java.util.function.Supplier

interface ChangeFieldTypeTest : RecipeTest {
    fun changeFieldType(from: String, to: String) = object : Recipe() {
        init {
            this.processor = Supplier {
                ChangeFieldType(JavaType.Class.build(from), to)
            }
        }
    }

    @Test
    fun changeFieldTypeDeclarative(jp: JavaParser) = assertChanged(
        jp,
        recipe = changeFieldType("java.util.List", "java.util.Collection"),
        before = """
            import java.util.List;
            public class A {
               List collection;
            }
        """,
        after = """
            import java.util.Collection;
            
            public class A {
               Collection collection;
            }
        """
    )
}
