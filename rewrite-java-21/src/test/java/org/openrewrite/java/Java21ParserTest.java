/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.isolated.ReloadableJava21Parser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Java21ParserTest implements RewriteTest {

    @Test
    void shouldLoadResourceFromClasspath() throws IOException {
        Files.deleteIfExists(Paths.get(System.getProperty("user.home"), ".rewrite", "classpath", "jackson-annotations-2.17.1.jar"));
        rewriteRun(spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jackson-annotations")));
    }

    @Test
    void stringIndexOutOfBoundsException() throws IOException {
        String src = """
          package nl.issue.test;
          
          import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
          import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
          
          import lombok.Builder;
          
          @Builder
          @JsonDeserialize(builder = Clazz.ClazzBuilder.class)
          public class Clazz {
          
              private final String accountID;
              private final String documentNumber;
          
              @JsonPOJOBuilder(withPrefix = "")
              public static final class ClazzBuilder {}
          }
          """;
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        ReloadableJava21Parser build = ReloadableJava21Parser.builder()
          .classpath("lombok")
          .build();
        Stream<SourceFile> sourceFileStream = build.parseInputs(List.of(Parser.Input.fromString(src)), null, ctx);
        Optional<SourceFile> optionalSourceFile = sourceFileStream.findFirst();
        assertThat(optionalSourceFile).isPresent();
        SourceFile sourceFile = optionalSourceFile.get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);
    }
}
