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
import org.openrewrite.java.Java21Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.DefaultJavaTypeFactory;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import javax.tools.ToolProvider;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.tree.JavaType.FullyQualified.Kind;

class ReloadableJava21TypeMappingTest {

    @TempDir
    static Path fixtureClasses;

    @BeforeAll
    static void compileFixture() throws Exception {
        Path source = fixtureClasses.resolve("Holder.java");
        Files.writeString(source, """
          package fixture;
          public class Holder {
              public Ann ann() { return null; }
              public Iface iface() { return null; }
              public Enm enm() { return null; }
              public Rec rec() { return null; }
              public Cls cls() { return null; }
          }
          @interface Ann {}
          interface Iface {}
          enum Enm {}
          record Rec() {}
          class Cls {}
          """);
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
          "-d", fixtureClasses.toString(), source.toString());
        assertThat(exit).isZero();
    }

    @Test
    void kindOfClasspathTypesFollowsTheirClassFileFlags() {
        JavaType.Class holder = parseHolderUse(new DefaultJavaTypeFactory(new JavaTypeCache()));

        assertThat(returnTypeOf(holder, "ann").getKind()).isEqualTo(Kind.Annotation);
        assertThat(returnTypeOf(holder, "iface").getKind()).isEqualTo(Kind.Interface);
        assertThat(returnTypeOf(holder, "enm").getKind()).isEqualTo(Kind.Enum);
        assertThat(returnTypeOf(holder, "rec").getKind()).isEqualTo(Kind.Record);
        assertThat(returnTypeOf(holder, "cls").getKind()).isEqualTo(Kind.Class);
        assertThat(returnTypeOf(holder, "iface").getFlags()).contains(Flag.Interface, Flag.Abstract);
    }

    @Test
    void typeServedByTheFactoryIsNotCompletedByJavac() throws Exception {
        JavaType.Class premade = new JavaType.Class(null, 0, "fixture.Iface", Kind.Interface,
          null, null, null, null, null, null, null);
        JavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache()) {
            @Override
            public JavaType.Class computeClass(String fqn, long flags, Kind kind, Consumer<JavaType.Class> initializer) {
                return premade.getFullyQualifiedName().equals(fqn) ? premade : super.computeClass(fqn, flags, kind, initializer);
            }
        };
        JavaParser parser = Java21Parser.builder().classpath(List.of(fixtureClasses)).typeFactory(factory).build();

        JavaType.Class holder = parseHolderUse(parser);

        assertThat(returnTypeOf(holder, "iface")).isSameAs(premade);
        assertThat(isCompleted(parser, "fixture.Holder")).as("referenced from source, javac loads it").isTrue();
        assertThat(isCompleted(parser, "fixture.Cls")).as("built by the initializer, which loads it").isTrue();
        assertThat(isCompleted(parser, "fixture.Iface")).as("served by the factory").isFalse();
    }

    private static JavaType.Class parseHolderUse(JavaTypeFactory factory) {
        return parseHolderUse(Java21Parser.builder().classpath(List.of(fixtureClasses)).typeFactory(factory).build());
    }

    private static JavaType.Class parseHolderUse(JavaParser parser) {
        J.CompilationUnit cu = (J.CompilationUnit) parser.parse(new InMemoryExecutionContext(),
          "class A { fixture.Holder h; }").findFirst().orElseThrow();
        J.VariableDeclarations field = (J.VariableDeclarations) cu.getClasses().get(0).getBody().getStatements().get(0);
        return (JavaType.Class) field.getType();
    }

    private static JavaType.Class returnTypeOf(JavaType.Class type, String methodName) {
        return (JavaType.Class) type.getMethods().stream()
          .filter(m -> m.getName().equals(methodName))
          .findFirst().orElseThrow()
          .getReturnType();
    }

    private static Boolean isCompleted(JavaParser parser, String fqn) throws Exception {
        Field delegateField = Java21Parser.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(parser);
        Class<?> probe = delegate.getClass().getClassLoader().loadClass("org.openrewrite.java.isolated.JavacSymbolProbe");
        return (Boolean) probe.getMethod("isCompleted", Object.class, String.class).invoke(null, delegate, fqn);
    }
}
