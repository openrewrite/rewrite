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
package org.openrewrite.json;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
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

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveEmptyKeys extends Recipe {

    private static final String CLEANABLE = "cleanable";
    private static final String CASCADABLE = "cascadable";

    @Option(displayName = "Keys",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) " +
                          "expression bounding the cleanup. Keys it selects, and the keys nested within them, are " +
                          "eligible for removal. When omitted, the whole document is eligible.",
            example = "$..devDependencies",
            required = false)
    @Nullable
    String keys;

    @Option(displayName = "Cascade to",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) " +
                          "expression letting removal continue outward past `keys`. A key this recipe empties is " +
                          "removed when `cascadeTo` selects it or nests it, so `keys` of `$.spec.template` with " +
                          "`cascadeTo` of `$.spec` goes on to remove `spec` once `template` is gone. Keys that were " +
                          "already empty outside `keys` are still left alone. When omitted, removal stops at the " +
                          "`keys` boundary.",
            example = "$.spec",
            required = false)
    @Nullable
    String cascadeTo;

    String displayName = "Remove empty keys";

    String description = "Remove mapping entries whose value is an empty object or array, such as those left behind " +
                         "by `DeleteKey`. Entries are removed from the inside out, so a chain of objects holding " +
                         "nothing but the removed entry is removed entirely. Array elements are left alone, since " +
                         "removing one shifts the indexes of its siblings.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher cleanMatcher = keys == null ? null : new JsonPathMatcher(keys);
        JsonPathMatcher cascadeMatcher = cascadeTo == null ? null : new JsonPathMatcher(cascadeTo);
        return new JsonIsoVisitor<ExecutionContext>() {
            private final Set<UUID> cleanable = new HashSet<>();
            private final Set<UUID> cascadable = new HashSet<>();
            private final Set<UUID> emptied = new HashSet<>();

            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                scope(cleanMatcher, CLEANABLE, cleanable, member);
                scope(cascadeMatcher, CASCADABLE, cascadable, member);
                return super.visitMember(member, ctx);
            }

            private void scope(@Nullable JsonPathMatcher matcher, String message, Set<UUID> scoped, Json.Member member) {
                if (matcher != null && (getCursor().getNearestMessage(message) != null || matcher.matches(getCursor()))) {
                    getCursor().putMessage(message, true);
                    scoped.add(member.getId());
                }
            }

            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, ExecutionContext ctx) {
                Json.JsonObject o = super.visitObject(obj, ctx);
                List<JsonRightPadded<Json>> members = o.getPadding().getMembers();
                List<JsonRightPadded<Json>> retained = ListUtils.map(members, m -> removable(m.getElement()) ? null : m);
                if (retained != members) {
                    // the last member's trailing space is the whitespace before the closing brace
                    JsonRightPadded<Json> last = members.get(members.size() - 1);
                    if (!retained.isEmpty() && retained.get(retained.size() - 1) != last) {
                        retained = ListUtils.mapLast(retained, m -> m.withAfter(concat(m.getAfter(), last.getAfter())));
                    }
                    o = o.getPadding().withMembers(retained);
                    if (holdsNothing(o.getMembers())) {
                        emptied.add(o.getId());
                    }
                }
                return o;
            }

            private boolean removable(Json member) {
                if (!(member instanceof Json.Member)) {
                    return false;
                }
                JsonValue value = ((Json.Member) member).getValue();
                if (!isEmptyContainer(value)) {
                    return false;
                }
                if (cleanMatcher == null || cleanable.contains(member.getId())) {
                    return true;
                }
                return cascadable.contains(member.getId()) && emptied.contains(value.getId());
            }
        };
    }

    private static boolean isEmptyContainer(JsonValue value) {
        return value instanceof Json.JsonObject ?
                holdsNothing(((Json.JsonObject) value).getMembers()) :
                value instanceof Json.Array && holdsNothing(((Json.Array) value).getValues());
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
