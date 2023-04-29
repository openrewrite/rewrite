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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.TabsAndIndentsVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Loop;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singleton;

public class ControlFlowIndentation extends Recipe {
    @Override
    public String getDisplayName() {
        return "Control flow statement indentation";
    }

    @Override
    public String getDescription() {
        return "Program flow control statements like `if`, `while`, and `for` can omit curly braces when they apply to " +
                "only a single statement. This recipe ensures that any statements which follow that statement are correctly " +
                "indented to show they are not part of the flow control statement.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-2681");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            TabsAndIndentsStyle tabsAndIndentsStyle;

            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                TabsAndIndentsStyle style = ((SourceFile)cu).getStyle(TabsAndIndentsStyle.class);
                if (style == null) {
                    style = IntelliJ.tabsAndIndents();
                }
                tabsAndIndentsStyle = style;
                return super.visitJavaSourceFile(cu, executionContext);
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                J.Block b = super.visitBlock(block, executionContext);
                AtomicBoolean foundControlFlowRequiringReformatting = new AtomicBoolean(false);
                return b.withStatements(ListUtils.map(b.getStatements(), (i, statement) -> {
                    if(foundControlFlowRequiringReformatting.get() || shouldReformat(statement)) {
                        foundControlFlowRequiringReformatting.set(true);
                        return (Statement) new TabsAndIndentsVisitor<>(tabsAndIndentsStyle).visit(statement, executionContext, getCursor());
                    }
                    return statement;
                }));
            }

            boolean shouldReformat(Statement s) {
                if(s instanceof J.If) {
                    return shouldReformat((J.If) s);
                } else if(s instanceof Loop) {
                    Statement body = ((Loop) s).getBody();
                    return !(body instanceof J.Block);
                }
                return false;
            }

            boolean shouldReformat(J.If s) {
                Statement thenPart = s.getThenPart();

                if (!(thenPart instanceof J.Block)) {
                    return true;
                }

                return shouldReformat(s.getElsePart());
            }

            boolean shouldReformat(@Nullable J.If.Else s) {
                if (s == null) {
                    return false;
                }
                Statement body = s.getBody();
                if (body instanceof J.If) {
                    return shouldReformat(((J.If) body));
                }
                return !(body instanceof J.Block);
            }
        };
    }
}
