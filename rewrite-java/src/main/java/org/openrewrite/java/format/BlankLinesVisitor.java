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

import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.BlankLinesStyle;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class BlankLinesVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final BlankLinesStyle style;

    public BlankLinesVisitor(BlankLinesStyle style) {
        this(style, null);
    }

    public BlankLinesVisitor(BlankLinesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            if (cu.getPackageDeclaration() != null) {
                if (!cu.getPrefix().getComments().isEmpty()) {
                    cu = cu.withComments(ListUtils.mapLast(cu.getComments(), c -> {
                        String suffix = keepMaximumLines(c.getSuffix(), style.getKeepMaximum().getBetweenHeaderAndPackage());
                        suffix = minimumLines(suffix, style.getMinimum().getBeforePackage());
                        return c.withSuffix(suffix);
                    }));
                } else {
                /*
                 if comments are empty and package is present, leading whitespace is on the compilation unit and
                 should be removed
                 */
                    cu = cu.withPrefix(Space.EMPTY);
                }
            }

            if (cu.getPackageDeclaration() == null) {
                if (cu.getComments().isEmpty()) {
                /*
                if package decl and comments are null/empty, leading whitespace is on the
                compilation unit and should be removed
                 */
                    cu = cu.withPrefix(Space.EMPTY);
                } else {
                    cu = cu.withComments(ListUtils.mapLast(cu.getComments(), c ->
                            c.withSuffix(minimumLines(c.getSuffix(), style.getMinimum().getBeforeImports()))));
                }
            } else {
                cu = cu.getPadding().withImports(ListUtils.mapFirst(cu.getPadding().getImports(), i ->
                        minimumLines(i, style.getMinimum().getAfterPackage())));
            }
            return super.visit(cu, p);
        }
        return super.visit(tree, p);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration j = super.visitClassDeclaration(classDecl, p);
        if (j.getBody() != null) {
            List<JRightPadded<Statement>> statements = j.getBody().getPadding().getStatements();
            J.ClassDeclaration.Kind.Type classKind = j.getKind();
            j = j.withBody(j.getBody().getPadding().withStatements(ListUtils.map(statements, (i, s) -> {
                if (i == 0) {
                    if (classKind != J.ClassDeclaration.Kind.Type.Enum) {
                        s = minimumLines(s, style.getMinimum().getAfterClassHeader());
                    }
                } else if (statements.get(i - 1).getElement() instanceof J.Block) {
                    s = minimumLines(s, style.getMinimum().getAroundInitializer());
                } else if (s.getElement() instanceof J.ClassDeclaration) {
                    // Apply `style.getMinimum().getAroundClass()` to inner classes.
                    s = minimumLines(s, style.getMinimum().getAroundClass());
                }

                return s;
            })));

            j = j.withBody(j.getBody().withEnd(minimumLines(j.getBody().getEnd(),
                    style.getMinimum().getBeforeClassEnd())));
        }

        JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
        boolean hasImports = !cu.getImports().isEmpty();
        boolean firstClass = j.equals(cu.getClasses().get(0));
        Set<J.ClassDeclaration> classes = new HashSet<>(cu.getClasses());

        j = firstClass ?
                (hasImports ? minimumLines(j, style.getMinimum().getAfterImports()) : j) :
                // Apply `style.getMinimum().getAroundClass()` to classes declared in the SourceFile.
                (classes.contains(j)) ? minimumLines(j, style.getMinimum().getAroundClass()) : j;

        // style.getKeepMaximum().getInDeclarations() also sets the maximum new lines of class declaration prefixes.
        j = firstClass ?
                (hasImports ? keepMaximumLines(j, Math.max(style.getKeepMaximum().getInDeclarations(), style.getMinimum().getAfterImports())) : j) :
                (classes.contains(j)) ? keepMaximumLines(j, style.getKeepMaximum().getInDeclarations()) : j;

        if (!hasImports && firstClass) {
            if (cu.getPackageDeclaration() == null) {
                if (!j.getPrefix().getWhitespace().isEmpty()) {
                    j = j.withPrefix(j.getPrefix().withWhitespace(""));
                }
            } else {
                j = minimumLines(j, style.getMinimum().getAfterPackage());
            }
        }

        return j;
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue e = super.visitEnumValue(_enum, p);
        return keepMaximumLines(e, style.getKeepMaximum().getInDeclarations());
    }

    @Override
    public J.Import visitImport(J.Import import_, P p) {
        J.Import i = super.visitImport(import_, p);
        JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
        if (i.equals(cu.getImports().get(0)) && cu.getPackageDeclaration() == null && cu.getPrefix().equals(Space.EMPTY)) {
            i = i.withPrefix(i.getPrefix().withWhitespace(""));
        }
        return i;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration j = super.visitMethodDeclaration(method, p);
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
        Iterator<Object> cursorPath = getCursor().getParentOrThrow().getPath(J.class::isInstance);
        Object parentTree = cursorPath.next();
        if (cursorPath.hasNext()) {
            Object grandparentTree = cursorPath.next();
            if (grandparentTree instanceof J.ClassDeclaration && parentTree instanceof J.Block) {
                J.Block block = (J.Block) parentTree;
                J.ClassDeclaration classDecl = (J.ClassDeclaration) grandparentTree;

                int declMax = style.getKeepMaximum().getInDeclarations();

                // don't adjust the first statement in a block
                if (!block.getStatements().isEmpty() && !block.getStatements().iterator().next().isScope(j)) {
                    if (j instanceof J.VariableDeclarations) {
                        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                            declMax = Math.max(declMax, style.getMinimum().getAroundFieldInInterface());
                            j = minimumLines(j, style.getMinimum().getAroundFieldInInterface());
                        } else {
                            declMax = Math.max(declMax, style.getMinimum().getAroundField());
                            j = minimumLines(j, style.getMinimum().getAroundField());
                        }
                    } else if (j instanceof J.MethodDeclaration) {
                        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                            declMax = Math.max(declMax, style.getMinimum().getAroundMethodInInterface());
                            j = minimumLines(j, style.getMinimum().getAroundMethodInInterface());
                        } else {
                            declMax = Math.max(declMax, style.getMinimum().getAroundMethod());
                            j = minimumLines(j, style.getMinimum().getAroundMethod());
                        }
                    } else if (j instanceof J.Block) {
                        declMax = Math.max(declMax, style.getMinimum().getAroundInitializer());
                        j = minimumLines(j, style.getMinimum().getAroundInitializer());
                    } else if (j instanceof J.ClassDeclaration) {
                        declMax = Math.max(declMax, style.getMinimum().getAroundClass());
                        j = minimumLines(j, style.getMinimum().getAroundClass());
                    }
                }

                j = keepMaximumLines(j, declMax);
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
        long blankLines = getNewLineCount(whitespace) - 1;
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
        return tree.withElement(minimumLines(tree.getElement(), min));
    }

    private <J2 extends J> J2 minimumLines(J2 tree, int min) {
        return tree.withPrefix(minimumLines(tree.getPrefix(), min));
    }

    private Space minimumLines(Space prefix, int min) {
        if (min == 0) {
            return prefix;
        }
        if (prefix.getComments().isEmpty() ||
            prefix.getWhitespace().contains("\n") ||
            prefix.getComments().get(0) instanceof Javadoc ||
            (prefix.getComments().get(0).isMultiline() && prefix.getComments().get(0)
                    .printComment(getCursor()).contains("\n"))) {
            return prefix.withWhitespace(minimumLines(prefix.getWhitespace(), min));
        }

        // the first comment is a trailing comment on the previous line
        return prefix.withComments(ListUtils.map(prefix.getComments(), (i, c) -> i == 0 ?
                c.withSuffix(minimumLines(c.getSuffix(), min)) : c));
    }

    public static String minimumLines(String whitespace, int min) {
        if (min == 0) {
            return whitespace;
        }
        String minWhitespace = whitespace;

        for (int i = 0; i < min - getNewLineCount(whitespace) + 1; i++) {
            //noinspection StringConcatenationInLoop
            minWhitespace = "\n" + minWhitespace;
        }
        return minWhitespace;
    }

    private static int getNewLineCount(String whitespace) {
        int newLineCount = 0;
        for (int i = 0; i < whitespace.length(); i++) {
            char c = whitespace.charAt(i);
            if (c == '\n') {
                newLineCount++;
            }
        }
        return newLineCount;
    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }
}
