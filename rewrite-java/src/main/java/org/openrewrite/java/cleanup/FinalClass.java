package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;

public class FinalClass extends Recipe {
    @Override
    public String getDisplayName() {
        return "Finalize classes with private constructors";
    }

    @Override
    public String getDescription() {
        return "Adds the `final` modifier to classes that expose no public or package-private constructors.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new FinalClassVisitor();
    }
}
