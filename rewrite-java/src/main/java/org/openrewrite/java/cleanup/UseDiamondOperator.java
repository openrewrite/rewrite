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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class UseDiamondOperator extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use diamond operator";
    }

    @Override
    public String getDescription() {
        return "The diamond operator (`<>`) should be used. Java 7 introduced the diamond operator (<>) to reduce the verbosity of generics code. For instance, instead of having to declare a List's type in both its declaration and its constructor, you can now simplify the constructor declaration with `<>`, and the compiler will infer the type.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J.NewClass n = super.visitNewClass(newClass, executionContext);
                if (n.getClazz() instanceof J.ParameterizedType && n.getBody() == null) {
                    J.ParameterizedType parameterizedType = (J.ParameterizedType) n.getClazz();
                    if (parameterizedType.getTypeParameters() != null && !parameterizedType.getTypeParameters().isEmpty()) {
                        if (parameterizedType.getTypeParameters().size() == 1 && !(parameterizedType.getTypeParameters().get(0) instanceof J.Empty)) {
                            n = n.withClazz(parameterizedType.withTypeParameters(singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))));
                        }
                    }
                }
                return n;
            }
        };
    }
}
