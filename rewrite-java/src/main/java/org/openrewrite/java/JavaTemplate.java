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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JavaTemplate {

    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final boolean autoFormat;
    private final String parameterMarker;

    private final JavaPrinter<String> printer = new JavaPrinter<String>(new TreePrinter<J, String>() {
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

    private JavaTemplate(JavaParser parser, String code, Set<String> imports, boolean autoFormat, String parameterMarker) {
        this.parser = parser;
        this.code = code;
        this.parameterCount = StringUtils.countOccurrences(code, parameterMarker);
        this.imports = imports;
        this.autoFormat = autoFormat;
        this.parameterMarker = parameterMarker;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public <J2 extends J> List<J2> generate(Cursor insertionScope, Object... params) {

        if (params.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        // flat map params to list of method declaration and variable, i.e. JavaType.Method and JavaType

        String printedInsertionScope = substituteParameters(params);

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

    private String substituteParameters(Object... parameters) {

        String codeInstance = code;
        for (Object parameter : parameters) {
            String value;
            if (parameter instanceof Tree) {
                value = ((Tree) parameter).printTrimmed();
            } else {
                value = parameter.toString();
            }
            codeInstance = StringUtils.replaceFirst(codeInstance, parameterMarker, value);
        }
        return codeInstance;
    }

    private static class ExtractTemplatedCode extends JavaProcessor {
        private long templateDepth = -1;
        private final List<J> templated = new ArrayList<>();

        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public J visitEach(J tree, ExecutionContext ctx) {
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


    /**
     * Convert a JavaType into a string import statement of the form:
     * <P><PRE>
     * FullyQualfied Types : import {}fully-qualified-name};
     * Method Types        : static import {fully-qualified-name}.{methodName};
     * </PRE>
     */
    private static String javaTypeToImport(JavaType type) {
        StringBuilder anImport = new StringBuilder(256);
        if (type instanceof JavaType.FullyQualified) {
            anImport.append("import ").append(((JavaType.FullyQualified) type).getFullyQualifiedName()).append(";\n");
        } else if (type instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            anImport.append("static import ").append(method.getDeclaringType().getFullyQualifiedName())
                    .append(".").append(method.getName()).append(";\n");
        } else {
            throw new IllegalArgumentException("Unsupported import type.");
        }
        return anImport.toString();
    }

    public static class Builder {

        private final String code;
        private JavaParser javaParser;
        private Set<String> imports;

        private boolean autoFormat = true;
        private String parameterMarker = "#{}";

        Builder(String code) {
            this.code = code;
        }

        /**
         * A list of fully-qualified types that will be added when generating/compiling snippets
         *
         * <PRE>
         *     Examples:
         *
         *     java.util.Collections
         *     java.util.Date
         * </PRE>
         */
        public Builder imports(String... fullyQualifiedTypeNames) {
            for (String typeName : fullyQualifiedTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("import " + typeName + ";\n");
            }
            return this;
        }

        /**
         * A list of fully-qualified member type names that will be statically imported when generating/compiling snippets.
         *
         * <PRE>
         *     Examples:
         *
         *     java.util.Collections.emptyList
         *     java.util.Collections.*
         * </PRE>
         */
        public Builder staticImports(String... fullyQualifiedMemberTypeNames) {
            for (String typeName : fullyQualifiedMemberTypeNames) {
                if (typeName.startsWith("import ") || typeName.startsWith("static ")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include an \"import \" or \"static \" prefix");
                } else if (typeName.endsWith(";") || typeName.endsWith("\n")) {
                    throw new IllegalArgumentException("Imports are expressed as fully-qualified names and should not include a suffixed terminator");
                }
                this.imports.add("static import " + typeName + ";\n");
            }
            return this;
        }

        /**
         * A list of JavaTypes that will be imported/statically imported when generating/compiling snippets.
         * <P><P>
         * FullyQualified types will be added as an import and Method types will be statically imported
         */
        public Builder imports(JavaType... types) {
            for (JavaType type : types) {
                imports.add(javaTypeToImport(type));
            }
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

        /**
         * Define an alternate marker to denote where a parameter should be inserted into the template. If not specified, the
         * default format for parameter marker is "#{}"
         */
        public Builder parameterMarker(@NonNull String parameterMarker) {
            this.parameterMarker = parameterMarker;
            return this;
        }

        public JavaTemplate build() {
            if (javaParser == null) {
                javaParser = JavaParser.fromJavaVersion()
                        .logCompilationWarningsAndErrors(false)
                        .build();
            }
            return new JavaTemplate(javaParser, code, imports, autoFormat, parameterMarker);
        }
    }

}
