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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Build ASTs from the text of Java source code without knowing how to build the AST
 * elements that make up that text.
 */
@Incubating(since = "7.0.0")
public class JavaTemplate {
    private static final String SNIPPET_MARKER_START = "<<<<START>>>>";
    private static final String SNIPPET_MARKER_END = "<<<<END>>>>";

    private final Supplier<Cursor> parentScopeGetter;
    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final String parameterMarker;
    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;

    private JavaTemplate(Supplier<Cursor> parentScopeGetter, JavaParser parser, String code, Set<String> imports,
                         String parameterMarker, Consumer<String> onAfterVariableSubstitution,
                         Consumer<String> onBeforeParseTemplate) {
        this.parentScopeGetter = parentScopeGetter;
        this.parser = parser;
        this.code = code;
        this.imports = imports;
        this.parameterMarker = parameterMarker;
        this.onAfterVariableSubstitution = onAfterVariableSubstitution;
        this.onBeforeParseTemplate = onBeforeParseTemplate;
        this.parameterCount = StringUtils.countOccurrences(code, parameterMarker);
    }

    public static Builder builder(Supplier<Cursor> parentScope, String code) {
        return new Builder(parentScope, code);
    }

    /**
     * @param changing    The tree that will be returned modified where one of its subtrees will have
     *                    been added or replaced by an AST formed from the template.
     * @param coordinates The point where the template will either insert or replace code.
     * @param parameters  Parameters substituted into the template.
     * @param <J2>        The type of the changing tree.
     * @return A modified form of the changing tree.
     */
    public <J2 extends J> J2 withTemplate(Tree changing, JavaCoordinates<?> coordinates, Object... parameters) {
        Cursor parentScope = parentScopeGetter.get();

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        //Substitute parameter markers with the string representation of each parameter.
        String substitutedTemplate = substituteParameters(parameters);
        onAfterVariableSubstitution.accept(substitutedTemplate);

        J.CompilationUnit cu = parentScope.firstEnclosingOrThrow(J.CompilationUnit.class);

        //Prune down the original AST to just the elements in scope at the insertion point.
        J.CompilationUnit pruned = (J.CompilationUnit) new TemplatePruner(coordinates, imports).visit(cu, parentScope);
        assert pruned != null;

        //As part of the pruning process, the coordinates may have changed if a parent tree is used with coordinates
        //that use replace semantics with an immediate child element.
        JavaCoordinates<?> newCoordinates = parentScope.pollMessage("newCoordinates");
        if (newCoordinates != null) {
            coordinates = newCoordinates;
        }

        String generatedSource = new TemplatePrinter(coordinates).print(pruned, substitutedTemplate);
        onBeforeParseTemplate.accept(generatedSource);

        parser.reset();
        J.CompilationUnit synthetic = parser.parse(generatedSource).iterator().next();

        //Extract the compiled template tree elements.
        ExtractionContext extractionContext = new ExtractionContext();
        new ExtractTemplatedCode().visit(synthetic, extractionContext);

        //noinspection unchecked
        List<J> generatedElements = extractionContext.getSnippets().stream()
                .map(snippet -> (J2) new AutoFormatVisitor<String>().visit(snippet, "", parentScope))
                .collect(toList());

        //noinspection unchecked,ConstantConditions
        return (J2) new InsertAtCoordinates(coordinates).visit(parentScope.getValue(), generatedElements);
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
     * A Java visitor that prunes the original AST down to just the things needed to compile the template code.
     * The typed Cursor represents the insertion point within the original AST.
     */
    private static class TemplatePruner extends JavaIsoVisitor<Cursor> {

        private final JavaCoordinates<?> coordinates;
        private final Set<String> imports;

        TemplatePruner(JavaCoordinates<?> coordinates, Set<String> imports) {
            this.coordinates = coordinates;
            this.imports = imports;
            setCursoringOn();
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Cursor insertionScope) {
            for (String impoort : imports) {
                doAfterVisit(new AddImport<>(impoort, null, false));
            }
            return super.visitCompilationUnit(cu, insertionScope);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, Cursor insertionScope) {
            if (!insertionScope.isScopeInPath(classDecl)) {
                return super.visitClassDecl(classDecl, insertionScope);
            }
            J.ClassDecl c = super.visitClassDecl(classDecl, insertionScope);
            if (coordinates.getTree().getId().equals(c.getId())) {
                switch (coordinates.getSpaceLocation()) {
                    case ANNOTATION_PREFIX:
                        insertionScope.putMessage("newCoordinates", c.getCoordinates().before());
                        break;
                    case TYPE_PARAMETER_SUFFIX:
                        c = c.withTypeParameters(Collections.singletonList(
                                new J.TypeParameter(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                                        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY), null)
                        ));
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", c.getTypeParameters().get(0).getCoordinates().replace());
                        break;
                    case EXTENDS:
                        if (c.getExtends() == null) {
                            c = c.getPadding().withExtends(
                                    new JLeftPadded<>(
                                            Space.format(" "),
                                            new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY),
                                            Markers.EMPTY)
                            );
                        }
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", c.getExtends().getCoordinates().replace());
                        break;
                    case IMPLEMENTS:
                        c = c.getPadding().withImplements(JContainer.build(
                                Space.format(" "),
                                JRightPadded.withElems(emptyList(),
                                        Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY))
                                ),
                                Markers.EMPTY)
                        );
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", c.getImplements().get(0).getCoordinates().replace());
                        break;
                    case BLOCK_END:
                        insertionScope.putMessage("newCoordinates", c.getBody().getCoordinates().replace());
                        break;
                    default:
                }
            }

            return c;
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, Cursor insertionScope) {
            if (!insertionScope.isScopeInPath(method)) {
                return method.withAnnotations(emptyList()).withBody(null);
            }
            J.MethodDecl m = super.visitMethod(method, insertionScope);
            if (coordinates.getTree().getId().equals(m.getId())) {
                switch (coordinates.getSpaceLocation()) {
                    case ANNOTATION_PREFIX:
                        insertionScope.putMessage("newCoordinates", m.getCoordinates().before());
                        break;
                    case TYPE_PARAMETER_SUFFIX:
                        m = m.withTypeParameters(Collections.singletonList(
                                new J.TypeParameter(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                                        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY), null)
                        ));
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", m.getTypeParameters().get(0).getCoordinates().replace());
                        break;
                    case METHOD_DECL_PARAMETERS:
                        m = m.withParams(Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
                        insertionScope.putMessage("newCoordinates", m.getParams().get(0).getCoordinates().replace());
                        break;
                    case THROWS:
                        m = m.withThrows(Collections.singletonList(new J.Empty(Tree.randomId(), Space.format(" "), Markers.EMPTY)));
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", m.getThrows().get(0).getCoordinates().replace());
                        break;
                    case BLOCK_END:
                        if (m.getBody() == null) {
                            m = m.withBody(new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), null, Space.EMPTY));
                        }
                        //noinspection ConstantConditions
                        insertionScope.putMessage("newCoordinates", m.getBody().getCoordinates().replace());
                        break;
                }
            }
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Cursor insertionScope) {
            if (!insertionScope.isScopeInPath(method)) {
                return super.visitMethodInvocation(method, insertionScope);
            }

            J.MethodInvocation m = super.visitMethodInvocation(method, insertionScope);
            if (coordinates.getTree().getId().equals(m.getId())
                    && coordinates.getSpaceLocation() == Space.Location.METHOD_INVOCATION_ARGUMENTS) {
                m = m.withArgs(Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
                insertionScope.putMessage("newCoordinates", m.getArgs().get(0).getCoordinates().replace());
            }
            return m;
        }

        @Override
        public J.Block visitBlock(J.Block block, Cursor insertionScope) {
            Cursor parent = getCursor().dropParentUntil(J.class::isInstance);
            if (coordinates.getTree().getId().equals(block.getId()) ||
                    (!insertionScope.isScopeInPath(block) && !(parent.getValue() instanceof J.ClassDecl))) {
                return block.withStatements(emptyList());
            }

            J.Block b;
            if (!(parent.getValue() instanceof J.ClassDecl)) {
                b = visitAndCast(block, insertionScope, this::preVisit);
                b = b.getPadding().withStatic(visitRightPadded(b.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, insertionScope));
                b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, insertionScope));
                b = visitAndCast(b, insertionScope, this::visitStatement);

                if (b.getStatements().stream().anyMatch(insertionScope::isScopeInPath)) {
                    //If a statement in the block is in insertion scope, then this will render each statement
                    //up to the statement that is in insertion scope.
                    List<JRightPadded<Statement>> statementsInScope = new ArrayList<>();
                    for (JRightPadded<Statement> statement : b.getPadding().getStatements()) {
                        statementsInScope.add(visitRightPadded(statement, JRightPadded.Location.BLOCK_STATEMENT, insertionScope));
                        if (insertionScope.isScopeInPath(statement.getElem())) {
                            break;
                        }
                    }
                    b = b.getPadding().withStatements(statementsInScope);
                }
            } else {
                b = super.visitBlock(block, insertionScope);
            }

            return b;
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Cursor insertionScope) {
            if (!insertionScope.isScopeInPath(variable)) {
                //Variables in the original AST only need to be declared, nulls out the initializers.
                return super.visitVariable(variable.withInitializer(null), insertionScope);
            }

            return super.visitVariable(variable, insertionScope);
        }
    }

    /**
     * Custom Java Printer that will add additional import and add the printed template at the insertion point.
     */
    private static class TemplatePrinter extends JavaPrinter<String> {
        private final JavaCoordinates<?> coordinates;

        private TemplatePrinter(JavaCoordinates<?> coordinates) {
            super(TreePrinter.identity());
            this.coordinates = coordinates;
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, Space.Location location, String template) {
            J parent = getCursor().firstEnclosing(J.class);
            if (parent != null && parent.getId().equals(coordinates.getTree().getId()) &&
                    location == coordinates.getSpaceLocation()) {
                getPrinter().append(getMarkedTemplate(template));
                return space;
            } else {
                return super.visitSpace(space, location, template);
            }
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, String template) {
            if (coordinates.getSpaceLocation() == Space.Location.REPLACE && tree != null && tree.getId().equals(coordinates.getTree().getId())) {
                getPrinter().append(getMarkedTemplate(template));
                return (J) tree;
            }
            return super.visit(tree, template);
        }

        private String getMarkedTemplate(String template) {
            return "/*" + SNIPPET_MARKER_START + "*/" + template
                    + "/*" + SNIPPET_MARKER_END + "*/";
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
        private boolean collectElements = false;
        private final List<CollectedElement> collectedElements = new ArrayList<>();
        private final Set<UUID> collectedIds = new HashSet<>();
        private long startDepth = 0;

        @SuppressWarnings("unchecked")
        private <J2 extends J> List<J2> getSnippets() {
            //This returns all elements that have the same depth as the starting element.
            return collectedElements.stream()
                    .filter(e -> e.depth == startDepth)
                    .map(e -> (J2) e.element)
                    .collect(toList());
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

    private static class ExtractTemplatedCode extends JavaVisitor<ExtractionContext> {
        public ExtractTemplatedCode() {
            setCursoringOn();
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExtractionContext context) {

            long templateDepth = getCursor().getPathAsStream(v -> v instanceof J).count();
            if (findMarker(space, SNIPPET_MARKER_END) != null) {
                //Ending marker found, stop collecting elements. NOTE: if the space was part of a prefix of an element
                //that element will not be collected.
                context.collectElements = false;

                while (context.collectedElements.size() > 1 && getCursor().isScopeInPath(context.collectedElements.get(0).element)) {
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
                J treeValue = getCursor().getValue();
                context.collectedIds.add(treeValue.getId());
                context.startDepth = templateDepth;

                if (getCursor().getValue() instanceof J.CompilationUnit) {
                    //Special case: The starting marker can exist at the compilation unit (when inserting before
                    //the first class declaration (with no imports). Do not add the compilation unit to the collected
                    //elements
                    context.startDepth++;
                    return space;
                }
                List<Comment> comments = new ArrayList<>(space.getComments());
                comments.remove(startToken);
                context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, treeValue.withPrefix(space.withComments(comments))));
            } else if (context.collectElements) {
                //If collecting elements and the current cursor element has not already been collected, add it.
                if (getCursor().getValue() instanceof J) {
                    J treeValue = getCursor().getValue();
                    if (!context.collectedIds.contains(treeValue.getId())) {
                        context.collectedElements.add(new ExtractionContext.CollectedElement(templateDepth, treeValue));
                        context.collectedIds.add(treeValue.getId());
                    }
                }
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

    /**
     * This visitor will insert the generated elements into the correct location within an AST and return the mutated
     * version.
     */
    private static class InsertAtCoordinates extends JavaVisitor<List<? extends J>> {
        private final UUID insertId;
        private final Space.Location location;

        private InsertAtCoordinates(JavaCoordinates<?> coordinates) {
            this.insertId = coordinates.getTree().getId();
            this.location = coordinates.getSpaceLocation();
        }

        @Override
        public @Nullable J preVisit(@Nullable J tree, List<? extends J> generated) {

            if (tree == null || location != Space.Location.REPLACE || !tree.getId().equals(insertId)) {
                return tree;
            }
            // Handles all cases where there is a replace on the current element.
            if (generated.size() == 1) {
                return generated.get(0);
            } else {
                throw new IllegalStateException("The template generated the incorrect number of elements.");
            }
        }

        @Override
        public J visitBlock(J.Block block, List<? extends J> generated) {
            J.Block b = visitAndCast(block, generated, super::visitBlock);
            if (b.getId().equals(insertId) && location == Space.Location.BLOCK_END) {
                //noinspection unchecked
                return b.withStatements(ListUtils.concatAll(b.getStatements(), (List<Statement>) generated));
            }
            //noinspection ConstantConditions
            b = b.withStatements(maybeMergeList(b.getStatements(), generated));
            return b;
        }

        @Override
        @SuppressWarnings("unchecked")
        public J visitClassDecl(J.ClassDecl classDeclaration, List<? extends J> generated) {
            J.ClassDecl c = visitAndCast(classDeclaration, generated, super::visitClassDecl);
            if (insertId.equals(c.getId())) {
                switch (location) {
                    case ANNOTATION_PREFIX:
                        c = c.withAnnotations((List<J.Annotation>) generated);
                        break;
                    case TYPE_PARAMETER_SUFFIX:
                        c = c.withTypeParameters((List<J.TypeParameter>) generated);
                        break;
                    case EXTENDS:
                        c = c.withExtends((TypeTree) generated.get(0));
                        break;
                    case IMPLEMENTS:
                        c = c.withImplements((List<TypeTree>) generated);
                        break;
                    case BLOCK_END:
                        c = c.withBody((J.Block) generated.get(0));
                        break;
                }
            } else {

                c = c.withAnnotations(maybeMergeList(c.getAnnotations(), generated));
                c = c.withTypeParameters(maybeMergeList(c.getTypeParameters(), generated));
                c = c.withImplements(maybeMergeList(c.getImplements(), generated));
            }
            return c;
        }

        @Override
        @SuppressWarnings("unchecked")
        public J visitMethod(J.MethodDecl method, List<? extends J> generated) {
            J.MethodDecl m = visitAndCast(method, generated, super::visitMethod);
            if (insertId.equals(m.getId())) {
                switch (location) {
                    case ANNOTATION_PREFIX:
                        m = m.withAnnotations((List<J.Annotation>) generated);
                        break;
                    case TYPE_PARAMETER_SUFFIX:
                        m = m.withTypeParameters((List<J.TypeParameter>) generated);
                        break;
                    case METHOD_DECL_PARAMETERS:
                        m = m.withParams((List<Statement>) generated);
                        break;
                    case THROWS:
                        m = m.withThrows((List<NameTree>) generated);
                        break;
                    case BLOCK_END:
                        m = m.withBody((J.Block) generated.get(0));
                        break;
                }
            } else {
                m = m.withAnnotations(maybeMergeList(m.getAnnotations(), generated));
                m = m.withTypeParameters(maybeMergeList(m.getTypeParameters(), generated));
                m = m.withThrows(maybeMergeList(m.getThrows(), generated));
                m = m.withAnnotations(maybeMergeList(m.getAnnotations(), generated));
            }
            return m;
        }

        @Override
        @SuppressWarnings("unchecked")
        public J visitMethodInvocation(J.MethodInvocation method, List<? extends J> generated) {
            J.MethodInvocation m = visitAndCast(method, generated, super::visitMethodInvocation);
            if (insertId.equals(m.getId()) && location == Space.Location.METHOD_INVOCATION_ARGUMENTS) {
                m = m.withArgs((List<Expression>) generated);
            } else {
                //noinspection ConstantConditions
                m = m.withArgs(maybeMergeList(m.getArgs(), generated));
            }
            return m;
        }

        @SuppressWarnings("unchecked")
        private <T extends J> @Nullable List<T> maybeMergeList(@Nullable List<T> originalList, List<? extends J> generated) {
            if (originalList != null) {
                for (int index = 0; index < originalList.size(); index++) {
                    if (insertId.equals(originalList.get(index).getId())) {
                        List<T> newList = new ArrayList<>();
                        if (location == Space.Location.REPLACE) {
                            newList.addAll(originalList.subList(0, index + 1));
                            newList.addAll((List<T>) generated);
                            newList.addAll(originalList.subList(index + 1, originalList.size()));
                        } else {
                            newList.addAll(originalList.subList(0, index));
                            newList.addAll((List<T>) generated);
                            newList.addAll(originalList.subList(index, originalList.size()));
                        }
                        return newList;
                    }
                }
            }
            return originalList;
        }
    }

    public static class Builder {
        private final Supplier<Cursor> parentScope;
        private final String code;
        private final Set<String> imports = new HashSet<>();

        private JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();

        private String parameterMarker = "#{}";

        private Consumer<String> onAfterVariableSubstitution = s -> {
        };
        private Consumer<String> onBeforeParseTemplate = s -> {
        };

        Builder(Supplier<Cursor> parentScope, String code) {
            this.parentScope = parentScope;
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
         * Examples:
         * <p>
         * java.util.Collections.emptyList
         * java.util.Collections.*
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

        public Builder javaParser(JavaParser javaParser) {
            this.javaParser = javaParser;
            return this;
        }

        /**
         * Define an alternate marker to denote where a parameter should be inserted into the template. If not specified, the
         * default format for parameter marker is "#{}"
         */
        public Builder parameterMarker(String parameterMarker) {
            this.parameterMarker = parameterMarker;
            return this;
        }

        public Builder doAfterVariableSubstitution(Consumer<String> afterVariableSubstitution) {
            this.onAfterVariableSubstitution = afterVariableSubstitution;
            return this;
        }

        public Builder doBeforeParseTemplate(Consumer<String> beforeParseTemplate) {
            this.onBeforeParseTemplate = beforeParseTemplate;
            return this;
        }

        public JavaTemplate build() {
            return new JavaTemplate(parentScope, javaParser, code, imports, parameterMarker,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
