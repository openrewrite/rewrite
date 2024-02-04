/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.Column;
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
        @Column(displayName = "Name",
                description = "The name of the committer.")
        String name;
        @Column(displayName = "Email",
                description = "The email of the committer.")
        String email;
        @Column(displayName = "Date",
                description = "The date of the day.")
        LocalDate day;
        @Column(displayName = "Number of commits",
                description = "The number of commits made by this committer on this day.")
        int commits;
    }
}
