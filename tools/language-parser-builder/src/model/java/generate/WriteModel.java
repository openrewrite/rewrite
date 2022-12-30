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
import java.util.function.Supplier;

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

    Supplier<JavaParser> parser = () -> JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build();

    JavaVisitor<ExecutionContext> writeModelClass = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate valueModel = JavaTemplate.builder(this::getCursor,
                """
                        @Value
                        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
                        @With
                        """
        ).javaParser(parser).build();

        final JavaTemplate paddedModel = JavaTemplate.builder(this::getCursor,
                """
                        @ToString
                        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
                        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
                        @RequiredArgsConstructor
                        @AllArgsConstructor(access = AccessLevel.PRIVATE)
                        """
        ).javaParser(parser).build();

        final JavaTemplate idField = JavaTemplate.builder(this::getCursor, "@EqualsAndHashCode.Include UUID id;").javaParser(parser).build();
        final JavaTemplate prefixField = JavaTemplate.builder(this::getCursor, "Space prefix;").javaParser(parser).build();
        final JavaTemplate markersField = JavaTemplate.builder(this::getCursor, "Markers markers;").javaParser(parser).build();
        final JavaTemplate paddingField = JavaTemplate.builder(this::getCursor, "@Nullable @NonFinal transient WeakReference<Padding> padding;").javaParser(parser).build();
        final JavaTemplate implementsTree = JavaTemplate.builder(this::getCursor, "Toml").javaParser(parser).build();

        final JavaTemplate getPadding = JavaTemplate.builder(this::getCursor,
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

        final JavaTemplate paddingClass = JavaTemplate.builder(this::getCursor,
                """
                        @RequiredArgsConstructor
                        public static class Padding {
                            private final #{} t;
                        }
                        """
        ).build();

        final JavaTemplate acceptMethod = JavaTemplate.builder(this::getCursor,
                """
                        @Override public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
                          return v.visit#{}(this, p);
                        }
                        """
        ).javaParser(parser).build();

        /**
         * The accessors in the model class that skips the padding and return the contained element.
         */
        final JavaTemplate unwrappedPaddedGetterWither = JavaTemplate.builder(this::getCursor,
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

        final JavaTemplate nullableUnwrappedPaddedGetterWither = JavaTemplate.builder(this::getCursor,
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
        final JavaTemplate unwrappedContainerGetterWither = JavaTemplate.builder(this::getCursor,
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

        final JavaTemplate withGetterAnnotations = JavaTemplate.builder(this::getCursor, "@With @Getter")
                .javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;
            if (FindAnnotations.find(c, "@generate.Skip").size() > 0) {
                //noinspection ConstantConditions
                return null;
            }

            boolean padded = c.getBody().getStatements().stream().anyMatch(this::isPadded);

            if(c.getImplements() == null) {
                c = c.withTemplate(implementsTree, c.getCoordinates().addImplementsClause());
            }

            c = c.withTemplate(markersField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(prefixField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(idField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(acceptMethod, c.getBody().getCoordinates().lastStatement(), classDecl.getSimpleName());

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
                                default -> c.withTemplate(withGetterAnnotations, varDec.getCoordinates()
                                        .addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                            };
                        } else if (padded) {
                            c = c.withTemplate(withGetterAnnotations, varDec.getCoordinates()
                                    .addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        }
                    }
                }
            }

            if (padded) {
                c = c.withTemplate(paddedModel, c.getCoordinates().replaceAnnotations());
                c = c.withTemplate(paddingField, c.getBody().getCoordinates().firstStatement());
                c = c.withTemplate(getPadding, c.getBody().getCoordinates().lastStatement());
                c = c.withTemplate(paddingClass, c.getBody().getCoordinates().lastStatement(), c.getSimpleName());
            } else {
                c = c.withTemplate(valueModel, c.getCoordinates().replaceAnnotations());
            }

            List<Statement> statements = c.getBody().getStatements();
            c = c.withBody(c.getBody().withStatements(ListUtils.map(statements, (i, statement) -> {
                if (statement instanceof J.VariableDeclarations && i > 0) {
                    Statement previous = statements.get(i - 1);
                    if (!((J.VariableDeclarations) statement).getAllAnnotations().isEmpty() ||
                        (previous instanceof J.VariableDeclarations) && !((J.VariableDeclarations) previous).getAllAnnotations().isEmpty()) {
                        return statement.withPrefix(Space.format("\n\n"));
                    }
                }
                return statement;
            })));

            return c;
        }

        private J.ClassDeclaration writeContainerGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType) {
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = c.getSimpleName();

            c = c.withTemplate(unwrappedContainerGetterWither, c.getBody().getCoordinates().lastStatement(),
                    elementTypeName, capitalizedName,
                    name,
                    modelTypeName, capitalizedName, elementTypeName, name,
                    capitalizedName, name,
                    name, name);

            return c;
        }

        private J.ClassDeclaration writePaddedGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType,
                                                            String leftOrRight) {
            boolean nullable = !FindAnnotations.find(varDec, "@org.openrewrite.internal.lang.Nullable").isEmpty();
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = c.getSimpleName();

            if (nullable) {
                StringJoiner newModelArguments = new StringJoiner(", ");
                for (Statement statement : c.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        newModelArguments.add(statement == varDec ? "null" : ((J.VariableDeclarations) statement).getVariables()
                                .get(0).getSimpleName());
                    }
                }
                c = c.withTemplate(nullableUnwrappedPaddedGetterWither, c.getBody().getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name, name, modelTypeName, capitalizedName,
                        elementTypeName, name, name, name, modelTypeName, newModelArguments.toString(),
                        capitalizedName, leftOrRight, name, name);
            } else {
                c = c.withTemplate(unwrappedPaddedGetterWither, c.getBody().getCoordinates().lastStatement(),
                        elementTypeName, capitalizedName, name,
                        modelTypeName, capitalizedName, elementTypeName, name,
                        capitalizedName, leftOrRight, name, name);
            }

            return c;
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
    protected JavaVisitor<ExecutionContext> getVisitor() {
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
