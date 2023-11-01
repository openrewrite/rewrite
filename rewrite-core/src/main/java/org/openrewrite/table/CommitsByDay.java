package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

import java.time.LocalDate;

public class CommitsByDay extends DataTable<CommitsByDay.Row> {

    public CommitsByDay(Recipe recipe) {
        super(recipe,
                "Commits by day",
                "The commit activity by day by committer.");
    }

    @Value
    public static class Row {
        String name;
        String email;
        LocalDate day;
        int commits;
    }
}
