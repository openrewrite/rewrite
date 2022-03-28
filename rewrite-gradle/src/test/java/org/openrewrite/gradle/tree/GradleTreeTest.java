/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.gradle.tree;

import org.intellij.lang.annotations.Language;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

public interface GradleTreeTest {
    default void assertParsePrintAndProcess(@Language("groovy") String code) {
        assertParsePrintAndProcess(code, true);
    }

    default void assertParsePrintAndProcess(@Language("groovy") String code, boolean validateTypes) {
        String trimmed = StringUtils.trimIndent(code);
        G.CompilationUnit cu = new GradleParser(GroovyParser.builder().logCompilationWarningsAndErrors(true)).parse(
                new InMemoryExecutionContext(t -> {
                    throw new RuntimeException(t.getMessage(), t);
                }),
                trimmed
        ).iterator().next();

        J processed = new GroovyVisitor<>().visit(cu, new Object());
        assertThat(processed).as("Processing is idempotent").isSameAs(cu);

        assertThat(cu.printAll()).as("Prints back to the original code").isEqualTo(trimmed);

        if (validateTypes) {
            //noinspection ConstantConditions
            G.CompilationUnit missingTypes = (G.CompilationUnit) new FindMissingTypes().getVisitor().visitNonNull(cu, null);
            assertThat(missingTypes.printAll()).as("All functions should have type attribution").isEqualTo(cu.printAll());
        }
    }
}
