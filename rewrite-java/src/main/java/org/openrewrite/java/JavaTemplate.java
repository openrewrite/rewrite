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

import org.openrewrite.Cursor;
import org.openrewrite.EvalContext;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.java.internal.PrintJava;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class JavaTemplate {
    private final JavaParser parser;
    private final String code;
    private final String extraImports;
    private final boolean autoFormat;

    private final PrintJava<String> printer = new PrintJava<String>(new TreePrinter<J, String>() {
        @Override
        public String doLast(Tree tree, String printed, String acc) {
            if (tree instanceof Statement) {
                return acc + printed;
            }
            return printed;
        }
    }) {
        @Override
        public String visitBlock(J.Block block, String context) {
            StringBuilder acc = new StringBuilder();

            if (block.getStatic() != null) {
                acc.append("static").append(visit(block.getStatic()));
            }

            acc.append('{').append(context).append(visit(block.getEnd())).append('}');

            return fmt(block, acc.toString());
        }

        @Override
        public String visitCompilationUnit(J.CompilationUnit cu, String acc) {
            return super.visit(cu.getImports(), ";", acc) + acc;
        }
    };

    public JavaTemplate(JavaParser parser, String code, String extraImports, boolean autoFormat) {
        this.parser = parser;
        this.code = code;
        this.extraImports = extraImports;
        this.autoFormat = autoFormat;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public <J2 extends J> List<J2> generate(Cursor insertionScope, Object... params) {
        // flat map params to list of method declaration and variable, i.e. JavaType.Method and JavaType

        // TODO substitute params in code
        String printedInsertionScope = code;
        for (Cursor scope = insertionScope; scope != null; scope = scope.getParent()) {
            printedInsertionScope = printer.visit((J) scope.getTree(), printedInsertionScope);
        }

        parser.reset();
        J.CompilationUnit cu = parser
                .parse(printedInsertionScope)
                .iterator().next();

        if (autoFormat) {
            // TODO format the new tree
        }

        ExtractTemplatedCode extractTemplatedCode = new ExtractTemplatedCode();
        extractTemplatedCode.visit(cu, null);

        //noinspection unchecked
        return (List<J2>) extractTemplatedCode.templated;
    }

    private static class ExtractTemplatedCode extends JavaEvalVisitor {
        private long templateDepth = -1;
        private final List<J> templated = new ArrayList<>();

        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public J visitEach(J tree, EvalContext ctx) {
            Comment startToken = findMarker(tree, "<<<<START>>>>");
            if (startToken != null) {
                templateDepth = getCursor().getPathAsStream().count();

                List<Comment> comments = new ArrayList<>(tree.getPrefix().getComments());
                comments.remove(startToken);

                templated.add(tree.withPrefix(tree.getPrefix().withComments(comments)));
            } else if (!templated.isEmpty() && getCursor().getPathAsStream().count() == templateDepth) {
                templated.add(tree);
            } else if (findMarker(tree, "<<<<STOP>>>>") != null) {
                return tree;
            }

            return super.visitEach(tree, ctx);
        }

        private Comment findMarker(J tree, String marker) {
            return tree.getPrefix().getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(marker))
                    .findAny()
                    .orElse(null);
        }
    }

    public static class Builder {
        private JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();
        private final String code;
        private String extraImports = "";
        private boolean autoFormat = true;

        Builder(String code) {
            this.code = code;
        }

        public Builder extraImports(String... imports) {
            StringBuilder extra = new StringBuilder();
            for (String anImport : imports) {
                extra.append("import ").append(anImport).append(";");
            }
            this.extraImports += extra;
            return this;
        }

        public Builder extraImports(JavaType... types) {
            StringBuilder extra = new StringBuilder();
            for (JavaType type : types) {
                if (type instanceof JavaType.FullyQualified) {
                    extra.append("import ")
                            .append(((JavaType.FullyQualified) type).getFullyQualifiedName())
                            .append(";");
                }
            }
            this.extraImports += extra;
            return this;
        }

        public Builder javaParser(JavaParser javaParser) {
            this.javaParser = javaParser;
            return this;
        }

        public Builder autoFormat(boolean autoFormat) {
            this.autoFormat = autoFormat;
            return this;
        }

        public JavaTemplate build() {
            return new JavaTemplate(javaParser, code, extraImports, autoFormat);
        }
    }
}
