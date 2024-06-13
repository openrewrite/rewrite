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
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeKey extends Recipe {
    @Option(displayName = "Old key path",
            description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML entry.",
            example = "$.subjects.kind")
    String oldKeyPath;

    @Option(displayName = "New key",
            description = "The new name for the key selected by the `oldKeyPath`.",
            example = "kind")
    String newKey;

    @Override
    public String getDisplayName() {
        return "Change key";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, newKey);
    }

    @Override
    public String getDescription() {
        return "Change a YAML mapping entry key while leaving the value intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (matcher.matches(getCursor())) {
                    if (e.getKey() instanceof Yaml.Scalar) {
                        e = e.withKey(((Yaml.Scalar) e.getKey()).withValue(newKey));
                    }
                }
                return e;
            }
        };
    }
}
