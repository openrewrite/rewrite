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
package org.openrewrite.scala.marker;

import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks a {@link org.openrewrite.java.tree.J.MethodDeclaration} written in Scala 2 procedure
 * syntax, such as {@code def f() { ... }} or the abstract {@code def f}. Its result type is an
 * implied {@code Unit}, and a body follows without an {@code =}.
 */
public class ProcedureSyntax implements Marker {
    private final UUID id;

    public ProcedureSyntax(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ProcedureSyntax withId(UUID id) {
        return new ProcedureSyntax(id);
    }
}
