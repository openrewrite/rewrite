/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.tree.ParseError;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before) {
        return docker(before, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        );
        spec.accept(dockerfile);
        return dockerfile;
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after) {
        return docker(before, after, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        ).after(s -> after);
        spec.accept(dockerfile);
        return dockerfile;
    }

    private static SourceFile validate(SourceFile sf, TypeValidation tv) {
        if (!tv.allowNonWhitespaceInWhitespace()) {
            List<Docker> elementsWithNonBlankWhitespace = new DockerIsoVisitor<List<Docker>>() {
                @Override
                public Space visitSpace(Space space, List<Docker> elements) {
                    // Strip line continuation characters (\ or ` followed by optional whitespace and newline)
                    // before checking, since they are valid in Dockerfile whitespace
                    String ws = space.getWhitespace().replaceAll("[\\\\`][ \\t]*(?=\\r?\\n)", "");
                    if (!ws.trim().isEmpty()) {
                        elements.add(getCursor().firstEnclosingOrThrow(Docker.class));
                    }
                    return super.visitSpace(space, elements);
                }
            }.reduce(sf, new ArrayList<>());
            if (!elementsWithNonBlankWhitespace.isEmpty()) {
                throw new AssertionError("Expected no non-whitespace in whitespace, but found: " + elementsWithNonBlankWhitespace);
            }
        }
        if (tv.parseAndPrintEquality()) {
            assertReparsesToTheSameTree(sf);
        }
        return sf;
    }

    /// A tree prints losslessly by construction, so printing alone cannot tell whether its elements
    /// mean what they say. Reading the printed source back and comparing what the two trees model
    /// catches a tree that prints correctly but holds the wrong thing: an image reference whose tag
    /// sits in its name, a value whose variable reference is only text, an exec form whose arguments
    /// have lost their quotes.
    private static void assertReparsesToTheSameTree(SourceFile sf) {
        String printed = sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
        List<SourceFile> reparsed = DockerParser.builder().build()
                .parse(new InMemoryExecutionContext(), printed)
                .collect(toList());
        if (reparsed.size() != 1 || reparsed.get(0) instanceof ParseError) {
            throw new AssertionError("Expected printing the tree to yield a parseable Dockerfile, but got " +
                    (reparsed.size() == 1 ? "a parse error" : reparsed.size() + " source files") + ":\n" + printed);
        }
        String printedSemantics = semantics(reparsed.get(0));
        String treeSemantics = semantics(sf);
        if (!printedSemantics.equals(treeSemantics)) {
            throw new AssertionError("Expected the tree to model what it prints, but\n" + printed +
                    "\nmodels " + printedSemantics + "\nwhile the tree models " + treeSemantics);
        }
    }

    /// Everything a tree models apart from its formatting, so that comparing two trees compares what
    /// they mean rather than what they print.
    private static String semantics(Tree tree) {
        return new DockerIsoVisitor<StringBuilder>() {
            @Override
            public @Nullable Docker visit(@Nullable Tree t, StringBuilder out) {
                if (!(t instanceof Docker)) {
                    return super.visit(t, out);
                }
                Docker d = (Docker) t;
                out.append('(').append(d.getClass().getSimpleName());
                if (d instanceof Docker.Literal) {
                    Docker.Literal l = (Docker.Literal) d;
                    out.append(' ').append(l.getQuoteStyle()).append(' ').append(l.getText());
                } else if (d instanceof Docker.EnvironmentVariable) {
                    Docker.EnvironmentVariable e = (Docker.EnvironmentVariable) d;
                    out.append(' ').append(e.getName()).append(' ').append(e.isBraced());
                } else if (d instanceof Docker.Flag) {
                    out.append(' ').append(((Docker.Flag) d).getName());
                } else if (d instanceof Docker.Port) {
                    Docker.Port p = (Docker.Port) d;
                    out.append(' ').append(p.getText()).append(' ').append(p.getStart())
                            .append('-').append(p.getEnd()).append(' ').append(p.getProtocol());
                } else if (d instanceof Docker.Volume) {
                    out.append(' ').append(((Docker.Volume) d).isJsonForm());
                } else if (d instanceof Docker.Healthcheck) {
                    out.append(' ').append(((Docker.Healthcheck) d).isNone());
                } else if (d instanceof Docker.HeredocForm) {
                    out.append(' ').append(((Docker.HeredocForm) d).getPreamble());
                } else if (d instanceof Docker.HeredocBody) {
                    Docker.HeredocBody b = (Docker.HeredocBody) d;
                    out.append(' ').append(b.getOpening()).append(' ').append(b.getClosing())
                            .append(' ').append(b.getContentLines());
                }
                Docker visited = super.visit(t, out);
                out.append(')');
                return visited;
            }

            @Override
            public Docker.From visitFrom(Docker.From from, StringBuilder out) {
                if (from.getFlags() != null) {
                    from.getFlags().forEach(flag -> visit(flag, out));
                }
                out.append(" name");
                visit(from.getImageName(), out);
                if (from.getTag() != null) {
                    out.append(" tag");
                    visit(from.getTag(), out);
                }
                if (from.getDigest() != null) {
                    out.append(" digest");
                    visit(from.getDigest(), out);
                }
                if (from.getAs() != null) {
                    visitFromAs(from.getAs(), out);
                }
                return from;
            }

            @Override
            public Docker.From.As visitFromAs(Docker.From.As as, StringBuilder out) {
                out.append("(As ").append(as.getKeyword());
                visit(as.getName(), out);
                out.append(')');
                return as;
            }

            @Override
            public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, StringBuilder out) {
                out.append("(EnvPair ").append(pair.isHasEquals());
                Docker.Env.EnvPair p = super.visitEnvPair(pair, out);
                out.append(')');
                return p;
            }

            @Override
            public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, StringBuilder out) {
                out.append("(LabelPair ").append(pair.isHasEquals());
                Docker.Label.LabelPair p = super.visitLabelPair(pair, out);
                out.append(')');
                return p;
            }

            @Override
            public Docker.CopyShellForm visitCopyShellForm(Docker.CopyShellForm form, StringBuilder out) {
                out.append(" sources");
                form.getSources().forEach(source -> visit(source, out));
                out.append(" destination");
                visit(form.getDestination(), out);
                return form;
            }
        }.reduce(tree, new StringBuilder()).toString();
    }
}
