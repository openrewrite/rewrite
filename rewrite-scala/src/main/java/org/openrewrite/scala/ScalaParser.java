/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.DefaultJavaTypeFactory;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.scala.internal.ScalaCompilerContext;
import org.openrewrite.scala.tree.S;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.internal.EncodingDetectingInputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ScalaParser implements Parser {
    private final @Nullable Collection<Path> classpath;

    private final boolean logCompilationWarningsAndErrors;
    private final JavaTypeCache typeCache;
    private final JavaTypeFactory typeFactory;

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\s+([.\\w]+)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(class|object|trait|case\\s+class)\\s*(<[^>]*>)?\\s+(\\w+)");

    private static String derivedRelativePath(String sourceCode) {
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(sourceCode);
        String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";

        Matcher classMatcher = CLASS_PATTERN.matcher(sourceCode);
        String simpleName = classMatcher.find() ? classMatcher.group(3) : Long.toString(System.nanoTime());

        return pkg + simpleName + ".scala";
    }

    @Override
    public Stream<SourceFile> parse(@Language("scala") String... sources) {
        return parseInputs(
                Arrays.stream(sources)
                        .map(sourceFile -> {
                            Path path = Paths.get(derivedRelativePath(sourceFile));
                            return Input.fromString(path, sourceFile);
                        })
                        .collect(Collectors.toList()),
                null,
                new InMemoryExecutionContext()
        );
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingExecutionContextView pctx = ParsingExecutionContextView.view(ctx);

        // Initialize the Scala compiler context
        ScalaCompilerContext compilerContext = new ScalaCompilerContext(
            classpath,
            logCompilationWarningsAndErrors,
            ctx
        );

        // Collect all inputs eagerly for batch compilation
        List<Input> inputList = new ArrayList<>();
        for (Input input : sources) {
            inputList.add(input);
        }

        // Batch compile all sources with type checking
        Map<String, ScalaCompilerContext.ParseResult> compiledResults = compilerContext.compileAll(inputList);

        // Map results back to SourceFiles
        return inputList.stream()
                .map(input -> {
                    Path path = input.getRelativePath(relativeTo);
                    pctx.getParsingListener().startedParsing(input);

                    try {
                        ScalaCompilerContext.ParseResult parseResult = compiledResults.get(input.getPath().toString());

                        // Fall back to single-file parse if batch didn't include this file
                        if (parseResult == null) {
                            parseResult = compilerContext.parse(input);
                        }

                        // Convert the Scala AST to OpenRewrite's LST
                        EncodingDetectingInputStream source = input.getSource(ctx);
                        String sourceStr = source.readFully();
                        ScalaParserVisitor visitor = new ScalaParserVisitor(
                            path,
                            input.getFileAttributes(),
                            sourceStr,
                            source.getCharset(),
                            source.isCharsetBomMarked(),
                            typeCache,
                            typeFactory,
                            ctx,
                            parseResult.getParseResult()
                        );

                        S.CompilationUnit cu = visitor.visitCompilationUnit(parseResult.getParseResult());

                        // Add any parse warnings as markers
                        if (!parseResult.getWarnings().isEmpty()) {
                            for (ParseWarning warning : parseResult.getWarnings()) {
                                cu = cu.withMarkers(cu.getMarkers().add(warning));
                            }
                        }

                        pctx.getParsingListener().parsed(input, cu);
                        return requirePrintEqualsInput(cu, input, relativeTo, ctx);

                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                        return ParseError.build(this, input, relativeTo, ctx, t);
                    }
                });
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".scala") || path.toString().endsWith(".sc");
    }

    @Override
    public ScalaParser reset() {
        typeCache.clear();
        return this;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve(derivedRelativePath(sourceCode));
    }

    public static ScalaParser.Builder builder() {
        return new Builder();
    }

    public static ScalaParser.Builder builder(Builder base) {
        return new Builder(base);
    }

    @SuppressWarnings("unused")
    public static class Builder extends Parser.Builder {
        private @Nullable Collection<Path> classpath = Collections.emptyList();

        protected @Nullable Collection<String> artifactNames = Collections.emptyList();

        private JavaTypeCache typeCache = new JavaTypeCache();

        @Nullable
        private JavaTypeFactory typeFactory;

        private JavaTypeFactory.@Nullable Provider typeFactoryProvider;

        private boolean logCompilationWarningsAndErrors = false;
        private final List<NamedStyles> styles = new ArrayList<>();

        public Builder() {
            super(S.CompilationUnit.class);
        }

        public Builder(Builder base) {
            super(S.CompilationUnit.class);
            this.classpath = base.classpath;
            this.artifactNames = base.artifactNames;
            this.typeCache = base.typeCache;
            this.typeFactory = base.typeFactory;
            this.typeFactoryProvider = base.typeFactoryProvider;
            this.logCompilationWarningsAndErrors = base.logCompilationWarningsAndErrors;
            this.styles.addAll(base.styles);
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

        public Builder classpath(String... artifactNames) {
            this.artifactNames = Arrays.asList(artifactNames);
            this.classpath = null;
            return this;
        }

        public Builder classpathFromResource(ExecutionContext ctx, String... artifactNamesWithVersions) {
            this.artifactNames = null;
            this.classpath = JavaParser.dependenciesFromResources(ctx, artifactNamesWithVersions);
            return this;
        }

        /**
         * This is an internal API which is subject to removal or change.
         */
        public Builder addClasspathEntry(Path entry) {
            if (classpath.isEmpty()) {
                classpath = Collections.singletonList(entry);
            } else if (!classpath.contains(entry)) {
                classpath = new ArrayList<>(classpath);
                classpath.add(entry);
            }
            return this;
        }

        /**
         * @deprecated Configure a {@link JavaTypeFactory} via {@link #typeFactory} or
         * {@link #typeFactoryProvider} instead. The cache becomes an implementation
         * detail of the default {@link DefaultJavaTypeFactory}.
         */
        @Deprecated
        @SuppressWarnings("unused")
        public Builder typeCache(JavaTypeCache typeCache) {
            this.typeCache = typeCache;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder typeFactory(JavaTypeFactory typeFactory) {
            this.typeFactory = typeFactory;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder typeFactoryProvider(JavaTypeFactory.Provider provider) {
            this.typeFactoryProvider = provider;
            return this;
        }

        public Builder styles(Iterable<? extends NamedStyles> styles) {
            for (NamedStyles style : styles) {
                this.styles.add(style);
            }
            return this;
        }

        private @Nullable Collection<Path> resolvedClasspath() {
            if (artifactNames != null && !artifactNames.isEmpty()) {
                classpath = JavaParser.dependenciesFromClasspath(artifactNames.toArray(new String[0]));
                artifactNames = null;
            }
            return classpath;
        }

        @Override
        public ScalaParser build() {
            Collection<Path> cp = resolvedClasspath();
            JavaTypeFactory factory = typeFactory;
            if (factory == null && typeFactoryProvider != null) {
                factory = typeFactoryProvider.create(cp == null ? new ArrayList<>() : new ArrayList<>(cp), null);
            }
            if (factory == null) {
                factory = new DefaultJavaTypeFactory(typeCache);
            }
            return new ScalaParser(cp, logCompilationWarningsAndErrors, typeCache, factory);
        }

        @Override
        public String getDslName() {
            return "scala";
        }

        @Override
        public Builder clone() {
            return new Builder(this);
        }
    }
}
