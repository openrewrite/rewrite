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
package org.openrewrite.kotlin;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.fir.FirModuleData;
import org.jetbrains.kotlin.fir.FirModuleDataImpl;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode;
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode;
import org.jetbrains.kotlin.fir.builder.RawFirBuilder;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar;
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider;
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider;
import org.jetbrains.kotlin.fir.session.FirSessionFactory;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY;
import static org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS;

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
        Map<Input, FirFile> compilerCus = parseInputsToCompilerAst(sources, relativeTo, pctx);
        List<K.CompilationUnit> cus = new ArrayList<>(compilerCus.size());

        for (Map.Entry<Input, FirFile> compiled : compilerCus.entrySet()) {
            try {
                KotlinParserVisitor mappingVisitor = new KotlinParserVisitor(
                        compiled.getKey().getRelativePath(relativeTo),
                        compiled.getKey().getFileAttributes(),
                        compiled.getKey().getSource(ctx),
                        typeCache,
                        ctx
                );
                K.CompilationUnit kcu = null; // FIXME map the compiler's AST to a K.CompilationUnit
                cus.add(kcu);
                parsingListener.parsed(compiled.getKey(), kcu);
            } catch (Throwable t) {
//                pctx.parseFailure(compiled.getKey().getRelativePath(relativeTo), t);
                ctx.getOnError().accept(t);
            }
        }

        return cus;
    }

    private Map<Input, FirFile> parseInputsToCompilerAst(Iterable<Input> sources, @Nullable Path relativeTo, ParsingExecutionContextView ctx) {
        Disposable disposable = Disposer.newDisposable();
        try {
            KotlinCoreEnvironment kenv = KotlinCoreEnvironment.createForProduction(
                    disposable, compilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);

            Project project = kenv.getProject();

            /*
                TopDownAnalyzerFacadeForJVM.kt
                fun newModuleSearchScope(project: Project, files: Collection<KtFile>): GlobalSearchScope {
                    // In case of separate modules, the source module scope generally consists of the following scopes:
                    // 1) scope which only contains passed Kotlin source files (.kt and .kts)
                    // 2) scope which contains all Java source files (.java) in the project
                    return GlobalSearchScope.filesScope(project, files.map { it.virtualFile }.toSet()).uniteWith(AllJavaSourcesInProjectScope(project))
                }
             */

            TargetPlatform targetPlatform = JvmPlatforms.INSTANCE.getJvm17();

            Collection<KtFile> files = new ArrayList<>(); // todo
            GlobalSearchScope testScope = TopDownAnalyzerFacadeForJVM.INSTANCE.newModuleSearchScope(project, files);

            Function1<FirSessionFactory.FirSessionConfigurator, Unit> sessionConfigurator = (firProjectSessionProvider) -> Unit.INSTANCE;

            // todo
            List<FirModuleData> dependencies = Collections.emptyList();
            List<FirModuleData> dependsOnDependencies = Collections.emptyList();
            List<FirModuleData> friendDependencies = Collections.emptyList();
            FirModuleData firModuleData = new FirModuleDataImpl(
                    Name.identifier("main"),
                    dependencies,
                    dependsOnDependencies,
                    friendDependencies,
                    targetPlatform,
                    JvmPlatformAnalyzerServices.INSTANCE
            );

            FirProjectSessionProvider firProjectSessionProvider = new FirProjectSessionProvider();

            LanguageVersionSettings languageVersionSettings = new LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_7,
                    ApiVersion.KOTLIN_1_7);

            FirSession firSessionComponents = configureMainSession(
                    project,
                    sessionConfigurator,
                    Collections.emptyList(), // todo
                    testScope,
                    firModuleData,
                    firProjectSessionProvider,
                    languageVersionSettings
            );

            RawFirBuilder rawFirBuilder = new RawFirBuilder(firSessionComponents,
                    new FirKotlinScopeProvider(), PsiHandlingMode.IDE, BodyBuildingMode.NORMAL);
            PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
            Map<Input, FirFile> cus = new LinkedHashMap<>();
            for (Input sourceFile : sources) {
                KtFile ktFile = (KtFile) psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, sourceFile.getSource().readFully());
                FirFile firFile = rawFirBuilder.buildFirFile(ktFile);
                cus.put(sourceFile, firFile);
            }
            return cus;
        } finally {
            disposable.dispose();
        }
    }

    private CompilerConfiguration compilerConfiguration() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.put(MESSAGE_COLLECTOR_KEY, logCompilationWarningsAndErrors ?
                new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true) :
                MessageCollector.Companion.getNONE());
        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, moduleName);
        return compilerConfiguration;
    }

    private static FirSession configureMainSession(Project project,
                                            Function1<? super FirSessionFactory.FirSessionConfigurator, Unit> sessionConfigurator,
                                            List<FirExtensionRegistrar> extensionRegistrars,
                                            GlobalSearchScope sourcesScope,
                                            FirModuleData mainModuleData,
                                            FirProjectSessionProvider sessionProvider,
                                            @Nullable LanguageVersionSettings languageVersionSettings) {

        LanguageVersionSettings languageVersionSettingsOrDefault = languageVersionSettings == null ? LanguageVersionSettingsImpl.DEFAULT : languageVersionSettings;

        JvmPackagePartProvider packagePartProvider = new JvmPackagePartProvider(languageVersionSettingsOrDefault, sourcesScope);
        Function<GlobalSearchScope, JvmPackagePartProvider> packagePartProviderFunction = (globalSearchScope) -> packagePartProvider;
        VfsBasedProjectEnvironment projectEnvironment = new VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                packagePartProviderFunction::apply);

        PsiBasedProjectFileSearchScope projectFileSearchScope = new PsiBasedProjectFileSearchScope(sourcesScope);
        return FirSessionFactory.INSTANCE.createJavaModuleBasedSession(
                mainModuleData,
                sessionProvider,
                projectFileSearchScope,
                projectEnvironment,
                null, // todo
                extensionRegistrars,
                languageVersionSettingsOrDefault,
                null, // todo
                null, // todo
                true,
                sessionConfigurator
        );
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".kt") || path.toString().endsWith(".kts");
    }

    @Override
    public KotlinParser reset() {
        typeCache.clear();
        return this;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.groovy");
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
