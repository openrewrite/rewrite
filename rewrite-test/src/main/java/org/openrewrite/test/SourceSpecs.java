/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.quark.Quark;
import org.openrewrite.quark.QuarkParser;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface SourceSpecs extends Iterable<SourceSpec<?>> {
    static SourceSpecs dir(String dir, SourceSpecs... sources) {
        return dir(dir, s -> {
        }, sources);
    }

    static SourceSpecs dir(String dir, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return new Dir(dir, spec, sources);
    }

    static SourceSpecs other(@Nullable String before) {
        return other(before, s -> {
        });
    }

    static SourceSpecs other(@Nullable String before, Consumer<SourceSpec<Quark>> spec) {
        SourceSpec<Quark> quark = new SourceSpec<>(Quark.class, null, QuarkParser.builder(), before, null);
        spec.accept(quark);
        return quark;
    }

    static SourceSpecs other(@Nullable String before, @Nullable String after) {
        return other(before, after, s -> {
        });
    }

    static SourceSpecs other(@Nullable String before, @Nullable String after,
                             Consumer<SourceSpec<Quark>> spec) {
        SourceSpec<Quark> quark = new SourceSpec<>(Quark.class, null, QuarkParser.builder(), before, s -> after);
        spec.accept(quark);
        return quark;
    }

    static SourceSpecs text(@Nullable String before) {
        return text(before, s -> {
        });
    }

    static SourceSpecs text(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, PlainTextParser.builder(), before, null);
        spec.accept(text);
        return text;
    }

    static SourceSpecs text(@Nullable String before, @Nullable String after) {
        return text(before, after, s -> {
        });
    }

    static SourceSpecs text(@Nullable String before, @Nullable String after,
                            Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, PlainTextParser.builder(), before, s-> after);
        spec.accept(text);
        return text;
    }
}
