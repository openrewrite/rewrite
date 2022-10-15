package org.openrewrite.kotlin;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.tree.K;

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
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("Kotlin has a different structure for its compilation unit. See K.CompilationUnit.");
    }
}
