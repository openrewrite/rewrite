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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.style.Autodetect;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.json.tree.Json;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;

import java.util.Optional;

import static java.util.Collections.singletonList;

public class AutoFormatVisitor<P> extends JsonIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Json preVisit(Json tree, P p) {
        stopAfterPreVisit();
        Json.Document doc = getCursor().firstEnclosingOrThrow(Json.Document.class);
        Cursor cursor = getCursor().getParentOrThrow();
        Autodetect autodetectedStyle = Autodetect.detector().sample(doc).build();
        Json js = tree;

        TabsAndIndentsStyle taiStyle = Optional.ofNullable(doc.getStyle(TabsAndIndentsStyle.class))
                .orElseGet(() -> NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(autodetectedStyle)));
        assert(taiStyle != null);
        js = new TabsAndIndentsVisitor<>(taiStyle, stopAfter).visitNonNull(js, p, cursor.fork());

        GeneralFormatStyle gfStyle = Optional.ofNullable(doc.getStyle(GeneralFormatStyle.class))
                        .orElseGet(() -> NamedStyles.merge(GeneralFormatStyle.class, singletonList(autodetectedStyle)));
        assert(gfStyle != null);
        js = new NormalizeLineBreaksVisitor<>(gfStyle, stopAfter).visitNonNull(js, p, cursor.fork());

        return js;
    }

    @Override
    public Json.Document visitDocument(Json.Document js, P p) {
        Autodetect autodetectedStyle = Autodetect.detector().sample(js).build();

        TabsAndIndentsStyle taiStyle = Optional.ofNullable(js.getStyle(TabsAndIndentsStyle.class))
                .orElseGet(() -> NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(autodetectedStyle)));
        assert(taiStyle != null);
        js = (Json.Document) new TabsAndIndentsVisitor<>(taiStyle, stopAfter).visitNonNull(js, p);

        GeneralFormatStyle gfStyle = Optional.ofNullable(js.getStyle(GeneralFormatStyle.class))
                .orElseGet(() -> NamedStyles.merge(GeneralFormatStyle.class, singletonList(autodetectedStyle)));
        assert(gfStyle != null);
        js = (Json.Document) new NormalizeLineBreaksVisitor<>(gfStyle, stopAfter).visitNonNull(js, p);

        return js;
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
