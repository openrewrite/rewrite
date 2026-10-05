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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class MergeSpacesVisitorTest {

    @Test
    void ignoresMismatchedContextForParentheses() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
          .parse("class Test { boolean value = (true); }")
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();
        J.Parentheses<?> parens = parentheses(cu);

        J merged = new MergeSpacesVisitor(emptyList()).visit(parens, cu);

        assertThat(merged).isSameAs(parens);
    }

    private static J.Parentheses<?> parentheses(J.CompilationUnit cu) {
        AtomicReference<J.Parentheses<?>> parentheses = new AtomicReference<>();
        new JavaIsoVisitor<AtomicReference<J.Parentheses<?>>>() {
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens,
                                                                    AtomicReference<J.Parentheses<?>> found) {
                found.set(parens);
                return super.visitParentheses(parens, found);
            }
        }.visit(cu, parentheses);
        return parentheses.get();
    }
}
