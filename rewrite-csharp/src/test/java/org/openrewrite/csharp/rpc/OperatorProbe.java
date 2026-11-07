/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.rpc;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.CSharpIsoVisitor;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

/**
 * Describes each conversion operator's method type as this Java peer received it.
 * It renames every operator's method type in {@link #visitType},
 * so the C# side receives values the Java visitor produced.
 */
public class OperatorProbe extends CSharpIsoVisitor<ExecutionContext> {
    @Override
    public Cs.ConversionOperatorDeclaration visitConversionOperatorDeclaration(Cs.ConversionOperatorDeclaration conversion,
                                                                               ExecutionContext ctx) {
        JavaType.Method received = conversion.getMethodType();
        return SearchResult.found(super.visitConversionOperatorDeclaration(conversion, ctx),
                received == null ? "no method type" : received.toString());
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
        Object declaration = getCursor().getValue();
        if (javaType instanceof JavaType.Method &&
            (declaration instanceof Cs.ConversionOperatorDeclaration ||
             declaration instanceof Cs.OperatorDeclaration)) {
            JavaType.Method method = (JavaType.Method) javaType;
            return method.withName("probed:" + method.getName());
        }
        return javaType;
    }
}
