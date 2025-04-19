package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddExplicitImport extends Recipe {
    @Option(displayName = "List of imports to add",
            description = "The list of imports, this field is multiline.",
            example = "foo.bar\nbiz.baz")
    String imports;

    @Override
    public String getDisplayName() {
        return "Add explicit imports";
    }

    @Override
    public int maxCycles() {
        return 1;
    }
    @Override
    public String getDescription() {
        return "This recipe adds an explicit import to a single Java file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(true, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (javaSourceFile != null) {
                    addExplicitImport(imports);
                }
                return super.visitCompilationUnit(cu, executionContext);
            }
        });
    }
}
