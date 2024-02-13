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
package org.openrewrite.java.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;

public class JavaWhitespaceValidationService implements WhitespaceValidationService {
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext executionContext) {
                if(!StringUtils.isBlank(space.getWhitespace())) {
                    return space.withWhitespace("~~(non-whitespace)~~>" + space.getWhitespace() + "<~~");
                }
                return space;
            }
        };
    }
}
