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
package org.openrewrite.bash.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.bash.Assertions.bash;

class ArrayTest implements RewriteTest {

    @Test
    void emptyArray() {
        rewriteRun(
          bash(
            "arr=()\n"
          )
        );
    }

    @Test
    void arrayWithValues() {
        rewriteRun(
          bash(
            "arr=(one two three)\n"
          )
        );
    }

    @Test
    void arrayWithQuotedValues() {
        rewriteRun(
          bash(
            "arr=(\"hello world\" 'single quoted' plain)\n"
          )
        );
    }

    @Test
    void localEmptyArray() {
        rewriteRun(
          bash(
            "local -a latencies=()\n"
          )
        );
    }

    @Test
    void arrayAppend() {
        rewriteRun(
          bash(
            "latencies+=($latency)\n"
          )
        );
    }

    @Test
    void arrayAppendQuoted() {
        rewriteRun(
          bash(
            "args+=(-H \"Authorization: Bearer $token\")\n"
          )
        );
    }

    @Test
    void arrayWithCommandSubstitution() {
        rewriteRun(
          bash(
            "sorted=($(printf '%s\\n' \"${arr[@]}\" | sort -n))\n"
          )
        );
    }

    @Test
    void localArrayWithCommandSubstitution() {
        rewriteRun(
          bash(
            "local sorted=($(echo hello))\n"
          )
        );
    }

    @Test
    void multiLineArray() {
        rewriteRun(
          bash(
            "ARTIFACTS=(\n    \"org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.pom\"\n    \"junit/junit/4.13.2/junit-4.13.2.pom\"\n)\n"
          )
        );
    }

    @Test
    void associativeArrayDeclaration() {
        rewriteRun(
          bash(
            "declare -A map=([key1]=val1 [key2]=val2)\n"
          )
        );
    }
}
