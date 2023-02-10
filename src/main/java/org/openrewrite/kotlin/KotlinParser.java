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

import kotlin.Unit;
import kotlin.annotation.AnnotationTarget;
import kotlin.jvm.functions.Function2;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.KtSourceFile;
import org.jetbrains.kotlin.KtVirtualFileSourceFile;
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.config.ContentRoot;
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerAnalyzedOutput;
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerInput;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory;
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.modules.Module;
import org.jetbrains.kotlin.modules.TargetId;
import org.jetbrains.kotlin.platform.CommonPlatforms;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;
import org.jetbrains.kotlin.utils.PathUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.kotlin.internal.KotlinParserVisitor;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.style.NamedStyles;
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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.*;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS;
import static org.jetbrains.kotlin.cli.jvm.K2JVMCompilerKt.configureModuleChunk;
import static org.jetbrains.kotlin.cli.jvm.compiler.CoreEnvironmentUtilsKt.applyModuleProperties;
import static org.jetbrains.kotlin.cli.jvm.compiler.CoreEnvironmentUtilsKt.forAllFiles;
import static org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompilerKt.configureSourceRoots;
import static org.jetbrains.kotlin.cli.jvm.compiler.pipeline.CompilerPipelineKt.compileModuleToAnalyzedFir;
import static org.jetbrains.kotlin.cli.jvm.compiler.pipeline.CompilerPipelineKt.convertAnalyzedFirToIr;
import static org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt.*;
import static org.jetbrains.kotlin.config.CommonConfigurationKeys.*;
import static org.jetbrains.kotlin.config.JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT;
import static org.jetbrains.kotlin.config.JVMConfigurationKeys.FRIEND_PATHS;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KotlinParser implements Parser<K.CompilationUnit> {
    @Nullable
    private final Collection<Path> classpath;

    private final List<NamedStyles> styles;
    private final boolean logCompilationWarningsAndErrors;
    private final JavaTypeCache typeCache;
    private final String moduleName;

    @Override
    public List<K.CompilationUnit> parse(@Language("kotlin") String... sources) {
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
    public List<K.CompilationUnit> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingExecutionContextView pctx = ParsingExecutionContextView.view(ctx);
        ParsingEventListener parsingListener = pctx.getParsingListener();
        CompilerConfiguration compilerConfiguration = compilerConfiguration();

        File buildFile = null;
        K2JVMCompilerArguments arguments = new K2JVMCompilerArguments();
        ModuleChunk moduleChunk = configureModuleChunk(compilerConfiguration, arguments, buildFile);
        List<Module> chunk = moduleChunk.getModules();

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

        configureSourceRoots(compilerConfiguration, chunk, buildFile);
        configureJdkClasspathRoots(compilerConfiguration);

        Disposable disposable = Disposer.newDisposable();
        Map<FirSession, List<CompiledKotlinSource>> sessionToCus = new HashMap<>();
        List<K.CompilationUnit> cus;
        try {
            KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                    disposable,
                    compilerConfiguration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES);

            Project project = environment.getProject();
            VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
            GlobalSearchScope globalScope = GlobalSearchScope.allScope(project);
            JvmPackagePartProvider packagePartProvider = environment.createPackagePartProvider(globalScope);
            Function<GlobalSearchScope, JvmPackagePartProvider> packagePartProviderFunction = (globalSearchScope) -> packagePartProvider;
            VfsBasedProjectEnvironment projectEnvironment = new VfsBasedProjectEnvironment(
                    project,
                    fileSystem,
                    packagePartProviderFunction::apply);

            if (chunk.size() > 1) {
                throw new IllegalStateException("Implement me. Expects chunk size of 1, but was " + chunk.size());
            }

            Module module = chunk.get(0);
            CompilerConfiguration moduleConfiguration = applyModuleProperties(compilerConfiguration, module, buildFile);
            moduleConfiguration.put(FRIEND_PATHS, module.getFriendPaths());

            Set<KtSourceFile> platformSources = new LinkedHashSet<>();
            Set<KtSourceFile> commonSources = new LinkedHashSet<>();

            List<ContentRoot> contentRoots = compilerConfiguration.get(CONTENT_ROOTS);
            List<KotlinSourceRoot> roots = contentRoots == null ? emptyList() : contentRoots.stream()
                    .filter(it -> it instanceof KotlinSourceRoot)
                    .map(it -> (KotlinSourceRoot) it).collect(toList());

            Function2<VirtualFile, Boolean, Unit> sortFiles = (virtualFile, isCommon) -> {
                KtVirtualFileSourceFile file = new KtVirtualFileSourceFile(virtualFile);
                if (isCommon) {
                    commonSources.add(file);
                } else {
                    platformSources.add(file);
                }
                return Unit.INSTANCE;
            };

            forAllFiles(roots, compilerConfiguration, project, null, sortFiles);

            /*
                Create a `LightVirtualFile` for each `Input` and add the virtual files as platform sources.
                A platform source will result in an IR FirFile.

                A `KtVirtualFileSourceFile` will have a different PSI than a file that is resolvable on disk.
                For actual source files, the input may be resolved by `forAllFiles` using `ContentRootsKt#addKotlinSourceRoots(Path, false)`.

                The benefit of `addKotlinSourceRoots` is a higher quality PSI element that backs the IR FirFile.
                `LightVirtualFile` are created to support tests and in the future, Kotlin template.
                We might want to extract the generation of `platformSources` later on.
             */
            int i = 0;
            for (Input source : sources) {
                String fileName = "openRewriteFile.kt".equals(source.getPath().toString()) ? "openRewriteFile.kt" + i : source.getPath().toString();
                VirtualFile vFile = new LightVirtualFile(fileName, KotlinFileType.INSTANCE, source.getSource(ctx).readFully());
                platformSources.add(new KtVirtualFileSourceFile(vFile));
            }

            BaseDiagnosticsCollector diagnosticsReporter = DiagnosticReporterFactory.INSTANCE.createReporter(false);
            ModuleCompilerInput compilerInput = new ModuleCompilerInput(
                    new TargetId(module.getModuleName(), module.getModuleType()),
                    CommonPlatforms.INSTANCE.getDefaultCommonPlatform(),
                    commonSources,
                    JvmPlatforms.INSTANCE.getUnspecifiedJvmPlatform(),
                    platformSources,
                    moduleConfiguration,
                    emptyList()
            );

            ModuleCompilerEnvironment compilerEnvironment = new ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter);
            CommonCompilerPerformanceManager performanceManager = compilerConfiguration.get(PERF_MANAGER);
            ModuleCompilerAnalyzedOutput output = compileModuleToAnalyzedFir(
                    compilerInput,
                    compilerEnvironment,
                    emptyList(),
                    null,
                    diagnosticsReporter,
                    performanceManager
            );
            convertAnalyzedFirToIr(compilerInput, output, compilerEnvironment);

            List<FirFile> firFiles = output.getFir();
            List<Input> inputs = new ArrayList<>(firFiles.size());
            sources.iterator().forEachRemaining(inputs::add);
            assert firFiles.size() == inputs.size();

            List<CompiledKotlinSource> compiledKotlinSources = new ArrayList<>();
            for (int j = 0; j < inputs.size(); j++) {
                Input input = inputs.get(j);
                FirFile firFile = firFiles.get(j);
                compiledKotlinSources.add(new CompiledKotlinSource(input, firFile));
            }

            sessionToCus.put(output.getSession(), compiledKotlinSources);

            FirSession firSession = (FirSession) sessionToCus.keySet().toArray()[0];
            List<CompiledKotlinSource> compilerCus = sessionToCus.get(firSession);
            cus = new ArrayList<>(sessionToCus.get(firSession).size());

            for (CompiledKotlinSource compiled : compilerCus) {
                try {
                    KotlinParserVisitor mappingVisitor = new KotlinParserVisitor(
                            compiled.getInput().getRelativePath(relativeTo),
                            compiled.getInput().getFileAttributes(),
                            compiled.getInput().getSource(ctx),
                            typeCache,
                            firSession,
                            ctx
                    );

                    K.CompilationUnit kcu = (K.CompilationUnit) mappingVisitor.visitFile(compiled.getFirFile(), new InMemoryExecutionContext());
                    cus.add(kcu);
                    parsingListener.parsed(compiled.getInput(), kcu);
                } catch (Throwable t) {
                    pctx.parseFailure(compiled.getInput(), compiled.getInput().getRelativePath(relativeTo), KotlinParser.builder().build(), t);
                    ctx.getOnError().accept(t);
                }
            }
        } finally {
            Disposer.dispose(disposable);
        }

        return cus;
    }

    private CompilerConfiguration compilerConfiguration() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, moduleName);
        compilerConfiguration.put(MESSAGE_COLLECTOR_KEY, logCompilationWarningsAndErrors ?
                new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true) :
                MessageCollector.Companion.getNONE());

        compilerConfiguration.put(LANGUAGE_VERSION_SETTINGS, new LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_7, ApiVersion.KOTLIN_1_7));

        compilerConfiguration.put(USE_FIR,  true);
        compilerConfiguration.put(DO_NOT_CLEAR_BINDING_CONTEXT, true);
        compilerConfiguration.put(ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS,  true);
        compilerConfiguration.put(INCREMENTAL_COMPILATION,  true);

        addJvmSdkRoots(compilerConfiguration, PathUtil.getJdkClassesRootsFromCurrentJre());

        return compilerConfiguration;
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".kt");
    }

    @Override
    public KotlinParser reset() {
        typeCache.clear();
        return this;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("openRewriteFile.kt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        @Nullable
        private Collection<Path> classpath = JavaParser.runtimeClasspath();

        private JavaTypeCache typeCache = new JavaTypeCache();
        private boolean logCompilationWarningsAndErrors = false;
        private final List<NamedStyles> styles = new ArrayList<>();
        private String moduleName = "main";

        public Builder() {
            super(K.CompilationUnit.class);
        }

        public Builder logCompilationWarningsAndErrors(boolean logCompilationWarningsAndErrors) {
            this.logCompilationWarningsAndErrors = logCompilationWarningsAndErrors;
            return this;
        }

        public Builder classpath(Collection<Path> classpath) {
            this.classpath = classpath;
            return this;
        }

        public Builder classpath(String... classpath) {
            this.classpath = JavaParser.dependenciesFromClasspath(classpath);
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

        public KotlinParser build() {
            return new KotlinParser(classpath, styles, logCompilationWarningsAndErrors, typeCache, moduleName);
        }

        @Override
        public String getDslName() {
            return "kotlin";
        }
    }
}
