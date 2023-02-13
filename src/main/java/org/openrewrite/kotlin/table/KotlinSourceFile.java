package org.openrewrite.kotlin.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class KotlinSourceFile extends DataTable<KotlinSourceFile.Row> {

    public KotlinSourceFile(Recipe recipe) {
        super(recipe, "Kotlin source files",
                "Kotlin sources present in ASTs on the SAAS.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path before the run",
                description = "The source path of the file before the run.")
        String sourcePath;

        @Column(displayName = "Source file type", description = "The source file type that was created.")
        SourceFileType sourceFileType;
    }

    public enum SourceFileType {
        Kotlin,
        Quark,
        PlainText
    }
}
