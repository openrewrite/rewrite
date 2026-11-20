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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ClassLiteralTypeTest {

    @Test
    void typedWhenAnnotationElementBoundIsMissingFromClasspath(@TempDir Path classes) throws Exception {
        Path factory = classes.resolve("Factory.java");
        Files.write(factory, """
          package fixture;
          public interface Factory {}
          """.getBytes(UTF_8));
        Path options = classes.resolve("Options.java");
        Files.write(options, """
          package fixture;
          public @interface Options {
              Class<? extends Factory> factory();
          }
          """.getBytes(UTF_8));
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
          "-d", classes.toString(), factory.toString(), options.toString());
        assertThat(exit).isZero();
        Files.delete(classes.resolve("fixture/Factory.class"));

        Expression literal = annotationArgument(singletonList(classes), """
          import fixture.Factory;
          import fixture.Options;

          @Options(factory = MyFactory.class)
          class Runner {
          }

          class MyFactory implements Factory {
          }
          """);

        assertThat(literal.getType()).hasToString("java.lang.Class<MyFactory>");
    }

    @Test
    void typedWhenAnnotationElementRejectsIt() {
        Expression literal = annotationArgument(emptyList(), """
          @Options(factory = String.class)
          class Runner {
          }

          @interface Options {
              Class<? extends Number> factory();
          }
          """);

        assertThat(literal.getType()).hasToString("java.lang.Class<java.lang.String>");
    }

    @Test
    void unknownWhenTargetIsUnresolved() {
        Expression literal = annotationArgument(emptyList(), """
          @Options(factory = Missing.class)
          class Runner {
          }

          @interface Options {
              Class<?> factory();
          }
          """);

        assertThat(literal.getType()).hasToString("Unknown");
    }

    private static Expression annotationArgument(List<Path> classpath, String source) {
        J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion().classpath(classpath).build()
          .parse(new InMemoryExecutionContext(), source).collect(toList()).get(0);
        J.Annotation annotation = cu.getClasses().get(0).getLeadingAnnotations().get(0);
        return ((J.Assignment) annotation.getArguments().get(0)).getAssignment();
    }
}
