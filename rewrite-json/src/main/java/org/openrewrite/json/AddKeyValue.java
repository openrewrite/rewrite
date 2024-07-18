/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddKeyValue extends Recipe {

    @Option(displayName = "Key path",
        description = "A JsonPath expression to locate the *parent* JSON entry.",
        example = "'$.subjects.*' or '$.' or '$.x[1].y.*' etc.")
    String keyPath;

    @Option(displayName = "Key",
        description = "The key to create.",
        example = "myKey")
    String key;

    @Option(displayName = "Value",
        description = "The value to add to the array at the specified key. Can be of any type." +
                      " String values should be quoted to be inserted as Strings.",
        example = "\"myValue\" or '{\"a\": 1}' or '[ 123 ]'")
    String value;

    @Option(displayName = "Prepend",
        required = false,
        description = "If set to `true` the value will be added to the beginning of the object")
    boolean prepend;

    @Override
    public String getDisplayName() {
        return "Add value to JSON Object";
    }

    @Override
    public String getDescription() {
        return "Adds a `value` at the specified `keyPath` with the specified `key`, if the key doesn't already exist.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher pathMatcher = new JsonPathMatcher(keyPath);

        return new JsonIsoVisitor<ExecutionContext>() {

            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, ExecutionContext ctx) {
                obj = super.visitObject(obj, ctx);

                if (pathMatcher.matches(getCursor()) && objectDoesNotContainKey(obj, key)) {

                    boolean jsonIsEmpty = obj.getMembers().isEmpty() || obj.getMembers().get(0) instanceof Json.Empty;
                    Space space = jsonIsEmpty ? Space.EMPTY : obj.getMembers().get(0).getPrefix();

                    JsonRightPadded<JsonKey> newKey = rightPaddedKey();
                    Json.Literal newValue = valueLiteral();
                    Json newMember = new Json.Member(randomId(), space, Markers.EMPTY, newKey, newValue);

                    List<Json> members = jsonIsEmpty ? new ArrayList<>() : obj.getMembers();

                    if (prepend) {
                        members.add(0, newMember);
                    } else {
                        members.add(newMember);
                    }

                    return obj.withMembers(members);
                }
                return obj;
            }

            private Json.Literal valueLiteral() {
                return new Json.Literal(randomId(), Space.build(" ", emptyList()), Markers.EMPTY, value, unQuote(value));
            }

            private JsonRightPadded<JsonKey> rightPaddedKey() {
                return new JsonRightPadded<>(
                    new Json.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "\"" + key + "\"", key),
                    Space.EMPTY, Markers.EMPTY
                );
            }

            private String unQuote(String value) {
                if (value.startsWith("'") || value.startsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }

            private boolean objectDoesNotContainKey(Json.JsonObject obj, String key) {
                for (Json member : obj.getMembers()) {
                    if (member instanceof Json.Member) {
                        if (keyMatches(((Json.Member) member).getKey(), key)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            private boolean keyMatches(JsonKey jsonKey, String key) {
                if (jsonKey instanceof Json.Literal) {
                    return key.equals(((Json.Literal) jsonKey).getValue());
                } else if (jsonKey instanceof Json.Identifier) {
                    return key.equals(((Json.Identifier) jsonKey).getName());
                }
                throw new IllegalStateException("Key is not 'Json.Literal' or 'Json.Identifier': " + jsonKey);
            }

        };
    }
}
