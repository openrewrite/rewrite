/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.properties.tree;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.internal.PropertiesPrinter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public interface Properties extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptProperties(v.adapt(PropertiesVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(PropertiesVisitor.class);
    }

    @Nullable
    default <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    String getPrefix();

    Properties withPrefix(String prefix);

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class File implements Properties, SourceFile {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;
        @With
        Markers markers;
        @With
        Path sourcePath;

        @With
        List<Content> content;
        @With
        String eof;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        boolean charsetBomMarked;

        @With
        @Nullable
        FileAttributes fileAttributes;

        @With
        @Nullable
        Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitFile(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new PropertiesPrinter<>();
        }
    }

    interface Content extends Properties {
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Entry implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        String key;

        /**
         * Automatically, removes continuations from the text.
         * @return the text value without continuations.
         */
        public String getKey() {
            return Continuation.getValue(key);
        }

        /**
         * @return the text with continuations.
         */
        public String getKeySource() {
            return key;
        }

        String beforeEquals;

        @Nullable
        Delimiter delimiter;

        Value value;

        public Delimiter getDelimiter() {
            return delimiter == null ? Delimiter.EQUALS : delimiter;
        }

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitEntry(this, p);
        }

        @Getter
        public enum Delimiter {
            COLON(':'), EQUALS('='), NONE('\0');

            private final Character character;

            Delimiter(Character character) {
                this.character = character;
            }

            public static Delimiter getDelimiter(String value) {
                return "=".equals(value.trim()) ? Delimiter.EQUALS :
                            ":".equals(value.trim()) ? Delimiter.COLON :
                            "".equals(value.trim()) ? Delimiter.NONE :
                                    Delimiter.EQUALS;
            }
        }
    }

    /**
     * Note that this class cannot easily be made to implement {@link Properties} like it should,
     * because existing serialized ASTs will not have a {@link com.fasterxml.jackson.annotation.JsonIdentityInfo}
     * reference to deserialize into the type.
     */
    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Value {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        String text;

        /**
         * Automatically, removes continuations from the text.
         * @return the text value without continuations.
         */
        public String getText() {
            return Continuation.getValue(text);
        }

        /**
         * @return the text with continuations.
         */
        public String getSource() {
            return text;
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        Delimiter delimiter;
        String message;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }

        @Getter
        public enum Delimiter {
            HASH_TAG('#'), EXCLAMATION_MARK('!');

            private final Character character;

            Delimiter(Character character) {
                this.character = character;
            }
        }
    }

    class Continuation {
        private static final Pattern LINE_CONTINUATION_PATTERN = Pattern.compile("\\\\\\R\\s*");
        static String getValue(String input) {
            return LINE_CONTINUATION_PATTERN.matcher(input).replaceAll("");
        }
    }
}
