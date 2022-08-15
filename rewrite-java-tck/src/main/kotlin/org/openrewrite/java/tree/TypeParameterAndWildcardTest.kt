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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.JavaTreeTest.NestingLevel.CompilationUnit

interface TypeParameterAndWildcardTest : JavaTreeTest {

    @Test
    fun annotatedTypeParametersOnWildcardBounds(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.List;
            interface B {}
            class A {
                List<? extends @NotNull B> checks;
            }
        """
    )

    @Test
    fun annotatedTypeParametersOnReturnTypeExpression(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.List;
            interface B {}
            class A {
                public List<
                    @NotNull(groups = Prioritized.P1.class)
                    @javax.validation.Valid
                    B> foo() {
                    return null;
                }
            }
        """
    )

    @Test
    fun extendsAndSuper(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.List;
            interface B {}
            interface C {}
            public class A {
                public <P  extends B> void foo(List<P> out, List<? super C> in) {}
            }
        """
    )

    @Test
    fun multipleExtends(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            interface B {}
            interface C {}
            public class A< T extends  B & C > {}
        """
    )

    @Test
    fun wildcardExtends(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.*;
            interface B {}
            public class A {
                List< ?  extends  B > bs;
            }
        """
    )

    @Test
    fun emptyWildcard(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            import java.util.*;
            public class A {
                List< ? > a;
            }
        """
    )
}
