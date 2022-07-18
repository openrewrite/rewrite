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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DataDivision implements Statement {
        @Nullable
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
        String data;

        CobolLeftPadded<String> division;
        CobolContainer<DataDivisionSection> sections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataDivision(this, p);
        }

        public String getDivision() {
            return division.getElement();
        }

        public DataDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolLeftPadded.withElement(this.division, division));
        }

        public List<DataDivisionSection> getSections() {
            return sections.getElements();
        }

        public DataDivision withSections(List<DataDivisionSection> sections) {
            return getPadding().withSections(this.sections.getPadding().withElements(CobolRightPadded.withElements(
                    this.sections.getPadding().getElements(), sections)));
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
            private final DataDivision t;

            public CobolLeftPadded<String> getDivision() {
                return t.division;
            }

            public DataDivision withDivision(CobolLeftPadded<String> division) {
                return t.division == division ? t : new DataDivision(t.padding, t.id, t.prefix, t.markers, t.data, division, t.sections);
            }

            public CobolContainer<DataDivisionSection> getSections() {
                return t.sections;
            }

            public DataDivision withSections(CobolContainer<DataDivisionSection> sections) {
                return t.sections == sections ? t : new DataDivision(t.padding, t.id, t.prefix, t.markers, t.data, t.division, sections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DataDescriptionEntry implements Cobol {
        @Nullable
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
        Integer level;

        @Nullable
        CobolLeftPadded<String> name;

        CobolContainer<Cobol> clauses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataDescriptionEntry(this, p);
        }

        @Nullable
        public String getName() {
            return name == null ? null : name.getElement();
        }

        public DataDescriptionEntry withName(@Nullable String name) {
            if (name == null) {
                return this.name == null ? this : new DataDescriptionEntry(id, prefix, markers, level, null, clauses);
            }
            return getPadding().withName(CobolLeftPadded.withElement(this.name, name));
        }

        public List<Cobol> getClauses() {
            return clauses.getElements();
        }

        public DataDescriptionEntry withClauses(List<Cobol> clauses) {
            return getPadding().withClauses(this.clauses.getPadding().withElements(CobolRightPadded.withElements(
                    this.clauses.getPadding().getElements(), clauses)));
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
            private final DataDescriptionEntry t;

            @Nullable
            public CobolLeftPadded<String> getName() {
                return t.name;
            }

            public DataDescriptionEntry withName(@Nullable CobolLeftPadded<String> name) {
                return t.name == name ? t : new DataDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.level, name, t.clauses);
            }

            public CobolContainer<Cobol> getClauses() {
                return t.clauses;
            }

            public DataDescriptionEntry withClauses(CobolContainer<Cobol> clauses) {
                return t.clauses == clauses ? t : new DataDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.level, t.name, clauses);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DataPictureClause implements Cobol {
        @Nullable
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
        String pic;

        @Nullable
        CobolLeftPadded<String> is;

        CobolContainer<Picture> pictures;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataPictureClause(this, p);
        }

        @Nullable
        public String getIs() {
            return is == null ? null : is.getElement();
        }

        public DataPictureClause withIs(@Nullable String is) {
            if (is == null) {
                return this.is == null ? this : new DataPictureClause(id, prefix, markers, pic, null, pictures);
            }
            return getPadding().withIs(CobolLeftPadded.withElement(this.is, is));
        }

        public List<Cobol.Picture> getPictures() {
            return pictures.getElements();
        }

        public DataPictureClause withPictures(List<Cobol.Picture> pictures) {
            return getPadding().withPictures(this.pictures.getPadding().withElements(CobolRightPadded.withElements(
                    this.pictures.getPadding().getElements(), pictures)));
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
            private final DataPictureClause t;

            @Nullable
            public CobolLeftPadded<String> getIs() {
                return t.is;
            }

            public DataPictureClause withIs(@Nullable CobolLeftPadded<String> is) {
                return t.is == is ? t : new DataPictureClause(t.padding, t.id, t.prefix, t.markers, t.pic, is, t.pictures);
            }

            public CobolContainer<Cobol.Picture> getPictures() {
                return t.pictures;
            }

            public DataPictureClause withPictures(CobolContainer<Cobol.Picture> pictures) {
                return t.pictures == pictures ? t : new DataPictureClause(t.padding, t.id, t.prefix, t.markers, t.pic, t.is, pictures);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Picture implements Cobol {
        @Nullable
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
        String chars;

        @Nullable
        CobolLeftPadded<String> cardinalitySource;

        @Nullable
        public String getCardinality() {
            return cardinalitySource == null ? null : cardinalitySource
                    .getElement()
                    .replace("(", "")
                    .replace(")", "")
                    .trim();
        }

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPicture(this, p);
        }

        @Nullable
        public String getCardinalitySource() {
            return cardinalitySource == null ? null : cardinalitySource.getElement();
        }

        public Picture withCardinalitySource(@Nullable String cardinalitySource) {
            if (cardinalitySource == null) {
                return this.cardinalitySource == null ? this : new Picture(id, prefix, markers, chars, null);
            }
            return getPadding().withCardinalitySource(CobolLeftPadded.withElement(this.cardinalitySource, cardinalitySource));
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
            private final Picture t;

            @Nullable
            public CobolLeftPadded<String> getCardinalitySource() {
                return t.cardinalitySource;
            }

            public Picture withCardinalitySource(@Nullable CobolLeftPadded<String> cardinalitySource) {
                return t.cardinalitySource == cardinalitySource ? t : new Picture(t.padding, t.id, t.prefix, t.markers, t.chars, cardinalitySource);
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
        String display;
        List<Name> operands;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDisplay(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements Name {
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
    class Literal implements Name {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Object value;
        String valueSource;

        @Override
        public String getSimpleName() {
            return value.toString();
        }

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
        @Nullable
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
        String identification;

        CobolLeftPadded<String> division;
        CobolLeftPadded<ProgramIdParagraph> programIdParagraph;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentificationDivision(this, p);
        }

        public String getDivision() {
            return division.getElement();
        }

        public IdentificationDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolLeftPadded.withElement(this.division, division));
        }

        public Cobol.ProgramIdParagraph getProgramIdParagraph() {
            return programIdParagraph.getElement();
        }

        public IdentificationDivision withProgramIdParagraph(Cobol.ProgramIdParagraph programIdParagraph) {
            //noinspection ConstantConditions
            return getPadding().withProgramIdParagraph(CobolLeftPadded.withElement(this.programIdParagraph, programIdParagraph));
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
            private final IdentificationDivision t;

            public CobolLeftPadded<String> getDivision() {
                return t.division;
            }

            public IdentificationDivision withDivision(CobolLeftPadded<String> division) {
                return t.division == division ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, division, t.programIdParagraph);
            }

            public CobolLeftPadded<Cobol.ProgramIdParagraph> getProgramIdParagraph() {
                return t.programIdParagraph;
            }

            public IdentificationDivision withProgramIdParagraph(CobolLeftPadded<Cobol.ProgramIdParagraph> programIdParagraph) {
                return t.programIdParagraph == programIdParagraph ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, t.division, programIdParagraph);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureDivision implements Cobol {
        @Nullable
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
        String procedure;

        CobolLeftPadded<String> division;
        CobolLeftPadded<ProcedureDivisionBody> body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
        }

        public String getDivision() {
            return division.getElement();
        }

        public ProcedureDivision withDivision(String division) {
            //noinspection ConstantConditions
            return getPadding().withDivision(CobolLeftPadded.withElement(this.division, division));
        }

        public Cobol.ProcedureDivisionBody getBody() {
            return body.getElement();
        }

        public ProcedureDivision withBody(Cobol.ProcedureDivisionBody body) {
            //noinspection ConstantConditions
            return getPadding().withBody(CobolLeftPadded.withElement(this.body, body));
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
            private final ProcedureDivision t;

            public CobolLeftPadded<String> getDivision() {
                return t.division;
            }

            public ProcedureDivision withDivision(CobolLeftPadded<String> division) {
                return t.division == division ? t : new ProcedureDivision(t.padding, t.id, t.prefix, t.markers, t.procedure, division, t.body);
            }

            public CobolLeftPadded<Cobol.ProcedureDivisionBody> getBody() {
                return t.body;
            }

            public ProcedureDivision withBody(CobolLeftPadded<Cobol.ProcedureDivisionBody> body) {
                return t.body == body ? t : new ProcedureDivision(t.padding, t.id, t.prefix, t.markers, t.procedure, t.division, body);
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Paragraphs implements Cobol {
        @Nullable
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

        CobolContainer<Sentence> sentences;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitParagraphs(this, p);
        }

        public List<Cobol.Sentence> getSentences() {
            return sentences.getElements();
        }

        public Paragraphs withSentences(List<Cobol.Sentence> sentences) {
            return getPadding().withSentences(this.sentences.getPadding().withElements(CobolRightPadded.withElements(
                    this.sentences.getPadding().getElements(), sentences)));
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
            private final Paragraphs t;

            public CobolContainer<Cobol.Sentence> getSentences() {
                return t.sentences;
            }

            public Paragraphs withSentences(CobolContainer<Cobol.Sentence> sentences) {
                return t.sentences == sentences ? t : new Paragraphs(t.padding, t.id, t.prefix, t.markers, sentences);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Sentence implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        List<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSentence(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProgramIdParagraph implements Cobol {
        @Nullable
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
        String programId;

        CobolLeftPadded<Name> programName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramIdParagraph(this, p);
        }

        public Name getProgramName() {
            return programName.getElement();
        }

        public ProgramIdParagraph withProgramName(Name programName) {
            //noinspection ConstantConditions
            return getPadding().withProgramName(CobolLeftPadded.withElement(this.programName, programName));
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
            private final ProgramIdParagraph t;

            public CobolLeftPadded<Name> getProgramName() {
                return t.programName;
            }

            public ProgramIdParagraph withProgramName(CobolLeftPadded<Name> programName) {
                return t.programName == programName ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, programName);
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Stop implements Statement {
        @Nullable
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
        String stop;

        CobolLeftPadded<String> run;

        @Getter
        @With
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStop(this, p);
        }

        public String getRun() {
            return run.getElement();
        }

        public Stop withRun(String run) {
            //noinspection ConstantConditions
            return getPadding().withRun(CobolLeftPadded.withElement(this.run, run));
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
            private final Stop t;

            public CobolLeftPadded<String> getRun() {
                return t.run;
            }

            public Stop withRun(CobolLeftPadded<String> run) {
                return t.run == run ? t : new Stop(t.padding, t.id, t.prefix, t.markers, t.stop, run, t.statement);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class WorkingStorageSection implements DataDivisionSection {
        @Nullable
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
        String workingStorage;

        CobolLeftPadded<String> section;
        CobolContainer<DataDescriptionEntry> dataDescriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWorkingStorageSection(this, p);
        }

        public String getSection() {
            return section.getElement();
        }

        public WorkingStorageSection withSection(String section) {
            //noinspection ConstantConditions
            return getPadding().withSection(CobolLeftPadded.withElement(this.section, section));
        }

        public List<Cobol.DataDescriptionEntry> getDataDescriptions() {
            return dataDescriptions.getElements();
        }

        public WorkingStorageSection withDataDescriptions(List<Cobol.DataDescriptionEntry> dataDescriptions) {
            return getPadding().withDataDescriptions(this.dataDescriptions.getPadding().withElements(CobolRightPadded.withElements(
                    this.dataDescriptions.getPadding().getElements(), dataDescriptions)));
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
            private final WorkingStorageSection t;

            public CobolLeftPadded<String> getSection() {
                return t.section;
            }

            public WorkingStorageSection withSection(CobolLeftPadded<String> section) {
                return t.section == section ? t : new WorkingStorageSection(t.padding, t.id, t.prefix, t.markers, t.workingStorage, section, t.dataDescriptions);
            }

            public CobolContainer<Cobol.DataDescriptionEntry> getDataDescriptions() {
                return t.dataDescriptions;
            }

            public WorkingStorageSection withDataDescriptions(CobolContainer<Cobol.DataDescriptionEntry> dataDescriptions) {
                return t.dataDescriptions == dataDescriptions ? t : new WorkingStorageSection(t.padding, t.id, t.prefix, t.markers, t.workingStorage, t.section, dataDescriptions);
            }
        }
    }
}
