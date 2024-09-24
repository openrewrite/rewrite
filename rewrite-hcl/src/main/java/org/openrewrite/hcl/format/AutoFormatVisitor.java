/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.hcl.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.style.BracketsStyle;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.style.TabsAndIndentsStyle;
import org.openrewrite.hcl.tree.Hcl;

import java.util.Optional;

public class AutoFormatVisitor<P> extends HclVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable Hcl preVisit(Hcl tree, P p) {
        stopAfterPreVisit();
        Cursor cursor = getCursor().getParentOrThrow();
        Hcl.ConfigFile cf = getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class);

        Hcl t = new NormalizeFormatVisitor<>().visit(tree, p, cursor.fork());

        t = new BracketsVisitor<>(Optional.ofNullable(cf.getStyle(BracketsStyle.class))
                .orElse(BracketsStyle.DEFAULT), stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(Optional.ofNullable(cf.getStyle(TabsAndIndentsStyle.class))
                .orElse(TabsAndIndentsStyle.DEFAULT), stopAfter)
                .visit(t, p, cursor.fork());

        t = new SpacesVisitor<>(Optional.ofNullable(cf.getStyle(SpacesStyle.class))
                .orElse(SpacesStyle.DEFAULT), stopAfter)
                .visit(t, p, cursor.fork());

        t = new BlankLinesVisitor<>(Optional.ofNullable(cf.getStyle(BlankLinesStyle.class))
                .orElse(BlankLinesStyle.DEFAULT), stopAfter)
                .visit(t, p, cursor.fork());

        if (t instanceof Hcl.ConfigFile) {
            t = visitConfigFile((Hcl.ConfigFile) t, p);
        }

        return t;
    }

    @Override
    public Hcl visitConfigFile(Hcl.ConfigFile cf, P p) {
        Hcl.ConfigFile t = (Hcl.ConfigFile) new RemoveTrailingWhitespaceVisitor<>().visit(cf, p);

        t = (Hcl.ConfigFile) new NormalizeFormatVisitor<>().visit(t, p);

        t = (Hcl.ConfigFile) new BracketsVisitor<>(Optional.ofNullable(cf.getStyle(BracketsStyle.class))
                .orElse(BracketsStyle.DEFAULT), stopAfter)
                .visit(t, p);

        t = (Hcl.ConfigFile) new TabsAndIndentsVisitor<>(Optional.ofNullable(cf.getStyle(TabsAndIndentsStyle.class))
                .orElse(TabsAndIndentsStyle.DEFAULT), stopAfter)
                .visit(t, p);

        t = (Hcl.ConfigFile) new SpacesVisitor<>(Optional.ofNullable(cf.getStyle(SpacesStyle.class))
                .orElse(SpacesStyle.DEFAULT), stopAfter)
                .visit(t, p);

        t = (Hcl.ConfigFile) new BlankLinesVisitor<>(Optional.ofNullable(cf.getStyle(BlankLinesStyle.class))
                .orElse(BlankLinesStyle.DEFAULT), stopAfter)
                .visit(t, p);

        assert t != null;
        return t;
    }
}
