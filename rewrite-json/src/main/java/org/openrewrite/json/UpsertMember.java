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
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpsertMember extends Recipe {
    @Option(displayName = "Parent key path",
            description = "JSONPath expression to the member's parent object",
            example = "$.metadata")
    String parent;

    @Option(displayName = "Key",
            description = "The member's key. If this key does not exist on the parent it will be added.",
            example = "author")
    String key;

    @Option(displayName = "Value",
            description = "The member's value.  This can be a string, number, boolean, array, or object",
            example = "\"Tolkien\"")
    @Language("Json")
    String value;

    @Option(displayName = "Accept theirs",
            description = "When the JSON key value pair to insert conflicts with an existing member use the original value.",
            required = false)
    @Nullable
    Boolean acceptTheirs;

    @Incubating(since = "7.11.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Upsert JSON Member";
    }

    @Override
    public String getDescription() {
        return "Updates (or inserts if not present) a key value pair to an object in a JSON document.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public JsonVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher parentMatcher = new JsonPathMatcher(parent);
        JsonPathMatcher keyMatcher = new JsonPathMatcher(parent + '.' + key);
        return new JsonIsoVisitor<ExecutionContext>() {
            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext executionContext) {
                Json.Member m = super.visitMember(member, executionContext);
                
                /*
                  Look at parent member to see if its value is an object without
                  the new key. If so, insert the new member (key value pair)
                 */
                if (parentMatcher.matches(getCursor()) && m.getValue() instanceof Json.JsonObject) {

                    boolean hasKey = false;
                    for (Json each : ((Json.JsonObject) m.getValue()).getMembers()) {
                        if (each instanceof Json.Member) {
                            if (((Json.Member) each).getKey().printTrimmed().replace("\"", "").equals(UpsertMember.this.key)) {
                                hasKey = true;
                                break;
                            }
                        }
                    }

                    if (!hasKey) {
                        String source = UpsertMember.this.value;
                        if (source.startsWith("'") || source.startsWith("\"")) {
                            source = source.substring(1, source.length() - 1);
                        }

                        Json.JsonObject parentsValue = (Json.JsonObject) m.getValue();
                        List<Json> members = parentsValue.getMembers();

                        // create new member
                        JsonRightPadded<JsonKey> newKey = new JsonRightPadded<>(new Json.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "\"" + UpsertMember.this.key + "\"", UpsertMember.this.key), m.getKey().getPrefix(), Markers.EMPTY);
                        Json.Literal newValue = new Json.Literal(randomId(), m.getValue().getPrefix(), Markers.EMPTY, UpsertMember.this.value, source);
                        Json newMember = new Json.Member(randomId(), members.get(0).getPrefix(), Markers.EMPTY, newKey, newValue);

                        /*
                         Last member is Empty (closing bracket) so we want to
                         insert the member right before
                        */
                        members.add(members.size() - 1, newMember);

                        m = m.withValue(parentsValue.withMembers(members));
                    }

                }

                // if key matches we determine if we should update or use theirs
                if ((keyMatcher.matches(getCursor()) && (!(m.getValue() instanceof Json.Literal) || !((Json.Literal) m.getValue()).getValue().equals(UpsertMember.this.value))) && Boolean.FALSE.equals(UpsertMember.this.acceptTheirs)) {
                    String source = UpsertMember.this.value;
                    if (source.startsWith("'") || source.startsWith("\"")) {
                        source = source.substring(1, source.length() - 1);
                    }
                    if (!(m.getValue() instanceof Json.Literal) || !((Json.Literal) m.getValue()).getSource().equals(UpsertMember.this.value)) {
                        m = m.withValue(new Json.Literal(randomId(), m.getValue().getPrefix(), Markers.EMPTY, UpsertMember.this.value, source));
                    }
                }

                return m;
            }
        };
    }
}
