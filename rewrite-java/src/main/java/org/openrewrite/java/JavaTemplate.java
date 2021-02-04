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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Incubating(since = "7.0.0")
public class JavaTemplate {

    public interface TemplateEventHandler {

        void afterVariableSubstitution(String substitutedTemplate);

        void beforeParseTemplate(String generatedTemplate);

    }

    private static final String SNIPPET_MARKER_START = "<<<<START>>>>";
    private static final String SNIPPET_MARKER_END = "<<<<END>>>>";

    private final JavaParser parser;
    private final String code;
    private final int parameterCount;
    private final Set<String> imports;
    private final String parameterMarker;
    @Nullable
    private final TemplateEventHandler templateEventHandler;

    private JavaTemplate(JavaParser parser, String code, Set<String> imports, String parameterMarker, @Nullable TemplateEventHandler templateEventHandler) {
        this.parser = parser;
        this.code = code;
        this.parameterCount = StringUtils.countOccurrences(code, parameterMarker);
        this.imports = imports;
        this.parameterMarker = parameterMarker;
        this.templateEventHandler = templateEventHandler;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public <J2 extends J> List<J2> generate(Cursor parentScope, JavaCoordinates<?> coordinates, Object... parameters) {

        if (parameters.length != parameterCount) {
            throw new IllegalArgumentException("This template requires " + parameterCount + " parameters.");
        }

        //Substitute parameter markers with the string representation of each parameter.
        String printedTemplate = substituteParameters(parameters);

        if (templateEventHandler != null) {
            templateEventHandler.afterVariableSubstitution(printedTemplate);
        }

        J.CompilationUnit cu = parentScope.firstEnclosingOrThrow(J.CompilationUnit.class);

        //Prune down the original AST to just the elements in scope at the insertion point.
        J.CompilationUnit pruned = (J.CompilationUnit) new TemplateVisitor(coordinates, imports).visit(cu, parentScope);

        //As part of the pruning process, the coordinates may have changed if a parent tree is used with coordinates
        //that use replace semantics with an immediate child element.
        JavaCoordinates<?> newCoordinates = parentScope.pollMessage("newCoordinates");
        if (newCoordinates != null) {
            coordinates = newCoordinates;
        }

        //Walk down from the parent scope to find the tree element mapped by the coordinates.
        AtomicReference<Cursor> cursorReference = new AtomicReference<>();
        new FindCoordinateCursor(parentScope, coordinates).visit(parentScope.getValue(), cursorReference);
        Cursor insertionScope = cursorReference.get();
        if (insertionScope == null) {
            insertionScope = parentScope;
        }

        String generatedSource = new TemplatePrinter(coordinates).print(pruned, printedTemplate);

        if (templateEventHandler != null) {
            templateEventHandler.beforeParseTemplate(generatedSource);
        }

        parser.reset();
        J.CompilationUnit synthetic = parser.parse(generatedSource).iterator().next();

        //Extract the compiled template tree elements.
        ExtractionContext extractionContext = new ExtractionContext();
        new ExtractTemplatedCode().visit(synthetic, extractionContext);

        final Cursor formatScope = extractionContext.formatCursor != null ? extractionContext.formatCursor :
                insertionScope.dropParentUntil(J.class::isInstance);

        //noinspection unchecked
        return extractionContext.getSnippets().stream()
                .map(snippet -> (J2) new AutoFormatVisitor<String>().visit(snippet, "", formatScope))
                .collect(toList());
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

    public class FindCoordinateCursor extends JavaVisitor<AtomicReference<Cursor>> {
        private final UUID elementId;

        public FindCoordinateCursor(Cursor parentCursor, JavaCoordinates<?> coordinates) {
            this.elementId = coordinates.getTree().getId();
            setCursoringOn();
            setCursor(parentCursor);
        }

        @Override
        public J visit(@Nullable Tree tree, AtomicReference<Cursor> cursorReference) {
            if (tree != null && tree.getId().equals(elementId)) {
                cursorReference.set(getCursor());
            }
            return super.visit(tree, cursorReference);
        }
    }
    /**
     * A java visitor that prunes the original AST down to just the things needed to compile the template code.
     * The typed Cursor represents the insertion point within the original AST.
     */
    private static class TemplateVisitor extends JavaIsoVisitor<Cursor> {

        private final JavaCoordinates<?> coordinates;
        private final Set<String> imports;

        TemplateVisitor(JavaCoordinates<?> coordinates, Set<String> imports) {
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
                    case TYPE_PARAMETER_SUFFIX:
                        c = c.withTypeParameters(Collections.singletonList(
                                new J.TypeParameter(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                                        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY), null)
                        ));
                        insertionScope.putMessage("newCoordinates", c.getTypeParameters().get(0).coordinates().replaceThis());
                        break;
                    case EXTENDS:
                        if (c.getExtends() == null) {
                            c = c.withExtends(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY));
                        }
                        insertionScope.putMessage("newCoordinates", c.getExtends().coordinates().replaceThis());
                        break;
                    case IMPLEMENTS_SUFFIX:
                        c = c.withImplements(Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
                        insertionScope.putMessage("newCoordinates", c.getImplements().get(0).coordinates().replaceThis());
                        break;
                    case BLOCK_END:
                        insertionScope.putMessage("newCoordinates", c.getBody().coordinates().replaceThis());
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
                    case TYPE_PARAMETER_SUFFIX:
                        m = m.withTypeParameters(Collections.singletonList(
                                new J.TypeParameter(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                                        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY), null)
                        ));
                        insertionScope.putMessage("newCoordinates", m.getTypeParameters().get(0).coordinates().replaceThis());
                        break;
                    case METHOD_DECL_PARAMETERS:
                        m = m.withParams(Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)));
                        insertionScope.putMessage("newCoordinates", m.getParams().get(0).coordinates().replaceThis());
                        break;
                    case THROWS:
                        m = m.withThrows(Collections.singletonList(new J.Empty(Tree.randomId(), Space.format(" "), Markers.EMPTY)));
                        insertionScope.putMessage("newCoordinates", m.getThrows().get(0).coordinates().replaceThis());
                        break;
                    case BLOCK_END:
                        if (m.getBody() == null) {
                            m = m.withBody(new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), null, Space.EMPTY));
                        }
                        insertionScope.putMessage("newCoordinates", m.getBody().coordinates().replaceThis());
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
                    insertionScope.putMessage("newCoordinates", m.getArgs().get(0).coordinates().replaceThis());
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
            if (parent != null && parent.getId().equals(coordinates.getTree().getId())
                    && location == coordinates.getSpaceLocation()) {
                getPrinterAcc().append(getMarkedTemplate(template));
                return space;
            } else {
                return super.visitSpace(space, location, template);
            }
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, String template) {
            if (coordinates.getSpaceLocation() == Space.Location.REPLACE && tree !=null && tree.getId().equals(coordinates.getTree().getId())) {
                getPrinterAcc().append(getMarkedTemplate(template));
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

        @Nullable
        private Cursor formatCursor;

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

                while(context.collectedElements.size() > 1 && getCursor().isScopeInPath(context.collectedElements.get(0).element)) {
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
                    context.formatCursor = getCursor();
                    return space;
                } else {
                    context.formatCursor = getCursor().dropParentUntil(J.class::isInstance);
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

    public static class Builder {
        private final String code;
        private final Set<String> imports = new HashSet<>();

        private JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();

        private String parameterMarker = "#{}";

        @Nullable
        private TemplateEventHandler templateEventHandler;

        Builder(String code) {
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

        public Builder templateEventHandler(TemplateEventHandler templateEventHandler) {
            this.templateEventHandler = templateEventHandler;
            return this;
        }

        public JavaTemplate build() {
            return new JavaTemplate(javaParser, code, imports, parameterMarker, templateEventHandler);
        }
    }
}
