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
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class Java11Parser implements JavaParser {
    private final JavaParser delegate;

    Java11Parser(JavaParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<J.CompilationUnit> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return delegate.parseInputs(sourceFiles, relativeTo, ctx);
    }

    @Override
    public JavaParser reset() {
        delegate.reset();
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

    public static class Builder extends JavaParser.Builder<Java11Parser, Builder> {

        @Nullable
        private static ClassLoader moduleClassLoader;

        static synchronized void lazyInitClassLoaders() {
            if (moduleClassLoader != null) {
                return;
            }

            ClassLoader appClassLoader = Java11Parser.class.getClassLoader();
            moduleClassLoader = new UnrestrictedModuleClassLoader(appClassLoader);

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
                        .newInstance(logCompilationWarningsAndErrors, classpath, classBytesClasspath, dependsOn, charset, styles, javaTypeCache);

                return new Java11Parser(delegate);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to construct Java11Parser.", e);
            }
        }
    }

    /**
     * Rewrite's JavaParser is reliant on java's compiler internal classes that are now encapsulated within Java's
     * module system. Starting in Java 17, the JVM now enforces strong encapsulation of these internal classes and
     * default behavior is to throw a security exception when attempting to use these internal classes. This classloader
     * circumvents these security restrictions by isolating Rewrite's Java 17 parser implementation classes and then
     * loading any of the internal classes directly from the .jmod files.
     *
     * NOTE: Any classes in the package "org.openrewrite.java.isolated" will be loaded into this isolated classloader.
     */
    private static class UnrestrictedModuleClassLoader extends ClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }

        final List<Path> modules;

        private UnrestrictedModuleClassLoader(ClassLoader parentClassloader) {
            super(parentClassloader);

            //A list of modules to load internal classes from
            final FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
            modules = List.of(
                    fs.getPath("modules", "jdk.compiler"),
                    fs.getPath("modules", "java.compiler"),
                    fs.getPath("modules", "java.base")
            );
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {

            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    return c;
                }

                String internalName = name.replace('.', '/') + ".class";

                //If the class is in the package "org.openrewrite.java.internal", load it from this class loader.
                Class<?> _class = loadIsolatedClass(name);
                if (_class != null) {
                    return _class;
                }

                //Otherwise look for internal classes in the list of modules.
                if (name.startsWith("com.sun") || name.startsWith("sun")) {
                    try {
                        for (Path path : modules) {
                            Path classFile = path.resolve(internalName);
                            if (Files.exists(classFile)) {
                                byte[] bytes = Files.readAllBytes(classFile);
                                return defineClass(name, bytes, 0, bytes.length);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return super.loadClass(name);
        }

        @Nullable
        private Class<?> loadIsolatedClass(String className) {
            if (!className.startsWith("org.openrewrite.java.isolated")) {
                return null;
            }
            String internalName = className.replace('.', '/') + ".class";
            URL url = Java11Parser.class.getClassLoader().getResource(internalName);
            if (url == null) {
                return null;
            }

            try (InputStream stream = url.openStream()) {
                ByteArrayOutputStream classBytes = new ByteArrayOutputStream();
                byte[] bytes = new byte[4096];
                int bytesRead;
                while ((bytesRead = stream.read(bytes)) > 0) {
                    classBytes.write(bytes, 0, bytesRead);
                }
                return defineClass(className, classBytes.toByteArray(), 0, classBytes.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
