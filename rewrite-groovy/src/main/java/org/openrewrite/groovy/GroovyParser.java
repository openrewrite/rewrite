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
package org.openrewrite.groovy;

import groovy.lang.GroovyClassLoader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.io.InputStreamReaderSource;
import org.codehaus.groovy.control.messages.WarningMessage;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GroovyParser implements Parser {
    @Nullable
    private final Collection<Path> classpath;

    private final List<NamedStyles> styles;
    private final boolean logCompilationWarningsAndErrors;
    private final JavaTypeCache typeCache;
    private final List<Consumer<CompilerConfiguration>> compilerCustomizers;

    @Override
    public Stream<SourceFile> parse(@Language("groovy") String... sources) {
        Pattern packagePattern = Pattern.compile("^package\\s+([^;]+);");
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
                                                       .orElse(Long.toString(System.nanoTime())) + ".java";

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
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.setTolerance(Integer.MAX_VALUE);
        configuration.setWarningLevel(WarningMessage.NONE);
        configuration.setClasspathList(classpath == null ? emptyList() : classpath.stream()
                .flatMap(cp -> {
                    try {
                        return Stream.of(cp.toFile().toString());
                    } catch (UnsupportedOperationException e) {
                        // can happen e.g. in the case of jdk.internal.jrtfs.JrtPath
                        return Stream.empty();
                    }
                })
                .collect(toList()));
        for (Consumer<CompilerConfiguration> compilerCustomizer : compilerCustomizers) {
            compilerCustomizer.accept(configuration);
        }

        ParsingExecutionContextView pctx = ParsingExecutionContextView.view(ctx);
        return StreamSupport.stream(sources.spliterator(), false)
                .map(input -> {
                    ParseWarningCollector errorCollector = new ParseWarningCollector(configuration, this);
                    try (GroovyClassLoader classLoader = new GroovyClassLoader(getClass().getClassLoader(), configuration, true)) {
                        SourceUnit unit = new SourceUnit(
                                "doesntmatter",
                                new InputStreamReaderSource(input.getSource(ctx), configuration),
                                configuration,
                                classLoader,
                                errorCollector
                        );

                        pctx.getParsingListener().startedParsing(input);
                        CompilationUnit compUnit = new CompilationUnit(configuration, null, classLoader, classLoader);
                        compUnit.addSource(unit);
                        compUnit.compile(Phases.CANONICALIZATION);
                        ModuleNode ast = unit.getAST();

                        for (ClassNode aClass : ast.getClasses()) {
                            try {
                                StaticTypeCheckingVisitor staticTypeCheckingVisitor = new StaticTypeCheckingVisitor(unit, aClass);
                                staticTypeCheckingVisitor.setCompilationUnit(compUnit);
                                staticTypeCheckingVisitor.visitClass(aClass);
                            } catch (NoClassDefFoundError ignored) {
                            }
                        }

                        CompiledGroovySource compiled = new CompiledGroovySource(input, unit, ast);
                        List<ParseWarning> warnings = errorCollector.getWarningMarkers();
                        GroovyParserVisitor mappingVisitor = new GroovyParserVisitor(
                                compiled.getInput().getRelativePath(relativeTo),
                                compiled.getInput().getFileAttributes(),
                                compiled.getInput().getSource(ctx),
                                typeCache,
                                ctx
                        );
                        G.CompilationUnit gcu = mappingVisitor.visit(compiled.getSourceUnit(), compiled.getModule());
                        if (warnings.size() > 0) {
                            Markers m = gcu.getMarkers();
                            for (ParseWarning warning : warnings) {
                                m = m.add(warning);
                            }
                            gcu = gcu.withMarkers(m);
                        }
                        pctx.getParsingListener().parsed(compiled.getInput(), gcu);
                        return requirePrintEqualsInput(gcu, input, relativeTo, ctx);
                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                        return ParseError.build(this, input, relativeTo, ctx, t);
                    } finally {
                        if (logCompilationWarningsAndErrors && (errorCollector.hasErrors() || errorCollector.hasWarnings())) {
                            try (StringWriter sw = new StringWriter();
                                 PrintWriter pw = new PrintWriter(sw)) {
                                errorCollector.write(pw, new Janitor());
                                org.slf4j.LoggerFactory.getLogger(GroovyParser.class).warn(sw.toString());
                            } catch (IOException ignored) {
                                // unreachable
                            }
                        }
                    }
                });
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".groovy") ||
               path.toFile().getName().startsWith("Jenkinsfile");
    }

    @Override
    public GroovyParser reset() {
        typeCache.clear();
        return this;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.groovy");
    }

    public static GroovyParser.Builder builder() {
        return new Builder();
    }

    public static GroovyParser.Builder builder(Builder base) {
        return new Builder(base);
    }

    @SuppressWarnings("unused")
    public static class Builder extends Parser.Builder {
        @Nullable
        private Collection<Path> classpath = Collections.emptyList();
        @Nullable
        protected Collection<String> artifactNames = Collections.emptyList();

        private JavaTypeCache typeCache = new JavaTypeCache();
        private boolean logCompilationWarningsAndErrors = false;
        private final List<NamedStyles> styles = new ArrayList<>();
        private final List<Consumer<CompilerConfiguration>> compilerCustomizers = new ArrayList<>();

        public Builder() {
            super(G.CompilationUnit.class);
        }

        public Builder(Builder base) {
            super(G.CompilationUnit.class);
            this.classpath = base.classpath;
            this.artifactNames = base.artifactNames;
            this.typeCache = base.typeCache;
            this.logCompilationWarningsAndErrors = base.logCompilationWarningsAndErrors;
            this.styles.addAll(base.styles);
            this.compilerCustomizers.addAll(base.compilerCustomizers);
        }

        public Builder logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return this;
        }

        public Builder classpath(@Nullable Collection<Path> classpath) {
            this.artifactNames = null;
            this.classpath = classpath;
            return this;
        }

        public Builder classpath(@Nullable String... artifactNames) {
            this.artifactNames = Arrays.asList(artifactNames);
            this.classpath = null;
            return this;
        }

        public Builder classpathFromResource(ExecutionContext ctx, String... artifactNamesWithVersions) {
            this.artifactNames = null;
            this.classpath = JavaParser.dependenciesFromResources(ctx, artifactNamesWithVersions);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder typeCache(JavaTypeCache typeCache) {
            this.typeCache = typeCache;
            return this;
        }

        public Builder styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return this;
        }

        @SafeVarargs
        public final GroovyParser.Builder compilerCustomizers(Consumer<CompilerConfiguration>... compilerCustomizers) {
            return compilerCustomizers(Arrays.asList(compilerCustomizers));
        }

        public GroovyParser.Builder compilerCustomizers(Iterable<Consumer<CompilerConfiguration>> compilerCustomizers) {
            for (Consumer<CompilerConfiguration> compilerCustomizer : compilerCustomizers) {
                this.compilerCustomizers.add(compilerCustomizer);
            }
            return this;
        }

        @Nullable
        private Collection<Path> resolvedClasspath() {
            if (artifactNames != null && !artifactNames.isEmpty()) {
                classpath = JavaParser.dependenciesFromClasspath(artifactNames.toArray(new String[0]));
                artifactNames = null;
            }
            return classpath;
        }

        public GroovyParser build() {
            return new GroovyParser(resolvedClasspath(), styles, logCompilationWarningsAndErrors, typeCache, compilerCustomizers);
        }

        @Override
        public String getDslName() {
            return "groovy";
        }
    }
}
