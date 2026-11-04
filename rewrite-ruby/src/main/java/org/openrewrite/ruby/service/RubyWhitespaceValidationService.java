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
package org.openrewrite.ruby.service;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.tree.Space;
import org.openrewrite.ruby.RubyIsoVisitor;

/**
 * Ruby treats a backslash line continuation as whitespace, so unlike the Java implementation this
 * validator accepts backslashes alongside ordinary whitespace.
 */
public class RubyWhitespaceValidationService implements WhitespaceValidationService {

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RubyIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                space = super.visitSpace(space, loc, ctx);
                if (!space.getWhitespace().replace("\\", "").trim().isEmpty()) {
                    return space.withWhitespace("~~(non-whitespace)~~>" + space.getWhitespace() + "<~~");
                }
                return space;
            }
        };
    }
}
