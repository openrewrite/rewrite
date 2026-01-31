/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin2;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.fir.FirSession;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.kotlin2.internal.CompiledSource2;
import org.openrewrite.kotlin2.internal.Kotlin2TreeParserVisitor;
import org.openrewrite.kotlin2.internal.PsiElementAssociations2;
import org.openrewrite.kotlin2.tree.Kt;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS;

/**
 * Kotlin 2 parser implementation using the K2 compiler with FIR frontend.
 * This parser leverages the new K2 compiler architecture introduced in Kotlin 2.0,
 * which uses FIR (Frontend Intermediate Representation) instead of the old PSI+BindingContext approach.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Kotlin2Parser implements Parser {
    public static final String SKIP_SOURCE_SET_TYPE_GENERATION = "org.openrewrite.kotlin2.skipSourceSetTypeGeneration";

    private String sourceSet = "main";

    @Nullable
    private transient JavaSourceSet sourceSetProvenance;

    @Nullable
    private final Collection<Path> classpath;

    @Nullable
    private final List<Input> dependsOn;

    private final List<NamedStyles> styles;
    private final boolean logCompilationWarningsAndErrors;
    private final JavaTypeCache typeCache;
    private final String moduleName;
    private final KotlinLanguageLevel languageLevel;
    private final boolean isKotlinScript;

    @Override
    public Stream<SourceFile> parse(@Language("kotlin") String... sources) {
        Pattern packagePattern = Pattern.compile("\\bpackage\\s+([`.\\w]+)");
        Pattern classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

        Function<String, String> simpleName = sourceStr -> {
            Matcher classMatcher = classPattern.matcher(sourceStr);
            return classMatcher.find() ? classMatcher.group(3) : null;
        };

        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> {
                            Matcher packageMatcher = packagePattern.matcher(sourceFile);
                            String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

                            String className = Optional.ofNullable(simpleName.apply(sourceFile))
                                                       .orElse(Long.toString(System.nanoTime())) + ".kt";

                            Path path = Paths.get(pkg + className);
                            return new Input(
                                    path, null,
                                    () -> new ByteArrayInputStream(sourceFile.getBytes(StandardCharsets.UTF_8)),
                                    true
                            );
                        })
                        .collect(toList()),
                null,
                new InMemoryExecutionContext()
        );
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingExecutionContextView pctx = ParsingExecutionContextView.view(ctx);
        ParsingEventListener parsingListener = pctx.getParsingListener();

        Disposable disposable = Disposer.newDisposable();
        CompiledSource2 compilerCus;
        List<Input> acceptedInputs = ListUtils.concatAll(dependsOn, acceptedInputs(sources).collect(toList()));
        try {
            compilerCus = parse(acceptedInputs, disposable, pctx);
        } catch (Exception e) {
            return acceptedInputs.stream().map(input -> ParseError.build(this, input, relativeTo, ctx, e));
        }

        FirSession firSession = compilerCus.getFirSession();
        return Stream.concat(
                        compilerCus.getSources().stream()
                                .map(kotlinSource -> {
                                    try {
                                        assert kotlinSource.getFirFile() != null;
                                        assert kotlinSource.getFirFile().getSource() != null;
                                        PsiElement psi = ((KtRealPsiSourceElement) kotlinSource.getFirFile().getSource()).getPsi();
                                        AnalyzerWithCompilerReport.SyntaxErrorReport report =
                                                AnalyzerWithCompilerReport.Companion.reportSyntaxErrors(psi, new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true));
                                        if (report.isHasErrors()) {
                                            return ParseError.build(Kotlin2Parser.this, kotlinSource.getInput(), relativeTo, ctx, new RuntimeException());
                                        }

                                        Kotlin2TypeMapping typeMapping = new Kotlin2TypeMapping(typeCache, firSession, kotlinSource.getFirFile());
                                        PsiElementAssociations2 associations = new PsiElementAssociations2(typeMapping, kotlinSource.getFirFile());
                                        associations.initialize();
                                        Kotlin2TreeParserVisitor psiParser = new Kotlin2TreeParserVisitor(kotlinSource, associations, styles, relativeTo, ctx);
                                        SourceFile cu = psiParser.parse();

                                        parsingListener.parsed(kotlinSource.getInput(), cu);
                                        return requirePrintEqualsInput(cu, kotlinSource.getInput(), relativeTo, ctx);
                                    } catch (Throwable t) {
                                        ctx.getOnError().accept(t);
                                        return ParseError.build(Kotlin2Parser.this, kotlinSource.getInput(), relativeTo, ctx, t);
                                    }
                                }),
                        compilerCus.getCompiledInputs().stream()
                )
                .map(it -> {
                    if (Boolean.parseBoolean(System.getProperty(SKIP_SOURCE_SET_TYPE_GENERATION, "false"))) {
                        return it;
                    }
                    if (sourceSetProvenance == null) {
                        sourceSetProvenance = new JavaSourceSet(Tree.randomId(), sourceSet, dependsOn == null ? emptyList() :
                                dependsOn.stream().map(i -> i.getRelativePath().toString()).collect(toList()));
                    }
                    return it.withMarkers(it.getMarkers().add(sourceSetProvenance));
                });
    }

    private CompiledSource2 parse(List<Input> acceptedInputs, Disposable disposable, ParsingExecutionContextView pctx) {
        // TODO: Implement K2 compiler configuration and FIR session setup
        // This will be the core integration with the K2 compiler
        throw new UnsupportedOperationException("K2 compiler integration not yet implemented");
    }

    @Override
    public boolean accept(Path path) {
        String filename = path.toString();
        return filename.endsWith(".kt") || filename.endsWith(".kts");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return SourcePathFromSourceTextResolver.determinePath(prefix, sourceCode);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static class Builder extends Parser.Builder {
        @Nullable
        private Collection<String> artifactNames = emptyList();
        @Nullable
        private Collection<Path> classpath = emptyList();
        private List<Input> dependsOn = emptyList();
        private JavaTypeCache typeCache = new JavaTypeCache();
        private boolean logCompilationWarningsAndErrors;
        private final List<NamedStyles> styles = new ArrayList<>();
        private String moduleName = "main";
        private KotlinLanguageLevel languageLevel = KotlinLanguageLevel.LATEST_STABLE;
        private boolean isKotlinScript = false;

        public Builder() {
            super(Kt.CompilationUnit.class);
        }

        public Builder artifactNames(String... artifactNames) {
            this.artifactNames = Arrays.asList(artifactNames);
            return this;
        }

        public Builder classpath(@Nullable Collection<Path> classpath) {
            this.classpath = classpath;
            return this;
        }

        public Builder classpath(@Nullable String... classpath) {
            if (classpath != null) {
                this.classpath = dependenciesFromClasspath(classpath);
            }
            return this;
        }

        public Builder classpathFromResources(ExecutionContext ctx, String... classpath) {
            this.classpath = dependenciesFromClasspath(dependenciesFromResources(ctx, classpath));
            return this;
        }

        public Builder dependsOn(@Nullable Collection<? extends Input> inputs) {
            this.dependsOn = inputs == null ? emptyList() : new ArrayList<>(inputs);
            return this;
        }

        public Builder typeCache(JavaTypeCache typeCache) {
            this.typeCache = typeCache;
            return this;
        }

        public Builder logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return this;
        }

        public Builder styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return this;
        }

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder languageLevel(KotlinLanguageLevel languageLevel) {
            this.languageLevel = languageLevel;
            return this;
        }

        public Builder isKotlinScript(boolean isKotlinScript) {
            this.isKotlinScript = isKotlinScript;
            return this;
        }

        private static List<Path> dependenciesFromClasspath(String... classpath) {
            return dependenciesFromClasspath(Arrays.asList(classpath));
        }

        private static List<Path> dependenciesFromClasspath(Iterable<String> classpath) {
            List<Path> dependencies = new ArrayList<>();
            for (String c : classpath) {
                dependencies.add(Paths.get(c));
            }
            return dependencies;
        }

        @Override
        public Kotlin2Parser build() {
            if (artifactNames != null && !artifactNames.isEmpty()) {
                for (String artifactName : artifactNames) {
                    classpath = new ArrayList<>(classpath == null ? emptyList() : classpath);
                    classpath.add(JavaParser.artifactClasspath(artifactName));
                }
            }
            return new Kotlin2Parser(classpath, dependsOn, styles, logCompilationWarningsAndErrors, typeCache, 
                                     moduleName, languageLevel, isKotlinScript);
        }

        @Override
        public String getDslName() {
            return "kotlin2";
        }
    }

    static class SourcePathFromSourceTextResolver {
        private static final Pattern packagePattern = Pattern.compile("^package\\s+([\\w.]+)");
        private static final Pattern classPattern = Pattern.compile("(class|interface|enum|object)\\s+(\\w+)");

        public static Path determinePath(Path prefix, String sourceCode) {
            Matcher packageMatcher = packagePattern.matcher(sourceCode);
            String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

            Matcher classMatcher = classPattern.matcher(sourceCode);
            String className = classMatcher.find() ? classMatcher.group(2) : "Unknown";

            return prefix.resolve(pkg + className + ".kt");
        }
    }
}
