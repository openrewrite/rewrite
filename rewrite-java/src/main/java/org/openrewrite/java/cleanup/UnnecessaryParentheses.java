/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.UnnecessaryParenthesesStyle;
import org.openrewrite.java.tree.JavaSourceFile;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class UnnecessaryParentheses extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unnecessary parentheses";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary parentheses from code where extra parentheses pairs are redundant.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-1110", "RSPEC-1611"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                UnnecessaryParenthesesStyle style = ((SourceFile) cu).getStyle(UnnecessaryParenthesesStyle.class);
                if (style == null) {
                    style = Checkstyle.unnecessaryParentheses();
                }
                doAfterVisit(new UnnecessaryParenthesesVisitor<>(style));
                return cu;
            }
        };
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }
}
