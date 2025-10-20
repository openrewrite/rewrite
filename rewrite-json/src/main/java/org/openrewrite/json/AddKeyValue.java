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
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.json.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
            description = "The value to add to the document at the specified key. Can be of any type representing JSON value." +
                          " String values should be quoted to be inserted as Strings.",
            example = "`\"myValue\"` or `{\"a\": 1}` or `[ 123 ]`")
    @Language("json")
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
        return new JsonIsoVisitor<ExecutionContext>() {
            private final JsonPathMatcher pathMatcher = new JsonPathMatcher(keyPath);

            @Override
            public Json.JsonObject visitObject(Json.JsonObject obj, ExecutionContext ctx) {
                obj = super.visitObject(obj, ctx);

                if (pathMatcher.matches(getCursor()) && objectDoesNotContainKey(obj, key)) {
                    List<Json> originalMembers = obj.getMembers();
                    boolean jsonIsEmpty = originalMembers.isEmpty() || originalMembers.get(0) instanceof Json.Empty;
                    Space space = jsonIsEmpty || prepend ? originalMembers.get(0).getPrefix() : Space.build("\n", emptyList());

                    Json newMember = new Json.Member(randomId(), space, Markers.EMPTY, rightPaddedKey(), parsedValue());

                    if (jsonIsEmpty) {
                        return autoFormat(obj.withMembers(singletonList(newMember)), ctx, getCursor().getParent());
                    }

                    List<Json> newMembers = prepend ?
                            ListUtils.concat(newMember, originalMembers) :
                            ListUtils.concat(originalMembers, newMember);
                    return autoFormat(obj.withMembers(newMembers), ctx, getCursor().getParent());
                }
                return obj;
            }

            private JsonValue parsedValue() {
                Json.Document parsedDoc = (Json.Document) JsonParser.builder().build()
                        .parse(value.trim()).findFirst().get();
                JsonValue value = parsedDoc.getValue();
                return value.withPrefix(value.getPrefix().withWhitespace(" "));
            }

            private JsonRightPadded<JsonKey> rightPaddedKey() {
                return new JsonRightPadded<>(
                        new Json.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "\"" + key + "\"", key),
                        Space.EMPTY, Markers.EMPTY
                );
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
                return false;
            }
        };
    }
}
