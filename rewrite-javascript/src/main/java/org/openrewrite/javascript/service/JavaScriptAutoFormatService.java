/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.J;

public class JavaScriptAutoFormatService extends AutoFormatService {

    @Override
    public <P> JavaVisitor<P> autoFormatVisitor(@Nullable Tree stopAfter) {
        return new JavaIsoVisitor<P>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, P ctx) {
                return (J) tree;
            }
        };
    }
}
