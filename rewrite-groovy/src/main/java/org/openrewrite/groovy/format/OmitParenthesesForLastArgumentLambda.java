/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;

import static org.openrewrite.Tree.randomId;

public class OmitParenthesesForLastArgumentLambda extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move a closure which is the last argument of a method invocation out of parentheses";
    }

    @Override
    public String getDescription() {
        return "Groovy allows a shorthand syntax that allows a closure to be placed outside of parentheses.";
    }

    @Override
    public GroovyVisitor<ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {

        };
    }
}
