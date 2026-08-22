/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Docker;

import java.util.Locale;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeFromAsCasing extends Recipe {

    @Override
    public String getDisplayName() {
        return "Match the casing of `AS` to the casing of `FROM`";
    }

    @Override
    public String getDescription() {
        return "BuildKit's `FromAsCasing` check reports a `FROM ... as name` whose `AS` is not written in the same " +
                "casing as the `FROM` that introduces it. A `FROM` whose own casing is mixed is left alone, as " +
                "there is no casing for its `AS` to match.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                Docker.From f = super.visitFrom(from, ctx);
                Docker.From.As as = f.getAs();
                if (as == null) {
                    return f;
                }
                String keyword = f.getKeyword();
                String lower = keyword.toLowerCase(Locale.ROOT);
                String upper = keyword.toUpperCase(Locale.ROOT);
                if (keyword.equals(lower)) {
                    return f.withAs(as.withKeyword(as.getKeyword().toLowerCase(Locale.ROOT)));
                }
                if (keyword.equals(upper)) {
                    return f.withAs(as.withKeyword(as.getKeyword().toUpperCase(Locale.ROOT)));
                }
                return f;
            }
        };
    }
}
