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
package org.openrewrite.text;

import lombok.*;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.ListUtils.nullIfEmpty;

/**
 * The simplest of all ASTs representing nothing more than just unstructured text.
 */
@Value
@Builder
@AllArgsConstructor
public class PlainText implements SourceFile, Tree {

    @Builder.Default
    @With
    UUID id = Tree.randomId();

    @With
    Path sourcePath;

    @Builder.Default
    @With
    Markers markers = Markers.EMPTY;

    @Nullable // for backwards compatibility
    @With(AccessLevel.PRIVATE)
    String charsetName;

    @With
    boolean charsetBomMarked;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    @With
    @Nullable
    FileAttributes fileAttributes;

    @With
    @Getter
    @Nullable
    Checksum checksum;

    @Builder.Default
    String text = "";

    public PlainText withText(String text) {
        if (!text.equals(this.text)) {
            return new PlainText(this.id, this.sourcePath, this.markers, this.charsetName, this.charsetBomMarked,
                    this.fileAttributes, this.checksum, text, this.snippets);
        }
        return this;
    }

    List<Snippet> snippets;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(PlainTextVisitor.class);
    }


    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) v.adapt(PlainTextVisitor.class).visitText(this, p);
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new PlainTextPrinter<>();
    }

    @Override
    public long getWeight(Predicate<Object> uniqueIdentity) {
        return text.length() / 10;
    }

    public List<Snippet> getSnippets() {
        if (snippets == null) {
            return emptyList();
        }
        return snippets;
    }

    public PlainText withSnippets(@Nullable List<Snippet> snippets) {
        snippets = nullIfEmpty(snippets);
        if (this.snippets == snippets) {
            return this;
        }
        return new PlainText(id, sourcePath, markers, charsetName, charsetBomMarked, fileAttributes, checksum, text, snippets);
    }

    @Value
    @With
    public static class Snippet implements Tree {
        UUID id;

        Markers markers;

        String text;

        @Override
        public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
            return v.isAdaptableTo(PlainTextVisitor.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
            return (R) v.adapt(PlainTextVisitor.class).visitSnippet(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new PlainTextPrinter<>();
        }
    }
}
