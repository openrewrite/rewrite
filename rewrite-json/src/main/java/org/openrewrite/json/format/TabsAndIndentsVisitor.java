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
package org.openrewrite.json.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.json.style.WrappingAndBracesStyle;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class TabsAndIndentsVisitor<P> extends JsonIsoVisitor<P> {
    private final String singleIndent;
    private final GeneralFormatStyle generalFormatStyle;
    private final WrappingAndBracesStyle wrappingAndBracesStyle;

    @Nullable
    private final Tree stopAfter;

    public TabsAndIndentsVisitor(WrappingAndBracesStyle wrappingAndBracesStyle,
                                 TabsAndIndentsStyle tabsAndIndentsStyle,
                                 GeneralFormatStyle generalFormatStyle,
                                 @Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;

        this.singleIndent = tabsAndIndentsStyle.singleIndent();
        this.wrappingAndBracesStyle = wrappingAndBracesStyle;
        this.generalFormatStyle = generalFormatStyle;
    }

    @Override
    public Json preVisit(Json tree, P p) {
        Json json = super.preVisit(tree, p);
        if (tree instanceof Json.JsonObject || tree instanceof Json.Array) {
            String newIndent = getCurrentIndent() + this.singleIndent;
            getCursor().putMessage("indentToUse", newIndent);
        }
        return json;
    }

    @Override
    public @Nullable Json postVisit(Json tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Json.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Json visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Json) tree;
        }
        Json json = super.visit(tree, p);

        final String relativeIndent = getCurrentIndent();

        if (json != null) {
            final String ws = json.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                String shiftedPrefix = combineIndent(ws, relativeIndent);
                if (!shiftedPrefix.equals(ws)) {
                    json = json.withPrefix(json.getPrefix().withWhitespace(shiftedPrefix));
                }
            }
        }
        if (json instanceof Json.JsonObject) {
            Json.JsonObject obj = (Json.JsonObject) json;
            List<JsonRightPadded<Json>> members = obj.getPadding().getMembers();
            LineWrapSetting wrappingSetting = this.wrappingAndBracesStyle.getWrapObjects();
            if (!WrappingAndBracesVisitor.isEmpty(members)) {
                members = applyWrappingStyleToLastChildSuffix(members, wrappingSetting, relativeIndent);
            }
            json = obj.getPadding().withMembers(members);
        }

        if (json instanceof Json.Array) {
            Json.Array array = (Json.Array) json;
            List<JsonRightPadded<JsonValue>> members = array.getPadding().getValues();
            LineWrapSetting wrappingSetting = this.wrappingAndBracesStyle.getWrapArrays();
            if (!WrappingAndBracesVisitor.isEmpty(members)) {
                members = applyWrappingStyleToLastChildSuffix(members, wrappingSetting, relativeIndent);
            }
            json = array.getPadding().withValues(members);
        }

        return json;
    }

    private <JS extends Json> List<JsonRightPadded<JS>> applyWrappingStyleToLastChildSuffix(List<JsonRightPadded<JS>> elements, LineWrapSetting wrapping, String relativeIndent) {
        return ListUtils.mapLast(elements, elem -> {
            String currentAfter = elem.getAfter().getWhitespace();
            final String newAfter;
            if (wrapping == LineWrapSetting.DoNotWrap) {
                newAfter = "";
            } else if (wrapping == LineWrapSetting.WrapAlways) {
                newAfter = this.generalFormatStyle.newLine() + relativeIndent;
            } else {
                throw new UnsupportedOperationException("Unknown LineWrapSetting: " + wrapping);
            }
            if (!newAfter.equals(currentAfter) && elem.getAfter().getComments().isEmpty()) {
                return elem.withAfter(Space.build(newAfter, emptyList()));
            }
            return elem;
        });
    }

    private String getCurrentIndent() {
        String ret = getCursor().getNearestMessage("indentToUse");
        if (ret == null) {
            // This is basically the first object we visit, not necessarily the root-level object as we might be
            // visiting only partial tree.
            Optional<Json> containingNode = getCursor().getPathAsStream().filter(obj ->
                    (obj instanceof Json) && ((Json) obj).getPrefix().getWhitespace().contains("\n")
            ).findFirst().map(obj -> (Json) obj);
            return containingNode.map(node -> node.getPrefix().getWhitespaceIndent()).orElse("");
        }
        return ret;
    }

    private String combineIndent(String oldWs, String relativeIndent) {
        return oldWs.substring(0, oldWs.lastIndexOf('\n') + 1) + relativeIndent;
    }
}
