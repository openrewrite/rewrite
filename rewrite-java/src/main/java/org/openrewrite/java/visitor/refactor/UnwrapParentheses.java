/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor;

import org.openrewrite.java.tree.J;

import java.util.UUID;

public class UnwrapParentheses extends ScopedJavaRefactorVisitor {
    public UnwrapParentheses(UUID scope) {
        super(scope);
    }

    @Override
    public String getName() {
        return "core.UnwrapParentheses";
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens) {
        return isScope(parens) ? parens.getTree().withFormatting(parens.getFormatting()) : super.visitParentheses(parens);
    }
}
