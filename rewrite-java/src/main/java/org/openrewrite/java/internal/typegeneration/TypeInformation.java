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
package org.openrewrite.java.internal.typegeneration;

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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.java.tree.TypeUtils;

public class TypeInformation {

    private String sourcePackage;
    private Set<JavaType.FullyQualified> sourceClasses = new HashSet<>();
    private Map<JavaType.FullyQualified, Set<JavaType.Method>> typeMap = new HashMap<>();

    public void setSourcePackage(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }
    public void addSourceClass(JavaType.Class sourceClass) {
        this.sourceClasses.add(sourceClass);
    }

    public void maybeAddType(JavaType type) {
        if (type instanceof JavaType.GenericTypeVariable || !(type instanceof JavaType.FullyQualified)) {
            return;
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        if (fullyQualified != null && !isKnowType(fullyQualified)) {
            typeMap.putIfAbsent(fullyQualified, new HashSet<>());
        }
    }

    public void maybeAddMethod(JavaType.Method methodType) {
        if (methodType == null || methodType.getName() == null ||
                methodType.getDeclaringType() == null || isKnowType(methodType.getDeclaringType())) {
            return;
        }
        Set<JavaType.Method> methods = typeMap.computeIfAbsent(methodType.getDeclaringType(), k -> new HashSet<>());
        methods.add(methodType);
        JavaType.Method.Signature signature = methodType.getResolvedSignature();
        if (signature != null) {
            maybeAddType(signature.getReturnType());
            if (signature.getParamTypes() != null) {
                signature.getParamTypes().forEach(this::maybeAddType);
            }
        }
    }

    private boolean isKnowType(JavaType.FullyQualified fullyQualified) {
        return fullyQualified.getPackageName().startsWith("java.");
    }

    public Map<JavaType.FullyQualified, Set<JavaType.Method>> getTypeMap() {
        return typeMap;
    }

    List<byte[]> getTypesAsByteCode() {

        Set<JavaType.FullyQualified> filteredTypes = typeMap.keySet().stream().filter(t -> !sourceClasses.contains(t)).collect(Collectors.toSet());

        List<byte[]> classes = new ArrayList<>();
        for (JavaType.FullyQualified type : filteredTypes) {
            if (type instanceof JavaType.Class) {
                classes.add(resolveClass((JavaType.Class) type, typeMap.get(type)));
            }
        }

        return classes;
    }

    byte[] resolveClass(JavaType.Class type, Set<JavaType.Method> methods) {
        assert type != null;
        PrintWriter writer = new PrintWriter(System.out);

        ClassWriter cw = new ClassWriter(0);
        final ClassVisitor classWriter = new TraceClassVisitor(cw, writer);
        int flags = type.getFlags().isEmpty() ? 1 : Flag.flagsToBitMap(type.getFlags());

        //NOTE: ACC_SUPER is set to true for all version of Java starting at 1.1
        flags = flags | kindBitMask(type.getKind()) | ACC_SUPER;

        String[] interfaceNames = type.getInterfaces().stream()
                .map(TypeUtils::asFullyQualified)
                .filter(Objects::nonNull)
                .map(TypeInformation::getAsmName).toArray(String[]::new);
        String signature = null;
        if (type.getSupertype() != null && !type.getSupertype().getFullyQualifiedName().equals("java.lang.Object")) {
            signature = getAsmReference(type.getSupertype());
        }

        classWriter.visit(V1_8, flags, getAsmName(type), signature, getAsmName(type.getSupertype()), interfaceNames);
        classWriter.visitSource(type.getClassName() + ".java", null);

        //Fields
        for (Variable variable: type.getMembers()) {
            classWriter.visitField(Flag.flagsToBitMap(variable.getFlags()), variable.getName(), getAsmReference(variable.getType()), null, null).visitEnd();
        }

        //Methods
        Stream.concat(type.getConstructors().stream(), methods.stream()).forEach(m -> generateMethod(m, classWriter));

        classWriter.visitEnd();
        writer.flush();
        return cw.toByteArray();
    }

    private static void generateMethod(JavaType.Method method, ClassVisitor classWriter) {

        JavaType.Method.Signature signature = method.getResolvedSignature() != null ? method.getResolvedSignature() : method.getGenericSignature();
        if (signature == null) return;

        String methodName = "<reflection_constructor>".equals(method.getName()) ? "<init>" : method.getName();
        StringBuilder descriptor = new StringBuilder();
        descriptor.append("(");
        descriptor.append(
                signature.getParamTypes().stream().map(TypeInformation::getAsmReference).collect(Collectors.joining())
        );
        descriptor.append(")");
        if (methodName.equals("<init>")) {
            descriptor.append("V");
        } else {
            descriptor.append(getAsmReference(signature.getReturnType()));
        }

        MethodVisitor methodVisitor;

        String[] exceptions = null;
        if (!method.getThrownExceptions().isEmpty()) {
            exceptions = method.getThrownExceptions().stream().map(TypeInformation::getAsmReference).toArray(String[]::new);
        }
        methodVisitor = classWriter.visitMethod(Flag.flagsToBitMap(method.getFlags()), methodName, descriptor.toString(), null, exceptions);
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

    private static String getAsmName(JavaType.FullyQualified type) {
        if (type == null) {
            return null;
        }
        JavaType.Class javaClass = TypeUtils.asClass(type);
        if (javaClass == null || javaClass.getOwningClass() == null) {
            return type.getFullyQualifiedName().replace(".", "/");
        } else {
            return type.getPackageName().replace(".", "/") + "/" + javaClass.getClassName().replace(".", "$");
        }
    }
    private static String getAsmReference(JavaType type) {

        if(type instanceof JavaType.Primitive) {
            switch ((JavaType.Primitive) type) {
                case Boolean:
                    return "Z";
                case Byte:
                    return "B";
                case Char:
                    return "C";
                case Double:
                    return "D";
                case Float:
                    return "F";
                case Int:
                    return "I";
                case Long:
                    return "J";
                case Short:
                    return "S";
                case Void:
                    return "V";
                case String:
                    return "Ljava/lang/String;";
                default:
                    throw new IllegalArgumentException("Primitive cannot be converted to ASM Reference");
            }
        } else if (type instanceof JavaType.Array) {
            return "[" + getAsmReference(((JavaType.Array) type).getElemType());
        } else if (type instanceof JavaType.FullyQualified) {
            JavaType.Class javaClass = TypeUtils.asClass(type);

            //If the type is not a class, build a class
            javaClass = javaClass != null ? javaClass : JavaType.Class.findClass(((JavaType.FullyQualified) type).getFullyQualifiedName());
            if (javaClass == null) {
                return "L" + getAsmName((JavaType.FullyQualified) type) + ";";
            }

            StringBuilder asmReference = new StringBuilder();
            asmReference.append("L").append(getAsmName(javaClass));
            if (!javaClass.getTypeParameters().isEmpty()) {
                asmReference.append("<");
                String typeParameters = javaClass.getTypeParameters().stream().map(TypeInformation::getAsmReference).collect(Collectors.joining(","));
                asmReference.append(typeParameters);
                asmReference.append(">");
            }
            return asmReference.append(";").toString();
        } else {
            throw new IllegalArgumentException("Type cannot be converted to ASM Reference");
        }
    }
}
