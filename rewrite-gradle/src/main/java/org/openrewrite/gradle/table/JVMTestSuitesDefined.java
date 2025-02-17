package org.openrewrite.gradle.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class JVMTestSuitesDefined extends DataTable<JVMTestSuitesDefined.Row> {
    public JVMTestSuitesDefined(Recipe recipe) {
        super(recipe, Row.class,
               JVMTestSuitesDefined.class.getName(),
                "JVMTestSuites defined",
                "JVMTestSuites defined.");
    }

    @Value
    public static class Row {

        @Column(displayName = "JVMTestSuite name",
                description = "Name of the defined JVMTestSuite.")
        String name;
    }
}