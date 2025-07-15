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
package org.openrewrite.scala;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.scala.tree.S;

/**
 * ScalaVisitor extends JavaVisitor to support visiting both Java (J) and Scala (S) AST elements.
 * This allows Scala code to be processed by Java-focused recipes while also supporting
 * Scala-specific transformations.
 */
public class ScalaVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof S.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "scala";
    }

    public J visitCompilationUnit(S.CompilationUnit cu, P p) {
        S.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        
        if (c.getPackageDeclaration() != null) {
            c = c.withPackageDeclaration(visitAndCast(c.getPackageDeclaration(), p));
        }
        
        c = c.withImports(ListUtils.map(c.getImports(), i -> visitAndCast(i, p)));
        c = c.withStatements(ListUtils.map(c.getStatements(), s -> visitAndCast(s, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        
        return c;
    }

    // Additional visit methods for Scala-specific constructs will be added here
    // as we implement more S types (e.g., visitTrait, visitObject, visitMatch, etc.)

    public J visitTuplePattern(S.TuplePattern tuplePattern, P p) {
        S.TuplePattern t = tuplePattern;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.getPadding().withElements(visitContainer(t.getPadding().getElements(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return t;
    }
}