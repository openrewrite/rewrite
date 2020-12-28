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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.style.ImportLayoutStyle
interface RemoveUnusedOrdersTest : RefactorVisitorTest {

    @Test
    fun unFoldPackageWildcard(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveUnusedImports()
            ),
            before = """
                import java.util.*;
                import java.math.BigDecimal;

                class A {
                    List<BigDecimal> getList() {
                        return Collections.emptyList();
                    }
                    Set<BigDecimal> getSet() {
                        return new HashSet<>();
                    }
                }
            """,
            after = """                
                import java.util.Collections;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Set;
                import java.math.BigDecimal;

                class A {
                    List<BigDecimal> getList() {
                        return Collections.emptyList();
                    }
                    Set<BigDecimal> getSet() {
                        return new HashSet<>();
                    }
                }
            """
    )

    @Test
    fun doNotUnfoldWildcards() = assertUnchanged(
        JavaParser.fromJavaVersion().
        styles(mutableListOf(ImportLayoutStyle.layout(3,3))).build(),
        visitors = listOf(
            RemoveUnusedImports()
        ),
        before = """
                import java.util.*;
                import java.math.BigDecimal;

                class A {
                    List<BigDecimal> getList() {
                        return Collections.emptyList();
                    }
                    Set<BigDecimal> getSet() {
                        return new HashSet<>();
                    }
                }
            """
    )

    @Test
    fun dontMoveImports(jp: JavaParser)  = assertUnchanged(
        jp,
        visitors = listOf(
            RemoveUnusedImports()
        ),
        before = """
                import java.util.ArrayList;
                import java.util.HashSet;
                import java.math.BigDecimal;
                import java.util.List;
                import java.util.Set;

                class A {
                    List<BigDecimal> getList() {
                        return Collections.emptyList();
                    }
                    Set<BigDecimal> getSet() {
                        return new HashSet<>();
                    }
                }
            """
    )

    @Test
    fun unfoldStaticStar(jp: JavaParser)  = assertRefactored(
        JavaParser.fromJavaVersion().styles(mutableListOf(ImportLayoutStyle.layout(999,999))).build(),
        visitors = listOf(
            RemoveUnusedImports()
        ),
        before = """
                import static java.util.Collections.*;
                import java.math.BigDecimal;
                import java.util.List;

                class A {
                    List<BigDecimal> getList() {
                        return singletonList(BigDecimal.valueOf(1));
                    }
                    List<BigDecimal> getEmptyList() {
                        return emptyList();
                    }
                }
            """,
        after = """                
                import static java.util.Collections.emptyList;
                import static java.util.Collections.singletonList;
                import java.math.BigDecimal;
                import java.util.List;

                class A {
                    List<BigDecimal> getList() {
                        return singletonList(BigDecimal.valueOf(1));
                    }
                    List<BigDecimal> getEmptyList() {
                        return emptyList();
                    }
                }
            """
    )

    @Test
    fun dontMoveStaticImports(jp: JavaParser)  = assertUnchanged(
        JavaParser.fromJavaVersion().styles(mutableListOf(ImportLayoutStyle.layout(999,999))).build(),
        visitors = listOf(
            RemoveUnusedImports()
        ),
        before = """
                import static java.util.Collections.emptyList;
                import java.math.BigDecimal;
                import java.util.List;
                import static java.util.Collections.singletonList;

                class A {
                    List<BigDecimal> getList() {
                        return singletonList(BigDecimal.valueOf(1));
                    }
                    List<BigDecimal> getEmptyList() {
                        return emptyList();
                    }
                }
            """
    )

    @Test
    fun doNotUnfoldStaticWildcards() = assertUnchanged(
        JavaParser.fromJavaVersion().
        styles(mutableListOf(ImportLayoutStyle.layout(2,2))).build(),
        visitors = listOf(
            RemoveUnusedImports()
        ),
        before = """
                import static java.util.Collections.*;
                import java.math.BigDecimal;
                import java.util.List;

                class A {
                    List<BigDecimal> getList() {
                        return singletonList(BigDecimal.valueOf(1));
                    }
                    List<BigDecimal> getEmptyList() {
                        return emptyList();
                    }
                }
            """
    )

}
