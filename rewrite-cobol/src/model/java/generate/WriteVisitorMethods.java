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
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.function.Supplier;

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
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            return classDecl;
        }
    };

    private final JavaVisitor<ExecutionContext> writeIsoVisitorMethods = new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate isoVisitorMethod = JavaTemplate.builder(this::getCursor, "" +
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

                c = c.withTemplate(isoVisitorMethod, c.getBody().getCoordinates().lastStatement(),
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
