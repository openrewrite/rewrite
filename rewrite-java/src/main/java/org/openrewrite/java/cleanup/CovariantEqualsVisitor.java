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

import java.util.Comparator;
import java.util.stream.Stream;

@Incubating(since = "7.0.0")
public class CovariantEqualsVisitor<P> extends JavaIsoVisitor<P> {
    private static final MethodMatcher OBJECT_EQUALS = new MethodMatcher("* equals(java.lang.Object)");

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, p);
        Stream<J.MethodDeclaration> mds = cd.getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast);
        if (mds.noneMatch(m -> OBJECT_EQUALS.matches(m, classDecl)) && cd.getKind() != J.ClassDeclaration.Kind.Type.Interface) {
            cd = (J.ClassDeclaration) new ChangeCovariantEqualsMethodVisitor<>(cd).visit(cd, p, getCursor().getParentOrThrow());
            assert cd != null;
        }
        return cd;
    }

    private static class ChangeCovariantEqualsMethodVisitor<P> extends JavaIsoVisitor<P> {
        private static final AnnotationMatcher OVERRIDE_ANNOTATION = new AnnotationMatcher("@java.lang.Override");
        private static final String EQUALS_BODY_PREFIX_TEMPLATE =
                "if (#{} == this) return true;\n" +
                        "if (#{} == null || getClass() != #{}.getClass()) return false;\n" +
                        "#{} #{} = (#{}) #{};\n";

        private final J.ClassDeclaration enclosingClass;

        public ChangeCovariantEqualsMethodVisitor(J.ClassDeclaration enclosingClass) {
            this.enclosingClass = enclosingClass;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

            /*
             * Looking for "public boolean equals(EnclosingClassType)" as the method signature match.
             * We'll replace it with "public boolean equals(Object)"
             */
            JavaType.FullyQualified type = enclosingClass.getType();
            if (type == null) {
                return m;
            }

            String ecfqn = type.getFullyQualifiedName();
            if (new MethodMatcher(ecfqn + " equals(" + ecfqn + ")").matches(m, enclosingClass) &&
                    m.hasModifier(J.Modifier.Type.Public) &&
                    m.getReturnTypeExpression() != null &&
                    JavaType.Primitive.Boolean.equals(m.getReturnTypeExpression().getType())) {

                if (m.getAllAnnotations().stream().noneMatch(OVERRIDE_ANNOTATION::matches)) {
                    m = m.withTemplate(
                            JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Override").build(),
                            m.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                    );
                }

                /*
                 * Change parameter type to Object, and maybe change input parameter name representing the other object.
                 * This is because we prepend these type-checking replacement statements to the existing "equals(..)" body.
                 * Therefore we don't want to collide with any existing variable names.
                 */
                J.VariableDeclarations.NamedVariable oldParamName = ((J.VariableDeclarations) m.getParameters().iterator().next()).getVariables().iterator().next();
                String paramName = "obj".equals(oldParamName.getSimpleName()) ? "other" : "obj";
                m = m.withTemplate(
                        JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Object #{}").build(),
                        m.getCoordinates().replaceParameters(),
                        paramName);

                /*
                 * We'll prepend this type-check and type-cast to the beginning of the existing
                 * equals(..) method body statements, and let the existing equals(..) method definition continue
                 * with the logic doing what it was doing.
                 */
                JavaTemplate equalsBodySnippet = JavaTemplate.builder(this::getCursor, EQUALS_BODY_PREFIX_TEMPLATE).build();

                assert m.getBody() != null;
                Object[] params = new Object[]{
                        paramName,
                        paramName,
                        paramName,
                        enclosingClass.getSimpleName(),
                        oldParamName.getSimpleName(),
                        enclosingClass.getSimpleName(),
                        paramName
                };

                m = m.withTemplate(
                        equalsBodySnippet,
                        m.getBody().getStatements().get(0).getCoordinates().before(),
                        params);
            }

            return m;
        }
    }
}
