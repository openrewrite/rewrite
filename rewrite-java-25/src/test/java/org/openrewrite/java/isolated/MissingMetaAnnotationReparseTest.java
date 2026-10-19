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
import org.openrewrite.SourceFile;
import org.openrewrite.java.Java25Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingMetaAnnotationReparseTest {

    @TempDir
    static Path fixtureClasses;

    @BeforeAll
    static void compileFixture() throws Exception {
        Path meta = fixtureClasses.resolve("Meta.java");
        Files.writeString(meta, """
          package fixture;
          import java.lang.annotation.*;
          @Target(ElementType.ANNOTATION_TYPE)
          @Retention(RetentionPolicy.RUNTIME)
          public @interface Meta {}
          """);
        Path marked = fixtureClasses.resolve("Marked.java");
        Files.writeString(marked, """
          package fixture;
          import java.lang.annotation.*;
          @Target(ElementType.METHOD)
          @Retention(RetentionPolicy.RUNTIME)
          @Meta
          public @interface Marked {}
          """);
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
          "-d", fixtureClasses.toString(), meta.toString(), marked.toString());
        assertThat(exit).isZero();
        Files.delete(fixtureClasses.resolve("fixture/Meta.class"));
    }

    @Test
    void secondParseWithSameBuilderToleratesMissingMetaAnnotation() {
        // given
        String source = """
          import fixture.Marked;
          class A {
              @Marked
              void m() {
                  boolean b = n() != null && n() > 0;
              }
              Integer n() {
                  return 1;
              }
          }
          """;
        JavaParser.Builder<?, ?> builder = Java25Parser.builder().classpath(List.of(fixtureClasses));
        assertThat(builder.build().parse(new InMemoryExecutionContext(), source).toList())
          .singleElement().isInstanceOf(J.CompilationUnit.class);

        // when
        List<Throwable> errors = new ArrayList<>();
        List<SourceFile> second = builder.clone().build()
          .parse(new InMemoryExecutionContext(errors::add), source).toList();

        // then
        assertThat(errors).isEmpty();
        assertThat(second).singleElement().isInstanceOf(J.CompilationUnit.class);
    }
}
