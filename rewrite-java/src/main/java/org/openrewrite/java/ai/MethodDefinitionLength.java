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
package org.openrewrite.java.ai;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.TokenCount;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class MethodDefinitionLength extends Recipe {
    transient TokenCount tokens = new TokenCount(this);

    @Override
    public String getDisplayName() {
        return "Calculate token length of method definitions";
    }

    @Override
    public String getDescription() {
        return "Locates method definitions and predicts the number of token in each.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (cd == null) {
                    return md;
                }
                int numberOfTokens = (int) (md.printTrimmed(getCursor()).length() / 3.5);
                tokens.insertRow(ctx, new TokenCount.Row(md.getSimpleName(), numberOfTokens));
                return md;
            }
        };
    }
}
