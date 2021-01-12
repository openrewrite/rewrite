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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.format.AutoFormatProcessor;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.search.FindTypesInNameScope;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.0.0")
public class JavaTemplate {
    private static final Logger logger = LoggerFactory.getLogger(JavaTemplate.class);

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

    public <J2 extends J> List<J2> generateBefore(@NonNull Cursor insertionScope, Object... parameters) {
        return generate(false, insertionScope, parameters);
    }

    public <J2 extends J> List<J2> generateAfter(@NonNull Cursor insertionScope, Object... parameters) {
        return generate(true, insertionScope, parameters);
    }

    private <J2 extends J> List<J2> generate(boolean after,
                                             @NonNull Cursor insertionScope,
                                             Object... parameters) {

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        //Extract any types from parameters that will be inserted into the template.
        Set<JavaType> parameterTypes = stream(parameters)
                .filter(J.class::isInstance)
                .map(J.class::cast)
                .flatMap(p -> FindTypesInNameScope.find(p).stream())
                .collect(toSet());

        //Substitute parameter markers with the string representation of each parameter.
        final String printedTemplate = substituteParameters(parameters);

        J.CompilationUnit cu = insertionScope.firstEnclosing(J.CompilationUnit.class);
        assert cu != null;

        //Prune down the original AST to just the elements in scope at the insertion point.
        cu = new TemplateProcessor(parameterTypes).visitCompilationUnit(cu, insertionScope);

        String generatedSource = new TemplatePrinter(after, insertionScope, imports)
                .visit(cu, printedTemplate);

        logger.debug("Generated Source:\n-------------------\n{}\n-------------------", generatedSource);

        parser.reset();
        cu = parser.parse(generatedSource).iterator().next();

        //Extract the compiled template tree elements.
        Extraction extraction = new Extraction();
        new ExtractTemplatedCode().visit(cu, extraction);

        @SuppressWarnings("ConstantConditions") Collection<? extends Style> styles = insertionScope
                .firstEnclosing(J.CompilationUnit.class).getStyles();

        List<J> snippets = extraction.getSnippets();
        return snippets.stream()
                .map(t -> {
                    if (autoFormat) {
                        //noinspection unchecked
                        return (J2) new AutoFormatProcessor<Void>(styles).visit(t, null,
                                insertionScope.getParentOrThrow());
                    }
                    //noinspection unchecked
                    return (J2) t;
                })
                .collect(Collectors.toList());
    }

    /**
     * Replace the parameter markers in the template with the parameters passed into the generate method.
     * Parameters that are Java Tree's will be correctly printed into the string. The parameters are not named and
     * rely purely on ordinal position.
     *
     * @param parameters A list of parameters
     * @return The final snippet to be generated.
     */
    private String substituteParameters(Object... parameters) {
        String codeInstance = code;
        for (Object parameter : parameters) {
            codeInstance = StringUtils.replaceFirst(codeInstance, parameterMarker,
                    substituteParameter(parameter));
        }
        return codeInstance;
    }

    private String substituteParameter(Object parameter) {
        if (parameter instanceof Tree) {
            return ((Tree) parameter).printTrimmed();
        } else if (parameter instanceof JRightPadded) {
            return substituteParameter(((JRightPadded<?>) parameter).getElem());
        } else if (parameter instanceof JLeftPadded) {
            return substituteParameter(((JLeftPadded<?>) parameter).getElem());
        }
        return parameter.toString();
    }

    /**
     * A java processor that prunes the original AST down to just the things needed to compile the template code.
     * The typed Cursor represents the insertion point within the original AST.
     */
    private static class TemplateProcessor extends JavaIsoProcessor<Cursor> {
        private final Set<JavaType> referencedTypes;

        TemplateProcessor(Set<JavaType> referencedTypes) {
            this.referencedTypes = referencedTypes;
            setCursoringOn();
        }

