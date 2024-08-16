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

import java.util.List;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class WriteVisitorMethods extends Recipe {
    final List<J.ClassDeclaration> modelClasses;

    @Override
    public String getDisplayName() {
        return "Write TOML boilerplate";
    }

    @Override
    public String getDescription() {
        return "Write the boilerplate for `TomlVisitor` and `TomlIsoVisitor`.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                return switch (classDecl.getSimpleName()) {
                    case "TomlVisitor" ->
                            writeVisitorMethods.visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                    case "TomlIsoVisitor" ->
                            writeIsoVisitorMethods.visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                    default -> classDecl;
                };

            }
        };
    }

    JavaParser.Builder<? extends JavaParser, ?> parser = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath());

    private final JavaVisitor<ExecutionContext> writeVisitorMethods = new JavaIsoVisitor<>() {

        final JavaTemplate visitMethod = JavaTemplate.builder(
                """
                public Toml visit#{}(Toml.#{} #{}, P p) {
                    Toml.#{} #{} = #{};
                    #{} = #{}.withPrefix(visitSpace(#{}.getPrefix(), p));
                    #{} = #{}.withMarkers(visitMarkers(#{}.getMarkers(), p));
                    #{}
                    return #{};
                }
                """
        ).javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            for (J.ClassDeclaration modelClass : missingVisitorMethods(c)) {
                String modelTypeName = modelClass.getSimpleName();
                String paramName = modelTypeName.substring(0, 1).toLowerCase() + modelTypeName.substring(1);
                String varName = paramName.substring(0, 1);
                if (varName.equals("p")) {
                    varName = "pp";
                }

                StringJoiner fields = new StringJoiner("\n    ");
                for (Statement statement : modelClass.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations varDec) {
                        boolean nullable = !FindAnnotations.find(varDec, "@org.openrewrite.internal.lang.Nullable").isEmpty();
                        String name = varDec.getVariables().get(0).getSimpleName();
                        String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);

                        JavaType.FullyQualified elemType = requireNonNull(TypeUtils.asFullyQualified(varDec.getType()));
                        switch (elemType.getClassName()) {
                            case "TomlLeftPadded":
                                if (nullable) {
                                    fields.add("if(" + varName + ".getPadding().get" + capitalizedName + "() != null) {");
                                }
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(visitLeftPadded(" +
                                           varName + ".getPadding().get" + capitalizedName + "(), p));");
                                if (nullable) {
                                    fields.add("}");
                                }
                                break;
                            case "TomlRightPadded":
                                if (nullable) {
                                    fields.add("if(" + varName + ".getPadding().get" + capitalizedName + "() != null) {");
                                }
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(visitRightPadded(" +
                                           varName + ".getPadding().get" + capitalizedName + "(), p));");
                                if (nullable) {
                                    fields.add("}");
                                }
                                break;
                            case "TomlContainer":
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(visitContainer(" + varName + ".getPadding().get" + capitalizedName + "(), p));");
                                break;
                            case "List":
                                J.ParameterizedType parameterizedType = requireNonNull((J.ParameterizedType) varDec.getTypeExpression());
                                String elemListType = requireNonNull(TypeUtils.asFullyQualified(requireNonNull(parameterizedType.getTypeParameters()).get(0).getType()))
                                        .getClassName();
                                fields.add(varName + " = " + varName + ".with" + capitalizedName + "(ListUtils.map(" +
                                           varName + ".get" + capitalizedName + "(), t -> (" + elemListType +
                                           ") visit(t, p)));");
                                break;
                            default:
                                if (elemType.getClassName().startsWith("Toml")) {
                                    fields.add(varName + " = " + varName + ".with" + capitalizedName + "((" +
                                               elemType.getClassName() + ") visit(" + varName + ".get" + capitalizedName + "(), p));");
                                }
                        }
                    }
                }
                c = visitMethod.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(),
                        modelTypeName, modelTypeName, paramName,
                        modelTypeName, varName, paramName,
                        varName, varName, varName,
                        varName, varName, varName,
                        fields,
                        varName);
            }

            return c;
        }
    };

    private final JavaVisitor<ExecutionContext> writeIsoVisitorMethods = new JavaIsoVisitor<>() {
        final JavaTemplate isoVisitMethod = JavaTemplate.builder(
                """
                @Override
                public Toml.#{} visit#{}(Toml.#{} #{}, P p) {
                    return (Toml.#{}) super.visit#{}(#{}, p);
                }
                """
        ).javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            for (J.ClassDeclaration modelClass : missingVisitorMethods(c)) {
                String modelTypeName = modelClass.getSimpleName();
                String paramName = modelTypeName.substring(0, 1).toLowerCase() + modelTypeName.substring(1);
                c = isoVisitMethod.apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(),
                        modelTypeName, modelTypeName, modelTypeName, paramName,
                        modelTypeName, modelTypeName, paramName);
            }

            return c;
        }
    };

    private List<J.ClassDeclaration> missingVisitorMethods(J.ClassDeclaration visitorClass) {
        return ListUtils.map(modelClasses, modelClass -> {
            for (Statement statement : visitorClass.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration m) {
                    if (m.getSimpleName().endsWith(modelClass.getSimpleName())) {
                        return null;
                    }
                }
            }
            return modelClass;
        });
    }
}
