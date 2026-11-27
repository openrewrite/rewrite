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
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
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

        assertThat(CalledMethod.find(receiver(call), "set", call.getArguments())).isNull();
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
        JavaType unknown = JavaType.Unknown.getInstance();
        assertThat(CalledMethod.find(receiver(call), "of", arguments(unknown, unknown, unknown))).isNull();
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
    void findsTheOverrideBeforeAndAfterTheMethodItOverrides() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            public interface Wide {
                Object get();
            }
            """,
          """
            package b;
            public interface Narrow extends Wide {
                Object get();
            }
            """,
          """
            package b;
            public interface New extends Wide, Narrow {
            }
            """,
          """
            class Test {
                void test(b.Narrow narrow, b.New n) {
                    narrow.get();
                    n.get();
                }
            }
            """
        );
        assertThat(find(calls.get(0))).hasToString(parsed(calls.get(0)));

        assertThat(find(calls.get(1))).hasToString(parsed(calls.get(1)));
    }

    @Test
    void methodOfUnrelatedInterfacesIsFoundOnlyWhereOneOverridesBoth() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            public interface One {
                Object get();
            }
            """,
          """
            package b;
            public interface Other {
                Object get();
            }
            """,
          """
            package b;
            public interface Both extends One, Other {
                Object get();
            }
            """,
          """
            package b;
            public interface Either extends One, Other {
            }
            """,
          """
            package b;
            public interface New extends One, Other, Both {
            }
            """,
          """
            class Test {
                void test(b.Either either, b.New n) {
                    either.get();
                    n.get();
                }
            }
            """
        );
        // javac has it be the one of `Other`, as an arbitrary choice
        assertThat(find(calls.get(0))).isNull();

        assertThat(find(calls.get(1))).hasToString(parsed(calls.get(1)));
    }

    @Test
    void concreteMethodOfAClassIsCalledOverThatOfAnUnrelatedInterface() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            public class Base {
                public Object get() { return null; }
            }
            """,
          """
            package b;
            public abstract class AbstractBase {
                public abstract Object get();
            }
            """,
          """
            package b;
            public interface Getter {
                Object get();
            }
            """,
          """
            package b;
            public class New extends Base implements Getter {
            }
            """,
          """
            package b;
            public abstract class AbstractNew extends AbstractBase implements Getter {
            }
            """,
          """
            class Test {
                void test(b.New n, b.AbstractNew abstractNew) {
                    n.get();
                    abstractNew.get();
                }
            }
            """
        );
        assertThat(find(calls.get(0))).hasToString(parsed(calls.get(0)));

        assertThat(find(calls.get(1))).isNull();
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
            assertThat(CalledMethod.find(type, "put", arguments(argumentType)))
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
        assertThat(CalledMethod.find(receiver(call), "put", arguments(arg))).isNull();
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
        assertThat(CalledMethod.find(type, "put", call.getArguments())).isNull();
    }

    @Test
    void parameterOfATypeVariableRulesOutNoMethod() {
        J.MethodInvocation call = call(
          """
            package b;
            public class New {
                public <R extends Number> void put(R r) {}
                public void put(Object o) {}
                public <R> void set(R[] rs) {}
                public void set(Object o) {}
            }
            """,
          """
            class Test {
                void test(b.New n, Integer[] is) {
                    n.put(is[0]);
                }
            }
            """
        );
        assertThat(find(call)).isNull();

        J.ArrayAccess element = (J.ArrayAccess) call.getArguments().get(0);
        assertThat(CalledMethod.find(receiver(call), "set", singletonList(element.getIndexed()))).isNull();
    }

    @Test
    void rawArgumentIsOneForAParameterizedParameter() {
        J.MethodInvocation call = call(
          """
            package b;
            import java.util.List;
            public class New {
                public void put(List<String> l) {}
                public void put(Integer i) {}
            }
            """,
          """
            import java.util.List;
            class Test {
                void test(b.New n, List raw) {
                    n.put(raw);
                }
            }
            """
        );
        assertThat(find(call)).hasToString(parsed(call));
    }

    @Test
    void expressionTypedByItsParameterRulesOutNoMethod() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            public interface Mapper {
                String map(String s);
            }
            """,
          """
            package b;
            public class New {
                public void go(Mapper m) {}
                public void go(Object o) {}
            }
            """,
          """
            import java.util.function.Function;
            class Test {
                void old(Function<String, String> f) {}
                void test(b.New n, boolean b) {
                    n.hashCode();
                    old((s -> s));
                    old(String::trim);
                    old(b ? s -> s : String::trim);
                    old(switch (1) { default -> s -> s; });
                }
            }
            """
        );
        // the parser has each be a `Function`, the parameter of `old`, which is none of `go`
        JavaType.FullyQualified type = receiver(calls.get(0));
        for (J.MethodInvocation old : calls.subList(1, calls.size())) {
            assertThat(CalledMethod.find(type, "go", old.getArguments()))
              .as("with the argument of %s", old)
              .isNull();
        }
    }

    @Test
    void callOfAGenericMethodRulesOutNoMethod() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            public class New {
                public Object old(Object o) { return o; }
                public Number neu(Object o) { return 0; }
                public String neu(String s) { return s; }
                public <T> T get() { return null; }
            }
            """,
          """
            class Test {
                void test(b.New n) {
                    n.old(n.get());
                }
            }
            """
        );
        // the parser has `get()` return the `Object` that `old` takes, where javac has it return a `String` for `neu`
        J.MethodInvocation old = calls.get(0);
        assertThat(CalledMethod.find(receiver(old), "neu", old.getArguments())).isNull();
    }

    @Test
    void typesOfAnInstanceMethodThatARawTypeInheritsAreErased() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            import java.util.List;
            public abstract class Box<T> {
                public abstract List<String> all();
                public static List<String> of() { return null; }
            }
            """,
          """
            package b;
            public abstract class RawBox extends Box {
            }
            """,
          """
            package b;
            public abstract class StringBox extends Box<String> {
            }
            """,
          """
            class Test {
                void test(b.RawBox raw, b.StringBox strings) {
                    raw.all();
                    raw.of();
                    strings.all();
                }
            }
            """
        );
        assertThat(isErased(calls.get(0))).isTrue();
        assertThat(parsed(calls.get(0))).contains("return=java.util.List,");

        for (J.MethodInvocation call : calls.subList(1, calls.size())) {
            assertThat(isErased(call)).as("types of %s", call).isFalse();
            assertThat(find(call)).hasToString(parsed(call));
        }
    }

    @Test
    void typesOfAMethodThatTakesARawArgumentAreErased() {
        List<J.MethodInvocation> calls = calls(
          """
            package b;
            import java.util.List;
            import java.util.function.Supplier;
            public class New {
                public List<String> put(List<String> l) { return l; }
                public List<String> each(List<String>... ls) { return null; }
                public List<String> get(Supplier<String> s) { return null; }
            }
            """,
          """
            package b;
            import java.util.List;
            public class Holder<T> {
                public List<String> hold(T t) { return null; }
            }
            """,
          """
            import java.util.ArrayList;
            import java.util.List;
            import java.util.concurrent.Callable;
            class Test {
                void old(Callable<String> c) {}
                void test(b.New n, b.Holder<List<String>> holder, List raw, ArrayList<String> strings,
                          List<String>[] lists) {
                    n.put(raw);
                    n.each(strings, raw);
                    holder.hold(raw);
                    n.put(strings);
                    n.put(null);
                    n.each(lists);
                    old(() -> "");
                    old(String::new);
                }
            }
            """
        );
        for (J.MethodInvocation call : calls.subList(0, 3)) {
            assertThat(isErased(call)).as("types of %s", call).isTrue();
            assertThat(parsed(call)).contains("return=java.util.List,");
        }

        for (J.MethodInvocation call : calls.subList(3, 5)) {
            assertThat(isErased(call)).as("types of %s", call).isFalse();
            assertThat(find(call)).hasToString(parsed(call));
        }
        assertThat(isErased(calls.get(5))).isFalse();

        // the parser has each be a `Callable`, the parameter of `old`, where `get` takes a `Supplier`
        JavaType.FullyQualified type = receiver(calls.get(0));
        JavaType.Method get = CalledMethod.candidates(type, "get", calls.get(6).getArguments()).get(0);
        assertThat(CalledMethod.isErased(type, get, calls.get(6).getArguments())).isFalse();
        assertThat(CalledMethod.isErased(type, get, calls.get(7).getArguments())).isFalse();
    }

    /**
     * The last call in the last of {@code sources}.
     */
    private static J.MethodInvocation call(@Language("java") String... sources) {
        List<J.MethodInvocation> calls = calls(sources);
        return calls.get(calls.size() - 1);
    }

    /**
     * The calls in the last of {@code sources}.
     */
    private static List<J.MethodInvocation> calls(@Language("java") String... sources) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parse(sources).collect(toList());
        List<J.MethodInvocation> calls = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                calls.add(method);
                return super.visitMethodInvocation(method, p);
            }
        }.visit(parsed.get(parsed.size() - 1), 0);
        return calls;
    }

    /**
     * The method that {@code call} is found to call, with the types the call has.
     */
    private static JavaType.@Nullable Method find(J.MethodInvocation call) {
        JavaType.FullyQualified type = receiver(call);
        JavaType.Method found = CalledMethod.find(type, call.getSimpleName(), call.getArguments());
        return found == null ? null : CalledMethod.calledOn(type, found);
    }

    private static boolean isErased(J.MethodInvocation call) {
        JavaType.FullyQualified type = receiver(call);
        List<JavaType.Method> candidates = CalledMethod.candidates(type, call.getSimpleName(), call.getArguments());
        assertThat(candidates).hasSize(1);
        return CalledMethod.isErased(type, candidates.get(0), call.getArguments());
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

    private static List<Expression> arguments(JavaType... types) {
        List<Expression> arguments = new ArrayList<>();
        for (JavaType type : types) {
            arguments.add(new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "arg", type, null));
        }
        return arguments;
    }
}
