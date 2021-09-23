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
package org.openrewrite.groovy.tree;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.TreeSerializer;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

public interface GroovyTreeTest {
    default void assertParsePrintAndProcess(String code) {
        G.CompilationUnit cu = GroovyParser.builder().build().parse(
                new InMemoryExecutionContext(t -> {
                    throw new RuntimeException(t.getMessage(), t);
                }),
                code
        ).iterator().next();

        J processed = new GroovyVisitor<>().visit(cu, new Object());
        assertThat(processed).as("Processing is idempotent").isSameAs(cu);

        TreeSerializer<G.CompilationUnit> treeSerializer = new TreeSerializer<>();
        G.CompilationUnit roundTripCu = treeSerializer.read(treeSerializer.write(cu));

        assertThat(roundTripCu.printAll())
                .as("Source code is printed the same after parsing")
                .isEqualTo(StringUtils.trimIndent(code));
    }
}
