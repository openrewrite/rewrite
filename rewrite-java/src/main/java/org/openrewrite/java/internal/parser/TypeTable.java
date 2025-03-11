/*
 * Copyright 2025 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaParserExecutionContextView;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.openrewrite.java.internal.parser.JavaParserCaller.findCaller;

/**
 * Type tables are written as a TSV file with the following columns:
 * <ul>
 *     <li>groupId</li>
 *     <li>artifactId</li>
 *     <li>version</li>
 *     <li>classAccess</li>
 *     <li>className</li>
 *     <li>classSignature</li>
 *     <li>classSuperclassSignature</li>
 *     <li>classSuperinterfaceSignatures[]</li>
 *     <li>access</li>
 *     <li>memberName</li>
 *     <li>descriptor</li>
 *     <li>signature</li>
 *     <li>parameterNames</li>
 *     <li>exceptions[]</li>
 * </ul>
 * <p>
 * Descriptor and signature are in <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3">JVMS 4.3</a> format.
 * Because these type tables could get fairly large, the format is optimized for fast record access. TSV was chosen over CSV for its
 * simplicity and for the reasons cited <a href="https://github.com/eBay/tsv-utils/blob/master/docs/comparing-tsv-and-csv.md">here</a>.
 * <p>
 * There is of course a lot of duplication in the class and GAV columns, but compression cuts down on
 * the disk impact of that and the value is an overall single table representation.
 * <p>
 * To read a compressed type table file (which is compressed with gzip), the following command can be used:
 * <code>gzcat types.tsv.zip</code>.
 */
@Incubating(since = "8.44.0")
@Value
public class TypeTable implements JavaParserClasspathLoader {
    /**
     * Verifies that the bytecodes written out for the types represented in a type table
     * will not be invalid and therefore rejected by the JVM verifier when used in a compilation
     * step.
     */
    public static final String VERIFY_CLASS_WRITING = "org.openrewrite.java.TypeTableClassWritingVerification";

    public static final String DEFAULT_RESOURCE_PATH = "META-INF/rewrite/classpath.tsv.zip";

    private static final Map<GroupArtifactVersion, Path> classesDirByArtifact = new LinkedHashMap<>();

