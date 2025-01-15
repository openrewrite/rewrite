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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.internal.JavaTypeCache;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

public class Java11Parser implements JavaParser {
    private final JavaParser delegate;

    Java11Parser(JavaParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JavaParser.Builder<Java11Parser, Builder> {

        @Nullable
        private static ClassLoader moduleClassLoader;

        static synchronized void lazyInitClassLoaders() {
            if (moduleClassLoader != null) {
                return;
            }

            ClassLoader appClassLoader = Java11Parser.class.getClassLoader();
            moduleClassLoader = new JavaUnrestrictedClassLoader(appClassLoader);
        }

        @Override
        public Java11Parser build() {
            lazyInitClassLoaders();

            try {
                //Load the parser implementation use the unrestricted module classloader.
                Class<?> parserImplementation = Class.forName("org.openrewrite.java.isolated.ReloadableJava11Parser", true, moduleClassLoader);

                Constructor<?> parserConstructor = parserImplementation
                        .getDeclaredConstructor(Boolean.TYPE, Collection.class, Collection.class, Collection.class, Charset.class,
                                Collection.class, JavaTypeCache.class);

                parserConstructor.setAccessible(true);

                JavaParser delegate = (JavaParser) parserConstructor
                        .newInstance(logCompilationWarningsAndErrors, resolvedClasspath(), classBytesClasspath, dependsOn, charset, styles, javaTypeCache);

                return new Java11Parser(delegate);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to construct Java11Parser.", e);
            }
        }
    }
}
