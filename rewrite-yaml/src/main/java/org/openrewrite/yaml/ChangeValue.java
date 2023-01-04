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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression to locate a YAML entry.",
            example = "$.subjects.kind")
    String oldKeyPath;

    @Option(displayName = "New value",
            description = "The new value to set for the key identified by oldKeyPath.",
            example = "Deployment")
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
        return "Change value";
    }

    @Override
    public String getDescription() {
        return "Change a YAML mapping entry value leaving the key intact.";
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
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext context) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, context);
                if (matcher.matches(getCursor()) && (!(e.getValue() instanceof Yaml.Scalar) || !((Yaml.Scalar) e.getValue()).getValue().equals(value))) {
                    Yaml.Anchor anchor = (e.getValue() instanceof Yaml.Scalar) ? ((Yaml.Scalar) e.getValue()).getAnchor() : null;
                    String prefix = e.getValue() instanceof Yaml.Sequence ? ((Yaml.Sequence) e.getValue()).getOpeningBracketPrefix() : e.getValue().getPrefix();
                    e = e.withValue(
                            new Yaml.Scalar(randomId(), prefix, Markers.EMPTY,
                                    Yaml.Scalar.Style.PLAIN, anchor, value)
                    );
                }
                return e;
            }

            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext executionContext) {
                Yaml.Scalar s = super.visitScalar(scalar, executionContext);
                if (matcher.matches(getCursor())) {
                    s = s.withValue(value);
                }
                return s;
            }
        };
    }


}
