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
package org.openrewrite.cobol.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.cobol.CobolVisitor;
import org.openrewrite.cobol.internal.CobolPrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Cobol extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof CobolVisitor ? (R) acceptCobol((CobolVisitor<P>) v, p) : v.defaultValue(this, p);
    }

    @Nullable
    default <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof CobolVisitor;
    }

    Space getPrefix();

    <P extends Cobol> P withPrefix(Space prefix);

    <P extends Cobol> P withMarkers(Markers markers);

    Markers getMarkers();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CompilationUnit implements Cobol, SourceFile {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Path sourcePath;

        @With
        @Getter
        @Nullable FileAttributes fileAttributes;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<CobolRightPadded<ProgramUnit>> programUnits;

        public List<ProgramUnit> getProgramUnits() {
            return CobolRightPadded.getElements(programUnits);
        }

        public CompilationUnit withProgramUnits(List<ProgramUnit> body) {
            return getPadding().withProgramUnits(CobolRightPadded.withElements(this.programUnits, body));
        }

        @With
        @Getter
        String eof;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new CobolPrinter<>();
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final CompilationUnit t;

            public List<CobolRightPadded<ProgramUnit>> getProgramUnits() {
                return t.programUnits;
            }

            public CompilationUnit withProgramUnits(List<CobolRightPadded<ProgramUnit>> programUnits) {
                return t.programUnits == programUnits ? t : new CompilationUnit(t.id, t.sourcePath, t.fileAttributes, t.prefix, t.markers, t.charsetName, t.charsetBomMarked, t.checksum, programUnits, t.eof);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Display implements Statement {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        List<Name> operands;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDisplay(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        String simpleName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        Object value;
        String valueSource;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IdentificationDivision implements Cobol {
        @Getter
        @Nullable
        @With
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        Space prefix;
        @Getter
        @With
        Markers markers;
        String identification;
        CobolLeftPadded<String> division;
        @Getter
        @With
        CobolLeftPadded<String> dot;
        @Getter
        @With
        ProgramIdParagraph programIdParagraph;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return this;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getDivision() {
            return division.getElement();
        }

        public IdentificationDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolLeftPadded.withElement(this.division, division));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final IdentificationDivision t;

            public CobolLeftPadded<String> getDivision() {
                return t.division;
            }

            public IdentificationDivision withDivision(CobolLeftPadded<String> division) {
                return t.division == division ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, division, t.dot, t.programIdParagraph);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureDivision implements Cobol {
        @Getter
        @Nullable
        @With
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        Space prefix;
        @Getter
        @With
        Markers markers;
        String rocedure;
        CobolLeftPadded<String> division;
        @Getter
        @With
        CobolLeftPadded<String> dot;
        @Getter
        @With
        ProcedureDivisionBody body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getDivision() {
            return division.getElement();
        }

        public ProcedureDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolLeftPadded.withElement(this.division, division));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ProcedureDivision t;

            public CobolLeftPadded<String> getDivision() {
                return t.division;
            }

            public ProcedureDivision withDivision(CobolLeftPadded<String> division) {
                return t;
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionBody implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        Paragraphs paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionBody(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Paragraphs implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        List<Sentence> sentences;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitParagraphs(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Sentence implements Cobol {
        @Getter
        @Nullable
        @With
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        Space prefix;
        @Getter
        @With
        Markers markers;
        @Getter
        @With
        List<Statement> statements;
        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSentence(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getDot() {
            return dot.getElement();
        }

        public Sentence withDot(String dot) {
            //noinspection ConstantConditions
            return getPadding().withDot(CobolLeftPadded.withElement(this.dot, dot));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Sentence t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public Sentence withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new Sentence(t.padding, t.id, t.prefix, t.markers, t.statements, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProgramIdParagraph implements Cobol {
        @Getter
        @Nullable
        @With
        @NonFinal
        transient WeakReference<Padding> padding;
        @Getter
        @EqualsAndHashCode.Include
        @With
        UUID id;
        @Getter
        @With
        Space prefix;
        @Getter
        @With
        Markers markers;
        String programId;
        CobolLeftPadded<String> dot1;
        @Getter
        @With
        CobolLeftPadded<String> programName;
        CobolLeftPadded<String> dot2;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return this;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public String getDot1() {
            return dot1.getElement();
        }

        public ProgramIdParagraph withDot1(String dot1) {
            //noinspection ConstantConditions
            return getPadding().withDot1(CobolLeftPadded.withElement(this.dot1, dot1));
        }

        public String getDot2() {
            return dot2.getElement();
        }

        public ProgramIdParagraph withDot2(String dot2) {
            //noinspection ConstantConditions
            return getPadding().withDot2(CobolLeftPadded.withElement(this.dot2, dot2));
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ProgramIdParagraph t;

            public CobolLeftPadded<String> getDot1() {
                return t.dot1;
            }

            public ProgramIdParagraph withDot1(CobolLeftPadded<String> dot1) {
                return t.dot1 == dot1 ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, dot1, t.programName, t.dot2);
            }

            public CobolLeftPadded<String> getDot2() {
                return t.dot2;
            }

            public ProgramIdParagraph withDot2(CobolLeftPadded<String> dot2) {
                return t.dot2 == dot2 ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, t.dot1, t.programName, dot2);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProgramUnit implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        IdentificationDivision identificationDivision;

        @Nullable
        ProcedureDivision procedureDivision;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramUnit(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stop implements Statement {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        String stop;
        CobolLeftPadded<String> run;
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStop(this, p);
        }
    }
}
