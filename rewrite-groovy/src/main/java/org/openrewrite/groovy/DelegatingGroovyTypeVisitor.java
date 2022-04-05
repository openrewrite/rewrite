/*
 * Copyright 2022 the original author or authors.
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


import org.openrewrite.Tree;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

public class DelegatingGroovyTypeVisitor<T extends JavaVisitor<Integer>> extends GroovyVisitor<Integer> {
    private final T delegate;
    public DelegatingGroovyTypeVisitor(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, Integer p) {
        if (tree instanceof G) {
            return super.visit(tree, p);
        }
        return delegate.visit(tree, p);
    }

    @Override
    public G.GString visitGString(G.GString gString, Integer p) {
        G.GString g = (G.GString) super.visitGString(gString, p);
        return g.withType(delegate.visitType(gString.getType(), p));
    }

    @Override
    public G.ListLiteral visitListLiteral(G.ListLiteral listLiteral, Integer p) {
        G.ListLiteral l = (G.ListLiteral) super.visitListLiteral(listLiteral, p);
        return l.withType(delegate.visitType(listLiteral.getType(), p));
    }

    @Override
    public G.MapEntry visitMapEntry(G.MapEntry mapEntry, Integer p) {
        G.MapEntry m = (G.MapEntry) super.visitMapEntry(mapEntry, p);
        return m.withType(delegate.visitType(mapEntry.getType(), p));
    }

    @Override
    public G.MapLiteral visitMapLiteral(G.MapLiteral mapLiteral, Integer p) {
        G.MapLiteral m = (G.MapLiteral) super.visitMapLiteral(mapLiteral, p);
        return m.withType(delegate.visitType(mapLiteral.getType(), p));
    }

    @Override
    public G.Binary visitBinary(G.Binary binary, Integer p) {
        G.Binary b = (G.Binary) super.visitBinary(binary, p);
        return b.withType(delegate.visitType(binary.getType(), p));
    }
}
