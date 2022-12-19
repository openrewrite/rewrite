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
package org.openrewrite.kotlin;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.tree.KLeftPadded;
import org.openrewrite.kotlin.tree.KRightPadded;
import org.openrewrite.kotlin.tree.KSpace;

public class KotlinVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof K.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "kotlin";
    }

    @Override
    public J visitJavaSourceFile(JavaSourceFile cu, P p) {
        return cu instanceof K.CompilationUnit ? visitCompilationUnit((K.CompilationUnit) cu, p) : cu;
    }

    public J visitCompilationUnit(K.CompilationUnit cu, P p) {
        K.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getPackageDeclaration() != null) {
            c = c.getPadding().withPackageDeclaration(visitRightPadded(c.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.withStatements(ListUtils.map(c.getStatements(), e -> visitAndCast(e, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("Kotlin has a different structure for its compilation unit. See K.CompilationUnit.");
    }

    public <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, KRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, KLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public Space visitSpace(Space space, KSpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }
}
