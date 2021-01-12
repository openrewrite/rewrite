package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.BlankLineStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;

class BlankLinesProcessor<P> extends JavaIsoProcessor<P> {
    private final BlankLineStyle style;

    public BlankLinesProcessor(BlankLineStyle style) {
        this.style = style;
        setCursoringOn();
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit j = super.visitCompilationUnit(cu, p);

        if (j.getPackageDecl() != null) {
            if (!j.getPrefix().getComments().isEmpty()) {
                j = j.withComments(ListUtils.mapLast(j.getComments(), c -> {
                    String suffix = keepMaximumLines(c.getSuffix(), style.getKeepMaximum().getBetweenHeaderAndPackage());
                    suffix = minimumLines(suffix, style.getMinimum().getBeforePackage());
                    return c.withSuffix(suffix);
                }));
            } else {
                j = minimumLines(j, style.getMinimum().getBeforePackage());
            }
        }

        if (j.getPackageDecl() == null) {
            if (j.getComments().isEmpty()) {
                j = j.withImports(ListUtils.mapFirst(cu.getImports(), i -> {
                    J.Import anImport = i.getElem();
                    return i.withElem(anImport.withPrefix(anImport.getPrefix().withWhitespace("")));
                }));
            } else {
                j = j.withComments(ListUtils.mapLast(j.getComments(), c ->
                        c.withSuffix(minimumLines(c.getSuffix(), style.getMinimum().getBeforeImports()))));
            }
        } else {
            j = j.withImports(ListUtils.mapFirst(j.getImports(), i ->
                    minimumLines(i, Math.max(
                            style.getMinimum().getBeforeImports(),
                            style.getMinimum().getAfterPackage()))));

            if (j.getImports().isEmpty()) {
                j = j.withClasses(ListUtils.mapFirst(j.getClasses(), c ->
                        minimumLines(c, style.getMinimum().getAfterPackage())));
            }
        }

        j = j.withClasses(ListUtils.map(j.getClasses(), (i, c) -> i == 0 ?
                minimumLines(c, style.getMinimum().getAfterImports()) :
                minimumLines(c, style.getMinimum().getAroundClass())
        ));

        return j;
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl j = super.visitClassDecl(classDecl, p);

        List<JRightPadded<Statement>> statements = j.getBody().getStatements();
        j = j.withBody(j.getBody().withStatements(ListUtils.map(statements, (i, s) -> {
            s = keepMaximumLines(s, style.getKeepMaximum().getInDeclarations());
            if (i == 0) {
                s = minimumLines(s, style.getMinimum().getAfterClassHeader());
            } else if (s.getElem() instanceof J.VariableDecls) {
                if (classDecl.getKind().getElem() == J.ClassDecl.Kind.Interface) {
                    s = minimumLines(s, style.getMinimum().getAroundFieldInInterface());
                } else {
                    s = minimumLines(s, style.getMinimum().getAroundField());
                }
            } else if (s.getElem() instanceof J.MethodDecl) {
                if (classDecl.getKind().getElem() == J.ClassDecl.Kind.Interface) {
                    s = minimumLines(s, style.getMinimum().getAroundMethodInInterface());
                } else {
                    s = minimumLines(s, style.getMinimum().getAroundMethod());
                }
            } else if (s.getElem() instanceof J.Block) {
                s = minimumLines(s, style.getMinimum().getAroundInitializer());
            }

            if (i > 0 && statements.get(i - 1).getElem() instanceof J.Block) {
                s = minimumLines(s, style.getMinimum().getAroundInitializer());
            }

            return s;
        })));

        j = j.withBody(j.getBody().withEnd(minimumLines(j.getBody().getEnd(),
                style.getMinimum().getBeforeClassEnd())));

        return j;
    }

    @Override
    public J.MethodDecl visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl j = super.visitMethod(method, p);

        if (j.getBody() != null) {
            if (j.getBody().getStatements().isEmpty()) {
                Space end = minimumLines(j.getBody().getEnd(),
                        style.getMinimum().getBeforeMethodBody());
                if (end.getIndent().isEmpty() && style.getMinimum().getBeforeMethodBody() > 0) {
                    end = end.withWhitespace(end.getWhitespace() + method.getPrefix().getIndent());
                }
                j = j.withBody(j.getBody().withEnd(end));
            } else {
                j = j.withBody(j.getBody().withStatements(ListUtils.mapFirst(j.getBody().getStatements(), s ->
                        minimumLines(s, style.getMinimum().getBeforeMethodBody()))));
            }
        }

        return j;
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        J.NewClass j = super.visitNewClass(newClass, p);
        if (j.getBody() != null) {
            j = j.withBody(j.getBody().withStatements(ListUtils.mapFirst(j.getBody().getStatements(), s ->
                    minimumLines(s, style.getMinimum().getAfterAnonymousClassHeader()))));
        }
        return j;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        Cursor parent = getCursor().getParentOrThrow();
        if (parent.getParent() != null && !(parent.getParentOrThrow().getTree() instanceof J.ClassDecl)) {
            return keepMaximumLines(j, style.getKeepMaximum().getInCode());
        }
        return j;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block j = super.visitBlock(block, p);
        j = j.withEnd(keepMaximumLines(j.getEnd(), style.getKeepMaximum().getBeforeEndOfBlock()));
        return j;
    }

    private <J2 extends J> JRightPadded<J2> keepMaximumLines(JRightPadded<J2> tree, int max) {
        return tree.withElem(keepMaximumLines(tree.getElem(), max));
    }

    private <J2 extends J> J2 keepMaximumLines(J2 tree, int max) {
        return tree.withPrefix(keepMaximumLines(tree.getPrefix(), max));
    }

    private Space keepMaximumLines(Space prefix, int max) {
        return prefix.withWhitespace(keepMaximumLines(prefix.getWhitespace(), max));
    }

    private String keepMaximumLines(String whitespace, int max) {
        long blankLines = whitespace.chars().filter(c -> c == '\n').count() - 1;
        if (blankLines > max) {
            int startWhitespaceAtIndex = 0;
            for (int i = 0; i < blankLines - max + 1; i++, startWhitespaceAtIndex++) {
                startWhitespaceAtIndex = whitespace.indexOf('\n', startWhitespaceAtIndex);
            }
            startWhitespaceAtIndex--;
            return whitespace.substring(startWhitespaceAtIndex);
        }
        return whitespace;
    }

    private <J2 extends J> JRightPadded<J2> minimumLines(JRightPadded<J2> tree, int min) {
        return tree.withElem(minimumLines(tree.getElem(), min));
    }

    private <J2 extends J> J2 minimumLines(J2 tree, int min) {
        return tree.withPrefix(minimumLines(tree.getPrefix(), min));
    }

    private Space minimumLines(Space prefix, int min) {
        return prefix.withWhitespace(minimumLines(prefix.getWhitespace(), min));
    }

    private String minimumLines(String whitespace, int min) {
        if (min == 0) {
            return whitespace;
        }
        String minWhitespace = whitespace;
        for (int i = 0; i < min - whitespace.chars().filter(c -> c == '\n').count() + 1; i++) {
            //noinspection StringConcatenationInLoop
            minWhitespace = "\n" + minWhitespace;
        }
        return minWhitespace;
    }
}
