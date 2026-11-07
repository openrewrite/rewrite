/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.jspecify.annotations.Nullable;
import org.openrewrite.groovy.marker.JenkinsLibraries;

import java.util.ArrayList;
import java.util.List;

/// Reads library declarations from the Groovy AST rather than the LST, because the LST keeps an
/// annotation on an `import` as part of that import's prefix.
class JenkinsLibraryCollector extends CodeVisitorSupport {
    private final List<JenkinsLibraries.Library> libraries = new ArrayList<>();

    static List<JenkinsLibraries.Library> collect(ModuleNode ast) {
        JenkinsLibraryCollector collector = new JenkinsLibraryCollector();
        List<ImportNode> imports = new ArrayList<>(ast.getImports());
        imports.addAll(ast.getStarImports());
        imports.addAll(ast.getStaticImports().values());
        imports.addAll(ast.getStaticStarImports().values());
        for (ImportNode anImport : imports) {
            collector.visitAnnotations(anImport);
        }
        ast.getStatementBlock().visit(collector);
        for (MethodNode method : ast.getMethods()) {
            Statement code = method.getCode();
            if (code != null) {
                code.visit(collector);
            }
        }
        return collector.libraries;
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
        visitAnnotations(expression);
        super.visitDeclarationExpression(expression);
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
        if (call.isImplicitThis() && "library".equals(call.getMethodAsString()) &&
            call.getArguments() instanceof TupleExpression) {
            for (Expression argument : ((TupleExpression) call.getArguments()).getExpressions()) {
                if (argument instanceof MapExpression) {
                    for (MapEntryExpression entry : ((MapExpression) argument).getMapEntryExpressions()) {
                        if ("identifier".equals(constantString(entry.getKeyExpression()))) {
                            addLibrary(entry.getValueExpression());
                        }
                    }
                } else {
                    addLibrary(argument);
                }
            }
        }
        super.visitMethodCallExpression(call);
    }

    private void visitAnnotations(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            String name = annotation.getClassNode().getName();
            if (!"Library".equals(name) && !"org.jenkinsci.plugins.workflow.libs.Library".equals(name)) {
                continue;
            }
            Expression value = annotation.getMember("value");
            if (value instanceof ListExpression) {
                for (Expression element : ((ListExpression) value).getExpressions()) {
                    addLibrary(element);
                }
            } else {
                addLibrary(value);
            }
        }
    }

    private void addLibrary(@Nullable Expression expression) {
        String identifier = constantString(expression);
        if (identifier == null || identifier.isEmpty()) {
            return;
        }
        int at = identifier.indexOf('@');
        if (at == -1) {
            libraries.add(new JenkinsLibraries.Library(identifier, null));
        } else if (at > 0) {
            String version = identifier.substring(at + 1);
            libraries.add(new JenkinsLibraries.Library(identifier.substring(0, at), version.isEmpty() ? null : version));
        }
    }

    private static @Nullable String constantString(@Nullable Expression expression) {
        if (expression instanceof ConstantExpression && ((ConstantExpression) expression).getValue() instanceof String) {
            return (String) ((ConstantExpression) expression).getValue();
        }
        return null;
    }
}
