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
package org.openrewrite.javascript;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.tree.JS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaScriptParserTest {

    private JavaScriptParser parser;

    @BeforeEach
    void before() {
        this.parser = JavaScriptParser.builder()
          .nodePath(Path.of("node"))
          .installationDir(Path.of("./rewrite/dist/src/rpc"))
          .build();
    }

    @Test
    void helloJavaScript() {
        @Language("js")
        String helloWorld = """
          console.info("Hello world!")
          """;
        Parser.Input input = Parser.Input.fromString(Paths.get("helloworld.js"), helloWorld);
        Optional<SourceFile> javascript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(javascript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(javascript.get()).satisfies(cu -> {
            assertThat(cu.printAll()).isEqualTo(helloWorld);
            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }

    @Test
    void helloTypeScript() {
        @Language("ts")
        String helloWorld = """
          const message: string = "Hello world!";
          console.info(message);
          """;
        Parser.Input input = Parser.Input.fromString(Paths.get("helloworld.ts"), helloWorld);
        Optional<SourceFile> typescript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(typescript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(typescript.get()).satisfies(cu -> {
            assertThat(cu.printAll()).isEqualTo(helloWorld);
            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }

    @Test
    @Disabled
    void complexTypeScript() throws MalformedURLException {
        URL url = URI.create("https://raw.githubusercontent.com/sinclairzx81/typebox/f958156785350aa052c5f822bc2970d0945d887b/src/syntax/parser.ts").toURL();
        Parser.Input input = new Parser.Input(Paths.get("parser.ts"), null, () -> {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Optional<SourceFile> typescript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst();
        assertThat(typescript).containsInstanceOf(JS.CompilationUnit.class);
        assertThat(typescript.get()).satisfies(cu -> {
            assertThat(cu.printAll()).isEqualTo(input.getSource(new InMemoryExecutionContext()).readFully());
//            assertThat(cu.getSourcePath()).isEqualTo(input.getPath());
        });
    }
}
