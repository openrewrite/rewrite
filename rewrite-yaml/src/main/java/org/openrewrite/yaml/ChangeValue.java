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
            description = "An XPath expression to locate a YAML entry.",
            example = "subjects/kind")
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
        XPathMatcher xPathMatcher = new XPathMatcher(oldKeyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext context) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, context);

                if (xPathMatcher.matches(getCursor()) && (!(e.getValue() instanceof Yaml.Scalar) || !((Yaml.Scalar) e.getValue()).getValue().equals(value))) {
                    e = e.withValue(
                            new Yaml.Scalar(randomId(), e.getValue().getPrefix(), Markers.EMPTY,
                                    Yaml.Scalar.Style.PLAIN, value)
                    );
                }
                return e;
            }
        };
    }


}
