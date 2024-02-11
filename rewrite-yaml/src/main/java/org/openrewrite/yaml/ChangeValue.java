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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeValue extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML entry.",
            example = "$.subjects.kind")
    String keyPath;

    @Option(displayName = "New value",
            description = "The new value to set for the key identified by the `oldKeyPath`.",
            example = "Deployment")
    String value;

    @Override
    public String getDisplayName() {
        return "Change value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", keyPath, value);
    }

    @Override
    public String getDescription() {
        return "Change a YAML mapping entry value while leaving the key intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
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
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Yaml.Scalar s = super.visitScalar(scalar, ctx);
                if (matcher.matches(getCursor())) {
                    s = s.withValue(value);
                }
                return s;
            }
        };
    }


}
