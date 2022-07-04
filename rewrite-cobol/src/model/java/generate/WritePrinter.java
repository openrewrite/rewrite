package generate;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.List;

@RequiredArgsConstructor
public class WritePrinter extends Recipe {
    final List<J.ClassDeclaration> modelClasses;

    @Override
    public String getDisplayName() {
        return "Write the boilerplate for `CobolPrinter`";
    }

    @Override
    public String getDescription() {
        return "Every print method starts with `visitSpace` then `visitMarkers`. " +
                "Every model element is visited. An engineer must fill in the places " +
                "where keywords are grammatically required.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }
}
