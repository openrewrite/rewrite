/*
 * Copyright 2022 the original author or authors.
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
package generate;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

@RequiredArgsConstructor
public class WriteModel extends Recipe {
    final List<J.ClassDeclaration> modelClasses;

    @Override
    public String getDisplayName() {
        return "Write the AST model";
    }

    @Override
    public String getDescription() {
        return "Expand the model into an AST with Lombok annotations, Padding classes, etc.";
    }

    JavaParser.Builder<? extends JavaParser, ?> parser = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath());

    JavaVisitor<ExecutionContext> writeModelClass = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate valueModel = JavaTemplate.builder(
                """
                        @Value
                        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
                        @With
                        """
        ).javaParser(parser).build();

        final JavaTemplate paddedModel = JavaTemplate.builder(
                """
                        @ToString
                        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
                        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
                        @RequiredArgsConstructor
                        @AllArgsConstructor(access = AccessLevel.PRIVATE)
                        """
        ).javaParser(parser).build();

        final JavaTemplate idField = JavaTemplate.builder("@EqualsAndHashCode.Include UUID id;").imports("java.util.UUID").javaParser(parser).build();
        final JavaTemplate prefixField = JavaTemplate.builder("Space prefix;").javaParser(parser).build();
        final JavaTemplate markersField = JavaTemplate.builder("Markers markers;").imports("org.openrewrite.marker.Markers").javaParser(parser).build();
        final JavaTemplate paddingField = JavaTemplate.builder("@Nullable @NonFinal transient WeakReference<Padding> padding;").javaParser(parser).build();
        final JavaTemplate implementsTree = JavaTemplate.builder("Toml").javaParser(parser).build();

        final JavaTemplate getPadding = JavaTemplate.builder(
                """
                        public Padding getPadding() {
                            Padding p;
                            if (this.padding == null) {
                                p = new Padding(this);
                                this.padding = new WeakReference<>(p);
                            } else {
                                p = this.padding.get();
                                if (p == null || p.t != this) {
                                    p = new Padding(this);
                                    this.padding = new WeakReference<>(p);
                                }
                            }
                            return p;
                        }
                        """
        ).build();

        final JavaTemplate paddingClass = JavaTemplate.builder(
                """
                        @RequiredArgsConstructor
                        public static class Padding {
                            private final #{} t;
                        }
                        """
        ).build();

        final JavaTemplate acceptMethod = JavaTemplate.builder(
                """
                        @Override public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
                          return v.visit#{}(this, p);
                        }
                        """
        ).javaParser(parser).build();

        /**
         * The accessors in the model class that skips the padding and return the contained element.
         */
        final JavaTemplate unwrappedPaddedGetterWither = JavaTemplate.builder(
                """
                        public #{} get#{}() {
                            return #{}.getElement();
                        }
                                                
                        public #{} with#{}(#{} #{}) {
                            //noinspection ConstantConditions
                            return getPadding().with#{}(Toml#{}Padded.withElement(this.#{}, #{}));
                        }
                        """
        ).javaParser(parser).build();

        final JavaTemplate nullableUnwrappedPaddedGetterWither = JavaTemplate.builder(
                """
                        @Nullable
                        public #{} get#{}() {
                            return #{} == null ? null : #{}.getElement();
                        }
                                                
                        public #{} with#{}(@Nullable #{} #{}) {
                            if (#{} == null) {
                                return this.#{} == null ? this : new #{}(#{});
                            }
                            return getPadding().with#{}(Toml#{}Padded.withElement(this.#{}, #{}));
                        }
                        """
        ).javaParser(parser).build();

        /**
         * The accessors in the model class that skips the padding and return the contained elements.
         */
        final JavaTemplate unwrappedContainerGetterWither = JavaTemplate.builder(
                """
                        public List<#{}> get#{}() {
                            return #{}.getElements();
                        }
                                                
                        public #{} with#{}(List<#{}> #{}) {
                            return getPadding().with#{}(this.#{}.getPadding().withElements(TomlRightPadded.withElements(
                                this.#{}.getPadding().getElements(), #{}));
                        }
                        """
        ).javaParser(parser).build();

        final JavaTemplate withGetterAnnotations = JavaTemplate.builder("@With @Getter")
                .javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;
            if (!FindAnnotations.find(c, "@generate.Skip").isEmpty()) {
                //noinspection ConstantConditions
                return null;
            }

            boolean padded = c.getBody().getStatements().stream().anyMatch(this::isPadded);

            if (c.getImplements() == null) {
                c = implementsTree.apply(updateCursor(c), c.getCoordinates().addImplementsClause());
            }


            c = markersField.apply(updateCursor(c), c.getBody().getCoordinates().firstStatement());
            c = prefixField.apply(updateCursor(c), c.getBody().getCoordinates().firstStatement());
            c = idField.apply(updateCursor(c), c.getBody().getCoordinates().firstStatement());
            c = acceptMethod.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(), classDecl.getSimpleName());

            for (Statement statement : c.getBody().getStatements()) {
                if (statement instanceof J.VariableDeclarations varDec) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(varDec.getType());

                    JavaType.FullyQualified elementType = null;
                    if (varDec.getTypeExpression() instanceof J.ParameterizedType typeExpression) {
                        if (typeExpression.getTypeParameters() != null) {
                            elementType = TypeUtils.asFullyQualified(typeExpression.getTypeParameters().get(0).getType());
                        }
                    }

                    if (fqn != null) {
                        if (elementType != null) {
                            c = switch (fqn.getClassName()) {
                                case "TomlContainer" -> writeContainerGetterWithers(c, varDec, elementType);
                                case "TomlLeftPadded" -> writePaddedGetterWithers(c, varDec, elementType, "Left");
                                case "TomlRightPadded" -> writePaddedGetterWithers(c, varDec, elementType, "Right");
                                default -> writeModelGetterWithers(c, varDec);
                            };
                        } else if (padded) {
                            c = writeModelGetterWithers(c, varDec);
                        }
                    } else if (padded) {
                        c = writeModelGetterWithers(c, varDec);
                    }
                }
            }

            if (padded) {
                c = paddedModel.apply(updateCursor(c), c.getCoordinates().replaceAnnotations());
                c = paddingField.apply(updateCursor(c), c.getBody().getCoordinates().firstStatement());
                c = getPadding.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement());
                c = paddingClass.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(), c.getSimpleName());
            } else {
                c = valueModel.apply(updateCursor(c), c.getCoordinates().replaceAnnotations());
            }

            List<Statement> statements = c.getBody().getStatements();
            c = c.withBody(c.getBody().withStatements(ListUtils.map(statements, (i, statement) -> {
                if (statement instanceof J.VariableDeclarations && i > 0) {
                    Statement previous = statements.get(i - 1);
                    if (!((J.VariableDeclarations) statement).getLeadingAnnotations().isEmpty() ||
                        (previous instanceof J.VariableDeclarations) && !((J.VariableDeclarations) previous).getLeadingAnnotations().isEmpty()) {
                        return statement.withPrefix(Space.format("\n\n"));
                    }
                }
                return statement;
            })));

            return c;
        }

        private J.ClassDeclaration writeModelGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec) {
            return c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), statement -> {
                if (statement == varDec) {
                    Cursor cursor = new Cursor(new Cursor(updateCursor(c), c.getBody()), varDec);
                    statement = withGetterAnnotations.apply(cursor, varDec.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                }
                return statement;
            })));
        }

        private J.ClassDeclaration writeContainerGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType) {
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = c.getSimpleName();

            J.Block body = unwrappedContainerGetterWither.apply(new Cursor(updateCursor(c), c.getBody()), c.getBody().getCoordinates().lastStatement(),
                    elementTypeName, capitalizedName,
                    name,
                    modelTypeName, capitalizedName, elementTypeName, name,
                    capitalizedName, name,
                    name, name);

            return c.withBody(body);
        }

        private J.ClassDeclaration writePaddedGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType,
                                                            String leftOrRight) {
            boolean nullable = !FindAnnotations.find(varDec, "@org.openrewrite.internal.lang.Nullable").isEmpty();
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = c.getSimpleName();

            J.Block body = c.getBody();
            if (nullable) {
                StringJoiner newModelArguments = new StringJoiner(", ");
                for (Statement statement : body.getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        newModelArguments.add(statement == varDec ? "null" : ((J.VariableDeclarations) statement).getVariables()
                                .get(0).getSimpleName());
                    }
                }
                body = nullableUnwrappedPaddedGetterWither.apply(new Cursor(getCursor(), body), body.getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name, name, modelTypeName, capitalizedName,
                        elementTypeName, name, name, name, modelTypeName, newModelArguments.toString(),
                        capitalizedName, leftOrRight, name, name);
            } else {
                body = unwrappedPaddedGetterWither.apply(new Cursor(getCursor(), body), body.getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name,
                        modelTypeName, capitalizedName, elementTypeName, name,
                        capitalizedName, leftOrRight, name, name);
            }

            return c.withBody(body);
        }

        boolean isPadded(Statement statement) {
            if (!(statement instanceof J.VariableDeclarations)) {
                return false;
            }
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(((J.VariableDeclarations) statement).getType());
            assert type != null;
            return type.getClassName().contains("Padded") || type.getClassName().equals("TomlContainer");
        }
    };

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (!(parent instanceof J.ClassDeclaration) || !((J.ClassDeclaration) parent).getSimpleName().equals("Toml")) {
                    return block;
                }

                J.Block b = block.withStatements(ListUtils.map(block.getStatements(), s -> s instanceof J.ClassDeclaration &&
                                                                                           !(((J.ClassDeclaration) s).getSimpleName().equals("CompilationUnit")) ? null : s));
                List<Statement> statements = new ArrayList<>(b.getStatements());
                statements.addAll(ListUtils.map(modelClasses,
                        mc -> (J.ClassDeclaration) writeModelClass.visitNonNull(mc, ctx, getCursor().getParentOrThrow())));
                b = b.withStatements(statements);

                return b;
            }
        };
    }
}
