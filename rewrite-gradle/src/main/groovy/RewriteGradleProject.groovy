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


import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.testing.TestFilter
import org.gradle.api.tasks.testing.TestFrameworkOptions
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.gradle.process.JavaForkOptions
import org.gradle.process.ProcessForkOptions

interface DependencyHandlerSpec extends DependencyHandler {
    Dependency annotationProcessor(Object... dependencyNotation)
    Dependency annotationProcessor(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency api(Object... dependencyNotation)
    Dependency api(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency classpath(Object... dependencyNotation)
    Dependency classpath(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency compile(Object... dependencyNotation)
    Dependency compile(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency compileOnly(Object... dependencyNotation)
    Dependency compileOnly(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency implementation(Object... dependencyNotation)
    Dependency implementation(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency runtime(Object... dependencyNotation)
    Dependency runtime(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency runtimeOnly(Object... dependencyNotation)
    Dependency runtimeOnly(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency runtimeClasspath(Object... dependencyNotation)
    Dependency runtimeClasspath(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency testCompile(Object... dependencyNotation)
    Dependency testCompile(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency testCompileOnly(Object... dependencyNotation)
    Dependency testCompileOnly(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ExternalDependency) Closure closure)
    Dependency testImplementation(Object... dependencyNotation)
    Dependency testImplementation(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency testRuntime(Object... dependencyNotation)
    Dependency testRuntime(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency testRuntimeOnly(Object... dependencyNotation)
    Dependency testRuntimeOnly(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency deploy(Object... dependencyNotation)
    Dependency deploy(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)
    Dependency earlib(Object... dependencyNotation)
    Dependency earlib(Object dependencyNotation, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value= ExternalDependency) Closure closure)

    void constraints(Action<? super DependencyConstraintHandler> configureAction)
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
    RewriteTestSpec include(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=FileTreeElement) Closure includeSpec)
    RewriteTestSpec exclude(String... excludes)
    RewriteTestSpec exclude(Iterable<String> excludes)
    RewriteTestSpec exclude(Spec<FileTreeElement> excludeSpec)
    RewriteTestSpec exclude(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=FileTreeElement) Closure excludeSpec)
    RewriteTestSpec setTestNameIncludePatterns(List<String> testNamePattern)
    FileCollection getTestClassesDirs()
    void setTestClassesDirs(FileCollection testClassesDirs)
    Set<String> getIncludes()
    RewriteTestSpec setIncludes(Iterable<String> includes)
    Set<String> getExcludes()
    RewriteTestSpec setExcludes(Iterable<String> excludes)
    TestFrameworkOptions options(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=TestFrameworkOptions) Closure testFrameworkConfigure)
    TestFrameworkOptions options(Action<? super TestFrameworkOptions> testFrameworkConfigure)
    void useJUnit()
    void useJUnit(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=JUnitOptions) Closure testFrameworkConfigure) // delegate
    void useJUnit(Action<? super JUnitOptions> testFrameworkConfigure)
    void useJUnitPlatform()
    void useJUnitPlatform(Action<? super JUnitPlatformOptions> testFrameworkConfigure)
    void useTestNG()
    void useTestNG(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=TestNGOptions) Closure testFrameworkConfigure)
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

interface ScriptHandlerSpec extends ScriptHandler {
    void repositories(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RepositoryHandlerSpec) Closure cl)
    void dependencies(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=DependencyHandlerSpec) Closure cl)
}

abstract class RewriteGradleProject extends groovy.lang.Script implements Project {
    Map ext

    // It would be more correct for ext to delegate to ExtraPropertiesExtension, but StaticTypeCheckingVisitor has problems with that
    abstract void buildscript(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ScriptHandlerSpec) Closure cl)
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

    abstract void apply(Map<String, String> args)
}
