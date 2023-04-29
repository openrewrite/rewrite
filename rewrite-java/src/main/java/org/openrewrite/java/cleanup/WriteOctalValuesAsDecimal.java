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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class WriteOctalValuesAsDecimal extends Recipe {
    @Override
    public String getDisplayName() {
        return "Write octal values as decimal";
    }

    @Override
    public String getDescription() {
        return "Developers may not recognize octal values as such, mistaking them instead for decimal values.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1314");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                String src = literal.getValueSource();
                if (src != null && src.startsWith("0")) {
                    if (src.length() >= 2 &&
                        src.charAt(1) != 'x' && src.charAt(1) != 'X' &&
                        src.charAt(1) != 'b' && src.charAt(1) != 'B' &&
                        src.charAt(1) != '.' &&
                        src.charAt(src.length() - 1) != 'L' && src.charAt(src.length() - 1) != 'l' &&
                        src.charAt(src.length() - 1) != 'F' && src.charAt(src.length() - 1) != 'f' &&
                        src.charAt(src.length() - 1) != 'D' && src.charAt(src.length() - 1) != 'd' &&
                        !src.contains(".")) {
                        assert literal.getValue() != null;
                        return literal.withValueSource(literal.getValue().toString());
                    }
                }
                return super.visitLiteral(literal, executionContext);
            }
        };
    }
}
