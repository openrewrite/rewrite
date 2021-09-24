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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;
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
        return "Replace List, Map, and Set double brace initialization with an initialization block.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1171");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("java.util.Map"));
                doAfterVisit(new UsesType<>("java.util.List"));
                doAfterVisit(new UsesType<>("java.util.Set"));
                return cu;
            }
        };
    }

    @Override
    protected NoDoubleBraceInitializationVisitor<ExecutionContext> getVisitor() {
        return new NoDoubleBraceInitializationVisitor<>();
    }

    private static class NoDoubleBraceInitializationVisitor<ExecutionContext> extends JavaIsoVisitor<ExecutionContext> {

        private boolean isDoubleBraceInitialization(J.NewClass nc) {
            if (nc.getBody() != null && !nc.getBody().getStatements().isEmpty()
                    && nc.getBody().getStatements().get(0) instanceof J.Block) {
                return TypeUtils.isAssignableTo(MAP_TYPE, nc.getType())
                        || TypeUtils.isAssignableTo(LIST_TYPE, nc.getType())
                        || TypeUtils.isAssignableTo(SET_TYPE, nc.getType());
            }
            return false;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            J.NewClass nc = super.visitNewClass(newClass, executionContext);
            if (isDoubleBraceInitialization(newClass)) {
                Cursor parentBlockCursor = getCursor().dropParentUntil(J.Block.class::isInstance);
                J.VariableDeclarations.NamedVariable var = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
                //noinspection ConstantConditions
                List<Statement> initStatements = ((J.Block) nc.getBody().getStatements().get(0)).getStatements();

                if (var != null && parentBlockCursor.getParent() != null) {
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
                            initStatements = addSelectToInitStatements(initStatements, var.getName());
                            initStatements.add(0, new J.Assignment(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, var.getName().withId(UUID.randomUUID()), JLeftPadded.build(nc), fq));
                            parentBlockCursor.computeMessageIfAbsent("INIT_STATEMENTS", v -> new HashMap<Object, List<Statement>>()).put(varDeclsCursor.getValue(), initStatements);
                        }
                    } else if (parentBlockCursor.getParent().getValue() instanceof J.MethodDeclaration) {
                        initStatements = addSelectToInitStatements(initStatements, var.getName());
                        Cursor varDeclsCursor = getCursor().dropParentUntil(parent -> parent instanceof J.VariableDeclarations);
                        parentBlockCursor.computeMessageIfAbsent("METHOD_DECL_STATEMENTS", v -> new HashMap<Object, List<Statement>>()).put(varDeclsCursor.getValue(), initStatements);
                        nc = nc.withBody(null);
                    }
                }

            }
            return nc;
        }

        private List<Statement> addSelectToInitStatements(List<Statement> statements, J.Identifier identifier) {
            return statements.stream().map(stmt -> {
                if (stmt instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) stmt;
                    if (mi.getSelect() == null && mi.getType() != null && TypeUtils.isAssignableTo(identifier.getType(), mi.getType().getDeclaringType())) {
                        return mi.withSelect(identifier);
                    }
                }
                return stmt;
            }).collect(Collectors.toList());
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
            Map<Object, List<Statement>> initStatements = getCursor().pollMessage("INIT_STATEMENTS");
            Map<Object, List<Statement>> methodInitStatemnts = getCursor().pollMessage("METHOD_DECL_STATEMENTS");

            if (initStatements != null) {
                for (Map.Entry<Object, List<Statement>> objectListEntry : initStatements.entrySet()) {
                    List<Statement> statements = bl.getStatements();
                    for (int i = 0; i < statements.size(); i++) {
                        Statement statement = statements.get(i);
                        JRightPadded<Boolean> padding;
                        if (statement instanceof J.VariableDeclarations && J.Modifier.hasModifier(((J.VariableDeclarations) statement).getModifiers(), J.Modifier.Type.Static)) {
                            padding = JRightPadded.build(true).withAfter(Space.format(" "));
                        } else {
                            padding = JRightPadded.build(false);
                        }
                        if (statement.equals(objectListEntry.getKey())) {
                            J.Block initBlock = new J.Block(
                                    Tree.randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    padding,
                                    objectListEntry.getValue().stream().map(JRightPadded::build).collect(Collectors.toList()),
                                    Space.EMPTY
                            );
                            //noinspection ConstantConditions
                            bl = maybeAutoFormat(bl, bl.withStatements(ListUtils.insertAll(bl.getStatements(), i + 1, Collections.singletonList(initBlock))),
                                    initBlock, ctx, getCursor().getParent(2));
                        }
                    }
                }
            } else if (methodInitStatemnts != null) {
                for (Map.Entry<Object, List<Statement>> objectListEntry : methodInitStatemnts.entrySet()) {
                    List<Statement> statements = bl.getStatements();
                    for (int i = 0; i < statements.size(); i++) {
                        Statement statement = statements.get(i);
                        if (statement.equals(objectListEntry.getKey())) {
                            //noinspection ConstantConditions
                            bl = maybeAutoFormat(bl, bl.withStatements(ListUtils.insertAll(bl.getStatements(), i + 1, objectListEntry.getValue())),
                                    objectListEntry.getValue().get(objectListEntry.getValue().size() - 1), ctx, getCursor().getParent());
                        }
                    }
                }

            }
            return bl;
        }
    }
}
