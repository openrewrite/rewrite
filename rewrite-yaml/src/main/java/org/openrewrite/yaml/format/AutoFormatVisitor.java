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
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.style.Autodetect;
import org.openrewrite.yaml.style.YamlDefaultStyles;
import org.openrewrite.yaml.style.IndentsStyle;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Optional;

public class AutoFormatVisitor<P> extends YamlVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable Yaml visit(@Nullable Tree tree, P p, Cursor cursor) {
        Yaml docs = cursor.firstEnclosingOrThrow(Yaml.Documents.class);

        docs = new IndentsVisitor<>(Optional.ofNullable(docs.getStyle(IndentsStyle.class))
                .orElse(Autodetect.tabsAndIndents(docs, YamlDefaultStyles.indents())), stopAfter)
                .visit(docs, p, cursor);

        return docs;
    }
}
