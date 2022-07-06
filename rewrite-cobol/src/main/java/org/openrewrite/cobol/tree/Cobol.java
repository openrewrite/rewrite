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
        Space eof;

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
class Display implements Statement {
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
        /**
         * Either an {@link Identifier} or {@link Literal}.
         */
        List<Cobol> operands;

        @Nullable
        CobolLeftPadded<Identifier> upon;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDisplay(this, p);
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

    @Nullable
    public Cobol.Identifier getUpon() {
        return upon == null ? null : upon.getElement();
    }

    public Display withUpon(@Nullable Cobol.Identifier upon) {
        if (upon == null) {
            return this.upon == null ? this : new Display(padding, id, prefix, markers, operands, null);
        }
        return getPadding().withUpon(CobolLeftPadded.withElement(this.upon, upon));
    }

    @RequiredArgsConstructor
    public static class Padding {
        private final Display t;

        @Nullable
        public CobolLeftPadded<Cobol.Identifier> getUpon() {
            return t.upon;
        }

        public Display withUpon(@Nullable CobolLeftPadded<Cobol.Identifier> upon) {
            return t.upon == upon ? t : new Display(t.padding, t.id, t.prefix, t.markers, t.operands, upon);
        }
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

        // identificationDivision
        //   : (IDENTIFICATION | ID) DIVISION DOT_FS programIdParagraph identificationDivisionBody*
        //   ;

        CobolRightPadded<IdKeyword> identification;
        CobolRightPadded<Space> division;
        CobolRightPadded<Space> dot;

        public enum IdKeyword {
            Identification,
            Id
        }

    @Getter
    @With
    ProgramIdParagraph programIdParagraph;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIdentificationDivision(this, p);
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

    public Cobol.IdentificationDivision.IdKeyword getIdentification() {
        return identification.getElement();
    }

    public IdentificationDivision withIdentification(Cobol.IdentificationDivision.IdKeyword identification) {
        //noinspection ConstantConditions
        return getPadding().withIdentification(CobolRightPadded.withElement(this.identification, identification));
    }

    public Space getDivision() {
        return division.getElement();
    }

    public IdentificationDivision withDivision(Space division) {
        //noinspection ConstantConditions
        return getPadding().withDivision(CobolRightPadded.withElement(this.division, division));
    }

    public Space getDot() {
        return dot.getElement();
    }

    public IdentificationDivision withDot(Space dot) {
        //noinspection ConstantConditions
        return getPadding().withDot(CobolRightPadded.withElement(this.dot, dot));
    }

    @RequiredArgsConstructor
    public static class Padding {
        private final IdentificationDivision t;

        public CobolRightPadded<Cobol.IdentificationDivision.IdKeyword> getIdentification() {
            return t.identification;
        }

        public IdentificationDivision withIdentification(CobolRightPadded<Cobol.IdentificationDivision.IdKeyword> identification) {
            return t.identification == identification ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, identification, t.division, t.dot, t.programIdParagraph);
        }

        public CobolRightPadded<Space> getDivision() {
            return t.division;
        }

        public IdentificationDivision withDivision(CobolRightPadded<Space> division) {
            return t.division == division ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, division, t.dot, t.programIdParagraph);
        }

        public CobolRightPadded<Space> getDot() {
            return t.dot;
        }

        public IdentificationDivision withDot(CobolRightPadded<Space> dot) {
            return t.dot == dot ? t : new IdentificationDivision(t.padding, t.id, t.prefix, t.markers, t.identification, t.division, dot, t.programIdParagraph);
        }
    }
    }

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
class ProcedureDivision implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
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
        CobolRightPadded<Space> programId;
    @Getter
    @With
    String programName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramIdParagraph(this, p);
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

    public Space getProgramId() {
        return programId.getElement();
    }

    public ProgramIdParagraph withProgramId(Space programId) {
        //noinspection ConstantConditions
        return getPadding().withProgramId(CobolRightPadded.withElement(this.programId, programId));
    }

    @RequiredArgsConstructor
    public static class Padding {
        private final ProgramIdParagraph t;

        public CobolRightPadded<Space> getProgramId() {
            return t.programId;
        }

        public ProgramIdParagraph withProgramId(CobolRightPadded<Space> programId) {
            return t.programId == programId ? t : new ProgramIdParagraph(t.padding, t.id, t.prefix, t.markers, programId, t.programName);
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
        Space stop;
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStop(this, p);
        }
    }
}
