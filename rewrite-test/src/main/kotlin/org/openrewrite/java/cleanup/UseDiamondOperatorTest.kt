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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("Convert2Diamond")
interface UseDiamondOperatorTest: JavaRecipeTest {
    override val recipe: Recipe?
        get() = UseDiamondOperator()

    @Test
    fun useDiamondOperator(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.*;

            class Test<X, Y> {
                void test() {
                    List<String> ls = new ArrayList<String>();
                    Map<X,Y> map = new HashMap<X,Y>();
                    List<String> ls2 = new ArrayList<String>() {
                    };
                }
            }
        """,
        after = """
            import java.util.*;

            class Test<X, Y> {
                void test() {
                    List<String> ls = new ArrayList<>();
                    Map<X,Y> map = new HashMap<>();
                    List<String> ls2 = new ArrayList<String>() {
                    };
                }
            }
        """
    )

    @Test
    fun noLeftSide(jp: JavaParser) = assertUnchanged(
        parser = jp,
        before = """
            import java.util.HashMap;
            class Test {
                static {
                    new HashMap<String, String>();
                }
            }
        """.trimIndent()
    )

    @Test
    fun notAsAChainedMethodInvocation(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                public static ResponseBuilder<String> bResponse(String entity) {
                    return new ResponseBuilder<String>().entity(entity);
                }
                public static ResponseBuilder<String> bResponse(String entity) {
                    return new ResponseBuilder<String>();
                }

                public static class ResponseBuilder<T> {
                    public ResponseBuilder<T> entity(T entity) {
                        return this;
                    }
                }
            }
        """,
        after = """
            class Test {
                public static ResponseBuilder<String> bResponse(String entity) {
                    return new ResponseBuilder<String>().entity(entity);
                }
                public static ResponseBuilder<String> bResponse(String entity) {
                    return new ResponseBuilder<>();
                }

                public static class ResponseBuilder<T> {
                    public ResponseBuilder<T> entity(T entity) {
                        return this;
                    }
                }
            }
        """
    )

    @Test
    fun removeUnusedImports(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.Map;
            import java.util.HashMap;
            import java.math.BigDecimal;
            import java.util.Date;

            class Test {
                void test() {
                    Map<Object,Object> map = new HashMap<BigDecimal,Date>();
                }
            }
        """,
        after = """
            import java.util.Map;
            import java.util.HashMap;

            class Test {
                void test() {
                    Map<Object,Object> map = new HashMap<>();
                }
            }
        """
    )

}
