package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.marker.LstProvenance;

@JsonIgnoreType
public class LstProvenanceTable extends DataTable<LstProvenanceTable.Row> {

    public LstProvenanceTable(Recipe recipe) {
        super(recipe,
                "Parser failures",
                "A list of files that failed to parse along with stack traces of their failures.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Build tool type",
                description = "The type of tool which produced the LST.")
        LstProvenance.Type buildToolType;

        @Column(displayName = "Build tool version",
                description = "The version of the build tool which produced the LST.")
        String buildToolVersion;

        @Column(displayName = "LST serializer version",
                description = "The version of LST serializer which produced the LST.")
        String lstSerializerVersion;
    }
}
