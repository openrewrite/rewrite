package generate;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class WriteVisitorMethods extends Recipe {
    final List<J.ClassDeclaration> modelClasses;

    @Override
    public String getDisplayName() {
        return "Write the boilerplate for `CobolVisitor` and `CobolIsoVisitor`";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                switch (classDecl.getSimpleName()) {
                    case "CobolVisitor":
                        return writeVisitorMethods.visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                    case "CobolIsoVisitor":
                        return writeIsoVisitorMethods.visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                }

                return classDecl;
            }
        };
    }

    Supplier<JavaParser> parser = () -> JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build();

    private final JavaVisitor<ExecutionContext> writeVisitorMethods = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate visitMethod = JavaTemplate.builder(this::getCursor, "" +
                "public Cobol visit#{}(Cobol.#{} #{}, P p) {" +
                "    Cobol.#{} #{} = #{};" +
                "    #{} = #{}.withPrefix(visitSpace(#{}.getPrefix(), p));" +
                "    #{} = #{}.withMarkers(visitMarkers(#{}.getMarkers(), p));" +
                "    #{}" +
                "    return #{};" +
                "}").javaParser(parser).build();

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
                    if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varDec = (J.VariableDeclarations) statement;
                        String name = varDec.getVariables().get(0).getSimpleName();
                        String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);

                        JavaType.FullyQualified elemType = requireNonNull(TypeUtils.asFullyQualified(varDec.getType()));
                        switch (elemType.getClassName()) {
                            case "CobolLeftPadded":
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(visitLeftPadded(" +
                                        varName + ".getPadding().get" + capitalizedName + "(), p));");
                                break;
                            case "CobolRightPadded":
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(visitRightPadded(" +
                                        varName + ".getPadding().get" + capitalizedName + "(), p));");
                                break;
                            case "CobolContainer":
                                throw new UnsupportedOperationException("Implement me!");
                            case "List":
                                J.ParameterizedType parameterizedType = requireNonNull((J.ParameterizedType) varDec.getTypeExpression());
                                String elemListType = requireNonNull(TypeUtils.asFullyQualified(requireNonNull(parameterizedType.getTypeParameters()).get(0).getType()))
                                        .getClassName();
                                fields.add(varName + " = " + varName + ".getPadding().with" + capitalizedName + "(ListUtils.map(" +
                                        varName + ".getPadding().get" + capitalizedName + "(), t -> (" + elemListType +
                                        ") visit(t, p)));");
                                break;
                            default:
                                if(elemType.getClassName().startsWith("Cobol")) {
                                    fields.add(varName + " = " + varName + ".with" + capitalizedName + "((" +
                                            elemType.getClassName() + ") visit(" + varName + ".get" + capitalizedName + "(), p));");
                                }
                        }
                    }
                }

                c = c.withTemplate(visitMethod, c.getBody().getCoordinates().lastStatement(),
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

    private final JavaVisitor<ExecutionContext> writeIsoVisitorMethods = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate isoVisitMethod = JavaTemplate.builder(this::getCursor, "" +
                "@Override " +
                "public Cobol.#{} visit#{}(Cobol.#{} #{}, P p) {" +
                "    return (Cobol.#{}) super.visit#{}(#{}, p);" +
                "}"
        ).javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;

            for (J.ClassDeclaration modelClass : missingVisitorMethods(c)) {
                String modelTypeName = modelClass.getSimpleName();
                String paramName = modelTypeName.substring(0, 1).toLowerCase() + modelTypeName.substring(1);

                c = c.withTemplate(isoVisitMethod, c.getBody().getCoordinates().lastStatement(),
                        modelTypeName, modelTypeName, modelTypeName, paramName,
                        modelTypeName, modelTypeName, paramName);
            }

            return c;
        }
    };

    private List<J.ClassDeclaration> missingVisitorMethods(J.ClassDeclaration visitorClass) {
        return ListUtils.map(modelClasses, modelClass -> {
            for (Statement statement : visitorClass.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration) {
                    J.MethodDeclaration m = (J.MethodDeclaration) statement;
                    if (m.getSimpleName().endsWith(modelClass.getSimpleName())) {
                        return null;
                    }
                }
            }
            return modelClass;
        });
    }
}
