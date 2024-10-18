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

public class ChangeStringValueOfNamedParameterInMethodInvocation extends Recipe {

    @Option
    public String methodName;

    @Option
    public String key;

    @Option
    public String value;

    public ChangeStringValueOfNamedParameterInMethodInvocation(final String methodName, final String key, final String value) {
        this.methodName = methodName;
        this.key = key;
        this.value = value;
    }

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Change the value of a groovy named string parameter of a method invocation";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Changes the value of a given parameter in a given groovy method invocation, supporting Strings and GStrings.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J visitMapEntry(G.MapEntry mapEntry, ExecutionContext ctx) {
                mapEntry = (G.MapEntry) super.visitMapEntry(mapEntry, ctx);

                if (!isInTargetMethod()) {
                    return mapEntry;
                }

                char quote = extractQuoting(mapEntry);
                if (quote != '\'' && extractQuoting(mapEntry) != '"') {
                    return mapEntry;
                }

                if (!mapEntry.getKey().toString().equals(key)) {
                    return mapEntry;
                }

                if (mapEntry.getValue().toString().equals(value)) {
                    return mapEntry;
                }

                return replaceValue(mapEntry, quote);
            }

            private boolean isInTargetMethod() {
                return getCursor().firstEnclosingOrThrow(J.MethodInvocation.class).getSimpleName().equals(methodName);
            }

            private G. MapEntry replaceValue(final G.MapEntry mapEntry, char quote) {
                return mapEntry.withValue(new J.Literal(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, value, String.format("%c%s%c", quote, value, quote), null, JavaType.Primitive.String));
            }

            private char extractQuoting(final G.MapEntry mapEntry) {
                return mapEntry.getValue().printTrimmed(getCursor()).charAt(0);
            }
        };
    }
}
