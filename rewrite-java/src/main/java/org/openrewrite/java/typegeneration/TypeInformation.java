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
package org.openrewrite.java.typegeneration;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.java.tree.TypeUtils;

@Incubating(since="7.5.0")
public class TypeInformation {

    private final PrintStream debugOut;

    private final Set<JavaType.FullyQualified> sourceClasses = new HashSet<>();

    private final Map<String, ClassMeta> typeMap = new HashMap<>();

    public TypeInformation() {
        this(null);
    }
    public TypeInformation(@Nullable PrintStream debugOut) {
        this.debugOut = debugOut;
    }

    public void addDeclaredType(@Nullable JavaType.Class declaredType) {
        maybeAddType(declaredType);
        if (declaredType != null) {
            this.sourceClasses.add(declaredType);
        }
    }

    public void maybeAddType(@Nullable JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable) {
            maybeAddType(((JavaType.GenericTypeVariable) type).getBound());
        } else if (type instanceof JavaType.Wildcard) {
            maybeAddType(((JavaType.Wildcard) type).getType());
        } else if (type instanceof JavaType.FullyQualified) {
            JavaType.Class classType = (type instanceof JavaType.Class) ?
                    (JavaType.Class) type : JavaType.Class.build(((JavaType.FullyQualified) type).getFullyQualifiedName());
            if (!isKnowType(classType) && !typeMap.containsKey(classType.getFullyQualifiedName())) {
                typeMap.computeIfAbsent(classType.getFullyQualifiedName(), k -> new ClassMeta(classType));
                if (classType.getTypeParameters() != null) {
                    classType.getTypeParameters().forEach(this::maybeAddType);
                }
                if (classType.getOwningClass() != null) {
                    maybeAddType(classType.getOwningClass());
                    ClassMeta ownerMeta = typeMap.get(classType.getOwningClass().getFullyQualifiedName());
                    if (ownerMeta != null) {
                        ownerMeta.innerClasses.add(classType);
                    }
                }
                maybeAddType(classType.getSupertype());
                if (classType.getInterfaces() != null) {
                    classType.getInterfaces().forEach(this::maybeAddType);
                }
            }
        }
    }

    public void maybeAddMethod(@Nullable JavaType.Method methodType) {

        if (methodType == null || methodType.getName() == null ||
                methodType.getDeclaringType() == null || isKnowType(methodType.getDeclaringType())) {
            return;
        }
        maybeAddType(methodType.getDeclaringType());
        ClassMeta classMeta = typeMap.get(methodType.getDeclaringType().getFullyQualifiedName());
        if (classMeta.methods.contains(methodType)) {
            return;
        }
        classMeta.methods.add(methodType);
        JavaType.Method.Signature signature = methodType.getResolvedSignature();
        if (signature != null) {
            maybeAddType(signature.getReturnType());
            if (signature.getParamTypes() != null) {
                signature.getParamTypes().forEach(this::maybeAddType);
            }
        }
        if (methodType.getTypeParameters() != null) {
            methodType.getTypeParameters().forEach(this::maybeAddType);
        }
    }

    private boolean isKnowType(JavaType.FullyQualified fullyQualified) {
        return fullyQualified.getPackageName().startsWith("java.");
    }

    @Nullable private static JavaType.Class resolveClass(JavaType type) {
        JavaType.Class javaClass = TypeUtils.asClass(type);
        if (javaClass == null) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if (fq != null) {
                javaClass = JavaType.Class.build(fq.getFullyQualifiedName());
            }
        }
        return javaClass;
    }

    public List<byte[]> getTypesAsByteCode() {

        List<ClassMeta> filteredTypes = typeMap.values().stream().filter(meta -> !sourceClasses.contains(meta.classType)).collect(Collectors.toList());

        List<byte[]> classes = new ArrayList<>();
        for (ClassMeta type : filteredTypes) {
            classes.add(classToBytes(type));
        }

        return classes;
    }

    byte[] classToBytes(ClassMeta meta) {
        JavaType.Class type = meta.classType;
        String asmTypeName = AsmUtils.getAsmName(type);
        ClassVisitor classVisitor;
        ClassWriter classWriter = new ClassWriter(0);
        if (debugOut != null) {
            PrintWriter writer = new PrintWriter(debugOut);
            classVisitor = new TraceClassVisitor(classWriter, writer);
        } else {
            classVisitor = classWriter;
        }

        int flags = type.getFlags().isEmpty() ? 1 : Flag.flagsToBitMap(type.getFlags());

        //NOTE: ACC_SUPER is set to true for all version of Java starting at 1.1
        flags = flags | kindBitMask(type.getKind()) | ACC_SUPER;

        String[] interfaceNames = type.getInterfaces().stream()
                .map(TypeUtils::asFullyQualified)
                .filter(Objects::nonNull)
                .map(AsmUtils::getAsmName).toArray(String[]::new);

        String signature = null;
        if ((type.getTypeParameters() != null && !type.getTypeParameters().isEmpty()) ||
                (type.getSupertype() != null && type.getSupertype().getTypeParameters() != null && !type.getSupertype().getTypeParameters().isEmpty())) {
            signature = AsmUtils.getClassSignature(type);
        }
        classVisitor.visit(V1_8, flags, asmTypeName, signature, AsmUtils.getAsmName(type.getSupertype()), interfaceNames);
        classVisitor.visitSource(type.getClassName() + ".java", null);

        //Inner Classes
        for (JavaType.Class innerClass : meta.innerClasses) {
            String innerClassName = innerClass.getClassName();
            int index = innerClassName.lastIndexOf('.');
            if (index > -1) {
                innerClassName = innerClassName.substring(index+1);
            }
            classVisitor.visitInnerClass(AsmUtils.getAsmName(innerClass), asmTypeName, innerClassName, Flag.flagsToBitMap(innerClass.getFlags()) | kindBitMask(innerClass.getKind()));
        }

        //Fields
        for (Variable variable: type.getMembers()) {
            //Enum fields need to have the enum flag set, this logic will conditionally or the enumeration bitmask to
            //fields when the type is an enum.
            int enum_mask = type.getKind() == JavaType.Class.Kind.Enum ? 0x4000 : 0;
            if (variable.getType() != null) {
                JavaType.Class classType = resolveClass(variable.getType());
                boolean hasTypeParameters = classType != null && classType.getTypeParameters() != null &&
                        !classType.getTypeParameters().isEmpty();
                classVisitor.visitField(
                        Flag.flagsToBitMap(variable.getFlags()) | enum_mask,
                        variable.getName(),
                        AsmUtils.getAsmDescriptor(variable.getType()),
                        hasTypeParameters ? AsmUtils.getTypeSignature(variable.getType()) : null,
                        null).visitEnd();
            }
        }

        //Methods
        Stream.concat(type.getConstructors().stream(), meta.methods.stream()).forEach(m -> generateMethod(m, classVisitor));

        classVisitor.visitEnd();
        if (debugOut != null) {
            debugOut.flush();
        }
        return classWriter.toByteArray();
    }

    private static void generateMethod(JavaType.Method method, ClassVisitor classWriter) {

        JavaType.Method.Signature signature = method.getResolvedSignature() != null ? method.getResolvedSignature() : method.getGenericSignature();
        if (signature == null) return;

        String methodName = "<reflection_constructor>".equals(method.getName()) ? "<init>" : method.getName();

        //Descriptor:
        StringBuilder descriptor = new StringBuilder();
        descriptor.append("(");
        descriptor.append(
                signature.getParamTypes().stream()
                        .map(AsmUtils::getAsmDescriptor)
                        .collect(Collectors.joining())
        );
        descriptor.append(")");
        if (methodName.equals("<init>") || signature.getReturnType() == null) {
            descriptor.append("V");
        } else {
            descriptor.append(AsmUtils.getAsmDescriptor(signature.getReturnType()));
        }

        //Signature
        String asmSignature = AsmUtils.getMethodTypeSignature(method);

        //The signature is only necessary when there are type parameters, we can determine this if the
        //descriptor and signatures are of different sizes.
        boolean signatureRequired = (asmSignature != null && asmSignature.length() != descriptor.length());
        MethodVisitor methodVisitor;

        String[] exceptions = null;
        if (!method.getThrownExceptions().isEmpty()) {
            exceptions = method.getThrownExceptions().stream()
                    .map(AsmUtils::getAsmDescriptor)
                    .toArray(String[]::new);
        }
        methodVisitor = classWriter.visitMethod(
                Flag.flagsToBitMap(method.getFlags()),
                methodName,
                descriptor.toString(),
                signatureRequired ? asmSignature : null,
                exceptions);
        methodVisitor.visitEnd();
    }

    private static int kindBitMask(JavaType.Class.Kind kind) {
        switch (kind) {
            case Annotation:
                return 0x2200;
            case Enum:
                return 0x4000;
            case Interface:
                return 0x0200;
            default:
                //Class
                return 0;
        }
    }
    private static class ClassMeta {
        final JavaType.Class classType;
        final Set<JavaType.Method> methods = new HashSet<>();
        final Set<JavaType.Class> innerClasses = new HashSet<>();
        private ClassMeta(JavaType.Class classType) {
            this.classType = classType;
        }
    }
}
