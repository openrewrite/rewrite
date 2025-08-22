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

public class WrappingAndBracesVisitor<P> extends JsonIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final String singleIndent;
    private final WrappingAndBracesStyle wrappingAndBracesStyle;
    private final GeneralFormatStyle generalFormatStyle;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle wrappingAndBracesStyle,
                                    TabsAndIndentsStyle tabsAndIndentsStyle,
                                    GeneralFormatStyle generalFormatStyle,
                                    @Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;

        this.singleIndent = tabsAndIndentsStyle.singleIndent();
        this.wrappingAndBracesStyle = wrappingAndBracesStyle;
        this.generalFormatStyle = generalFormatStyle;
    }

    @Override
    public Json.JsonObject visitObject(Json.JsonObject obj, P p) {
        Json.JsonObject ret = super.visitObject(obj, p);
        List<JsonRightPadded<Json>> members = ret.getPadding().getMembers();
        LineWrapSetting wrappingSetting = this.wrappingAndBracesStyle.getWrapObjects();

        if (!isEmpty(members)) {
            members = ensureCollectionHasIndents(members, wrappingSetting);
            members = applyWrappingStyleToSuffixes(members, wrappingSetting, getCurrentIndent());
        } else {
            members = collapseToNoSpaceIfEmpty(members);
        }
        return ret.getPadding().withMembers(members);
    }

    @Override
    public Json.Array visitArray(Json.Array array, P p) {
        Json.Array ret = super.visitArray(array, p);
        List<JsonRightPadded<JsonValue>> members = ret.getPadding().getValues();
        LineWrapSetting wrappingSetting = this.wrappingAndBracesStyle.getWrapArrays();

        if (!isEmpty(members)) {
            members = ensureCollectionHasIndents(members, wrappingSetting);
            members = applyWrappingStyleToSuffixes(members, wrappingSetting, getCurrentIndent());
        } else {
            members = collapseToNoSpaceIfEmpty(members);
        }
        return ret.getPadding().withValues(members);
    }

    @Override
    public @Nullable Json postVisit(Json tree, P p) {
        if (tree instanceof Json.JsonObject || tree instanceof Json.Array) {
            String newIndent = getCurrentIndent() + this.singleIndent;
            getCursor().putMessage("indentToUse", newIndent);
        }

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
        return super.visit(tree, p);
    }

    private <J extends Json> List<JsonRightPadded<J>> ensureCollectionHasIndents(List<JsonRightPadded<J>> elements, LineWrapSetting wrapping) {
        return ListUtils.map(elements, (Integer idx, JsonRightPadded<J> elem) -> {
            Space prefix = elem.getElement().getPrefix();
            final String newPrefixString;
            String currentAfterNewLine = prefix.getWhitespaceIndent();
            if (wrapping == LineWrapSetting.DoNotWrap && idx == 0) {
                newPrefixString = "";
            } else if (wrapping == LineWrapSetting.DoNotWrap) {
                newPrefixString = " ";
            } else if (wrapping == LineWrapSetting.WrapAlways) {
                newPrefixString = this.generalFormatStyle.newLine() + currentAfterNewLine;
            } else {
                throw new UnsupportedOperationException("Unknown LineWrapSetting: " + wrapping);
            }
            if (!newPrefixString.equals(prefix.getWhitespace()) && elem.getAfter().getComments().isEmpty()) {
                return elem.withElement(elem.getElement().withPrefix(prefix.withWhitespace((newPrefixString))));
            }
            return elem;
        });
    }

    private <JS extends Json> List<JsonRightPadded<JS>> applyWrappingStyleToSuffixes(List<JsonRightPadded<JS>> elements, LineWrapSetting wrapping, String relativeIndent) {
        return ListUtils.map(elements, (Integer idx, JsonRightPadded<JS> elem) -> {
            String currentAfter = elem.getAfter().getWhitespace();
            final String newAfter;
            if (idx == elements.size() - 1) {
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
            if (!newAfter.equals(currentAfter) && elem.getAfter().getComments().isEmpty()) {
                return elem.withAfter(Space.build(newAfter, elem.getAfter().getComments()));
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

    public static <JS extends Json> boolean isEmpty(List<JsonRightPadded<JS>> list) {
        return list.size() == 1 &&
                (list.get(0).getElement() instanceof Json.Empty) &&
                (list.get(0).getElement().getPrefix().getComments().isEmpty()) &&
                (list.get(0).getAfter().getComments().isEmpty());
    }

    private <JS extends Json> List<JsonRightPadded<JS>> collapseToNoSpaceIfEmpty(List<JsonRightPadded<JS>> list) {
        if (isEmpty(list)) {
            return ListUtils.map(list, right -> right.withElement(right.getElement().withPrefix(Space.EMPTY)).withAfter(Space.EMPTY));
        }
        return list;
    }
}
