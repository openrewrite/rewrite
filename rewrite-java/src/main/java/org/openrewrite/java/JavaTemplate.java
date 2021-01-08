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
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.search.FindTypesInNameScope;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Tree.randomId;

@Incubating(since="7.0.0")
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
        return generate(InsertionStrategy.INSERT_BEFORE, insertionScope, parameters);
    }

    public <J2 extends J> List<J2> generate(@NonNull Cursor insertionScope, Object... parameters) {
        return generate(InsertionStrategy.REPLACE, insertionScope, parameters);
    }

    public <J2 extends J> List<J2> generateAfter(@NonNull Cursor insertionScope, Object... parameters) {
        return generate(InsertionStrategy.INSERT_AFTER, insertionScope, parameters);
    }

    private <J2 extends J> List<J2> generate(final InsertionStrategy insertionStrategy,
                                             final @NonNull Cursor insertionScope, final Object... parameters) {

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        //Extract any types from parameters that will be inserted into the template.
        Set<JavaType> parameterTypes = Arrays.stream(parameters)
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

        String generatedSource = new TemplatePrinter(insertionStrategy, insertionScope.getTree().getId(), imports)
                .visit(cu, printedTemplate);

        logger.debug("Generated Source:\n-------------------\n{}\n-------------------", generatedSource);

        parser.reset();
        cu = parser.parse(generatedSource).iterator().next();

//        if (autoFormat) {
//            // TODO format the new tree
//        }

        //Extract the compiled template tree elements.
        List<J> snippetElements = new ArrayList<>();
        new ExtractTemplatedCode().visit(cu, snippetElements);
        //noinspection unchecked
        return (List<J2>) snippetElements;
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

    /**
     * A java processor that prunes the original AST down to just the things needed to compile the template code.
     * The typed Cursor represents the insertion point within the original AST.
     */
    private static class TemplateProcessor extends JavaIsoProcessor<Cursor> {

        private final Set<JavaType> referencedTypes;
        private final J.Return nullReturn = new J.Return(randomId(), null, Markers.EMPTY,
                        new J.Literal(randomId(), new Space(" ", emptyList()), Markers.EMPTY,  null, "null", JavaType.Primitive.Null)
                );
        TemplateProcessor(Set<JavaType> referencedTypes) {
            this.referencedTypes = referencedTypes;
            setCursoringOn();
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, Cursor insertionScope) {
            //If the method is within insertion scope, then we must traverse the method.
            if (insertionScope.isScopeInPath(method)) {
                return super.visitMethod(method, insertionScope);
            }

            if (referencedTypes.contains(method.getType())) {
                if (method.getBody() == null) {
                    //Abstract method will have no body.
                    return method.withAnnotations(emptyList());
                }
                //If the method is referenced, they method body is replaced with a stubbed return statement.
                TypeTree returnType = method.getReturnTypeExpr();
                List<JRightPadded<Statement>>  statements = new ArrayList<>();
                if (returnType instanceof J.Primitive) {
                    J.Primitive primitive = (J.Primitive) returnType;
                    //Note void primitive will result in an empty body.
                    if (primitive.getType() != JavaType.Primitive.Void) {
                        //For all other primitive types, return the default value for that primitive
                        statements.add(new JRightPadded<>(
                                new J.Return(randomId(), null, Markers.EMPTY,
                                    new J.Literal(randomId(), new Space(" ", emptyList()), Markers.EMPTY,
                                            null, primitive.getType().getDefaultValue(), primitive.getType()
                                    )
                                ), null));
                    }
                } else {
                    //All fully-qualified types/arrays will return null.
                    statements.add(new JRightPadded<>(nullReturn, null));
                }
                return method.withBody(method.getBody().withStatements(statements)).withAnnotations(emptyList());
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

        @Override
        public J.Block visitBlock(J.Block block, Cursor insertionScope) {

            Cursor parent = getCursor().getParent();

            if (parent != null && !(parent.getTree() instanceof J.ClassDecl) && insertionScope.isScopeInPath(block)) {

                J.Block b = call(block, insertionScope, this::visitEach);
                b = b.withStatik(visitSpace(b.getStatic(), insertionScope));
                b = b.withPrefix(visitSpace(b.getPrefix(), insertionScope));
                b = call(b, insertionScope, this::visitStatement);


                if (b.getStatements().stream().anyMatch(s -> insertionScope.isScopeInPath(s.getElem()))) {
                    //If a statement in the block is in insertion scope, then this will render each statement
                    //up to the statement that is in insertion scope.
                    List<JRightPadded<Statement>> statementsInScope = new ArrayList<>();
                    for (JRightPadded<Statement> statement : b.getStatements()) {
                        statementsInScope.add(statement);
                        if (insertionScope.isScopeInPath(statement.getElem())) {
                            break;
                        }
                    }
                    b = b.withStatements(ListUtils.map(statementsInScope, t -> call(t, insertionScope)));
                    return b.withEnd(visitSpace(b.getEnd(), insertionScope));
                }
            } else if (parent != null && parent.getTree() instanceof J.ClassDecl) {
                    return super.visitBlock(block, insertionScope);
            }
            return block.withStatements(Collections.emptyList());
        }
    }

    /**
     * Custom Java Printer that will add additional import and add the printed template at the insertion point.
     */
    private static class TemplatePrinter extends JavaPrinter<String> {

        private final Set<String> imports;

        TemplatePrinter(InsertionStrategy insertionStrategy, UUID insertionPoint, Set<String> imports) {
            super(new TreePrinter<String>() {
                @Override
                public String doLast(Tree tree, String printed, String printedTemplate) {
                    if (insertionPoint.equals(tree.getId())) {
                        StringBuilder templateCode = new StringBuilder()
                                .append("/*").append(SNIPPET_MARKER_START).append("*/");

                        if (printedTemplate.endsWith(";")) {
                            //If the printed template ends with a ;, we want to make sure the ending marker is BEFORE
                            //the semi-colon.
                            //noinspection StringOperationCanBeSimplified
                            templateCode.append(printedTemplate.substring(0, printedTemplate.length() - 1))
                                    .append("/*").append(SNIPPET_MARKER_END).append("*/;");
                        } else {
                            templateCode.append(printedTemplate).append("/*").append(SNIPPET_MARKER_END).append("*/");
                        }

                        if (insertionStrategy == InsertionStrategy.REPLACE) {
                            return templateCode.toString();
                        } else if (insertionStrategy == InsertionStrategy.INSERT_BEFORE) {
                            return templateCode.append(printed).toString();
                        } else {
                            return printed + templateCode.toString();
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

            output.append("\n\n//Additional Imports\n");
            for (String _import : imports) {
                output.append(_import);
            }

            //Visit the classes of the compilation unit.
            return output.append(visit(cu.getClasses(), acc)).append(visit(cu.getEof())).toString();
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
                //noinspection SuspiciousMethodCalls
                if (!context.contains(getCursor().getTree())) {
                    context.add(getCursor().getTree());
                }
            }
            if (findMarker(space, SNIPPET_MARKER_END) != null) {
                snippetEnd = true;
            }

            return space;
        }

        private Comment findMarker(Space space, String marker) {
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

    /**
     * The insertion strategy determines where the template's code will be inserted into an the AST relative to the insertion point.
     */
    private enum InsertionStrategy {
        INSERT_BEFORE,
        REPLACE,
        INSERT_AFTER
    }
}
