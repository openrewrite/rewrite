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

import org.jetbrains.annotations.NotNull;
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;

public class TabsAndIndentsVisitor<P> extends JsonIsoVisitor<P> {
    private final String singleIndent;
    private final GeneralFormatStyle generalFormatStyle;
    private final WrappingAndBracesStyle wrappingAndBracesStyle;
    private final String objectsWrappingDelimiter;
    private final String arrayWrappingDelimiter;

    @Nullable
    private final Tree stopAfter;

    public TabsAndIndentsVisitor(WrappingAndBracesStyle wrappingAndBracesStyle,
                                 TabsAndIndentsStyle tabsAndIndentsStyle,
                                 GeneralFormatStyle generalFormatStyle,
                                 @Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;

        if (tabsAndIndentsStyle.getUseTabCharacter()) {
            this.singleIndent = "\t";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < tabsAndIndentsStyle.getIndentSize(); j++) {
                sb.append(" ");
            }
            singleIndent = sb.toString();
        }

        this.objectsWrappingDelimiter = wrappingAndBracesStyle.getWrapObjects().delimiter(generalFormatStyle);
        this.arrayWrappingDelimiter = wrappingAndBracesStyle.getWrapArrays().delimiter(generalFormatStyle);
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
            members = ensureCollectionHasIndents(members, wrappingSetting);
            members = applyWrappingStyleToSuffixes(members, wrappingSetting, relativeIndent);
            json = obj.getPadding().withMembers(members);
        }
        if (json instanceof Json.Array) {
            Json.Array array = (Json.Array) json;
            List<JsonRightPadded<JsonValue>> members = array.getPadding().getValues();
            LineWrapSetting wrappingSetting = this.wrappingAndBracesStyle.getWrapArrays();
            members = ensureCollectionHasIndents(members, wrappingSetting);
            members = applyWrappingStyleToSuffixes(members, wrappingSetting, relativeIndent);
            json = array.getPadding().withValues(members);
        }
        return json;
    }

    private @NotNull <J extends Json> List<JsonRightPadded<J>> ensureCollectionHasIndents(List<JsonRightPadded<J>> elements, LineWrapSetting wrapping) {
        AtomicInteger i = new AtomicInteger(0);
        return ListUtils.map(elements, elem -> {
            boolean isFirst = i.get() == 0;
            i.incrementAndGet();
            Space prefix = elem.getElement().getPrefix();
            final String newPrefixString;
            String currentAfterNewLine = prefix.getWhitespaceIndent();
            if (wrapping == LineWrapSetting.DoNotWrap && isFirst) {
                newPrefixString = "";
            } else if (wrapping == LineWrapSetting.DoNotWrap) {
                newPrefixString = " ";
            } else if (wrapping == LineWrapSetting.WrapAlways) {
                newPrefixString = this.generalFormatStyle.newLine() + currentAfterNewLine;
            } else {
                throw new UnsupportedOperationException("Unknown LineWrapSetting: " + wrapping);
            }
            if (!newPrefixString.equals(prefix.getWhitespace())) {
                return elem.withElement(elem.getElement().withPrefix(prefix.withWhitespace((newPrefixString))));
            } else {
                return elem;
            }
        });
    }

    private <JS extends Json> List<JsonRightPadded<JS>> applyWrappingStyleToSuffixes(List<JsonRightPadded<JS>> elements, LineWrapSetting wrapping, String relativeIndent) {
        AtomicInteger i = new AtomicInteger(0);
        return ListUtils.map(elements, elem -> {
            // TODO Refactor the isLast logic into `mapLast`
            boolean isLast = i.get() == elements.size() - 1;
            i.incrementAndGet();

            String currentAfter = elem.getAfter().getWhitespace();
            final String newAfter;
            if (isLast) {
                if (wrapping == LineWrapSetting.DoNotWrap) {
                    newAfter = "";
                } else if (wrapping == LineWrapSetting.WrapAlways) {
                    newAfter = this.generalFormatStyle.newLine() + relativeIndent;
                } else {
                    throw new UnsupportedOperationException("Unknown LineWrapSetting: " + wrapping);
                }
            } else {
                newAfter = "";
            }
            if (!newAfter.equals(currentAfter)) {
                return elem.withAfter(Space.build(newAfter, emptyList()));
            } else {
                return elem;
            }
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
