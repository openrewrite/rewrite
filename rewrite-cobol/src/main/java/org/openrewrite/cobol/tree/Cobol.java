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
            return v.visitCompilationUnit(this, p);
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
class Add implements Statement {
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
        String add;

@Getter
        @With
        Cobol operation;

@Getter
        @Nullable
        @With
        StatementPhrase onSizeError;

@Nullable
        CobolLeftPadded<String> endAdd;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAdd(this, p);
        }

        @Nullable
        public String getEndAdd() {
            return endAdd == null ? null : endAdd.getElement();
        }

        public Add withEndAdd(@Nullable String endAdd) {
            if (endAdd == null) {
                return this.endAdd == null ? this : new Add(id, prefix, markers, add, operation, onSizeError, null);
            }
            return getPadding().withEndAdd(CobolLeftPadded.withElement(this.endAdd, endAdd));
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
        private final Add t;

        @Nullable
        public CobolLeftPadded<String> getEndAdd() {
            return t.endAdd;
        }

        public Add withEndAdd(@Nullable CobolLeftPadded<String> endAdd) {
            return t.endAdd == endAdd ? t : new Add(t.padding, t.id, t.prefix, t.markers, t.add, t.operation, t.onSizeError, endAdd);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class AddTo implements Cobol {
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

CobolContainer<Name> from;

@Nullable
        CobolContainer<Name> to;

@Nullable
        CobolContainer<Name> giving;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAddTo(this, p);
        }

        public List<Name> getFrom() {
            return from.getElements();
        }

        public AddTo withFrom(List<Name> from) {
            return getPadding().withFrom(this.from.getPadding().withElements(CobolRightPadded.withElements(
                    this.from.getPadding().getElements(), from)));
        }

        public List<Name> getTo() {
            return to.getElements();
        }

        public AddTo withTo(List<Name> to) {
            return getPadding().withTo(this.to.getPadding().withElements(CobolRightPadded.withElements(
                    this.to.getPadding().getElements(), to)));
        }

        public List<Name> getGiving() {
            return giving.getElements();
        }

        public AddTo withGiving(List<Name> giving) {
            return getPadding().withGiving(this.giving.getPadding().withElements(CobolRightPadded.withElements(
                    this.giving.getPadding().getElements(), giving)));
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
        private final AddTo t;

        public CobolContainer<Name> getFrom() {
            return t.from;
        }

        public AddTo withFrom(CobolContainer<Name> from) {
            return t.from == from ? t : new AddTo(t.padding, t.id, t.prefix, t.markers, from, t.to, t.giving);
        }

        @Nullable
        public CobolContainer<Name> getTo() {
            return t.to;
        }

        public AddTo withTo(@Nullable CobolContainer<Name> to) {
            return t.to == to ? t : new AddTo(t.padding, t.id, t.prefix, t.markers, t.from, to, t.giving);
        }

        @Nullable
        public CobolContainer<Name> getGiving() {
            return t.giving;
        }

        public AddTo withGiving(@Nullable CobolContainer<Name> giving) {
            return t.giving == giving ? t : new AddTo(t.padding, t.id, t.prefix, t.markers, t.from, t.to, giving);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ConfigurationSection implements Cobol {
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
        String words;

CobolContainer<Cobol> paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitConfigurationSection(this, p);
        }

        public List<Cobol> getParagraphs() {
            return paragraphs.getElements();
        }

        public ConfigurationSection withParagraphs(List<Cobol> paragraphs) {
            return getPadding().withParagraphs(this.paragraphs.getPadding().withElements(CobolRightPadded.withElements(
                    this.paragraphs.getPadding().getElements(), paragraphs)));
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
        private final ConfigurationSection t;

        public CobolContainer<Cobol> getParagraphs() {
            return t.paragraphs;
        }

        public ConfigurationSection withParagraphs(CobolContainer<Cobol> paragraphs) {
            return t.paragraphs == paragraphs ? t : new ConfigurationSection(t.padding, t.id, t.prefix, t.markers, t.words, paragraphs);
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
        String words;

CobolContainer<DataDivisionSection> sections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataDivision(this, p);
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

        public CobolContainer<DataDivisionSection> getSections() {
            return t.sections;
        }

        public DataDivision withSections(CobolContainer<DataDivisionSection> sections) {
            return t.sections == sections ? t : new DataDivision(t.padding, t.id, t.prefix, t.markers, t.words, sections);
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
        String words;

CobolContainer<Picture> pictures;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataPictureClause(this, p);
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

        public CobolContainer<Cobol.Picture> getPictures() {
            return t.pictures;
        }

        public DataPictureClause withPictures(CobolContainer<Cobol.Picture> pictures) {
            return t.pictures == pictures ? t : new DataPictureClause(t.padding, t.id, t.prefix, t.markers, t.words, pictures);
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
class EndProgram implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

Space prefix;
        Markers markers;
        String words;
        Name programName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEndProgram(this, p);
        }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class EnvironmentDivision implements Cobol {
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
        String words;

CobolContainer<Cobol> body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEnvironmentDivision(this, p);
        }

        public List<Cobol> getBody() {
            return body.getElements();
        }

        public EnvironmentDivision withBody(List<Cobol> body) {
            return getPadding().withBody(this.body.getPadding().withElements(CobolRightPadded.withElements(
                    this.body.getPadding().getElements(), body)));
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
        private final EnvironmentDivision t;

        public CobolContainer<Cobol> getBody() {
            return t.body;
        }

        public EnvironmentDivision withBody(CobolContainer<Cobol> body) {
            return t.body == body ? t : new EnvironmentDivision(t.padding, t.id, t.prefix, t.markers, t.words, body);
        }
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
        String words;

CobolLeftPadded<ProgramIdParagraph> programIdParagraph;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentificationDivision(this, p);
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

        public CobolLeftPadded<Cobol.ProgramIdParagraph> getProgramIdParagraph() {
            return t.programIdParagraph;
        }

        public IdentificationDivision withProgramIdParagraph(CobolLeftPadded<Cobol.ProgramIdParagraph> programIdParagraph) {
            return t.programIdParagraph == programIdParagraph ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.words, programIdParagraph);
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
        String words;

CobolLeftPadded<ProcedureDivisionBody> body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
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

        public CobolLeftPadded<Cobol.ProcedureDivisionBody> getBody() {
            return t.body;
        }

        public ProcedureDivision withBody(CobolLeftPadded<Cobol.ProcedureDivisionBody> body) {
            return t.body == body ? t : new ProcedureDivision(t.padding, t.id, t.prefix, t.markers, t.words, body);
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

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Roundable implements Name {
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
        Identifier identifier;

@Nullable
        CobolLeftPadded<String> rounded;

        @Override
        public String getSimpleName() {
            return identifier.getSimpleName();
        }

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRoundable(this, p);
        }

        @Nullable
        public String getRounded() {
            return rounded == null ? null : rounded.getElement();
        }

        public Roundable withRounded(@Nullable String rounded) {
            if (rounded == null) {
                return this.rounded == null ? this : new Roundable(id, prefix, markers, identifier, null);
            }
            return getPadding().withRounded(CobolLeftPadded.withElement(this.rounded, rounded));
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
        private final Roundable t;

        @Nullable
        public CobolLeftPadded<String> getRounded() {
            return t.rounded;
        }

        public Roundable withRounded(@Nullable CobolLeftPadded<String> rounded) {
            return t.rounded == rounded ? t : new Roundable(t.padding, t.id, t.prefix, t.markers, t.identifier, rounded);
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

@Nullable
        CobolLeftPadded<String> programAttributes;

@Nullable
        CobolLeftPadded<String> dot;

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

        @Nullable
        public String getProgramAttributes() {
            return programAttributes == null ? null : programAttributes.getElement();
        }

        public ProgramIdParagraph withProgramAttributes(@Nullable String programAttributes) {
            if (programAttributes == null) {
                return this.programAttributes == null ? this : new ProgramIdParagraph(id, prefix, markers, programId, programName, null, dot);
            }
            return getPadding().withProgramAttributes(CobolLeftPadded.withElement(this.programAttributes, programAttributes));
        }

        @Nullable
        public String getDot() {
            return dot == null ? null : dot.getElement();
        }

        public ProgramIdParagraph withDot(@Nullable String dot) {
            if (dot == null) {
                return this.dot == null ? this : new ProgramIdParagraph(id, prefix, markers, programId, programName, programAttributes, null);
            }
            return getPadding().withDot(CobolLeftPadded.withElement(this.dot, dot));
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
            return t.programName == programName ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, programName, t.programAttributes, t.dot);
        }

        @Nullable
        public CobolLeftPadded<String> getProgramAttributes() {
            return t.programAttributes;
        }

        public ProgramIdParagraph withProgramAttributes(@Nullable CobolLeftPadded<String> programAttributes) {
            return t.programAttributes == programAttributes ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, t.programName, programAttributes, t.dot);
        }

        @Nullable
        public CobolLeftPadded<String> getDot() {
            return t.dot;
        }

        public ProgramIdParagraph withDot(@Nullable CobolLeftPadded<String> dot) {
            return t.dot == dot ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, t.programId, t.programName, t.programAttributes, dot);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ProgramUnit implements Cobol {
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
        IdentificationDivision identificationDivision;

@Getter
        @Nullable
        @With
        EnvironmentDivision environmentDivision;

@Getter
        @Nullable
        @With
        DataDivision dataDivision;

@Getter
        @Nullable
        @With
        ProcedureDivision procedureDivision;

CobolContainer<ProgramUnit> programUnits;

@Nullable
        CobolRightPadded<EndProgram> endProgram;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramUnit(this, p);
        }

        public List<Cobol.ProgramUnit> getProgramUnits() {
            return programUnits.getElements();
        }

        public ProgramUnit withProgramUnits(List<Cobol.ProgramUnit> programUnits) {
            return getPadding().withProgramUnits(this.programUnits.getPadding().withElements(CobolRightPadded.withElements(
                    this.programUnits.getPadding().getElements(), programUnits)));
        }

        @Nullable
        public Cobol.EndProgram getEndProgram() {
            return endProgram == null ? null : endProgram.getElement();
        }

        public ProgramUnit withEndProgram(@Nullable Cobol.EndProgram endProgram) {
            if (endProgram == null) {
                return this.endProgram == null ? this : new ProgramUnit(id, prefix, markers, identificationDivision, environmentDivision, dataDivision, procedureDivision, programUnits, null);
            }
            return getPadding().withEndProgram(CobolRightPadded.withElement(this.endProgram, endProgram));
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
        private final ProgramUnit t;

        public CobolContainer<Cobol.ProgramUnit> getProgramUnits() {
            return t.programUnits;
        }

        public ProgramUnit withProgramUnits(CobolContainer<Cobol.ProgramUnit> programUnits) {
            return t.programUnits == programUnits ? t : new ProgramUnit(t.padding, t.id, t.prefix, t.markers, t.identificationDivision, t.environmentDivision, t.dataDivision, t.procedureDivision, programUnits, t.endProgram);
        }

        @Nullable
        public CobolRightPadded<Cobol.EndProgram> getEndProgram() {
            return t.endProgram;
        }

        public ProgramUnit withEndProgram(@Nullable CobolRightPadded<Cobol.EndProgram> endProgram) {
            return t.endProgram == endProgram ? t : new ProgramUnit(t.padding, t.id, t.prefix, t.markers, t.identificationDivision, t.environmentDivision, t.dataDivision, t.procedureDivision, t.programUnits, endProgram);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Set implements Statement {
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
        String set;

@Nullable
        CobolContainer<SetTo> to;

@Getter
        @Nullable
        @With
        SetUpDown upDown;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSet(this, p);
        }

        public List<Cobol.SetTo> getTo() {
            return to.getElements();
        }

        public Set withTo(List<Cobol.SetTo> to) {
            return getPadding().withTo(this.to.getPadding().withElements(CobolRightPadded.withElements(
                    this.to.getPadding().getElements(), to)));
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
        private final Set t;

        @Nullable
        public CobolContainer<Cobol.SetTo> getTo() {
            return t.to;
        }

        public Set withTo(@Nullable CobolContainer<Cobol.SetTo> to) {
            return t.to == to ? t : new Set(t.padding, t.id, t.prefix, t.markers, t.set, to, t.upDown);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class SetTo implements Cobol {
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

CobolContainer<Identifier> to;
        CobolContainer<Name> values;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSetTo(this, p);
        }

        public List<Cobol.Identifier> getTo() {
            return to.getElements();
        }

        public SetTo withTo(List<Cobol.Identifier> to) {
            return getPadding().withTo(this.to.getPadding().withElements(CobolRightPadded.withElements(
                    this.to.getPadding().getElements(), to)));
        }

        public List<Name> getValues() {
            return values.getElements();
        }

        public SetTo withValues(List<Name> values) {
            return getPadding().withValues(this.values.getPadding().withElements(CobolRightPadded.withElements(
                    this.values.getPadding().getElements(), values)));
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
        private final SetTo t;

        public CobolContainer<Cobol.Identifier> getTo() {
            return t.to;
        }

        public SetTo withTo(CobolContainer<Cobol.Identifier> to) {
            return t.to == to ? t : new SetTo(t.padding, t.id, t.prefix, t.markers, to, t.values);
        }

        public CobolContainer<Name> getValues() {
            return t.values;
        }

        public SetTo withValues(CobolContainer<Name> values) {
            return t.values == values ? t : new SetTo(t.padding, t.id, t.prefix, t.markers, t.to, values);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class SetUpDown implements Cobol {
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

CobolContainer<Identifier> to;
        CobolLeftPadded<String> operation;

@Getter
        @With
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSetUpDown(this, p);
        }

        public List<Cobol.Identifier> getTo() {
            return to.getElements();
        }

        public SetUpDown withTo(List<Cobol.Identifier> to) {
            return getPadding().withTo(this.to.getPadding().withElements(CobolRightPadded.withElements(
                    this.to.getPadding().getElements(), to)));
        }

        public String getOperation() {
            return operation.getElement();
        }

        public SetUpDown withOperation(String operation) {
            //noinspection ConstantConditions
            return getPadding().withOperation(CobolLeftPadded.withElement(this.operation, operation));
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
        private final SetUpDown t;

        public CobolContainer<Cobol.Identifier> getTo() {
            return t.to;
        }

        public SetUpDown withTo(CobolContainer<Cobol.Identifier> to) {
            return t.to == to ? t : new SetUpDown(t.padding, t.id, t.prefix, t.markers, to, t.operation, t.value);
        }

        public CobolLeftPadded<String> getOperation() {
            return t.operation;
        }

        public SetUpDown withOperation(CobolLeftPadded<String> operation) {
            return t.operation == operation ? t : new SetUpDown(t.padding, t.id, t.prefix, t.markers, t.to, operation, t.value);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class SourceComputer implements Cobol {
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

CobolRightPadded<String> words;

@Nullable
        CobolRightPadded<SourceComputerDefinition> computer;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSourceComputer(this, p);
        }

        public String getWords() {
            return words.getElement();
        }

        public SourceComputer withWords(String words) {
            //noinspection ConstantConditions
            return getPadding().withWords(CobolRightPadded.withElement(this.words, words));
        }

        @Nullable
        public Cobol.SourceComputerDefinition getComputer() {
            return computer == null ? null : computer.getElement();
        }

        public SourceComputer withComputer(@Nullable Cobol.SourceComputerDefinition computer) {
            if (computer == null) {
                return this.computer == null ? this : new SourceComputer(id, prefix, markers, words, null);
            }
            return getPadding().withComputer(CobolRightPadded.withElement(this.computer, computer));
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
        private final SourceComputer t;

        public CobolRightPadded<String> getWords() {
            return t.words;
        }

        public SourceComputer withWords(CobolRightPadded<String> words) {
            return t.words == words ? t : new SourceComputer(t.padding, t.id, t.prefix, t.markers, words, t.computer);
        }

        @Nullable
        public CobolRightPadded<Cobol.SourceComputerDefinition> getComputer() {
            return t.computer;
        }

        public SourceComputer withComputer(@Nullable CobolRightPadded<Cobol.SourceComputerDefinition> computer) {
            return t.computer == computer ? t : new SourceComputer(t.padding, t.id, t.prefix, t.markers, t.words, computer);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class SourceComputerDefinition implements Cobol {
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
        String computerName;

@Nullable
        CobolLeftPadded<String> debuggingMode;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSourceComputerDefinition(this, p);
        }

        @Nullable
        public String getDebuggingMode() {
            return debuggingMode == null ? null : debuggingMode.getElement();
        }

        public SourceComputerDefinition withDebuggingMode(@Nullable String debuggingMode) {
            if (debuggingMode == null) {
                return this.debuggingMode == null ? this : new SourceComputerDefinition(id, prefix, markers, computerName, null);
            }
            return getPadding().withDebuggingMode(CobolLeftPadded.withElement(this.debuggingMode, debuggingMode));
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
        private final SourceComputerDefinition t;

        @Nullable
        public CobolLeftPadded<String> getDebuggingMode() {
            return t.debuggingMode;
        }

        public SourceComputerDefinition withDebuggingMode(@Nullable CobolLeftPadded<String> debuggingMode) {
            return t.debuggingMode == debuggingMode ? t : new SourceComputerDefinition(t.padding, t.id, t.prefix, t.markers, t.computerName, debuggingMode);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ObjectComputer implements Cobol {
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

CobolRightPadded<String> words;

@Nullable
        CobolRightPadded<ObjectComputerDefinition> computer;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitObjectComputer(this, p);
        }

        public String getWords() {
            return words.getElement();
        }

        public ObjectComputer withWords(String words) {
            //noinspection ConstantConditions
            return getPadding().withWords(CobolRightPadded.withElement(this.words, words));
        }

        @Nullable
        public Cobol.ObjectComputerDefinition getComputer() {
            return computer == null ? null : computer.getElement();
        }

        public ObjectComputer withComputer(@Nullable Cobol.ObjectComputerDefinition computer) {
            if (computer == null) {
                return this.computer == null ? this : new ObjectComputer(id, prefix, markers, words, null);
            }
            return getPadding().withComputer(CobolRightPadded.withElement(this.computer, computer));
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
        private final ObjectComputer t;

        public CobolRightPadded<String> getWords() {
            return t.words;
        }

        public ObjectComputer withWords(CobolRightPadded<String> words) {
            return t.words == words ? t : new ObjectComputer(t.padding, t.id, t.prefix, t.markers, words, t.computer);
        }

        @Nullable
        public CobolRightPadded<Cobol.ObjectComputerDefinition> getComputer() {
            return t.computer;
        }

        public ObjectComputer withComputer(@Nullable CobolRightPadded<Cobol.ObjectComputerDefinition> computer) {
            return t.computer == computer ? t : new ObjectComputer(t.padding, t.id, t.prefix, t.markers, t.words, computer);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ObjectComputerDefinition implements Cobol {
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
        String computerName;

CobolContainer<Cobol> specifications;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitObjectComputerDefinition(this, p);
        }

        public List<Cobol> getSpecifications() {
            return specifications.getElements();
        }

        public ObjectComputerDefinition withSpecifications(List<Cobol> specifications) {
            return getPadding().withSpecifications(this.specifications.getPadding().withElements(CobolRightPadded.withElements(
                    this.specifications.getPadding().getElements(), specifications)));
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
        private final ObjectComputerDefinition t;

        public CobolContainer<Cobol> getSpecifications() {
            return t.specifications;
        }

        public ObjectComputerDefinition withSpecifications(CobolContainer<Cobol> specifications) {
            return t.specifications == specifications ? t : new ObjectComputerDefinition(t.padding, t.id, t.prefix, t.markers, t.computerName, specifications);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class ValuedObjectComputerClause implements Cobol {
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
        Type type;

@Getter
        @With
        String words;

@Getter
        @Nullable
        @With
        Cobol value;

@Nullable
        CobolLeftPadded<String> units;

        public enum Type {
            Memory,
            Disk,
            SegmentLimit,
            CharacterSet
        }

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitValuedObjectComputerClause(this, p);
        }

        @Nullable
        public String getUnits() {
            return units == null ? null : units.getElement();
        }

        public ValuedObjectComputerClause withUnits(@Nullable String units) {
            if (units == null) {
                return this.units == null ? this : new ValuedObjectComputerClause(id, prefix, markers, type, words, value, null);
            }
            return getPadding().withUnits(CobolLeftPadded.withElement(this.units, units));
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
        private final ValuedObjectComputerClause t;

        @Nullable
        public CobolLeftPadded<String> getUnits() {
            return t.units;
        }

        public ValuedObjectComputerClause withUnits(@Nullable CobolLeftPadded<String> units) {
            return t.units == units ? t : new ValuedObjectComputerClause(t.padding, t.id, t.prefix, t.markers, t.type, t.words, t.value, units);
        }
    }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class CollatingSequenceClause implements Cobol {
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
        String words;

CobolContainer<Identifier> alphabetName;

@Getter
        @Nullable
        @With
        CollatingSequenceAlphabet alphanumeric;

@Getter
        @Nullable
        @With
        CollatingSequenceAlphabet national;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCollatingSequenceClause(this, p);
        }

        public List<Cobol.Identifier> getAlphabetName() {
            return alphabetName.getElements();
        }

        public CollatingSequenceClause withAlphabetName(List<Cobol.Identifier> alphabetName) {
            return getPadding().withAlphabetName(this.alphabetName.getPadding().withElements(CobolRightPadded.withElements(
                    this.alphabetName.getPadding().getElements(), alphabetName)));
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
        private final CollatingSequenceClause t;

        public CobolContainer<Cobol.Identifier> getAlphabetName() {
            return t.alphabetName;
        }

        public CollatingSequenceClause withAlphabetName(CobolContainer<Cobol.Identifier> alphabetName) {
            return t.alphabetName == alphabetName ? t : new CollatingSequenceClause(t.padding, t.id, t.prefix, t.markers, t.words, alphabetName, t.alphanumeric, t.national);
        }
    }
    }

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
class CollatingSequenceAlphabet implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

Space prefix;
        Markers markers;
        String words;
        Identifier alphabetName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCollatingSequenceAlphabet(this, p);
        }
    }

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class StatementPhrase implements Cobol {
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
        String phrase;

CobolContainer<Statement> statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStatementPhrase(this, p);
        }

        public List<Statement> getStatement() {
            return statement.getElements();
        }

        public StatementPhrase withStatement(List<Statement> statement) {
            return getPadding().withStatement(this.statement.getPadding().withElements(CobolRightPadded.withElements(
                    this.statement.getPadding().getElements(), statement)));
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
        private final StatementPhrase t;

        public CobolContainer<Statement> getStatement() {
            return t.statement;
        }

        public StatementPhrase withStatement(CobolContainer<Statement> statement) {
            return t.statement == statement ? t : new StatementPhrase(t.padding, t.id, t.prefix, t.markers, t.phrase, statement);
        }
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
        String words;
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStop(this, p);
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
        String words;

CobolContainer<DataDescriptionEntry> dataDescriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWorkingStorageSection(this, p);
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

        public CobolContainer<Cobol.DataDescriptionEntry> getDataDescriptions() {
            return t.dataDescriptions;
        }

        public WorkingStorageSection withDataDescriptions(CobolContainer<Cobol.DataDescriptionEntry> dataDescriptions) {
            return t.dataDescriptions == dataDescriptions ? t : new WorkingStorageSection(t.padding, t.id, t.prefix, t.markers, t.words, dataDescriptions);
        }
    }
    }
}
