/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;

public class GStringCurlyBraces extends Recipe {
    @Override
    public String getDisplayName() {
        return "Groovy GString curly braces";
    }

    @Override
    public String getDescription() {
        return "In Groovy [GStrings](https://docs.groovy-lang.org/latest/html/api/groovy/lang/GString.html), curly braces are optional for single variable expressions. " +
               "This recipe adds them, so that the expression is always surrounded by curly braces.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitGStringValue(G.GString.Value value, ExecutionContext executionContext) {
                return value.withEnclosedInBraces(true);
            }
        };
    }
}
