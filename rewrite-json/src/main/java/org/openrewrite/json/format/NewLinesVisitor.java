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
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;

import java.util.List;

import static java.util.Collections.emptyList;

public class NewLinesVisitor<P> extends JsonIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public NewLinesVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Json.JsonObject visitObject(Json.JsonObject obj, P p) {
        Json.JsonObject ret = super.visitObject(obj, p);
        List<JsonRightPadded<Json>> members = ret.getPadding().getMembers();
        members = ListUtils.mapLast(members, last -> {
            String currentAfterNewLine = last.getAfter().getWhitespaceIndent();
            String currentAfter = last.getAfter().getWhitespace();
            String newAfter = "\n" + currentAfterNewLine;
            if (!newAfter.equals(currentAfter)) {
                return last.withAfter(Space.build(newAfter, emptyList()));
            } else {
                return last;
            }
        });
        members = ListUtils.map(members, elem -> {
            String oldAfterNewLine = elem.getElement().getPrefix().getWhitespaceIndent();
            String newPrefix = "\n" + oldAfterNewLine;
            if (!newPrefix.equals(elem.getElement().getPrefix().getWhitespace())) {
                return elem.withElement(elem.getElement().withPrefix(Space.build(newPrefix, emptyList())));
            } else {
                return elem;
            }
        });
        return ret.getPadding().withMembers(members);
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

}
