/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinParserTest implements RewriteTest {

    @Test
    void classDefinitionFromDependsOn() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().dependsOn("""
            package foo.bar

            class MyClass
            """)),
          kotlin(
            """
              import foo.bar.MyClass

              val myClass: MyClass? = null
              """
          )
        );
    }

    @Test
    void dependsOnWithAbsoluteRelativeTo(@TempDir Path tempDir) {
        rewriteRun(
          spec -> spec
            .relativeTo(tempDir)
            .parser(KotlinParser.builder().dependsOn("""
              package foo.bar

              class MyClass
              """)),
          kotlin(
            """
              import foo.bar.MyClass

              val myClass: MyClass? = null
              """
          )
        );
    }

    @Test
    void dependsOnDoesNotLeakAsParseErrorWhenParseThrows() {
        KotlinParser parser = KotlinParser.builder()
          .dependsOn(
            """
              package foo.bar

              class MyClass
              """
          )
          .build();

        // An input whose source supplier throws on its first call forces KotlinParser#parse
        // to fail before any per-CU processing, exercising the catch block in parseInputs.
        // Subsequent calls return an empty stream so ParseError.build can still capture the input.
        AtomicInteger callCount = new AtomicInteger();
        Parser.Input throwingInput = new Parser.Input(
          Paths.get("Bad.kt"),
          null,
          () -> {
              if (callCount.getAndIncrement() == 0) {
                  throw new RuntimeException("intentional parse failure");
              }
              return new ByteArrayInputStream(new byte[0]);
          },
          true
        );

        List<SourceFile> results = parser
          .parseInputs(singletonList(throwingInput), null, new InMemoryExecutionContext(t -> {
          }))
          .collect(Collectors.toList());

        // dependsOn must not leak into the returned stream on the error path
        assertThat(results)
          .extracting(SourceFile::getSourcePath)
          .containsExactly(Paths.get("Bad.kt"));
    }

    @Test
    void multiDollarStringInterpolation() {
        rewriteRun(
          kotlin(
            """
              val x = $$"$something"
              """
          )
        );
    }

}
