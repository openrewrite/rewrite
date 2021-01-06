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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.search.FindReferencedTypes;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

public class JavaTemplate {
    private static final String SNIPPET_MARKER_START = "<<<<START>>>>";
    private static final String SNIPPET_MARKER_END = "<<<<END>>>>";

    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final boolean autoFormat;
    private final String parameterMarker;

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

    public <J2 extends J> List<J2> generate(@NonNull Cursor insertionScope, Object... parameters) {
        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        Set<JavaType> parameterTypes = Arrays.stream(parameters)
                .filter(J.class::isInstance)
                .map(J.class::cast)
                .flatMap(p -> FindReferencedTypes.find(p).stream())
                .collect(toSet());

        String printedInsertionScope = substituteParameters(parameters);

        J.CompilationUnit cu = insertionScope.firstEnclosing(J.CompilationUnit.class);
        assert cu != null;

        parser.reset();
        cu = parser.parse(cu.print(new TemplatePrinter(printedInsertionScope,
                insertionScope, parameterTypes))).iterator().next();

//        if (autoFormat) {
//            // TODO format the new tree
//        }

        List<J> snippetElements = new ArrayList<>();
        new ExtractTemplatedCode().visit(cu, snippetElements);
        return (List<J2>) snippetElements;
    }

    /**
     * Replace the parameter markers in the template with the parameters passed into the generate method.
     * Parameters that are Java Tree's will be correctly printed into the string. The parameters are not named and
     * this relies purely on ordinal position of the parameter.
     *
     * @param parameters A list of parameters
     * @return The final snippet to be generated.
     */
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

    private static class TemplatePrinter implements TreePrinter<Cursor> {
        private final String template;
        private final Cursor scope;
        private final Set<JavaType> referencedTypes;

        private TemplatePrinter(String template, Cursor scope, Set<JavaType> referencedTypes) {
            this.template = "/*" + SNIPPET_MARKER_START + "*/" + template +
                    "/*" + SNIPPET_MARKER_END + "*/";
            this.scope = scope;
            this.referencedTypes = referencedTypes;
        }

        @Override
        public <T2 extends Tree> T2 doFirst(T2 tree, Cursor cursor) {
            //noinspection unchecked
            return (T2) new JavaProcessor<Void>() {
                {
                    setCursoringOn();
                }

                @Override
                public J visitMethod(J.MethodDecl method, Void unused) {
                    if (referencedTypes.contains(method.getType())) {
                        return method.withBody(null).withAnnotations(emptyList());
                    }
                    if (cursor.isScopeInPath(method)) {
                        return super.visitMethod(method, unused);
                    }
                    return null;
                }

                @Override
                public J visitVariable(J.VariableDecls.NamedVar variable, Void unused) {
                    if (referencedTypes.contains(variable.getType())) {
                        return variable.withInitializer(null);
                    }
                    return null;
                }

                @Override
                public J visitBlock(J.Block block, Void unused) {
                    if (getCursor().getParentOrThrow().getParentOrThrow().getTree() instanceof J.ClassDecl ||
                            cursor.isScopeInPath(block)) {
                        return super.visitBlock(block, unused);
                    }
                    return null;
                }
            }.visit(tree, null);
        }

        @Override
        public String doLast(Tree tree, String printed, Cursor cursor) {
            StringBuilder print = new StringBuilder();

            new JavaProcessor<StringBuilder>() {
                @Override
                public J visitEach(J tree, StringBuilder acc) {
                    if (cursor.getTree().isScope(tree)) {
                        // if before
                        acc.append(template).append(printed);
                        // if after
                        acc.append(printed).append(template);
                    } else {
                        acc.append(printed);
                    }
                    return super.visitEach(tree, acc);
                }
            }.visit(tree, print);

            return print.toString();
        }
    }

    public static class Builder {

        private final String code;
        private JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();
        private final Set<String> imports = new HashSet<>();

        private boolean autoFormat = true;
        private String parameterMarker = "#{}";

        Builder(String code) {
            this.code = code;
        }

        /**
         * A list of fully-qualified types that will be added when generating/compiling snippets
         *
         * <PRE>
         * Examples:
         * <p>
         * java.util.Collections
         * java.util.Date
         * </PRE>
         */
        public Builder imports(@NonNull String... fullyQualifiedTypeNames) {
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
         * Examples:
         * <p>
         * java.util.Collections.emptyList
         * java.util.Collections.*
         * </PRE>
         */
        public Builder staticImports(@NonNull String... fullyQualifiedMemberTypeNames) {
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

    private static class ExtractTemplatedCode extends JavaProcessor<List<J>> {
        private long templateDepth = -1;
        private boolean snippetEnd = false;

        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, List<J> context) {
            Comment startToken = findMarker(space, SNIPPET_MARKER_START);
            if (startToken != null) {
                templateDepth = getCursor().getPathAsStream().count();
                List<Comment> comments = new ArrayList<>(space.getComments());
                comments.remove(startToken);
                context.add(((J) getCursor().getTree()).withPrefix(space.withComments(comments)));
            } else if (!context.isEmpty() && getCursor().getPathAsStream().count() == templateDepth && !snippetEnd) {
                context.add(getCursor().getTree());
            } else if (findMarker(space, SNIPPET_MARKER_END) != null) {
                snippetEnd = true;
            }

            return space;
        }

        private Comment findMarker(Space space, String marker) {
            return space.getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(marker))
                    .findAny()
                    .orElse(null);
        }
    }
}
