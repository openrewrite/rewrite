/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.csharp.service;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.csharp.CSharpIsoVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.tree.Space;

public class CSharpWhitespaceValidationService implements WhitespaceValidationService {
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CSharpIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                if (!StringUtils.isBlank(space.getWhitespace())) {
                    return space.withWhitespace("~~(non-whitespace)~~>" + space.getWhitespace() + "<~~");
                }
                return space;
            }
        };
    }
}
