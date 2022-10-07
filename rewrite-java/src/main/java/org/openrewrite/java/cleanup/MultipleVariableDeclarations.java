/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class MultipleVariableDeclarations extends Recipe {
    @Override
    public String getDisplayName() {
        return "No multiple variable declarations";
    }

    @Override
    public String getDescription() {
        return "Places each variable declaration in its own statement and on its own line. " +
                "Using one variable declaration per line encourages commenting and can increase readability.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1659");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MultipleVariableDeclarationsVisitor();
    }

}
