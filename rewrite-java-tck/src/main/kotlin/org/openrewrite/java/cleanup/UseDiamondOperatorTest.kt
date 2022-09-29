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
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("Convert2Diamond")
interface UseDiamondOperatorTest: RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseDiamondOperator())
    }

    @Test
    fun useDiamondOperator() = rewriteRun(
        java("""
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
        """
            import java.util.*;

            class Test<X, Y> {
                void test() {
                    List<String> ls = new ArrayList<>();
                    Map<X,Y> map = new HashMap<>();
                    List<String> ls2 = new ArrayList<String>() {
                    };
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1297")
    @Test
    fun doNotUseDiamondOperatorsForVariablesHavingNullOrUnknownTypes() = rewriteRun(
        java("""
            import lombok.val;
            import java.util.ArrayList;

            class Test<X, Y> {
                void test() {
                    val ls = new ArrayList<String>();
                    UnknownThing o = new UnknownThing<String>();
                }
            }
        """)
    )

    @Test
    fun noLeftSide() = rewriteRun(
        java("""
            import java.util.HashMap;
            class Test {
                static {
                    new HashMap<String, String>();
                }
            }
        """)
    )

    @Test
    fun notAsAChainedMethodInvocation() = rewriteRun(
        java("""
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
        """
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
        """)
    )

    @Test
    fun removeUnusedImports() = rewriteRun(
        java("""
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
        """
            import java.util.Map;
            import java.util.HashMap;

            class Test {
                void test() {
                    Map<Object,Object> map = new HashMap<>();
                }
            }
        """)
    )

}
