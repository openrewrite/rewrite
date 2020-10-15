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

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.Formatting;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.style.TabAndIndentStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.refactor.Formatter;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
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

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl cd = super.visitClassDecl(classDecl);

            if(stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                // check annotations formatting
                List<J.Annotation> annotations = new ArrayList<>(cd.getAnnotations());
                if (!annotations.isEmpty()) {

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

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl methodDecl) {
            J.MethodDecl m = super.visitMethod(methodDecl);
            if(stream(scope).anyMatch(s -> getCursor().isScopeInPath(s))) {
                // Format comments
                Formatting originalFormatting = m.getFormatting();
                List<String> splitPrefix = splitCStyleComments(originalFormatting.getPrefix());

                // Ensure that there is exactly one blank line separating a methodDecl from whatever proceeds it
                String newPrefix = Stream.concat(
                        Stream.of(splitPrefix.get(0))
                                .map(it -> StringUtils.ensureNewlineCountBeforeComment(it, 2)),
                        splitPrefix.stream().skip(1)
                ).collect(Collectors.joining());

                m = m.withFormatting(originalFormatting.withPrefix(newPrefix));
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
                    // returnTypeExpr
                    else if(m.getReturnTypeExpr() != null && !m.getReturnTypeExpr().getPrefix().contains("\n")) {
                        m = m.withReturnTypeExpr(m.getReturnTypeExpr().withPrefix("\n"));
                    }
                    // name
                    else if(!m.getName().getPrefix().contains("\n")) {
                        m = m.withName(m.getName().withPrefix("\n"));
                    }

                }
            }

            return m;
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
