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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteKey extends Recipe {

    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a JSON entry.",
            example = "$.subjects.kind")
    String keyPath;

    @Option(displayName = "Delete empty parents",
            description = "Delete objects and arrays that become empty as a result of deleting the key, " +
                          "applied recursively so that a chain of containers which only held the deleted key is " +
                          "removed entirely. For example, deleting `$.engines.node` from `{\"engines\": {\"node\": \"20\"}}` " +
                          "also deletes `engines`. Containers that were already empty before this recipe ran are left " +
                          "alone. Defaults to `false`.",
            required = false)
    @Nullable
    Boolean deleteEmptyParents;

    String displayName = "Delete key";

    String description = "Delete a JSON mapping entry key.";

    @JsonCreator
    public DeleteKey(String keyPath, @Nullable Boolean deleteEmptyParents) {
        this.keyPath = keyPath;
        this.deleteEmptyParents = deleteEmptyParents;
    }

    @Deprecated
    public DeleteKey(String keyPath) {
        this(keyPath, null);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        boolean deleteEmpty = Boolean.TRUE.equals(deleteEmptyParents);
        return new JsonIsoVisitor<ExecutionContext>() {
            private final Set<UUID> emptied = new HashSet<>();

            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, ExecutionContext ctx) {
                Json.JsonObject o = super.visitObject(obj, ctx);
                List<JsonRightPadded<Json>> paddedMembers = o.getPadding().getMembers();
                int lastMember = paddedMembers.size() - 1;
                AtomicReference<Space> copyFirstPrefix = new AtomicReference<>();
                AtomicReference<Space> copyLastAfter = new AtomicReference<>();
                o = o.withMembers(ListUtils.map(o.getMembers(), (i, e) -> {
                    if (matcher.matches(new Cursor(getCursor(), e)) || (deleteEmpty && holdsEmptiedContainer(e))) {
                        if (i == 0 && getCursor().getParentOrThrow().getValue() instanceof Json.Array) {
                            copyFirstPrefix.set(e.getPrefix());
                        }
                        if (i == lastMember) {
                            copyLastAfter.set(paddedMembers.get(i).getAfter());
                        }
                        return null;
                    }
                    return e;
                }));

                if (!o.getMembers().isEmpty() && copyFirstPrefix.get() != null) {
                    o = o.withMembers(ListUtils.mapFirst(o.getMembers(), e -> e.withPrefix(copyFirstPrefix.get())));
                }

                if (!o.getMembers().isEmpty() && copyLastAfter.get() != null) {
                    o = o.getPadding().withMembers(ListUtils.mapLast(o.getPadding().getMembers(),
                            m -> m.withAfter(concat(m.getAfter(), copyLastAfter.get()))));
                }

                if (deleteEmpty && holdsNothing(o.getMembers()) && !holdsNothing(obj.getMembers())) {
                    emptied.add(o.getId());
                }

                return o;
            }

            @Override
            public Json.Array visitArray(Json.Array array, ExecutionContext ctx) {
                Json.Array a = super.visitArray(array, ctx);
                if (!deleteEmpty) {
                    return a;
                }

                List<JsonRightPadded<JsonValue>> paddedValues = a.getPadding().getValues();
                int lastValue = paddedValues.size() - 1;
                AtomicReference<Space> copyFirstPrefix = new AtomicReference<>();
                AtomicReference<Space> copyLastAfter = new AtomicReference<>();
                a = a.withValues(ListUtils.map(a.getValues(), (i, v) -> {
                    if (emptied.contains(v.getId())) {
                        if (i == 0) {
                            copyFirstPrefix.set(v.getPrefix());
                        }
                        if (i == lastValue) {
                            copyLastAfter.set(paddedValues.get(i).getAfter());
                        }
                        return null;
                    }
                    return v;
                }));

                if (!a.getValues().isEmpty() && copyFirstPrefix.get() != null) {
                    a = a.withValues(ListUtils.mapFirst(a.getValues(), v -> v.withPrefix(copyFirstPrefix.get())));
                }

                if (!a.getValues().isEmpty() && copyLastAfter.get() != null) {
                    a = a.getPadding().withValues(ListUtils.mapLast(a.getPadding().getValues(),
                            v -> v.withAfter(concat(v.getAfter(), copyLastAfter.get()))));
                }

                if (holdsNothing(a.getValues()) && !holdsNothing(array.getValues())) {
                    emptied.add(a.getId());
                }

                return a;
            }

            private boolean holdsEmptiedContainer(Json member) {
                if (!(member instanceof Json.Member)) {
                    return false;
                }
                JsonValue value = ((Json.Member) member).getValue();
                return (value instanceof Json.JsonObject || value instanceof Json.Array) && emptied.contains(value.getId());
            }
        };
    }

    private static boolean holdsNothing(List<? extends Json> elements) {
        for (Json element : elements) {
            if (!(element instanceof Json.Empty)) {
                return false;
            }
        }
        return true;
    }

    private static Space concat(Space existing, Space trailing) {
        if (existing.getComments().isEmpty()) {
            return trailing;
        }
        List<Comment> comments = new ArrayList<>(existing.getComments());
        int last = comments.size() - 1;
        comments.set(last, comments.get(last).withSuffix(trailing.getWhitespace()));
        comments.addAll(trailing.getComments());
        return existing.withComments(comments);
    }
}
