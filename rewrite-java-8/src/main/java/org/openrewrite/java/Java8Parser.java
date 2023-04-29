/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

public class Java8Parser implements JavaParser {
    private final JavaParser delegate;

    Java8Parser(JavaParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Stream<J.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return delegate.parseInputs(sourceFiles, relativeTo, ctx);
    }

    @Override
    public JavaParser reset() {
        delegate.reset();
        return this;
    }

    @Override
    public JavaParser reset(Collection<URI> cus) {
        delegate.reset(cus);
        return this;
    }

    @Override
    public void setClasspath(Collection<Path> classpath) {
        delegate.setClasspath(classpath);
    }

    @Override
    public void setSourceSet(String sourceSet) {
        delegate.setSourceSet(sourceSet);
    }

    @Override
    public JavaSourceSet getSourceSet(ExecutionContext ctx) {
        return delegate.getSourceSet(ctx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JavaParser.Builder<Java8Parser, Builder> {

        @Nullable
        private static ClassLoader toolsClassLoader;

        @Nullable
        private static ClassLoader toolsAwareClassLoader;

        static synchronized void lazyInitClassLoaders() {
            if (toolsClassLoader != null && toolsAwareClassLoader != null) {
                return;
            }

            try {
                File tools = Paths.get(System.getProperty("java.home")).resolve("../lib/tools.jar").toFile();
                if (!tools.exists()) {
                    throw new IllegalStateException("To use Java8Parser, you must run the process with a JDK and not a JRE.");
                }

                toolsClassLoader = new URLClassLoader(new URL[]{tools.toURI().toURL()}, Java8Parser.class.getClassLoader());
                URLClassLoader appClassLoader = (URLClassLoader) Java8Parser.class.getClassLoader();

                toolsAwareClassLoader = new URLClassLoader(appClassLoader.getURLs(), toolsClassLoader) {
                    @Override
                    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                        if (!name.contains("ReloadableJava8") &&
                                !name.startsWith("com.sun.tools") &&
                                !name.startsWith("com.sun.source") &&
                                !name.contains("ReloadableTypeMapping")) {
                            return toolsClassLoader.loadClass(name);
                        }

                        Class<?> loadedClass = findLoadedClass(name);

                        if (loadedClass == null) {
                            try {
                                loadedClass = findClass(name);
                            } catch (ClassNotFoundException e) {
                                loadedClass = super.loadClass(name, resolve);
                            }
                        }

                        if (resolve) {
                            resolveClass(loadedClass);
                        }

                        return loadedClass;
                    }
                };
            } catch (MalformedURLException e) {
                throw new IllegalStateException("To use Java8Parser, you must run the process with a JDK and not a JRE.", e);
            }
        }

        @Override
        public Java8Parser build() {
            lazyInitClassLoaders();

            try {
                // need to reverse this parent/child relationship
                Class<?> reloadableParser = Class.forName("org.openrewrite.java.ReloadableJava8Parser", true,
                        toolsAwareClassLoader);

                Constructor<?> delegateParserConstructor = reloadableParser
                        .getDeclaredConstructor(Collection.class, Collection.class, Collection.class, Charset.class,
                                Boolean.TYPE, Collection.class, JavaTypeCache.class);

                delegateParserConstructor.setAccessible(true);

                JavaParser delegate = (JavaParser) delegateParserConstructor
                        .newInstance(classpath, classBytesClasspath, dependsOn, charset, logCompilationWarningsAndErrors, styles, javaTypeCache);

                return new Java8Parser(delegate);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to construct Java8Parser.", e);
            }
        }
    }
}
