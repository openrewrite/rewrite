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
package org.openrewrite.yaml.format;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.style.Autodetect;
import org.openrewrite.yaml.style.IndentsStyle;
import org.openrewrite.yaml.style.YamlDefaultStyles;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Optional;

public class AutoFormatVisitor<P> extends YamlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable Yaml preVisit(Yaml tree, P p) {
        stopAfterPreVisit();
        Yaml.Documents docs = getCursor().firstEnclosingOrThrow(Yaml.Documents.class);
        Cursor cursor = getCursor().getParentOrThrow();

        Yaml y = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        y = new MinimumViableSpacingVisitor<>(stopAfter).visit(y, p, cursor.fork());

        y = new IndentsVisitor<>(Optional.ofNullable(docs.getStyle(IndentsStyle.class))
                .orElse(Autodetect.tabsAndIndents(docs, YamlDefaultStyles.indents())), stopAfter)
                .visit(y, p, cursor.fork());

        y = new NormalizeLineBreaksVisitor<>(Optional.ofNullable(docs.getStyle(GeneralFormatStyle.class))
                .orElse(Autodetect.generalFormat(docs)), stopAfter)
                .visit(y, p, cursor.fork());

        return y;
    }

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, P p) {
        Yaml.Documents y = (Yaml.Documents) new NormalizeFormatVisitor<>(stopAfter).visit(documents, p);

        y = (Yaml.Documents) new MinimumViableSpacingVisitor<>(stopAfter).visit(y, p);

        y = (Yaml.Documents) new IndentsVisitor<>(Optional.ofNullable(documents.getStyle(IndentsStyle.class))
                .orElse(Autodetect.tabsAndIndents(y, YamlDefaultStyles.indents())), stopAfter)
                .visit(documents, p);

        y = (Yaml.Documents) new NormalizeLineBreaksVisitor<>(Optional.ofNullable(documents.getStyle(GeneralFormatStyle.class))
                .orElse(Autodetect.generalFormat(y)), stopAfter)
                .visit(documents, p);

        assert y != null;
        return y;
    }

    @Override
    public @Nullable Yaml postVisit(Yaml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Yaml.Documents.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Yaml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Yaml) tree;
        }
        return super.visit(tree, p);
    }
}
