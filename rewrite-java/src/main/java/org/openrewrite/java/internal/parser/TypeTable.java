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
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
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

    private static final Map<GroupArtifactVersion, CompletableFuture<Path>> classesDirByArtifact = new ConcurrentHashMap<>();

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
        Collection<String> missingArtifacts = artifactsNotYetWritten(artifactNames);
        if (missingArtifacts.isEmpty()) {
            // all artifacts have already been extracted
            return;
        }

        try (InputStream is = url.openStream(); InputStream inflate = new GZIPInputStream(is)) {
            new Reader(ctx).read(inflate, missingArtifacts);
        } catch (ZipException e) {
            // Fallback to `InflaterInputStream` for older files created as raw zlib data using DeflaterOutputStream
            try (InputStream is = url.openStream(); InputStream inflate = new InflaterInputStream(is)) {
                new Reader(ctx).read(inflate, missingArtifacts);
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
        private static final int NESTED_TYPE_ACCESS_MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED |
                Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_INTERFACE |
                Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_ANNOTATION |
                Opcodes.ACC_ENUM;

        private final ExecutionContext ctx;

        public void read(InputStream is, Collection<String> artifactNames) throws IOException {
            if (artifactNames.isEmpty()) {
                // could be empty due to the filtering in `artifactsNotYetWritten()`
                return;
            }

            Set<Pattern> artifactNamePatterns = artifactNames.stream()
                    .map(name -> Pattern.compile(name + ".*"))
                    .collect(toSet());

            AtomicReference<@Nullable GroupArtifactVersion> matchedGav = new AtomicReference<>();
            Map<String, ClassDefinition> classesByName = new HashMap<>();
            // nested types appear first in type tables and therefore not stored in a `ClassDefinition` field
            Map<String, List<ClassDefinition>> nestedTypesByOwner = new HashMap<>();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                AtomicReference<@Nullable GroupArtifactVersion> lastGav = new AtomicReference<>();
                in.lines().skip(1).forEach(line -> {
                    String[] fields = line.split("\t", -1);
                    GroupArtifactVersion rowGav = new GroupArtifactVersion(fields[0], fields[1], fields[2]);

                    if (!Objects.equals(rowGav, lastGav.get())) {
                        writeClassesDir(matchedGav.get(), classesByName, nestedTypesByOwner);
                        matchedGav.set(null);
                        classesByName.clear();
                        nestedTypesByOwner.clear();

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
                        String className = fields[4];
                        ClassDefinition classDefinition = classesByName.computeIfAbsent(className, name ->
                                new ClassDefinition(
                                        Integer.parseInt(fields[3]),
                                        name,
                                        fields[5].isEmpty() ? null : fields[5],
                                        fields[6].isEmpty() ? null : fields[6],
                                        fields[7].isEmpty() ? null : fields[7].split("\\|")
                                ));
                        int lastIndexOf$ = className.lastIndexOf('$');
                        if (lastIndexOf$ != -1) {
                            String ownerName = className.substring(0, lastIndexOf$);
                            nestedTypesByOwner.computeIfAbsent(ownerName, k -> new ArrayList<>(4))
                                    .add(classDefinition);
                        }
                        int memberAccess = Integer.parseInt(fields[8]);
                        if (memberAccess != -1) {
                            classDefinition.addMember(new Member(
                                    classDefinition,
                                    memberAccess,
                                    fields[9],
                                    fields[10],
                                    fields[11].isEmpty() ? null : fields[11],
                                    fields[12].isEmpty() ? null : fields[12].split("\\|"),
                                    fields[13].isEmpty() ? null : fields[13].split("\\|")
                            ));
                        }
                    }
                });
            }

            writeClassesDir(matchedGav.get(), classesByName, nestedTypesByOwner);
        }

        private void writeClassesDir(@Nullable GroupArtifactVersion gav, Map<String, ClassDefinition> classes, Map<String, List<ClassDefinition>> nestedTypesByOwner) {
            if (gav == null) {
                return;
            }

            CompletableFuture<@Nullable Path> future = new CompletableFuture<>();
            if (classesDirByArtifact.putIfAbsent(gav, future) != null) {
                // is already being written (by concurrent thread)
                return;
            }

            Path classesDir = getClassesDir(ctx, gav);
            try {
                classes.values().forEach(classDef -> {
                    Path classFile = classesDir.resolve(classDef.getName() + ".class");
                    if (!classFile.getParent().toFile().mkdirs() && !Files.exists(classFile.getParent())) {
                        throw new UncheckedIOException(new IOException("Failed to create directory " + classesDir.getParent()));
                    }

                    ClassWriter cw = new ClassWriter(COMPUTE_MAXS);
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

                    for (ClassDefinition innerClassDef : nestedTypesByOwner.getOrDefault(classDef.getName(), emptyList())) {
                        classWriter.visitInnerClass(
                                innerClassDef.getName(),
                                classDef.getName(),
                                innerClassDef.getName().substring(classDef.getName().length() + 1),
                                innerClassDef.getAccess() & NESTED_TYPE_ACCESS_MASK
                        );
                    }

                    for (Member member : classDef.getMembers()) {
                        if (member.getDescriptor().contains("(")) {
                            MethodVisitor mv = classWriter
                                    .visitMethod(
                                            member.getAccess(),
                                            member.getName(),
                                            member.getDescriptor(),
                                            member.getSignature(),
                                            member.getExceptions()
                                    );
                            String[] parameterNames = member.getParameterNames();
                            if (parameterNames != null) {
                                for (String parameterName : parameterNames) {
                                    mv.visitParameter(parameterName, 0);
                                }
                            }
                            writeMethodBody(member, mv);
                            mv.visitEnd();
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
                future.complete(classesDir);
            } catch (Exception e) {
                future.completeExceptionally(e);
                classesDirByArtifact.remove(gav);
            }
        }

        private void writeMethodBody(Member member, MethodVisitor mv) {
            if ((member.getAccess() & Opcodes.ACC_ABSTRACT) == 0) {
                mv.visitCode();

                if ("<init>".equals(member.getName())) {
                    // constructors: the called super constructor doesn't need to exist; we only need to pass JVM class verification
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, member.getClassDefinition().getSuperclassSignature(), "<init>", "()V", false);
                }

                Type returnType = Type.getReturnType(member.getDescriptor());

                switch (returnType.getSort()) {
                    case Type.VOID:
                        mv.visitInsn(Opcodes.RETURN);
                        break;

                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        mv.visitInsn(Opcodes.ICONST_0); // Push default value (0)
                        mv.visitInsn(Opcodes.IRETURN);  // Return integer-compatible value
                        break;

                    case Type.LONG:
                        mv.visitInsn(Opcodes.LCONST_0); // Push default value (0L)
                        mv.visitInsn(Opcodes.LRETURN);
                        break;

                    case Type.FLOAT:
                        mv.visitInsn(Opcodes.FCONST_0); // Push default value (0.0f)
                        mv.visitInsn(Opcodes.FRETURN);
                        break;

                    case Type.DOUBLE:
                        mv.visitInsn(Opcodes.DCONST_0); // Push default value (0.0d)
                        mv.visitInsn(Opcodes.DRETURN);
                        break;

                    case Type.OBJECT:
                    case Type.ARRAY:
                        mv.visitInsn(Opcodes.ACONST_NULL); // Push default value (null)
                        mv.visitInsn(Opcodes.ARETURN);
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown return type: " + returnType);
                }
                mv.visitMaxs(0, 0);
            }
        }
    }

    private static Path getClassesDir(ExecutionContext ctx, GroupArtifactVersion gav) {
        Path jarsFolder = JavaParserExecutionContextView.view(ctx)
                .getParserClasspathDownloadTarget().toPath().resolve(".tt");
        if (!jarsFolder.toFile().mkdirs() && !Files.exists(jarsFolder)) {
            throw new UncheckedIOException(new IOException("Failed to create directory " + jarsFolder));
        }

        Path classesDir = jarsFolder;
        for (String g : gav.getGroupId().split("\\.")) {
            classesDir = classesDir.resolve(g);
        }
        classesDir = classesDir.resolve(gav.getArtifactId()).resolve(gav.getVersion());

        if (!classesDir.toFile().mkdirs() && !Files.exists(classesDir)) {
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
        for (Map.Entry<GroupArtifactVersion, CompletableFuture<Path>> gavAndClassesDir : classesDirByArtifact.entrySet()) {
            GroupArtifactVersion gav = gavAndClassesDir.getKey();
            if (Pattern.compile(artifactName + ".*")
                    .matcher(gav.getArtifactId() + "-" + gav.getVersion())
                    .matches()) {
                return gavAndClassesDir.getValue().join();
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
                                    final List<String> collectedParameterNames = new ArrayList<>();

                                    @Override
                                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                        int lastIndexOf$ = name.lastIndexOf('$');
                                        if (lastIndexOf$ != -1 && lastIndexOf$ < name.length() - 1 && !Character.isJavaIdentifierStart(name.charAt(lastIndexOf$ + 1))) {
                                            // skip anonymous subclasses
                                            classDefinition = null;
                                        } else {
                                            classDefinition = new ClassDefinition(Jar.this, access, name, signature, superName, interfaces);
                                            wroteFieldOrMethod = false;
                                            super.visit(version, access, name, signature, superName, interfaces);
                                            if (!wroteFieldOrMethod && !"module-info".equals(name)) {
                                                // No fields or methods, which can happen for marker annotations for example
                                                classDefinition.writeClass();
                                            }
                                        }
                                    }

                                    @Override
                                    public @Nullable FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                        if (classDefinition != null) {
                                            wroteFieldOrMethod |= classDefinition
                                                    .writeField(access, name, descriptor, signature);
                                        }

                                        return null;
                                    }

                                    @Override
                                    public @Nullable MethodVisitor visitMethod(int access, @Nullable String name, String descriptor,
                                                                               String signature, String[] exceptions) {
                                        // Repeating check from `writeMethod()` for performance reasons
                                        if (classDefinition != null && ((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0 &&
                                                name != null && !"<clinit>".equals(name)) {
                                            return new MethodVisitor(Opcodes.ASM9) {
                                                @Override
                                                public void visitParameter(@Nullable String name, int access) {
                                                    if (name != null) {
                                                        collectedParameterNames.add(name);
                                                    }
                                                }

                                                @Override
                                                public void visitEnd() {
                                                    wroteFieldOrMethod |= classDefinition
                                                            .writeMethod(access, name, descriptor, signature, collectedParameterNames.isEmpty() ? null : collectedParameterNames, exceptions);
                                                    collectedParameterNames.clear();
                                                }
                                            };
                                        }
                                        return null;
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
                            -1, "", "", "", "", "");
                }
            }

            public boolean writeMethod(int access, @Nullable String name, String descriptor,
                                       @Nullable String signature,
                                       @Nullable List<String> parameterNames,
                                       String @Nullable [] exceptions) {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0 && name != null && !"<clinit>".equals(name)) {
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
    @RequiredArgsConstructor
    private static class ClassDefinition {
        int access;
        String name;

        @Nullable
        String signature;

        @Nullable
        String superclassSignature;

        String @Nullable [] superinterfaceSignatures;

        @NonFinal
        @Nullable
        @ToString.Exclude
        List<Member> members;

        public List<Member> getMembers() {
            return members != null ? members : emptyList();
        }

        public void addMember(Member member) {
            if (members == null) {
                members = new ArrayList<>();
            }
            members.add(member);
        }
    }

    @Value
    private static class Member {
        ClassDefinition classDefinition;
        int access;
        String name;
        String descriptor;

        @Nullable
        String signature;

        String @Nullable [] parameterNames;
        String @Nullable [] exceptions;
    }
}
