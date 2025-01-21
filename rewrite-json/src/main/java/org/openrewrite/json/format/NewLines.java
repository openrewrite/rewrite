package org.openrewrite.json.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;

public class NewLines extends Recipe {
    @Override
    public String getDisplayName() {
        return "JSON new lines";
    }

    @Override
    public String getDescription() {
        return "Split members into separate lines in JSON.";
    }

    @Override
    public NewLinesVisitor<ExecutionContext> getVisitor() {
        return new NewLinesVisitor<>(null);
    }
}
