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
import kotlin.jvm.functions.Function1;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.CompilerPipelineKt;
import org.jetbrains.kotlin.com.intellij.core.CoreProjectScopeBuilder;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.roots.FileIndexFacade;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems;
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.fir.DependencyListForCliModule;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.backend.*;
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator;
import org.jetbrains.kotlin.fir.backend.generators.CallAndReferenceGenerator;
import org.jetbrains.kotlin.fir.backend.generators.DelegatedMemberGenerator;
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator;
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider;
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler;
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode;
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode;
import org.jetbrains.kotlin.fir.builder.RawFirBuilder;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor;
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider;
import org.jetbrains.kotlin.fir.resolve.ScopeSession;
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider;
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope;
import org.jetbrains.kotlin.fir.signaturer.FirBasedSignatureComposer;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.ir.IrBuiltIns;
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler;
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler;
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl;
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl;
import org.jetbrains.kotlin.ir.util.NameProvider;
import org.jetbrains.kotlin.ir.util.SymbolTable;
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
import java.io.File;
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
import static org.jetbrains.kotlin.config.JVMConfigurationKeys.*;
import static org.jetbrains.kotlin.fir.pipeline.AnalyseKt.runResolution;

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

                K.CompilationUnit kcu = (K.CompilationUnit) mappingVisitor.visitFile(compiled.getValue(), new InMemoryExecutionContext());
                cus.add(kcu);
                parsingListener.parsed(compiled.getKey(), kcu);
            } catch (Throwable t) {
                pctx.parseFailure(compiled.getKey(), compiled.getKey().getRelativePath(relativeTo), KotlinParser.builder().build(), t);
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
            TargetPlatform targetPlatform = JvmPlatforms.INSTANCE.getJvm17();

            FileIndexFacade fileIndexFacade = FileIndexFacade.getInstance(project);
            CoreProjectScopeBuilder coreProjectScopeBuilder = new CoreProjectScopeBuilder(project, fileIndexFacade);
            GlobalSearchScope globalScope = coreProjectScopeBuilder.buildAllScope();

            // TODO: Fix me. Hardcoded for main.
            Name name = Name.identifier("main");

            DependencyListForCliModule.Builder dependencyListForCliModuleBuilder = new DependencyListForCliModule.Builder(name, targetPlatform, JvmPlatformAnalyzerServices.INSTANCE);
            // Class path dependencies may be added here:
//            dependencyListForCliModuleBuilder.dependencies();
            DependencyListForCliModule dependencyListForCliModule = dependencyListForCliModuleBuilder.build();

            FirProjectSessionProvider firProjectSessionProvider = new FirProjectSessionProvider();

            LanguageVersionSettings languageVersionSettings = new LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_7,
                    ApiVersion.KOTLIN_1_7);

            JvmPackagePartProvider packagePartProvider = new JvmPackagePartProvider(languageVersionSettings, globalScope);
            Function<GlobalSearchScope, JvmPackagePartProvider> packagePartProviderFunction = (globalSearchScope) -> packagePartProvider;
            VfsBasedProjectEnvironment projectEnvironment = new VfsBasedProjectEnvironment(
                    project,
                    VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                    packagePartProviderFunction::apply);

            Function1<DependencyListForCliModule.Builder, Unit> cliModuleUnitFunction1 = (builder) -> Unit.INSTANCE;
            FirSession firSession = CompilerPipelineKt.createSession(
                    name.asString(),
                    targetPlatform,
                    kenv.getConfiguration(),
                    projectEnvironment,
                    AbstractProjectFileSearchScope.ANY.INSTANCE,
                    dependencyListForCliModule.getAnalyzerServices(),
                    firProjectSessionProvider,
                    Collections.emptyList(),
                    null,
                    true,
                    true,
                    cliModuleUnitFunction1
            );

            RawFirBuilder rawFirBuilder = new RawFirBuilder(
                    firSession,
                    new FirKotlinScopeProvider(),
                    PsiHandlingMode.COMPILER,
                    BodyBuildingMode.NORMAL
            );

            PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);

            Map<Input, FirFile> cus = new LinkedHashMap<>();

            for (Input sourceFile : sources) {
                KtFile ktFile = (KtFile) psiFileFactory.createFileFromText(
                        sourceFile.getPath().getFileName().toString(),
                        KotlinLanguage.INSTANCE,
                        sourceFile.getSource().readFully());

                FirFile firFile = rawFirBuilder.buildFirFile(ktFile);
                cus.put(sourceFile, firFile);
            }

            List<FirFile> firFiles = new ArrayList<>(cus.values());
            runResolution(firSession, firFiles);

            convertFirToIr(firFiles, firSession, languageVersionSettings);
            return cus;
        } finally {
            disposable.dispose();
        }
    }

    private CompilerConfiguration compilerConfiguration() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // TODO: fix me. Adds the JDK location to resolve JavaTypes.
        File javaHome = new File(System.getProperty("java.home"));
        compilerConfiguration.put(JVMConfigurationKeys.JDK_HOME, javaHome);

        compilerConfiguration.put(DO_NOT_CLEAR_BINDING_CONTEXT,  true);

        compilerConfiguration.put(MESSAGE_COLLECTOR_KEY, logCompilationWarningsAndErrors ?
                new PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, true) :
                MessageCollector.Companion.getNONE());
        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, moduleName);
        return compilerConfiguration;
    }

    private void convertFirToIr(List<FirFile> firFiles, FirSession firSession, LanguageVersionSettings languageVersionSettings) {
        FirJvmKotlinMangler firMangler = new FirJvmKotlinMangler(firSession);
        FirBasedSignatureComposer firBasedSignatureComposer = new FirBasedSignatureComposer(firMangler);

        JvmDescriptorMangler descriptorMangler = new JvmDescriptorMangler(null);
        JvmIdSignatureDescriptor idSignatureDescriptor = new JvmIdSignatureDescriptor(descriptorMangler);
        SymbolTable symbolTable = new SymbolTable(idSignatureDescriptor, IrFactoryImpl.INSTANCE, NameProvider.DEFAULT.INSTANCE);
        Fir2IrComponentsStorage components = new Fir2IrComponentsStorage(
                firSession,
                new ScopeSession(),
                symbolTable,
                IrFactoryImpl.INSTANCE,
                firBasedSignatureComposer,
                Fir2IrExtensions.Default.INSTANCE
        );

        FirModuleDescriptor firModuleDescriptor = new FirModuleDescriptor(firSession);
        Fir2IrConverter fir2IrConverter = new Fir2IrConverter(firModuleDescriptor, components);
        components.setConverter(fir2IrConverter);

        Fir2IrClassifierStorage fir2IrClassifierStorage = new Fir2IrClassifierStorage(components);
        components.setClassifierStorage(fir2IrClassifierStorage);

        DelegatedMemberGenerator delegatedMemberGenerator = new DelegatedMemberGenerator(components);
        components.setDelegatedMemberGenerator(delegatedMemberGenerator);

        Fir2IrDeclarationStorage fir2IrDeclarationStorage = new Fir2IrDeclarationStorage(components, firModuleDescriptor);
        components.setDeclarationStorage(fir2IrDeclarationStorage);

        Fir2IrVisibilityConverter fir2IrVisibilityConverter = Fir2IrVisibilityConverter.Default.INSTANCE;
        components.setVisibilityConverter(fir2IrVisibilityConverter);

        Fir2IrTypeConverter fir2IrTypeConverter = new Fir2IrTypeConverter(components);
        components.setTypeConverter(fir2IrTypeConverter);

        IrBuiltIns irBuiltIns = new IrBuiltInsOverFir(
                components,
                languageVersionSettings,
                firModuleDescriptor,
                JvmIrMangler.INSTANCE,
                languageVersionSettings.getFlag(AnalysisFlags.getBuiltInsFromSources())
        );
        components.setIrBuiltIns(irBuiltIns);

        Fir2IrConversionScope conversionScope = new Fir2IrConversionScope();
        Fir2IrVisitor fir2IrVisitor = new Fir2IrVisitor(components, conversionScope);
        Fir2IrSpecialSymbolProvider fir2IrSpecialSymbolProvider = new Fir2IrJvmSpecialAnnotationSymbolProvider();

        Fir2IrBuiltIns fir2IrBuiltIns = new Fir2IrBuiltIns(components, fir2IrSpecialSymbolProvider);
        components.setBuiltIns(fir2IrBuiltIns);

        AnnotationGenerator annotationGenerator = new AnnotationGenerator(components);
        components.setAnnotationGenerator(annotationGenerator);

        FakeOverrideGenerator fakeOverrideGenerator = new FakeOverrideGenerator(components, conversionScope);
        components.setFakeOverrideGenerator(fakeOverrideGenerator);

        CallAndReferenceGenerator callAndReferenceGenerator = new CallAndReferenceGenerator(components, fir2IrVisitor, conversionScope);
        components.setCallGenerator(callAndReferenceGenerator);

        FirIrProvider firIrProvider = new FirIrProvider(components);
        components.setIrProviders(Collections.singletonList(firIrProvider));

        Fir2IrExtensions fir2IrExtensions = Fir2IrExtensions.Default.INSTANCE;
        fir2IrExtensions.registerDeclarations(symbolTable);

        IrModuleFragmentImpl irModuleFragment = new IrModuleFragmentImpl(firModuleDescriptor, irBuiltIns, Collections.emptyList());
        fir2IrConverter.runSourcesConversion(
                firFiles,
                irModuleFragment,
                Collections.emptyList(),
                fir2IrVisitor,
                fir2IrExtensions
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
        return prefix.resolve("file.kt");
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
