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
package org.openrewrite.scala;

import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.scala.tree.S;

import java.util.Collections;
import java.util.List;

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
        c = c.withStatements(ListUtils.map(c.getStatements(), s -> {
            try {
                return visitAndCast(s, p);
            } catch (Exception e) {
                // Some Java recipes may fail on Scala-specific class structures
                // (e.g., missing primaryConstructor). Return unchanged rather than crashing.
                return s;
            }
        }));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));

        // Bridge to Java visitor: create a temporary J.CompilationUnit so that Java recipes
        // that override visitCompilationUnit(J.CompilationUnit, P) can process imports, classes, etc.
        J.CompilationUnit tempJcu = new J.CompilationUnit(
                Tree.randomId(),
                c.getPrefix(),
                c.getMarkers(),
                c.getSourcePath(),
                c.getFileAttributes(),
                c.getCharset().name(),
                c.isCharsetBomMarked(),
                c.getChecksum(),
                c.getPadding().getPackageDeclaration(),
                c.getPadding().getImports(),
                Collections.emptyList(),
                c.getEof()
        );

        // Cast to JavaVisitor to invoke visitCompilationUnit(J.CompilationUnit, P) virtually.
        // When this visitor is an adapted delegate, the adapter's override will call
        // the original Java recipe's visitCompilationUnit (e.g., OrderImports).
        J.CompilationUnit result = (J.CompilationUnit) ((JavaVisitor<P>) this).visitCompilationUnit(tempJcu, p);

        // Sync changes from the J.CompilationUnit back to S.CompilationUnit
        if (result != tempJcu) {
            List<JRightPadded<J.Import>> newImports = result.getPadding().getImports();
            if (newImports != c.getPadding().getImports()) {
                c = (S.CompilationUnit) c.getPadding().withImports(newImports);
            }
            if (result.getPrefix() != c.getPrefix()) {
                c = c.withPrefix(result.getPrefix());
            }
            if (result.getMarkers() != c.getMarkers()) {
                c = c.withMarkers(result.getMarkers());
            }
            if (result.getEof() != c.getEof()) {
                c = c.withEof(result.getEof());
            }
        }

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

    public J visitWildcard(S.Wildcard wildcard, P p) {
        S.Wildcard w = wildcard;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        return w;
    }

    public J visitStatementExpression(S.StatementExpression statementExpression, P p) {
        // Transparent — just visit the inner statement
        return statementExpression.acceptScala(this, p);
    }

    public J visitTypeAscription(S.TypeAscription typeAscription, P p) {
        S.TypeAscription t = typeAscription;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withExpression(visitAndCast(t.getExpression(), p));
        t = t.withTypeTree(visitAndCast(t.getTypeTree(), p));
        return t;
    }

    public J visitTypeAlias(S.TypeAlias typeAlias, P p) {
        S.TypeAlias t = typeAlias;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return t;
    }

    public J visitPatternDefinition(S.PatternDefinition patDef, P p) {
        S.PatternDefinition pd = patDef;
        pd = pd.withPrefix(visitSpace(pd.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        pd = pd.withMarkers(visitMarkers(pd.getMarkers(), p));
        return pd;
    }

    public J visitFunctionCall(S.FunctionCall functionCall, P p) {
        S.FunctionCall f = functionCall;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.getPadding().withFunction(visitRightPadded(f.getPadding().getFunction(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        f = f.getPadding().withArguments(visitContainer(f.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
        return f;
    }
}
