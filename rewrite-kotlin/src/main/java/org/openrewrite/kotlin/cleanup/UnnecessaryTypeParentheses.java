/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.kotlin.KotlinVisitor;

import java.time.Duration;

public class UnnecessaryTypeParentheses extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unnecessary parentheses on Kotlin types";
    }

    @Override
    public String getDescription() {
        return "In Kotlin, it's possible to add redundant nested parentheses in type definitions. This recipe is designed to remove those unnecessary parentheses.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinVisitor<ExecutionContext>() {
            @Override
            public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, ExecutionContext ctx) {
                J j = super.visitParenthesizedTypeTree(parTree, ctx);
                if (!(j instanceof J.ParenthesizedTypeTree)) {
                    return j;
                }

                Space prefix = j.getPrefix();
                TypeTree tt = (TypeTree) j;
                while (tt instanceof J.ParenthesizedTypeTree) {
                    tt = ((J.ParenthesizedTypeTree) tt).getParenthesizedType().getTree();
                }

                return tt.withPrefix(prefix);
            }
        };
    }
}