        @Override
        public J.Block visitBlock(J.Block block, Cursor insertionScope) {
            Cursor parent = getCursor().getParent();

            if (parent != null && !(parent.getTree() instanceof J.ClassDecl) && insertionScope.isScopeInPath(block)) {
                J.Block b = call(block, insertionScope, this::visitEach);
                b = b.withStatik(b.getStatic() != null ? visitSpace(b.getStatic(), insertionScope) : null);
                b = b.withPrefix(visitSpace(b.getPrefix(), insertionScope));
                b = call(b, insertionScope, this::visitStatement);

                if (b.getStatements().stream().anyMatch(s -> insertionScope.isScopeInPath(s.getElem()))) {
                    //If a statement in the block is in insertion scope, then this will render each statement
                    //up to the statement that is in insertion scope.
                    List<JRightPadded<Statement>> statementsInScope = new ArrayList<>();
                    for (JRightPadded<Statement> statement : b.getStatements()) {
                        statementsInScope.add(call(statement, insertionScope));
                        if (insertionScope.isScopeInPath(statement.getElem())) {
                            break;
                        }
                    }
                    return b.withStatements(statementsInScope);
                }
            } else if (parent != null && parent.getTree() instanceof J.ClassDecl) {
                return super.visitBlock(block, insertionScope);
            }
            return block.withStatements(emptyList());
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, Cursor insertionScope) {
            //If the method is within insertion scope, then we must traverse the method.
            if (insertionScope.isScopeInPath(method)) {
                return super.visitMethod(method, insertionScope);
            }

            if (referencedTypes.contains(method.getType())) {
                return method.withAnnotations(emptyList())
                        .withBody(null);
            }

            //Otherwise, prune the method declaration.
            return null;
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Cursor insertionScope) {
            J.VariableDecls.NamedVar var = super.visitVariable(variable, insertionScope);
            //Variables in the original AST only need to be declared, this nulls out the initializers.
            return var.withInitializer(null);
        }
    }

    /**
     * Custom Java Printer that will add additional import and add the printed template at the insertion point.
     */
    private static class TemplatePrinter extends JavaPrinter<String> {

        private final Set<String> imports;

        TemplatePrinter(boolean after, Cursor insertionScope, Set<String> imports) {
            super(new TreePrinter<String>() {
                @Override
                public String doLast(Tree tree, String printed, String printedTemplate) {
                    // individual statement, but block doLast which is invoking this adds the ;
                    if (insertionScope.getTree().getId().equals(tree.getId())) {
                        String templateCode = "/*" + SNIPPET_MARKER_START + "*/" +
                                printedTemplate + "/*" + SNIPPET_MARKER_END + "*/";
                        if (after) {
                            // since the visit method on J.Block is responsible for adding the ';', we
                            // add it pre-emptively here before concatenating the template.
                            return printed +
                                    ((insertionScope.getParentOrThrow().getTree() instanceof J.Block) ?
                                            ";" : "") +
                                    "\n" + templateCode;
                        } else {
                            return "\n" + templateCode + "\n" + printed;
                        }
                    } else {
                        return printed;
                    }
                }
            });
            this.imports = imports;
        }

        @Override
        public String visitCompilationUnit(J.CompilationUnit cu, String acc) {

            //Print all original imports from the compilation unit
            String originalImports = super.visit(cu.getImports(), ";", acc);

            StringBuilder output = new StringBuilder(originalImports.length() + acc.length() + 1024);
            output.append(originalImports);
            if (!cu.getImports().isEmpty()) {
                output.append(";");
            }

            for (String i : imports) {
                output.append(i);
            }

            //Visit the classes of the compilation unit.
            return output.append(visit(cu.getClasses(), acc)).append(visit(cu.getEof())).toString();
        }
    }

    private static class Extraction {
        Map<Long, List<J>> snippetsByDepth = new TreeMap<>();
        Long endDepth;

        public List<J> getSnippets() {
            return snippetsByDepth.get(endDepth);
        }
    }

    private static class ExtractTemplatedCode extends JavaProcessor<Extraction> {
        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, Extraction context) {
            long templateDepth = getCursor().getPathAsStream().count();
            Comment startToken = findMarker(space, SNIPPET_MARKER_START);
            if (findMarker(space, SNIPPET_MARKER_END) != null) {
                context.endDepth = templateDepth;
            }

            if (startToken != null) {
                List<Comment> comments = new ArrayList<>(space.getComments());
                comments.remove(startToken);
                context.snippetsByDepth.computeIfAbsent(templateDepth, n -> new ArrayList<>())
                        .add(((J) getCursor().getTree()).withPrefix(space.withComments(comments)));
            } else if (context.endDepth == null) {
                context.snippetsByDepth.computeIfAbsent(templateDepth, n -> new ArrayList<>())
                        .add(getCursor().getTree());
            }

            return space;
        }

        @Nullable
        private Comment findMarker(@Nullable Space space, String marker) {
            if (space == null) {
                return null;
            }
            return space.getComments().stream()
                    .filter(c -> Comment.Style.BLOCK.equals(c.getStyle()))
                    .filter(c -> c.getText().equals(marker))
                    .findAny()
                    .orElse(null);
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

        Builder(@NonNull String code) {
            this.code = code.trim();
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
}
