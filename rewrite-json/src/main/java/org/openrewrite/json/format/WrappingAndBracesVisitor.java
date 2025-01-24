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
import org.openrewrite.json.style.WrappingAndBracesStyle;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;

import static java.util.Collections.emptyList;

public class WrappingAndBracesVisitor<P> extends JsonIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final WrappingAndBracesStyle wrappingAndBracesStyle;
    private final GeneralFormatStyle generalFormatStyle;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle wrappingAndBracesStyle,
                                    GeneralFormatStyle generalFormatStyle,
                                    @Nullable Tree stopAfter) {
        this.wrappingAndBracesStyle = wrappingAndBracesStyle;
        this.generalFormatStyle = generalFormatStyle;
        this.stopAfter = stopAfter;
    }

    @Override
    public Json.JsonObject visitObject(Json.JsonObject obj, P p) {
        Json.JsonObject ret = super.visitObject(obj, p);
        final String possibleNewLine = this.wrappingAndBracesStyle.getWrapObjects().delimiter(generalFormatStyle);
        List<JsonRightPadded<Json>> members = ret.getPadding().getMembers();
        members = applyWrappingStyleToLastChildSuffix(members, possibleNewLine);
        members = applyWrappingStyleToPrefixes(members, possibleNewLine);
        return ret.getPadding().withMembers(members);
    }

    @Override
    public Json.Array visitArray(Json.Array array, P p) {
        Json.Array ret = super.visitArray(array, p);
        final String possibleNewLine = this.wrappingAndBracesStyle.getWrapArrays().delimiter(generalFormatStyle);
        List<JsonRightPadded<JsonValue>> members = ret.getPadding().getValues();
        members = applyWrappingStyleToLastChildSuffix(members, possibleNewLine);
        members = applyWrappingStyleToPrefixes(members, possibleNewLine);
        return ret.getPadding().withValues(members);
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
        return super.visit(tree, p);
    }

    private static <JS extends Json> List<JsonRightPadded<JS>> applyWrappingStyleToPrefixes(List<JsonRightPadded<JS>> elements, String possibleNewLine) {
        return ListUtils.map(elements, elem -> {
            String oldAfterNewLine = elem.getElement().getPrefix().getWhitespaceIndent();
            String newPrefix = possibleNewLine + oldAfterNewLine;
            if (!newPrefix.equals(elem.getElement().getPrefix().getWhitespace())) {
                return elem.withElement(elem.getElement().withPrefix(Space.build(newPrefix, emptyList())));
            } else {
                return elem;
            }
        });
    }

    private static <JS extends Json> List<JsonRightPadded<JS>> applyWrappingStyleToLastChildSuffix(List<JsonRightPadded<JS>> elements, String possibleNewLine) {
        return ListUtils.mapLast(elements, last -> {
            String currentAfterNewLine = last.getAfter().getWhitespaceIndent();
            String currentAfter = last.getAfter().getWhitespace();
            String newAfter = possibleNewLine + currentAfterNewLine;
            if (!newAfter.equals(currentAfter)) {
                return last.withAfter(Space.build(newAfter, emptyList()));
            } else {
                return last;
            }
        });
    }
}
