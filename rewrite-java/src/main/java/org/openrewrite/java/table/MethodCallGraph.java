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
package org.openrewrite.java.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class MethodCallGraph extends DataTable<MethodCallGraph.Row> {

    public MethodCallGraph(Recipe recipe) {
        super(recipe,
                "Method call graph",
                "The call relationships between methods.");
    }

    @Value
    public static class Row {
        @Column(displayName = "From",
                description = "The containing method that is making the call.")
        String from;

        @Column(displayName = "To",
                description = "The method that is being called.")
        String to;
    }
}
