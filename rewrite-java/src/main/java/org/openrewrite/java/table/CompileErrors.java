package org.openrewrite.java.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class CompileErrors extends DataTable<CompileErrors.Row> {
    public CompileErrors(Recipe recipe) {
        super(recipe,
                "Compile errors",
                "The source code of compile errors.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file that the method call occurred in.")
        String sourceFile;

        @Column(displayName = "Source",
                description = "The source code of the type use.")
        String code;
    }
}