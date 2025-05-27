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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.StringJoiner;

/**
 * TODO Unable to add accessors in the first phase due to some bug in JavaTemplate.
 */
@RequiredArgsConstructor
public class WritePaddingAccessors extends Recipe {
    @Override
    public String getDisplayName() {
        return "Write accessors for padded parts of the model";
    }

    @Override
    public String getDescription() {
        return "Write accessors for padded parts of the model.";
    }

    JavaParser.Builder<? extends JavaParser, ?> parser = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath());

    @RequiredArgsConstructor
    class WritePaddingAccessorsVisitor extends JavaIsoVisitor<ExecutionContext> {
        final J.ClassDeclaration modelClassDeclaration;

        /**
         * The accessors in the Padding class that return the padding wrapped element.
         */
        final JavaTemplate paddedGetterWither = JavaTemplate.builder(
                """
                        #{}
                        public Toml#{}<#{}> get#{}() {
                            return t.#{};
                        }

                        public #{} with#{}(#{}Toml#{}<#{}> #{}) {
                            return t.#{} == #{} ? t : new #{}(#{});
                        }
                        """
        ).javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            if (c.getSimpleName().equals("Padding")) {
                for (Statement statement : modelClassDeclaration.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations varDec) {
                        JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(varDec.getType());

                        JavaType.FullyQualified elementType = null;
                        if (varDec.getTypeExpression() instanceof J.ParameterizedType typeExpression) {
                            if (typeExpression.getTypeParameters() != null) {
                                elementType = TypeUtils.asFullyQualified(typeExpression.getTypeParameters().get(0).getType());
                            }
                        }

                        if (fqn != null && elementType != null) {
                            c = switch (fqn.getClassName()) {
                                case "TomlContainer" -> writePaddedGetterWithers(c, varDec, elementType, "Container");
                                case "TomlLeftPadded" -> writePaddedGetterWithers(c, varDec, elementType, "LeftPadded");
                                case "TomlRightPadded" -> writePaddedGetterWithers(c, varDec, elementType, "RightPadded");
                                default -> c;
                            };
                        }
                    }
                }

                return c;
            }

            return super.visitClassDeclaration(classDecl, ctx);
        }

        private J.ClassDeclaration writePaddedGetterWithers(J.ClassDeclaration c, J.VariableDeclarations varDec, JavaType.FullyQualified elementType,
                                                            String leftOrRight) {
            boolean nullable = !FindAnnotations.find(varDec, "@org.openrewrite.internal.lang.Nullable").isEmpty();
            String name = varDec.getVariables().get(0).getSimpleName();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            String elementTypeName = elementType.getClassName();
            String modelTypeName = modelClassDeclaration.getSimpleName();

            StringJoiner newModelArguments = new StringJoiner(", ");
            for (Statement paddingStatement : modelClassDeclaration.getBody().getStatements()) {
                if (paddingStatement instanceof J.VariableDeclarations) {
                    newModelArguments.add(paddingStatement == varDec ? name : "t." + ((J.VariableDeclarations) paddingStatement).getVariables()
                            .get(0).getSimpleName());
                }
            }

            c = paddedGetterWither.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(),
                    nullable ? "@Nullable " : "", leftOrRight, elementTypeName, capitalizedName,
                    name, modelTypeName, capitalizedName,
                    nullable ? "@Nullable " : "", leftOrRight,
                    elementTypeName, name, name, name, modelTypeName, newModelArguments);

            return c;
        }
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (!(parent instanceof J.ClassDeclaration) || !((J.ClassDeclaration) parent).getSimpleName().equals("Toml")) {
                    return block;
                }

                J.Block b = block;

                b = b.withStatements(ListUtils.map(b.getStatements(),
                        mc -> mc instanceof J.ClassDeclaration ?
                                (Statement) new WritePaddingAccessorsVisitor((J.ClassDeclaration) mc)
                                        .visitNonNull(mc, ctx, getCursor().getParentOrThrow()) :
                                mc)
                );

                return b;
            }
        };
    }
}
