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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AppendToSequence extends Recipe {
    @Option(displayName = "sequence path",
            description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML sequence.",
            example = "$.universe.planets")
    String sequencePath;

    @Option(displayName = "New value",
            description = "The new value to be appended to the sequence.",
            example = "earth")
    String value;

    @Option(displayName = "Optional: match existing sequence values",
            description = "If specified, the item will only be appended if the existing sequence matches these values.",
            example = "existingValue1",
            required = false)
    @Nullable
    List<String> existingSequenceValues;

    @Option(displayName = "Optional: match existing sequence values in any order",
            description = "If specified in combination with the above parameter, the item will only be appended if the existing sequence has the specified values in any order.",
            example = "true",
            required = false)
    @Nullable
    Boolean matchExistingSequenceValuesInAnyOrder;

    @Override
    public String getDisplayName() {
        return "Append to sequence";
    }

    @Override
    public String getInstanceName() {
        return String.format("Append %s to sequence `%s`",
                value, sequencePath);
    }

    @Override
    public String getDescription() {
        return "Append item to YAML sequence.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(sequencePath);
        return new AppendToSequenceVisitor(matcher, value, existingSequenceValues,
                Boolean.TRUE.equals(matchExistingSequenceValuesInAnyOrder));
    }
}
