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
package org.openrewrite.java.isolated;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.Java17Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.DefaultJavaTypeFactory;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TypedTree;

import javax.tools.ToolProvider;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.tree.JavaType.FullyQualified.Kind;

class ReloadableJava17TypeMappingTest {

    @TempDir
    static Path fixtureClasses;

    @BeforeAll
    static void compileFixture() throws Exception {
        Path holder = fixtureClasses.resolve("Holder.java");
        Files.writeString(holder, """
          package fixture;
          public class Holder {
              public Iface iface() { return null; }
              public Cls cls() { return null; }
              public void take(Param p) {}
          }
          class Cls {}
          """);
        Path iface = fixtureClasses.resolve("Iface.java");
        Files.writeString(iface, "package fixture; public interface Iface {}");
        Path param = fixtureClasses.resolve("Param.java");
        Files.writeString(param, "package fixture; public class Param {}");
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
          "-d", fixtureClasses.toString(), holder.toString(), iface.toString(), param.toString());
        assertThat(exit).isZero();
    }

    @Test
    void typeServedByTheFactoryIsNotCompletedByJavac() throws Exception {
        JavaType.Class premade = premade("fixture.Iface", Kind.Interface);
        JavaParser parser = parserServing(premade);

        J.CompilationUnit cu = parse(parser, "class A { fixture.Holder h; }");
        J.VariableDeclarations field =
          (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
        JavaType.Class holder = (JavaType.Class) field.getType();

        assertThat(returnTypeOf(holder, "iface")).isSameAs(premade);
        assertThat(isCompleted(parser, "fixture.Holder")).as("referenced from source, javac loads it").isTrue();
        assertThat(isCompleted(parser, "fixture.Cls")).as("built by the initializer, which loads it").isTrue();
        assertThat(isCompleted(parser, "fixture.Iface")).as("served by the factory").isFalse();
    }

    @Test
    void typesNamedOnlyInJavadocAreNotCompletedByJavac() throws Exception {
        JavaType.Class premadeIface = premade("fixture.Iface", Kind.Interface);
        JavaType.Class premadeParam = premade("fixture.Param", Kind.Class);
        JavaParser parser = parserServing(premadeIface, premadeParam);

        J.CompilationUnit cu = parse(parser, """
          /**
           * {@link fixture.Iface} {@link fixture.Holder#take(fixture.Param)}
           */
          class A {}
          """);
        List<Javadoc.Link> links = cu.getClasses().get(0).getPrefix().getComments().stream()
          .flatMap(c -> ((Javadoc.DocComment) c).getBody().stream())
          .filter(Javadoc.Link.class::isInstance)
          .map(Javadoc.Link.class::cast)
          .toList();
        J.MethodInvocation take = (J.MethodInvocation) links.get(1).getTreeReference().getTree();

        assertThat(((TypedTree) links.get(0).getTreeReference().getTree()).getType()).isSameAs(premadeIface);
        assertThat(take.getMethodType()).isNotNull();
        assertThat(((Expression) take.getArguments().get(0)).getType()).isSameAs(premadeParam);
        assertThat(isCompleted(parser, "fixture.Holder")).as("owner of the referenced method").isTrue();
        assertThat(isCompleted(parser, "fixture.Iface")).as("named only as a link").isFalse();
        assertThat(isCompleted(parser, "fixture.Param")).as("named only as a parameter type").isFalse();
    }

    private static JavaType.Class premade(String fqn, Kind kind) {
        return new JavaType.Class(null, Flag.Public.getBitMask(), fqn, kind, null, null, null, null, null, null, null);
    }

    private static JavaParser parserServing(JavaType.Class... premade) {
        Map<String, JavaType.Class> served = Stream.of(premade)
          .collect(toMap(JavaType.Class::getFullyQualifiedName, Function.identity()));
        JavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache()) {
            @Override
            public JavaType.Class computeClass(String fqn, long flags, Kind kind,
                                               Consumer<JavaType.Class> initializer) {
                JavaType.Class hit = served.get(fqn);
                return hit != null ? hit : super.computeClass(fqn, flags, kind, initializer);
            }
        };
        return Java17Parser.builder().classpath(List.of(fixtureClasses)).typeFactory(factory).build();
    }

    private static J.CompilationUnit parse(JavaParser parser, String source) {
        return (J.CompilationUnit) parser.parse(new InMemoryExecutionContext(), source).findFirst().orElseThrow();
    }

    private static JavaType.Class returnTypeOf(JavaType.Class type, String methodName) {
        return (JavaType.Class) type.getMethods().stream()
          .filter(m -> m.getName().equals(methodName))
          .findFirst().orElseThrow()
          .getReturnType();
    }

    private static Boolean isCompleted(JavaParser parser, String fqn) throws Exception {
        Field delegateField = Java17Parser.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(parser);
        Class<?> probe = delegate.getClass().getClassLoader().loadClass("org.openrewrite.java.isolated.JavacSymbolProbe");
        return (Boolean) probe.getMethod("isCompleted", Object.class, String.class).invoke(null, delegate, fqn);
    }
}
