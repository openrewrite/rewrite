/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ParserTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/8707")
    @Test
    void inferredLambdaParametersAfterLombokBuilderInvocationArePrintIdempotent() {
        String source = """
          import java.util.List;
          import java.util.Map;
          import java.util.stream.Collectors;
          import java.util.stream.Stream;
          import lombok.Builder;

          class RoundTrip {
              void convert(Map<String, List<String>> first, Map<String, List<String>> second) {
                  Map<String, List<String>> merged = Stream.concat(first.entrySet().stream(), second.entrySet().stream())
                      .collect(Collectors.toMap(Map.Entry::getKey,
                          entry -> ResultView.builder()
                              .value(entry.getKey())
                              .details(entry.getValue())
                              .build(),
                          (a, b) -> {
                              a.addAll(b);
                              return a;
                          }));
              }
          }

          @Builder
          class ResultView {
              String value;
              List<String> details;
          }
          """;

        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        SourceFile parsed = JavaParser.fromJavaVersion()
          .classpath("lombok")
          .build()
          .parse(ctx, source)
          .findFirst()
          .get();

        assertThat(parsed.printAll()).isEqualTo(source);
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/4914")
    @Test
    void parseString() throws Exception {
        // path needs to be resolvable from `rewrite-java-8` etc.
        Path targetFile = Paths.get("../rewrite-java-tck/src/main/java/org/openrewrite/java/tree/ParserTest.java");
        @SuppressWarnings("SimplifyStreamApiCallChains") List<SourceFile> ignore = JavaParser.fromJavaVersion()
          .build()
          .parse(new String(Files.readAllBytes(targetFile)))
          .collect(toList());
    }
}
