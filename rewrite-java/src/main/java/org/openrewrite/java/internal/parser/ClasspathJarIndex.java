/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaParser;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Collections.emptyList;

/**
 * The class and source files of a jar on a Java parser's classpath by package. javac's file manager opens and walks
 * every jar on its classpath for each parser, so a parser whose execution context sets
 * {@link JavaParser#SHARE_CLASSPATH_JAR_INDEX} lists its jars from an index read once per JVM.
 */
public final class ClasspathJarIndex {
    private static final URI PLACEHOLDER_URI = URI.create("file:///");

    /**
     * By path, file key, size and modification time, so that a jar replaced on disk is read again.
     */
    private static final Map<String, SoftReference<ClasspathJarIndex>> INDEXES = new ConcurrentHashMap<>();

    private final Path jar;
    private final Map<String, List<String>> entriesByPackage = new HashMap<>();
    private final Set<String> entries = new HashSet<>();
    private final boolean addsToClasspath;

    /**
     * Opened to read the first class javac completes from the jar, and closed once the index is collected.
     */
    private @Nullable ZipFile zip;

    private ClasspathJarIndex(Path jar) throws IOException {
        this.jar = jar;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                String name = entry.getName();
                JavaFileObject.Kind kind = kind(name);
                int slash = name.lastIndexOf('/');
                String dir = slash < 0 ? "" : name.substring(0, slash);
                if (!entry.isDirectory() && kind != null && isPackage(dir) &&
                    isClass(name.substring(slash + 1, name.length() - kind.extension.length()))) {
                    entriesByPackage.computeIfAbsent(dir.replace('/', '.'), d -> new ArrayList<>()).add(name);
                    entries.add(name);
                }
            }
            ZipEntry manifest = zip.getEntry(JarFile.MANIFEST_NAME);
            if (manifest == null) {
                addsToClasspath = false;
            } else {
                try (InputStream in = zip.getInputStream(manifest)) {
                    Attributes attributes = new Manifest(in).getMainAttributes();
                    addsToClasspath = attributes.getValue(Attributes.Name.CLASS_PATH) != null;
                }
            }
        }
    }

    /**
     * Indexes the jars at the end of a classpath, after its last directory and its last jar whose manifest adds to
     * the classpath. A file manager lists them after the entries that javac searches itself, which keeps the order in
     * which javac would search them all.
     */
    public static List<ClasspathJarIndex> indexTrailingJars(List<Path> classpath) {
        List<ClasspathJarIndex> trailing = new ArrayList<>();
        for (int i = classpath.size() - 1; i >= 0; i--) {
            ClasspathJarIndex index = of(classpath.get(i));
            if (index == null || index.addsToClasspath) {
                break;
            }
            trailing.add(index);
        }
        Collections.reverse(trailing);
        return trailing;
    }

    private static @Nullable ClasspathJarIndex of(Path entry) {
        if (!entry.toString().endsWith(".jar")) {
            return null;
        }
        String key;
        try {
            BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                return null;
            }
            key = entry.toAbsolutePath() + "|" + attributes.fileKey() + "|" + attributes.size() + "|" +
                  attributes.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return null;
        }
        SoftReference<ClasspathJarIndex> cached = INDEXES.get(key);
        ClasspathJarIndex index = cached == null ? null : cached.get();
        if (index == null) {
            try {
                index = new ClasspathJarIndex(entry);
            } catch (IOException e) {
                // javac reports a jar it can't read
                return null;
            }
            INDEXES.put(key, new SoftReference<>(index));
        }
        return index;
    }

    /**
     * javac's own listing of the classpath entries before {@code jars}, followed by the files of {@code packageName},
     * or of it and its subpackages, in the jars.
     */
    public static Iterable<JavaFileObject> list(Iterable<JavaFileObject> listed, List<ClasspathJarIndex> jars,
                                                String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse,
                                                Charset charset) {
        if (jars.isEmpty()) {
            return listed;
        }
        List<JavaFileObject> all = new ArrayList<>();
        for (JavaFileObject file : listed) {
            all.add(file);
        }
        for (ClasspathJarIndex jar : jars) {
            if (recurse) {
                for (Map.Entry<String, List<String>> pkg : jar.entriesByPackage.entrySet()) {
                    if (packageName.isEmpty() || pkg.getKey().equals(packageName) ||
                        pkg.getKey().startsWith(packageName + ".")) {
                        jar.add(pkg.getValue(), kinds, charset, all);
                    }
                }
            } else {
                jar.add(jar.entriesByPackage.getOrDefault(packageName, emptyList()), kinds, charset, all);
            }
        }
        return all;
    }

    /**
     * @return The file of a class or of its source by binary name in the first of {@code jars} that has it.
     */
    public static @Nullable JavaFileObject find(List<ClasspathJarIndex> jars, String binaryName,
                                                JavaFileObject.Kind kind, Charset charset) {
        String entryName = binaryName.replace('.', '/') + kind.extension;
        for (ClasspathJarIndex jar : jars) {
            if (jar.entries.contains(entryName)) {
                return new JarEntryFile(jar, entryName, kind, charset);
            }
        }
        return null;
    }

    /**
     * @return The files of a classpath location, followed by {@code jars}.
     */
    public static Iterable<? extends File> location(Iterable<? extends File> location, List<ClasspathJarIndex> jars) {
        if (jars.isEmpty()) {
            return location;
        }
        List<File> all = new ArrayList<>();
        for (File file : location) {
            all.add(file);
        }
        for (ClasspathJarIndex jar : jars) {
            all.add(jar.jar.toFile());
        }
        return all;
    }

    /**
     * @return The binary name of a file listed or found in an index, or null for another file.
     */
    public static @Nullable String binaryName(JavaFileObject file) {
        return file instanceof JarEntryFile ? ((JarEntryFile) file).binaryName : null;
    }

    private void add(List<String> names, Set<JavaFileObject.Kind> kinds, Charset charset,
                     Collection<JavaFileObject> into) {
        for (String name : names) {
            JavaFileObject.Kind kind = kind(name);
            if (kind != null && kinds.contains(kind)) {
                into.add(new JarEntryFile(this, name, kind, charset));
            }
        }
    }

    private synchronized ZipFile zip() throws IOException {
        if (zip == null) {
            zip = new ZipFile(jar.toFile());
        }
        return zip;
    }

    private static JavaFileObject.@Nullable Kind kind(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension)) {
            return JavaFileObject.Kind.CLASS;
        } else if (name.endsWith(JavaFileObject.Kind.SOURCE.extension)) {
            return JavaFileObject.Kind.SOURCE;
        }
        return null;
    }

    /**
     * Whether javac reads a jar directory as a package, which every part of its name has to be an identifier for.
     */
    private static boolean isPackage(String dir) {
        if (dir.isEmpty()) {
            return true;
        }
        for (String part : dir.split("/")) {
            if (!SourceVersion.isIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether javac reads a file of a package as a class, as it does a {@code package-info} but not a class file
     * whose name holds a space, such as one Kotlin compiles a function with spaces in its name to.
     */
    private static boolean isClass(String simpleName) {
        return SourceVersion.isIdentifier(simpleName) || "package-info".equals(simpleName);
    }

    private static final class JarEntryFile extends SimpleJavaFileObject {
        private final ClasspathJarIndex index;
        private final String entryName;
        private final Charset charset;
        private final String binaryName;

        JarEntryFile(ClasspathJarIndex index, String entryName, Kind kind, Charset charset) {
            // a jar entry's URI isn't the hierarchical one SimpleJavaFileObject requires, so toUri() makes its own
            super(PLACEHOLDER_URI, kind);
            this.index = index;
            this.entryName = entryName;
            this.charset = charset;
            this.binaryName = entryName.substring(0, entryName.length() - kind.extension.length()).replace('/', '.');
        }

        @Override
        public URI toUri() {
            return URI.create("jar:" + index.jar.toAbsolutePath().toUri() + "!/" + entryName);
        }

        @Override
        public String getName() {
            return index.jar + "(" + entryName + ")";
        }

        @Override
        public InputStream openInputStream() throws IOException {
            ZipFile zip = index.zip();
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new FileNotFoundException(getName());
            }
            return zip.getInputStream(entry);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            try (InputStream in = openInputStream()) {
                return StringUtils.readFully(in, charset);
            }
        }

        @Override
        public long getLastModified() {
            try {
                ZipEntry entry = index.zip().getEntry(entryName);
                return entry == null ? 0 : entry.getTime();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            String fileName = simpleName + kind.extension;
            return kind == getKind() && (entryName.equals(fileName) || entryName.endsWith("/" + fileName));
        }
    }
}
