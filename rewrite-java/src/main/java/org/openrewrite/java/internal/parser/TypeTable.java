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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

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
 * To read a compressed type table file (which is compressed with zlib), the following command can be used:
 * <code>printf "\x1f\x8b\x08\x00\x00\x00\x00\x00" |cat - types.tsv.zip |gzip -dc</code>
 * It prepends the gzip magic header onto the zlib data and used gzip to decompress.
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
        try (InputStream is = findCaller().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            return is == null ? null : new TypeTable(ctx, is, artifactNames);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public TypeTable(ExecutionContext ctx, InputStream is, Collection<String> artifactNames) {
        try (InputStream inflate = new InflaterInputStream(is)) {
            new Reader(ctx).read(inflate, artifactsNotYetWritten(artifactNames));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Collection<String> artifactsNotYetWritten(Collection<String> artifactNames) {
        Collection<String> notWritten = new ArrayList<>(artifactNames);
        for (String artifactName : artifactNames) {
            for (GroupArtifactVersion groupArtifactVersion : classesDirByArtifact.keySet()) {
                if (Pattern.compile(artifactName + ".*")
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
        private @Nullable GroupArtifactVersion gav;
        private final Map<ClassDefinition, List<Member>> membersByClassName = new HashMap<>();

        public void read(InputStream is, Collection<String> artifactNames) throws IOException {
            Set<Pattern> artifactNamePatterns = artifactNames.stream()
                    .map(name -> Pattern.compile(name + ".*"))
                    .collect(Collectors.toSet());

            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                in.lines().skip(1).forEach(line -> {
                    String[] fields = line.split("\t", -1);
                    GroupArtifactVersion rowGav = new GroupArtifactVersion(fields[0], fields[1], fields[2]);
                    if (!rowGav.equals(gav)) {
                        writeClassesDir();
                    }

                    String artifactVersion = fields[1] + "-" + fields[2];

                    for (Pattern artifactNamePattern : artifactNamePatterns) {
                        if (artifactNamePattern.matcher(artifactVersion).matches()) {
                            gav = rowGav;
                            break;
                        }
                    }

                    if (gav != null) {
                        Member member = new Member(
                                new ClassDefinition(
                                        Integer.parseInt(fields[3]),
                                        fields[4],
                                        fields[5].isEmpty() ? null : fields[5],
                                        fields[6].isEmpty() ? null : fields[6],
                                        fields[7].isEmpty() ? null : fields[7].split("\\|")
                                ),
                                Integer.parseInt(fields[8]),
                                fields[9],
                                fields[10],
                                fields[11].isEmpty() ? null : fields[11],
                                fields[12].isEmpty() ? null : fields[12].split("\\|")
                        );
                        membersByClassName
                                .computeIfAbsent(member.getClassDefinition(), cd -> new ArrayList<>())
                                .add(member);
                    }
                });
            }
            writeClassesDir();
        }

        private void writeClassesDir() {
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
                        cw : new CheckClassAdapter(cw);

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
                    classWriter.visitInnerClass(
                            innerClass.getName(),
                            classDef.getName(),
                            innerClass.getName().substring(innerClass.getName().lastIndexOf('$') + 1),
                            innerClass.getAccess()
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

            gav = null;
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
        return new Writer(out);
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
        private final DeflaterOutputStream deflater;

        public Writer(OutputStream out) {
            this.deflater = new DeflaterOutputStream(out);
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

                                    @Override
                                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                        classDefinition = classDefinition(access, name, signature, superName, interfaces);
                                        super.visit(version, access, name, signature, superName, interfaces);
                                    }

                                    @Override
                                    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                        requireNonNull(classDefinition).writeField(access, name, descriptor, signature);
                                        return super.visitField(access, name, descriptor, signature, value);
                                    }

                                    @Override
                                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                                     String signature, String[] exceptions) {
                                        requireNonNull(classDefinition).writeMethod(access, name, descriptor, signature, null, exceptions);
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

            public ClassDefinition classDefinition(int access, String name, @Nullable String signature,
                                                   String superclassName, String @Nullable [] superinterfaceSignatures) {
                return new ClassDefinition(this, access, name, signature, superclassName, superinterfaceSignatures);
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

            public void writeMethod(int access, String name, String descriptor,
                                    @Nullable String signature,
                                    String @Nullable [] parameterNames,
                                    String @Nullable [] exceptions) {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0) {
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
                }
            }

            public void writeField(int access, String name, String descriptor,
                                   @Nullable String signature) {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0) {
                    // Fits into the same table structure
                    writeMethod(access, name, descriptor, signature, null, null);
                }
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
