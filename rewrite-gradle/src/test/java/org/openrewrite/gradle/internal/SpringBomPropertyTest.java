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
package org.openrewrite.gradle.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.tree.JavaSourceFile;

import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class SpringBomPropertyTest {

    @ParameterizedTest
    @ValueSource(strings = {"build.gradle", "build.gradle.kts"})
    void emptyBuildScriptIsLeftUnchanged(String path) {
        JavaSourceFile empty = (JavaSourceFile) GradleParser.builder().build()
          .parseInputs(singletonList(Parser.Input.fromString(Paths.get(path), "")), null,
            new InMemoryExecutionContext(t -> {
                throw new AssertionError(t);
            }))
          .findFirst()
          .orElseThrow();

        assertThat(SpringBomProperty.addDeclaration(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), empty),
          "log4j2.version", "2.17.1"))
          .isSameAs(empty);
    }
}
