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
package org.openrewrite.groovy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.tree.G;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LazyGStringTest {
    @ParameterizedTest
    @ValueSource(strings = {
      "def value = \"${-> projectExt.gitProps['git.commit.id.describe']}\"\n",
      "def value = \"before ${ -> 42 } after\"\n",
      "def value = \"${ writer -> writer << 42 }\"\n",
      "def value = \"${-> 1} and ${-> 2}\"\n",
      "def value = \"${-> [1, 2].collect { it + 1 }}\"\n",
      "def value = \"${{ -> 42 }}\"\n",
      "def value = \"${if (true) { return 42 }; return 0}\"\n",
      "def value = \"${42}\"\n"
    })
    void roundTrip(String source) {
        SourceFile parsed = GroovyParser.builder().build()
          .parse(new InMemoryExecutionContext(), source).findFirst().orElseThrow();
        assertInstanceOf(G.CompilationUnit.class, parsed);
        assertEquals(source, parsed.printAll());
    }
}
