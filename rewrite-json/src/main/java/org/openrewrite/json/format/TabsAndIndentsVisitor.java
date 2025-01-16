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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.json.tree.Json;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabsAndIndentsVisitor<P> extends JsonIsoVisitor<P> {

    private final TabsAndIndentsStyle style;

    @Nullable
    private final Tree stopAfter;

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public @NotNull Json preVisit(Json tree, P p) {
        Json json = super.preVisit(tree, p);
        if (json != null) {
            final String ws = json.getPrefix().getWhitespace();
            if (ws.contains("\n")) {
                int indentMultiple = (int) getCursor().getPathAsStream().filter(Json.JsonObject.class::isInstance).count();
                String shiftedPrefix = createIndent(ws, indentMultiple);
                if (!shiftedPrefix.equals(ws)) {
                    return json.withPrefix(json.getPrefix().withWhitespace(shiftedPrefix));
                }
            }
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
        return super.visit(tree, p);
    }

    private @NotNull String createIndent(String ws, int indentMultiple) {
        StringBuilder shiftedPrefixBuilder = new StringBuilder(ws.substring(0, ws.lastIndexOf('\n') + 1));
        for (int i = 0; i < indentMultiple; i++) {
            if (style.getUseTabCharacter()) {
                shiftedPrefixBuilder.append("\t");
            } else {
                for (int j = 0; j < style.getIndentSize(); j++) {
                    shiftedPrefixBuilder.append(" ");
                }
            }
        }

        return shiftedPrefixBuilder.toString();
    }
}
