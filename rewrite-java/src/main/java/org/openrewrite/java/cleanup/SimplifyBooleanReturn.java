/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Incubating(since = "7.0.0")
public class SimplifyBooleanReturn extends Recipe {

    @Override
    public String getDisplayName() {
        return "Simplify boolean return";
    }

    @Override
    public String getDescription() {
        return "Simplifies Boolean expressions by removing redundancies, e.g.: `a && true` simplifies to `a`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1126");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public SimplifyBooleanReturnVisitor<ExecutionContext> getVisitor() {
        return new SimplifyBooleanReturnVisitor<>();
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }
}
