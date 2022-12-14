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
//file:noinspection UnstableApiUsage

import org.gradle.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.dsl.*
import org.gradle.api.artifacts.ivy.*
import org.gradle.api.artifacts.maven.*
import org.gradle.api.artifacts.query.*
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.artifacts.result.*
import org.gradle.api.artifacts.transform.*
import org.gradle.api.artifacts.type.*
import org.gradle.api.artifacts.verification.*
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.*
import org.gradle.api.attributes.plugin.*
import org.gradle.api.capabilities.*
import org.gradle.api.component.*
import org.gradle.api.credentials.*
import org.gradle.api.distribution.*
import org.gradle.api.distribution.plugins.*
import org.gradle.api.execution.*
import org.gradle.api.file.*
import org.gradle.api.initialization.*
import org.gradle.api.initialization.definition.*
import org.gradle.api.initialization.dsl.*
import org.gradle.api.initialization.resolve.*
import org.gradle.api.invocation.*
import org.gradle.api.java.archives.*
import org.gradle.api.jvm.*
import org.gradle.api.logging.*
import org.gradle.api.logging.configuration.*
import org.gradle.api.model.*
import org.gradle.api.plugins.*
import org.gradle.api.plugins.antlr.*
import org.gradle.api.plugins.catalog.*
import org.gradle.api.plugins.jvm.*
import org.gradle.api.plugins.quality.*
import org.gradle.api.plugins.scala.*
import org.gradle.api.provider.*
import org.gradle.api.publish.*
import org.gradle.api.publish.ivy.*
import org.gradle.api.publish.ivy.plugins.*
import org.gradle.api.publish.ivy.tasks.*
import org.gradle.api.publish.maven.*
import org.gradle.api.publish.maven.plugins.*
import org.gradle.api.publish.maven.tasks.*
import org.gradle.api.publish.plugins.*
import org.gradle.api.publish.tasks.*
import org.gradle.api.reflect.*
import org.gradle.api.reporting.*
import org.gradle.api.reporting.components.*
import org.gradle.api.reporting.dependencies.*
import org.gradle.api.reporting.dependents.*
import org.gradle.api.reporting.model.*
import org.gradle.api.reporting.plugins.*
import org.gradle.api.resources.*
import org.gradle.api.services.*
import org.gradle.api.specs.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.ant.*
import org.gradle.api.tasks.application.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.api.tasks.diagnostics.*
import org.gradle.api.tasks.incremental.*
import org.gradle.api.tasks.javadoc.*
import org.gradle.api.tasks.options.*
import org.gradle.api.tasks.scala.*
import org.gradle.api.tasks.testing.*
import org.gradle.api.tasks.testing.junit.*
import org.gradle.api.tasks.testing.junitplatform.*
import org.gradle.api.tasks.testing.testng.*
import org.gradle.api.tasks.util.*
import org.gradle.api.tasks.wrapper.*
import org.gradle.authentication.*
import org.gradle.authentication.aws.*
import org.gradle.authentication.http.*
import org.gradle.build.event.*
import org.gradle.buildinit.*
import org.gradle.buildinit.plugins.*
import org.gradle.buildinit.tasks.*
import org.gradle.caching.*
import org.gradle.caching.configuration.*
import org.gradle.caching.http.*
import org.gradle.caching.local.*
import org.gradle.concurrent.*
import org.gradle.external.javadoc.*
import org.gradle.ivy.*
import org.gradle.jvm.*
import org.gradle.jvm.application.scripts.*
import org.gradle.jvm.application.tasks.*
import org.gradle.jvm.tasks.*
import org.gradle.jvm.toolchain.*
import org.gradle.language.*
import org.gradle.language.assembler.*
import org.gradle.language.assembler.plugins.*
import org.gradle.language.assembler.tasks.*
import org.gradle.language.base.*
import org.gradle.language.base.artifact.*
import org.gradle.language.base.compile.*
import org.gradle.language.base.plugins.*
import org.gradle.language.base.sources.*
import org.gradle.language.java.artifact.*
import org.gradle.language.jvm.tasks.*
import org.gradle.language.plugins.*
import org.gradle.language.rc.*
import org.gradle.language.rc.plugins.*
import org.gradle.language.rc.tasks.*
import org.gradle.maven.*
import org.gradle.model.*
import org.gradle.normalization.*
import org.gradle.platform.base.*
import org.gradle.platform.base.binary.*
import org.gradle.platform.base.component.*
import org.gradle.platform.base.plugins.*
import org.gradle.plugin.devel.*
import org.gradle.plugin.devel.plugins.*
import org.gradle.plugin.devel.tasks.*
import org.gradle.plugin.management.*
import org.gradle.plugin.use.*
import org.gradle.plugins.ear.*
import org.gradle.plugins.ear.descriptor.*
import org.gradle.plugins.ide.*
import org.gradle.plugins.ide.api.*
import org.gradle.plugins.ide.eclipse.*
import org.gradle.plugins.ide.idea.*
import org.gradle.plugins.signing.*
import org.gradle.plugins.signing.signatory.*
import org.gradle.plugins.signing.signatory.pgp.*
import org.gradle.plugins.signing.type.*
import org.gradle.plugins.signing.type.pgp.*
import org.gradle.process.*
import org.gradle.testing.base.*
import org.gradle.testing.base.plugins.*
import org.gradle.testing.jacoco.plugins.*
import org.gradle.testing.jacoco.tasks.*
import org.gradle.testing.jacoco.tasks.rules.*
import org.gradle.testkit.runner.*
import org.gradle.util.*
import org.gradle.vcs.*
import org.gradle.vcs.git.*
import org.gradle.work.*
import org.gradle.workers.*

