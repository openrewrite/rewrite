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

import kotlin.Pair;
import kotlin.Unit;
import kotlin.annotation.AnnotationTarget;
import kotlin.jvm.functions.Function1;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtilRt;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.kotlin.com.intellij.psi.FileViewProvider;
import org.jetbrains.kotlin.com.intellij.psi.PsiManager;
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider;
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory;
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector;
import org.jetbrains.kotlin.fir.DependencyListForCliModule;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider;
import org.jetbrains.kotlin.fir.pipeline.*;
import org.jetbrains.kotlin.fir.resolve.ScopeSession;
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator;
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper;
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices;
import org.jetbrains.kotlin.utils.PathUtil;
import org.junit.platform.commons.util.StringUtils;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.kotlin.internal.CompiledSource;
import org.openrewrite.kotlin.internal.KotlinParserVisitor;
import org.openrewrite.kotlin.internal.KotlinSource;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS;
import static org.jetbrains.kotlin.cli.jvm.JvmArgumentsKt.*;
import static org.jetbrains.kotlin.cli.jvm.compiler.pipeline.CompilerPipelineKt.createProjectEnvironment;
import static org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt.*;
import static org.jetbrains.kotlin.config.CommonConfigurationKeys.*;
import static org.jetbrains.kotlin.config.JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT;
import static org.jetbrains.kotlin.incremental.IncrementalFirJvmCompilerRunnerKt.configureBaseRoots;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KotlinParser implements Parser {
    public static final String SKIP_SOURCE_SET_TYPE_GENERATION = "org.openrewrite.kotlin.skipSourceSetTypeGeneration";

    private String sourceSet = "main";

    @Nullable
    private transient JavaSourceSet sourceSetProvenance;

    @Nullable
    private final Collection<Path> classpath;

    private final List<NamedStyles> styles;
    private final boolean logCompilationWarningsAndErrors;
    private final JavaTypeCache typeCache;
    private final String moduleName;
    private final KotlinLanguageLevel languageLevel;
    private final boolean isKotlinScript;

    @Override
    public Stream<SourceFile> parse(@Language("kotlin") String... sources) {
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
        CompiledSource compilerCus;
        try {
            compilerCus = parse(acceptedInputs(sources).collect(Collectors.toList()), disposable, pctx);
        } catch (Exception e) {
            // TODO: associate the compiler exception to a specific source file.
            // https://github.com/openrewrite/rewrite-kotlin/issues/24
            return Stream.empty();
        }

        FirSession firSession = compilerCus.getFirSession();
        return Stream.concat(
                        compilerCus.getSources().stream()
                                .map(kotlinSource -> {
                                    try {
                                        KotlinParserVisitor mappingVisitor = new KotlinParserVisitor(
                                                kotlinSource,
                                                relativeTo,
                                                styles,
                                                typeCache,
                                                firSession,
                                                ctx
                                        );

                                        assert kotlinSource.getFirFile() != null;
                                        SourceFile kcu = (SourceFile) mappingVisitor.visitFile(kotlinSource.getFirFile(), new InMemoryExecutionContext());
                                        parsingListener.parsed(kotlinSource.getInput(), kcu);
                                        return requirePrintEqualsInput(kcu, kotlinSource.getInput(), relativeTo, ctx);
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
                .filter(Objects::nonNull);
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
                sourceSetProvenance = new JavaSourceSet(Tree.randomId(), sourceSet, emptyList());
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

    @SuppressWarnings("unused")
    public static class Builder extends Parser.Builder {
        @Nullable
        private Collection<String> artifactNames = emptyList();
        @Nullable
        private Collection<Path> classpath = emptyList();

        private JavaTypeCache typeCache = new JavaTypeCache();
        private boolean logCompilationWarningsAndErrors;
        private final List<NamedStyles> styles = new ArrayList<>();
        private String moduleName = "main";
        private KotlinLanguageLevel languageLevel = KotlinLanguageLevel.KOTLIN_1_9;
        private boolean isKotlinScript = false;

        public Builder() {
            super(K.CompilationUnit.class);
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

        @Nullable
        private Collection<Path> resolvedClasspath() {
            if (artifactNames != null && !artifactNames.isEmpty()) {
                classpath = JavaParser.dependenciesFromClasspath(artifactNames.toArray(new String[0]));
                artifactNames = null;
            }
            return classpath;
        }

        public KotlinParser build() {
            return new KotlinParser(resolvedClasspath(), styles, logCompilationWarningsAndErrors, typeCache, moduleName, languageLevel, isKotlinScript);
        }

        @Override
        public String getDslName() {
            return "kotlin";
        }

        public KotlinParser.Builder clone() {
            KotlinParser.Builder clone = (KotlinParser.Builder)super.clone();
            clone.typeCache = this.typeCache.clone();
            return clone;
        }
    }

    public CompiledSource parse(List<Parser.Input> sources, Disposable disposable, ExecutionContext ctx) {
        CompilerConfiguration compilerConfiguration = compilerConfiguration();

        if (classpath != null) {
            for (Path path : classpath) {
                File file;
                try {
                    file = path.toFile();
                } catch (UnsupportedOperationException ex) {
                    continue;
                }
                addJvmClasspathRoot(compilerConfiguration, file);
            }
        }
        addJvmClasspathRoot(compilerConfiguration, PathUtil.getResourcePathForClass(AnnotationTarget.class));

        K2JVMCompilerArguments arguments = new K2JVMCompilerArguments();
        configureJdkHome(compilerConfiguration, arguments);
        configureJavaModulesContentRoots(compilerConfiguration, arguments);
        configureAdvancedJvmOptions(compilerConfiguration, arguments);
        configureKlibPaths(compilerConfiguration, arguments);
        configureContentRootsFromClassPath(compilerConfiguration, arguments);
        configureJdkClasspathRoots(compilerConfiguration);
        configureBaseRoots(compilerConfiguration, arguments);

        KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                compilerConfiguration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES);

        List<KtFile> ktFiles = new ArrayList<>(sources.size());

        List<KotlinSource> kotlinSources = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            Parser.Input source = sources.get(i);
            String fileName;

            if ("openRewriteFile.kt".equals(source.getPath().toString())) {
                fileName = "openRewriteFile" + i + ".kt";
            } else if ("openRewriteFile.kts".equals(source.getPath().toString())) {
                fileName = "openRewriteFile" + i + ".kts";
            } else {
                fileName = source.getPath().toString();
            }

            VirtualFile vFile = new LightVirtualFile(fileName, KotlinFileType.INSTANCE, StringUtilRt.convertLineSeparators(source.getSource(ctx).readFully()));
            final FileViewProvider fileViewProvider = new SingleRootFileViewProvider(
                    PsiManager.getInstance(environment.getProject()),
                    vFile
            );
            KtFile file = (KtFile) fileViewProvider.getPsi(KotlinLanguage.INSTANCE);
            assert file != null;
            ktFiles.add(file);
            kotlinSources.add(new KotlinSource(source, file));
        }

        BaseDiagnosticsCollector diagnosticsReporter = DiagnosticReporterFactory.INSTANCE.createReporter(false);
        VfsBasedProjectEnvironment projectEnvironment = createProjectEnvironment(compilerConfiguration, disposable,
                EnvironmentConfigFiles.JVM_CONFIG_FILES, compilerConfiguration.getNotNull(MESSAGE_COLLECTOR_KEY));
        AbstractProjectFileSearchScope sourceScope = projectEnvironment.getSearchScopeByPsiFiles(ktFiles, false);
        sourceScope.plus(projectEnvironment.getSearchScopeForProjectJavaSources());

        AbstractProjectFileSearchScope libraryScope = projectEnvironment.getSearchScopeForProjectLibraries();
        LanguageVersionSettings languageVersionSettings = compilerConfiguration.getNotNull(LANGUAGE_VERSION_SETTINGS);

        FirProjectSessionProvider sessionProvider = new FirProjectSessionProvider();

        Function1<DependencyListForCliModule.Builder, Unit> dependencyListBuilderProvider = builder -> {
            List<File> jvmContentFiles = JvmContentRootsKt.getJvmClasspathRoots(compilerConfiguration);
            List<Path> jvmContentPaths = new ArrayList<>(jvmContentFiles.size());
            for (File jvmContentFile : jvmContentFiles) {
                jvmContentPaths.add(jvmContentFile.toPath());
            }
            builder.dependencies(jvmContentPaths);

            List<File> jvmModularFiles = JvmContentRootsKt.getJvmModularRoots(compilerConfiguration);
            List<Path> jvmModularPaths = new ArrayList<>(jvmModularFiles.size());
            for (File jvmModularFile : jvmModularFiles) {
                jvmModularPaths.add(jvmModularFile.toPath());
            }
            builder.dependencies(jvmModularPaths);
            return Unit.INSTANCE;
        };

        Function1<FirSessionConfigurator, Unit> sessionConfigurator = session -> Unit.INSTANCE;

        FirSession firSession = FirSessionFactoryHelper.INSTANCE.createSessionWithDependencies(
                Name.identifier(moduleName),
                JvmPlatforms.INSTANCE.getUnspecifiedJvmPlatform(),
                JvmPlatformAnalyzerServices.INSTANCE,
                sessionProvider,
                projectEnvironment,
                languageVersionSettings,
                sourceScope,
                libraryScope,
                compilerConfiguration.get(LOOKUP_TRACKER),
                compilerConfiguration.get(ENUM_WHEN_TRACKER),
                null, // Do not incrementally compile
                emptyList(), // Add extension registrars when needed here.
                true,
                dependencyListBuilderProvider,
                sessionConfigurator
        );

        List<FirFile> rawFir = FirUtilsKt.buildFirFromKtFiles(firSession, ktFiles);
        Pair<ScopeSession, List<FirFile>> result = AnalyseKt.runResolution(firSession, rawFir);
        AnalyseKt.runCheckers(firSession, result.getFirst(), result.getSecond(), diagnosticsReporter);
//        ModuleCompilerAnalyzedOutput analyzedOutput = new ModuleCompilerAnalyzedOutput(firSession, result.getFirst(), result.getSecond());
//        FirResult firResult = new FirResult(singletonList(analyzedOutput));
//
//        Fir2IrExtensions extensions = Fir2IrExtensions.Default.INSTANCE;
//        Fir2IrConfiguration irConfiguration = new Fir2IrConfiguration(
//                languageVersionSettings,
//                compilerConfiguration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES),
//                compilerConfiguration.putIfAbsent(EVALUATED_CONST_TRACKER, EvaluatedConstTracker.Companion.create())
//        );
//        // TODO: add generation extensions
//        List<IrGenerationExtension> irGenerationExtensions = new ArrayList<>();
//        Fir2IrActualizedResult actualizedResult = convertToIrAndActualizeForJvm(firResult, extensions, irConfiguration, irGenerationExtensions, diagnosticsReporter);

        assert kotlinSources.size() == result.getSecond().size();
        List<FirFile> second = result.getSecond();
        for (int i = 0; i < second.size(); i++) {
            kotlinSources.get(i).setFirFile(second.get(i));
        }

        return new CompiledSource(firSession, kotlinSources);
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
       KOTLIN_1_9
    }

    private CompilerConfiguration compilerConfiguration() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, moduleName);
        compilerConfiguration.put(MESSAGE_COLLECTOR_KEY, logCompilationWarningsAndErrors ?
                new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true) :
                MessageCollector.Companion.getNONE());

        compilerConfiguration.put(LANGUAGE_VERSION_SETTINGS, new LanguageVersionSettingsImpl(getLanguageVersion(languageLevel), getApiVersion(languageLevel)));

        compilerConfiguration.put(USE_FIR, true);
        compilerConfiguration.put(DO_NOT_CLEAR_BINDING_CONTEXT, true);
        compilerConfiguration.put(ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, true);
        compilerConfiguration.put(INCREMENTAL_COMPILATION, true);

        addJvmSdkRoots(compilerConfiguration, PathUtil.getJdkClassesRootsFromCurrentJre());

        return compilerConfiguration;
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
            default:
                throw new IllegalArgumentException("Unknown language level: " + languageLevel);
        }
    }
}
