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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.style.Autodetect;
import org.openrewrite.yaml.style.IndentsStyle;
import org.openrewrite.yaml.style.YamlDefaultStyles;
import org.openrewrite.yaml.tree.Yaml;

public class AutoFormatVisitor<P> extends YamlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Yaml preVisit(Yaml tree, P p) {
        stopAfterPreVisit();
        Yaml.Documents docs = getCursor().firstEnclosingOrThrow(Yaml.Documents.class);
        Cursor cursor = getCursor().getParentOrThrow();

        Yaml y = new NormalizeFormatVisitor<>(stopAfter).visitNonNull(tree, p, cursor.fork());

        y = new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(y, p, cursor.fork());

        y = new IndentsVisitor<>(
                Style.from(IndentsStyle.class, docs, () -> Autodetect.tabsAndIndents(docs, YamlDefaultStyles.indents())),
                    stopAfter)
                .visitNonNull(y, p, cursor.fork());

        return new NormalizeLineBreaksVisitor<>(
                Style.from(GeneralFormatStyle.class, docs, () -> Autodetect.generalFormat(docs)),
                stopAfter)
                .visitNonNull(y, p, cursor.fork());
    }

    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, P p) {
        Yaml.Documents y = (Yaml.Documents) new NormalizeFormatVisitor<>(stopAfter).visitNonNull(documents, p);

        y = (Yaml.Documents) new MinimumViableSpacingVisitor<>(stopAfter).visitNonNull(y, p);

        y = (Yaml.Documents) new IndentsVisitor<>(
                Style.from(IndentsStyle.class, documents, () -> Autodetect.tabsAndIndents(documents, YamlDefaultStyles.indents())),
                stopAfter)
                .visitNonNull(y, p);

        return (Yaml.Documents) new NormalizeLineBreaksVisitor<>(
                Style.from(GeneralFormatStyle.class, documents, () -> Autodetect.generalFormat(documents)),
                stopAfter)
                .visitNonNull(y, p);
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
