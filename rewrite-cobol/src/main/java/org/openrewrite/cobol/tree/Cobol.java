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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Abbreviation implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String not;

        @Nullable
        RelationalOperator relationalOperator;

        @Nullable
        String leftParen;

        Cobol arithmeticExpression;

        @Nullable
        Cobol abbreviation;

        @Nullable
        String rightParen;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAbbreviation(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Accept implements Statement {
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
        String accept;

        @Getter
        @With
        Identifier identifier;

        @Getter
        @With
        Cobol operation;

        @Getter
        @Nullable
        @With
        StatementPhrase onExceptionClause;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnExceptionClause;

        @Nullable
        CobolLeftPadded<String> endAccept;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAccept(this, p);
        }

        @Nullable
        public String getEndAccept() {
            return endAccept == null ? null : endAccept.getElement();
        }

        public Accept withEndAccept(@Nullable String endAccept) {
            if (endAccept == null) {
                return this.endAccept == null ? this : new Accept(id, prefix, markers, accept, identifier, operation, onExceptionClause, notOnExceptionClause, null);
            }
            return getPadding().withEndAccept(CobolLeftPadded.withElement(this.endAccept, endAccept));
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
            private final Accept t;

            @Nullable
            public CobolLeftPadded<String> getEndAccept() {
                return t.endAccept;
            }

            public Accept withEndAccept(@Nullable CobolLeftPadded<String> endAccept) {
                return t.endAccept == endAccept ? t : new Accept(t.padding, t.id, t.prefix, t.markers, t.accept, t.identifier, t.operation, t.onExceptionClause, t.notOnExceptionClause, endAccept);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AcceptFromDateStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAcceptFromDateStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AcceptFromMnemonicStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String from;

        Identifier mnemonicName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAcceptFromMnemonicStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AcceptFromEscapeKeyStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAcceptFromEscapeKeyStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AcceptMessageCountStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAcceptMessageCountStatement(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AccessModeClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAccessModeClause(this, p);
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
    class AlteredGoTo implements Cobol {
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

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlteredGoTo(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public AlteredGoTo withDot(String dot) {
            //noinspection ConstantConditions
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
            private final AlteredGoTo t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public AlteredGoTo withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new AlteredGoTo(t.padding, t.id, t.prefix, t.markers, t.words, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AlphabetClause implements Cobol {
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

        @Getter
        @With
        Identifier name;

        @Nullable
        CobolLeftPadded<String> standard;

        @Nullable
        CobolContainer<AlphabetLiteral> literals;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlphabetClause(this, p);
        }

        @Nullable
        public String getStandard() {
            return standard == null ? null : standard.getElement();
        }

        public AlphabetClause withStandard(@Nullable String standard) {
            if (standard == null) {
                return this.standard == null ? this : new AlphabetClause(id, prefix, markers, words, name, null, literals);
            }
            return getPadding().withStandard(CobolLeftPadded.withElement(this.standard, standard));
        }

        public List<Cobol.AlphabetLiteral> getLiterals() {
            return literals.getElements();
        }

        public AlphabetClause withLiterals(List<Cobol.AlphabetLiteral> literals) {
            return getPadding().withLiterals(this.literals.getPadding().withElements(CobolRightPadded.withElements(
                    this.literals.getPadding().getElements(), literals)));
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
            private final AlphabetClause t;

            @Nullable
            public CobolLeftPadded<String> getStandard() {
                return t.standard;
            }

            public AlphabetClause withStandard(@Nullable CobolLeftPadded<String> standard) {
                return t.standard == standard ? t : new AlphabetClause(t.padding, t.id, t.prefix, t.markers, t.words, t.name, standard, t.literals);
            }

            @Nullable
            public CobolContainer<Cobol.AlphabetLiteral> getLiterals() {
                return t.literals;
            }

            public AlphabetClause withLiterals(@Nullable CobolContainer<Cobol.AlphabetLiteral> literals) {
                return t.literals == literals ? t : new AlphabetClause(t.padding, t.id, t.prefix, t.markers, t.words, t.name, t.standard, literals);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AlphabetLiteral implements Cobol {
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
        Literal literal;

        @Getter
        @Nullable
        @With
        AlphabetThrough alphabetThrough;

        @Nullable
        CobolContainer<AlphabetAlso> alphabetAlso;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlphabetLiteral(this, p);
        }

        public List<Cobol.AlphabetAlso> getAlphabetAlso() {
            return alphabetAlso.getElements();
        }

        public AlphabetLiteral withAlphabetAlso(List<Cobol.AlphabetAlso> alphabetAlso) {
            return getPadding().withAlphabetAlso(this.alphabetAlso.getPadding().withElements(CobolRightPadded.withElements(
                    this.alphabetAlso.getPadding().getElements(), alphabetAlso)));
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
            private final AlphabetLiteral t;

            @Nullable
            public CobolContainer<Cobol.AlphabetAlso> getAlphabetAlso() {
                return t.alphabetAlso;
            }

            public AlphabetLiteral withAlphabetAlso(@Nullable CobolContainer<Cobol.AlphabetAlso> alphabetAlso) {
                return t.alphabetAlso == alphabetAlso ? t : new AlphabetLiteral(t.padding, t.id, t.prefix, t.markers, t.literal, t.alphabetThrough, alphabetAlso);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AlphabetThrough implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Literal literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlphabetThrough(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AlphabetAlso implements Cobol {
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

        CobolContainer<Literal> literals;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlphabetAlso(this, p);
        }

        public List<Literal> getLiterals() {
            return literals.getElements();
        }

        public AlphabetAlso withLiterals(List<Literal> literals) {
            return getPadding().withLiterals(this.literals.getPadding().withElements(CobolRightPadded.withElements(
                    this.literals.getPadding().getElements(), literals)));
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
            private final AlphabetAlso t;

            public CobolContainer<Literal> getLiterals() {
                return t.literals;
            }

            public AlphabetAlso withLiterals(CobolContainer<Literal> literals) {
                return t.literals == literals ? t : new AlphabetAlso(t.padding, t.id, t.prefix, t.markers, t.words, literals);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AlternateRecordKeyClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String alternateWords;
        QualifiedDataName qualifiedDataName;

        @Nullable
        PasswordClause passwordClause;

        String duplicates;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlternateRecordKeyClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AlterProceedTo implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        ProcedureName from;
        String words;
        ProcedureName to;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlterProceedTo(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AlterStatement implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        List<AlterProceedTo> alterProceedTo;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAlterStatement(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class AndOrCondition implements Cobol {
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
        String logicalOperator;

        @Getter
        @Nullable
        @With
        CombinableCondition combinableCondition;

        @Nullable
        CobolContainer<Cobol> abbreviations;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAndOrCondition(this, p);
        }

        public List<Cobol> getAbbreviations() {
            return abbreviations.getElements();
        }

        public AndOrCondition withAbbreviations(List<Cobol> abbreviations) {
            return getPadding().withAbbreviations(this.abbreviations.getPadding().withElements(CobolRightPadded.withElements(
                    this.abbreviations.getPadding().getElements(), abbreviations)));
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
            private final AndOrCondition t;

            @Nullable
            public CobolContainer<Cobol> getAbbreviations() {
                return t.abbreviations;
            }

            public AndOrCondition withAbbreviations(@Nullable CobolContainer<Cobol> abbreviations) {
                return t.abbreviations == abbreviations ? t : new AndOrCondition(t.padding, t.id, t.prefix, t.markers, t.logicalOperator, t.combinableCondition, abbreviations);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Argument implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        Cobol first;

        @Nullable
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitArgument(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class AssignClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitAssignClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class BlockContainsClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String firstWords;
        CobolWord integerLiteral;

        @Nullable
        BlockContainsTo blockContainsTo;

        String lastWords;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitBlockContainsClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class BlockContainsTo implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String to;
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitBlockContainsTo(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CommunicationSection implements Cobol {
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

        CobolContainer<Cobol> entries;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCommunicationSection(this, p);
        }

        public List<Cobol> getEntries() {
            return entries.getElements();
        }

        public CommunicationSection withEntries(List<Cobol> entries) {
            return getPadding().withEntries(this.entries.getPadding().withElements(CobolRightPadded.withElements(
                    this.entries.getPadding().getElements(), entries)));
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
            private final CommunicationSection t;

            public CobolContainer<Cobol> getEntries() {
                return t.entries;
            }

            public CommunicationSection withEntries(CobolContainer<Cobol> entries) {
                return t.entries == entries ? t : new CommunicationSection(t.padding, t.id, t.prefix, t.markers, t.words, entries);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CommunicationDescriptionEntryFormat1 implements Cobol {
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
        String cd;

        @Getter
        @With
        CobolWord name;

        @Getter
        @With
        String words;

        CobolContainer<Cobol> inputs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCommunicationDescriptionEntryFormat1(this, p);
        }

        public List<Cobol> getInputs() {
            return inputs.getElements();
        }

        public CommunicationDescriptionEntryFormat1 withInputs(List<Cobol> inputs) {
            return getPadding().withInputs(this.inputs.getPadding().withElements(CobolRightPadded.withElements(
                    this.inputs.getPadding().getElements(), inputs)));
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
            private final CommunicationDescriptionEntryFormat1 t;

            public CobolContainer<Cobol> getInputs() {
                return t.inputs;
            }

            public CommunicationDescriptionEntryFormat1 withInputs(CobolContainer<Cobol> inputs) {
                return t.inputs == inputs ? t : new CommunicationDescriptionEntryFormat1(t.padding, t.id, t.prefix, t.markers, t.cd, t.name, t.words, inputs);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CommunicationDescriptionEntryFormat2 implements Cobol {
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
        String cd;

        @Getter
        @With
        CobolWord name;

        @Getter
        @With
        String words;

        CobolContainer<Cobol> outputs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCommunicationDescriptionEntryFormat2(this, p);
        }

        public List<Cobol> getOutputs() {
            return outputs.getElements();
        }

        public CommunicationDescriptionEntryFormat2 withOutputs(List<Cobol> outputs) {
            return getPadding().withOutputs(this.outputs.getPadding().withElements(CobolRightPadded.withElements(
                    this.outputs.getPadding().getElements(), outputs)));
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
            private final CommunicationDescriptionEntryFormat2 t;

            public CobolContainer<Cobol> getOutputs() {
                return t.outputs;
            }

            public CommunicationDescriptionEntryFormat2 withOutputs(CobolContainer<Cobol> outputs) {
                return t.outputs == outputs ? t : new CommunicationDescriptionEntryFormat2(t.padding, t.id, t.prefix, t.markers, t.cd, t.name, t.words, outputs);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CommunicationDescriptionEntryFormat3 implements Cobol {
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
        String cd;

        @Getter
        @With
        CobolWord name;

        @Getter
        @With
        String words;

        CobolContainer<Cobol> initialIOs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCommunicationDescriptionEntryFormat3(this, p);
        }

        public List<Cobol> getInitialIOs() {
            return initialIOs.getElements();
        }

        public CommunicationDescriptionEntryFormat3 withInitialIOs(List<Cobol> initialIOs) {
            return getPadding().withInitialIOs(this.initialIOs.getPadding().withElements(CobolRightPadded.withElements(
                    this.initialIOs.getPadding().getElements(), initialIOs)));
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
            private final CommunicationDescriptionEntryFormat3 t;

            public CobolContainer<Cobol> getInitialIOs() {
                return t.initialIOs;
            }

            public CommunicationDescriptionEntryFormat3 withInitialIOs(CobolContainer<Cobol> initialIOs) {
                return t.initialIOs == initialIOs ? t : new CommunicationDescriptionEntryFormat3(t.padding, t.id, t.prefix, t.markers, t.cd, t.name, t.words, initialIOs);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Call implements Statement {
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
        String call;

        @Getter
        @With
        Name identifier;

        @Getter
        @Nullable
        @With
        CallPhrase callUsingPhrase;

        @Getter
        @Nullable
        @With
        CallGivingPhrase callGivingPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase onOverflowPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase onExceptionClause;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnExceptionClause;

        @Nullable
        CobolLeftPadded<String> endCall;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCall(this, p);
        }

        @Nullable
        public String getEndCall() {
            return endCall == null ? null : endCall.getElement();
        }

        public Call withEndCall(@Nullable String endCall) {
            if (endCall == null) {
                return this.endCall == null ? this : new Call(id, prefix, markers, call, identifier, callUsingPhrase, callGivingPhrase, onOverflowPhrase, onExceptionClause, notOnExceptionClause, null);
            }
            return getPadding().withEndCall(CobolLeftPadded.withElement(this.endCall, endCall));
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
            private final Call t;

            @Nullable
            public CobolLeftPadded<String> getEndCall() {
                return t.endCall;
            }

            public Call withEndCall(@Nullable CobolLeftPadded<String> endCall) {
                return t.endCall == endCall ? t : new Call(t.padding, t.id, t.prefix, t.markers, t.call, t.identifier, t.callUsingPhrase, t.callGivingPhrase, t.onOverflowPhrase, t.onExceptionClause, t.notOnExceptionClause, endCall);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CallPhrase implements Cobol {
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

        CobolContainer<Cobol> parameters;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCallPhrase(this, p);
        }

        public List<Cobol> getParameters() {
            return parameters.getElements();
        }

        public CallPhrase withParameters(List<Cobol> parameters) {
            return getPadding().withParameters(this.parameters.getPadding().withElements(CobolRightPadded.withElements(
                    this.parameters.getPadding().getElements(), parameters)));
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
            private final CallPhrase t;

            public CobolContainer<Cobol> getParameters() {
                return t.parameters;
            }

            public CallPhrase withParameters(CobolContainer<Cobol> parameters) {
                return t.parameters == parameters ? t : new CallPhrase(t.padding, t.id, t.prefix, t.markers, t.words, parameters);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ArithmeticExpression implements Cobol {
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
        MultDivs multDivs;

        @Nullable
        CobolContainer<PlusMinus> plusMinuses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitArithmeticExpression(this, p);
        }

        public List<Cobol.PlusMinus> getPlusMinuses() {
            return plusMinuses.getElements();
        }

        public ArithmeticExpression withPlusMinuses(List<Cobol.PlusMinus> plusMinuses) {
            return getPadding().withPlusMinuses(this.plusMinuses.getPadding().withElements(CobolRightPadded.withElements(
                    this.plusMinuses.getPadding().getElements(), plusMinuses)));
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
            private final ArithmeticExpression t;

            public CobolContainer<Cobol.PlusMinus> getPlusMinuses() {
                return t.plusMinuses;
            }

            public ArithmeticExpression withPlusMinuses(CobolContainer<Cobol.PlusMinus> plusMinuses) {
                return t.plusMinuses == plusMinuses ? t : new ArithmeticExpression(t.padding, t.id, t.prefix, t.markers, t.multDivs, plusMinuses);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CallGivingPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCallGivingPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CallBy implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Nullable
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCallBy(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Cancel implements Statement {
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
        String cancel;

        CobolContainer<CancelCall> cancelCalls;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCancel(this, p);
        }

        public List<Cobol.CancelCall> getCancelCalls() {
            return cancelCalls.getElements();
        }

        public Cancel withCancelCalls(List<Cobol.CancelCall> cancelCalls) {
            return getPadding().withCancelCalls(this.cancelCalls.getPadding().withElements(CobolRightPadded.withElements(
                    this.cancelCalls.getPadding().getElements(), cancelCalls)));
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
            private final Cancel t;

            public CobolContainer<Cobol.CancelCall> getCancelCalls() {
                return t.cancelCalls;
            }

            public Cancel withCancelCalls(CobolContainer<Cobol.CancelCall> cancelCalls) {
                return t.cancelCalls == cancelCalls ? t : new Cancel(t.padding, t.id, t.prefix, t.markers, t.cancel, cancelCalls);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CancelCall implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        Name libraryName;

        @Nullable
        String by;

        @Nullable
        Identifier identifier;

        @Nullable
        Literal literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCancelCall(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ChannelClause implements Cobol {
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

        @Getter
        @With
        Literal literal;

        @Nullable
        CobolLeftPadded<String> is;

        @Getter
        @With
        Identifier mnemonicName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitChannelClause(this, p);
        }

        @Nullable
        public String getIs() {
            return is == null ? null : is.getElement();
        }

        public ChannelClause withIs(@Nullable String is) {
            if (is == null) {
                return this.is == null ? this : new ChannelClause(id, prefix, markers, words, literal, null, mnemonicName);
            }
            return getPadding().withIs(CobolLeftPadded.withElement(this.is, is));
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
            private final ChannelClause t;

            @Nullable
            public CobolLeftPadded<String> getIs() {
                return t.is;
            }

            public ChannelClause withIs(@Nullable CobolLeftPadded<String> is) {
                return t.is == is ? t : new ChannelClause(t.padding, t.id, t.prefix, t.markers, t.words, t.literal, is, t.mnemonicName);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ClassClause implements Cobol {
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
        String clazz;

        @Getter
        @With
        CobolWord className;

        @Getter
        @With
        String words;

        CobolContainer<ClassClauseThrough> throughs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClassClause(this, p);
        }

        public List<Cobol.ClassClauseThrough> getThroughs() {
            return throughs.getElements();
        }

        public ClassClause withThroughs(List<Cobol.ClassClauseThrough> throughs) {
            return getPadding().withThroughs(this.throughs.getPadding().withElements(CobolRightPadded.withElements(
                    this.throughs.getPadding().getElements(), throughs)));
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
            private final ClassClause t;

            public CobolContainer<Cobol.ClassClauseThrough> getThroughs() {
                return t.throughs;
            }

            public ClassClause withThroughs(CobolContainer<Cobol.ClassClauseThrough> throughs) {
                return t.throughs == throughs ? t : new ClassClause(t.padding, t.id, t.prefix, t.markers, t.clazz, t.className, t.words, throughs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ClassClauseThrough implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        Name from;
        @Nullable
        CobolWord through;
        @Nullable
        Name to;
        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClassClauseThrough(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ClassCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;

        @Nullable
        String words;

        @Nullable
        Name className;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClassCondition(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Close implements Statement {
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
        String close;

        CobolContainer<CloseFile> closeFiles;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClose(this, p);
        }

        public List<Cobol.CloseFile> getCloseFiles() {
            return closeFiles.getElements();
        }

        public Close withCloseFiles(List<Cobol.CloseFile> closeFiles) {
            return getPadding().withCloseFiles(this.closeFiles.getPadding().withElements(CobolRightPadded.withElements(
                    this.closeFiles.getPadding().getElements(), closeFiles)));
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
            private final Close t;

            public CobolContainer<Cobol.CloseFile> getCloseFiles() {
                return t.closeFiles;
            }

            public Close withCloseFiles(CobolContainer<Cobol.CloseFile> closeFiles) {
                return t.closeFiles == closeFiles ? t : new Close(t.padding, t.id, t.prefix, t.markers, t.close, closeFiles);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CloseFile implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name fileName;

        @Nullable
        Cobol closeStatement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCloseFile(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CloseReelUnitStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCloseReelUnitStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CloseRelativeStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCloseRelativeStatement(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ClosePortFileIOStatement implements Cobol {
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

        CobolContainer<Cobol> closePortFileIOUsing;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClosePortFileIOStatement(this, p);
        }

        public List<Cobol> getClosePortFileIOUsing() {
            return closePortFileIOUsing.getElements();
        }

        public ClosePortFileIOStatement withClosePortFileIOUsing(List<Cobol> closePortFileIOUsing) {
            return getPadding().withClosePortFileIOUsing(this.closePortFileIOUsing.getPadding().withElements(CobolRightPadded.withElements(
                    this.closePortFileIOUsing.getPadding().getElements(), closePortFileIOUsing)));
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
            private final ClosePortFileIOStatement t;

            public CobolContainer<Cobol> getClosePortFileIOUsing() {
                return t.closePortFileIOUsing;
            }

            public ClosePortFileIOStatement withClosePortFileIOUsing(CobolContainer<Cobol> closePortFileIOUsing) {
                return t.closePortFileIOUsing == closePortFileIOUsing ? t : new ClosePortFileIOStatement(t.padding, t.id, t.prefix, t.markers, t.words, closePortFileIOUsing);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ClosePortFileIOUsingCloseDisposition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClosePortFileIOUsingCloseDisposition(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ClosePortFileIOUsingAssociatedData implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String associatedData;

        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClosePortFileIOUsingAssociatedData(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ClosePortFileIOUsingAssociatedDataLength implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitClosePortFileIOUsingAssociatedDataLength(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    public class CobolWord implements Literal, Identifier {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCobolWord(this, p);
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

        public List<Identifier> getAlphabetName() {
            return alphabetName.getElements();
        }

        public CollatingSequenceClause withAlphabetName(List<Identifier> alphabetName) {
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

            public CobolContainer<Identifier> getAlphabetName() {
                return t.alphabetName;
            }

            public CollatingSequenceClause withAlphabetName(CobolContainer<Identifier> alphabetName) {
                return t.alphabetName == alphabetName ? t : new CollatingSequenceClause(t.padding, t.id, t.prefix, t.markers, t.words, alphabetName, t.alphanumeric, t.national);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CodeSetClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord alphabetName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCodeSetClause(this, p);
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CommitmentControlClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord fileName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCommitmentControlClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Compute implements Statement {
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
        String compute;

        CobolContainer<Roundable> roundables;

        @Getter
        @With
        String equalWord;

        @Getter
        @With
        ArithmeticExpression arithmeticExpression;

        @Getter
        @With
        StatementPhrase onSizeErrorPhrase;

        @Getter
        @With
        StatementPhrase notOnSizeErrorPhrase;

        @Getter
        @With
        String endCompute;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCompute(this, p);
        }

        public List<Roundable> getRoundables() {
            return roundables.getElements();
        }

        public Compute withRoundables(List<Roundable> roundables) {
            return getPadding().withRoundables(this.roundables.getPadding().withElements(CobolRightPadded.withElements(
                    this.roundables.getPadding().getElements(), roundables)));
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
            private final Compute t;

            public CobolContainer<Roundable> getRoundables() {
                return t.roundables;
            }

            public Compute withRoundables(CobolContainer<Cobol.Roundable> roundables) {
                return t.roundables == roundables ? t : new Compute(t.padding, t.id, t.prefix, t.markers, t.compute, roundables, t.equalWord, t.arithmeticExpression, t.onSizeErrorPhrase, t.notOnSizeErrorPhrase, t.endCompute);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CombinableCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String not;

        Cobol simpleCondition;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCombinableCondition(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Condition implements Cobol {
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
        CombinableCondition combinableCondition;

        CobolContainer<AndOrCondition> andOrConditions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCondition(this, p);
        }

        public List<Cobol.AndOrCondition> getAndOrConditions() {
            return andOrConditions.getElements();
        }

        public Condition withAndOrConditions(List<Cobol.AndOrCondition> andOrConditions) {
            return getPadding().withAndOrConditions(this.andOrConditions.getPadding().withElements(CobolRightPadded.withElements(
                    this.andOrConditions.getPadding().getElements(), andOrConditions)));
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
            private final Condition t;

            public CobolContainer<Cobol.AndOrCondition> getAndOrConditions() {
                return t.andOrConditions;
            }

            public Condition withAndOrConditions(CobolContainer<Cobol.AndOrCondition> andOrConditions) {
                return t.andOrConditions == andOrConditions ? t : new Condition(t.padding, t.id, t.prefix, t.markers, t.combinableCondition, andOrConditions);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ConditionNameReference implements Cobol {
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
        Name name;

        @Nullable
        CobolContainer<InData> inDatas;

        @Getter
        @Nullable
        @With
        InFile inFile;

        @Nullable
        CobolContainer<Parenthesized> references;

        @Nullable
        CobolContainer<InMnemonic> inMnemonics;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitConditionNameReference(this, p);
        }

        public List<Cobol.InData> getInDatas() {
            return inDatas.getElements();
        }

        public ConditionNameReference withInDatas(List<Cobol.InData> inDatas) {
            return getPadding().withInDatas(this.inDatas.getPadding().withElements(CobolRightPadded.withElements(
                    this.inDatas.getPadding().getElements(), inDatas)));
        }

        public List<Cobol.Parenthesized> getReferences() {
            return references.getElements();
        }

        public ConditionNameReference withReferences(List<Cobol.Parenthesized> references) {
            return getPadding().withReferences(this.references.getPadding().withElements(CobolRightPadded.withElements(
                    this.references.getPadding().getElements(), references)));
        }

        public List<Cobol.InMnemonic> getInMnemonics() {
            return inMnemonics.getElements();
        }

        public ConditionNameReference withInMnemonics(List<Cobol.InMnemonic> inMnemonics) {
            return getPadding().withInMnemonics(this.inMnemonics.getPadding().withElements(CobolRightPadded.withElements(
                    this.inMnemonics.getPadding().getElements(), inMnemonics)));
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
            private final ConditionNameReference t;

            @Nullable
            public CobolContainer<Cobol.InData> getInDatas() {
                return t.inDatas;
            }

            public ConditionNameReference withInDatas(@Nullable CobolContainer<Cobol.InData> inDatas) {
                return t.inDatas == inDatas ? t : new ConditionNameReference(t.padding, t.id, t.prefix, t.markers, t.name, inDatas, t.inFile, t.references, t.inMnemonics);
            }

            @Nullable
            public CobolContainer<Cobol.Parenthesized> getReferences() {
                return t.references;
            }

            public ConditionNameReference withReferences(@Nullable CobolContainer<Cobol.Parenthesized> references) {
                return t.references == references ? t : new ConditionNameReference(t.padding, t.id, t.prefix, t.markers, t.name, t.inDatas, t.inFile, references, t.inMnemonics);
            }

            @Nullable
            public CobolContainer<Cobol.InMnemonic> getInMnemonics() {
                return t.inMnemonics;
            }

            public ConditionNameReference withInMnemonics(@Nullable CobolContainer<Cobol.InMnemonic> inMnemonics) {
                return t.inMnemonics == inMnemonics ? t : new ConditionNameReference(t.padding, t.id, t.prefix, t.markers, t.name, t.inDatas, t.inFile, t.references, inMnemonics);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class ConditionNameSubscriptReference implements Cobol {
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
        String leftParen;

        @Getter
        @With
        List<Cobol> subscripts;

        @Getter
        @With
        String rightParen;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitConditionNameSubscriptReference(this, p);
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Continue implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitContinue(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class CurrencyClause implements Cobol {
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

        @Getter
        @With
        Literal literal;

        @Nullable
        CobolLeftPadded<String> pictureSymbol;

        @Getter
        @Nullable
        @With
        Literal pictureSymbolLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitCurrencyClause(this, p);
        }

        @Nullable
        public String getPictureSymbol() {
            return pictureSymbol == null ? null : pictureSymbol.getElement();
        }

        public CurrencyClause withPictureSymbol(@Nullable String pictureSymbol) {
            if (pictureSymbol == null) {
                return this.pictureSymbol == null ? this : new CurrencyClause(id, prefix, markers, words, literal, null, pictureSymbolLiteral);
            }
            return getPadding().withPictureSymbol(CobolLeftPadded.withElement(this.pictureSymbol, pictureSymbol));
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
            private final CurrencyClause t;

            @Nullable
            public CobolLeftPadded<String> getPictureSymbol() {
                return t.pictureSymbol;
            }

            public CurrencyClause withPictureSymbol(@Nullable CobolLeftPadded<String> pictureSymbol) {
                return t.pictureSymbol == pictureSymbol ? t : new CurrencyClause(t.padding, t.id, t.prefix, t.markers, t.words, t.literal, pictureSymbol, t.pictureSymbolLiteral);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DataBaseSection implements Cobol {
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

        CobolContainer<DataBaseSectionEntry> entries;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataBaseSection(this, p);
        }

        public List<Cobol.DataBaseSectionEntry> getEntries() {
            return entries.getElements();
        }

        public DataBaseSection withEntries(List<Cobol.DataBaseSectionEntry> entries) {
            return getPadding().withEntries(this.entries.getPadding().withElements(CobolRightPadded.withElements(
                    this.entries.getPadding().getElements(), entries)));
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
            private final DataBaseSection t;

            public CobolContainer<Cobol.DataBaseSectionEntry> getEntries() {
                return t.entries;
            }

            public DataBaseSection withEntries(CobolContainer<Cobol.DataBaseSectionEntry> entries) {
                return t.entries == entries ? t : new DataBaseSection(t.padding, t.id, t.prefix, t.markers, t.words, entries);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DataBaseSectionEntry implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String db;

        Literal from;
        String invoke;
        Literal to;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataBaseSectionEntry(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DataDivision implements Cobol {
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
        String level;

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
    class DataRecordsClause implements Cobol {
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

        CobolContainer<Name> dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDataRecordsClause(this, p);
        }

        public List<Name> getDataName() {
            return dataName.getElements();
        }

        public DataRecordsClause withDataName(List<Name> dataName) {
            return getPadding().withDataName(this.dataName.getPadding().withElements(CobolRightPadded.withElements(
                    this.dataName.getPadding().getElements(), dataName)));
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
            private final DataRecordsClause t;

            public CobolContainer<Name> getDataName() {
                return t.dataName;
            }

            public DataRecordsClause withDataName(CobolContainer<Name> dataName) {
                return t.dataName == dataName ? t : new DataRecordsClause(t.padding, t.id, t.prefix, t.markers, t.words, dataName);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DecimalPointClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDecimalPointClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DefaultComputationalSignClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDefaultComputationalSignClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DefaultDisplaySignClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDefaultDisplaySignClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Delete implements Statement {
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
        String delete;

        @Getter
        @With
        Name fileName;

        @Getter
        @Nullable
        @With
        String record;

        @Getter
        @Nullable
        @With
        StatementPhrase invalidKey;

        @Getter
        @Nullable
        @With
        StatementPhrase notInvalidKey;

        @Nullable
        CobolLeftPadded<String> endDelete;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDelete(this, p);
        }

        @Nullable
        public String getEndDelete() {
            return endDelete == null ? null : endDelete.getElement();
        }

        public Delete withEndDelete(@Nullable String endDelete) {
            if (endDelete == null) {
                return this.endDelete == null ? this : new Delete(id, prefix, markers, delete, fileName, record, invalidKey, notInvalidKey, null);
            }
            return getPadding().withEndDelete(CobolLeftPadded.withElement(this.endDelete, endDelete));
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
            private final Delete t;

            @Nullable
            public CobolLeftPadded<String> getEndDelete() {
                return t.endDelete;
            }

            public Delete withEndDelete(@Nullable CobolLeftPadded<String> endDelete) {
                return t.endDelete == endDelete ? t : new Delete(t.padding, t.id, t.prefix, t.markers, t.delete, t.fileName, t.record, t.invalidKey, t.notInvalidKey, endDelete);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Disable implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String disable;
        String type;
        Name cdName;

        @Nullable
        String with;

        String key;
        Name keyName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDisable(this, p);
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Divide implements Statement {
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
        String divide;

        @Getter
        @With
        Name name;

        @Getter
        @With
        Cobol action;

        @Getter
        @Nullable
        @With
        DivideRemainder divideRemainder;

        @Getter
        @Nullable
        @With
        StatementPhrase onSizeErrorPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnSizeErrorPhrase;

        @Nullable
        CobolLeftPadded<String> endDivide;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDivide(this, p);
        }

        @Nullable
        public String getEndDivide() {
            return endDivide == null ? null : endDivide.getElement();
        }

        public Divide withEndDivide(@Nullable String endDivide) {
            if (endDivide == null) {
                return this.endDivide == null ? this : new Divide(id, prefix, markers, divide, name, action, divideRemainder, onSizeErrorPhrase, notOnSizeErrorPhrase, null);
            }
            return getPadding().withEndDivide(CobolLeftPadded.withElement(this.endDivide, endDivide));
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
            private final Divide t;

            @Nullable
            public CobolLeftPadded<String> getEndDivide() {
                return t.endDivide;
            }

            public Divide withEndDivide(@Nullable CobolLeftPadded<String> endDivide) {
                return t.endDivide == endDivide ? t : new Divide(t.padding, t.id, t.prefix, t.markers, t.divide, t.name, t.action, t.divideRemainder, t.onSizeErrorPhrase, t.notOnSizeErrorPhrase, endDivide);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DivideInto implements Cobol {
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
        String into;

        CobolContainer<Roundable> roundable;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDivideInto(this, p);
        }

        public List<Cobol.Roundable> getRoundable() {
            return roundable.getElements();
        }

        public DivideInto withRoundable(List<Cobol.Roundable> roundable) {
            return getPadding().withRoundable(this.roundable.getPadding().withElements(CobolRightPadded.withElements(
                    this.roundable.getPadding().getElements(), roundable)));
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
            private final DivideInto t;

            public CobolContainer<Cobol.Roundable> getRoundable() {
                return t.roundable;
            }

            public DivideInto withRoundable(CobolContainer<Cobol.Roundable> roundable) {
                return t.roundable == roundable ? t : new DivideInto(t.padding, t.id, t.prefix, t.markers, t.into, roundable);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DivideGiving implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;
        Name name;

        @Nullable
        DivideGivingPhrase divideGivingPhrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDivideGiving(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class DivideGivingPhrase implements Cobol {
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
        String giving;

        CobolContainer<Roundable> roundable;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDivideGivingPhrase(this, p);
        }

        public List<Cobol.Roundable> getRoundable() {
            return roundable.getElements();
        }

        public DivideGivingPhrase withRoundable(List<Cobol.Roundable> roundable) {
            return getPadding().withRoundable(this.roundable.getPadding().withElements(CobolRightPadded.withElements(
                    this.roundable.getPadding().getElements(), roundable)));
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
            private final DivideGivingPhrase t;

            public CobolContainer<Cobol.Roundable> getRoundable() {
                return t.roundable;
            }

            public DivideGivingPhrase withRoundable(CobolContainer<Cobol.Roundable> roundable) {
                return t.roundable == roundable ? t : new DivideGivingPhrase(t.padding, t.id, t.prefix, t.markers, t.giving, roundable);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DivideRemainder implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String remainder;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitDivideRemainder(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Enable implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String enable;
        String type;
        Name cdName;

        @Nullable
        String with;

        String key;
        Name keyName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEnable(this, p);
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
    class Entry implements Statement {
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
        String entry;

        @Getter
        @With
        Literal literal;

        @Nullable
        CobolContainer<Identifier> identifiers;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEntry(this, p);
        }

        public List<Identifier> getIdentifiers() {
            return identifiers.getElements();
        }

        public Entry withIdentifiers(List<Identifier> identifiers) {
            return getPadding().withIdentifiers(this.identifiers.getPadding().withElements(CobolRightPadded.withElements(
                    this.identifiers.getPadding().getElements(), identifiers)));
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
            private final Entry t;

            @Nullable
            public CobolContainer<Identifier> getIdentifiers() {
                return t.identifiers;
            }

            public Entry withIdentifiers(@Nullable CobolContainer<Identifier> identifiers) {
                return t.identifiers == identifiers ? t : new Entry(t.padding, t.id, t.prefix, t.markers, t.entry, t.literal, identifiers);
            }
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Evaluate implements Statement {
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
        String evaluate;

        @Getter
        @With
        Cobol select;

        @Nullable
        CobolContainer<EvaluateAlso> alsoSelect;

        @Nullable
        CobolContainer<EvaluateWhenPhrase> whenPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase whenOther;

        CobolLeftPadded<String> endPhrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluate(this, p);
        }

        public List<Cobol.EvaluateAlso> getAlsoSelect() {
            return alsoSelect.getElements();
        }

        public Evaluate withAlsoSelect(List<Cobol.EvaluateAlso> alsoSelect) {
            return getPadding().withAlsoSelect(this.alsoSelect.getPadding().withElements(CobolRightPadded.withElements(
                    this.alsoSelect.getPadding().getElements(), alsoSelect)));
        }

        public List<Cobol.EvaluateWhenPhrase> getWhenPhrase() {
            return whenPhrase.getElements();
        }

        public Evaluate withWhenPhrase(List<Cobol.EvaluateWhenPhrase> whenPhrase) {
            return getPadding().withWhenPhrase(this.whenPhrase.getPadding().withElements(CobolRightPadded.withElements(
                    this.whenPhrase.getPadding().getElements(), whenPhrase)));
        }

        public String getEndPhrase() {
            return endPhrase.getElement();
        }

        public Evaluate withEndPhrase(String endPhrase) {
            //noinspection ConstantConditions
            return getPadding().withEndPhrase(CobolLeftPadded.withElement(this.endPhrase, endPhrase));
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
            private final Evaluate t;

            @Nullable
            public CobolContainer<Cobol.EvaluateAlso> getAlsoSelect() {
                return t.alsoSelect;
            }

            public Evaluate withAlsoSelect(@Nullable CobolContainer<Cobol.EvaluateAlso> alsoSelect) {
                return t.alsoSelect == alsoSelect ? t : new Evaluate(t.padding, t.id, t.prefix, t.markers, t.evaluate, t.select, alsoSelect, t.whenPhrase, t.whenOther, t.endPhrase);
            }

            @Nullable
            public CobolContainer<Cobol.EvaluateWhenPhrase> getWhenPhrase() {
                return t.whenPhrase;
            }

            public Evaluate withWhenPhrase(@Nullable CobolContainer<Cobol.EvaluateWhenPhrase> whenPhrase) {
                return t.whenPhrase == whenPhrase ? t : new Evaluate(t.padding, t.id, t.prefix, t.markers, t.evaluate, t.select, t.alsoSelect, whenPhrase, t.whenOther, t.endPhrase);
            }

            public CobolLeftPadded<String> getEndPhrase() {
                return t.endPhrase;
            }

            public Evaluate withEndPhrase(CobolLeftPadded<String> endPhrase) {
                return t.endPhrase == endPhrase ? t : new Evaluate(t.padding, t.id, t.prefix, t.markers, t.evaluate, t.select, t.alsoSelect, t.whenPhrase, t.whenOther, endPhrase);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EvaluateAlso implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String also;
        Cobol select;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateAlso(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EvaluateAlsoCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String also;

        EvaluateCondition condition;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateAlsoCondition(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EvaluateCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String words;

        @Nullable
        Cobol condition;

        @Nullable
        EvaluateThrough evaluateThrough;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateCondition(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EvaluateThrough implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String through;
        Cobol value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateThrough(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EvaluateValueThrough implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String not;

        Cobol value;

        @Nullable
        EvaluateThrough evaluateThrough;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateValueThrough(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class EvaluateWhen implements Cobol {
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
        String when;

        @Getter
        @With
        EvaluateCondition condition;

        @Nullable
        CobolContainer<EvaluateAlsoCondition> alsoCondition;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateWhen(this, p);
        }

        public List<Cobol.EvaluateAlsoCondition> getAlsoCondition() {
            return alsoCondition.getElements();
        }

        public EvaluateWhen withAlsoCondition(List<Cobol.EvaluateAlsoCondition> alsoCondition) {
            return getPadding().withAlsoCondition(this.alsoCondition.getPadding().withElements(CobolRightPadded.withElements(
                    this.alsoCondition.getPadding().getElements(), alsoCondition)));
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
            private final EvaluateWhen t;

            @Nullable
            public CobolContainer<Cobol.EvaluateAlsoCondition> getAlsoCondition() {
                return t.alsoCondition;
            }

            public EvaluateWhen withAlsoCondition(@Nullable CobolContainer<Cobol.EvaluateAlsoCondition> alsoCondition) {
                return t.alsoCondition == alsoCondition ? t : new EvaluateWhen(t.padding, t.id, t.prefix, t.markers, t.when, t.condition, alsoCondition);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class EvaluateWhenPhrase implements Statement {
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

        CobolContainer<EvaluateWhenPhrase> whens;

        @Nullable
        CobolContainer<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitEvaluateWhenPhrase(this, p);
        }

        public List<Cobol.EvaluateWhenPhrase> getWhens() {
            return whens.getElements();
        }

        public EvaluateWhenPhrase withWhens(List<Cobol.EvaluateWhenPhrase> whens) {
            return getPadding().withWhens(this.whens.getPadding().withElements(CobolRightPadded.withElements(
                    this.whens.getPadding().getElements(), whens)));
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public EvaluateWhenPhrase withStatements(List<Statement> statements) {
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
            private final EvaluateWhenPhrase t;

            public CobolContainer<Cobol.EvaluateWhenPhrase> getWhens() {
                return t.whens;
            }

            public EvaluateWhenPhrase withWhens(CobolContainer<Cobol.EvaluateWhenPhrase> whens) {
                return t.whens == whens ? t : new EvaluateWhenPhrase(t.padding, t.id, t.prefix, t.markers, whens, t.statements);
            }

            @Nullable
            public CobolContainer<Statement> getStatements() {
                return t.statements;
            }

            public EvaluateWhenPhrase withStatements(@Nullable CobolContainer<Statement> statements) {
                return t.statements == statements ? t : new EvaluateWhenPhrase(t.padding, t.id, t.prefix, t.markers, t.whens, statements);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ExecCicsStatement implements Statement {
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

        CobolContainer<CobolWord> execCicsLines;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExecCicsStatement(this, p);
        }

        public List<Cobol.CobolWord> getExecCicsLines() {
            return execCicsLines.getElements();
        }

        public ExecCicsStatement withExecCicsLines(List<Cobol.CobolWord> execCicsLines) {
            return getPadding().withExecCicsLines(this.execCicsLines.getPadding().withElements(CobolRightPadded.withElements(
                    this.execCicsLines.getPadding().getElements(), execCicsLines)));
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
            private final ExecCicsStatement t;

            public CobolContainer<Cobol.CobolWord> getExecCicsLines() {
                return t.execCicsLines;
            }

            public ExecCicsStatement withExecCicsLines(CobolContainer<Cobol.CobolWord> execCicsLines) {
                return t.execCicsLines == execCicsLines ? t : new ExecCicsStatement(t.padding, t.id, t.prefix, t.markers, execCicsLines);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ExecSqlStatement implements Statement {
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

        CobolContainer<CobolWord> execSqlLines;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExecSqlStatement(this, p);
        }

        public List<Cobol.CobolWord> getExecSqlLines() {
            return execSqlLines.getElements();
        }

        public ExecSqlStatement withExecSqlLines(List<Cobol.CobolWord> execSqlLines) {
            return getPadding().withExecSqlLines(this.execSqlLines.getPadding().withElements(CobolRightPadded.withElements(
                    this.execSqlLines.getPadding().getElements(), execSqlLines)));
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
            private final ExecSqlStatement t;

            public CobolContainer<Cobol.CobolWord> getExecSqlLines() {
                return t.execSqlLines;
            }

            public ExecSqlStatement withExecSqlLines(CobolContainer<Cobol.CobolWord> execSqlLines) {
                return t.execSqlLines == execSqlLines ? t : new ExecSqlStatement(t.padding, t.id, t.prefix, t.markers, execSqlLines);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ExecSqlImsStatement implements Cobol {
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

        CobolContainer<CobolWord> execSqlLmsLines;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExecSqlImsStatement(this, p);
        }

        public List<Cobol.CobolWord> getExecSqlLmsLines() {
            return execSqlLmsLines.getElements();
        }

        public ExecSqlImsStatement withExecSqlLmsLines(List<Cobol.CobolWord> execSqlLmsLines) {
            return getPadding().withExecSqlLmsLines(this.execSqlLmsLines.getPadding().withElements(CobolRightPadded.withElements(
                    this.execSqlLmsLines.getPadding().getElements(), execSqlLmsLines)));
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
            private final ExecSqlImsStatement t;

            public CobolContainer<Cobol.CobolWord> getExecSqlLmsLines() {
                return t.execSqlLmsLines;
            }

            public ExecSqlImsStatement withExecSqlLmsLines(CobolContainer<Cobol.CobolWord> execSqlLmsLines) {
                return t.execSqlLmsLines == execSqlLmsLines ? t : new ExecSqlImsStatement(t.padding, t.id, t.prefix, t.markers, execSqlLmsLines);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Exhibit implements Statement {
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

        CobolContainer<Identifier> operands;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExhibit(this, p);
        }

        public List<Identifier> getOperands() {
            return operands.getElements();
        }

        public Exhibit withOperands(List<Identifier> operands) {
            return getPadding().withOperands(this.operands.getPadding().withElements(CobolRightPadded.withElements(
                    this.operands.getPadding().getElements(), operands)));
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
            private final Exhibit t;

            public CobolContainer<Identifier> getOperands() {
                return t.operands;
            }

            public Exhibit withOperands(CobolContainer<Identifier> operands) {
                return t.operands == operands ? t : new Exhibit(t.padding, t.id, t.prefix, t.markers, t.words, operands);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Exit implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExit(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ExternalClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitExternalClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileControlParagraph implements Cobol {
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
        String fileControl;

        @Nullable
        @Getter
        @With
        List<Cobol> controlEntries;

        @Nullable
        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFileControlParagraph(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public FileControlParagraph withDot(String dot) {
            //noinspection ConstantConditions
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
            private final FileControlParagraph t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public FileControlParagraph withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new FileControlParagraph(t.padding, t.id, t.prefix, t.markers, t.fileControl, t.controlEntries, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileControlEntry implements Cobol {
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
        Cobol selectClause;

        @Nullable
        CobolContainer<Cobol> controlClauses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFileControlEntry(this, p);
        }

        public List<Cobol> getControlClauses() {
            return controlClauses.getElements();
        }

        public FileControlEntry withControlClauses(List<Cobol> controlClauses) {
            return getPadding().withControlClauses(this.controlClauses.getPadding().withElements(CobolRightPadded.withElements(
                    this.controlClauses.getPadding().getElements(), controlClauses)));
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
            private final FileControlEntry t;

            @Nullable
            public CobolContainer<Cobol> getControlClauses() {
                return t.controlClauses;
            }

            public FileControlEntry withControlClauses(@Nullable CobolContainer<Cobol> controlClauses) {
                return t.controlClauses == controlClauses ? t : new FileControlEntry(t.padding, t.id, t.prefix, t.markers, t.selectClause, controlClauses);
            }
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileDescriptionEntry implements Cobol {
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

        @Getter
        @With
        CobolWord name;

        @Getter
        @With
        @Nullable
        List<Cobol> clauses;

        @Nullable
        CobolContainer<DataDescriptionEntry> dataDescriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFileDescriptionEntry(this, p);
        }

        public List<Cobol.DataDescriptionEntry> getDataDescriptions() {
            return dataDescriptions.getElements();
        }

        public FileDescriptionEntry withDataDescriptions(List<Cobol.DataDescriptionEntry> dataDescriptions) {
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
            private final FileDescriptionEntry t;

            @Nullable
            public CobolContainer<Cobol.DataDescriptionEntry> getDataDescriptions() {
                return t.dataDescriptions;
            }

            public FileDescriptionEntry withDataDescriptions(@Nullable CobolContainer<Cobol.DataDescriptionEntry> dataDescriptions) {
                return t.dataDescriptions == dataDescriptions ? t : new FileDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.words, t.name, t.clauses, dataDescriptions);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileSection implements DataDivisionSection {
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

        CobolContainer<FileDescriptionEntry> fileDescriptionEntry;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFileSection(this, p);
        }

        public List<Cobol.FileDescriptionEntry> getFileDescriptionEntry() {
            return fileDescriptionEntry.getElements();
        }

        public FileSection withFileDescriptionEntry(List<Cobol.FileDescriptionEntry> fileDescriptionEntry) {
            return getPadding().withFileDescriptionEntry(this.fileDescriptionEntry.getPadding().withElements(CobolRightPadded.withElements(
                    this.fileDescriptionEntry.getPadding().getElements(), fileDescriptionEntry)));
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
            private final FileSection t;

            public CobolContainer<Cobol.FileDescriptionEntry> getFileDescriptionEntry() {
                return t.fileDescriptionEntry;
            }

            public FileSection withFileDescriptionEntry(CobolContainer<Cobol.FileDescriptionEntry> fileDescriptionEntry) {
                return t.fileDescriptionEntry == fileDescriptionEntry ? t : new FileSection(t.padding, t.id, t.prefix, t.markers, t.words, fileDescriptionEntry);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FileStatusClause implements Cobol {
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

        CobolContainer<QualifiedDataName> qualifiedDataNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFileStatusClause(this, p);
        }

        public List<Cobol.QualifiedDataName> getQualifiedDataNames() {
            return qualifiedDataNames.getElements();
        }

        public FileStatusClause withQualifiedDataNames(List<Cobol.QualifiedDataName> qualifiedDataNames) {
            return getPadding().withQualifiedDataNames(this.qualifiedDataNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.qualifiedDataNames.getPadding().getElements(), qualifiedDataNames)));
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
            private final FileStatusClause t;

            @Nullable
            public CobolContainer<QualifiedDataName> getQualifiedDataNames() {
                return t.qualifiedDataNames;
            }

            public FileStatusClause withQualifiedDataNames(@Nullable CobolContainer<QualifiedDataName> qualifiedDataNames) {
                return t.qualifiedDataNames == qualifiedDataNames ? t : new FileStatusClause(t.padding, t.id, t.prefix, t.markers, t.words, qualifiedDataNames);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FunctionCall implements Identifier {
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
        String function;

        @Getter
        @With
        CobolWord functionName;

        CobolContainer<Parenthesized> arguments;

        @Getter
        @Nullable
        @With
        ReferenceModifier referenceModifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitFunctionCall(this, p);
        }

        public List<Cobol.Parenthesized> getArguments() {
            return arguments.getElements();
        }

        public FunctionCall withArguments(List<Cobol.Parenthesized> arguments) {
            return getPadding().withArguments(this.arguments.getPadding().withElements(CobolRightPadded.withElements(
                    this.arguments.getPadding().getElements(), arguments)));
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
            private final FunctionCall t;

            public CobolContainer<Cobol.Parenthesized> getArguments() {
                return t.arguments;
            }

            public FunctionCall withArguments(CobolContainer<Cobol.Parenthesized> arguments) {
                return t.arguments == arguments ? t : new FunctionCall(t.padding, t.id, t.prefix, t.markers, t.function, t.functionName, arguments, t.referenceModifier);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Generate implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String generate;
        QualifiedDataName reportName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitGenerate(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class GlobalClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitGlobalClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class GoBack implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord goBack;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitGoBack(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class GoTo implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitGoTo(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class GoToDependingOnStatement implements Cobol {
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

        @Nullable
        CobolContainer<ProcedureName> procedureNames;

        @Getter
        @With
        String words;

        @Getter
        @Nullable
        @With
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitGoToDependingOnStatement(this, p);
        }

        public List<Cobol.ProcedureName> getProcedureNames() {
            return procedureNames.getElements();
        }

        public GoToDependingOnStatement withProcedureNames(List<Cobol.ProcedureName> procedureNames) {
            return getPadding().withProcedureNames(this.procedureNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.procedureNames.getPadding().getElements(), procedureNames)));
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
            private final GoToDependingOnStatement t;

            @Nullable
            public CobolContainer<Cobol.ProcedureName> getProcedureNames() {
                return t.procedureNames;
            }

            public GoToDependingOnStatement withProcedureNames(@Nullable CobolContainer<Cobol.ProcedureName> procedureNames) {
                return t.procedureNames == procedureNames ? t : new GoToDependingOnStatement(t.padding, t.id, t.prefix, t.markers, procedureNames, t.words, t.identifier);
            }
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
    class If implements Statement {
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
        String word;

        @Getter
        @With
        Condition condition;

        @Getter
        @With
        IfThen ifThen;

        @Getter
        @Nullable
        @With
        IfElse ifElse;

        CobolLeftPadded<String> endIf;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIf(this, p);
        }

        public String getEndIf() {
            return endIf.getElement();
        }

        public If withEndIf(String endIf) {
            //noinspection ConstantConditions
            return getPadding().withEndIf(CobolLeftPadded.withElement(this.endIf, endIf));
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
            private final If t;

            public CobolLeftPadded<String> getEndIf() {
                return t.endIf;
            }

            public If withEndIf(CobolLeftPadded<String> endIf) {
                return t.endIf == endIf ? t : new If(t.padding, t.id, t.prefix, t.markers, t.word, t.condition, t.ifThen, t.ifElse, endIf);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IfThen implements Cobol {
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
        String word;

        @Getter
        @Nullable
        @With
        String nextSentence;

        @Nullable
        CobolContainer<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIfThen(this, p);
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public IfThen withStatements(List<Statement> statements) {
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
            private final IfThen t;

            @Nullable
            public CobolContainer<Statement> getStatements() {
                return t.statements;
            }

            public IfThen withStatements(@Nullable CobolContainer<Statement> statements) {
                return t.statements == statements ? t : new IfThen(t.padding, t.id, t.prefix, t.markers, t.word, t.nextSentence, statements);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IfElse implements Cobol {
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
        String word;

        @Getter
        @Nullable
        @With
        String nextSentence;

        @Nullable
        CobolContainer<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIfElse(this, p);
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public IfElse withStatements(List<Statement> statements) {
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
            private final IfElse t;

            @Nullable
            public CobolContainer<Statement> getStatements() {
                return t.statements;
            }

            public IfElse withStatements(@Nullable CobolContainer<Statement> statements) {
                return t.statements == statements ? t : new IfElse(t.padding, t.id, t.prefix, t.markers, t.word, t.nextSentence, statements);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InData implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInData(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InFile implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInFile(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InMnemonic implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInMnemonic(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InSection implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInSection(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InLibrary implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInLibrary(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InTable implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInTable(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Initialize implements Statement {
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
        String initialize;

        CobolContainer<Identifier> identifiers;

        @Getter
        @Nullable
        @With
        InitializeReplacingPhrase initializeReplacingPhrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInitialize(this, p);
        }

        public List<Identifier> getIdentifiers() {
            return identifiers.getElements();
        }

        public Initialize withIdentifiers(List<Identifier> identifiers) {
            return getPadding().withIdentifiers(this.identifiers.getPadding().withElements(CobolRightPadded.withElements(
                    this.identifiers.getPadding().getElements(), identifiers)));
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
            private final Initialize t;

            public CobolContainer<Identifier> getIdentifiers() {
                return t.identifiers;
            }

            public Initialize withIdentifiers(CobolContainer<Identifier> identifiers) {
                return t.identifiers == identifiers ? t : new Initialize(t.padding, t.id, t.prefix, t.markers, t.initialize, identifiers, t.initializeReplacingPhrase);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InitializeReplacingPhrase implements Cobol {
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
        String replacing;

        CobolContainer<InitializeReplacingBy> initializeReplacingBy;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInitializeReplacingPhrase(this, p);
        }

        public List<Cobol.InitializeReplacingBy> getInitializeReplacingBy() {
            return initializeReplacingBy.getElements();
        }

        public InitializeReplacingPhrase withInitializeReplacingBy(List<Cobol.InitializeReplacingBy> initializeReplacingBy) {
            return getPadding().withInitializeReplacingBy(this.initializeReplacingBy.getPadding().withElements(CobolRightPadded.withElements(
                    this.initializeReplacingBy.getPadding().getElements(), initializeReplacingBy)));
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
            private final InitializeReplacingPhrase t;

            public CobolContainer<Cobol.InitializeReplacingBy> getInitializeReplacingBy() {
                return t.initializeReplacingBy;
            }

            public InitializeReplacingPhrase withInitializeReplacingBy(CobolContainer<Cobol.InitializeReplacingBy> initializeReplacingBy) {
                return t.initializeReplacingBy == initializeReplacingBy ? t : new InitializeReplacingPhrase(t.padding, t.id, t.prefix, t.markers, t.replacing, initializeReplacingBy);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InitializeReplacingBy implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInitializeReplacingBy(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Initiate implements Statement {
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
        String initiate;

        CobolContainer<QualifiedDataName> reportNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInitiate(this, p);
        }

        public List<Cobol.QualifiedDataName> getReportNames() {
            return reportNames.getElements();
        }

        public Initiate withReportNames(List<Cobol.QualifiedDataName> reportNames) {
            return getPadding().withReportNames(this.reportNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.reportNames.getPadding().getElements(), reportNames)));
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
            private final Initiate t;

            public CobolContainer<Cobol.QualifiedDataName> getReportNames() {
                return t.reportNames;
            }

            public Initiate withReportNames(CobolContainer<Cobol.QualifiedDataName> reportNames) {
                return t.reportNames == reportNames ? t : new Initiate(t.padding, t.id, t.prefix, t.markers, t.initiate, reportNames);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InputOutputSection implements Cobol {
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

        @Nullable
        CobolContainer<Cobol> paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInputOutputSection(this, p);
        }

        public List<Cobol> getParagraphs() {
            return paragraphs.getElements();
        }

        public InputOutputSection withParagraphs(List<Cobol> paragraphs) {
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
            private final InputOutputSection t;

            @Nullable
            public CobolContainer<Cobol> getParagraphs() {
                return t.paragraphs;
            }

            public InputOutputSection withParagraphs(@Nullable CobolContainer<Cobol> paragraphs) {
                return t.paragraphs == paragraphs ? t : new InputOutputSection(t.padding, t.id, t.prefix, t.markers, t.words, paragraphs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Inspect implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String inspect;
        Identifier identifier;
        Cobol phrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspect(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectAllLeading implements Cobol {
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
        CobolWord word;

        @Nullable
        CobolContainer<InspectBeforeAfter> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectAllLeading(this, p);
        }

        public List<Cobol.InspectBeforeAfter> getInspections() {
            return inspections.getElements();
        }

        public InspectAllLeading withInspections(List<Cobol.InspectBeforeAfter> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectAllLeading t;

            @Nullable
            public CobolContainer<Cobol.InspectBeforeAfter> getInspections() {
                return t.inspections;
            }

            public InspectAllLeading withInspections(@Nullable CobolContainer<Cobol.InspectBeforeAfter> inspections) {
                return t.inspections == inspections ? t : new InspectAllLeading(t.padding, t.id, t.prefix, t.markers, t.word, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectAllLeadings implements Cobol {
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
        String word;

        CobolContainer<InspectAllLeading> leadings;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectAllLeadings(this, p);
        }

        public List<Cobol.InspectAllLeading> getLeadings() {
            return leadings.getElements();
        }

        public InspectAllLeadings withLeadings(List<Cobol.InspectAllLeading> leadings) {
            return getPadding().withLeadings(this.leadings.getPadding().withElements(CobolRightPadded.withElements(
                    this.leadings.getPadding().getElements(), leadings)));
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
            private final InspectAllLeadings t;

            public CobolContainer<Cobol.InspectAllLeading> getLeadings() {
                return t.leadings;
            }

            public InspectAllLeadings withLeadings(CobolContainer<Cobol.InspectAllLeading> leadings) {
                return t.leadings == leadings ? t : new InspectAllLeadings(t.padding, t.id, t.prefix, t.markers, t.word, leadings);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InspectBeforeAfter implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectBeforeAfter(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InspectBy implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String by;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectBy(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectCharacters implements Cobol {
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
        String character;

        @Nullable
        CobolContainer<InspectBeforeAfter> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectCharacters(this, p);
        }

        public List<Cobol.InspectBeforeAfter> getInspections() {
            return inspections.getElements();
        }

        public InspectCharacters withInspections(List<Cobol.InspectBeforeAfter> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectCharacters t;

            @Nullable
            public CobolContainer<Cobol.InspectBeforeAfter> getInspections() {
                return t.inspections;
            }

            public InspectCharacters withInspections(@Nullable CobolContainer<Cobol.InspectBeforeAfter> inspections) {
                return t.inspections == inspections ? t : new InspectCharacters(t.padding, t.id, t.prefix, t.markers, t.character, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectConvertingPhrase implements Cobol {
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
        String converting;

        @Getter
        @With
        Name identifier;

        @Getter
        @With
        InspectTo inspectTo;

        @Nullable
        CobolContainer<InspectBeforeAfter> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectConvertingPhrase(this, p);
        }

        public List<Cobol.InspectBeforeAfter> getInspections() {
            return inspections.getElements();
        }

        public InspectConvertingPhrase withInspections(List<Cobol.InspectBeforeAfter> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectConvertingPhrase t;

            @Nullable
            public CobolContainer<Cobol.InspectBeforeAfter> getInspections() {
                return t.inspections;
            }

            public InspectConvertingPhrase withInspections(@Nullable CobolContainer<Cobol.InspectBeforeAfter> inspections) {
                return t.inspections == inspections ? t : new InspectConvertingPhrase(t.padding, t.id, t.prefix, t.markers, t.converting, t.identifier, t.inspectTo, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectFor implements Cobol {
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

        @Getter
        @With
        String word;

        CobolContainer<Cobol> inspects;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectFor(this, p);
        }

        public List<Cobol> getInspects() {
            return inspects.getElements();
        }

        public InspectFor withInspects(List<Cobol> inspects) {
            return getPadding().withInspects(this.inspects.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspects.getPadding().getElements(), inspects)));
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
            private final InspectFor t;

            public CobolContainer<Cobol> getInspects() {
                return t.inspects;
            }

            public InspectFor withInspects(CobolContainer<Cobol> inspects) {
                return t.inspects == inspects ? t : new InspectFor(t.padding, t.id, t.prefix, t.markers, t.identifier, t.word, inspects);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectReplacingAllLeadings implements Cobol {
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
        String word;

        CobolContainer<InspectReplacingAllLeading> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectReplacingAllLeadings(this, p);
        }

        public List<Cobol.InspectReplacingAllLeading> getInspections() {
            return inspections.getElements();
        }

        public InspectReplacingAllLeadings withInspections(List<Cobol.InspectReplacingAllLeading> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectReplacingAllLeadings t;

            public CobolContainer<Cobol.InspectReplacingAllLeading> getInspections() {
                return t.inspections;
            }

            public InspectReplacingAllLeadings withInspections(CobolContainer<Cobol.InspectReplacingAllLeading> inspections) {
                return t.inspections == inspections ? t : new InspectReplacingAllLeadings(t.padding, t.id, t.prefix, t.markers, t.word, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectReplacingAllLeading implements Cobol {
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
        Name identifier;

        @Getter
        @With
        InspectBy inspectBy;

        @Nullable
        CobolContainer<InspectBeforeAfter> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectReplacingAllLeading(this, p);
        }

        public List<Cobol.InspectBeforeAfter> getInspections() {
            return inspections.getElements();
        }

        public InspectReplacingAllLeading withInspections(List<Cobol.InspectBeforeAfter> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectReplacingAllLeading t;

            @Nullable
            public CobolContainer<Cobol.InspectBeforeAfter> getInspections() {
                return t.inspections;
            }

            public InspectReplacingAllLeading withInspections(@Nullable CobolContainer<Cobol.InspectBeforeAfter> inspections) {
                return t.inspections == inspections ? t : new InspectReplacingAllLeading(t.padding, t.id, t.prefix, t.markers, t.identifier, t.inspectBy, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectReplacingCharacters implements Cobol {
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
        String word;

        @Getter
        @With
        InspectBy inspectBy;

        @Nullable
        CobolContainer<InspectBeforeAfter> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectReplacingCharacters(this, p);
        }

        public List<Cobol.InspectBeforeAfter> getInspections() {
            return inspections.getElements();
        }

        public InspectReplacingCharacters withInspections(List<Cobol.InspectBeforeAfter> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectReplacingCharacters t;

            @Nullable
            public CobolContainer<Cobol.InspectBeforeAfter> getInspections() {
                return t.inspections;
            }

            public InspectReplacingCharacters withInspections(@Nullable CobolContainer<Cobol.InspectBeforeAfter> inspections) {
                return t.inspections == inspections ? t : new InspectReplacingCharacters(t.padding, t.id, t.prefix, t.markers, t.word, t.inspectBy, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectReplacingPhrase implements Cobol {
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
        String word;

        CobolContainer<Cobol> inspections;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectReplacingPhrase(this, p);
        }

        public List<Cobol> getInspections() {
            return inspections.getElements();
        }

        public InspectReplacingPhrase withInspections(List<Cobol> inspections) {
            return getPadding().withInspections(this.inspections.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspections.getPadding().getElements(), inspections)));
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
            private final InspectReplacingPhrase t;

            public CobolContainer<Cobol> getInspections() {
                return t.inspections;
            }

            public InspectReplacingPhrase withInspections(CobolContainer<Cobol> inspections) {
                return t.inspections == inspections ? t : new InspectReplacingPhrase(t.padding, t.id, t.prefix, t.markers, t.word, inspections);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectTallyingPhrase implements Cobol {
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
        String tallying;

        CobolContainer<InspectFor> inspectFors;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectTallyingPhrase(this, p);
        }

        public List<Cobol.InspectFor> getInspectFors() {
            return inspectFors.getElements();
        }

        public InspectTallyingPhrase withInspectFors(List<Cobol.InspectFor> inspectFors) {
            return getPadding().withInspectFors(this.inspectFors.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspectFors.getPadding().getElements(), inspectFors)));
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
            private final InspectTallyingPhrase t;

            public CobolContainer<Cobol.InspectFor> getInspectFors() {
                return t.inspectFors;
            }

            public InspectTallyingPhrase withInspectFors(CobolContainer<Cobol.InspectFor> inspectFors) {
                return t.inspectFors == inspectFors ? t : new InspectTallyingPhrase(t.padding, t.id, t.prefix, t.markers, t.tallying, inspectFors);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class InspectTallyingReplacingPhrase implements Cobol {
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
        String tallying;

        CobolContainer<InspectFor> inspectFors;
        CobolContainer<InspectReplacingPhrase> replacingPhrases;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectTallyingReplacingPhrase(this, p);
        }

        public List<Cobol.InspectFor> getInspectFors() {
            return inspectFors.getElements();
        }

        public InspectTallyingReplacingPhrase withInspectFors(List<Cobol.InspectFor> inspectFors) {
            return getPadding().withInspectFors(this.inspectFors.getPadding().withElements(CobolRightPadded.withElements(
                    this.inspectFors.getPadding().getElements(), inspectFors)));
        }

        public List<Cobol.InspectReplacingPhrase> getReplacingPhrases() {
            return replacingPhrases.getElements();
        }

        public InspectTallyingReplacingPhrase withReplacingPhrases(List<Cobol.InspectReplacingPhrase> replacingPhrases) {
            return getPadding().withReplacingPhrases(this.replacingPhrases.getPadding().withElements(CobolRightPadded.withElements(
                    this.replacingPhrases.getPadding().getElements(), replacingPhrases)));
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
            private final InspectTallyingReplacingPhrase t;

            public CobolContainer<Cobol.InspectFor> getInspectFors() {
                return t.inspectFors;
            }

            public InspectTallyingReplacingPhrase withInspectFors(CobolContainer<Cobol.InspectFor> inspectFors) {
                return t.inspectFors == inspectFors ? t : new InspectTallyingReplacingPhrase(t.padding, t.id, t.prefix, t.markers, t.tallying, inspectFors, t.replacingPhrases);
            }

            public CobolContainer<Cobol.InspectReplacingPhrase> getReplacingPhrases() {
                return t.replacingPhrases;
            }

            public InspectTallyingReplacingPhrase withReplacingPhrases(CobolContainer<Cobol.InspectReplacingPhrase> replacingPhrases) {
                return t.replacingPhrases == replacingPhrases ? t : new InspectTallyingReplacingPhrase(t.padding, t.id, t.prefix, t.markers, t.tallying, t.inspectFors, replacingPhrases);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class InspectTo implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String to;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitInspectTo(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class IoControlParagraph implements Cobol {
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
        String iOControl;

        CobolLeftPadded<String> dot;

        @Getter
        @Nullable
        @With
        CobolWord fileName;

        @Getter
        @Nullable
        @With
        String fileNameDot;

        @Nullable
        CobolContainer<Cobol> clauses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitIoControlParagraph(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public IoControlParagraph withDot(String dot) {
            //noinspection ConstantConditions
            return getPadding().withDot(CobolLeftPadded.withElement(this.dot, dot));
        }

        public List<Cobol> getClauses() {
            return clauses.getElements();
        }

        public IoControlParagraph withClauses(List<Cobol> clauses) {
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
            private final IoControlParagraph t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public IoControlParagraph withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new IoControlParagraph(t.padding, t.id, t.prefix, t.markers, t.iOControl, dot, t.fileName, t.fileNameDot, t.clauses);
            }

            @Nullable
            public CobolContainer<Cobol> getClauses() {
                return t.clauses;
            }

            public IoControlParagraph withClauses(@Nullable CobolContainer<Cobol> clauses) {
                return t.clauses == clauses ? t : new IoControlParagraph(t.padding, t.id, t.prefix, t.markers, t.iOControl, t.dot, t.fileName, t.fileNameDot, clauses);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LabelRecordsClause implements Cobol {
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

        @Nullable
        CobolContainer<CobolWord> dataNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLabelRecordsClause(this, p);
        }

        public List<Cobol.CobolWord> getDataNames() {
            return dataNames.getElements();
        }

        public LabelRecordsClause withDataNames(List<Cobol.CobolWord> dataNames) {
            return getPadding().withDataNames(this.dataNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.dataNames.getPadding().getElements(), dataNames)));
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
            private final LabelRecordsClause t;

            @Nullable
            public CobolContainer<Cobol.CobolWord> getDataNames() {
                return t.dataNames;
            }

            public LabelRecordsClause withDataNames(@Nullable CobolContainer<Cobol.CobolWord> dataNames) {
                return t.dataNames == dataNames ? t : new LabelRecordsClause(t.padding, t.id, t.prefix, t.markers, t.words, dataNames);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryAttributeClauseFormat1 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryAttributeClauseFormat1(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryAttributeClauseFormat2 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String attribute;

        @Nullable
        LibraryAttributeFunction libraryAttributeFunction;

        String words;

        @Nullable
        LibraryAttributeParameter libraryAttributeParameter;

        @Nullable
        LibraryAttributeTitle libraryAttributeTitle;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryAttributeClauseFormat2(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryAttributeFunction implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryAttributeFunction(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryAttributeParameter implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryAttributeParameter(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryAttributeTitle implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryAttributeTitle(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryDescriptionEntryFormat1 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String ld;

        CobolWord libraryName;

        String export;

        @Nullable
        LibraryAttributeClauseFormat1 libraryAttributeClauseFormat1;

        @Nullable
        LibraryEntryProcedureClauseFormat1 libraryEntryProcedureClauseFormat1;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryDescriptionEntryFormat1(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LibraryDescriptionEntryFormat2 implements Cobol {
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
        String lb;

        @Getter
        @With
        CobolWord libraryName;

        @Getter
        @With
        String export;

        @Getter
        @Nullable
        @With
        LibraryIsGlobalClause libraryIsGlobalClause;

        @Getter
        @Nullable
        @With
        LibraryIsCommonClause libraryIsCommonClause;

        @Nullable
        CobolContainer<Cobol> clauseFormats;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryDescriptionEntryFormat2(this, p);
        }

        public List<Cobol> getClauseFormats() {
            return clauseFormats.getElements();
        }

        public LibraryDescriptionEntryFormat2 withClauseFormats(List<Cobol> clauseFormats) {
            return getPadding().withClauseFormats(this.clauseFormats.getPadding().withElements(CobolRightPadded.withElements(
                    this.clauseFormats.getPadding().getElements(), clauseFormats)));
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
            private final LibraryDescriptionEntryFormat2 t;

            @Nullable
            public CobolContainer<Cobol> getClauseFormats() {
                return t.clauseFormats;
            }

            public LibraryDescriptionEntryFormat2 withClauseFormats(@Nullable CobolContainer<Cobol> clauseFormats) {
                return t.clauseFormats == clauseFormats ? t : new LibraryDescriptionEntryFormat2(t.padding, t.id, t.prefix, t.markers, t.lb, t.libraryName, t.export, t.libraryIsGlobalClause, t.libraryIsCommonClause, clauseFormats);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryEntryProcedureClauseFormat1 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String entryProcedure;
        CobolWord programName;

        @Nullable
        LibraryEntryProcedureForClause libraryEntryProcedureForClause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureClauseFormat1(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryEntryProcedureClauseFormat2 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String entryProcedure;
        CobolWord programName;

        @Nullable
        LibraryEntryProcedureForClause libraryEntryProcedureForClause;

        @Nullable
        LibraryEntryProcedureWithClause libraryEntryProcedureWithClause;

        @Nullable
        LibraryEntryProcedureUsingClause libraryEntryProcedureUsingClause;

        @Nullable
        LibraryEntryProcedureGivingClause libraryEntryProcedureGivingClause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureClauseFormat2(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryEntryProcedureForClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;
        Name literal;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureForClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryEntryProcedureGivingClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String giving;
        CobolWord dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureGivingClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LibraryEntryProcedureUsingClause implements Cobol {
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
        String using;

        CobolContainer<CobolWord> names;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureUsingClause(this, p);
        }

        public List<Cobol.CobolWord> getNames() {
            return names.getElements();
        }

        public LibraryEntryProcedureUsingClause withNames(List<Cobol.CobolWord> names) {
            return getPadding().withNames(this.names.getPadding().withElements(CobolRightPadded.withElements(
                    this.names.getPadding().getElements(), names)));
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
            private final LibraryEntryProcedureUsingClause t;

            @Nullable
            public CobolContainer<Cobol.CobolWord> getNames() {
                return t.names;
            }

            public LibraryEntryProcedureUsingClause withNames(@Nullable CobolContainer<Cobol.CobolWord> names) {
                return t.names == names ? t : new LibraryEntryProcedureUsingClause(t.padding, t.id, t.prefix, t.markers, t.using, names);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LibraryEntryProcedureWithClause implements Cobol {
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
        String with;

        CobolContainer<CobolWord> names;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryEntryProcedureWithClause(this, p);
        }

        public List<Cobol.CobolWord> getNames() {
            return names.getElements();
        }

        public LibraryEntryProcedureWithClause withNames(List<Cobol.CobolWord> names) {
            return getPadding().withNames(this.names.getPadding().withElements(CobolRightPadded.withElements(
                    this.names.getPadding().getElements(), names)));
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
            private final LibraryEntryProcedureWithClause t;

            @Nullable
            public CobolContainer<Cobol.CobolWord> getNames() {
                return t.names;
            }

            public LibraryEntryProcedureWithClause withNames(@Nullable CobolContainer<Cobol.CobolWord> names) {
                return t.names == names ? t : new LibraryEntryProcedureWithClause(t.padding, t.id, t.prefix, t.markers, t.with, names);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryIsCommonClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryIsCommonClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LibraryIsGlobalClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLibraryIsGlobalClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LinageClause implements Cobol {
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

        @Getter
        @With
        Name name;

        @Getter
        @With
        String lines;

        @Nullable
        CobolContainer<Cobol> linageAt;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLinageClause(this, p);
        }

        public List<Cobol> getLinageAt() {
            return linageAt.getElements();
        }

        public LinageClause withLinageAt(List<Cobol> linageAt) {
            return getPadding().withLinageAt(this.linageAt.getPadding().withElements(CobolRightPadded.withElements(
                    this.linageAt.getPadding().getElements(), linageAt)));
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
            private final LinageClause t;

            @Nullable
            public CobolContainer<Cobol> getLinageAt() {
                return t.linageAt;
            }

            public LinageClause withLinageAt(@Nullable CobolContainer<Cobol> linageAt) {
                return t.linageAt == linageAt ? t : new LinageClause(t.padding, t.id, t.prefix, t.markers, t.words, t.name, t.lines, linageAt);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LinageFootingAt implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLinageFootingAt(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LinageLinesAtTop implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLinageLinesAtTop(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class LinageLinesAtBottom implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLinageLinesAtBottom(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LinkageSection implements Cobol {
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
            return v.visitLinkageSection(this, p);
        }

        public List<Cobol.DataDescriptionEntry> getDataDescriptions() {
            return dataDescriptions.getElements();
        }

        public LinkageSection withDataDescriptions(List<Cobol.DataDescriptionEntry> dataDescriptions) {
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
            private final LinkageSection t;

            public CobolContainer<Cobol.DataDescriptionEntry> getDataDescriptions() {
                return t.dataDescriptions;
            }

            public LinkageSection withDataDescriptions(CobolContainer<Cobol.DataDescriptionEntry> dataDescriptions) {
                return t.dataDescriptions == dataDescriptions ? t : new LinkageSection(t.padding, t.id, t.prefix, t.markers, t.words, dataDescriptions);
            }
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class LocalStorageSection implements Cobol {
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

        @Getter
        @With
        String localData;

        @Getter
        @With
        Name localName;

        CobolContainer<DataDescriptionEntry> dataDescriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitLocalStorageSection(this, p);
        }

        public List<Cobol.DataDescriptionEntry> getDataDescriptions() {
            return dataDescriptions.getElements();
        }

        public LocalStorageSection withDataDescriptions(List<Cobol.DataDescriptionEntry> dataDescriptions) {
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
            private final LocalStorageSection t;

            public CobolContainer<Cobol.DataDescriptionEntry> getDataDescriptions() {
                return t.dataDescriptions;
            }

            public LocalStorageSection withDataDescriptions(CobolContainer<Cobol.DataDescriptionEntry> dataDescriptions) {
                return t.dataDescriptions == dataDescriptions ? t : new LocalStorageSection(t.padding, t.id, t.prefix, t.markers, t.words, t.localData, t.localName, dataDescriptions);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MultDivs implements Cobol {
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
        Powers powers;

        @Nullable
        CobolContainer<MultDiv> multDivs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultDivs(this, p);
        }

        public List<Cobol.MultDiv> getMultDivs() {
            return multDivs.getElements();
        }

        public MultDivs withMultDivs(List<Cobol.MultDiv> multDivs) {
            return getPadding().withMultDivs(this.multDivs.getPadding().withElements(CobolRightPadded.withElements(
                    this.multDivs.getPadding().getElements(), multDivs)));
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
            private final MultDivs t;

            public CobolContainer<Cobol.MultDiv> getMultDivs() {
                return t.multDivs;
            }

            public MultDivs withMultDivs(CobolContainer<Cobol.MultDiv> multDivs) {
                return t.multDivs == multDivs ? t : new MultDivs(t.padding, t.id, t.prefix, t.markers, t.powers, multDivs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MultDiv implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Powers powers;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultDiv(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Merge implements Statement {
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

        @Getter
        @With
        Name fileName;

        CobolContainer<MergeOnKeyClause> mergeOnKeyClause;

        @Getter
        @Nullable
        @With
        MergeCollatingSequencePhrase mergeCollatingSequencePhrase;

        CobolContainer<Name> mergeUsing;

        @Getter
        @Nullable
        @With
        MergeOutputProcedurePhrase mergeOutputProcedurePhrase;

        CobolContainer<MergeGivingPhrase> mergeGivingPhrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMerge(this, p);
        }

        public List<Cobol.MergeOnKeyClause> getMergeOnKeyClause() {
            return mergeOnKeyClause.getElements();
        }

        public Merge withMergeOnKeyClause(List<Cobol.MergeOnKeyClause> mergeOnKeyClause) {
            return getPadding().withMergeOnKeyClause(this.mergeOnKeyClause.getPadding().withElements(CobolRightPadded.withElements(
                    this.mergeOnKeyClause.getPadding().getElements(), mergeOnKeyClause)));
        }

        public List<Name> getMergeUsing() {
            return mergeUsing.getElements();
        }

        public Merge withMergeUsing(List<Name> mergeUsing) {
            return getPadding().withMergeUsing(this.mergeUsing.getPadding().withElements(CobolRightPadded.withElements(
                    this.mergeUsing.getPadding().getElements(), mergeUsing)));
        }

        public List<Cobol.MergeGivingPhrase> getMergeGivingPhrase() {
            return mergeGivingPhrase.getElements();
        }

        public Merge withMergeGivingPhrase(List<Cobol.MergeGivingPhrase> mergeGivingPhrase) {
            return getPadding().withMergeGivingPhrase(this.mergeGivingPhrase.getPadding().withElements(CobolRightPadded.withElements(
                    this.mergeGivingPhrase.getPadding().getElements(), mergeGivingPhrase)));
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
            private final Merge t;

            public CobolContainer<Cobol.MergeOnKeyClause> getMergeOnKeyClause() {
                return t.mergeOnKeyClause;
            }

            public Merge withMergeOnKeyClause(CobolContainer<Cobol.MergeOnKeyClause> mergeOnKeyClause) {
                return t.mergeOnKeyClause == mergeOnKeyClause ? t : new Merge(t.padding, t.id, t.prefix, t.markers, t.words, t.fileName, mergeOnKeyClause, t.mergeCollatingSequencePhrase, t.mergeUsing, t.mergeOutputProcedurePhrase, t.mergeGivingPhrase);
            }

            public CobolContainer<Name> getMergeUsing() {
                return t.mergeUsing;
            }

            public Merge withMergeUsing(CobolContainer<Name> mergeUsing) {
                return t.mergeUsing == mergeUsing ? t : new Merge(t.padding, t.id, t.prefix, t.markers, t.words, t.fileName, t.mergeOnKeyClause, t.mergeCollatingSequencePhrase, mergeUsing, t.mergeOutputProcedurePhrase, t.mergeGivingPhrase);
            }

            public CobolContainer<Cobol.MergeGivingPhrase> getMergeGivingPhrase() {
                return t.mergeGivingPhrase;
            }

            public Merge withMergeGivingPhrase(CobolContainer<Cobol.MergeGivingPhrase> mergeGivingPhrase) {
                return t.mergeGivingPhrase == mergeGivingPhrase ? t : new Merge(t.padding, t.id, t.prefix, t.markers, t.words, t.fileName, t.mergeOnKeyClause, t.mergeCollatingSequencePhrase, t.mergeUsing, t.mergeOutputProcedurePhrase, mergeGivingPhrase);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MergeOnKeyClause implements Cobol {
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

        CobolContainer<QualifiedDataName> qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeOnKeyClause(this, p);
        }

        public List<Cobol.QualifiedDataName> getQualifiedDataName() {
            return qualifiedDataName.getElements();
        }

        public MergeOnKeyClause withQualifiedDataName(List<Cobol.QualifiedDataName> qualifiedDataName) {
            return getPadding().withQualifiedDataName(this.qualifiedDataName.getPadding().withElements(CobolRightPadded.withElements(
                    this.qualifiedDataName.getPadding().getElements(), qualifiedDataName)));
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
            private final MergeOnKeyClause t;

            public CobolContainer<Cobol.QualifiedDataName> getQualifiedDataName() {
                return t.qualifiedDataName;
            }

            public MergeOnKeyClause withQualifiedDataName(CobolContainer<Cobol.QualifiedDataName> qualifiedDataName) {
                return t.qualifiedDataName == qualifiedDataName ? t : new MergeOnKeyClause(t.padding, t.id, t.prefix, t.markers, t.words, qualifiedDataName);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MergeCollatingSequencePhrase implements Cobol {
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

        CobolContainer<Name> name;

        @Getter
        @Nullable
        @With
        Mergeable mergeCollatingAlphanumeric;

        @Getter
        @Nullable
        @With
        Mergeable mergeCollatingNational;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeCollatingSequencePhrase(this, p);
        }

        public List<Name> getName() {
            return name.getElements();
        }

        public MergeCollatingSequencePhrase withName(List<Name> name) {
            return getPadding().withName(this.name.getPadding().withElements(CobolRightPadded.withElements(
                    this.name.getPadding().getElements(), name)));
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
            private final MergeCollatingSequencePhrase t;

            public CobolContainer<Name> getName() {
                return t.name;
            }

            public MergeCollatingSequencePhrase withName(CobolContainer<Name> name) {
                return t.name == name ? t : new MergeCollatingSequencePhrase(t.padding, t.id, t.prefix, t.markers, t.words, name, t.mergeCollatingAlphanumeric, t.mergeCollatingNational);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Mergeable implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeable(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MergeUsing implements Cobol {
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

        CobolContainer<Name> fileNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeUsing(this, p);
        }

        public List<Name> getFileNames() {
            return fileNames.getElements();
        }

        public MergeUsing withFileNames(List<Name> fileNames) {
            return getPadding().withFileNames(this.fileNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.fileNames.getPadding().getElements(), fileNames)));
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
            private final MergeUsing t;

            public CobolContainer<Name> getFileNames() {
                return t.fileNames;
            }

            public MergeUsing withFileNames(CobolContainer<Name> fileNames) {
                return t.fileNames == fileNames ? t : new MergeUsing(t.padding, t.id, t.prefix, t.markers, t.words, fileNames);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MergeOutputProcedurePhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        ProcedureName procedureName;

        @Nullable
        MergeOutputThrough mergeOutputThrough;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeOutputProcedurePhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MergeOutputThrough implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        ProcedureName procedureName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeOutputThrough(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MergeGivingPhrase implements Cobol {
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

        CobolContainer<MergeGiving> mergeGiving;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeGivingPhrase(this, p);
        }

        public List<Cobol.MergeGiving> getMergeGiving() {
            return mergeGiving.getElements();
        }

        public MergeGivingPhrase withMergeGiving(List<Cobol.MergeGiving> mergeGiving) {
            return getPadding().withMergeGiving(this.mergeGiving.getPadding().withElements(CobolRightPadded.withElements(
                    this.mergeGiving.getPadding().getElements(), mergeGiving)));
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
            private final MergeGivingPhrase t;

            public CobolContainer<Cobol.MergeGiving> getMergeGiving() {
                return t.mergeGiving;
            }

            public MergeGivingPhrase withMergeGiving(CobolContainer<Cobol.MergeGiving> mergeGiving) {
                return t.mergeGiving == mergeGiving ? t : new MergeGivingPhrase(t.padding, t.id, t.prefix, t.markers, t.words, mergeGiving);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MergeGiving implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;

        @Nullable
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMergeGiving(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MoveStatement implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol moveToStatement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMoveStatement(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MoveToStatement implements Cobol {
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
        Name from;

        CobolContainer<Identifier> to;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMoveToStatement(this, p);
        }

        public List<Identifier> getTo() {
            return to.getElements();
        }

        public MoveToStatement withTo(List<Identifier> to) {
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
            private final MoveToStatement t;

            public CobolContainer<Identifier> getTo() {
                return t.to;
            }

            public MoveToStatement withTo(CobolContainer<Identifier> to) {
                return t.to == to ? t : new MoveToStatement(t.padding, t.id, t.prefix, t.markers, t.from, to);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MoveCorrespondingToStatement implements Cobol {
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

        @Getter
        @With
        Identifier moveCorrespondingToSendingArea;

        CobolContainer<Identifier> to;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMoveCorrespondingToStatement(this, p);
        }

        public List<Identifier> getTo() {
            return to.getElements();
        }

        public MoveCorrespondingToStatement withTo(List<Identifier> to) {
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
            private final MoveCorrespondingToStatement t;

            public CobolContainer<Identifier> getTo() {
                return t.to;
            }

            public MoveCorrespondingToStatement withTo(CobolContainer<Identifier> to) {
                return t.to == to ? t : new MoveCorrespondingToStatement(t.padding, t.id, t.prefix, t.markers, t.words, t.moveCorrespondingToSendingArea, to);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MultipleFileClause implements Cobol {
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

        CobolContainer<Cobol> filePositions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultipleFileClause(this, p);
        }

        public List<Cobol> getFilePositions() {
            return filePositions.getElements();
        }

        public MultipleFileClause withFilePositions(List<Cobol> filePositions) {
            return getPadding().withFilePositions(this.filePositions.getPadding().withElements(CobolRightPadded.withElements(
                    this.filePositions.getPadding().getElements(), filePositions)));
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
            private final MultipleFileClause t;

            public CobolContainer<Cobol> getFilePositions() {
                return t.filePositions;
            }

            public MultipleFileClause withFilePositions(CobolContainer<Cobol> filePositions) {
                return t.filePositions == filePositions ? t : new MultipleFileClause(t.padding, t.id, t.prefix, t.markers, t.words, filePositions);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MultipleFilePosition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord fileName;

        @Nullable
        String position;

        @Nullable
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultipleFilePosition(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Multiply implements Statement {
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

        @Getter
        @With
        Name multiplicand;

        @Getter
        @With
        String by;

        @Getter
        @With
        Cobol multiply;

        @Getter
        @Nullable
        @With
        StatementPhrase onSizeErrorPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnSizeErrorPhrase;

        @Nullable
        CobolLeftPadded<String> endMultiply;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultiply(this, p);
        }

        @Nullable
        public String getEndMultiply() {
            return endMultiply == null ? null : endMultiply.getElement();
        }

        public Multiply withEndMultiply(@Nullable String endMultiply) {
            if (endMultiply == null) {
                return this.endMultiply == null ? this : new Multiply(id, prefix, markers, words, multiplicand, by, multiply, onSizeErrorPhrase, notOnSizeErrorPhrase, null);
            }
            return getPadding().withEndMultiply(CobolLeftPadded.withElement(this.endMultiply, endMultiply));
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
            private final Multiply t;

            @Nullable
            public CobolLeftPadded<String> getEndMultiply() {
                return t.endMultiply;
            }

            public Multiply withEndMultiply(@Nullable CobolLeftPadded<String> endMultiply) {
                return t.endMultiply == endMultiply ? t : new Multiply(t.padding, t.id, t.prefix, t.markers, t.words, t.multiplicand, t.by, t.multiply, t.onSizeErrorPhrase, t.notOnSizeErrorPhrase, endMultiply);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MultiplyRegular implements Cobol {
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

        CobolContainer<Roundable> operand;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultiplyRegular(this, p);
        }

        public List<Cobol.Roundable> getOperand() {
            return operand.getElements();
        }

        public MultiplyRegular withOperand(List<Cobol.Roundable> operand) {
            return getPadding().withOperand(this.operand.getPadding().withElements(CobolRightPadded.withElements(
                    this.operand.getPadding().getElements(), operand)));
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
            private final MultiplyRegular t;

            public CobolContainer<Cobol.Roundable> getOperand() {
                return t.operand;
            }

            public MultiplyRegular withOperand(CobolContainer<Cobol.Roundable> operand) {
                return t.operand == operand ? t : new MultiplyRegular(t.padding, t.id, t.prefix, t.markers, operand);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MultiplyGiving implements Cobol {
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
        Name operand;

        CobolContainer<Roundable> result;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitMultiplyGiving(this, p);
        }

        public List<Cobol.Roundable> getResult() {
            return result.getElements();
        }

        public MultiplyGiving withResult(List<Cobol.Roundable> result) {
            return getPadding().withResult(this.result.getPadding().withElements(CobolRightPadded.withElements(
                    this.result.getPadding().getElements(), result)));
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
            private final MultiplyGiving t;

            public CobolContainer<Cobol.Roundable> getResult() {
                return t.result;
            }

            public MultiplyGiving withResult(CobolContainer<Cobol.Roundable> result) {
                return t.result == result ? t : new MultiplyGiving(t.padding, t.id, t.prefix, t.markers, t.operand, result);
            }
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class NextSentence implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitNextSentence(this, p);
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OdtClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Identifier mnemonicName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOdtClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Open implements Statement {
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

        CobolContainer<Cobol> open;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOpen(this, p);
        }

        public List<Cobol> getOpen() {
            return open.getElements();
        }

        public Open withOpen(List<Cobol> open) {
            return getPadding().withOpen(this.open.getPadding().withElements(CobolRightPadded.withElements(
                    this.open.getPadding().getElements(), open)));
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
            private final Open t;

            public CobolContainer<Cobol> getOpen() {
                return t.open;
            }

            public Open withOpen(CobolContainer<Cobol> open) {
                return t.open == open ? t : new Open(t.padding, t.id, t.prefix, t.markers, t.words, open);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class OpenInputOutputStatement implements Cobol {
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

        CobolContainer<Openable> openInput;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOpenInputOutputStatement(this, p);
        }

        public List<Cobol.Openable> getOpenInput() {
            return openInput.getElements();
        }

        public OpenInputOutputStatement withOpenInput(List<Cobol.Openable> openInput) {
            return getPadding().withOpenInput(this.openInput.getPadding().withElements(CobolRightPadded.withElements(
                    this.openInput.getPadding().getElements(), openInput)));
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
            private final OpenInputOutputStatement t;

            public CobolContainer<Cobol.Openable> getOpenInput() {
                return t.openInput;
            }

            public OpenInputOutputStatement withOpenInput(CobolContainer<Cobol.Openable> openInput) {
                return t.openInput == openInput ? t : new OpenInputOutputStatement(t.padding, t.id, t.prefix, t.markers, t.words, openInput);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Openable implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name fileName;

        @Nullable
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOpenable(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class OpenIOExtendStatement implements Cobol {
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

        CobolContainer<Name> fileNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOpenIOExtendStatement(this, p);
        }

        public List<Name> getFileNames() {
            return fileNames.getElements();
        }

        public OpenIOExtendStatement withFileNames(List<Name> fileNames) {
            return getPadding().withFileNames(this.fileNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.fileNames.getPadding().getElements(), fileNames)));
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
            private final OpenIOExtendStatement t;

            public CobolContainer<Name> getFileNames() {
                return t.fileNames;
            }

            public OpenIOExtendStatement withFileNames(CobolContainer<Name> fileNames) {
                return t.fileNames == fileNames ? t : new OpenIOExtendStatement(t.padding, t.id, t.prefix, t.markers, t.words, fileNames);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OrganizationClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitOrganizationClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PaddingCharacterClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPaddingCharacterClause(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PasswordClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPasswordClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Perform implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol statement;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerform(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class PerformInlineStatement implements Cobol {

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
        @Nullable
        @With
        Cobol performType;

        CobolContainer<Statement> statements;

        @Getter
        @With
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformInlineStatement(this, p);
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public PerformInlineStatement withStatements(List<Statement> statements) {
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
            private final PerformInlineStatement t;

            public CobolContainer<Statement> getStatements() {
                return t.statements;
            }

            public PerformInlineStatement withStatements(CobolContainer<Statement> statements) {
                return t.statements == statements ? t : new PerformInlineStatement(t.padding, t.id, t.prefix, t.markers, t.performType, statements, t.words);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformProcedureStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        ProcedureName procedureName;

        @Nullable
        String words;

        @Nullable
        ProcedureName throughProcedure;

        @Nullable
        Cobol performType;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformProcedureStatement(this, p);
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformTimes implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name value;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformTimes(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformUntil implements Cobol {

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        @Nullable
        PerformTestClause performTestClause;

        String words;
        Condition condition;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformUntil(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformVarying implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Cobol first;

        @Nullable
        Cobol second;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformVarying(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class PerformVaryingClause implements Cobol {
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

        @Getter
        @With
        PerformVaryingPhrase performVaryingPhrase;

        CobolContainer<Performable> performAfter;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformVaryingClause(this, p);
        }

        public List<Cobol.Performable> getPerformAfter() {
            return performAfter.getElements();
        }

        public PerformVaryingClause withPerformAfter(List<Cobol.Performable> performAfter) {
            return getPadding().withPerformAfter(this.performAfter.getPadding().withElements(CobolRightPadded.withElements(
                    this.performAfter.getPadding().getElements(), performAfter)));
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
            private final PerformVaryingClause t;

            public CobolContainer<Cobol.Performable> getPerformAfter() {
                return t.performAfter;
            }

            public PerformVaryingClause withPerformAfter(CobolContainer<Cobol.Performable> performAfter) {
                return t.performAfter == performAfter ? t : new PerformVaryingClause(t.padding, t.id, t.prefix, t.markers, t.words, t.performVaryingPhrase, performAfter);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformVaryingPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;
        PerformFrom from;
        Performable by;
        PerformUntil until;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformVaryingPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Performable implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol expression;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformable(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformFrom implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol from;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformFrom(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PerformTestClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPerformTestClause(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureName implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name paragraphName;

        @Nullable
        InSection inSection;

        @Nullable
        Name sectionName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureName(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Purge implements Statement {
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

        CobolContainer<Name> names;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPurge(this, p);
        }

        public List<Name> getNames() {
            return names.getElements();
        }

        public Purge withNames(List<Name> names) {
            return getPadding().withNames(this.names.getPadding().withElements(CobolRightPadded.withElements(
                    this.names.getPadding().getElements(), names)));
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
            private final Purge t;

            public CobolContainer<Name> getNames() {
                return t.names;
            }

            public Purge withNames(CobolContainer<Name> names) {
                return t.names == names ? t : new Purge(t.padding, t.id, t.prefix, t.markers, names);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Powers implements Cobol {
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
        String plusMinusChar;

        @Getter
        @With
        Cobol expression;

        @Nullable
        CobolContainer<Power> powers;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPowers(this, p);
        }

        public List<Cobol.Power> getPowers() {
            return powers.getElements();
        }

        public Powers withPowers(List<Cobol.Power> powers) {
            return getPadding().withPowers(this.powers.getPadding().withElements(CobolRightPadded.withElements(
                    this.powers.getPadding().getElements(), powers)));
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
            private final Powers t;

            public CobolContainer<Cobol.Power> getPowers() {
                return t.powers;
            }

            public Powers withPowers(CobolContainer<Cobol.Power> powers) {
                return t.powers == powers ? t : new Powers(t.padding, t.id, t.prefix, t.markers, t.plusMinusChar, t.expression, powers);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Power implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String power;

        Cobol expression;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPower(this, p);
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

        @Getter
        @Nullable
        @With
        ProcedureDivisionUsingClause procedureDivisionUsingClause;

        @Getter
        @Nullable
        @With
        ProcedureDivisionGivingClause procedureDivisionGivingClause;

        CobolLeftPadded<String> dot;

        @Getter
        @Nullable
        @With
        ProcedureDeclaratives procedureDeclaratives;

        CobolLeftPadded<ProcedureDivisionBody> body;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivision(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public ProcedureDivision withDot(String dot) {
            //noinspection ConstantConditions
            return getPadding().withDot(CobolLeftPadded.withElement(this.dot, dot));
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

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ProcedureDivision withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ProcedureDivision(t.padding, t.id, t.prefix, t.markers, t.words, t.procedureDivisionUsingClause, t.procedureDivisionGivingClause, dot, t.procedureDeclaratives, t.body);
            }

            public CobolLeftPadded<Cobol.ProcedureDivisionBody> getBody() {
                return t.body;
            }

            public ProcedureDivision withBody(CobolLeftPadded<Cobol.ProcedureDivisionBody> body) {
                return t.body == body ? t : new ProcedureDivision(t.padding, t.id, t.prefix, t.markers, t.words, t.procedureDivisionUsingClause, t.procedureDivisionGivingClause, t.dot, t.procedureDeclaratives, body);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureDeclaratives implements Cobol {
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
        String declaratives;

        CobolContainer<ProcedureDeclarative> procedureDeclarative;

        @Getter
        @With
        String endDeclaratives;

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDeclaratives(this, p);
        }

        public List<Cobol.ProcedureDeclarative> getProcedureDeclarative() {
            return procedureDeclarative.getElements();
        }

        public ProcedureDeclaratives withProcedureDeclarative(List<Cobol.ProcedureDeclarative> procedureDeclarative) {
            return getPadding().withProcedureDeclarative(this.procedureDeclarative.getPadding().withElements(CobolRightPadded.withElements(
                    this.procedureDeclarative.getPadding().getElements(), procedureDeclarative)));
        }

        public String getDot() {
            return dot.getElement();
        }

        public ProcedureDeclaratives withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ProcedureDeclaratives t;

            public CobolContainer<Cobol.ProcedureDeclarative> getProcedureDeclarative() {
                return t.procedureDeclarative;
            }

            public ProcedureDeclaratives withProcedureDeclarative(CobolContainer<Cobol.ProcedureDeclarative> procedureDeclarative) {
                return t.procedureDeclarative == procedureDeclarative ? t : new ProcedureDeclaratives(t.padding, t.id, t.prefix, t.markers, t.declaratives, procedureDeclarative, t.endDeclaratives, t.dot);
            }

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ProcedureDeclaratives withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ProcedureDeclaratives(t.padding, t.id, t.prefix, t.markers, t.declaratives, t.procedureDeclarative, t.endDeclaratives, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureDeclarative implements Cobol {
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
        ProcedureSectionHeader procedureSectionHeader;

        CobolLeftPadded<UseStatement> useStatement;
        CobolLeftPadded<Paragraphs> paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDeclarative(this, p);
        }

        public Cobol.UseStatement getUseStatement() {
            return useStatement.getElement();
        }

        public ProcedureDeclarative withUseStatement(Cobol.UseStatement useStatement) {
            //noinspection ConstantConditions
            return getPadding().withUseStatement(CobolLeftPadded.withElement(this.useStatement, useStatement));
        }

        public Cobol.Paragraphs getParagraphs() {
            return paragraphs.getElement();
        }

        public ProcedureDeclarative withParagraphs(Cobol.Paragraphs paragraphs) {
            //noinspection ConstantConditions
            return getPadding().withParagraphs(CobolLeftPadded.withElement(this.paragraphs, paragraphs));
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
            private final ProcedureDeclarative t;

            public CobolLeftPadded<Cobol.UseStatement> getUseStatement() {
                return t.useStatement;
            }

            public ProcedureDeclarative withUseStatement(CobolLeftPadded<Cobol.UseStatement> useStatement) {
                return t.useStatement == useStatement ? t : new ProcedureDeclarative(t.padding, t.id, t.prefix, t.markers, t.procedureSectionHeader, useStatement, t.paragraphs);
            }

            public CobolLeftPadded<Cobol.Paragraphs> getParagraphs() {
                return t.paragraphs;
            }

            public ProcedureDeclarative withParagraphs(CobolLeftPadded<Cobol.Paragraphs> paragraphs) {
                return t.paragraphs == paragraphs ? t : new ProcedureDeclarative(t.padding, t.id, t.prefix, t.markers, t.procedureSectionHeader, t.useStatement, paragraphs);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureSection implements Cobol {
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
        ProcedureSectionHeader procedureSectionHeader;

        CobolLeftPadded<String> dot;

        @Getter
        @With
        Paragraphs paragraphs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureSection(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public ProcedureSection withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ProcedureSection t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ProcedureSection withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ProcedureSection(t.padding, t.id, t.prefix, t.markers, t.procedureSectionHeader, dot, t.paragraphs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureSectionHeader implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name sectionName;
        String section;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureSectionHeader(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionGivingClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionGivingClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionUsingClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        List<Cobol> procedureDivisionUsingParameter;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionUsingClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionByReferencePhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String words;

        List<ProcedureDivisionByReference> procedureDivisionByReference;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionByReferencePhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionByReference implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String words;

        Name reference;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionByReference(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcedureDivisionByValuePhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        List<Name> phrases;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionByValuePhrase(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProcedureDivisionBody implements Cobol {
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
        Paragraphs paragraphs;

        @Nullable
        CobolContainer<ProcedureSection> procedureSection;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProcedureDivisionBody(this, p);
        }

        public List<Cobol.ProcedureSection> getProcedureSection() {
            return procedureSection.getElements();
        }

        public ProcedureDivisionBody withProcedureSection(List<Cobol.ProcedureSection> procedureSection) {
            return getPadding().withProcedureSection(this.procedureSection.getPadding().withElements(CobolRightPadded.withElements(
                    this.procedureSection.getPadding().getElements(), procedureSection)));
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
            private final ProcedureDivisionBody t;

            @Nullable
            public CobolContainer<Cobol.ProcedureSection> getProcedureSection() {
                return t.procedureSection;
            }

            public ProcedureDivisionBody withProcedureSection(@Nullable CobolContainer<Cobol.ProcedureSection> procedureSection) {
                return t.procedureSection == procedureSection ? t : new ProcedureDivisionBody(t.padding, t.id, t.prefix, t.markers, t.paragraphs, procedureSection);
            }
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ProgramLibrarySection implements Cobol {
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

        @Nullable
        CobolContainer<Cobol> libraryDescriptionEntries;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitProgramLibrarySection(this, p);
        }

        public List<Cobol> getLibraryDescriptionEntries() {
            return libraryDescriptionEntries.getElements();
        }

        public ProgramLibrarySection withLibraryDescriptionEntries(List<Cobol> libraryDescriptionEntries) {
            return getPadding().withLibraryDescriptionEntries(this.libraryDescriptionEntries.getPadding().withElements(CobolRightPadded.withElements(
                    this.libraryDescriptionEntries.getPadding().getElements(), libraryDescriptionEntries)));
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
            private final ProgramLibrarySection t;

            @Nullable
            public CobolContainer<Cobol> getLibraryDescriptionEntries() {
                return t.libraryDescriptionEntries;
            }

            public ProgramLibrarySection withLibraryDescriptionEntries(@Nullable CobolContainer<Cobol> libraryDescriptionEntries) {
                return t.libraryDescriptionEntries == libraryDescriptionEntries ? t : new ProgramLibrarySection(t.padding, t.id, t.prefix, t.markers, t.words, libraryDescriptionEntries);
            }
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

        CobolContainer<Paragraph> paragraphs;

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

        public List<Cobol.Paragraph> getParagraphs() {
            return paragraphs.getElements();
        }

        public Paragraphs withParagraphs(List<Cobol.Paragraph> paragraphs) {
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
            private final Paragraphs t;

            public CobolContainer<Cobol.Sentence> getSentences() {
                return t.sentences;
            }

            public Paragraphs withSentences(CobolContainer<Cobol.Sentence> sentences) {
                return t.sentences == sentences ? t : new Paragraphs(t.padding, t.id, t.prefix, t.markers, sentences, t.paragraphs);
            }

            public CobolContainer<Cobol.Paragraph> getParagraphs() {
                return t.paragraphs;
            }

            public Paragraphs withParagraphs(CobolContainer<Cobol.Paragraph> paragraphs) {
                return t.paragraphs == paragraphs ? t : new Paragraphs(t.padding, t.id, t.prefix, t.markers, t.sentences, paragraphs);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Paragraph implements Cobol {
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
        Name paragraphName;

        @Nullable
        CobolLeftPadded<String> dot;

        @Getter
        @Nullable
        @With
        AlteredGoTo alteredGoTo;

        @Nullable
        CobolContainer<Sentence> sentences;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitParagraph(this, p);
        }

        @Nullable
        public String getDot() {
            return dot == null ? null : dot.getElement();
        }

        public Paragraph withDot(@Nullable String dot) {
            if (dot == null) {
                return this.dot == null ? this : new Paragraph(id, prefix, markers, paragraphName, null, alteredGoTo, sentences);
            }
            return getPadding().withDot(CobolLeftPadded.withElement(this.dot, dot));
        }

        public List<Cobol.Sentence> getSentences() {
            return sentences.getElements();
        }

        public Paragraph withSentences(List<Cobol.Sentence> sentences) {
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
            private final Paragraph t;

            @Nullable
            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public Paragraph withDot(@Nullable CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new Paragraph(t.padding, t.id, t.prefix, t.markers, t.paragraphName, dot, t.alteredGoTo, t.sentences);
            }

            @Nullable
            public CobolContainer<Cobol.Sentence> getSentences() {
                return t.sentences;
            }

            public Paragraph withSentences(@Nullable CobolContainer<Cobol.Sentence> sentences) {
                return t.sentences == sentences ? t : new Paragraph(t.padding, t.id, t.prefix, t.markers, t.paragraphName, t.dot, t.alteredGoTo, sentences);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Parenthesized implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        String leftParen;
        List<Cobol> contents;
        String rightParen;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitParenthesized(this, p);
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

        CobolContainer<CobolWord> words;

        @Getter
        @Nullable
        @With
        Parenthesized Parenthesized;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPicture(this, p);
        }

        public List<Cobol.CobolWord> getWords() {
            return words.getElements();
        }

        public Picture withWords(List<Cobol.CobolWord> words) {
            return getPadding().withWords(this.words.getPadding().withElements(CobolRightPadded.withElements(
                    this.words.getPadding().getElements(), words)));
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

            public CobolContainer<Cobol.CobolWord> getWords() {
                return t.words;
            }

            public Picture withWords(CobolContainer<Cobol.CobolWord> words) {
                return t.words == words ? t : new Picture(t.padding, t.id, t.prefix, t.markers, words, t.Parenthesized);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class PictureString implements Cobol {
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

        CobolContainer<Picture> pictures;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPictureString(this, p);
        }

        public List<Cobol.Picture> getPictures() {
            return pictures.getElements();
        }

        public PictureString withPictures(List<Cobol.Picture> pictures) {
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
            private final PictureString t;

            public CobolContainer<Cobol.Picture> getPictures() {
                return t.pictures;
            }

            public PictureString withPictures(CobolContainer<Cobol.Picture> pictures) {
                return t.pictures == pictures ? t : new PictureString(t.padding, t.id, t.prefix, t.markers, pictures);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PlusMinus implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        MultDivs multDivs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitPlusMinus(this, p);
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QualifiedDataName implements Cobol, Identifier {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Cobol dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedDataName(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class QualifiedDataNameFormat1 implements Cobol {
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
        Name name;

        @Nullable
        CobolContainer<Cobol> qualifiedInData;

        @Getter
        @Nullable
        @With
        InFile inFile;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedDataNameFormat1(this, p);
        }

        public List<Cobol> getQualifiedInData() {
            return qualifiedInData.getElements();
        }

        public QualifiedDataNameFormat1 withQualifiedInData(List<Cobol> qualifiedInData) {
            return getPadding().withQualifiedInData(this.qualifiedInData.getPadding().withElements(CobolRightPadded.withElements(
                    this.qualifiedInData.getPadding().getElements(), qualifiedInData)));
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
            private final QualifiedDataNameFormat1 t;

            @Nullable
            public CobolContainer<Cobol> getQualifiedInData() {
                return t.qualifiedInData;
            }

            public QualifiedDataNameFormat1 withQualifiedInData(@Nullable CobolContainer<Cobol> qualifiedInData) {
                return t.qualifiedInData == qualifiedInData ? t : new QualifiedDataNameFormat1(t.padding, t.id, t.prefix, t.markers, t.name, qualifiedInData, t.inFile);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QualifiedDataNameFormat2 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name paragraphName;
        InSection inSection;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedDataNameFormat2(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QualifiedDataNameFormat3 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name textName;
        InLibrary inLibrary;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedDataNameFormat3(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QualifiedDataNameFormat4 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String linageCounter;
        InFile inFile;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedDataNameFormat4(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QualifiedInData implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Cobol in;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitQualifiedInData(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Read implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name fileName;
        String nextRecord;

        @Nullable
        ReadInto readInto;

        @Nullable
        ReadWith readWith;

        @Nullable
        ReadKey readKey;

        @Nullable
        StatementPhrase invalidKeyPhrase;

        @Nullable
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        StatementPhrase atEndPhrase;

        @Nullable
        StatementPhrase notAtEndPhrase;

        String endRead;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRead(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReadInto implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReadInto(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReadWith implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReadWith(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReadKey implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReadKey(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Receive implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String receive;
        Cobol fromOrInto;

        @Nullable
        StatementPhrase onExceptionClause;

        @Nullable
        StatementPhrase notOnExceptionClause;

        String endReceive;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReceive(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReceiveFromStatement implements Cobol {
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
        CobolWord dataName;

        @Getter
        @With
        String from;

        @Getter
        @With
        ReceiveFrom receiveFrom;

        CobolContainer<Cobol> beforeWithThreadSizeStatus;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReceiveFromStatement(this, p);
        }

        public List<Cobol> getBeforeWithThreadSizeStatus() {
            return beforeWithThreadSizeStatus.getElements();
        }

        public ReceiveFromStatement withBeforeWithThreadSizeStatus(List<Cobol> beforeWithThreadSizeStatus) {
            return getPadding().withBeforeWithThreadSizeStatus(this.beforeWithThreadSizeStatus.getPadding().withElements(CobolRightPadded.withElements(
                    this.beforeWithThreadSizeStatus.getPadding().getElements(), beforeWithThreadSizeStatus)));
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
            private final ReceiveFromStatement t;

            public CobolContainer<Cobol> getBeforeWithThreadSizeStatus() {
                return t.beforeWithThreadSizeStatus;
            }

            public ReceiveFromStatement withBeforeWithThreadSizeStatus(CobolContainer<Cobol> beforeWithThreadSizeStatus) {
                return t.beforeWithThreadSizeStatus == beforeWithThreadSizeStatus ? t : new ReceiveFromStatement(t.padding, t.id, t.prefix, t.markers, t.dataName, t.from, t.receiveFrom, beforeWithThreadSizeStatus);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReceiveFrom implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Nullable
        CobolWord dataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReceiveFrom(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReceiveIntoStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord cdName;
        String words;
        Identifier identifier;

        @Nullable
        StatementPhrase receiveNoData;

        @Nullable
        StatementPhrase receiveWithData;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReceiveIntoStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Receivable implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReceivable(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordContainsClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String record;
        Cobol clause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordContainsClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordContainsClauseFormat1 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String contains;
        CobolWord integerLiteral;
        String characters;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordContainsClauseFormat1(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordContainsClauseFormat2 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        List<Cobol> fromClause;
        List<Cobol> qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordContainsClauseFormat2(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordContainsClauseFormat3 implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String contains;
        CobolWord integerLiteral;
        RecordContainsTo recordContainsTo;
        String characters;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordContainsClauseFormat3(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordContainsTo implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String to;
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordContainsTo(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordDelimiterClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Nullable
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordDelimiterClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordingModeClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord mode;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordingModeClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RecordKeyClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String recordWords;
        QualifiedDataName qualifiedDataName;

        @Nullable
        PasswordClause passwordClause;

        String duplicates;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRecordKeyClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReferenceModifier implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String leftParen;
        ArithmeticExpression characterPosition;
        String colon;

        @Nullable
        ArithmeticExpression length;

        String rightParen;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReferenceModifier(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelationalOperator implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelationalOperator(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelationArithmeticComparison implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        ArithmeticExpression arithmeticExpressionA;
        RelationalOperator relationalOperator;
        ArithmeticExpression arithmeticExpressionB;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelationArithmeticComparison(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelationCombinedComparison implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        ArithmeticExpression arithmeticExpression;
        RelationalOperator relationalOperator;
        Parenthesized combinedCondition;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelationCombinedComparison(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelationCombinedCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        List<Cobol> relationalArithmeticExpressions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelationCombinedCondition(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelationSignCondition implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        ArithmeticExpression arithmeticExpression;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelationSignCondition(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RelativeKeyClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelativeKeyClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Release implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String release;
        QualifiedDataName recordName;
        String from;

        @Nullable
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRelease(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Return implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name fileName;
        String record;
        @Nullable
        ReturnInto into;

        StatementPhrase atEndPhrase;

        @Nullable
        StatementPhrase notAtEndPhrase;

        String endReturn;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReturnInto implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String into;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReturnInto(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportDescription implements Cobol {
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
        ReportDescriptionEntry reportDescriptionEntry;

        CobolContainer<Cobol> groupDescriptionEntries;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportDescription(this, p);
        }

        public List<Cobol> getGroupDescriptionEntries() {
            return groupDescriptionEntries.getElements();
        }

        public ReportDescription withGroupDescriptionEntries(List<Cobol> groupDescriptionEntries) {
            return getPadding().withGroupDescriptionEntries(this.groupDescriptionEntries.getPadding().withElements(CobolRightPadded.withElements(
                    this.groupDescriptionEntries.getPadding().getElements(), groupDescriptionEntries)));
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
            private final ReportDescription t;

            public CobolContainer<Cobol> getGroupDescriptionEntries() {
                return t.groupDescriptionEntries;
            }

            public ReportDescription withGroupDescriptionEntries(CobolContainer<Cobol> groupDescriptionEntries) {
                return t.groupDescriptionEntries == groupDescriptionEntries ? t : new ReportDescription(t.padding, t.id, t.prefix, t.markers, t.reportDescriptionEntry, groupDescriptionEntries);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportDescriptionEntry implements Cobol {
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
        String rd;

        @Getter
        @With
        QualifiedDataName qualifiedDataName;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionGlobalClause;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionPageLimitClause;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionHeadingClause;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionFirstDetailClause;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionLastDetailClause;

        @Getter
        @Nullable
        @With
        ReportDescriptionGlobalClause reportDescriptionFootingClause;

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportDescriptionEntry(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public ReportDescriptionEntry withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ReportDescriptionEntry t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ReportDescriptionEntry withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ReportDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.rd, t.qualifiedDataName, t.reportDescriptionGlobalClause, t.reportDescriptionPageLimitClause, t.reportDescriptionHeadingClause, t.reportDescriptionFirstDetailClause, t.reportDescriptionLastDetailClause, t.reportDescriptionFootingClause, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportGroupDescriptionEntryFormat1 implements Cobol {
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
        CobolWord integerLiteral;

        @Getter
        @With
        CobolWord dataName;

        @Getter
        @Nullable
        @With
        ReportGroupLineNumberClause groupLineNumberClause;

        @Getter
        @Nullable
        @With
        ReportGroupNextGroupClause groupNextGroupClause;

        @Getter
        @With
        ReportGroupTypeClause groupTypeClause;

        @Getter
        @Nullable
        @With
        ReportGroupUsageClause groupUsageClause;

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupDescriptionEntryFormat1(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public ReportGroupDescriptionEntryFormat1 withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ReportGroupDescriptionEntryFormat1 t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ReportGroupDescriptionEntryFormat1 withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ReportGroupDescriptionEntryFormat1(t.padding, t.id, t.prefix, t.markers, t.integerLiteral, t.dataName, t.groupLineNumberClause, t.groupNextGroupClause, t.groupTypeClause, t.groupUsageClause, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportGroupDescriptionEntryFormat2 implements Cobol {
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
        CobolWord integerLiteral;

        @Getter
        @Nullable
        @With
        CobolWord dataName;

        @Getter
        @Nullable
        @With
        ReportGroupLineNumberClause reportGroupLineNumberClause;

        @Getter
        @With
        ReportGroupUsageClause groupUsageClause;

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupDescriptionEntryFormat2(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public ReportGroupDescriptionEntryFormat2 withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ReportGroupDescriptionEntryFormat2 t;

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ReportGroupDescriptionEntryFormat2 withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ReportGroupDescriptionEntryFormat2(t.padding, t.id, t.prefix, t.markers, t.integerLiteral, t.dataName, t.reportGroupLineNumberClause, t.groupUsageClause, dot);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportGroupDescriptionEntryFormat3 implements Cobol {
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
        CobolWord integerLiteral;

        @Getter
        @Nullable
        @With
        CobolWord dataName;

        @Nullable
        CobolContainer<Cobol> clauses;

        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupDescriptionEntryFormat3(this, p);
        }

        public List<Cobol> getClauses() {
            return clauses.getElements();
        }

        public ReportGroupDescriptionEntryFormat3 withClauses(List<Cobol> clauses) {
            return getPadding().withClauses(this.clauses.getPadding().withElements(CobolRightPadded.withElements(
                    this.clauses.getPadding().getElements(), clauses)));
        }

        public String getDot() {
            return dot.getElement();
        }

        public ReportGroupDescriptionEntryFormat3 withDot(String dot) {
            //noinspection ConstantConditions
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
            private final ReportGroupDescriptionEntryFormat3 t;

            @Nullable
            public CobolContainer<Cobol> getClauses() {
                return t.clauses;
            }

            public ReportGroupDescriptionEntryFormat3 withClauses(@Nullable CobolContainer<Cobol> clauses) {
                return t.clauses == clauses ? t : new ReportGroupDescriptionEntryFormat3(t.padding, t.id, t.prefix, t.markers, t.integerLiteral, t.dataName, clauses, t.dot);
            }

            public CobolLeftPadded<String> getDot() {
                return t.dot;
            }

            public ReportGroupDescriptionEntryFormat3 withDot(CobolLeftPadded<String> dot) {
                return t.dot == dot ? t : new ReportGroupDescriptionEntryFormat3(t.padding, t.id, t.prefix, t.markers, t.integerLiteral, t.dataName, t.clauses, dot);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupLineNumberClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol clause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupLineNumberClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupLineNumberNextPage implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord integerLiteral;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupLineNumberNextPage(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupLineNumberPlus implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String plus;
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupLineNumberPlus(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupNextGroupClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol clause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupNextGroupClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupNextGroupNextPage implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String nextPage;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupNextGroupNextPage(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupNextGroupPlus implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String plus;
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupNextGroupPlus(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupTypeClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol type;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupTypeClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportGroupUsageClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportGroupUsageClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportDescriptionGlobalClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportDescriptionGlobalClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ReportSection implements Cobol {
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

        CobolContainer<Cobol> descriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportSection(this, p);
        }

        public List<Cobol> getDescriptions() {
            return descriptions.getElements();
        }

        public ReportSection withDescriptions(List<Cobol> descriptions) {
            return getPadding().withDescriptions(this.descriptions.getPadding().withElements(CobolRightPadded.withElements(
                    this.descriptions.getPadding().getElements(), descriptions)));
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
            private final ReportSection t;

            public CobolContainer<Cobol> getDescriptions() {
                return t.descriptions;
            }

            public ReportSection withDescriptions(CobolContainer<Cobol> descriptions) {
                return t.descriptions == descriptions ? t : new ReportSection(t.padding, t.id, t.prefix, t.markers, t.words, descriptions);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReportName implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReportName(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RerunClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String rerun;

        @Nullable
        String on;

        @Nullable
        CobolWord name;

        String every;

        Cobol action;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRerunClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RerunEveryClock implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord integerLiteral;
        String clockUnits;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRerunEveryClock(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RerunEveryOf implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String records;
        CobolWord fileName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRerunEveryOf(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RerunEveryRecords implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord integerLiteral;
        String records;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRerunEveryRecords(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReserveClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        List<Cobol> words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReserveClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ReserveNetworkClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitReserveNetworkClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Rewrite implements Statement {
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
        String rewrite;

        @Getter
        @Nullable
        @With
        QualifiedDataName recordName;

        @Getter
        @Nullable
        @With
        StatementPhrase invalidKeyPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        CobolLeftPadded<String> endRewrite;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRewrite(this, p);
        }

        @Nullable
        public String getEndRewrite() {
            return endRewrite == null ? null : endRewrite.getElement();
        }

        public Rewrite withEndRewrite(@Nullable String endRewrite) {
            if (endRewrite == null) {
                return this.endRewrite == null ? this : new Rewrite(id, prefix, markers, rewrite, recordName, invalidKeyPhrase, notInvalidKeyPhrase, null);
            }
            return getPadding().withEndRewrite(CobolLeftPadded.withElement(this.endRewrite, endRewrite));
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
            private final Rewrite t;

            @Nullable
            public CobolLeftPadded<String> getEndRewrite() {
                return t.endRewrite;
            }

            public Rewrite withEndRewrite(@Nullable CobolLeftPadded<String> endRewrite) {
                return t.endRewrite == endRewrite ? t : new Rewrite(t.padding, t.id, t.prefix, t.markers, t.rewrite, t.recordName, t.invalidKeyPhrase, t.notInvalidKeyPhrase, endRewrite);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class RewriteFrom implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String from;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitRewriteFrom(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Roundable implements Cobol {
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SameClause implements Cobol {
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

        CobolContainer<CobolWord> fileNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSameClause(this, p);
        }

        public List<Cobol.CobolWord> getFileNames() {
            return fileNames.getElements();
        }

        public SameClause withFileNames(List<Cobol.CobolWord> fileNames) {
            return getPadding().withFileNames(this.fileNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.fileNames.getPadding().getElements(), fileNames)));
        }

        public SameClause.Padding getPadding() {
            SameClause.Padding p;
            if (this.padding == null) {
                p = new SameClause.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new SameClause.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final SameClause t;

            public CobolContainer<Cobol.CobolWord> getFileNames() {
                return t.fileNames;
            }

            public SameClause withFileNames(CobolContainer<Cobol.CobolWord> fileNames) {
                return t.fileNames == fileNames ? t : new SameClause(t.padding, t.id, t.prefix, t.markers, t.words, fileNames);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ScreenSection implements Cobol {
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

        CobolContainer<ScreenDescriptionEntry> descriptions;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenSection(this, p);
        }

        public List<Cobol.ScreenDescriptionEntry> getDescriptions() {
            return descriptions.getElements();
        }

        public ScreenSection withDescriptions(List<Cobol.ScreenDescriptionEntry> descriptions) {
            return getPadding().withDescriptions(this.descriptions.getPadding().withElements(CobolRightPadded.withElements(
                    this.descriptions.getPadding().getElements(), descriptions)));
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
            private final ScreenSection t;

            public CobolContainer<Cobol.ScreenDescriptionEntry> getDescriptions() {
                return t.descriptions;
            }

            public ScreenSection withDescriptions(CobolContainer<Cobol.ScreenDescriptionEntry> descriptions) {
                return t.descriptions == descriptions ? t : new ScreenSection(t.padding, t.id, t.prefix, t.markers, t.words, descriptions);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ScreenDescriptionEntry implements Cobol {
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

        @Nullable
        CobolLeftPadded<String> name;

        CobolContainer<Cobol> clauses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionEntry(this, p);
        }

        @Nullable
        public String getName() {
            return name == null ? null : name.getElement();
        }

        public ScreenDescriptionEntry withName(@Nullable String name) {
            if (name == null) {
                return this.name == null ? this : new ScreenDescriptionEntry(id, prefix, markers, words, null, clauses);
            }
            return getPadding().withName(CobolLeftPadded.withElement(this.name, name));
        }

        public List<Cobol> getClauses() {
            return clauses.getElements();
        }

        public ScreenDescriptionEntry withClauses(List<Cobol> clauses) {
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
            private final ScreenDescriptionEntry t;

            @Nullable
            public CobolLeftPadded<String> getName() {
                return t.name;
            }

            public ScreenDescriptionEntry withName(@Nullable CobolLeftPadded<String> name) {
                return t.name == name ? t : new ScreenDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.words, name, t.clauses);
            }

            public CobolContainer<Cobol> getClauses() {
                return t.clauses;
            }

            public ScreenDescriptionEntry withClauses(CobolContainer<Cobol> clauses) {
                return t.clauses == clauses ? t : new ScreenDescriptionEntry(t.padding, t.id, t.prefix, t.markers, t.words, t.name, clauses);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionAutoClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String auto;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionAutoClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionBackgroundColorClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String background;

        String is;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionBackgroundColorClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionBellClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String bell;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionBellClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionBlankClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionBlankClause(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionBlankWhenZeroClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionBlankWhenZeroClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionBlinkClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String blink;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionBlinkClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionColumnClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionColumnClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionControlClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionControlClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionEraseClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionEraseClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionForegroundColorClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionForegroundColorClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionFromClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String from;
        Name value;

        @Nullable
        ScreenDescriptionToClause screenDescriptionToClause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionFromClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionFullClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionFullClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionGridClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionGridClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionJustifiedClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionJustifiedClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionLightClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String light;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionLightClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionLineClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionLineClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionPictureClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        PictureString pictureString;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionPictureClause(this, p);
        }
    }


    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionPromptClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Nullable
        ScreenDescriptionPromptOccursClause screenDescriptionPromptOccursClause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionPromptClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionPromptOccursClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String occurs;
        CobolWord integer;
        String times;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionPromptOccursClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionRequiredClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String required;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionRequiredClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionReverseVideoClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionReverseVideoClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionSignClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionSignClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionSizeClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionSizeClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionToClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String to;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionToClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionUnderlineClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String underline;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionUnderlineClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionUsageClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionUsageClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionUsingClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String using;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionUsingClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionValueClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name value;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionValueClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ScreenDescriptionZeroFillClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitScreenDescriptionZeroFillClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Search implements Statement {
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

        @Getter
        @With
        QualifiedDataName qualifiedDataName;

        @Getter
        @Nullable
        @With
        SearchVarying searchVarying;

        @Getter
        @Nullable
        @With
        StatementPhrase atEndPhrase;

        CobolContainer<SearchWhen> searchWhen;

        @Getter
        @With
        String endSearch;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSearch(this, p);
        }

        public List<Cobol.SearchWhen> getSearchWhen() {
            return searchWhen.getElements();
        }

        public Search withSearchWhen(List<Cobol.SearchWhen> searchWhen) {
            return getPadding().withSearchWhen(this.searchWhen.getPadding().withElements(CobolRightPadded.withElements(
                    this.searchWhen.getPadding().getElements(), searchWhen)));
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
            private final Search t;

            public CobolContainer<Cobol.SearchWhen> getSearchWhen() {
                return t.searchWhen;
            }

            public Search withSearchWhen(CobolContainer<Cobol.SearchWhen> searchWhen) {
                return t.searchWhen == searchWhen ? t : new Search(t.padding, t.id, t.prefix, t.markers, t.words, t.qualifiedDataName, t.searchVarying, t.atEndPhrase, searchWhen, t.endSearch);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SearchVarying implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String varying;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSearchVarying(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SearchWhen implements Cobol {
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
        String when;

        @Getter
        @With
        Condition condition;

        @Getter
        @With
        String nextSentence;
        CobolContainer<Statement> statements;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSearchWhen(this, p);
        }

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public SearchWhen withStatements(List<Statement> statements) {
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
            private final SearchWhen t;

            public CobolContainer<Statement> getStatements() {
                return t.statements;
            }

            public SearchWhen withStatements(CobolContainer<Statement> statements) {
                return t.statements == statements ? t : new SearchWhen(t.padding, t.id, t.prefix, t.markers, t.when, t.condition, t.nextSentence, statements);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SelectClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord fileName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSelectClause(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Send implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String send;
        Cobol statement;

        @Nullable
        StatementPhrase onExceptionClause;

        @Nullable
        StatementPhrase notOnExceptionClause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSend(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SendStatementSync implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;

        @Nullable
        SendPhrase sendFromPhrase;

        @Nullable
        SendPhrase sendWithPhrase;

        @Nullable
        SendPhrase sendReplacingPhrase;

        @Nullable
        SendPhrase sendAdvancingPhrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSendStatementSync(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SendPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;

        @Nullable
        Cobol target;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSendPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SendAdvancingLines implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;
        String lines;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSendAdvancingLines(this, p);
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

        @Getter
        @With
        List<Statement> statements;
        CobolLeftPadded<String> dot;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSentence(this, p);
        }

        public String getDot() {
            return dot.getElement();
        }

        public Sentence withDot(String dot) {
            //noinspection ConstantConditions
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

        public List<Identifier> getTo() {
            return to.getElements();
        }

        public SetTo withTo(List<Identifier> to) {
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

            public CobolContainer<Identifier> getTo() {
                return t.to;
            }

            public SetTo withTo(CobolContainer<Identifier> to) {
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

        public List<Identifier> getTo() {
            return to.getElements();
        }

        public SetUpDown withTo(List<Identifier> to) {
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

            public CobolContainer<Identifier> getTo() {
                return t.to;
            }

            public SetUpDown withTo(CobolContainer<Identifier> to) {
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
    class SpecialNames implements Cobol {
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

        @Nullable
        CobolContainer<Cobol> clauses;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSpecialNames(this, p);
        }

        public List<Cobol> getClauses() {
            return clauses.getElements();
        }

        public SpecialNames withClauses(List<Cobol> clauses) {
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
            private final SpecialNames t;

            @Nullable
            public CobolContainer<Cobol> getClauses() {
                return t.clauses;
            }

            public SpecialNames withClauses(@Nullable CobolContainer<Cobol> clauses) {
                return t.clauses == clauses ? t : new SpecialNames(t.padding, t.id, t.prefix, t.markers, t.words, clauses);
            }
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Sort implements Statement {
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
        String sort;

        @Getter
        @With
        CobolWord fileName;

        CobolContainer<Sortable> sortOnKeyClause;

        @Getter
        @Nullable
        @With
        CobolWord sortDuplicatesPhrase;

        @Getter
        @With
        SortCollatingSequencePhrase sortCollatingSequencePhrase;

        @Getter
        @With
        SortProcedurePhrase sortInputProcedurePhrase;

        CobolContainer<Sortable> sortUsing;

        @Getter
        @Nullable
        @With
        SortProcedurePhrase sortOutputProcedurePhrase;

        CobolContainer<Sortable> sortGiving;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSort(this, p);
        }

        public List<Cobol.Sortable> getSortOnKeyClause() {
            return sortOnKeyClause.getElements();
        }

        public Sort withSortOnKeyClause(List<Cobol.Sortable> sortOnKeyClause) {
            return getPadding().withSortOnKeyClause(this.sortOnKeyClause.getPadding().withElements(CobolRightPadded.withElements(
                    this.sortOnKeyClause.getPadding().getElements(), sortOnKeyClause)));
        }

        public List<Cobol.Sortable> getSortUsing() {
            return sortUsing.getElements();
        }

        public Sort withSortUsing(List<Cobol.Sortable> sortUsing) {
            return getPadding().withSortUsing(this.sortUsing.getPadding().withElements(CobolRightPadded.withElements(
                    this.sortUsing.getPadding().getElements(), sortUsing)));
        }

        public List<Cobol.Sortable> getSortGiving() {
            return sortGiving.getElements();
        }

        public Sort withSortGiving(List<Cobol.Sortable> sortGiving) {
            return getPadding().withSortGiving(this.sortGiving.getPadding().withElements(CobolRightPadded.withElements(
                    this.sortGiving.getPadding().getElements(), sortGiving)));
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
            private final Sort t;

            public CobolContainer<Cobol.Sortable> getSortOnKeyClause() {
                return t.sortOnKeyClause;
            }

            public Sort withSortOnKeyClause(CobolContainer<Cobol.Sortable> sortOnKeyClause) {
                return t.sortOnKeyClause == sortOnKeyClause ? t : new Sort(t.padding, t.id, t.prefix, t.markers, t.sort, t.fileName, sortOnKeyClause, t.sortDuplicatesPhrase, t.sortCollatingSequencePhrase, t.sortInputProcedurePhrase, t.sortUsing, t.sortOutputProcedurePhrase, t.sortGiving);
            }

            public CobolContainer<Cobol.Sortable> getSortUsing() {
                return t.sortUsing;
            }

            public Sort withSortUsing(CobolContainer<Cobol.Sortable> sortUsing) {
                return t.sortUsing == sortUsing ? t : new Sort(t.padding, t.id, t.prefix, t.markers, t.sort, t.fileName, t.sortOnKeyClause, t.sortDuplicatesPhrase, t.sortCollatingSequencePhrase, t.sortInputProcedurePhrase, sortUsing, t.sortOutputProcedurePhrase, t.sortGiving);
            }

            public CobolContainer<Cobol.Sortable> getSortGiving() {
                return t.sortGiving;
            }

            public Sort withSortGiving(CobolContainer<Cobol.Sortable> sortGiving) {
                return t.sortGiving == sortGiving ? t : new Sort(t.padding, t.id, t.prefix, t.markers, t.sort, t.fileName, t.sortOnKeyClause, t.sortDuplicatesPhrase, t.sortCollatingSequencePhrase, t.sortInputProcedurePhrase, t.sortUsing, t.sortOutputProcedurePhrase, sortGiving);
            }
        }

    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SortCollatingSequencePhrase implements Cobol {
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

        CobolContainer<CobolWord> alphabetNames;

        @Getter
        @Nullable
        @With
        Sortable sortCollatingAlphanumeric;

        @Getter
        @Nullable
        @With
        Sortable sortCollatingNational;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSortCollatingSequencePhrase(this, p);
        }

        public List<Cobol.CobolWord> getAlphabetNames() {
            return alphabetNames.getElements();
        }

        public SortCollatingSequencePhrase withAlphabetNames(List<Cobol.CobolWord> alphabetNames) {
            return getPadding().withAlphabetNames(this.alphabetNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.alphabetNames.getPadding().getElements(), alphabetNames)));
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
            private final SortCollatingSequencePhrase t;
            @Nullable
            public CobolContainer<Cobol.CobolWord> getAlphabetNames() {
                return t.alphabetNames;
            }

            public SortCollatingSequencePhrase withAlphabetNames(@Nullable CobolContainer<Cobol.CobolWord> alphabetNames) {
                return t.alphabetNames == alphabetNames ? t : new SortCollatingSequencePhrase(t.padding, t.id, t.prefix, t.markers, t.words, alphabetNames, t.sortCollatingAlphanumeric, t.sortCollatingNational);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SortProcedurePhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        CobolWord procedureName;

        @Nullable
        Sortable sortInputThrough;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSortProcedurePhrase(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Sortable implements Cobol {
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

        CobolContainer<CobolWord> names;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSortable(this, p);
        }

        public List<Cobol.CobolWord> getNames() {
            return names.getElements();
        }

        public Sortable withNames(List<Cobol.CobolWord> names) {
            return getPadding().withNames(this.names.getPadding().withElements(CobolRightPadded.withElements(
                    this.names.getPadding().getElements(), names)));
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
            private final Sortable t;

            @Nullable
            public CobolContainer<Cobol.CobolWord> getNames() {
                return t.names;
            }

            public Sortable withNames(@Nullable CobolContainer<Cobol.CobolWord> names) {
                return t.names == names ? t : new Sortable(t.padding, t.id, t.prefix, t.markers, t.words, names);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SortGiving implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord fileName;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSortGiving(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Start implements Statement {
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
        String start;

        @Getter
        @With
        CobolWord fileName;

        @Getter
        @Nullable
        @With
        StartKey startKey;

        @Getter
        @Nullable
        @With
        StatementPhrase invalidKeyPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        CobolLeftPadded<String> endStart;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStart(this, p);
        }

        @Nullable
        public String getEndStart() {
            return endStart == null ? null : endStart.getElement();
        }

        public Start withEndStart(@Nullable String endStart) {
            if (endStart == null) {
                return this.endStart == null ? this : new Start(id, prefix, markers, start, fileName, startKey, invalidKeyPhrase, notInvalidKeyPhrase, null);
            }
            return getPadding().withEndStart(CobolLeftPadded.withElement(this.endStart, endStart));
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
            private final Start t;

            @Nullable
            public CobolLeftPadded<String> getEndStart() {
                return t.endStart;
            }

            public Start withEndStart(@Nullable CobolLeftPadded<String> endStart) {
                return t.endStart == endStart ? t : new Start(t.padding, t.id, t.prefix, t.markers, t.start, t.fileName, t.startKey, t.invalidKeyPhrase, t.notInvalidKeyPhrase, endStart);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StartKey implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStartKey(this, p);
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
    class StringStatement implements Statement {
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
        CobolWord string;

        CobolContainer<Cobol> stringSendingPhrases;

        @Getter
        @With
        StringIntoPhrase stringIntoPhrase;

        @Getter
        @Nullable
        @With
        StringWithPointerPhrase stringWithPointerPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase onOverflowPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnOverflowPhrase;

        @Nullable
        CobolLeftPadded<String> endString;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringStatement(this, p);
        }

        public List<Cobol> getStringSendingPhrases() {
            return stringSendingPhrases.getElements();
        }

        public StringStatement withStringSendingPhrases(List<Cobol> stringSendingPhrases) {
            return getPadding().withStringSendingPhrases(this.stringSendingPhrases.getPadding().withElements(CobolRightPadded.withElements(
                    this.stringSendingPhrases.getPadding().getElements(), stringSendingPhrases)));
        }

        @Nullable
        public String getEndString() {
            return endString == null ? null : endString.getElement();
        }

        public StringStatement withEndString(@Nullable String endString) {
            if (endString == null) {
                return this.endString == null ? this : new StringStatement(id, prefix, markers, string, stringSendingPhrases, stringIntoPhrase, stringWithPointerPhrase, onOverflowPhrase, notOnOverflowPhrase, null);
            }
            return getPadding().withEndString(CobolLeftPadded.withElement(this.endString, endString));
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
            private final StringStatement t;

            public CobolContainer<Cobol> getStringSendingPhrases() {
                return t.stringSendingPhrases;
            }

            public StringStatement withStringSendingPhrases(CobolContainer<Cobol> stringSendingPhrases) {
                return t.stringSendingPhrases == stringSendingPhrases ? t : new StringStatement(t.padding, t.id, t.prefix, t.markers, t.string, stringSendingPhrases, t.stringIntoPhrase, t.stringWithPointerPhrase, t.onOverflowPhrase, t.notOnOverflowPhrase, t.endString);
            }

            @Nullable
            public CobolLeftPadded<String> getEndString() {
                return t.endString;
            }

            public StringStatement withEndString(@Nullable CobolLeftPadded<String> endString) {
                return t.endString == endString ? t : new StringStatement(t.padding, t.id, t.prefix, t.markers, t.string, t.stringSendingPhrases, t.stringIntoPhrase, t.stringWithPointerPhrase, t.onOverflowPhrase, t.notOnOverflowPhrase, endString);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringSendingPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        List<Cobol> sendings;
        Cobol phrase;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringSendingPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringDelimitedByPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringDelimitedByPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringForPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String word;
        Name identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringForPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringIntoPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String into;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringIntoPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringWithPointerPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitStringWithPointerPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Subscript implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        Cobol first;

        @Nullable
        CobolWord integerLiteral;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubscript(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Subtract implements Statement {
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
        CobolWord substract;

        @Getter
        @With
        Cobol operation;

        @Getter
        @Nullable
        @With
        StatementPhrase onSizeErrorPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnSizeErrorPhrase;

        @Nullable
        CobolLeftPadded<String> endSubtract;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubtract(this, p);
        }

        @Nullable
        public String getEndSubtract() {
            return endSubtract == null ? null : endSubtract.getElement();
        }

        public Subtract withEndSubtract(@Nullable String endSubtract) {
            if (endSubtract == null) {
                return this.endSubtract == null ? this : new Subtract(id, prefix, markers, substract, operation, onSizeErrorPhrase, notOnSizeErrorPhrase, null);
            }
            return getPadding().withEndSubtract(CobolLeftPadded.withElement(this.endSubtract, endSubtract));
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
            private final Subtract t;

            @Nullable
            public CobolLeftPadded<String> getEndSubtract() {
                return t.endSubtract;
            }

            public Subtract withEndSubtract(@Nullable CobolLeftPadded<String> endSubtract) {
                return t.endSubtract == endSubtract ? t : new Subtract(t.padding, t.id, t.prefix, t.markers, t.substract, t.operation, t.onSizeErrorPhrase, t.notOnSizeErrorPhrase, endSubtract);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SubtractFromStatement implements Cobol {
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

        CobolContainer<Name> subtractSubtrahend;

        @Getter
        @With
        CobolWord from;

        CobolContainer<Roundable> subtractMinuend;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubtractFromStatement(this, p);
        }

        public List<Name> getSubtractSubtrahend() {
            return subtractSubtrahend.getElements();
        }

        public SubtractFromStatement withSubtractSubtrahend(List<Name> subtractSubtrahend) {
            return getPadding().withSubtractSubtrahend(this.subtractSubtrahend.getPadding().withElements(CobolRightPadded.withElements(
                    this.subtractSubtrahend.getPadding().getElements(), subtractSubtrahend)));
        }

        public List<Cobol.Roundable> getSubtractMinuend() {
            return subtractMinuend.getElements();
        }

        public SubtractFromStatement withSubtractMinuend(List<Cobol.Roundable> subtractMinuend) {
            return getPadding().withSubtractMinuend(this.subtractMinuend.getPadding().withElements(CobolRightPadded.withElements(
                    this.subtractMinuend.getPadding().getElements(), subtractMinuend)));
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
            private final SubtractFromStatement t;

            public CobolContainer<Name> getSubtractSubtrahend() {
                return t.subtractSubtrahend;
            }

            public SubtractFromStatement withSubtractSubtrahend(CobolContainer<Name> subtractSubtrahend) {
                return t.subtractSubtrahend == subtractSubtrahend ? t : new SubtractFromStatement(t.padding, t.id, t.prefix, t.markers, subtractSubtrahend, t.from, t.subtractMinuend);
            }

            public CobolContainer<Cobol.Roundable> getSubtractMinuend() {
                return t.subtractMinuend;
            }

            public SubtractFromStatement withSubtractMinuend(CobolContainer<Cobol.Roundable> subtractMinuend) {
                return t.subtractMinuend == subtractMinuend ? t : new SubtractFromStatement(t.padding, t.id, t.prefix, t.markers, t.subtractSubtrahend, t.from, subtractMinuend);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SubtractFromGivingStatement implements Cobol {
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

        CobolContainer<Name> subtractSubtrahend;

        @Getter
        @With
        CobolWord from;

        @Getter
        @With
        Name subtractMinuendGiving;

        @Getter
        @With
        CobolWord giving;

        CobolContainer<Roundable> subtractGiving;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubtractFromGivingStatement(this, p);
        }

        public List<Name> getSubtractSubtrahend() {
            return subtractSubtrahend.getElements();
        }

        public SubtractFromGivingStatement withSubtractSubtrahend(List<Name> subtractSubtrahend) {
            return getPadding().withSubtractSubtrahend(this.subtractSubtrahend.getPadding().withElements(CobolRightPadded.withElements(
                    this.subtractSubtrahend.getPadding().getElements(), subtractSubtrahend)));
        }

        public List<Cobol.Roundable> getSubtractGiving() {
            return subtractGiving.getElements();
        }

        public SubtractFromGivingStatement withSubtractGiving(List<Cobol.Roundable> subtractGiving) {
            return getPadding().withSubtractGiving(this.subtractGiving.getPadding().withElements(CobolRightPadded.withElements(
                    this.subtractGiving.getPadding().getElements(), subtractGiving)));
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
            private final SubtractFromGivingStatement t;

            public CobolContainer<Name> getSubtractSubtrahend() {
                return t.subtractSubtrahend;
            }

            public SubtractFromGivingStatement withSubtractSubtrahend(CobolContainer<Name> subtractSubtrahend) {
                return t.subtractSubtrahend == subtractSubtrahend ? t : new SubtractFromGivingStatement(t.padding, t.id, t.prefix, t.markers, subtractSubtrahend, t.from, t.subtractMinuendGiving, t.giving, t.subtractGiving);
            }

            public CobolContainer<Cobol.Roundable> getSubtractGiving() {
                return t.subtractGiving;
            }

            public SubtractFromGivingStatement withSubtractGiving(CobolContainer<Cobol.Roundable> subtractGiving) {
                return t.subtractGiving == subtractGiving ? t : new SubtractFromGivingStatement(t.padding, t.id, t.prefix, t.markers, t.subtractSubtrahend, t.from, t.subtractMinuendGiving, t.giving, subtractGiving);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SubtractCorrespondingStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord corresponding;
        QualifiedDataName qualifiedDataName;
        CobolWord giving;
        SubtractMinuendCorresponding subtractMinuendCorresponding;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubtractCorrespondingStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class SubtractMinuendCorresponding implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        QualifiedDataName qualifiedDataName;

        @Nullable
        CobolWord rounded;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSubtractMinuendCorresponding(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SymbolicCharacter implements Cobol {
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

        CobolContainer<Identifier> symbols;
        CobolContainer<Literal> literals;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSymbolicCharacter(this, p);
        }

        public List<Identifier> getSymbols() {
            return symbols.getElements();
        }

        public SymbolicCharacter withSymbols(List<Identifier> symbols) {
            return getPadding().withSymbols(this.symbols.getPadding().withElements(CobolRightPadded.withElements(
                    this.symbols.getPadding().getElements(), symbols)));
        }

        public List<Literal> getLiterals() {
            return literals.getElements();
        }

        public SymbolicCharacter withLiterals(List<Literal> literals) {
            return getPadding().withLiterals(this.literals.getPadding().withElements(CobolRightPadded.withElements(
                    this.literals.getPadding().getElements(), literals)));
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
            private final SymbolicCharacter t;

            public CobolContainer<Identifier> getSymbols() {
                return t.symbols;
            }

            public SymbolicCharacter withSymbols(CobolContainer<Identifier> symbols) {
                return t.symbols == symbols ? t : new SymbolicCharacter(t.padding, t.id, t.prefix, t.markers, symbols, t.literals);
            }

            public CobolContainer<Literal> getLiterals() {
                return t.literals;
            }

            public SymbolicCharacter withLiterals(CobolContainer<Literal> literals) {
                return t.literals == literals ? t : new SymbolicCharacter(t.padding, t.id, t.prefix, t.markers, t.symbols, literals);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class SymbolicCharactersClause implements Cobol {
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

        CobolContainer<SymbolicCharacter> symbols;

        @Nullable
        CobolLeftPadded<String> inAlphabet;

        @Getter
        @Nullable
        @With
        Identifier alphabetName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitSymbolicCharactersClause(this, p);
        }

        public List<Cobol.SymbolicCharacter> getSymbols() {
            return symbols.getElements();
        }

        public SymbolicCharactersClause withSymbols(List<Cobol.SymbolicCharacter> symbols) {
            return getPadding().withSymbols(this.symbols.getPadding().withElements(CobolRightPadded.withElements(
                    this.symbols.getPadding().getElements(), symbols)));
        }

        @Nullable
        public String getInAlphabet() {
            return inAlphabet == null ? null : inAlphabet.getElement();
        }

        public SymbolicCharactersClause withInAlphabet(@Nullable String inAlphabet) {
            if (inAlphabet == null) {
                return this.inAlphabet == null ? this : new SymbolicCharactersClause(id, prefix, markers, words, symbols, null, alphabetName);
            }
            return getPadding().withInAlphabet(CobolLeftPadded.withElement(this.inAlphabet, inAlphabet));
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
            private final SymbolicCharactersClause t;

            public CobolContainer<Cobol.SymbolicCharacter> getSymbols() {
                return t.symbols;
            }

            public SymbolicCharactersClause withSymbols(CobolContainer<Cobol.SymbolicCharacter> symbols) {
                return t.symbols == symbols ? t : new SymbolicCharactersClause(t.padding, t.id, t.prefix, t.markers, t.words, symbols, t.inAlphabet, t.alphabetName);
            }

            @Nullable
            public CobolLeftPadded<String> getInAlphabet() {
                return t.inAlphabet;
            }

            public SymbolicCharactersClause withInAlphabet(@Nullable CobolLeftPadded<String> inAlphabet) {
                return t.inAlphabet == inAlphabet ? t : new SymbolicCharactersClause(t.padding, t.id, t.prefix, t.markers, t.words, t.symbols, inAlphabet, t.alphabetName);
            }
        }
    }


    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class TableCall implements Identifier {
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
        QualifiedDataName qualifiedDataName;

        CobolContainer<Parenthesized> subscripts;

        @Getter
        @Nullable
        @With
        ReferenceModifier referenceModifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitTableCall(this, p);
        }

        public List<Cobol.Parenthesized> getSubscripts() {
            return subscripts.getElements();
        }

        public TableCall withSubscripts(List<Cobol.Parenthesized> subscripts) {
            return getPadding().withSubscripts(this.subscripts.getPadding().withElements(CobolRightPadded.withElements(
                    this.subscripts.getPadding().getElements(), subscripts)));
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
            private final TableCall t;

            public CobolContainer<Cobol.Parenthesized> getSubscripts() {
                return t.subscripts;
            }

            public TableCall withSubscripts(CobolContainer<Cobol.Parenthesized> subscripts) {
                return t.subscripts == subscripts ? t : new TableCall(t.padding, t.id, t.prefix, t.markers, t.qualifiedDataName, subscripts, t.referenceModifier);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Terminate implements Statement {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord terminate;
        QualifiedDataName reportName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitTerminate(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class UnString implements Statement {
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
        String unstring;

        @Getter
        @With
        UnstringSendingPhrase unstringSendingPhrase;

        @Getter
        @With
        UnstringIntoPhrase unstringIntoPhrase;

        @Getter
        @Nullable
        @With
        UnstringWithPointerPhrase unstringWithPointerPhrase;

        @Getter
        @Nullable
        @With
        UnstringTallyingPhrase unstringTallyingPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase onOverflowPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notOnOverflowPhrase;

        @Nullable
        CobolLeftPadded<String> endUnstring;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnString(this, p);
        }

        @Nullable
        public String getEndUnstring() {
            return endUnstring == null ? null : endUnstring.getElement();
        }

        public UnString withEndUnstring(@Nullable String endUnstring) {
            if (endUnstring == null) {
                return this.endUnstring == null ? this : new UnString(id, prefix, markers, unstring, unstringSendingPhrase, unstringIntoPhrase, unstringWithPointerPhrase, unstringTallyingPhrase, onOverflowPhrase, notOnOverflowPhrase, null);
            }
            return getPadding().withEndUnstring(CobolLeftPadded.withElement(this.endUnstring, endUnstring));
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
            private final UnString t;

            @Nullable
            public CobolLeftPadded<String> getEndUnstring() {
                return t.endUnstring;
            }

            public UnString withEndUnstring(@Nullable CobolLeftPadded<String> endUnstring) {
                return t.endUnstring == endUnstring ? t : new UnString(t.padding, t.id, t.prefix, t.markers, t.unstring, t.unstringSendingPhrase, t.unstringIntoPhrase, t.unstringWithPointerPhrase, t.unstringTallyingPhrase, t.onOverflowPhrase, t.notOnOverflowPhrase, endUnstring);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class UnstringSendingPhrase implements Cobol {
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

        @Getter
        @Nullable
        @With
        UnstringDelimitedByPhrase unstringDelimitedByPhrase;

        @Nullable
        CobolContainer<UnstringOrAllPhrase> unstringOrAllPhrases;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringSendingPhrase(this, p);
        }

        public List<Cobol.UnstringOrAllPhrase> getUnstringOrAllPhrases() {
            return unstringOrAllPhrases.getElements();
        }

        public UnstringSendingPhrase withUnstringOrAllPhrases(List<Cobol.UnstringOrAllPhrase> unstringOrAllPhrases) {
            return getPadding().withUnstringOrAllPhrases(this.unstringOrAllPhrases.getPadding().withElements(CobolRightPadded.withElements(
                    this.unstringOrAllPhrases.getPadding().getElements(), unstringOrAllPhrases)));
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
            private final UnstringSendingPhrase t;

            @Nullable
            public CobolContainer<Cobol.UnstringOrAllPhrase> getUnstringOrAllPhrases() {
                return t.unstringOrAllPhrases;
            }

            public UnstringSendingPhrase withUnstringOrAllPhrases(@Nullable CobolContainer<Cobol.UnstringOrAllPhrase> unstringOrAllPhrases) {
                return t.unstringOrAllPhrases == unstringOrAllPhrases ? t : new UnstringSendingPhrase(t.padding, t.id, t.prefix, t.markers, t.identifier, t.unstringDelimitedByPhrase, unstringOrAllPhrases);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringDelimitedByPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringDelimitedByPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringOrAllPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringOrAllPhrase(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class UnstringIntoPhrase implements Cobol {
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
        String into;

        CobolContainer<UnstringInto> unstringIntos;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringIntoPhrase(this, p);
        }

        public List<Cobol.UnstringInto> getUnstringIntos() {
            return unstringIntos.getElements();
        }

        public UnstringIntoPhrase withUnstringIntos(List<Cobol.UnstringInto> unstringIntos) {
            return getPadding().withUnstringIntos(this.unstringIntos.getPadding().withElements(CobolRightPadded.withElements(
                    this.unstringIntos.getPadding().getElements(), unstringIntos)));
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
            private final UnstringIntoPhrase t;

            public CobolContainer<Cobol.UnstringInto> getUnstringIntos() {
                return t.unstringIntos;
            }

            public UnstringIntoPhrase withUnstringIntos(CobolContainer<Cobol.UnstringInto> unstringIntos) {
                return t.unstringIntos == unstringIntos ? t : new UnstringIntoPhrase(t.padding, t.id, t.prefix, t.markers, t.into, unstringIntos);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringInto implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier identifier;

        @Nullable
        UnstringDelimiterIn unstringDelimiterIn;

        @Nullable
        UnstringCountIn unstringCountIn;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringInto(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringDelimiterIn implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringDelimiterIn(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringCountIn implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Identifier identifier;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringCountIn(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringWithPointerPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringWithPointerPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UnstringTallyingPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        QualifiedDataName qualifiedDataName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUnstringTallyingPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UseStatement implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String use;
        Cobol clause;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUseStatement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UseAfterClause implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        UseAfterOn useAfterOn;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUseAfterClause(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class UseAfterOn implements Cobol {
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
        @Nullable
        @With
        String afterOn;

        @Nullable
        CobolContainer<Name> fileNames;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUseAfterOn(this, p);
        }

        public List<Name> getFileNames() {
            return fileNames.getElements();
        }

        public UseAfterOn withFileNames(List<Name> fileNames) {
            return getPadding().withFileNames(this.fileNames.getPadding().withElements(CobolRightPadded.withElements(
                    this.fileNames.getPadding().getElements(), fileNames)));
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
            private final UseAfterOn t;

            @Nullable
            public CobolContainer<Name> getFileNames() {
                return t.fileNames;
            }

            public UseAfterOn withFileNames(@Nullable CobolContainer<Name> fileNames) {
                return t.fileNames == fileNames ? t : new UseAfterOn(t.padding, t.id, t.prefix, t.markers, t.afterOn, fileNames);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class UseDebugClause implements Cobol {
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

        CobolContainer<UseDebugOn> useDebugs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUseDebugClause(this, p);
        }

        public List<Cobol.UseDebugOn> getUseDebugs() {
            return useDebugs.getElements();
        }

        public UseDebugClause withUseDebugs(List<Cobol.UseDebugOn> useDebugs) {
            return getPadding().withUseDebugs(this.useDebugs.getPadding().withElements(CobolRightPadded.withElements(
                    this.useDebugs.getPadding().getElements(), useDebugs)));
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
            private final UseDebugClause t;

            public CobolContainer<Cobol.UseDebugOn> getUseDebugs() {
                return t.useDebugs;
            }

            public UseDebugClause withUseDebugs(CobolContainer<Cobol.UseDebugOn> useDebugs) {
                return t.useDebugs == useDebugs ? t : new UseDebugClause(t.padding, t.id, t.prefix, t.markers, t.words, useDebugs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class UseDebugOn implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        String words;

        @Nullable
        Name name;

        @Nullable
        ProcedureName procedureName;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitUseDebugOn(this, p);
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
    class ValueOfClause implements Cobol {
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
        String valueOf;

        CobolContainer<ValuePair> valuePairs;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitValueOfClause(this, p);
        }

        public List<Cobol.ValuePair> getValuePairs() {
            return valuePairs.getElements();
        }

        public ValueOfClause withValuePairs(List<Cobol.ValuePair> valuePairs) {
            return getPadding().withValuePairs(this.valuePairs.getPadding().withElements(CobolRightPadded.withElements(
                    this.valuePairs.getPadding().getElements(), valuePairs)));
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
            private final ValueOfClause t;

            public CobolContainer<Cobol.ValuePair> getValuePairs() {
                return t.valuePairs;
            }

            public ValueOfClause withValuePairs(CobolContainer<Cobol.ValuePair> valuePairs) {
                return t.valuePairs == valuePairs ? t : new ValueOfClause(t.padding, t.id, t.prefix, t.markers, t.valueOf, valuePairs);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ValuePair implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        CobolWord systemName;
        String is;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitValuePair(this, p);
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

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Write implements Statement {
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
        String write;

        @Getter
        @With
        QualifiedDataName recordName;

        @Getter
        @Nullable
        @With
        WriteFromPhrase writeFromPhrase;

        @Getter
        @Nullable
        @With
        WriteAdvancingPhrase writeAdvancingPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase writeAtEndOfPagePhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase writeNotAtEndOfPagePhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase invalidKeyPhrase;

        @Getter
        @Nullable
        @With
        StatementPhrase notInvalidKeyPhrase;

        @Nullable
        CobolLeftPadded<String> endWrite;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWrite(this, p);
        }

        @Nullable
        public String getEndWrite() {
            return endWrite == null ? null : endWrite.getElement();
        }

        public Write withEndWrite(@Nullable String endWrite) {
            if (endWrite == null) {
                return this.endWrite == null ? this : new Write(id, prefix, markers, write, recordName, writeFromPhrase, writeAdvancingPhrase, writeAtEndOfPagePhrase, writeNotAtEndOfPagePhrase, invalidKeyPhrase, notInvalidKeyPhrase, null);
            }
            return getPadding().withEndWrite(CobolLeftPadded.withElement(this.endWrite, endWrite));
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
            private final Write t;

            @Nullable
            public CobolLeftPadded<String> getEndWrite() {
                return t.endWrite;
            }

            public Write withEndWrite(@Nullable CobolLeftPadded<String> endWrite) {
                return t.endWrite == endWrite ? t : new Write(t.padding, t.id, t.prefix, t.markers, t.write, t.recordName, t.writeFromPhrase, t.writeAdvancingPhrase, t.writeAtEndOfPagePhrase, t.writeNotAtEndOfPagePhrase, t.invalidKeyPhrase, t.notInvalidKeyPhrase, endWrite);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WriteFromPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String from;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWriteFromPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WriteAdvancingPhrase implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String words;
        Cobol writeBy;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWriteAdvancingPhrase(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WriteAdvancingPage implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String page;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWriteAdvancingPage(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WriteAdvancingLines implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;
        String words;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWriteAdvancingLines(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class WriteAdvancingMnemonic implements Cobol {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Name name;

        @Override
        public <P> Cobol acceptCobol(CobolVisitor<P> v, P p) {
            return v.visitWriteAdvancingMnemonic(this, p);
        }
    }
}
