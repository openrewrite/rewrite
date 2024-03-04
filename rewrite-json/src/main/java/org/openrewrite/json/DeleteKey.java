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
package org.openrewrite.json;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.Space;

import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression to locate a JSON entry.",
            example = "$.subjects.kind")
    String keyPath;

    @Override
    public String getDisplayName() {
        return "Delete key";
    }

    @Override
    public String getDescription() {
        return "Delete a JSON mapping entry key.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, ExecutionContext ctx) {
                Json.JsonObject o = super.visitObject(obj, ctx);
                AtomicReference<Space> copyFirstPrefix = new AtomicReference<>();
                o = o.withMembers(ListUtils.map(o.getMembers(), (i, e) -> {
                    if (matcher.matches(new Cursor(getCursor(), e))) {
                        if (i == 0 && getCursor().getParentOrThrow().getValue() instanceof Json.Array) {
                            copyFirstPrefix.set(e.getPrefix());
                        }
                        return null;
                    }
                    return e;
                }));

                if (!o.getMembers().isEmpty() && copyFirstPrefix.get() != null) {
                    o = o.withMembers(ListUtils.mapFirst(o.getMembers(), e -> e.withPrefix(copyFirstPrefix.get())));
                }

                return o;
            }
        };
    }
}
