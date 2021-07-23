package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;

public class RemoveUnusedPrivateMethods extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove unused private methods";
    }

    @Override
    public String getDescription() {
        return "`private` methods that are never executed are dead code: unnecessary, inoperative code that should be removed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1144");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                JavaType.Method methodType = TypeUtils.asMethod(method.getType());

                if (methodType != null && methodType.hasFlags(Flag.Private)) {
                    J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                    if(!cu.getTypesInUse().contains(methodType)) {
                        //noinspection ConstantConditions
                        return null;
                    }
                }

                return super.visitMethodDeclaration(method, executionContext);
            }
        };
    }
}
