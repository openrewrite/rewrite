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

CobolContainer<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSentence(this, p);
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public Sentence withStatements(List<Statement> statements) {
            return getPadding().withStatements(this.statements.getPadding().withElements(CobolRightPadded.withElements(
                    this.statements.getPadding().getElements(), statements)));
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
        private final Sentence t;

        public CobolContainer<Statement> getStatements() {
            return t.statements;
        }

        public Sentence withStatements(CobolContainer<Statement> statements) {
            return t.statements == statements ? t : new Sentence(t.padding, t.id, t.prefix, t.markers, statements);
        }
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
}
