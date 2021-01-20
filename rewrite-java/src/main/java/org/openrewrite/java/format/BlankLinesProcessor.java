/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.List;

class BlankLinesProcessor<P> extends JavaIsoProcessor<P> {
    private final BlankLinesStyle style;

    public BlankLinesProcessor(BlankLinesStyle style) {
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
                /*
                 if comments are empty and package is present, leading whitespace is on the compilation unit and
                 should be removed
                 */
                j = j.withPrefix(Space.EMPTY);
            }
        }

        if (j.getPackageDecl() == null) {
            if (j.getComments().isEmpty()) {
                /*
                if package decl and comments are null/empty, leading whitespace is on the
                compilation unit and should be removed
                 */
                j = j.withPrefix(Space.EMPTY);
            } else {
                j = j.withComments(ListUtils.mapLast(j.getComments(), c ->
                        c.withSuffix(minimumLines(c.getSuffix(), style.getMinimum().getBeforeImports()))));
            }
        } else {
            j = j.withImports(ListUtils.mapFirst(j.getImports(), i ->
                    minimumLines(i, style.getMinimum().getAfterPackage())));

            if (j.getImports().isEmpty()) {
                j = j.withClasses(ListUtils.mapFirst(j.getClasses(), c ->
                        minimumLines(c, style.getMinimum().getAfterPackage())));
            }
        }
        boolean hasImports = !j.getImports().isEmpty();
        j = j.withClasses(ListUtils.map(j.getClasses(), (i, c) -> i == 0 ?
                (hasImports ? minimumLines(c, style.getMinimum().getAfterImports()) : c) :
                minimumLines(c, style.getMinimum().getAroundClass())
        ));

        return j;
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl j = super.visitClassDecl(classDecl, p);
        List<JRightPadded<Statement>> statements = j.getBody().getStatements();
        j = j.withBody(j.getBody().withStatements(ListUtils.map(statements, (i, s) -> {
            if (i == 0) {
                s = minimumLines(s, style.getMinimum().getAfterClassHeader());
            } else if (statements.get(i - 1).getElem() instanceof J.Block) {
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
        if (parent.getParent() != null) {
            Tree parentTree = parent.getTree();
            Tree grandparentTree = parent.getParentOrThrow().getTree();
            if (grandparentTree instanceof J.ClassDecl && parentTree instanceof J.Block) {
                J.Block block = (J.Block) parentTree;
                J.ClassDecl classDecl = (J.ClassDecl) grandparentTree;

                j = keepMaximumLines(j, style.getKeepMaximum().getInDeclarations());

                // don't adjust the first statement in a block
                if (block.getStatements().iterator().next().getElem() != j) {
                    if (j instanceof J.VariableDecls) {
                        if (classDecl.getKind().getElem() == J.ClassDecl.Kind.Interface) {
                            j = minimumLines(j, style.getMinimum().getAroundFieldInInterface());
                        } else {
                            j = minimumLines(j, style.getMinimum().getAroundField());
                        }
                    } else if (j instanceof J.MethodDecl) {
                        if (classDecl.getKind().getElem() == J.ClassDecl.Kind.Interface) {
                            j = minimumLines(j, style.getMinimum().getAroundMethodInInterface());
                        } else {
                            j = minimumLines(j, style.getMinimum().getAroundMethod());
                        }
                    } else if (j instanceof J.Block) {
                        j = minimumLines(j, style.getMinimum().getAroundInitializer());
                    }
                }
            } else {
                return keepMaximumLines(j, style.getKeepMaximum().getInCode());
            }
        }
        return j;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block j = super.visitBlock(block, p);
        j = j.withEnd(keepMaximumLines(j.getEnd(), style.getKeepMaximum().getBeforeEndOfBlock()));
        return j;
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
