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
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;

import java.nio.file.Path;
import java.util.List;

public class JavaScriptParserTest {

    private JavaScriptParser parser;

    @BeforeEach
    void before() {
//        this.log = new PrintStream(new FileOutputStream("rpc.java.log"));
        this.parser = JavaScriptParser.builder()
          .nodePath(Path.of("node"))
          .installationDir(Path.of("./rewrite/dist/src/rpc"))
          .build();
    }

    @Test
    void parseJavascript() {
        @Language("js")
        String helloWorld = """
          console.info("Hello world!")
          """;
        Parser.Input input = Parser.Input.fromString(helloWorld);
        SourceFile javascript = parser.parseInputs(List.of(input), null, new InMemoryExecutionContext()).findFirst().get();
        System.out.println(javascript);
    }
}
