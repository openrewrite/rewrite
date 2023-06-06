package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class CollidingSourceFiles extends DataTable<CollidingSourceFiles.Row> {

    public CollidingSourceFiles(Recipe recipe) {
        super(recipe,
                "Colliding source files",
                "Source files that have the same relative path.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Relative path", description = "The relative path of the source file within its repository.")
        String relativePath;

        @Column(displayName = "Source file type", description = "The type of the source file.")
        String sourceFileType;
    }
}
