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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ObjectEqualsChecksArgumentType extends Recipe {
    @Override
    public String getDisplayName() {
        return "`Object.equals` methods validate argument type";
    }

    @Override
    public String getDescription() {
        return "Adds type validation to `Object.equals` methods not validating argument types.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2097");
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("java.lang.Object equals(..)");
    }

    @Override
    protected ObjectEqualsChecksArgumentTypeVisitor getVisitor() {
        return new ObjectEqualsChecksArgumentTypeVisitor();
    }

    private static class ObjectEqualsChecksArgumentTypeVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher objectsEqualsMethodMatcher = new MethodMatcher("java.lang.Object equals(java.lang.Object)");
        private static final MethodMatcher getClassMethodMatcher = new MethodMatcher("Object getClass()");

        @Override
        public J.Binary visitBinary(J.Binary binary, ExecutionContext executionContext) {
            J.Binary bn = super.visitBinary(binary, executionContext);
            J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
            if (md != null && JavaType.Primitive.Boolean == bn.getType() && objectsEqualsMethodMatcher.matches(md.getType())
                && getClassMethodMatcher.matches(bn.getRight().getType()) && getClassMethodMatcher.matches(bn.getLeft().getType())) {
                getCursor().dropParentUntil(md::equals).putMessage("FOUND_CLASS_EQUALS", Boolean.TRUE);
            }
            return bn;
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext executionContext) {
            J.InstanceOf iof = super.visitInstanceOf(instanceOf, executionContext);
            J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
            J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (md != null && cd != null) {
                J.VariableDeclarations methodDeclParam = (J.VariableDeclarations)md.getParameters().get(0);
                J.Identifier iofIdent = (J.Identifier)iof.getExpression();
                if (TypeUtils.isOfType(iofIdent.getType(), methodDeclParam.getType())) {
                    getCursor().dropParentUntil(md::equals).putMessage("FOUND_CLASS_EQUALS", Boolean.TRUE);
                }
            }

            return iof;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
            if (md.getBody() != null && getCursor().pollMessage("FOUND_CLASS_EQUALS") == null && objectsEqualsMethodMatcher.matches(md.getType())) {
                String paramNam  = ((J.VariableDeclarations) md.getParameters().get(0)).getVariables().get(0).getSimpleName();
                List<Statement> statements = md.getBody().getStatements();
                md = md.withTemplate(
                        JavaTemplate.builder(this::getCursor,"if (this.getClass() != " + paramNam + ".getClass()) {return false;}").build(),
                        md.getBody().getCoordinates().lastStatement());
                assert md.getBody() != null;
                statements.add(statements.size() - 1, md.getBody().getStatements().get(md.getBody().getStatements().size() - 1));
                md = md.withBody(md.getBody().withStatements(statements));
            }
            return md;
        }
    }
}
