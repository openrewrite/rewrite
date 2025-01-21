package org.openrewrite.json.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;

public class AutoFormat extends Recipe {
    @Override
    public String getDisplayName() {
        return "Format JSON";
    }

    @Override
    public String getDescription() {
        return "Indents JSON using the most common indentation size and tabs or space choice in use in the file.";
    }

    @Override
    public AutoFormatVisitor<ExecutionContext> getVisitor() {
        return new AutoFormatVisitor<>();
    }
}
