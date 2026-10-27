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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Collections.emptyList;

/**
 * The class and source files of a jar on a Java parser's classpath by package. javac's file manager opens and walks
 * every jar on its classpath for each parser, so a jar is indexed once per JVM and shared by every parser's file
 * manager.
 */
public final class ClasspathJarIndex {
    private static final int MAX_JARS = 512;

    /**
     * By path, size and modification time, so that a jar replaced on disk is read again. An evicted jar keeps its file
     * open until the parsers that use it are collected.
     */
    private static final Map<String, Optional<ClasspathJarIndex>> INDEXES =
            new LinkedHashMap<String, Optional<ClasspathJarIndex>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Optional<ClasspathJarIndex>> eldest) {
                    return size() > MAX_JARS;
                }
            };

    @Getter
    private final Path jar;

    private final String jarUri;
    private final ZipFile zip;
    private final Map<String, List<ZipEntry>> entriesByPackage = new HashMap<>();
    private final boolean addsToClasspath;

    private ClasspathJarIndex(Path jar) throws IOException {
        this.jar = jar;
        this.jarUri = jar.toAbsolutePath().toUri().toString();
        this.zip = new ZipFile(jar.toFile());
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            int slash = name.lastIndexOf('/');
            String dir = slash < 0 ? "" : name.substring(0, slash);
            if (!entry.isDirectory() && kind(name) != null && isPackage(dir)) {
                entriesByPackage.computeIfAbsent(dir.replace('/', '.'), d -> new ArrayList<>()).add(entry);
            }
        }
        ZipEntry manifestEntry = zip.getEntry(JarFile.MANIFEST_NAME);
        if (manifestEntry == null) {
            this.addsToClasspath = false;
        } else {
            try (InputStream in = zip.getInputStream(manifestEntry)) {
                Attributes attributes = new Manifest(in).getMainAttributes();
                this.addsToClasspath = attributes.getValue(Attributes.Name.CLASS_PATH) != null;
            }
        }
    }

    /**
     * Indexes the jars at the end of a classpath, after its last directory and its last jar whose manifest adds to
     * the classpath. A file manager lists them after the entries that javac searches itself, which keeps the order in
     * which javac would search them all.
     */
    public static List<ClasspathJarIndex> indexTrailingJars(List<Path> classpath) {
        LinkedList<ClasspathJarIndex> trailing = new LinkedList<>();
        for (int i = classpath.size() - 1; i >= 0; i--) {
            ClasspathJarIndex index = of(classpath.get(i));
            if (index == null || index.addsToClasspath) {
                break;
            }
            trailing.addFirst(index);
        }
        return trailing.isEmpty() ? emptyList() : new ArrayList<>(trailing);
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
            key = entry.toAbsolutePath() + "|" + attributes.size() + "|" + attributes.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return null;
        }
        Optional<ClasspathJarIndex> index;
        synchronized (INDEXES) {
            index = INDEXES.get(key);
        }
        if (index == null) {
            try {
                index = Optional.of(new ClasspathJarIndex(entry));
            } catch (IOException e) {
                // javac reports a jar it can't read
                index = Optional.empty();
            }
            synchronized (INDEXES) {
                Optional<ClasspathJarIndex> raced = INDEXES.putIfAbsent(key, index);
                if (raced != null) {
                    index = raced;
                }
            }
        }
        return index.orElse(null);
    }

    /**
     * Adds the files of {@code packageName}, or of it and its subpackages, as javac's own listing of a jar does.
     */
    public void list(String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse, Charset charset,
                     Collection<JavaFileObject> into) {
        if (recurse) {
            for (Map.Entry<String, List<ZipEntry>> pkg : entriesByPackage.entrySet()) {
                if (packageName.isEmpty() || pkg.getKey().equals(packageName) ||
                    pkg.getKey().startsWith(packageName + ".")) {
                    add(pkg.getValue(), kinds, charset, into);
                }
            }
        } else {
            add(entriesByPackage.getOrDefault(packageName, emptyList()), kinds, charset, into);
        }
    }

    /**
     * @return The file of a class or of its source by binary name, or null when the jar has none.
     */
    public @Nullable JavaFileObject find(String binaryName, JavaFileObject.Kind kind, Charset charset) {
        int dot = binaryName.lastIndexOf('.');
        String packageName = dot < 0 ? "" : binaryName.substring(0, dot);
        String entryName = binaryName.replace('.', '/') + kind.extension;
        for (ZipEntry entry : entriesByPackage.getOrDefault(packageName, emptyList())) {
            if (entry.getName().equals(entryName)) {
                return new JarEntryFile(this, entry, kind, charset);
            }
        }
        return null;
    }

    /**
     * @return The binary name of a file this index listed, or null for another file.
     */
    public static @Nullable String binaryName(JavaFileObject file) {
        return file instanceof JarEntryFile ? ((JarEntryFile) file).binaryName : null;
    }

    private void add(List<ZipEntry> entries, Set<JavaFileObject.Kind> kinds, Charset charset,
                     Collection<JavaFileObject> into) {
        for (ZipEntry entry : entries) {
            JavaFileObject.Kind kind = kind(entry.getName());
            if (kind != null && kinds.contains(kind)) {
                into.add(new JarEntryFile(this, entry, kind, charset));
            }
        }
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

    private static final class JarEntryFile extends SimpleJavaFileObject {
        private final ClasspathJarIndex index;
        private final ZipEntry entry;
        private final Charset charset;
        private final String binaryName;
        private final URI uri;

        JarEntryFile(ClasspathJarIndex index, ZipEntry entry, Kind kind, Charset charset) {
            // SimpleJavaFileObject requires a hierarchical URI, which a jar entry's URI isn't
            super(URI.create("file:///" + entry.getName()), kind);
            this.index = index;
            this.entry = entry;
            this.charset = charset;
            String name = entry.getName();
            this.binaryName = name.substring(0, name.length() - kind.extension.length()).replace('/', '.');
            this.uri = URI.create("jar:" + index.jarUri + "!/" + name);
        }

        @Override
        public URI toUri() {
            return uri;
        }

        @Override
        public String getName() {
            return index.jar + "(" + entry.getName() + ")";
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return index.zip.getInputStream(entry);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            try (InputStream in = openInputStream()) {
                return StringUtils.readFully(in, charset);
            }
        }

        @Override
        public long getLastModified() {
            return entry.getTime();
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            String fileName = simpleName + kind.extension;
            return kind == getKind() &&
                   (entry.getName().equals(fileName) || entry.getName().endsWith("/" + fileName));
        }
    }
}
