package org.openrewrite.java.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

public class FormatFirstClassPrefix extends JavaIsoVisitor<ExecutionContext> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    public FormatFirstClassPrefix() {
        setCursoringOn();
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
        J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
        if (c.equals(cu.getClasses().get(0))) {
            J.ClassDeclaration temp = autoFormat(c.withBody(EMPTY_BLOCK), ctx);
            c = c.withPrefix(temp.getPrefix());
        }
        return c;
    }
}
