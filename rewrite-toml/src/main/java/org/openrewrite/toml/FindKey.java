/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.toml.tree.Toml;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A TOML path expression to locate keys. Wildcards are supported.",
            example = "**.name")
    String keyPath;

    @Override
    public String getDisplayName() {
        return "Find TOML keys";
    }

    @Override
    public String getDescription() {
        return "Find TOML keys matching a path expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TomlPathMatcher matcher = new TomlPathMatcher(keyPath);
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);
                return matcher.matches(getCursor()) ? SearchResult.found(kv) : kv;
            }
        };
    }
}
