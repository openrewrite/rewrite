/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin2;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin2.tree.Kt;

/**
 * Visitor for Kotlin 2 language constructs.
 * This visitor extends JavaVisitor to leverage common JVM language support
 * while adding Kotlin 2-specific visit methods.
 */
public class Kotlin2Visitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Kt.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "kotlin2";
    }

    public J visitCompilationUnit(Kt.CompilationUnit cu, P p) {
        Kt.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        
        if (c.getPackageDeclaration() != null) {
            c = c.withPackageDeclaration(visitAndCast(c.getPackageDeclaration(), p));
        }
        
        c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, p)));
        c = c.withStatements(ListUtils.map(c.getStatements(), s -> visitAndCast(s, p)));
        c = c.withEof(visitSpace(c.getEof(), p));
        
        return c;
    }

    public J visitContextReceiver(Kt.ContextReceiver contextReceiver, P p) {
        Kt.ContextReceiver c = contextReceiver;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withContext(visitAndCast(c.getContext(), p));
        return c;
    }

    public J visitDefinitelyNonNullableType(Kt.DefinitelyNonNullableType type, P p) {
        Kt.DefinitelyNonNullableType t = type;
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withBaseType(visitAndCast(t.getBaseType(), p));
        return t;
    }
}