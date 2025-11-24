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
package org.openrewrite.kotlin;

import kotlin.annotation.AnnotationTarget;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.backend.Fir2IrComponentsStorage;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.incremental.CompilerRunnerUtils;
import org.jetbrains.kotlin.ir.declarations.IrFile;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.kotlin.internal.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS;
import static org.openrewrite.kotlin.KotlinParser.SourcePathFromSourceTextResolver.determinePath;

@SuppressWarnings("CommentedOutCode")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KotlinParser implements Parser {
    public static final String SKIP_SOURCE_SET_TYPE_GENERATION = "org.openrewrite.kotlin.skipSourceSetTypeGeneration";

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

        // TODO: FIR and disposable may not be necessary using the IR.
        Disposable disposable = Disposer.newDisposable();
        CompiledSource compilerCus;
        List<Input> acceptedInputs = ListUtils.concatAll(dependsOn, acceptedInputs(sources).collect(toList()));
        try {
            compilerCus = parse(acceptedInputs, disposable, pctx);
        } catch (Exception e) {
            disposable.dispose();
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
                                            return ParseError.build(KotlinParser.this, kotlinSource.getInput(), relativeTo, ctx, new RuntimeException());
                                        }

                                        KotlinTypeMapping typeMapping = new KotlinTypeMapping(typeCache, firSession, kotlinSource.getFirFile());
                                        PsiElementAssociations associations = new PsiElementAssociations(typeMapping, kotlinSource.getFirFile());
                                        associations.initialize();
                                        KotlinTreeParserVisitor psiParser = new KotlinTreeParserVisitor(kotlinSource, associations, styles, relativeTo, ctx);
                                        SourceFile cu = psiParser.parse();

                                        parsingListener.parsed(kotlinSource.getInput(), cu);
                                        return requirePrintEqualsInput(cu, kotlinSource.getInput(), relativeTo, ctx);
                                    } catch (Throwable t) {
                                        ctx.getOnError().accept(t);
                                        return ParseError.build(this, kotlinSource.getInput(), relativeTo, ctx, t);
                                    }
                                }),
                        Stream.generate(() -> {
                                    // The disposable should be disposed of exactly once after all sources have been parsed
                                    Disposer.dispose(disposable);
                                    return (SourceFile) null;
                                })
                                .limit(1))
                .filter(Objects::nonNull)
                .filter(source -> {
                    return !source.getSourcePath().getFileName().toString().startsWith("dependsOn-");
                });
    }

    @Override
    public boolean accept(Path path) {
        String p = path.toString();
        return p.endsWith(".kt") || p.endsWith(".kts");
    }

    @Override
    public KotlinParser reset() {
        typeCache.clear();
        return this;
    }

    @Deprecated//(since = "0.4.0", forRemoval = true)
    public void setSourceSet(String sourceSet) {
        this.sourceSetProvenance = null;
        this.sourceSet = sourceSet;
    }

    @Deprecated//(since = "0.4.0", forRemoval = true)
    public JavaSourceSet getSourceSet(ExecutionContext ctx) {
        if (sourceSetProvenance == null) {
            if (ctx.getMessage(SKIP_SOURCE_SET_TYPE_GENERATION, false)) {
                sourceSetProvenance = new JavaSourceSet(Tree.randomId(), sourceSet, emptyList(), emptyMap());
            } else {
                sourceSetProvenance = JavaSourceSet.build(sourceSet, classpath == null ? emptyList() : classpath,
                        typeCache, false);
            }
        }
        return sourceSetProvenance;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve(isKotlinScript ? "openRewriteFile.kts" : "openRewriteFile.kt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Builder base) {
        return new Builder(base);
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
        private KotlinLanguageLevel languageLevel = KotlinLanguageLevel.KOTLIN_1_9;
        private boolean isKotlinScript = false;

        public Builder() {
            super(K.CompilationUnit.class);
        }

        public Builder(Builder base) {
            super(K.CompilationUnit.class);
            this.classpath = base.classpath;
            this.artifactNames = base.artifactNames;
            this.typeCache = base.typeCache;
            this.logCompilationWarningsAndErrors = base.logCompilationWarningsAndErrors;
            this.styles.addAll(base.styles);
        }

        public Builder logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return this;
        }

        public Builder isKotlinScript(boolean isKotlinScript) {
            this.isKotlinScript = isKotlinScript;
            return this;
        }

        public Builder classpath(Collection<Path> classpath) {
            this.artifactNames = null;
            this.classpath = classpath;
            return this;
        }

        public Builder classpath(String... artifactNames) {
            this.artifactNames = Arrays.asList(artifactNames);
            this.classpath = null;
            return this;
        }

        public Builder classpathFromResources(ExecutionContext ctx, String... classpath) {
            this.artifactNames = null;
            this.classpath = JavaParser.dependenciesFromResources(ctx, classpath);
            return this;
        }

        /**
         * This is an internal API which is subject to removal or change.
         */
        public Builder addClasspathEntry(Path entry) {
            if (classpath.isEmpty()) {
                classpath = singletonList(entry);
            } else if (!classpath.contains(entry)) {
                classpath = new ArrayList<>(classpath);
                classpath.add(entry);
            }
            return this;
        }

        public Builder dependsOn(@Language("kotlin") String... inputsAsStrings) {
            this.dependsOn = Arrays.stream(inputsAsStrings)
                    .map(input -> Input.fromString(determinePath("dependsOn-", input), input))
                    .collect(toList());
            return this;
        }

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

        public Builder moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder languageLevel(KotlinLanguageLevel languageLevel) {
            this.languageLevel = languageLevel;
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
        public KotlinParser build() {
            return new KotlinParser(resolvedClasspath(), dependsOn, styles, logCompilationWarningsAndErrors, typeCache, moduleName, languageLevel, isKotlinScript);
        }

        @Override
        public String getDslName() {
            return "kotlin";
        }

        @Override
        public KotlinParser.Builder clone() {
            KotlinParser.Builder clone = (KotlinParser.Builder) super.clone();
            clone.typeCache = this.typeCache.clone();
            return clone;
        }
    }

    static Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("rewrite");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: Can we use VirtualFiles if the Compiler needs actual file paths?
    public static String tempFile(ExecutionContext ctx, Parser.Input source, int idx) {
        String fileName;
        if ("openRewriteFile.kt".equals(source.getPath().toString())) {
            fileName = "openRewriteFile" + idx + ".kt";
        } else if ("openRewriteFile.kts".equals(source.getPath().toString())) {
            fileName = "openRewriteFile" + idx + ".kts";
        } else {
            fileName = source.getPath().toString();
        }
        Path tempFile = tempDir.resolve(fileName);
        try {
            Files.createDirectories(tempFile.getParent());
        } catch (FileAlreadyExistsException e) {
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        String sourceText = source.getSource(ctx).readFully();
        try {
            Files.write(tempFile, StringUtilRt.convertLineSeparators(sourceText).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tempFile.toAbsolutePath().toString();
    }

    private K2JVMCompilerArguments k2compilerArgs(ExecutionContext ctx, List<Parser.Input> sources){
        K2JVMCompilerArguments arguments = new K2JVMCompilerArguments();
        arguments.setModuleName(moduleName);
        arguments.setLanguageVersion(getLanguageVersion(languageLevel).getVersionString());
        // Use incremental compilation, not light-tree
        arguments.setUseFirLT(false);
        arguments.setUseFirIC(true);
        arguments.setDoNotClearBindingContext(true);
        arguments.setAllowAnyScriptsInSourceRoots(true);
        arguments.setIncrementalCompilation(true);
        arguments.setLinkViaSignatures(true);
        // TODO: some tests' code dont compile, e.g. because of missing return statements etc.
        //   -> are they intended to work like that?
//        arguments.setSuppressedDiagnostics(new String[]{"NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY"});

        if (classpath != null) {
            List<File> classpathFiles = new ArrayList<>();
            for (Path path : classpath) {
                try {
                    classpathFiles.add(path.toFile());
                } catch (UnsupportedOperationException ex) {
                }
            }
            classpathFiles.add(PathUtil.getResourcePathForClass(AnnotationTarget.class));
            CompilerRunnerUtils.setClasspathAsList(arguments,classpathFiles);
        }
        AtomicInteger idx = new AtomicInteger(0);
        arguments.setFreeArgs(sources.stream()
                        .map(source -> tempFile(ctx, source, idx.getAndIncrement())).collect(Collectors.toList()));
        return arguments;
    }

    public CompiledSource parse(List<Parser.Input> sources, Disposable disposable, ExecutionContext ctx) {
        List<KotlinSource> kotlinSources = new ArrayList<>(sources.size());

        MessageCollector messageCollector = logCompilationWarningsAndErrors ?
                new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true) :
                MessageCollector.Companion.getNONE();

        JvmFir2IrPipeline kotlinToIrCompiler = new JvmFir2IrPipeline();
        JvmFir2IrPipelineArtifact jvmFir2IrPipelineArtifact = kotlinToIrCompiler
                .execute(k2compilerArgs(ctx, sources), Services.EMPTY, messageCollector, disposable);
        assert jvmFir2IrPipelineArtifact != null;
        List<IrFile> irFiles = jvmFir2IrPipelineArtifact.getResult().getIrModuleFragment().getFiles();
        List<FirFile> firFiles = ((Fir2IrComponentsStorage) jvmFir2IrPipelineArtifact.getResult().getComponents()).getFir();
        assert irFiles.size() == sources.size();
        assert firFiles.size() == sources.size();

        for (int i = 0; i < sources.size(); i++) {
            Input source = sources.get(i);
            String sourceText = source.getSource(ctx).readFully();
            List<Integer> cRLFLocations = getCRLFLocations(sourceText);
            KotlinSource kotlinSource = new KotlinSource(source, cRLFLocations);
            kotlinSource.setFirFile(firFiles.get(i));
            kotlinSource.setIrFile(irFiles.get(i));
            kotlinSources.add(kotlinSource);
            System.out.println(PsiTreePrinter.printIrFile(irFiles.get(i)));
        }
        return new CompiledSource(jvmFir2IrPipelineArtifact.getResult().getComponents().getSession(), kotlinSources);
    }

    public enum KotlinLanguageLevel {
        KOTLIN_1_0,
        KOTLIN_1_1,
        KOTLIN_1_2,
        KOTLIN_1_3,
        KOTLIN_1_4,
        KOTLIN_1_5,
        KOTLIN_1_6,
        KOTLIN_1_7,
        KOTLIN_1_8,
        KOTLIN_1_9,
        KOTLIN_2_0,
    }

    private LanguageVersion getLanguageVersion(KotlinLanguageLevel languageLevel) {
        switch (languageLevel) {
            case KOTLIN_1_0:
                return LanguageVersion.KOTLIN_1_0;
            case KOTLIN_1_1:
                return LanguageVersion.KOTLIN_1_1;
            case KOTLIN_1_2:
                return LanguageVersion.KOTLIN_1_2;
            case KOTLIN_1_3:
                return LanguageVersion.KOTLIN_1_3;
            case KOTLIN_1_4:
                return LanguageVersion.KOTLIN_1_4;
            case KOTLIN_1_5:
                return LanguageVersion.KOTLIN_1_5;
            case KOTLIN_1_6:
                return LanguageVersion.KOTLIN_1_6;
            case KOTLIN_1_7:
                return LanguageVersion.KOTLIN_1_7;
            case KOTLIN_1_8:
                return LanguageVersion.KOTLIN_1_8;
            case KOTLIN_1_9:
                return LanguageVersion.KOTLIN_1_9;
            case KOTLIN_2_0:
                return LanguageVersion.KOTLIN_2_0;
            default:
                throw new IllegalArgumentException("Unknown language level: " + languageLevel);
        }
    }

    private ApiVersion getApiVersion(KotlinLanguageLevel languageLevel) {
        switch (languageLevel) {
            case KOTLIN_1_0:
                return ApiVersion.KOTLIN_1_0;
            case KOTLIN_1_1:
                return ApiVersion.KOTLIN_1_1;
            case KOTLIN_1_2:
                return ApiVersion.KOTLIN_1_2;
            case KOTLIN_1_3:
                return ApiVersion.KOTLIN_1_3;
            case KOTLIN_1_4:
                return ApiVersion.KOTLIN_1_4;
            case KOTLIN_1_5:
                return ApiVersion.KOTLIN_1_5;
            case KOTLIN_1_6:
                return ApiVersion.KOTLIN_1_6;
            case KOTLIN_1_7:
                return ApiVersion.KOTLIN_1_7;
            case KOTLIN_1_8:
                return ApiVersion.KOTLIN_1_8;
            case KOTLIN_1_9:
                return ApiVersion.KOTLIN_1_9;
            case KOTLIN_2_0:
                return ApiVersion.KOTLIN_2_0;
            default:
                throw new IllegalArgumentException("Unknown language level: " + languageLevel);
        }
    }

    private List<Integer> getCRLFLocations(String source) {
        if (source.isEmpty()) {
            return emptyList();
        }
        List<Integer> cRLFIndices = new ArrayList<>();
        int pos = 0;
        for (int i = 0; i < source.length(); i++) {
            char currentChar = source.charAt(i);
            if (currentChar == '\r') {
                // Check if the next character is '\n' (CRLF)
                if (i + 1 < source.length() && source.charAt(i + 1) == '\n') {
                    cRLFIndices.add(pos);
                    i++; // Skip the next character ('\n')
                }
            }
            pos++;
        }

        return cRLFIndices;
    }

    static class SourcePathFromSourceTextResolver {
        private static final Pattern packagePattern = Pattern.compile("^package\\s+(\\S+)");
        private static final Pattern classPattern = Pattern.compile("(class|interface|enum class)\\s*(<[^>]*>)?\\s+(\\w+)");
        private static final Pattern publicClassPattern = Pattern.compile("public\\s+" + classPattern.pattern());

        private static Optional<String> matchClassPattern(Pattern pattern, String source) {
            Matcher classMatcher = pattern.matcher(source);
            if (classMatcher.find()) {
                return Optional.of(classMatcher.group(3));
            }
            return Optional.empty();
        }

        static Path determinePath(String prefix, String sourceCode) {
            String className = matchClassPattern(publicClassPattern, sourceCode)
                    .orElseGet(() -> matchClassPattern(classPattern, sourceCode)
                            .orElse(Long.toString(System.nanoTime())));
            Matcher packageMatcher = packagePattern.matcher(sourceCode);
            String pkg = packageMatcher.find() ? packageMatcher.group(1).replace('.', '/') + "/" : "";
            return Paths.get(pkg, prefix + className + ".kt");
        }
    }
}
