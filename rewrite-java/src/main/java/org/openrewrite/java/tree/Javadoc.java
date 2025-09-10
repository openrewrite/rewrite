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
package org.openrewrite.java.tree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavadocPrinter;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

public interface Javadoc extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptJavadoc((JavadocVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof JavadocVisitor;
    }

    default <P> @Nullable Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new JavadocPrinter<>();
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Attribute implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;

        @Nullable
        List<Javadoc> spaceBeforeEqual;

        @Nullable
        List<Javadoc> value;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Author implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> name;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitAuthor(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Deprecated implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDeprecated(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EndElement implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<Javadoc> spaceBeforeEndBracket;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitEndElement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class DocComment implements Javadoc, Comment {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Markers markers;

        @With
        List<Javadoc> body;

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
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocComment(this, p);
        }

        @Override
        public <P> void printComment(Cursor cursor, PrintOutputCapture<P> print) {
            new JavadocPrinter<P>().visit(this, print, cursor);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DocRoot implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocRoot(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DocType implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocType(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Erroneous implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitErroneous(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InheritDoc implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitInheritDoc(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Hidden implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> body;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitHidden(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Index implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> searchTerm;
        List<Javadoc> description;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitIndex(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InlinedValue implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> spaceBeforeTree;

        @Nullable
        J tree;

        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitInlinedValue(this, p);
        }
    }

    /**
     * Holds line break plus the margin that starts the next line. The margin in many cases
     * ends in a '*'.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class LineBreak implements Javadoc {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        String margin;
        Markers markers;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Link implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        boolean plain;
        List<Javadoc> spaceBeforeTree;

        @Nullable
        J tree;

        public @Nullable Reference getTreeReference() {
            if (tree != null && treeReference == null) {
                treeReference = new Reference(Tree.randomId(), Markers.EMPTY, tree, null);
            }
            return treeReference;
        }

        // This is non-final to maintain backwards compatibility.
        @NonFinal
        @Nullable
        Reference treeReference;

        List<Javadoc> label;

        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLink(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        boolean code;
        List<Javadoc> description;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Parameter implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> spaceBeforeName;

        @Nullable
        J name;

        public @Nullable Reference getNameReference() {
            if (name != null && nameReference == null) {
                nameReference = new Reference(Tree.randomId(), Markers.EMPTY, name, null);
            }
            return nameReference;
        }

        // This is non-final to maintain backwards compatibility.
        @NonFinal
        @Nullable
        Reference nameReference;

        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitParameter(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Provides implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> spaceBeforeServiceType;
        J serviceType;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitProvides(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Return implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class See implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> spaceBeforeTree;

        @Nullable
        J tree;

        public @Nullable Reference getTreeReference() {
            if (tree != null && treeReference == null) {
                treeReference = new Reference(Tree.randomId(), Markers.EMPTY, tree, null);
            }
            return treeReference;
        }

        // This is non-final to maintain backwards compatibility.
        @NonFinal
        @Nullable
        Reference treeReference;

        List<Javadoc> reference;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSee(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Serial implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerial(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SerialData implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerialData(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SerialField implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        J.Identifier name;
        J type;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerialField(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Since implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSince(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StartElement implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<Javadoc> attributes;
        boolean selfClosing;
        List<Javadoc> spaceBeforeEndBracket;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitStartElement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Snippet implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> attributes;
        List<Javadoc> content;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSnippet(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Summary implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> summary;
        List<Javadoc> beforeBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSummary(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Text implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitText(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Throws implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        /**
         * {@code @throws} and {@code @exception} are synonyms.
         */
        boolean throwsKeyword;

        List<Javadoc> spaceBeforeExceptionName;
        J exceptionName;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitThrows(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnknownBlock implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<Javadoc> content;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUnknownBlock(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnknownInline implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        String name;
        List<Javadoc> content;
        List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUnknownInline(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Uses implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> beforeServiceType;
        J serviceType;
        List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUses(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Version implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        List<Javadoc> body;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitVersion(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Reference implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        /**
         * Not truly nullable, but done for deserialization backwards compatibility since this
         * field was accidentally omitted prior to 7.31.0.
         */
        @Nullable
        Markers markers;

        @Override
        public Markers getMarkers() {
            return markers == null ? Markers.EMPTY : markers;
        }

        @Nullable
        J tree;

        @Nullable
        List<Javadoc> lineBreaks;

        public List<Javadoc> getLineBreaks() {
            return lineBreaks == null ? emptyList() : lineBreaks;
        }

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitReference(this, p);
        }
    }
}
