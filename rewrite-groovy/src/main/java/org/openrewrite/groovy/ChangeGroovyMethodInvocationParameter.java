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
package org.openrewrite.groovy;


import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.UUID;

public class ChangeGroovyMethodInvocationParameter extends Recipe {
    @Option
    public String key;

    @Option
    public String value;

    public ChangeGroovyMethodInvocationParameter() {
    }

    public ChangeGroovyMethodInvocationParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Change groovy a  parameter";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "It updates the value of a given parameter on all groovy method invocations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J visitMapEntry(G.MapEntry mapEntry, ExecutionContext executionContext) {
                mapEntry = (G.MapEntry) super.visitMapEntry(mapEntry, executionContext);

                if (mapEntry.getValue().toString().equals("'" + value + "'")) {
                    return mapEntry;
                }

                if (mapEntry.getKey().toString().equals(key)) {
                    final J.Literal valueLiteral = new J.Literal(UUID.randomUUID(), Space.SINGLE_SPACE, Markers.EMPTY, "'" + value + "'", "'" + value + "'", null, JavaType.Primitive.String);
                    return mapEntry.withValue(valueLiteral);
                }

                return mapEntry;
            }
        };
    }
}
