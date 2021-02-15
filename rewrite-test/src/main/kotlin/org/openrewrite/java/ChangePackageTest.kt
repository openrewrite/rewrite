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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

interface ChangePackageTest: JavaRecipeTest {
    @Test
    fun changePackage(jp: JavaParser) = assertChanged(
        jp,
        recipe = ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test"
        ),
        before = """
            package org.openrewrite;
            class Test {
            }
        """,
        after = """ 
            package org.openrewrite.test;
            class Test {
            }
        """,
        afterConditions = { cu ->
            assertThat(cu.sourcePath.toString()).contains("org/openrewrite/test")
        }
    )

    @Test
    fun changePackageReferences(jp: JavaParser) = assertChanged(
        jp,
        cycles = 2,
        recipe = ChangePackage(
            "org.openrewrite",
            "org.openrewrite.test"
        ),
        before = """
            import org.openrewrite.*;
            
            class A<T extends org.openrewrite.Test> {
                Test test;
            }
        """,
        after = """ 
            import org.openrewrite.test.Test;
            
            class A<T extends org.openrewrite.test.Test> {
                Test test;
            }
        """,
        dependsOn = arrayOf("""
            package org.openrewrite;
            public class Test {
            }
        """)
    )
}
