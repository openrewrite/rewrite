/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class Assertions {
    public static <S extends SourceFile> StringSourceFileAssert<S> whenParsedBy(Parser<S> parser, String source) {
        return new StringSourceFileAssert<>(parser, source);
    }

    public static <S extends SourceFile> PathSourceFileAssert<S> whenParsedBy(Parser<S> parser, Path source) {
        return new PathSourceFileAssert<>(parser, source);
    }

    public static class StringSourceFileAssert<S extends SourceFile> {
        private final Parser<S> parser;
        private final String primarySource;
        private final List<String> sourceFiles = new ArrayList<>();

        public StringSourceFileAssert(Parser<S> parser, String source) {
            this.parser = parser;
            this.primarySource = StringUtils.trimIndent(source);
            this.sourceFiles.add(primarySource);
        }

        public StringSourceFileAssert<S> whichDependsOn(String... sources) {
            for (String source : sources) {
                sourceFiles.add(StringUtils.trimIndent(source));
            }
            return this;
        }

        public RefactoringAssert<S> whenVisitedBy(RefactorVisitor<?> visitor) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedBy(visitor);
        }

        public RefactoringAssert<S> whenVisitedBy(Iterable<RefactorVisitor<?>> visitors) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedBy(visitors);
        }

        public RefactoringAssert<S> whenVisitedByMapped(Function<S, RefactorVisitor<? super S>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedByMapped(visitorFunction);
        }

        public RefactoringAssert<S> whenVisitedByMany(Function<S, Iterable<RefactorVisitor<? super S>>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedByMany(visitorFunction);
        }

        private S primary(List<S> sources) {
            return sources.stream().filter(s -> s.print().trim().equals(primarySource)).findAny()
                    .orElseThrow(() -> new IllegalStateException("unable to find primary source"));
        }
    }

    public static class PathSourceFileAssert<S extends SourceFile> {
        private final Parser<S> parser;

        private final Path primarySource;
        private final List<Path> sourceFiles = new ArrayList<>();

        @Nullable
        private Path relativeTo;

        public PathSourceFileAssert(Parser<S> parser, Path source) {
            this.parser = parser;
            this.primarySource = source;
            this.sourceFiles.add(source);
        }

        public PathSourceFileAssert<S> relativeTo(@Nullable Path relativeTo) {
            this.relativeTo = relativeTo;
            return this;
        }

        public PathSourceFileAssert<S> whichDependsOn(Path... sources) {
            Collections.addAll(sourceFiles, sources);
            return this;
        }

        public RefactoringAssert<S> whenVisitedBy(RefactorVisitor<?> visitor) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedBy(visitor);
        }

        public RefactoringAssert<S> whenVisitedBy(Iterable<RefactorVisitor<?>> visitors) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedBy(visitors);
        }

        public RefactoringAssert<S> whenVisitedByMapped(Function<S, RefactorVisitor<? super S>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedByMapped(visitorFunction);
        }

        public RefactoringAssert<S> whenVisitedByMany(Function<S, Iterable<RefactorVisitor<? super S>>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new RefactoringAssert<>(primary(sources), sources).whenVisitedByMany(visitorFunction);
        }

        private S primary(List<S> sources) {
            return sources.stream().filter(s -> s.getSourcePath().equals(primarySource.toString())).findAny()
                    .orElseThrow(() -> new IllegalStateException("unable to find primary source"));
        }
    }

    public static class RefactoringAssert<S extends SourceFile> {
        private final Refactor refactor = new Refactor(true);

        private final S primarySource;
        private final List<S> sources;

        public RefactoringAssert(S primarySource, List<S> sources) {
            this.primarySource = primarySource;
            this.sources = sources;
        }

        public RefactoringAssert<S> whenVisitedBy(RefactorVisitor<?> visitor) {
            refactor.visit(visitor);
            return this;
        }

        public RefactoringAssert<S> whenVisitedBy(Iterable<RefactorVisitor<?>> visitors) {
            refactor.visit(visitors);
            return this;
        }

        public RefactoringAssert<S> whenVisitedByMapped(Function<S, RefactorVisitor<? super S>> visitorFunction) {
            assertThat(sources).withFailMessage("Expected sources to be provided, but none were.").isNotEmpty();
            return whenVisitedBy(visitorFunction.apply(sources.iterator().next()));
        }

        public RefactoringAssert<S> whenVisitedByMany(Function<S, Iterable<RefactorVisitor<? super S>>> visitorFunction) {
            assertThat(sources).withFailMessage("Expected sources to be provided, but none were.").isNotEmpty();
            visitorFunction.apply(sources.iterator().next()).forEach(refactor::visit);
            return this;
        }

        private SourceFile doRefactor() {
            Collection<Change> fixes = refactor.fix(sources);
            assertThat(fixes).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotEmpty();

            return fixes.stream().filter(f -> primarySource.equals(f.getOriginal())).findAny()
                    .map(Change::getFixed)
                    .orElseThrow(() -> new IllegalStateException("unable to find primary source"));
        }

        public RefactoringAssert<S> isRefactoredTo(String expected) {
            SourceFile fixed = doRefactor();
            assertThat(fixed).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotNull();
            assertThat(fixed.printTrimmed()).isEqualTo(StringUtils.trimIndent(expected));

            return this;
        }

        public RefactoringAssert<S> isRefactoredTo(Supplier<String> expected) {
            SourceFile fixed = doRefactor();
            assertThat(fixed).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotNull();
            assertThat(fixed.printTrimmed()).isEqualTo(StringUtils.trimIndent(expected.get()));

            return this;
        }

        public RefactoringAssert<S> isUnchanged() {
            List<String> results = new ArrayList<>();
            for (Change change : refactor.fix(sources)) {
                if(change.getFixed() != null) {
                    results.add(change.getFixed().printTrimmed());
                }
                else {
                    assert change.getOriginal() != null;
                    results.add(change.getOriginal().getSourcePath() + " has been DELETED");
                }
            }

            assertThat(results).isEmpty();
            return this;
        }

        @SuppressWarnings("unchecked")
        public List<S> fixed() {
            return refactor.fix(sources).stream()
                    .map(Change::getFixed)
                    .map(s -> (S) s)
                    .collect(toList());
        }
    }
}