interface PluginSpec {
    Plugin id(String i)
}

interface Plugin {
    Plugin version(String v)
    Plugin apply(boolean a)
}

interface DependencyHandlerSpec extends DependencyHandler {
    Dependency api(String dependencyNotation)
    Dependency api(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency api(Map<String, String> dependencyNotation)
    Dependency api(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency classpath(String dependencyNotation)
    Dependency classpath(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency classpath(Map<String, String> dependencyNotation)
    Dependency classpath(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency compile(String dependencyNotation)
    Dependency compile(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency compile(Map<String, String> dependencyNotation)
    Dependency compile(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency compileOnly(String dependencyNotation)
    Dependency compileOnly(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency compileOnly(Map<String, String> dependencyNotation)
    Dependency compileOnly(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency implementation(String dependencyNotation)
    Dependency implementation(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency implementation(Map<String, String> dependencyNotation)
    Dependency implementation(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtime(String dependencyNotation)
    Dependency runtime(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtime(Map<String, String> dependencyNotation)
    Dependency runtime(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtimeOnly(String dependencyNotation)
    Dependency runtimeOnly(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtimeOnly(Map<String, String> dependencyNotation)
    Dependency runtimeOnly(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtimeClasspath(String dependencyNotation)
    Dependency runtimeClasspath(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency runtimeClasspath(Map<String, String> dependencyNotation)
    Dependency runtimeClasspath(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testCompile(String dependencyNotation)
    Dependency testCompile(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testCompile(Map<String, String> dependencyNotation)
    Dependency testCompile(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testImplementation(String dependencyNotation)
    Dependency testImplementation(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testImplementation(Map<String, String> dependencyNotation)
    Dependency testImplementation(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testRuntime(String dependencyNotation)
    Dependency testRuntime(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testRuntime(Map<String, String> dependencyNotation)
    Dependency testRuntime(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testRuntimeOnly(String dependencyNotation)
    Dependency testRuntimeOnly(String dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
    Dependency testRuntimeOnly(Map<String, String> dependencyNotation)
    Dependency testRuntimeOnly(Map<String, String> dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ModuleDependency) Closure closure)
}

interface RepositoryHandlerSpec extends RepositoryHandler {
    MavenArtifactRepository maven(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=MavenArtifactRepositorySpec) Closure closure)
    IvyArtifactRepository ivy(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=IvyArtifactRepository) Closure closure)
}

interface MavenArtifactRepositorySpec extends MavenArtifactRepository {
    void url(Object url)
}

interface RewriteTestSpec {
    File getWorkingDir()
    void setWorkingDir(File dir)
    void setWorkingDir(Object dir)
    RewriteTestSpec workingDir(Object dir)
    JavaVersion getJavaVersion()
    String getExecutable()
    RewriteTestSpec executable(Object executable)
    void setExecutable(String executable)
    void setExecutable(Object executable)
    Map<String, Object> getSystemProperties()
    void setSystemProperties(Map<String, ?> properties)
    RewriteTestSpec systemProperties(Map<String, ?> properties)
    RewriteTestSpec systemProperty(String name, Object value)
    FileCollection getBootstrapClasspath()
    void setBootstrapClasspath(FileCollection classpath)
    RewriteTestSpec bootstrapClasspath(Object... classpath)
    String getMinHeapSize()
    String getDefaultCharacterEncoding()
    void setDefaultCharacterEncoding(String defaultCharacterEncoding)
    void setMinHeapSize(String heapSize)
    String getMaxHeapSize()
    void setMaxHeapSize(String heapSize)
    List<String> getJvmArgs()
    void setJvmArgs(List<String> arguments)
    void setJvmArgs(Iterable<?> arguments)
    RewriteTestSpec jvmArgs(Iterable<?> arguments)
    RewriteTestSpec jvmArgs(Object... arguments)
    boolean getEnableAssertions()
    void setEnableAssertions(boolean enabled)
    boolean getDebug()
    void setDebug(boolean enabled)
    void setFailFast(boolean failFast)
    boolean getFailFast()
    List<String> getAllJvmArgs()
    void setAllJvmArgs(List<String> arguments)
    void setAllJvmArgs(Iterable<?> arguments)
    Map<String, Object> getEnvironment()
    RewriteTestSpec environment(Map<String, ?> environmentVariables)
    RewriteTestSpec environment(String name, Object value)
    void setEnvironment(Map<String, ?> environmentVariables)
    RewriteTestSpec copyTo(ProcessForkOptions target)
    RewriteTestSpec copyTo(JavaForkOptions target)
    RewriteTestSpec include(String... includes)
    RewriteTestSpec include(Iterable<String> includes)
    RewriteTestSpec include(Spec<FileTreeElement> includeSpec)
    RewriteTestSpec include(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=FileTreeElement)Closure includeSpec)
    RewriteTestSpec exclude(String... excludes)
    RewriteTestSpec exclude(Iterable<String> excludes)
    RewriteTestSpec exclude(Spec<FileTreeElement> excludeSpec)
    RewriteTestSpec exclude(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=FileTreeElement)Closure excludeSpec)
    RewriteTestSpec setTestNameIncludePatterns(List<String> testNamePattern)
    FileCollection getTestClassesDirs()
    void setTestClassesDirs(FileCollection testClassesDirs)
    Set<String> getIncludes()
    RewriteTestSpec setIncludes(Iterable<String> includes)
    Set<String> getExcludes()
    RewriteTestSpec setExcludes(Iterable<String> excludes)
    TestFrameworkOptions options(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=TestFrameworkOptions)Closure testFrameworkConfigure)
    TestFrameworkOptions options(Action<? super TestFrameworkOptions> testFrameworkConfigure)
    void useJUnit()
    void useJUnit(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=JUnitOptions)Closure testFrameworkConfigure) // delegate
    void useJUnit(Action<? super JUnitOptions> testFrameworkConfigure)
    void useJUnitPlatform()
    void useJUnitPlatform(Action<? super JUnitPlatformOptions> testFrameworkConfigure)
    void useTestNG()
    void useTestNG(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=TestNGOptions)Closure testFrameworkConfigure)
    void useTestNG(Action<? super TestNGOptions> testFrameworkConfigure)
    FileCollection getClasspath()
    void setClasspath(FileCollection classpath)
    boolean isScanForTestClasses()
    void setScanForTestClasses(boolean scanForTestClasses)
    long getForkEvery()
    void setForkEvery(Long forkEvery)
    int getMaxParallelForks()
    void setMaxParallelForks(int maxParallelForks)
    FileTree getCandidateClassFiles()
    void filter(Action<TestFilter> action)
}

abstract class RewriteGradleProject implements Project {
    Map ext;

    // It would be more correct for ext to delegate to ExtraPropertiesExtension, but StaticTypeCheckingVisitor has problems with that
    abstract void ext(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=Map) Closure cl)
    abstract void dependencies(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=DependencyHandlerSpec) Closure cl)
    abstract void plugins(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PluginSpec) Closure cl)
    abstract void repositories(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RepositoryHandlerSpec) Closure cl)
    abstract void subprojects(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RewriteGradleProject) Closure cl)
    abstract void allprojects(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RewriteGradleProject) Closure cl)
    abstract void test(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RewriteTestSpec) Closure cl)

    // @Deprecated(since="7.1.0", replacedBy="org.gradle.api.plugins.JavaPluginExtension")
    // @see org.gradle.api.plugins.JavaPluginConvention
    abstract JavaVersion getSourceCompatibility();
    abstract void setSourceCompatibility(Object value);
    abstract void setSourceCompatibility(JavaVersion value);
    abstract JavaVersion getTargetCompatibility();
    abstract void setTargetCompatibility(Object value);
    abstract void setTargetCompatibility(JavaVersion value);

    // These functions don't actually exist in the Gradle API, but are syntactic sugar which forward to TaskContainer.create()
    abstract Task task(Map<String, ?> options)
    abstract Task task(Map<String, ?> options, Closure configureClosure)
    abstract Task task(String name, Closure configureClosure)
    abstract Task task(String name)
    abstract <T extends Task> T task(String name, Class<T> type)
    abstract <T extends Task> T task(String name, Class<T> type, Object... constructorArgs)
    abstract <T extends Task> T task(String name, Class<T> type, Action<? super T> configuration)

    void __script__() {
}}
