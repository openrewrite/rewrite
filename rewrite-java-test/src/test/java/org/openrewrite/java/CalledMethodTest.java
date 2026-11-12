/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT;

class CalledMethodTest {

    @Test
    void methodThatRejectsTheArgumentsIsNotTheOneCalled() {
        J.MethodInvocation call = call(
          """
            package b;
            public class New {
                public void put(Integer i) {}
                public void put(CharSequence s) {}
                public void set(Integer i) {}
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.put("");
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));

        assertThat(CalledMethod.find(receiver(call), "set", argumentTypes(call))).isNull();
    }

    @Test
    void findsNoMethodWhereOneOfTheNameTakesVarargs() {
        J.MethodInvocation call = call(
          """
            package b;
            public class New {
                public void of(String a, String b, String c) {}
                public void of(int... is) {}
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.of(1, 2, 3);
                }
            }
            """
        );
        // arguments that rule out no method leave `of(String, String, String)` as the only one of their number
        assertThat(CalledMethod.find(receiver(call), "of", nCopies(3, JavaType.Unknown.getInstance()))).isNull();
    }

    @Test
    void staticMethodOfAnInterfaceIsNotInherited() {
        J.MethodInvocation call = call(
          """
            package b;
            public interface Of {
                static void of(int i) {}
            }
            """,
          """
            package b;
            public class New implements Of {
                public static void of(long l) {}
            }
            """,
          """
            class Test {
                void test() {
                    b.New.of(1);
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));
    }

    @Test
    void privateMethodOfASupertypeIsNotInherited() {
        J.MethodInvocation call = call(
          """
            package b;
            public class Base {
                private void put(String s) {}
                public void put(CharSequence s) {}
            }
            """,
          """
            package b;
            public class New extends Base {
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.put("");
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));
    }

    @Test
    void privateMethodOfTheTypeItselfMayBeTheOneCalled() {
        J.MethodInvocation call = call(
          """
            class Test {
                private void put(String s) {}
                public void put(CharSequence s) {}
                void test(Test t) {
                    t.put("");
                }
            }
            """
        );
        assertThat(find(call)).isNull();
    }

    @Test
    void findsTheInheritedMethodWithTheNarrowerReturnType() {
        J.MethodInvocation call = call(
          """
            package b;
            public interface Wide {
                Object get();
            }
            """,
          """
            package b;
            public interface Narrow extends Wide {
                String get();
            }
            """,
          """
            package b;
            public interface New extends Wide, Narrow {
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.get();
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));
    }

    @Test
    void typeArgumentsOfTheReceiverAcceptTheArguments() {
        J.MethodInvocation call = call(
          """
            package b;
            public interface Box<T> {
                void put(T t);
                void put(Integer i);
            }
            """,
          """
            class Test {
                void test(b.Box<String> box) {
                    box.put("");
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));
    }

    @Test
    void argumentOfATypeThatIsOnlyItsNameRulesOutNoMethod() {
        J.MethodInvocation call = call(
          """
            package b;
            public class New {
                public void put(CharSequence s) {}
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.put("");
                }
            }
            """
        );
        JavaType.FullyQualified type = receiver(call);
        JavaType nameOnly = JavaType.ShallowClass.build("a.Arg");
        JavaType wildcard = new JavaType.GenericTypeVariable(null, "?", COVARIANT, singletonList(nameOnly));
        for (JavaType argumentType : List.of(
          nameOnly,
          JavaType.Unknown.getInstance(),
          new JavaType.Array(null, nameOnly, null),
          new JavaType.Parameterized(null, type, singletonList(wildcard))
        )) {
            assertThat(CalledMethod.find(type, "put", singletonList(argumentType)))
              .as("with an argument of type %s", argumentType)
              .hasToString(parsed(call));
        }
    }

    @Test
    void argumentWithASupertypeThatIsOnlyItsNameRulesOutNoMethod() {
        J.MethodInvocation call = call(
          """
            package b;
            public interface Base {
            }
            """,
          """
            package b;
            public class Arg implements Base {
            }
            """,
          """
            package b;
            public class New {
                public void put(Base b) {}
                public void put(Object o) {}
            }
            """,
          """
            class Test {
                void test(b.New n, b.Arg arg) {
                    n.put(arg);
                }
            }
            """
        );
        // as `ChangeType` of `b.Base` to `c.Base` leaves the type `b.Arg`
        JavaType.Class arg = ((JavaType.Class) requireNonNull(call.getArguments().get(0).getType()))
          .withInterfaces(singletonList(JavaType.ShallowClass.build("c.Base")));
        assertThat(CalledMethod.find(receiver(call), "put", singletonList(arg))).isNull();
    }

    @Test
    void findsNoMethodOfATypeWithASupertypeThatIsOnlyItsName() {
        J.MethodInvocation call = call(
          """
            package b;
            public class Base {
                public void put(int i) {}
            }
            """,
          """
            package b;
            public class New extends Base {
                public void put(long l) {}
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.put(1);
                }
            }
            """
        );
        // as `ChangeType` of `b.Base` to `c.Base` leaves the type `b.New`
        JavaType.Class type = ((JavaType.Class) receiver(call))
          .withSupertype(JavaType.ShallowClass.build("c.Base"));
        assertThat(CalledMethod.find(type, "put", argumentTypes(call))).isNull();
    }

    /**
     * The one call in the last of {@code sources}.
     */
    private static J.MethodInvocation call(@Language("java") String... sources) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parse(sources).collect(toList());
        AtomicReference<J.MethodInvocation> call = new AtomicReference<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                call.set(method);
                return method;
            }
        }.visit(parsed.get(parsed.size() - 1), 0);
        return requireNonNull(call.get());
    }

    private static JavaType.@Nullable Method find(J.MethodInvocation call) {
        return CalledMethod.find(receiver(call), call.getSimpleName(), argumentTypes(call));
    }

    /**
     * The method the parser has {@code call} call, which is the one to find.
     */
    private static String parsed(J.MethodInvocation call) {
        return requireNonNull(call.getMethodType()).toString();
    }

    private static JavaType.FullyQualified receiver(J.MethodInvocation call) {
        return requireNonNull(TypeUtils.asFullyQualified(requireNonNull(call.getSelect()).getType()));
    }

    private static List<@Nullable JavaType> argumentTypes(J.MethodInvocation call) {
        List<@Nullable JavaType> argumentTypes = new ArrayList<>();
        for (Expression argument : call.getArguments()) {
            if (!(argument instanceof J.Empty)) {
                argumentTypes.add(argument.getType());
            }
        }
        return argumentTypes;
    }
}
