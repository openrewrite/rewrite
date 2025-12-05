/*
 * Copyright 2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class AddInputStreamBulkReadMethod extends Recipe {

    private static final String MARKER_MESSAGE = "Missing bulk read method may cause significant performance degradation";

    @Override
    public String getDisplayName() {
        return "Add bulk read method to `InputStream` implementations";
    }

    @Override
    public String getDescription() {
        return "Adds a `read(byte[], int, int)` method to `InputStream` subclasses that only override the single-byte " +
               "`read()` method. Java's default `InputStream.read(byte[], int, int)` implementation calls the " +
               "single-byte `read()` method in a loop, which can cause severe performance degradation (up to 350x " +
               "slower) for bulk reads. This recipe detects `InputStream` implementations that delegate to another " +
               "stream and adds the missing bulk read method to delegate bulk reads as well.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                // Skip if not extending InputStream
                if (!isInputStreamSubclass(cd)) {
                    return cd;
                }

                // Skip FilterInputStream subclasses (they already delegate bulk reads)
                if (isFilterInputStreamSubclass(cd)) {
                    return cd;
                }

                // Find read() method and check for bulk read
                AnalysisResult result = analyzeClass(cd.getBody().getStatements());
                if (result == null) {
                    return cd;
                }

                if (result.hasBulkRead) {
                    return cd;
                }

                // No delegate found or complex body - add marker for manual implementation
                if (result.delegate == null || result.isComplex) {
                    return SearchResult.found(cd, MARKER_MESSAGE);
                }

                // Simple delegation - add bulk read method
                Statement bulkMethod = createBulkReadMethod(result.delegate, result.hasNullCheck, result.usesIfStyle, cd.getBody());
                if (bulkMethod == null) {
                    return cd;
                }

                final J.MethodDeclaration targetMethod = result.readMethod;
                cd = cd.withBody(cd.getBody().withStatements(
                        ListUtils.flatMap(cd.getBody().getStatements(), stmt -> {
                            if (stmt == targetMethod) {
                                return Arrays.asList(stmt, bulkMethod);
                            }
                            return singletonList(stmt);
                        })
                ));

                return cd;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = super.visitNewClass(newClass, ctx);

                // Must be an anonymous class
                if (nc.getBody() == null) {
                    return nc;
                }

                // Must extend InputStream
                if (!TypeUtils.isAssignableTo("java.io.InputStream", nc.getType())) {
                    return nc;
                }

                // Skip FilterInputStream subclasses
                if (TypeUtils.isAssignableTo("java.io.FilterInputStream", nc.getType())) {
                    return nc;
                }

                // Analyze the class
                AnalysisResult result = analyzeClass(nc.getBody().getStatements());
                if (result == null) {
                    return nc;
                }

                if (result.hasBulkRead) {
                    return nc;
                }

                // No delegate found or complex body - add marker for manual implementation
                if (result.delegate == null || result.isComplex) {
                    return SearchResult.found(nc, MARKER_MESSAGE);
                }

                // Simple delegation - add bulk read method
                Statement bulkMethod = createBulkReadMethod(result.delegate, result.hasNullCheck, result.usesIfStyle, nc.getBody());
                if (bulkMethod == null) {
                    return nc;
                }

                final J.MethodDeclaration targetMethod = result.readMethod;
                nc = nc.withBody(nc.getBody().withStatements(
                        ListUtils.flatMap(nc.getBody().getStatements(), stmt -> {
                            if (stmt == targetMethod) {
                                return Arrays.asList(stmt, bulkMethod);
                            }
                            return singletonList(stmt);
                        })
                ));

                return nc;
            }

            private boolean isInputStreamSubclass(J.ClassDeclaration classDecl) {
                if (classDecl.getExtends() == null) {
                    return false;
                }
                return TypeUtils.isAssignableTo("java.io.InputStream", classDecl.getType());
            }

            private boolean isFilterInputStreamSubclass(J.ClassDeclaration classDecl) {
                return TypeUtils.isAssignableTo("java.io.FilterInputStream", classDecl.getType());
            }

            private @Nullable AnalysisResult analyzeClass(List<Statement> statements) {
                J.MethodDeclaration readMethod = null;
                boolean hasBulkRead = false;

                for (Statement stmt : statements) {
                    if (!(stmt instanceof J.MethodDeclaration)) {
                        continue;
                    }
                    J.MethodDeclaration method = (J.MethodDeclaration) stmt;
                    if (!"read".equals(method.getSimpleName())) {
                        continue;
                    }
                    if (method.getParameters().isEmpty() ||
                        (method.getParameters().size() == 1 && method.getParameters().get(0) instanceof J.Empty)) {
                        readMethod = method;
                    } else if (method.getParameters().size() == 3) {
                        hasBulkRead = true;
                    }
                }

                if (readMethod == null) {
                    return null;
                }

                // Check if body is complex
                boolean isComplex = isComplexBody(readMethod);

                // Find delegate - use simple finder for simple bodies, broader search for complex
                String delegate;
                if (isComplex) {
                    delegate = findAnyDelegate(readMethod);
                } else {
                    delegate = findDelegate(readMethod);
                }
                String nullCheckVar = findNullCheckVariable(readMethod);
                boolean hasNullCheck = nullCheckVar != null && nullCheckVar.equals(delegate);

                // Detect if null check uses if-statement style vs ternary style
                // Check the read() method body, not the class body
                List<Statement> readMethodStatements = readMethod.getBody() != null ?
                        readMethod.getBody().getStatements() : emptyList();
                boolean usesIfStyle = readMethodStatements.size() == 2 && readMethodStatements.get(0) instanceof J.If;

                return new AnalysisResult(readMethod, hasBulkRead, delegate, hasNullCheck, usesIfStyle, isComplex);
            }

            private boolean isComplexBody(J.MethodDeclaration method) {
                if (method.getBody() == null) {
                    return true;
                }

                List<Statement> statements = method.getBody().getStatements();

                // Single statement: return ...
                if (statements.size() == 1) {
                    return !isSimpleReturnStatement(statements.get(0));
                }

                // Two statements: if (x == null) return -1; return x.read();
                if (statements.size() == 2) {
                    return !isNullCheckIfPattern(statements.get(0), statements.get(1));
                }

                // More than two statements is complex
                return true;
            }

            private boolean isSimpleReturnStatement(Statement stmt) {
                if (!(stmt instanceof J.Return)) {
                    return false;
                }

                J.Return ret = (J.Return) stmt;
                Expression expr = ret.getExpression();
                if (expr == null) {
                    return false;
                }

                // Handle ternary: delegate == null ? -1 : delegate.read()
                if (expr instanceof J.Ternary) {
                    J.Ternary ternary = (J.Ternary) expr;
                    // Check condition is simple null check
                    if (!isSimpleNullCheck(ternary.getCondition())) {
                        return false;
                    }
                    // Check true part is a literal (like -1)
                    if (!(ternary.getTruePart() instanceof J.Literal) &&
                        !(ternary.getTruePart() instanceof J.Unary)) {
                        return false;
                    }
                    expr = ternary.getFalsePart();
                }

                return isSimpleReadInvocation(expr);
            }

            private boolean isNullCheckIfPattern(Statement first, Statement second) {
                // First statement should be: if (x == null) return -1;
                if (!(first instanceof J.If)) {
                    return false;
                }
                J.If ifStmt = (J.If) first;

                // Check condition is simple null check
                if (!isSimpleNullCheck(ifStmt.getIfCondition().getTree())) {
                    return false;
                }

                // Check then branch is a simple return with literal
                Statement thenStmt = ifStmt.getThenPart();
                if (thenStmt instanceof J.Block) {
                    List<Statement> thenStatements = ((J.Block) thenStmt).getStatements();
                    if (thenStatements.size() != 1) {
                        return false;
                    }
                    thenStmt = thenStatements.get(0);
                }
                if (!(thenStmt instanceof J.Return)) {
                    return false;
                }
                J.Return thenReturn = (J.Return) thenStmt;
                if (!(thenReturn.getExpression() instanceof J.Literal) &&
                    !(thenReturn.getExpression() instanceof J.Unary)) {
                    return false;
                }

                // Should not have else branch
                if (ifStmt.getElsePart() != null) {
                    return false;
                }

                // Second statement should be: return x.read();
                if (!(second instanceof J.Return)) {
                    return false;
                }
                J.Return ret = (J.Return) second;
                return isSimpleReadInvocation(ret.getExpression());
            }

            private boolean isSimpleReadInvocation(@Nullable Expression expr) {
                if (!(expr instanceof J.MethodInvocation)) {
                    return false;
                }

                J.MethodInvocation mi = (J.MethodInvocation) expr;
                if (!"read".equals(mi.getSimpleName())) {
                    return false;
                }

                // Should have no arguments (single-byte read)
                if (!mi.getArguments().isEmpty() &&
                    !(mi.getArguments().size() == 1 && mi.getArguments().get(0) instanceof J.Empty)) {
                    return false;
                }

                return true;
            }

            private boolean isSimpleNullCheck(Expression condition) {
                if (!(condition instanceof J.Binary)) {
                    return false;
                }
                J.Binary binary = (J.Binary) condition;
                if (binary.getOperator() != J.Binary.Type.Equal) {
                    return false;
                }
                Expression left = binary.getLeft();
                Expression right = binary.getRight();
                boolean leftIsNull = left instanceof J.Literal && ((J.Literal) left).getValue() == null;
                boolean rightIsNull = right instanceof J.Literal && ((J.Literal) right).getValue() == null;
                boolean leftIsIdent = left instanceof J.Identifier;
                boolean rightIsIdent = right instanceof J.Identifier;
                return (leftIsNull && rightIsIdent) || (rightIsNull && leftIsIdent);
            }

            private @Nullable String findDelegate(J.MethodDeclaration method) {
                if (method.getBody() == null) {
                    return null;
                }

                for (Statement stmt : method.getBody().getStatements()) {
                    if (!(stmt instanceof J.Return)) {
                        continue;
                    }
                    J.Return ret = (J.Return) stmt;
                    Expression expr = ret.getExpression();

                    // Handle ternary: delegate == null ? -1 : delegate.read()
                    if (expr instanceof J.Ternary) {
                        expr = ((J.Ternary) expr).getFalsePart();
                    }

                    // Look for method invocation of read()
                    if (!(expr instanceof J.MethodInvocation)) {
                        continue;
                    }
                    J.MethodInvocation mi = (J.MethodInvocation) expr;
                    if (!"read".equals(mi.getSimpleName()) || mi.getSelect() == null) {
                        continue;
                    }

                    // The select must be an InputStream
                    if (!TypeUtils.isAssignableTo("java.io.InputStream", mi.getSelect().getType())) {
                        continue;
                    }

                    // Get the delegate name
                    if (mi.getSelect() instanceof J.Identifier) {
                        return ((J.Identifier) mi.getSelect()).getSimpleName();
                    } else if (mi.getSelect() instanceof J.FieldAccess) {
                        J.FieldAccess fa = (J.FieldAccess) mi.getSelect();
                        return fa.print(getCursor());
                    }
                }
                return null;
            }

            /**
             * Broader search for any delegate.read() call in the method body.
             * Used for complex bodies to determine if a marker should be added.
             * Returns null if multiple different delegates are found (intentional design).
             */
            private @Nullable String findAnyDelegate(J.MethodDeclaration method) {
                if (method.getBody() == null) {
                    return null;
                }

                // Use a visitor to search the entire method body
                final java.util.Set<String> foundDelegates = new java.util.HashSet<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, Integer p) {
                        if ("read".equals(mi.getSimpleName()) &&
                            mi.getSelect() != null &&
                            TypeUtils.isAssignableTo("java.io.InputStream", mi.getSelect().getType())) {
                            if (mi.getSelect() instanceof J.Identifier) {
                                foundDelegates.add(((J.Identifier) mi.getSelect()).getSimpleName());
                            } else if (mi.getSelect() instanceof J.FieldAccess) {
                                foundDelegates.add(((J.FieldAccess) mi.getSelect()).getSimpleName());
                            }
                        }
                        return super.visitMethodInvocation(mi, p);
                    }
                }.visit(method.getBody(), 0);

                // Return null if multiple different delegates found (intentional design)
                // Return the single delegate if only one found
                return foundDelegates.size() == 1 ? foundDelegates.iterator().next() : null;
            }

            private @Nullable String findNullCheckVariable(J.MethodDeclaration method) {
                if (method.getBody() == null) {
                    return null;
                }

                List<Statement> statements = method.getBody().getStatements();

                // Check for if-statement null check pattern: if (x == null) return -1; return x.read();
                if (statements.size() == 2 && statements.get(0) instanceof J.If) {
                    J.If ifStmt = (J.If) statements.get(0);
                    String nullVar = extractNullCheckVariable(ifStmt.getIfCondition().getTree());
                    if (nullVar != null) {
                        return nullVar;
                    }
                }

                // Check for ternary null check pattern: return x == null ? -1 : x.read();
                for (Statement stmt : statements) {
                    if (!(stmt instanceof J.Return)) {
                        continue;
                    }
                    J.Return ret = (J.Return) stmt;
                    Expression expr = ret.getExpression();

                    if (!(expr instanceof J.Ternary)) {
                        continue;
                    }
                    J.Ternary ternary = (J.Ternary) expr;
                    String nullVar = extractNullCheckVariable(ternary.getCondition());
                    if (nullVar != null) {
                        return nullVar;
                    }
                }
                return null;
            }

            private @Nullable String extractNullCheckVariable(Expression condition) {
                if (!(condition instanceof J.Binary)) {
                    return null;
                }
                J.Binary binary = (J.Binary) condition;
                if (binary.getOperator() != J.Binary.Type.Equal) {
                    return null;
                }

                Expression left = binary.getLeft();
                Expression right = binary.getRight();

                if (right instanceof J.Literal && ((J.Literal) right).getValue() == null) {
                    if (left instanceof J.Identifier) {
                        return ((J.Identifier) left).getSimpleName();
                    }
                }
                if (left instanceof J.Literal && ((J.Literal) left).getValue() == null) {
                    if (right instanceof J.Identifier) {
                        return ((J.Identifier) right).getSimpleName();
                    }
                }
                return null;
            }

            private @Nullable Statement createBulkReadMethod(String delegate, boolean hasNullCheck, boolean usesIfStyle, J.Block body) {
                String bulkReadMethod;
                if (hasNullCheck) {
                    if (usesIfStyle) {
                        bulkReadMethod = String.format(
                                "@Override\n" +
                                "public int read(byte[] b, int off, int len) throws IOException {\n" +
                                "    if (%s == null) {\n" +
                                "        return -1;\n" +
                                "    }\n" +
                                "    return %s.read(b, off, len);\n" +
                                "}",
                                delegate, delegate);
                    } else {
                        bulkReadMethod = String.format(
                                "@Override\n" +
                                "public int read(byte[] b, int off, int len) throws IOException {\n" +
                                "    return %s == null ? -1 : %s.read(b, off, len);\n" +
                                "}",
                                delegate, delegate);
                    }
                } else {
                    bulkReadMethod = String.format(
                            "@Override\n" +
                            "public int read(byte[] b, int off, int len) throws IOException {\n" +
                            "    return %s.read(b, off, len);\n" +
                            "}",
                            delegate);
                }

                JavaTemplate template = JavaTemplate.builder(bulkReadMethod)
                        .contextSensitive()
                        .imports("java.io.IOException")
                        .build();

                J.Block newBody = body.withStatements(emptyList());
                J.Block withNewMethod = template.apply(
                        new Cursor(getCursor(), newBody),
                        newBody.getCoordinates().lastStatement());
                Statement bulkMethod = withNewMethod.getStatements().get(0);

                // Add blank line before the new method
                String existingWhitespace = bulkMethod.getPrefix().getWhitespace();
                return bulkMethod.withPrefix(Space.format("\n" + existingWhitespace));
            }
        };
    }

    private static class AnalysisResult {
        final J.MethodDeclaration readMethod;
        final boolean hasBulkRead;
        final @Nullable String delegate;
        final boolean hasNullCheck;
        final boolean usesIfStyle;
        final boolean isComplex;

        AnalysisResult(J.MethodDeclaration readMethod, boolean hasBulkRead, @Nullable String delegate,
                       boolean hasNullCheck, boolean usesIfStyle, boolean isComplex) {
            this.readMethod = readMethod;
            this.hasBulkRead = hasBulkRead;
            this.delegate = delegate;
            this.hasNullCheck = hasNullCheck;
            this.usesIfStyle = usesIfStyle;
            this.isComplex = isComplex;
        }
    }
}
