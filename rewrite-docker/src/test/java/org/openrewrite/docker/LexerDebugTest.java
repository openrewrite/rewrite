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
package org.openrewrite.docker;

import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;
import org.openrewrite.docker.internal.grammar.DockerLexer;

import static org.assertj.core.api.Assertions.assertThat;

class LexerDebugTest {

    @Test
    void debugHeredocTokens() {
        String input = """
FROM ubuntu:20.04
RUN <<EOF
addgroup -S docker
adduser -S --shell /bin/bash --ingroup docker vscode
EOF
""";
        CharStream cs = CharStreams.fromString(input);
        DockerLexer lexer = new DockerLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        System.out.println("=== Tokens ===");
        for (Token token : tokens.getTokens()) {
            String typeName = DockerLexer.VOCABULARY.getSymbolicName(token.getType());
            if (typeName == null) typeName = String.valueOf(token.getType());
            System.out.printf("Line %d:%d %-20s '%s'%n",
                token.getLine(), token.getCharPositionInLine(),
                typeName, token.getText().replace("\n", "\\n"));

            // Check that we don't get SHELL token inside heredoc
            if (token.getLine() >= 3 && token.getLine() <= 4) {
                assertThat(typeName)
                    .describedAs("Token at line %d:%d should not be SHELL", token.getLine(), token.getCharPositionInLine())
                    .isNotEqualTo("SHELL");
            }
        }
    }
}
