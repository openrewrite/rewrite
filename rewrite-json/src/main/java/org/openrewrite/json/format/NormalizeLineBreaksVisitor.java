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
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;

import static org.openrewrite.format.LineBreaks.normalizeNewLines;

public class NormalizeLineBreaksVisitor<P> extends JsonIsoVisitor<P> {
    private final GeneralFormatStyle generalFormatStyle;

    @Nullable
    private final Tree stopAfter;

    public NormalizeLineBreaksVisitor(GeneralFormatStyle generalFormatStyle, @Nullable Tree stopAfter) {
        this.generalFormatStyle = generalFormatStyle;
        this.stopAfter = stopAfter;
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

        Json y = super.visit(tree, p);
        if (y != null) {
            String modifiedWs = normalizeNewLines(y.getPrefix().getWhitespace(), generalFormatStyle.isUseCRLFNewLines());
            if (!y.getPrefix().getWhitespace().equals(modifiedWs)) {
                y = y.withPrefix(y.getPrefix().withWhitespace(modifiedWs));
            }
        }
        return y;
    }
}
