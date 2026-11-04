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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class OmitParenthesesForLastArgumentLambda extends Recipe {

    @Getter
    final String displayName = "Move a closure which is the last argument of a method invocation out of parentheses";

    @Getter
    final String description = "Groovy allows a shorthand syntax that allows a closure to be placed outside of parentheses.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new OmitParenthesesForLastArgumentLambdaVisitor<>(null);
    }
}
