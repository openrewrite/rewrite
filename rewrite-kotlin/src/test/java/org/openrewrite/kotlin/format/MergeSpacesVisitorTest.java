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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.tree.K;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MergeSpacesVisitorTest {

    @Test
    void ignoresMismatchedContextForParentheses() {
        K.CompilationUnit cu = KotlinParser.builder().build()
          .parse("class Test { val value = (true) }")
          .map(K.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();
        J.Parentheses<?> parens = parentheses(cu);

        J merged = new MergeSpacesVisitor(IntelliJ.wrappingAndBraces()).visit(parens, cu);

        assertThat(merged).isSameAs(parens);
    }

    private static J.Parentheses<?> parentheses(K.CompilationUnit cu) {
        AtomicReference<J.Parentheses<?>> parentheses = new AtomicReference<>();
        new KotlinIsoVisitor<AtomicReference<J.Parentheses<?>>>() {
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
