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
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.format.AutoFormatProcessor;
import org.openrewrite.java.internal.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import static java.util.Collections.emptyList;

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

        //Substitute parameter markers with the string representation of each parameter.
        String printedTemplate = substituteParameters(parameters);

        final J.CompilationUnit cu = insertionScope.firstEnclosing(J.CompilationUnit.class);
        assert cu != null;

        //Walk up the insertion scope and find the first element that is an immediate child of a J.Block. The template
        //will always be inserted into a block.
        boolean memberVariableInitializer = false;

        while(insertionScope.getParent() != null &&
                !(insertionScope.getParent().getTree() instanceof J.CompilationUnit) &&
                !(insertionScope.getParent().getTree() instanceof J.Block)
            ) {

            if (insertionScope.getParent().getTree() instanceof J.VariableDecls.NamedVar) {
                //There is one edge case that can trip up compilation: If the insertion scope is the initializer
                //of a member variable and that scope is not itself in a nested block. In this case, a class block
                //must be created to correctly compile the template

                //Find the first block's parent and if that parent is a class declaration, account for the edge case.
                Iterator<Tree> index =  insertionScope.getPath();
                while (index.hasNext()) {
                    if (index.next() instanceof J.Block && index.hasNext() && index.next() instanceof J.ClassDecl) {
                        memberVariableInitializer = true;
                    }
                }
            }
            insertionScope = insertionScope.getParent();
        }

        //Prune down the original AST to just the elements in scope at the insertion point.
        J.CompilationUnit synthetic = new TemplateProcessor().visitCompilationUnit(cu, insertionScope);

        String generatedSource = new TemplatePrinter(after, memberVariableInitializer, insertionScope, imports)
                .visit(synthetic, printedTemplate);

        logger.debug("Generated Source:\n-------------------\n{}\n-------------------", generatedSource);

        parser.reset();
        synthetic = parser.parse(generatedSource).iterator().next();

        //Extract the compiled template tree elements.
        ExtractionContext extractionContext = new ExtractionContext();
        new ExtractTemplatedCode().visit(synthetic, extractionContext);

        List<J> snippets = extractionContext.getSnippets();
        final Cursor finalInsertionScope = insertionScope;
        return snippets.stream()
                .map(t -> {
                    if (autoFormat) {
                        //noinspection unchecked
                        return (J2) new AutoFormatProcessor<Void>(cu.getStyles()).visit(t, null,
                                finalInsertionScope.getParentOrThrow());
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

        TemplateProcessor() {
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

            return method.withAnnotations(emptyList())
                    .withBody(null);
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Cursor insertionScope) {
            if (!insertionScope.isScopeInPath(variable)) {
                //Variables in the original AST only need to be declared, this nulls out the initializers.
                variable = variable.withInitializer(null);
            } else {
                //A variable within the insertion scope, we must mutate
                variable = variable.withName(variable.getName().withName("_" + variable.getSimpleName()));
            }
            return super.visitVariable(variable, insertionScope);
        }
    }

    /**
     * Custom Java Printer that will add additional import and add the printed template at the insertion point.
     */
    private static class TemplatePrinter extends JavaPrinter<String> {

        private final Set<String> imports;


        TemplatePrinter(boolean after, boolean memberVariableInitializer, Cursor insertionScope, Set<String> imports) {
            super(new TreePrinter<String>() {
                @Override
                public String doLast(Tree tree, String printed, String printedTemplate) {
                    //Note: A block is added around the template and markers when the insertion point is within a
                    //      member variable initializer to prevent compiler issues.
                    String blockStart = memberVariableInitializer ? "{" : "";
                    String blockEnd = memberVariableInitializer ? "}" : "";
                    // individual statement, but block doLast which is invoking this adds the ;
                    if (insertionScope.getTree().getId().equals(tree.getId())) {
                        String templateCode = blockStart + "/*" + SNIPPET_MARKER_START + "*/" +
                                printedTemplate + "/*" + SNIPPET_MARKER_END + "*/" + blockEnd;
                        if (after) {
                            // since the visit method on J.Block is responsible for adding the ';', we
                            // add it pre-emptively here before concatenating the template.
                            printed = printed +
                                    ((insertionScope.getParentOrThrow().getTree() instanceof J.Block) ?
                                            ";" : "") +
                                    "\n" + templateCode;
                        } else {
                            printed = "\n" + templateCode + "\n" + printed;
                        }
                    }
                    return printed;
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
                output.append(";\n");
            }

            for (String i : imports) {
                output.append(i);
            }

            //Visit the classes of the compilation unit.
            return output.append(visit(cu.getClasses(), acc)).append(visit(cu.getEof())).toString();
        }
    }

    /**
     * The template code is marked before/after with comments. The extraction code will grab cursor elements between
     * those two markers. Depending on insertion scope, the first element (the one that has the start marker comment)
     * may not be part of the template. The context is used to demarcate when elements should be collected, collect
     * the elements of the template (and keeping track of the depth those elements appear in the tree), and finally
     * keep track of element IDs that have already been collected (so they are not inadvertently added twice)
     */
    private static class ExtractionContext {
        boolean collectElements = false;
        List<CollectedElement> collectedElements = new ArrayList<>();
        Set<UUID> collectedIds = new HashSet<>();
        long startDepth = 0;

        public List<J> getSnippets() {
            //This returns all elements that have the same depth as the starting element.
            return collectedElements.stream().filter(e -> e.depth == startDepth).map(e -> e.element).collect(Collectors.toList());
        }

        /**
         * The context captures each element and it's depth in the tree.
         */
        private static class CollectedElement {
            final long depth;
            final J element;
            CollectedElement(long depth, J element) {
                this.depth = depth;
                this.element = element;
            }
        }
    }

    private static class ExtractTemplatedCode extends JavaProcessor<ExtractionContext> {
        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, ExtractionContext context) {

            long templateDepth = getCursor().getPathAsStream().count();
            if (findMarker(space, SNIPPET_MARKER_END) != null) {
                //Ending marker found, stop collecting elements. NOTE: if the space was part of a prefix of an element
                //that element will not be collected.
                context.collectElements = false;

                if (context.collectedElements.size() > 1 && getCursor().isScopeInPath(context.collectedElements.get(0).element)) {
                    //If we have collected more than one element and the ending element is on the path of the first element, then
                    //the first element does not belong to the template, exclude it and move the start depth up.
                    context.collectedElements.remove(0);
                    context.startDepth++;
                }
            }

            Comment startToken = findMarker(space, SNIPPET_MARKER_START);
            if (startToken != null) {
                //If the starting marker is found, record the starting depth, collect the current cursor tree element,
                //remove the marker comment, and flag the extractor to start collecting all elements until the end marker
                //is found.
                context.collectElements = true;
                context.collectedIds.add(getCursor().getTree().getId());
                context.startDepth = templateDepth;

                if (getCursor().getTree() instanceof J.CompilationUnit) {
                    //Special case: The starting marker can exist at the compilation unit (when inserting before
                    //the first class declaration (with no imports). Do not add the compilation unit to the collected
                    //elements
                    context.startDepth++;
                    return space;
                }
                List<Comment> comments = new ArrayList<>(space.getComments());
                comments.remove(startToken);
                context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, ((J) getCursor().getTree()).withPrefix(space.withComments(comments))));
            } else if (context.collectElements && !context.collectedIds.contains(getCursor().getTree().getId())) {
                //If collecting elements and the current cursor element has not already been collected, add it.
                context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, (getCursor().getTree())));
                context.collectedIds.add(getCursor().getTree().getId());
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
