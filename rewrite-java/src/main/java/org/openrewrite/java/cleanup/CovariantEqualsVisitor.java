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
package org.openrewrite.java.cleanup;

import org.openrewrite.Incubating;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Incubating(since = "7.0.0")
public class CovariantEqualsVisitor<P> extends JavaIsoVisitor<P> {

    private static final MethodMatcher OBJECT_EQUALS_SIGNATURE = new MethodMatcher("* equals(java.lang.Object)");

    public CovariantEqualsVisitor() {
        setCursoringOn();
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, p);
        Stream<J.MethodDeclaration> mds = cd.getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast);
        if (mds.noneMatch(m -> OBJECT_EQUALS_SIGNATURE.matches(m, classDecl))) {
            cd = (J.ClassDeclaration) new ChangeCovariantEqualsMethodVisitor<>(cd).visit(cd, p, getCursor());
        }
        return cd;
    }

    private static class ChangeCovariantEqualsMethodVisitor<P> extends JavaIsoVisitor<P> {

        private static final AnnotationMatcher OVERRIDE_ANNOTATION_SIGNATURE = new AnnotationMatcher("@java.lang.Override");
        private static final String EQUALS_BODY_PREFIX_TEMPLATE = "{\n" +
                "if (#{} == this) return true;\n" +
                "if (#{} == null || getClass() != #{}.getClass()) return false;\n" +
                "#{} #{} = (#{}) #{};\n" +
                "#{}\n" +
                "}";

        private final J.ClassDeclaration enclosingClass;

        public ChangeCovariantEqualsMethodVisitor(J.ClassDeclaration enclosingClass) {
            this.enclosingClass = enclosingClass;
            setCursoringOn();
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

            /**
             * Looking for "public boolean equals(EnclosingClassType)" as the method signature match.
             * We'll replace it with "public boolean equals(Object)"
             */
            String ecfqn = enclosingClass.getType().getFullyQualifiedName();
            if (new MethodMatcher(String.format("%s equals(%s)", ecfqn, ecfqn)).matches(m, enclosingClass) &&
                    m.hasModifier(J.Modifier.Type.Public) &&
                    m.getReturnTypeExpression() != null &&
                    JavaType.Primitive.Boolean.equals(m.getReturnTypeExpression().getType())) {

                if (m.getAnnotations().stream().noneMatch(OVERRIDE_ANNOTATION_SIGNATURE::matches)) {
                    m = maybeAutoFormat(m,
                            m.withTemplate(
                                    template("@Override").build(),
                                    m.getAnnotations().isEmpty() ? m.getCoordinates().replaceAnnotations() : m.getAnnotations().get(0).getCoordinates().before()
                            ), p, getCursor().getParentOrThrow());
                }

                /**
                 * Change parameter type to Object, and maybe change input parameter name representing the other object.
                 * This is because we prepend these type-checking replacement statements to the existing "equals(..)" body.
                 * Therefore we don't want to collide with any existing variable names.
                 */
                J.VariableDeclarations.NamedVariable oldParamName = ((J.VariableDeclarations) m.getParameters().iterator().next()).getVariables().iterator().next();
                String paramName = "obj".equals(oldParamName.getSimpleName()) ? "other" : "obj";
                m = maybeAutoFormat(m,
                        m.withTemplate(
                                template("(Object #{})").build(),
                                m.getCoordinates().replaceParameters(),
                                paramName
                        ), p, getCursor().getParentOrThrow());

                /**
                 * We'll prepend this type-check and type-cast to the beginning of the existing
                 * equals(..) method body statements, and let the existing equals(..) method definition continue
                 * with the logic doing what it was doing.
                 */
                JavaTemplate equalsBodySnippet = template(EQUALS_BODY_PREFIX_TEMPLATE).build();
                Object[] params = new Object[]{
                        paramName,
                        paramName,
                        paramName,
                        enclosingClass.getSimpleName(),
                        oldParamName.printTrimmed(),
                        enclosingClass.getSimpleName(),
                        paramName,
                        m.getBody().getStatements().stream().map(J::printTrimmed).collect(Collectors.joining(";"))
                };

                m = maybeAutoFormat(m,
                        m.withTemplate(
                                equalsBodySnippet,
                                m.getCoordinates().replaceBody(),
                                params
                        ), p, getCursor().getParentOrThrow());
            }

            return m;
        }
    }

}


