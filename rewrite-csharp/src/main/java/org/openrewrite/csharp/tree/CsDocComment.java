/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.csharp.CsDocCommentPrinter;
import org.openrewrite.csharp.CsDocCommentVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.UUID;

public interface CsDocComment extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptCsDocComment((CsDocCommentVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof CsDocCommentVisitor;
    }

    default <P> @Nullable CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new CsDocCommentPrinter<>();
    }

    /**
     * The root of a C# XML documentation comment. Implements {@link Comment} so it can be
     * stored in {@link org.openrewrite.java.tree.Space#getComments()}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class DocComment implements CsDocComment, Comment {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @With
        List<CsDocComment> body;

        String suffix;

        @Override
        @SuppressWarnings("unchecked")
        public DocComment withSuffix(String suffix) {
            if (!suffix.equals(this.suffix)) {
                return new DocComment(id, markers, body, suffix);
            }
            return this;
        }

        @Override
        public boolean isMultiline() {
            return true;
        }

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitDocComment(this, p);
        }

        @Override
        public <P> void printComment(Cursor cursor, PrintOutputCapture<P> print) {
            new CsDocCommentPrinter<P>().visit(this, print, cursor);
        }
    }

    /**
     * An XML element with opening and closing tags: {@code <tag attr="val">content</tag>}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlElement implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<CsDocComment> attributes;
        List<CsDocComment> spaceBeforeClose;
        List<CsDocComment> content;
        List<CsDocComment> closingTagSpaceBeforeClose;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlElement(this, p);
        }
    }

    /**
     * A self-closing XML element: {@code <tag attr="val"/>}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlEmptyElement implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<CsDocComment> attributes;
        List<CsDocComment> spaceBeforeSlashClose;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlEmptyElement(this, p);
        }
    }

    /**
     * Plain text content within an XML documentation comment.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlText implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String text;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlText(this, p);
        }
    }

    /**
     * A generic XML attribute: {@code attr="value"}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlAttribute implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;

        @Nullable
        List<CsDocComment> spaceBeforeEquals;

        @Nullable
        List<CsDocComment> value;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlAttribute(this, p);
        }
    }

    /**
     * A {@code cref} attribute referencing a type or member: {@code cref="System.String"}.
     * Contains a type-attributed {@link J} reference for recipe support.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlCrefAttribute implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        @Nullable
        List<CsDocComment> spaceBeforeEquals;

        @Nullable
        List<CsDocComment> value;

        @Nullable
        J reference;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlCrefAttribute(this, p);
        }
    }

    /**
     * A {@code name} attribute binding to a parameter or type parameter: {@code name="paramName"}.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlNameAttribute implements CsDocComment {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        @Nullable
        List<CsDocComment> spaceBeforeEquals;

        @Nullable
        List<CsDocComment> value;

        @Nullable
        J paramName;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitXmlNameAttribute(this, p);
        }
    }

    /**
     * A line break within a documentation comment, including the margin (e.g., {@code "\n/// "}).
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class LineBreak implements CsDocComment {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        String margin;
        Markers markers;

        @Override
        public <P> CsDocComment acceptCsDocComment(CsDocCommentVisitor<P> v, P p) {
            return v.visitLineBreak(this, p);
        }

        public LineBreak withMargin(String margin) {
            if (margin.equals(this.margin)) {
                return this;
            }
            return new LineBreak(this.id, margin, this.markers);
        }

        @Override
        @SuppressWarnings("unchecked")
        public LineBreak withMarkers(Markers markers) {
            if (markers == this.markers) {
                return this;
            }
            return new LineBreak(id, margin, markers);
        }
    }
}
