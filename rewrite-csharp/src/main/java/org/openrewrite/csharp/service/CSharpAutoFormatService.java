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
package org.openrewrite.csharp.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.tree.J;

/**
 * No-op auto-format service for C#. The actual formatting is handled
 * on the .NET side via Roslyn's built-in formatter.
 * <p>
 * This service prevents the Java-side formatter from running on C# trees,
 * which would produce incorrect results since Java formatting rules
 * don't apply to C#.
 */
public class CSharpAutoFormatService extends AutoFormatService {

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
