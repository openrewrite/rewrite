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
package org.openrewrite.java.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils

interface TypesInUseTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    fun findAnnotationArgumentType(jp: JavaParser.Builder<*, *>) {
        val cus = jp.build().parse(
            """
            package org.openrewrite.test;
            
            public @interface YesOrNo {
                Status status();
                enum Status {
                    YES, NO
                }
            }
        """,
            """
            package org.openrewrite.test;
            
            import static org.openrewrite.test.YesOrNo.Status.YES;
            
            @YesOrNo(status = YES)
            public class Foo {}
        """
        )

        val foo = cus.find { it.classes[0].name.simpleName == "Foo" }!!
        val foundTypes = foo.typesInUse.variables
        assertThat(foundTypes.isNotEmpty())
        assertThat(foundTypes).anyMatch { TypeUtils.asFullyQualified(it.type)!!.fullyQualifiedName.equals("org.openrewrite.test.YesOrNo.Status") }

        assertThat(
            foundTypes.filterIsInstance<JavaType.FullyQualified>()
                .map { TypeUtils.asFullyQualified(it)!!.fullyQualifiedName }
        ).containsExactlyInAnyOrder("org.openrewrite.test.YesOrNo", "org.openrewrite.test.YesOrNo.Status")
    }
}
