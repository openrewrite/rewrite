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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class JavaParserCompilationErrorsTest {

    @Test
    void addErrorsToTheCollectionInTheExecutionContext() {
        assertThat(errors(
          """
            class A {
                String compiles = String.valueOf(1);
                String s = String.join(1);
            }
            """
        )).singleElement().satisfies(error -> {
            assertThat(error.getSource().getName()).isEqualTo("A.java");
            assertThat(error.getLineNumber()).isEqualTo(3);
            assertThat(error.getMessage(Locale.ENGLISH)).contains("int cannot be converted to java.lang.CharSequence");
        });
    }

    @Test
    void addErrorsOfFlowAnalysis() {
        assertThat(errors(
          """
            class A {
                void m() {
                    throw new Exception();
                }
            }
            """
        )).singleElement().satisfies(error ->
          assertThat(error.getMessage(Locale.ENGLISH)).contains("unreported exception java.lang.Exception"));
    }

    private static List<Diagnostic<? extends JavaFileObject>> errors(String source) {
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.COMPILATION_ERRORS, errors);
        //noinspection ResultOfMethodCallIgnored
        JavaParser.fromJavaVersion().build().parse(ctx, source).collect(toList());
        return errors;
    }
}
