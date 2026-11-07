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

import org.openrewrite.ExecutionContext;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

/**
 * Marks each element access with the type it arrived with.
 * Retypes it {@code long} so that the reply carries a type.
 */
public class ArrayAccessTypeProbe extends CSharpVisitor<ExecutionContext> {
    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
        J.ArrayAccess a = (J.ArrayAccess) super.visitArrayAccess(arrayAccess, ctx);
        return SearchResult.found(a.withType(JavaType.Primitive.Long), String.valueOf(a.getType()));
    }
}
