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
import java.util.function.Consumer;
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

        public EvalAssert<S> whenVisitedBy(EvalVisitor<?> visitor) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new EvalAssert<>(primary(sources), sources).whenVisitedBy(visitor);
        }

        public EvalAssert<S> whenVisitedBy(Iterable<EvalVisitor<?>> visitors) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new EvalAssert<>(primary(sources), sources).whenVisitedBy(visitors);
        }

        public EvalAssert<S> whenVisitedByMapped(Function<S, EvalVisitor<? super S>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new EvalAssert<>(primary(sources), sources).whenVisitedByMapped(visitorFunction);
        }

        public EvalAssert<S> whenVisitedByMany(Function<S, Iterable<EvalVisitor<? super S>>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles.toArray(new String[0]));
            return new EvalAssert<>(primary(sources), sources).whenVisitedByMany(visitorFunction);
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

        public EvalAssert<S> whenVisitedBy(EvalVisitor<?> visitor) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new EvalAssert<>(primary(sources), sources).whenVisitedBy(visitor);
        }

        public EvalAssert<S> whenVisitedBy(Iterable<EvalVisitor<?>> visitors) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new EvalAssert<>(primary(sources), sources).whenVisitedBy(visitors);
        }

        public EvalAssert<S> whenVisitedByMapped(Function<S, EvalVisitor<? super S>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new EvalAssert<>(primary(sources), sources).whenVisitedByMapped(visitorFunction);
        }

        public EvalAssert<S> whenVisitedByMany(Function<S, Iterable<EvalVisitor<? super S>>> visitorFunction) {
            List<S> sources = parser.parse(sourceFiles, relativeTo);
            return new EvalAssert<>(primary(sources), sources).whenVisitedByMany(visitorFunction);
        }

        private S primary(List<S> sources) {
            return sources.stream().filter(s -> s.getSourcePath().equals(primarySource.toUri().toString())).findAny()
                    .orElseThrow(() -> new IllegalStateException("unable to find primary source"));
        }
    }

    public static class EvalAssert<S extends SourceFile> {
        private final Eval.Builder eval = Eval.builder().eagerlyThrow(true);

        private final S primarySource;
        private final List<S> sources;

        public EvalAssert(S primarySource, List<S> sources) {
            this.primarySource = primarySource;
            this.sources = sources;
        }

        public EvalAssert<S> whenVisitedBy(EvalVisitor<?> visitor) {
            eval.visit(visitor);
            return this;
        }

        public EvalAssert<S> whenVisitedBy(Iterable<EvalVisitor<?>> visitors) {
            eval.visit(visitors);
            return this;
        }

        public EvalAssert<S> whenVisitedByMapped(Function<S, EvalVisitor<? super S>> visitorFunction) {
            assertThat(sources).withFailMessage("Expected sources to be provided, but none were.").isNotEmpty();
            return whenVisitedBy(visitorFunction.apply(sources.iterator().next()));
        }

        public EvalAssert<S> whenVisitedByMany(Function<S, Iterable<EvalVisitor<? super S>>> visitorFunction) {
            assertThat(sources).withFailMessage("Expected sources to be provided, but none were.").isNotEmpty();
            visitorFunction.apply(sources.iterator().next()).forEach(eval::visit);
            return this;
        }

        private SourceFile doEval() {
            Collection<Result> fixes = eval.build().visit(sources);
            assertThat(fixes).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotEmpty();

            return fixes.stream().filter(f -> primarySource.equals(f.getBefore())).findAny()
                    .map(Result::getAfter)
                    .orElseThrow(() -> new IllegalStateException("unable to find primary source"));
        }

        public EvalAssert<S> isRefactoredTo(String expected) {
            return isRefactoredTo(expected, s -> {
            });
        }

        @SuppressWarnings("unchecked")
        public EvalAssert<S> isRefactoredTo(String expected, Consumer<S> conditions) {
            S fixed = (S) doEval();
            assertThat(fixed).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotNull();
            assertThat(fixed.printTrimmed()).isEqualTo(StringUtils.trimIndent(expected));
            conditions.accept(fixed);
            return this;
        }

        public EvalAssert<S> isRefactoredTo(Supplier<String> expected) {
            return isRefactoredTo(expected, s -> {});
        }

        @SuppressWarnings("unchecked")
        public EvalAssert<S> isRefactoredTo(Supplier<String> expected, Consumer<S> conditions) {
            S fixed = (S) doEval();
            assertThat(fixed).withFailMessage("Expecting refactoring visitor to make changes to source file, but none were made.").isNotNull();
            assertThat(fixed.printTrimmed()).isEqualTo(StringUtils.trimIndent(expected.get()));

            return this;
        }

        public EvalAssert<S> isUnchanged() {
            List<String> results = new ArrayList<>();
            for (Result result : eval.build().visit(sources)) {
                if (result.getAfter() != null) {
                    results.add(result.getAfter().printTrimmed());
                } else {
                    assert result.getBefore() != null;
                    results.add(result.getBefore().getSourcePath() + " has been DELETED");
                }
            }

            assertThat(results).isEmpty();
            return this;
        }

        @SuppressWarnings("unchecked")
        public List<S> results() {
            return eval.build().visit(sources).stream()
                    .map(Result::getAfter)
                    .map(s -> (S) s)
                    .collect(toList());
        }
    }
}
