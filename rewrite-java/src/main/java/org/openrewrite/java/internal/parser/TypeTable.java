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

import lombok.*;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.openrewrite.java.internal.parser.AnnotationSerializer.convertAnnotationValueToString;
import static org.openrewrite.java.internal.parser.AnnotationSerializer.serializeArray;
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
 *     <li>elementAnnotations</li>
 *     <li>parameterAnnotations[]</li>
 *     <li>typeAnnotations[]</li>
 *     <li>constantValue</li>
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
 * <code>gzcat types.tsv.gz</code>.
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

    public static final String DEFAULT_RESOURCE_PATH = "META-INF/rewrite/classpath.tsv.gz";

    private static final Map<GroupArtifactVersion, CompletableFuture<Path>> classesDirByArtifact = new ConcurrentHashMap<>();

    public static @Nullable TypeTable fromClasspath(ExecutionContext ctx, Collection<String> artifactNames) {
        try {
            ClassLoader classLoader = findCaller().getClassLoader();
            Vector<URL> combinedResources = new Vector<>();
            for (Enumeration<URL> e = classLoader.getResources(DEFAULT_RESOURCE_PATH); e.hasMoreElements(); ) {
                combinedResources.add(e.nextElement());
            }
            // TO-BE-REMOVED(2025-10-31) In the future we only want to support the `.gz` extension
            for (Enumeration<URL> e = classLoader.getResources(DEFAULT_RESOURCE_PATH.replace(".gz", ".zip")); e.hasMoreElements(); ) {
                combinedResources.add(e.nextElement());
            }

            if (!combinedResources.isEmpty()) {
                return new TypeTable(ctx, combinedResources.elements(), artifactNames);
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

        Reader.Options options = Reader.Options.builder().artifactPrefixes(artifactNames).build();
        try (InputStream is = url.openStream(); InputStream inflate = new GZIPInputStream(is)) {
            new Reader(ctx).read(inflate, options);
        } catch (ZipException e) {
            // Fallback to `InflaterInputStream` for older files created as raw zlib data using DeflaterOutputStream
            try (InputStream is = url.openStream(); InputStream inflate = new InflaterInputStream(is)) {
                new Reader(ctx).read(inflate, options);
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
    public static class Reader {

        /**
         * Options for controlling how type tables are read.
         * This allows for flexible filtering and processing of type table entries.
         * <p>
         * Uses a builder pattern for future extensibility without breaking changes.
         */
        @lombok.Builder(builderClassName = "Builder")
        @lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
        public static class Options {
            @lombok.Builder.Default
            @Getter(AccessLevel.PACKAGE)
            private final Predicate<String> artifactMatcher = gav -> true;

            /**
             * Creates options that match all artifacts.
             */
            public static Options matchAll() {
                return builder().build();
            }

            /**
             * Enhanced builder with convenience methods.
             */
            public static class Builder {

                /**
                 * Matches artifacts whose names start with any of the given prefixes.
                 * @param artifactPrefixes Collection of artifact name prefixes to match
                 */
                public Builder artifactPrefixes(Collection<String> artifactPrefixes) {
                    Set<Pattern> patterns = artifactPrefixes.stream()
                            .map(prefix -> Pattern.compile(prefix + ".*"))
                            .collect(toSet());
                    this.artifactMatcher(artifactVersion -> patterns.stream()
                            .anyMatch(pattern -> pattern.matcher(artifactVersion).matches()));
                    return this;
                }
            }
        }

        private static final int NESTED_TYPE_ACCESS_MASK = Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED |
                Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_INTERFACE |
                Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_ANNOTATION |
                Opcodes.ACC_ENUM;

        private final ExecutionContext ctx;

        public void read(InputStream is, Options options) throws IOException {
            parseTsvAndProcess(is, options, this::writeClassesDir);
        }

        /**
         * Read a type table and process classes with custom ClassVisitors instead of writing to disk.
         *
         * @param is The input stream containing the TSV data
         * @param options Options controlling how the type table is read
         * @param visitorSupplier Supplier to create a ClassVisitor for each class
         */
        public void read(InputStream is, Options options, Supplier<ClassVisitor> visitorSupplier) throws IOException {
            parseTsvAndProcess(is, options,
                    (gav, classes, nestedTypes) -> {
                        for (ClassDefinition classDef : classes.values()) {
                            processClass(classDef, nestedTypes.getOrDefault(classDef.getName(), emptyList()), visitorSupplier.get());
                        }
                    });
        }

        /**
         * Common TSV parsing logic used by both read() methods.
         * Parses the TSV and calls the processor for each GAV's classes.
         */
        public void parseTsvAndProcess(InputStream is, Options options,
                                        ClassesProcessor processor) throws IOException {
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
                        if (matchedGav.get() != null) {
                            processor.accept(matchedGav.get(), classesByName, nestedTypesByOwner);
                        }
                        matchedGav.set(null);
                        classesByName.clear();
                        nestedTypesByOwner.clear();

                        String artifactVersion = fields[1] + "-" + fields[2];

                        // Check if this artifact matches our predicate
                        if (options.getArtifactMatcher().test(artifactVersion)) {
                            matchedGav.set(rowGav);
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
                                        fields[7].isEmpty() ? null : fields[7].split("\\|"),
                                        fields.length > 14 && !fields[14].isEmpty() ? fields[14] : null,  // elementAnnotations - raw string (may have | delimiters)
                                        fields.length > 17 && !fields[17].isEmpty() ? fields[17] : null  // constantValue moved to column 17
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
                                    fields[13].isEmpty() ? null : fields[13].split("\\|"),
                                    fields.length > 14 && !fields[14].isEmpty() ? fields[14] : null,  // elementAnnotations - raw string
                                    fields.length > 15 && !fields[15].isEmpty() ? fields[15] : null,
                                    fields.length > 16 && !fields[16].isEmpty() ? TsvEscapeUtils.splitAnnotationList(fields[16], '|') : null,  // typeAnnotations - keep `|` delimiter between different type contexts
                                    fields.length > 17 && !fields[17].isEmpty() ? fields[17] : null
                            ));
                        }
                    }
                });
            }

            // Process final GAV if any
            if (matchedGav.get() != null) {
                processor.accept(matchedGav.get(), classesByName, nestedTypesByOwner);
            }
        }

        @FunctionalInterface
        interface ClassesProcessor {
            void accept(@Nullable GroupArtifactVersion gav, Map<String, ClassDefinition> classes,
                        Map<String, List<ClassDefinition>> nestedTypes);
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

                    processClass(classDef, nestedTypesByOwner.getOrDefault(classDef.getName(), emptyList()), classWriter);

                    try {
                        Files.write(classFile, cw.toByteArray());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                future.complete(classesDir);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }

        /**
         * Process a single class definition by feeding it to a ClassVisitor.
         * This contains the core logic for converting TypeTable data to ASM visitor calls.
         */
        private void processClass(ClassDefinition classDef, List<ClassDefinition> nestedTypes, ClassVisitor classVisitor) {
            classVisitor.visit(
                    V1_8,
                    classDef.getAccess(),
                    classDef.getName(),
                    classDef.getSignature(),
                    classDef.getSuperclassSignature(),
                    classDef.getSuperinterfaceSignatures()
            );

            // Apply annotations to the class
            if (classDef.getAnnotations() != null) {
                AnnotationApplier.applyAnnotations(classDef.getAnnotations(), classVisitor::visitAnnotation);
            }

            for (ClassDefinition innerClassDef : nestedTypes) {
                classVisitor.visitInnerClass(
                        innerClassDef.getName(),
                        classDef.getName(),
                        innerClassDef.getName().substring(classDef.getName().length() + 1),
                        innerClassDef.getAccess() & NESTED_TYPE_ACCESS_MASK
                );
            }

            for (Member member : classDef.getMembers()) {
                if (member.getDescriptor().startsWith("(")) {
                    MethodVisitor mv = classVisitor
                            .visitMethod(
                                    member.getAccess(),
                                    member.getName(),
                                    member.getDescriptor(),
                                    member.getSignature(),
                                    member.getExceptions()
                            );

                    if (mv != null) {
                        // Apply element annotations to the method
                        if (member.getAnnotations() != null) {
                            AnnotationApplier.applyAnnotations(member.getAnnotations(), mv::visitAnnotation);
                        }

                        // Apply parameter annotations
                        if (member.getParameterAnnotations() != null && !member.getParameterAnnotations().isEmpty()) {
                            // Parse dense format: "[annotations]||[annotations]|" where each position represents a parameter
                            // Empty positions mean no annotations for that parameter
                            String[] paramAnnotations = TsvEscapeUtils.splitAnnotationList(member.getParameterAnnotations(), '|');
                            for (int i = 0; i < paramAnnotations.length; i++) {
                                final int paramIndex = i;
                                String annotationsPart = paramAnnotations[i];
                                if (!annotationsPart.isEmpty()) {
                                    // Parse and apply the annotation sequence (no delimiters needed within)
                                    AnnotationApplier.applyAnnotations(annotationsPart,
                                            (descriptor, visible) -> mv.visitParameterAnnotation(paramIndex, descriptor, visible));
                                }
                            }
                        }

                        // Apply type annotations
                        if (member.getTypeAnnotations() != null) {
                            for (String typeAnnotation : member.getTypeAnnotations()) {
                                TypeAnnotationSupport.TypeAnnotationInfo info =
                                        TypeAnnotationSupport.TypeAnnotationInfo.parse(typeAnnotation);
                                AnnotationApplier.applyAnnotation(info.annotation,
                                        (descriptor, visible) -> mv.visitTypeAnnotation(info.typeRef, info.typePath, descriptor, visible));
                            }
                        }

                        String[] parameterNames = member.getParameterNames();
                        if (parameterNames != null) {
                            for (String parameterName : parameterNames) {
                                mv.visitParameter(parameterName, 0);
                            }
                        }

                        if (member.getConstantValue() != null) {
                            AnnotationVisitor annotationDefaultVisitor = mv.visitAnnotationDefault();
                            if (annotationDefaultVisitor != null) {
                                AnnotationSerializer.processAnnotationDefaultValue(
                                        annotationDefaultVisitor,
                                        AnnotationDeserializer.parseValue(member.getConstantValue())
                                );
                                annotationDefaultVisitor.visitEnd();
                            }
                        }

                        writeMethodBody(member, mv);
                        mv.visitEnd();
                    }
                } else {
                    // Determine the constant value for static final fields
                    // Only set constantValue for bytecode if it's a valid ConstantValue attribute type
                    Object constantValue = null;
                    if ((member.getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL) &&
                            member.getConstantValue() != null) {
                        Object parsedValue = AnnotationDeserializer.parseValue(member.getConstantValue());
                        // Only primitive types and strings can be ConstantValue attributes
                        if (isValidConstantValueType(parsedValue)) {
                            constantValue = parsedValue;
                        }
                    }

                    FieldVisitor fv = classVisitor
                            .visitField(
                                    member.getAccess(),
                                    member.getName(),
                                    member.getDescriptor(),
                                    member.getSignature(),
                                    constantValue
                            );

                    if (fv != null) {
                        // Apply element annotations to the field
                        if (member.getAnnotations() != null) {
                            AnnotationApplier.applyAnnotations(member.getAnnotations(), fv::visitAnnotation);
                        }

                        // Apply type annotations for fields
                        if (member.getTypeAnnotations() != null) {
                            for (String typeAnnotation : member.getTypeAnnotations()) {
                                TypeAnnotationSupport.TypeAnnotationInfo info =
                                        TypeAnnotationSupport.TypeAnnotationInfo.parse(typeAnnotation);
                                AnnotationApplier.applyAnnotation(info.annotation,
                                        (descriptor, visible) -> fv.visitTypeAnnotation(info.typeRef, info.typePath, descriptor, visible));
                            }
                        }

                        fv.visitEnd();
                    }
                }
            }

            classVisitor.visitEnd();
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

    private static boolean isValidConstantValueType(@Nullable Object value) {
        if (value == null) {
            return false; // null values cannot be ConstantValue attributes
        }
        return value instanceof String ||
                value instanceof Integer || value instanceof Long ||
                value instanceof Float || value instanceof Double ||
                value instanceof Boolean || value instanceof Character ||
                value instanceof Byte || value instanceof Short;
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
            this.out.println("groupId\tartifactId\tversion\tclassAccess\tclassName\tclassSignature\tclassSuperclassSignature\tclassSuperinterfaceSignatures\taccess\tname\tdescriptor\tsignature\tparameterNames\texceptions\telementAnnotations\tparameterAnnotations\ttypeAnnotations\tconstantValue");
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
                                writeClass(inputStream);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            /**
             * Write a single class from an InputStream containing bytecode.
             * This can be used for processing individual class files.
             *
             * @param classInputStream InputStream containing the class bytecode
             * @throws IOException if reading the class fails
             */
            public void writeClass(InputStream classInputStream) throws IOException {
                new ClassReader(classInputStream).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Nullable
                    ClassDefinition classDefinition;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                        int lastIndexOf$ = name.lastIndexOf('$');
                        if (lastIndexOf$ != -1 && lastIndexOf$ < name.length() - 1 && !Character.isJavaIdentifierStart(name.charAt(lastIndexOf$ + 1))) {
                            // skip anonymous subclasses
                            classDefinition = null;
                        } else {
                            classDefinition = new ClassDefinition(
                                    Jar.this,
                                    access,
                                    name,
                                    signature,
                                    superName,
                                    interfaces
                            );
                            super.visit(version, access, name, signature, superName, interfaces);
                        }
                    }

                    @Override
                    public @Nullable AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        if (classDefinition != null) {
                            return AnnotationCollectorHelper.createCollector(descriptor, requireNonNull(classDefinition).classAnnotations);
                        }
                        return null;
                    }

                    @Override
                    public @Nullable AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        if (classDefinition != null) {
                            List<String> tempCollector = new ArrayList<>();
                            AnnotationVisitor collector = AnnotationCollectorHelper.createCollector(descriptor, tempCollector);
                            return new AnnotationVisitor(Opcodes.ASM9, collector) {
                                @Override
                                public void visitEnd() {
                                    super.visitEnd();
                                    if (!tempCollector.isEmpty()) {
                                        String annotation = tempCollector.get(0);
                                        String formatted = TypeAnnotationSupport.formatTypeAnnotation(typeRef, typePath, annotation);
                                        classDefinition.classTypeAnnotations.add(formatted);
                                    }
                                }
                            };
                        }
                        return null;
                    }

                    @Override
                    public @Nullable FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                        if (classDefinition != null) {
                            Writer.Member member = new Writer.Member(access, name, descriptor, signature, null, null);

                            // Only store constant values that can be ConstantValue attributes in bytecode
                            if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL) &&
                                    isValidConstantValueType(value)) {
                                member.constantValue = AnnotationSerializer.convertConstantValueWithType(value, descriptor);
                            }

                            return new FieldVisitor(Opcodes.ASM9) {
                                @Override
                                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                    return AnnotationCollectorHelper.createCollector(descriptor, member.elementAnnotations);
                                }

                                @Override
                                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                                    List<String> tempCollector = new ArrayList<>();
                                    AnnotationVisitor collector = AnnotationCollectorHelper.createCollector(descriptor, tempCollector);
                                    return new AnnotationVisitor(Opcodes.ASM9, collector) {
                                        @Override
                                        public void visitEnd() {
                                            super.visitEnd();
                                            if (!tempCollector.isEmpty()) {
                                                String annotation = tempCollector.get(0);
                                                String formatted = TypeAnnotationSupport.formatTypeAnnotation(typeRef, typePath, annotation);
                                                member.typeAnnotations.add(formatted);
                                            }
                                        }
                                    };
                                }

                                @Override
                                public void visitEnd() {
                                    classDefinition.addField(member);
                                }
                            };
                        }

                        return null;
                    }

                    @Override
                    public @Nullable MethodVisitor visitMethod(int access, @Nullable String name, String descriptor,
                                                               @Nullable String signature, String @Nullable [] exceptions) {
                        // Repeating check from `writeMethod()` for performance reasons
                        if (classDefinition != null && ((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0 &&
                                name != null && !"<clinit>".equals(name)) {
                            Writer.Member member = new Writer.Member(access, name, descriptor, signature, exceptions, null);
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitParameter(@Nullable String name, int access) {
                                    if (name != null) {
                                        member.parameterNames.add(name);
                                    }
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                    return AnnotationCollectorHelper.createCollector(descriptor, member.elementAnnotations);
                                }

                                @Override
                                public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                                    List<String> tempCollector = new ArrayList<>();
                                    AnnotationVisitor collector = AnnotationCollectorHelper.createCollector(descriptor, tempCollector);
                                    // After collection, add to parameter annotations with parameter index
                                    return new AnnotationVisitor(Opcodes.ASM9, collector) {
                                        @Override
                                        public void visitEnd() {
                                            super.visitEnd();
                                            if (!tempCollector.isEmpty()) {
                                                // Format: "paramIndex:annotation"
                                                member.parameterAnnotations.add(parameter + ":" + tempCollector.get(0));
                                            }
                                        }
                                    };
                                }

                                @Override
                                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                                    List<String> tempCollector = new ArrayList<>();
                                    AnnotationVisitor collector = AnnotationCollectorHelper.createCollector(descriptor, tempCollector);
                                    return new AnnotationVisitor(Opcodes.ASM9, collector) {
                                        @Override
                                        public void visitEnd() {
                                            super.visitEnd();
                                            if (!tempCollector.isEmpty()) {
                                                String annotation = tempCollector.get(0);
                                                String formatted = TypeAnnotationSupport.formatTypeAnnotation(typeRef, typePath, annotation);
                                                member.typeAnnotations.add(formatted);
                                            }
                                        }
                                    };
                                }

                                @Override
                                public AnnotationVisitor visitAnnotationDefault() {
                                    List<String> nested = new ArrayList<>();
                                    // Collect default values for annotation methods
                                    return new AnnotationVisitor(Opcodes.ASM9) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            member.constantValue = convertAnnotationValueToString(value);
                                        }

                                        @Override
                                        public AnnotationVisitor visitArray(String name) {
                                            return new AnnotationVisitor(Opcodes.ASM9) {
                                                final List<String> arrayValues = new ArrayList<>();

                                                @Override
                                                public void visit(String name, Object value) {
                                                    arrayValues.add(convertAnnotationValueToString(value));
                                                }

                                                @Override
                                                public void visitEnd() {
                                                    member.constantValue = "[" + String.join(",", arrayValues) + "]";
                                                }
                                            };
                                        }

                                        @Override
                                        public void visitEnum(String name, String descriptor, String value) {
                                            member.constantValue = "e" + descriptor + "." + value;
                                        }

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                            return AnnotationCollectorHelper.createCollector(descriptor, nested);
                                        }

                                        @Override
                                        public void visitEnd() {
                                            if (!nested.isEmpty()) {
                                                member.constantValue = serializeArray(nested.toArray(new String[0]));
                                            }
                                        }
                                    };
                                }

                                @Override
                                public void visitEnd() {
                                    classDefinition.addMethod(member);
                                }
                            };
                        }
                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        if (classDefinition != null && !"module-info".equals(classDefinition.className)) {
                            // No fields or methods, which can happen for marker annotations for example
                            classDefinition.writeClass();
                        }
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            }
        }

        @Value
        class ClassDefinition {
            Jar jar;
            int classAccess;
            String className;

            @Nullable
            String classSignature;

            @Nullable
            String classSuperclassName;

            String @Nullable [] classSuperinterfaceSignatures;

            List<String> classAnnotations = new ArrayList<>(4);
            List<String> classTypeAnnotations = new ArrayList<>(4);
            List<Writer.Member> members = new ArrayList<>();

            public void writeClass() {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & classAccess) == 0) {
                    out.printf(
                            "%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                            jar.groupId, jar.artifactId, jar.version,
                            classAccess, className,
                            classSignature == null ? "" : classSignature,
                            classSuperclassName,
                            classSuperinterfaceSignatures == null ? "" : String.join("|", classSuperinterfaceSignatures),
                            -1, "", "", "", "", "",
                            classAnnotations.isEmpty() ? "" : String.join("", classAnnotations),
                            "", // Empty parameter annotations for class row
                            classTypeAnnotations.isEmpty() ? "" : PipeDelimitedJoiner.joinWithPipes(classTypeAnnotations),
                            ""); // Empty constant value for class row

                    for (Writer.Member member : members) {
                        member.writeMember(jar, this);
                    }
                }
            }

            void addMethod(Writer.Member member) {
                members.add(member);
            }

            void addField(Writer.Member member) {
                members.add(member);
            }
        }

        @Value
        private class Member {
            int access;
            String name;
            String descriptor;

            @Nullable
            String signature;

            String @Nullable [] exceptions;
            List<String> parameterNames = new ArrayList<>(4);
            List<String> elementAnnotations = new ArrayList<>(4);
            List<String> parameterAnnotations = new ArrayList<>(4);
            List<String> typeAnnotations = new ArrayList<>(4);

            @Nullable
            @NonFinal
            String constantValue;

            private void writeMember(Jar jar, ClassDefinition classDefinition) {
                if (((Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC) & access) == 0) {
                    out.printf(
                            "%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                            jar.groupId, jar.artifactId, jar.version,
                            classDefinition.classAccess, classDefinition.className,
                            classDefinition.classSignature == null ? "" : classDefinition.classSignature,
                            classDefinition.classSuperclassName,
                            classDefinition.classSuperinterfaceSignatures == null ? "" : String.join("|", classDefinition.classSuperinterfaceSignatures),
                            access, name, descriptor,
                            signature == null ? "" : signature,
                            parameterNames.isEmpty() ? "" : String.join("|", parameterNames),
                            exceptions == null ? "" : String.join("|", exceptions),
                            elementAnnotations.isEmpty() ? "" : String.join("", elementAnnotations),
                            serializeParameterAnnotations(parameterAnnotations, descriptor),
                            typeAnnotations.isEmpty() ? "" : PipeDelimitedJoiner.joinWithPipes(typeAnnotations),
                            constantValue == null ? "" : constantValue
                    );
                }
            }
        }
    }

    @Value
    static class GroupArtifactVersion {
        String groupId;
        String artifactId;
        String version;
    }

    @Value
    @RequiredArgsConstructor
    static class ClassDefinition {
        int access;
        String name;

        @Nullable
        String signature;

        @Nullable
        String superclassSignature;

        String @Nullable [] superinterfaceSignatures;

        @Nullable
        String annotations;

        @Nullable
        String constantValue;

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
    static class Member {
        ClassDefinition classDefinition;
        int access;
        String name;
        String descriptor;

        @Nullable
        String signature;

        String @Nullable [] parameterNames;
        String @Nullable [] exceptions;

        @Nullable
        String annotations;

        @Nullable
        String parameterAnnotations;

        String @Nullable [] typeAnnotations;

        @Nullable
        String constantValue;
    }

    /**
     * Serializes parameter annotations from a list of "paramIndex:annotation" strings
     * into a dense TSV format where each parameter position is represented.
     * For a method with 4 parameters where only parameters 0 and 2 have annotations,
     * the format would be: "[annotations]||[annotations]|"
     * Returns empty string if no parameters have annotations.
     */
    private static String serializeParameterAnnotations(List<String> parameterAnnotations, String descriptor) {
        if (parameterAnnotations.isEmpty()) {
            return "";
        }

        // Parse the method descriptor to get parameter count
        Type methodType = Type.getMethodType(descriptor);
        int paramCount = methodType.getArgumentTypes().length;

        // Group annotations by parameter index
        Map<Integer, List<String>> annotationsByParam = new TreeMap<>();
        for (String paramAnnotation : parameterAnnotations) {
            int colonIdx = paramAnnotation.indexOf(':');
            if (colonIdx > 0) {
                int paramIndex = Integer.parseInt(paramAnnotation.substring(0, colonIdx));
                String annotation = paramAnnotation.substring(colonIdx + 1);
                annotationsByParam.computeIfAbsent(paramIndex, k -> new ArrayList<>()).add(annotation);
            }
        }

        // If no parameters have annotations, return empty string
        if (annotationsByParam.isEmpty()) {
            return "";
        }

        // Build the dense representation
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) {
                result.append('|');
            }
            List<String> annotations = annotationsByParam.get(i);
            if (annotations != null && !annotations.isEmpty()) {
                // Escape pipes in annotation values since we use pipes to separate parameters
                for (String annotation : annotations) {
                    result.append(PipeDelimitedJoiner.escapePipes(annotation));
                }
            }
        }
        return result.toString();
    }

    private static class PipeDelimitedJoiner {
        static String joinWithPipes(List<String> items) {
            if (items.isEmpty()) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (String item : items) {
                if (!first) {
                    result.append('|');
                }
                first = false;
                // Escape any pipes in the item
                result.append(escapePipes(item));
            }
            return result.toString();
        }

        private static String escapePipes(String str) {
            if (!str.contains("|")) {
                return str;
            }
            return str.replace("|", "\\|");
        }
    }
}