    public static @Nullable TypeTable fromClasspath(ExecutionContext ctx, Collection<String> artifactNames) {
        try {
            Enumeration<URL> resources = findCaller().getClassLoader().getResources(DEFAULT_RESOURCE_PATH);
            if (resources.hasMoreElements()) {
                return new TypeTable(ctx, resources, artifactNames);
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    TypeTable(ExecutionContext ctx, URL url, Collection<String> artifactNames) {
        read(url, artifactNames, ctx);
    }

    TypeTable(ExecutionContext ctx, Enumeration<URL> resources, Collection<String> artifactNames) {
        while (resources.hasMoreElements()) {
            read(resources.nextElement(), artifactNames, ctx);
        }
    }

    private static void read(URL url, Collection<String> artifactNames, ExecutionContext ctx) {
        try (InputStream is = url.openStream(); InputStream inflate = new GZIPInputStream(is)) {
            new Reader(ctx).read(inflate, artifactsNotYetWritten(artifactNames));
        } catch (ZipException e) {
            // Fallback to `InflaterInputStream` for older files created as raw zlib data using DeflaterOutputStream
            try (InputStream is = url.openStream(); InputStream inflate = new InflaterInputStream(is)) {
                new Reader(ctx).read(inflate, artifactsNotYetWritten(artifactNames));
            } catch (IOException e1) {
                throw new UncheckedIOException(e1);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Collection<String> artifactsNotYetWritten(Collection<String> artifactNames) {
        Collection<String> notWritten = new ArrayList<>(artifactNames);
        for (String artifactName : artifactNames) {
            Pattern artifactPattern = Pattern.compile(artifactName + ".*");
            for (GroupArtifactVersion groupArtifactVersion : classesDirByArtifact.keySet()) {
                if (artifactPattern
                        .matcher(groupArtifactVersion.getArtifactId() + "-" + groupArtifactVersion.getVersion())
                        .matches()) {
                    notWritten.remove(artifactName);
                }
            }
        }
        return notWritten;
    }

    /**
     * Reads a type table from the classpath, and writes classes directories to disk for matching artifact names.
     */
    @RequiredArgsConstructor
    static class Reader {
        private final ExecutionContext ctx;

        public void read(InputStream is, Collection<String> artifactNames) throws IOException {
            if (artifactNames.isEmpty()) {
                // could be empty due to the filtering in `artifactsNotYetWritten()`
                return;
            }

            Set<Pattern> artifactNamePatterns = artifactNames.stream()
                    .map(name -> Pattern.compile(name + ".*"))
                    .collect(Collectors.toSet());

            AtomicReference<@Nullable GroupArtifactVersion> matchedGav = new AtomicReference<>();
            Map<ClassDefinition, List<Member>> membersByClassName = new HashMap<>();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                AtomicReference<@Nullable GroupArtifactVersion> lastGav = new AtomicReference<>();
                in.lines().skip(1).forEach(line -> {
                    String[] fields = line.split("\t", -1);
                    GroupArtifactVersion rowGav = new GroupArtifactVersion(fields[0], fields[1], fields[2]);

                    if (!Objects.equals(rowGav, lastGav.get())) {
                        writeClassesDir(matchedGav.get(), membersByClassName);
                        matchedGav.set(null);
                        membersByClassName.clear();

                        String artifactVersion = fields[1] + "-" + fields[2];

                        for (Pattern artifactNamePattern : artifactNamePatterns) {
                            if (artifactNamePattern.matcher(artifactVersion).matches()) {
                                matchedGav.set(rowGav);
                                break;
                            }
                        }
                    }
                    lastGav.set(rowGav);

                    if (matchedGav.get() != null) {
                        ClassDefinition classDefinition = new ClassDefinition(
                                Integer.parseInt(fields[3]),
                                fields[4],
                                fields[5].isEmpty() ? null : fields[5],
                                fields[6].isEmpty() ? null : fields[6],
                                fields[7].isEmpty() ? null : fields[7].split("\\|")
                        );
                        List<Member> classMembers = membersByClassName.computeIfAbsent(classDefinition, cd -> new ArrayList<>());
                        int memberAccess = Integer.parseInt(fields[8]);
                        if (memberAccess != -1) {
                            classMembers.add(new Member(
                                    classDefinition,
                                    memberAccess,
                                    fields[9],
                                    fields[10],
                                    fields[11].isEmpty() ? null : fields[11],
                                    fields[12].isEmpty() ? null : fields[12].split("\\|")
                            ));
                        }
                    }
                });
            }

            writeClassesDir(matchedGav.get(), membersByClassName);
        }

        private void writeClassesDir(@Nullable GroupArtifactVersion gav, Map<ClassDefinition, List<Member>> membersByClassName) {
            if (gav == null) {
                return;
            }

            Path classesDir = getClassesDir(ctx, gav);
            classesDirByArtifact.put(gav, classesDir);

            membersByClassName.forEach((classDef, members) -> {
                Path classFile = classesDir.resolve(classDef.getName() + ".class");
                if (!Files.exists(classFile.getParent()) && !classFile.getParent().toFile().mkdirs()) {
                    throw new UncheckedIOException(new IOException("Failed to create directory " + classesDir.getParent()));
                }

                ClassWriter cw = new ClassWriter(0);
                ClassVisitor classWriter = ctx.getMessage(VERIFY_CLASS_WRITING, false) ?
                        new CheckClassAdapter(cw) : cw;

                classWriter.visit(
                        V1_8,
                        classDef.getAccess(),
                        classDef.getName(),
                        classDef.getSignature(),
                        classDef.getSuperclassSignature(),
                        classDef.getSuperinterfaceSignatures()
                );

                Set<ClassDefinition> innerClasses = new HashSet<>();
                for (ClassDefinition innerClassDef : membersByClassName.keySet()) {
                    if (innerClassDef.getName().contains("$")) {
                        innerClasses.add(innerClassDef);
                    }
                }

                for (ClassDefinition innerClass : innerClasses) {
                    int lastIndexOf$ = innerClass.getName().lastIndexOf('$');
                    classWriter.visitInnerClass(
                            innerClass.getName(),
                            innerClass.getName().substring(0, lastIndexOf$),
                            innerClass.getName().substring(lastIndexOf$ + 1),
                            innerClass.getAccess() & 30239
                    );
                }

                for (Member member : members) {
                    if (member.getDescriptor().contains("(")) {
                        classWriter
                                .visitMethod(
                                        member.getAccess(),
                                        member.getName(),
                                        member.getDescriptor(),
                                        member.getSignature(),
                                        member.getExceptions()
                                )
                                .visitEnd();
                    } else {
                        classWriter
                                .visitField(
                                        member.getAccess(),
                                        member.getName(),
                                        member.getDescriptor(),
                                        member.getSignature(),
                                        null
                                )
                                .visitEnd();
                    }
                }

                try {
                    Files.write(classFile, cw.toByteArray());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static Path getClassesDir(ExecutionContext ctx, GroupArtifactVersion gav) {
        Path jarsFolder = JavaParserExecutionContextView.view(ctx)
                .getParserClasspathDownloadTarget().toPath().resolve(".tt");
        if (!Files.exists(jarsFolder) && !jarsFolder.toFile().mkdirs()) {
            throw new UncheckedIOException(new IOException("Failed to create directory " + jarsFolder));
        }

        Path classesDir = jarsFolder;
        for (String g : gav.getGroupId().split("\\.")) {
            classesDir = classesDir.resolve(g);
        }
        classesDir = classesDir.resolve(gav.getArtifactId()).resolve(gav.getVersion());

        if (!Files.exists(classesDir) && !classesDir.toFile().mkdirs()) {
            throw new UncheckedIOException(new IOException("Failed to create directory " + classesDir));
        }

        return classesDir;
    }

    public static Writer newWriter(OutputStream out) {
        try {
            return new Writer(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public @Nullable Path load(String artifactName) {
        for (Map.Entry<GroupArtifactVersion, Path> gavAndClassesDir : classesDirByArtifact.entrySet()) {
            GroupArtifactVersion gav = gavAndClassesDir.getKey();
            if (Pattern.compile(artifactName + ".*")
                    .matcher(gav.getArtifactId() + "-" + gav.getVersion())
                    .matches()) {
                return gavAndClassesDir.getValue();
            }
        }
        return null;
    }

    public static class Writer implements AutoCloseable {
        private final PrintStream out;
        private final GZIPOutputStream deflater;

        public Writer(OutputStream out) throws IOException {
            this.deflater = new GZIPOutputStream(out);
            this.out = new PrintStream(deflater);
            this.out.println("groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions");
        }

        public Jar jar(String groupId, String artifactId, String version) {
            return new Jar(groupId, artifactId, version);
        }

        @Override
        public void close() throws IOException {
            deflater.flush();
            out.close();
        }

        @Value
        public class Jar {
            String groupId;
            String artifactId;
            String version;

            public void write(Path jar) {
                try (JarFile jarFile = new JarFile(jar.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class")) {
                            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                new ClassReader(inputStream).accept(new ClassVisitor(Opcodes.ASM9) {
                                    @Nullable
                                    ClassDefinition classDefinition;
                                    boolean wroteFieldOrMethod;

                                    @Override
                                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                        classDefinition = new ClassDefinition(Jar.this, access, name, signature, superName, interfaces);
                                        wroteFieldOrMethod = false;
                                        super.visit(version, access, name, signature, superName, interfaces);
                                        if (!wroteFieldOrMethod && !"module-info".equals(name)) {
                                            // No fields or methods, which can happen for marker annotations for example
                                            classDefinition.writeClass();
                                        }
                                    }

                                    @Override
                                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                        wroteFieldOrMethod |= requireNonNull(classDefinition)
                                                .writeField(access, name, descriptor, signature);
                                        return super.visitField(access, name, descriptor, signature, value);
                                    }

                                    @Override
                                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                                     String signature, String[] exceptions) {
                                        wroteFieldOrMethod |= requireNonNull(classDefinition)
                                                .writeMethod(access, name, descriptor, signature, null, exceptions);
                                        return super.visitMethod(access, name, descriptor, signature, exceptions);
                                    }
                                }, SKIP_CODE);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        @Value
        public class ClassDefinition {
            Jar jar;
            int classAccess;
            String className;

            @Nullable
            String classSignature;

            String classSuperclassName;
            String @Nullable [] classSuperinterfaceSignatures;

            public void writeClass() {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & classAccess) == 0) {
                    out.printf(
                            "%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s%n",
                            jar.groupId, jar.artifactId, jar.version,
                            classAccess, className,
                            classSignature == null ? "" : classSignature,
                            classSuperclassName,
                            classSuperinterfaceSignatures == null ? "" : String.join("|", classSuperinterfaceSignatures),
                            -1, null, null, null, null, null);
                }
            }

            public boolean writeMethod(int access, String name, String descriptor,
                                       @Nullable String signature,
                                       String @Nullable [] parameterNames,
                                       String @Nullable [] exceptions) {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0 && !name.equals("<clinit>")) {
                    out.printf(
                            "%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s%n",
                            jar.groupId, jar.artifactId, jar.version,
                            classAccess, className,
                            classSignature == null ? "" : classSignature,
                            classSuperclassName,
                            classSuperinterfaceSignatures == null ? "" : String.join("|", classSuperinterfaceSignatures),
                            access, name, descriptor,
                            signature == null ? "" : signature,
                            parameterNames == null ? "" : String.join("|", parameterNames),
                            exceptions == null ? "" : String.join("|", exceptions)
                    );
                    return true;
                }
                return false;
            }

            public boolean writeField(int access, String name, String descriptor, @Nullable String signature) {
                // Fits into the same table structure
                return writeMethod(access, name, descriptor, signature, null, null);
            }
        }
    }

    @Value
    private static class GroupArtifactVersion {
        String groupId;
        String artifactId;
        String version;
    }

    @Value
    private static class ClassDefinition {
        int access;
        String name;

        @Nullable
        String signature;

        @Nullable
        String superclassSignature;

        String @Nullable [] superinterfaceSignatures;
    }

    @Value
    private static class Member {
        ClassDefinition classDefinition;
        int access;
        String name;
        String descriptor;

        @Nullable
        String signature;

        String @Nullable [] exceptions;
    }
}
