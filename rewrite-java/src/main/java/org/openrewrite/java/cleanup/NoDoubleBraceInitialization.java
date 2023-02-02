/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class NoDoubleBraceInitialization extends Recipe {
    private static final JavaType MAP_TYPE = JavaType.buildType("java.util.Map");
    private static final JavaType LIST_TYPE = JavaType.buildType("java.util.List");
    private static final JavaType SET_TYPE = JavaType.buildType("java.util.Set");

    @Override
    public String getDisplayName() {
        return "No double brace initialization";
    }

    @Override
    public String getDescription() {
        return "Replace `List`, `Map`, and `Set` double brace initialization with an initialization block.";
    }

    @Override
    public Set<String> getTags() {
        return new LinkedHashSet<>(Arrays.asList("RSPEC-1171", "RSPEC-3599"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("java.util.Map"));
                doAfterVisit(new UsesType<>("java.util.List"));
                doAfterVisit(new UsesType<>("java.util.Set"));
                return cu;
            }
        };
    }

    @Override
    public NoDoubleBraceInitializationVisitor getVisitor() {
        return new NoDoubleBraceInitializationVisitor();
    }

    private static class NoDoubleBraceInitializationVisitor extends JavaIsoVisitor<ExecutionContext> {

        private boolean isSupportedDoubleBraceInitialization(J.NewClass nc) {
            if (getCursor().getParent() == null
                    || getCursor().getParent().firstEnclosing(J.class) instanceof J.MethodInvocation
                    || getCursor().getParent().firstEnclosing(J.class) instanceof J.NewClass) {
                return false;
            }
            if (nc.getBody() != null && !nc.getBody().getStatements().isEmpty()
                    && nc.getBody().getStatements().size() == 1
                    && nc.getBody().getStatements().get(0) instanceof J.Block
                    && getCursor().getParent(3) != null) {
                return TypeUtils.isAssignableTo(MAP_TYPE, nc.getType())
                        || TypeUtils.isAssignableTo(LIST_TYPE, nc.getType())
                        || TypeUtils.isAssignableTo(SET_TYPE, nc.getType());
            }
            return false;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            J.NewClass nc = super.visitNewClass(newClass, executionContext);
            if (isSupportedDoubleBraceInitialization(newClass)) {
                Cursor parentBlockCursor = getCursor().dropParentUntil(J.Block.class::isInstance);
                J.VariableDeclarations.NamedVariable var = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
                //noinspection ConstantConditions
                J.Block secondBlock = (J.Block) nc.getBody().getStatements().get(0);
                List<Statement> initStatements = secondBlock.getStatements();

                boolean maybeMistakenlyMissedAddingElement = !initStatements.isEmpty()
                        && initStatements.stream().allMatch(statement -> statement instanceof J.NewClass);

                if (maybeMistakenlyMissedAddingElement) {
                    JavaType newClassType = nc.getType();
                    String addToCollectionMethod = TypeUtils.isAssignableTo(MAP_TYPE, newClassType) ? "put()" : "add()";
                    return nc.withBody(AddWarningMessage.addWarningComment(nc.getBody(), addToCollectionMethod));
                }

                // If not any method invocation (like add(), push(), etc) happened in the double brace to initialize
                // the content of the collection, it means the intention of the code in the double brace is uncertain
                // or maybe a custom code bug (like issue: https://github.com/openrewrite/rewrite/issues/2674),
                // we don't want to rewrite code for this case to avoid introducing other warnings.
                boolean hasMethodInvocationInDoubleBrace = FindMethodInvocationInDoubleBrace.find(secondBlock);

                if (hasMethodInvocationInDoubleBrace && var != null && parentBlockCursor.getParent() != null) {
                    if (parentBlockCursor.getParent().getValue() instanceof J.ClassDeclaration) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(nc.getType());
                        if (fq != null && fq.getSupertype() != null) {
                            Cursor varDeclsCursor = getCursor().dropParentUntil(parent -> parent instanceof J.VariableDeclarations);
                            Cursor namedVarCursor = getCursor().dropParentUntil(J.VariableDeclarations.NamedVariable.class::isInstance);
                            namedVarCursor.putMessage("DROP_INITIALIZER", Boolean.TRUE);

                            fq = fq.getSupertype();
                            String newInitializer = " new " + fq.getClassName() + "<>();";
                            JavaTemplate template = JavaTemplate.builder(this::getCursor, newInitializer).imports(fq.getFullyQualifiedName()).build();
                            nc = nc.withTemplate(template, nc.getCoordinates().replace());
                            initStatements = addSelectToInitStatements(initStatements, var.getName(), executionContext);
                            initStatements.add(0, new J.Assignment(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, var.getName().withId(UUID.randomUUID()), JLeftPadded.build(nc), fq));
                            parentBlockCursor.computeMessageIfAbsent("INIT_STATEMENTS", v -> new HashMap<Statement, List<Statement>>()).put(varDeclsCursor.getValue(), initStatements);
                        }
                    } else if (parentBlockCursor.getParent().getValue() instanceof J.MethodDeclaration) {
                        initStatements = addSelectToInitStatements(initStatements, var.getName(), executionContext);
                        Cursor varDeclsCursor = getCursor().dropParentUntil(parent -> parent instanceof J.VariableDeclarations);
                        parentBlockCursor.computeMessageIfAbsent("METHOD_DECL_STATEMENTS", v -> new HashMap<Statement, List<Statement>>()).put(varDeclsCursor.getValue(), initStatements);
                        nc = nc.withBody(null);
                    }
                }

            }
            return nc;
        }

        private List<Statement> addSelectToInitStatements(List<Statement> statements, J.Identifier identifier, ExecutionContext ctx) {
            AddSelectVisitor selectVisitor = new AddSelectVisitor(identifier);
            List<Statement> statementList = new ArrayList<>();
            for (Statement statement : statements) {
                statementList.add((Statement) selectVisitor.visit(statement, ctx));
            }
            return statementList;
        }

        private static class AddSelectVisitor extends JavaIsoVisitor<ExecutionContext> {
            private final J.Identifier identifier;

            public AddSelectVisitor(J.Identifier identifier) {
                this.identifier = identifier;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (mi.getMethodType() != null && identifier.getFieldType() != null && mi.getSelect() == null
                        || (mi.getSelect() instanceof J.Identifier && "this".equals(((J.Identifier) mi.getSelect()).getSimpleName()))) {
                    JavaType rawFieldType = identifier.getFieldType().getType();
                    rawFieldType = rawFieldType instanceof JavaType.Parameterized ? ((JavaType.Parameterized) rawFieldType).getType() : rawFieldType;
                    JavaType rawMethodDeclaringType = mi.getMethodType().getDeclaringType();
                    rawMethodDeclaringType = rawMethodDeclaringType instanceof JavaType.Parameterized ? ((JavaType.Parameterized) rawMethodDeclaringType).getType() : rawMethodDeclaringType;

                    if (TypeUtils.isAssignableTo(rawFieldType, rawMethodDeclaringType)) {
                        return mi.withSelect(identifier);
                    }
                }
                return mi;
            }
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
            J.VariableDeclarations.NamedVariable var = super.visitVariable(variable, executionContext);
            if (getCursor().pollMessage("DROP_INITIALIZER") != null) {
                var = var.withInitializer(null);
            }
            return var;
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
            J.Block bl = super.visitBlock(block, ctx);
            Map<Statement, List<Statement>> initStatements = getCursor().pollMessage("INIT_STATEMENTS");
            Map<Statement, List<Statement>> methodInitStatements = getCursor().pollMessage("METHOD_DECL_STATEMENTS");

            if (initStatements != null) {
                for (Map.Entry<Statement, List<Statement>> objectListEntry : initStatements.entrySet()) {
                    Object statement = objectListEntry.getKey();
                    int statementIndex = bl.getStatements().indexOf(statement);
                    if (statementIndex > -1) {
                        JRightPadded<Boolean> isStatic;
                        if (objectListEntry.getKey() instanceof J.VariableDeclarations && J.Modifier.hasModifier(((J.VariableDeclarations) statement).getModifiers(), J.Modifier.Type.Static)) {
                            isStatic = JRightPadded.build(true).withAfter(Space.format(" "));
                        } else {
                            isStatic = JRightPadded.build(false);
                        }
                        J.Block initBlock = new J.Block(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                isStatic,
                                objectListEntry.getValue().stream().map(JRightPadded::build).collect(Collectors.toList()),
                                Space.EMPTY
                        );
                        //noinspection ConstantConditions
                        bl = maybeAutoFormat(bl, bl.withStatements(ListUtils.insertAll(bl.getStatements(), statementIndex + 1, Collections.singletonList(initBlock))),
                                initBlock, ctx, getCursor().getParent(2));
                    }
                }
            } else if (methodInitStatements != null) {
                for (Map.Entry<Statement, List<Statement>> objectListEntry : methodInitStatements.entrySet()) {
                    int statementIndex = bl.getStatements().indexOf(objectListEntry.getKey());
                    if (statementIndex > -1) {
                        //noinspection ConstantConditions
                        bl = maybeAutoFormat(bl, bl.withStatements(ListUtils.insertAll(bl.getStatements(), statementIndex + 1, objectListEntry.getValue())),
                                objectListEntry.getValue().get(objectListEntry.getValue().size() - 1), ctx, getCursor().getParent());
                    }
                }
            }
            return bl;
        }
    }

    private static class AddWarningMessage extends JavaIsoVisitor<String> {
        static <T extends J> T addWarningComment(T nc, String methodName) {
            //noinspection unchecked
            return (T) new AddWarningMessage().visitNonNull(nc, methodName);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, String methodName) {
            String comment = "Did you mean to invoke " + methodName + " method to the collection?";
            return SearchResult.found(newClass, comment);
        }
    }

    private static class FindMethodInvocationInDoubleBrace extends JavaIsoVisitor<AtomicBoolean> {
        /**
         * Find whether any collection content initialization method(e.g add() or put()) is invoked in the double brace.
         *
         * @param j The subtree to search, supposed to be the 2nd brace (J.Block)
         * @return true if any method invocation found in the double brace, otherwise false.
         */
        static boolean find(J j) {
            return new FindMethodInvocationInDoubleBrace()
                    .reduce(j, new AtomicBoolean()).get();
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
            if (atomicBoolean.get() || method.getMethodType() == null) {
                return method;
            }
            JavaType.FullyQualified declaring = method.getMethodType().getDeclaringType();
            if (TypeUtils.isAssignableTo(MAP_TYPE, declaring) || TypeUtils.isAssignableTo(LIST_TYPE, declaring) || TypeUtils.isAssignableTo(SET_TYPE, declaring)) {
                atomicBoolean.set(true);
                return method;
            }

            return super.visitMethodInvocation(method, atomicBoolean);
        }
    }
}
