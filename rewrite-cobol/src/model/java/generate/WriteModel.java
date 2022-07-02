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

import java.util.List;
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
        final JavaTemplate valueModel = JavaTemplate.builder(this::getCursor, "" +
                "@Value " +
                "@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true) " +
                "@With").javaParser(parser).build();

        final JavaTemplate idField = JavaTemplate.builder(this::getCursor, "@EqualsAndHashCode.Include UUID id;").javaParser(parser).build();
        final JavaTemplate prefixField = JavaTemplate.builder(this::getCursor, "Space prefix;").javaParser(parser).build();
        final JavaTemplate markersField = JavaTemplate.builder(this::getCursor, "Markers markers;").javaParser(parser).build();

        final JavaTemplate acceptMethod = JavaTemplate.builder(this::getCursor, "" +
                "@Override public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {" +
                "  return v.visit#{}(this, p);" +
                "}").javaParser(parser).build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl.withTemplate(valueModel, classDecl.getCoordinates().replaceAnnotations());
            c = c.withTemplate(markersField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(prefixField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(idField, c.getBody().getCoordinates().firstStatement());
            c = c.withTemplate(acceptMethod, c.getBody().getCoordinates().lastStatement(), classDecl.getSimpleName());
            return c;
        }
    };

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (!(parent instanceof J.ClassDeclaration) || !((J.ClassDeclaration) parent).getSimpleName().equals("Cobol")) {
                    return block;
                }

                J.Block b = block.withStatements(ListUtils.map(block.getStatements(), s -> s instanceof J.ClassDeclaration &&
                        !(((J.ClassDeclaration) s).getSimpleName().equals("CompilationUnit")) ? null : s));

                b = b.withStatements(ListUtils.concatAll(b.getStatements(), ListUtils.map(modelClasses,
                        mc -> (J.ClassDeclaration) writeModelClass.visitNonNull(mc, ctx, getCursor().getParentOrThrow()))));

                return b;
            }
        };
    }
}
