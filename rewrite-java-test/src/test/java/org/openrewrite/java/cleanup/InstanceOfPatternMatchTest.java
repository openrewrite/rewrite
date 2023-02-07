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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"RedundantCast", "DataFlowIssue", "ConstantValue"})
class InstanceOfPatternMatchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InstanceOfPatternMatch());
    }

    @SuppressWarnings({"ImplicitArrayToString", "PatternVariableCanBeUsed", "UnnecessaryLocalVariable"})
    @Nested
    class If {

        @Test
        void ifConditionWithoutPattern() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            Object s = 1;
                            if (o instanceof String && ((String) (o)).length() > 0) {
                                if (((String) o).length() > 1) {
                                    System.out.println(o);
                                }
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            Object s = 1;
                            if (o instanceof String string && ((String) (o)).length() > 0) {
                                if (string.length() > 1) {
                                    System.out.println(o);
                                }
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void multipleCasts() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o, Object o2) {
                            Object string = 1;
                            if (o instanceof String && o2 instanceof Integer) {
                                System.out.println((String) o);
                                System.out.println((Integer) o2);
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o, Object o2) {
                            Object string = 1;
                            if (o instanceof String string1 && o2 instanceof Integer integer) {
                                System.out.println(string1);
                                System.out.println(integer);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void longNames() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.ArrayList;
                    public class A {
                        void test(Object o) {
                            Object list = 1;
                            if (o instanceof ArrayList<?>) {
                                System.out.println((ArrayList<?>) o);
                            }
                        }
                    }
                    """,
                  """
                    import java.util.ArrayList;
                    public class A {
                        void test(Object o) {
                            Object list = 1;
                            if (o instanceof ArrayList<?> arrayList) {
                                System.out.println(arrayList);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void primitiveArray() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof int[]) {
                                System.out.println((int[]) o);
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof int[] ints) {
                                System.out.println(ints);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void matchingVariableInBody() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                String str = (String) o;
                                String str2 = (String) o;
                                System.out.println(str + str2);
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String str) {
                                String str2 = str;
                                System.out.println(str + str2);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void conflictingVariableInBody() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                String string = 'x';
                                System.out.println((String) o);
                    //            String string1 = "y";
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String string1) {
                                String string = 'x';
                                System.out.println(string1);
                    //            String string1 = "y";
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/2787")
        @Disabled
        @Test
        void nestedPotentiallyConflictingIfs() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                if (o instanceof String) {
                                    System.out.println((String) o);
                                }
                                System.out.println((String) o);
                            }
                        }
                    }
                    """,
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String string) {
                                if (o instanceof String string1) {
                                    System.out.println(string1);
                                }
                                System.out.println(string);
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void expressionWithSideEffects() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            Object s = 1;
                            if (convert(o) instanceof String && ((String) convert(o)).length() > 0) {
                                if (((String) convert(o)).length() > 1) {
                                    System.out.println(o);
                                }
                            }
                        }
                        Object convert(Object o) {
                            return o;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void noTypeCast() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                System.out.println(o);
                            }
                        }
                    }
                     """
                ), 17)
            );
        }

        @Test
        void typeCastInElse() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String) {
                                System.out.println(o);
                            } else {
                                System.out.println((String) o);
                            }
                        }
                    }
                     """
                ), 17)
            );
        }

        @Test
        void ifConditionWithPattern() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String s && s.length() > 0) {
                                System.out.println(s);
                            }
                        }
                    }
                     """
                ), 17)
            );
        }

        @Test
        void orOperationInIfCondition() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (o instanceof String || ((String) o).length() > 0) {
                                if (((String) o).length() > 1) {
                                    System.out.println(o);
                                }
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void negatedInstanceOfMatchedInElse() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        void test(Object o) {
                            if (!(o instanceof String)) {
                                System.out.println(((String) o).length());
                            } else {
                                System.out.println(((String) o).length());
                            }
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }

    @SuppressWarnings({"CastCanBeRemovedNarrowingVariableType", "ClassInitializerMayBeStatic"})
    @Nested
    class Ternary {

        @Test
        void typeCastInTrue() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        String test(Object o) {
                            return o instanceof String ? ((String) o).substring(1) : o.toString();
                        }
                    }
                    """,
                  """
                    public class A {
                        String test(Object o) {
                            return o instanceof String s ? s.substring(1) : o.toString();
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void multipleVariablesOnlyOneUsed() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        String test(Object o1, Object o2) {
                            return o1 instanceof String && o2 instanceof Number
                                ? ((String) o1).substring(1) : o1.toString();
                        }
                    }
                    """,
                  """
                    public class A {
                        String test(Object o1, Object o2) {
                            return o1 instanceof String s && o2 instanceof Number
                                ? s.substring(1) : o1.toString();
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void initBlocks() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        static {
                            Object o = null;
                            String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                        }
                        {
                            Object o = null;
                            String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                        }
                    }
                    """,
                  """
                    public class A {
                        static {
                            Object o = null;
                            String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                        }
                        {
                            Object o = null;
                            String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void typeCastInFalse() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        String test(Object o) {
                            return o instanceof String ? o.toString() : ((String) o).substring(1);
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }

    @Nested
    class Binary {

        @Test
        void onlyReplacementsBeforeOrOperator() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof String && ((String) o).length() > 1 || ((String) o).length() > 2;
                        }
                    }
                    """,
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof String s && s.length() > 1 || ((String) o).length() > 2;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void methodCallBreaksFlowScope() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean m(Object o) {
                            return test(o instanceof String) && ((String) o).length() > 1;
                        }
                        boolean test(boolean b) {
                            return b;
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }

    @Nested
    class Arrays {

        @Test
        void string() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof String[] && ((java.lang.String[]) o).length > 1 || ((String[]) o).length > 2;
                        }
                    }
                    """,
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof String[] ss && ss.length > 1 || ((String[]) o).length > 2;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void primitive() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof int[] && ((int[]) o).length > 1 || ((int[]) o).length > 2;
                        }
                    }
                    """,
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof int[] is && is.length > 1 || ((int[]) o).length > 2;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void multiDimensional() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof int[][] && ((int[][]) o).length > 1 || ((int[][]) o).length > 2;
                        }
                    }
                    """,
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof int[][] is && is.length > 1 || ((int[][]) o).length > 2;
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void dimensionalMismatch() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        boolean test(Object o) {
                            return o instanceof int[][] && ((int[]) o).length > 1;
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nested
    class Generics {
        @Test
        void wildcardInstanceOf() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            if (o instanceof List<?>) {
                                return ((List<?>) o).get(0);
                            }
                            return o.toString();
                        }
                    }
                    """,
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            if (o instanceof List<?> list) {
                                return list.get(0);
                            }
                            return o.toString();
                        }
                    }
                    """
                ), 17)
            );
        }

        @Test
        void rawInstanceOfAndWildcardParameterizedCast() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            return o instanceof List ? ((List<?>) o).get(0) : o.toString();
                        }
                    }
                    """,
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            return o instanceof List l ? l.get(0) : o.toString();
                        }
                    }
                    """
                ), 17)
            );
        }

        @Test
        void rawInstanceOfAndObjectParameterizedCast() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            return o instanceof List ? ((List<Object>) o).get(0) : o.toString();
                        }
                    }
                    """,
                  """
                    import java.util.List;
                    public class A {
                        Object test(Object o) {
                            return o instanceof List l ? l.get(0) : o.toString();
                        }
                    }
                    """
                ), 17)
            );
        }

        @Test
        void rawInstanceOfAndParameterizedCast() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.List;
                    public class A {
                        String test(Object o) {
                            return o instanceof List ? ((List<String>) o).get(0) : o.toString();
                        }
                    }
                    """
                ), 17
              )
            );
        }

        @Test
        void unboundGenericTypeVariable() {
            rewriteRun(
              version(
                java(
                  """
                    import java.util.List;
                    public class A<T> {
                        void test(Object t) {
                            if (t instanceof List) {
                                List<T> l = (List<T>) t;
                                System.out.println(l.size());
                            }
                        }
                    }
                    """
                ), 17)
            );
        }
    }

    @Nested
    class Various {
        @Test
        void unaryWithoutSideEffects() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        String test(Object o) {
                            return ((Object) ("1" + ~1)) instanceof String ? ((String) ((Object) ("1" + ~1))).substring(1) : o.toString();
                        }
                    }
                    """,
                  """
                    public class A {
                        String test(Object o) {
                            return ((Object) ("1" + ~1)) instanceof String s ? s.substring(1) : o.toString();
                        }
                    }
                    """
                ), 17
              )
            );
        }
        @Test
        void nestedClasses() {
            rewriteRun(
              version(
                java(
                  """
                    public class A {
                        public static class Else {}
                        String test(Object o) {
                            if (o instanceof Else) {
                                return ((Else) o).toString();
                            }
                            return o.toString();
                        }
                    }
                    """,
                  """
                    public class A {
                        public static class Else {}
                        String test(Object o) {
                            if (o instanceof Else else1) {
                                return else1.toString();
                            }
                            return o.toString();
                        }
                    }
                    """
                ), 17
              )
            );
        }
    }
}