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
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

public class MethodNameCasing extends Recipe {
    @Override
    public String getDisplayName() {
        return "Method name casing";
    }

    @Override
    public String getDescription() {
        return "Method names should comply with a naming convention.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-100");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern standardMethodName = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                if (method.getType() != null &&
                        !method.isConstructor() &&
                        !standardMethodName.matcher(method.getSimpleName()).matches()) {
                    StringBuilder standardized = new StringBuilder();

                    char[] name = method.getSimpleName().toCharArray();
                    for (int i = 0; i < name.length; i++) {
                        char c = name[i];

                        if (i == 0) {
                            // the java specification requires identifiers to start with [a-zA-Z$_]
                            if (c != '$' && c != '_') {
                                standardized.append(Character.toLowerCase(c));
                            }
                        } else {
                            if (!Character.isLetterOrDigit(c)) {
                                while (i < name.length && (!Character.isLetterOrDigit(name[i]) || name[i] > 'z')) {
                                    i++;
                                }
                                standardized.append(Character.toUpperCase(name[i]));
                            } else {
                                standardized.append(c);
                            }
                        }
                    }

                    doNext(new ChangeMethodName(MethodMatcher.methodPattern(method), standardized.toString(), null));
                }

                return super.visitMethodDeclaration(method, executionContext);
            }
        };
    }
}
