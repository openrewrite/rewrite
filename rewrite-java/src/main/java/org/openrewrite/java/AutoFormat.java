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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.refactor.Formatter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;
import static org.openrewrite.internal.StringUtils.splitCStyleComments;

/**
 * A general purpose means of formatting arbitrarily complex blocks of code relative based on their
 * surrounding context.
 * <p>
 * TODO when complete, this should replace {@link ShiftFormatRightVisitor}.
 */
@Incubating(since = "2.1.0")
public class AutoFormat extends JavaIsoRefactorVisitor {
    private final J[] scope;

    public AutoFormat(J... scope) {
        this.scope = scope;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {
        andThen(new FixNewlines());
        andThen(new FixIndentation());
        return super.visitCompilationUnit(cu);
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    private class FixNewlines extends JavaIsoRefactorVisitor {

        FixNewlines() {
            setCursoringOn();
        }

        /**
         * Given a single line string with a whitespace prefix and a non-whitespace suffix, alter the whitespace prefix
         * to have the appropriate indentation.
         *
         * Right now comments aren't AST elements so FixIndentation.visitTree() is blind to them.
         * This special treatment of comment indentation will be able to go away once we start treating
         * comments as their own, independent AST element.
         */
        public String indentLine(String prefix) {
            if (!prefix.isEmpty() && stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                int indentMultiple = (int) getCursor().getPathAsStream().filter(J.Block.class::isInstance).count();
                Formatter.Result wholeSourceIndent = formatter.wholeSourceIndent();
                int nonWhiteSpaceIndex = StringUtils.indexOfNonWhitespace(prefix);
                boolean insideJavaDocComment = false;

                if(nonWhiteSpaceIndex == -1) {
                    nonWhiteSpaceIndex = prefix.length();
                } else if(prefix.charAt(nonWhiteSpaceIndex) == '*') {
                    insideJavaDocComment = true;
                }

                String indentation = prefix.substring(0, nonWhiteSpaceIndex);
                String comment = prefix.substring(nonWhiteSpaceIndex);

                // +1 because for String.substring(start, end) start is inclusive, but end is exclusive
                String newIndentation = indentation.substring(0, indentation.lastIndexOf('\n') + 1) +
                        range(0, indentMultiple * wholeSourceIndent.getIndentToUse())
                            .mapToObj(n -> wholeSourceIndent.isIndentedWithSpaces() ? " " : "\t")
                            .collect(Collectors.joining(""));

                if(insideJavaDocComment) //noinspection DanglingJavadoc
                {
                    // By convention Javadoc-style comments are indented such that lines beginning with '*'
                    // have an extra space so as to vertically align the '*' characters from different lines. e.g.:
                    /**
                     *
                     */
                    newIndentation += " ";
                }
                return newIndentation + comment;
            }
            return prefix;
        }

        /**
         * Ensure that the element in question is indented the right amount
         */
        private <T extends J> T spaceHorizontally(T j) {
            if(stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                Formatting originalFormatting = j.getFormatting();

                // Ensure that comments are indented correctly
                String newPrefix = stream(originalFormatting.getPrefix().split("\\n"))
                        .map(this::indentLine)
                        .collect(Collectors.joining("\n"));
                if(originalFormatting.getPrefix().endsWith("\n")) {
                    // split() will eliminate a trailing newline, put it back
                    newPrefix += '\n';
                }
                j = j.withFormatting(originalFormatting.withPrefix(newPrefix));
            }
            return j;
        }

        /**
         * Ensure there's a single blank line between the previous declaration and the current j
         * Ensure that any comments are on their own line
         */
        private <T extends J> T spaceVertically(T j) {
            Formatting originalFormatting = j.getFormatting();
            List<String> splitPrefix = splitCStyleComments(originalFormatting.getPrefix());

            String newPrefix = Stream.concat(
                    Stream.of(splitPrefix.get(0))
                            .map(it -> StringUtils.ensureNewlineCountBeforeComment(it, 2)),
                    splitPrefix.stream().skip(1)
                            .map(it -> StringUtils.ensureNewlineCountBeforeComment(it, 1))
            )
                    .collect(Collectors.joining());
            return j.withFormatting(originalFormatting.withPrefix(newPrefix));
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl methodDecl) {
            J.MethodDecl m = super.visitMethod(methodDecl);

            if(stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                m = spaceHorizontally(m);
                m = spaceVertically(m);

                // Annotations should each appear on their own line
                List<J.Annotation> annotations = new ArrayList<>(m.getAnnotations());
                if(!annotations.isEmpty()) {

                    // ensure all annotations except the first have a \n in their prefixes
                    // The first annotation doesn't need a \n since the prefix of the MethodDecl itself should contain that \n
                    for(int i = 1; i < annotations.size(); i++) {
                        if(!annotations.get(i).getPrefix().contains("\n")) {
                            annotations.set(i, annotations.get(i).withPrefix("\n"));
                        }
                    }

                    m = m.withAnnotations(annotations);

                    List<J.Modifier> modifiers = new ArrayList<>(m.getModifiers());

                    // ensure first modifier following annotations has \n in prefix
                    if(!modifiers.isEmpty()) {
                        if(!modifiers.get(0).getPrefix().contains("\n")) {
                            modifiers.set(0, modifiers.get(0).withPrefix("\n"));
                            m = m.withModifiers(modifiers);
                        }
                    }
                    // typeParameters
                    else if(m.getTypeParameters() != null) {
                        if(!m.getTypeParameters().getPrefix().contains("\n")) {
                            m = m.withTypeParameters(m.getTypeParameters().withPrefix("\n"));
                        }
                    }
                    // returnTypeExpr
                    else if(m.getReturnTypeExpr() != null) {
                        if(!m.getReturnTypeExpr().getPrefix().contains("\n")) {
                            m = m.withReturnTypeExpr(m.getReturnTypeExpr().withPrefix("\n"));
                        }
                    }
                    // name
                    else if(!m.getName().getPrefix().contains("\n")) {
                        m = m.withName(m.getName().withPrefix("\n"));
                    }

                }
            }

            return m;
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = super.visitClassDecl(classDecl);

            if(stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                cd = spaceHorizontally(cd);
                cd = spaceVertically(cd);

                // check annotations formatting
                List<J.Annotation> annotations = new ArrayList<>(cd.getAnnotations());
                if (!annotations.isEmpty()) {

                    if(annotations.get(0).getFormatting().getPrefix().contains("\n")) {
                        annotations.set(0, annotations.get(0).withPrefix(""));
                    }

                    // Ensure all annotations have a \n in their prefixes
                    // The first annotation is skipped because the whitespace prior to it is stored in the formatting for ClassDecl
                    for (int i = 1; i < annotations.size(); i++) {
                        if (!annotations.get(i).getPrefix().contains("\n")) {
                            annotations.set(i, annotations.get(i).withPrefix("\n"));
                        }
                    }

                    cd = cd.withAnnotations(annotations);

                    // ensure first statement following annotations has \n in prefix
                    List<J.Modifier> modifiers = new ArrayList<>(cd.getModifiers());
                    if (!modifiers.isEmpty()) {
                        if (!modifiers.get(0).getPrefix().contains("\n")) {
                            modifiers.set(0, modifiers.get(0).withPrefix("\n"));
                            cd = cd.withModifiers(modifiers);
                        }
                    } else if (!cd.getKind().getPrefix().contains("\n")) {
                        cd = cd.withKind(cd.getKind().withPrefix("\n"));
                    }
                }
            }
            return cd;
        }
    }

    private class FixIndentation extends JavaIsoRefactorVisitor {
        FixIndentation() {
            setCursoringOn();
        }

        @Override
        public J reduce(J r1, J r2) {
            J j = super.reduce(r1, r2);
            if (r2 != null && r2.getPrefix().startsWith("|")) {
                j = j.withPrefix(r2.getPrefix().substring(1));
            }
            return j;
        }

        @Override
        public J visitTree(Tree tree) {
            J j = super.visitTree(tree);

            String prefix = tree.getPrefix();
            if (prefix.contains("\n") && stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                int indentMultiple = (int) getCursor().getPathAsStream().filter(J.Block.class::isInstance).count();
                if(tree instanceof J.Block.End) {
                    indentMultiple--;
                }
                Formatter.Result wholeSourceIndent = formatter.wholeSourceIndent();
                String shiftedPrefix = "|" + prefix.substring(0, prefix.lastIndexOf('\n') + 1) + range(0, indentMultiple * wholeSourceIndent.getIndentToUse())
                        .mapToObj(n -> wholeSourceIndent.isIndentedWithSpaces() ? " " : "\t")
                        .collect(Collectors.joining(""));

                if (!shiftedPrefix.equals(prefix)) {
                    j = j.withPrefix(shiftedPrefix);
                }
            }

            return j;
        }
    }

}
