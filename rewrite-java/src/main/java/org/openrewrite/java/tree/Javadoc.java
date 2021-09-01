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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavadocPrinter;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.marker.Markers;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Javadoc extends Serializable, Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptJavadoc((JavadocVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof JavadocVisitor;
    }

    @Nullable
    default <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    default <P> String print(TreePrinter<P> printer, P p) {
        return new JavadocPrinter<>(printer).print(this, p);
    }

    @Override
    default <P> String print(P p) {
        return print(TreePrinter.identity(), p);
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Attribute implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        String name;

        @Nullable
        List<Javadoc> beforeEqual;

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

        String prefix;
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

        String prefix;
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

        String prefix;
        Markers markers;
        String name;
        String beforeEndBracket;

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

        @SuppressWarnings("unchecked")
        public DocComment withSuffix(String suffix) {
            if(!suffix.equals(this.suffix)) {
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
        public String printComment() {
            return print();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DocRoot implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        String beforeEndBrace;

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

        String prefix;
        Markers markers;
        String text;

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

        String prefix;
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

        String prefix;
        Markers markers;
        String beforeEndBrace;

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

        String prefix;
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

        String prefix;
        Markers markers;
        Javadoc searchTerm;
        List<Javadoc> description;

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

        String prefix;
        Markers markers;

        @Nullable
        J tree;

        String beforeEndBrace;

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
    @With
    class LineBreak implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        String margin;
        Markers markers;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLineBreak(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Link implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        boolean plain;
        J tree;
        String beforeEndBrace;

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

        String prefix;
        Markers markers;
        boolean code;
        List<Javadoc> description;

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

        String prefix;
        Markers markers;
        J name;
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

        String prefix;
        Markers markers;
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

        String prefix;
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

        String prefix;
        Markers markers;

        @Nullable
        J tree;

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

        String prefix;
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

        String prefix;
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

        String prefix;
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

        String prefix;
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

        String prefix;
        Markers markers;
        String name;
        List<Javadoc> attributes;
        boolean selfClosing;
        String beforeEndBracket;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitStartElement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Summary implements Javadoc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        List<Javadoc> summary;

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

        String prefix;
        Markers markers;

        /**
         * {@code @throws} and {@code @exception} are synonyms.
         */
        boolean throwsKeyword;

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

        String prefix;
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

        String prefix;
        Markers markers;
        String name;
        String beforeEndBrace;

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

        String prefix;
        Markers markers;
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

        String prefix;
        Markers markers;
        List<Javadoc> body;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitVersion(this, p);
        }
    }
}
