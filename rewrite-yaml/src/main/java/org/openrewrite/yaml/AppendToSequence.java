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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HasSourcePath;
import org.openrewrite.Incubating;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class AppendToSequence extends Recipe {
    @Option(displayName = "sequence path",
            description = "A JsonPath expression to locate a YAML sequence.",
            example = "$.universe.planets")
    String sequencePath;

    @Option(displayName = "New value",
            description = "The new value to be appended to the sequence.",
            example = "earth")
    String value;

    @Incubating(since = "7.8.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Append to sequence";
    }

    @Override
    public String getDescription() {
        return "Append item to YAML sequence.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(sequencePath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ec) {
                Yaml.Sequence s = super.visitSequence(sequence, ec);
                if (!matcher.matches(getCursor().getParent())) {
                    return s;
                }
                List<Yaml.Sequence.Entry> entries = sequence.getEntries();
                boolean hasDash = true;
                Yaml.Scalar.Style style = Yaml.Scalar.Style.PLAIN;
                String entryPrefix = "";
                String entryTrailingCommaPrefix = "";
                String itemPrefix = "";
                if (!entries.isEmpty()) {
                    Yaml.Sequence.Entry existingEntry = entries.get(entries.size() - 1);
                    hasDash = existingEntry.isDash();
                    entryPrefix = existingEntry.getPrefix();
                    entryTrailingCommaPrefix = existingEntry.getTrailingCommaPrefix();
                    Yaml.Sequence.Block block = existingEntry.getBlock();
                    itemPrefix = block.getPrefix();
                    if (block instanceof Yaml.Sequence.Scalar) {
                        style = ((Yaml.Sequence.Scalar)block).getStyle();
                    }
                }
                Yaml.Scalar newItem = new Yaml.Scalar(randomId(), itemPrefix, Markers.EMPTY, style, null, AppendToSequence.this.value);
                Yaml.Sequence.Entry newEntry = new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, newItem, hasDash, entryTrailingCommaPrefix);
                entries.add(newEntry);
                return maybeAutoFormat(sequence, s.withEntries(entries), ec);
            }
        };
    }


}
