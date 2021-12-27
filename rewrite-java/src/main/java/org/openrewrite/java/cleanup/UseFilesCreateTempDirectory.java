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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.*;

public class UseFilesCreateTempDirectory extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use Files#createTempDirectory";
    }

    @Override
    public String getDescription() {
        return "Use `Files#createTempDirectory` when the sequence `File#createTempFile(..)`->`File#delete()`->`File#mkdir()` is used for creating a temp directory.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-5445");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("java.io.File createTempFile(..)");
    }

    @Override
    protected UsesFilesCreateTempDirVisitor getVisitor() {
        return new UsesFilesCreateTempDirVisitor();
    }

    private static class UsesFilesCreateTempDirVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher CREATE_TEMP_FILE_MATCHER = new MethodMatcher("java.io.File createTempFile(..)");
        private static final MethodMatcher DELETE_MATCHER = new MethodMatcher("java.io.File delete()");
        private static final MethodMatcher MKDIR_MATCHER = new MethodMatcher("java.io.File mkdir()");

        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
            Optional<JavaVersion> javaVersion = cu.getMarkers().findFirst(JavaVersion.class);
            if (javaVersion.isPresent() && javaVersion.get().getMajorVersion() < 7) {
                return cu;
            }
            return super.visitJavaSourceFile(cu, executionContext);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if (CREATE_TEMP_FILE_MATCHER.matches(mi)) {
                J.Block block = getCursor().firstEnclosing(J.Block.class);
                if (block != null) {
                    J createFileStatement = null;
                    J.Assignment assignment = getCursor().firstEnclosing(J.Assignment.class);
                    if (assignment != null && assignment.getVariable() instanceof J.Identifier) {
                        createFileStatement = assignment;
                    }
                    if (createFileStatement == null) {
                        createFileStatement = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
                    }
                    if (createFileStatement != null) {
                        getCursor().dropParentUntil(J.Block.class::isInstance)
                                .computeMessageIfAbsent("CREATE_FILE_STATEMENT", v -> new ArrayList<J>()).add(createFileStatement);
                    }
                }
            }
            return mi;
        }

        @Override
        public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
            J.Block bl = super.visitBlock(block, executionContext);
            List<J> createFileStatements = getCursor().pollMessage("CREATE_FILE_STATEMENT");
            if (createFileStatements != null) {
                for (J createFileStatement : createFileStatements) {
                    List<Statement> statements = bl.getStatements();
                    int statementIndex = -1;
                    Statement createTempDirectoryStatement = null;
                    for (int i = 0; i < statements.size() - 2; i++) {
                        Statement stmt = statements.get(i);
                        J.Identifier createFileIdentifier = getIdent(createFileStatement);
                        if (createFileIdentifier != null && isMatchingCreateFileStatement(createFileStatement, stmt)
                                && isMethodForIdent(createFileIdentifier, DELETE_MATCHER, statements.get(i + 1))
                                && isMethodForIdent(createFileIdentifier, MKDIR_MATCHER, statements.get(i + 2))) {
                            createTempDirectoryStatement = toCreateTempDirectoryStatement(stmt);
                            statementIndex = i;
                            break;
                        }
                    }
                    if (createTempDirectoryStatement != null) {
                        List<Statement> updatedStatements = new ArrayList<>(statements);
                        updatedStatements.remove(statementIndex);
                        updatedStatements.remove(statementIndex);
                        updatedStatements.remove(statementIndex);
                        updatedStatements.add(statementIndex, createTempDirectoryStatement);
                        bl = bl.withStatements(updatedStatements);
                        maybeAddImport("java.nio.file.Files");
                    }
                }
            }
            return bl;
        }

        @Nullable
        private Statement toCreateTempDirectoryStatement(Statement statement) {
            StringBuilder templateString = new StringBuilder();
            if (statement instanceof J.Assignment) {
                J.Identifier ident = getIdent(statement);
                if (ident != null) {
                    templateString.append(ident.getSimpleName());
                }
            } else if (statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations varD = (J.VariableDeclarations) statement;
                templateString.append("File ").append(varD.getVariables().get(0).getName().getSimpleName());
            }
            if (templateString.length() > 0) {
                Object[] args = getArgs(statement);
                templateString.append(" = Files.createTempDirectory(");
                if (args.length == 1) {
                    templateString.append("#{any()}");
                } else if (args.length == 2) {
                    templateString.append("#{any(java.io.File)}.toPath(), #{any()}");
                }
                templateString.append(").toFile()");
                JavaTemplate template = JavaTemplate.builder(this::getCursor, templateString.toString()).imports("java.nio.file.Files").build();
                return statement.withTemplate(template, statement.getCoordinates().replace(), args);
            }
            return null;
        }

        private boolean isMatchingCreateFileStatement(J createFileStatement, Statement statement) {
            if (createFileStatement.equals(statement)) {
                return true;
            }
            if (createFileStatement instanceof J.VariableDeclarations.NamedVariable && statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations varDecls = (J.VariableDeclarations) statement;
                for (J.VariableDeclarations.NamedVariable variable : varDecls.getVariables()) {
                    if (variable.equals(createFileStatement)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isMethodForIdent(J.Identifier ident, MethodMatcher methodMatcher, Statement statement) {
            if (statement instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) statement;
                if (mi.getSelect() instanceof J.Identifier && methodMatcher.matches(mi)) {
                    J.Identifier sel = (J.Identifier) mi.getSelect();
                    return ident.getSimpleName().equals(sel.getSimpleName())
                            && TypeUtils.isOfClassType(ident.getType(), "java.io.File");
                }
            }
            return false;
        }

        @Nullable
        private J.Identifier getIdent(J createFileStatement) {
            if (createFileStatement instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) createFileStatement;
                return (J.Identifier) assignment.getVariable();
            } else if (createFileStatement instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable var = (J.VariableDeclarations.NamedVariable) createFileStatement;
                return var.getName();
            }
            return null;
        }

        @Nullable
        private Object[] getArgs(J createFileStatement) {
            List<Expression> args = null;
            if (createFileStatement instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) createFileStatement;
                args = ((J.MethodInvocation) assignment.getAssignment()).getArguments();
            } else if (createFileStatement instanceof J.VariableDeclarations) {
                J.VariableDeclarations var = (J.VariableDeclarations) createFileStatement;
                J.MethodInvocation initializer = (J.MethodInvocation) var.getVariables().get(0).getInitializer();
                if (initializer != null) {
                    args = initializer.getArguments();
                }
            }
            if (args != null) {
                return args.size() == 3 ? new Object[]{args.get(2), args.get(0)} : new Object[]{args.get(0)};
            }
            return new Object[0];
        }
    }
}
