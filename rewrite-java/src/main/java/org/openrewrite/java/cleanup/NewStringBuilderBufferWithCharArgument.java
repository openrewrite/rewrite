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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.Set;

public class NewStringBuilderBufferWithCharArgument extends Recipe {
    @Override
    public String getDisplayName() {
        return "StringBuilder and StringBuffer character constructor arg to String";
    }

    @Override
    public String getDescription() {
        return "Instantiating a `StringBuilder` or a `StringBuffer` with a `Character` results in the `int` representation of the character being used for the initial size.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1317");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J.NewClass nc = super.visitNewClass(newClass, executionContext);
                if ((TypeUtils.isOfClassType(nc.getType(), "java.lang.StringBuilder") || TypeUtils.isOfClassType(nc.getType(), "java.lang.StringBuffer"))
                        && nc.getArguments() != null && nc.getArguments().get(0).getType() == JavaType.Primitive.Char) {
                    nc = nc.withArguments(ListUtils.mapFirst(nc.getArguments(), arg -> {
                        J.Literal l = (J.Literal) arg;
                        l = l.withType(JavaType.buildType("String"));
                        if (l.getValueSource() != null) {
                            l = l.withValueSource(l.getValueSource().replace("'", "\""));
                        }
                        return l;
                    }));
                }
                return nc;
            }
        };
    }
}
