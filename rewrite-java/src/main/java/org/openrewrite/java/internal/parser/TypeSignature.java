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

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured representation of JVMS type signatures, parsed from ASM.
 * <p>
 * Used by {@link TypeTableSink} to receive parsed generic signature data
 * instead of raw signature strings, avoiding unnecessary string round-trips.
 */
public abstract class TypeSignature {

    private TypeSignature() {}

    /**
     * A formal type parameter declaration (e.g., {@code T extends Comparable<T>}).
     */
    public static class FormalTypeParameter {
        private final String name;
        private final @Nullable TypeSignature classBound;
        private final List<TypeSignature> interfaceBounds;

        public FormalTypeParameter(String name, @Nullable TypeSignature classBound,
                                    List<TypeSignature> interfaceBounds) {
            this.name = name;
            this.classBound = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        public String getName() { return name; }
        public @Nullable TypeSignature getClassBound() { return classBound; }
        public List<TypeSignature> getInterfaceBounds() { return interfaceBounds; }
    }

    /**
     * A class type, possibly parameterized (e.g., {@code java/util/List<TE;>}).
     */
    public static class ClassType extends TypeSignature {
        private final String internalName;
        private final List<TypeSignature> typeArguments;

        public ClassType(String internalName, List<TypeSignature> typeArguments) {
            this.internalName = internalName;
            this.typeArguments = typeArguments;
        }

        public String getInternalName() { return internalName; }
        public List<TypeSignature> getTypeArguments() { return typeArguments; }
        public boolean isParameterized() { return !typeArguments.isEmpty(); }
    }

    /**
     * A type variable reference (e.g., {@code TE;}).
     */
    public static class TypeVariable extends TypeSignature {
        private final String name;

        public TypeVariable(String name) {
            this.name = name;
        }

        public String getName() { return name; }
    }

    /**
     * An array type (e.g., {@code [Ljava/lang/String;}).
     */
    public static class ArrayType extends TypeSignature {
        private final TypeSignature elementType;

        public ArrayType(TypeSignature elementType) {
            this.elementType = elementType;
        }

        public TypeSignature getElementType() { return elementType; }
    }

    /**
     * A primitive type (e.g., {@code I}, {@code Z}).
     */
    public static class BaseType extends TypeSignature {
        private final char descriptor;

        public BaseType(char descriptor) {
            this.descriptor = descriptor;
        }

        public char getDescriptor() { return descriptor; }
    }

    /**
     * A wildcard type argument.
     */
    public static class Wildcard extends TypeSignature {
        public enum Bound { UNBOUNDED, EXTENDS, SUPER }

        private final Bound bound;
        private final @Nullable TypeSignature boundType;

        public Wildcard(Bound bound, @Nullable TypeSignature boundType) {
            this.bound = bound;
            this.boundType = boundType;
        }

        public Bound getBound() { return bound; }
        public @Nullable TypeSignature getBoundType() { return boundType; }
    }

    /**
     * Result of parsing a JVMS ClassSignature.
     */
    public static class ClassSignatureResult {
        private final List<FormalTypeParameter> typeParameters;
        private final TypeSignature superclass;
        private final List<TypeSignature> interfaces;

        public ClassSignatureResult(List<FormalTypeParameter> typeParameters,
                                     TypeSignature superclass,
                                     List<TypeSignature> interfaces) {
            this.typeParameters = typeParameters;
            this.superclass = superclass;
            this.interfaces = interfaces;
        }

        public List<FormalTypeParameter> getTypeParameters() { return typeParameters; }
        public TypeSignature getSuperclass() { return superclass; }
        public List<TypeSignature> getInterfaces() { return interfaces; }
    }

    /**
     * Parse a JVMS ClassSignature using ASM's SignatureReader.
     *
     * @param signature the raw signature string from ASM
     * @return structured result, or null if parsing fails
     */
    public static @Nullable ClassSignatureResult parseClassSignature(String signature) {
        try {
            ClassSignatureCollector collector = new ClassSignatureCollector();
            new SignatureReader(signature).accept(collector);
            return collector.getResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ASM SignatureVisitor that collects class signature data into structured types.
     */
    private static class ClassSignatureCollector extends SignatureVisitor {
        private final List<FormalTypeParameter> typeParameters = new ArrayList<>();
        private @Nullable TypeSignature superclass;
        private final List<TypeSignature> interfaces = new ArrayList<>();

        // State for current formal type parameter being collected
        private @Nullable String currentParamName;
        private @Nullable TypeSignature currentClassBound;
        private List<TypeSignature> currentInterfaceBounds = new ArrayList<>();

        ClassSignatureCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            flushCurrentTypeParameter();
            currentParamName = name;
            currentClassBound = null;
            currentInterfaceBounds = new ArrayList<>();
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return new TypeSignatureCollector(sig -> currentClassBound = sig);
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return new TypeSignatureCollector(sig -> currentInterfaceBounds.add(sig));
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            flushCurrentTypeParameter();
            return new TypeSignatureCollector(sig -> superclass = sig);
        }

        @Override
        public SignatureVisitor visitInterface() {
            return new TypeSignatureCollector(sig -> interfaces.add(sig));
        }

        private void flushCurrentTypeParameter() {
            if (currentParamName != null) {
                typeParameters.add(new FormalTypeParameter(
                        currentParamName, currentClassBound, currentInterfaceBounds));
                currentParamName = null;
            }
        }

        ClassSignatureResult getResult() {
            flushCurrentTypeParameter();
            return new ClassSignatureResult(typeParameters,
                    superclass != null ? superclass : new ClassType("java/lang/Object", Collections.emptyList()),
                    interfaces);
        }
    }

    /**
     * Collects a single type signature and delivers it to a callback.
     */
    private static class TypeSignatureCollector extends SignatureVisitor {
        private final java.util.function.Consumer<TypeSignature> callback;

        // State
        private @Nullable String className;
        private final List<TypeSignature> typeArguments = new ArrayList<>();

        TypeSignatureCollector(java.util.function.Consumer<TypeSignature> callback) {
            super(Opcodes.ASM9);
            this.callback = callback;
        }

        @Override
        public void visitBaseType(char descriptor) {
            deliver(new BaseType(descriptor));
        }

        @Override
        public void visitTypeVariable(String name) {
            deliver(new TypeVariable(name));
        }

        @Override
        public SignatureVisitor visitArrayType() {
            return new TypeSignatureCollector(elemType ->
                    deliver(new ArrayType(elemType)));
        }

        @Override
        public void visitClassType(String name) {
            className = name;
            typeArguments.clear();
        }

        @Override
        public void visitInnerClassType(String name) {
            // Inner class: Map$Entry. Previous type args belong to the outer class.
            className = className + "$" + name;
            typeArguments.clear();
        }

        @Override
        public void visitTypeArgument() {
            // Unbounded wildcard: ?
            typeArguments.add(new Wildcard(Wildcard.Bound.UNBOUNDED, null));
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return new TypeSignatureCollector(argType -> {
                switch (wildcard) {
                    case '+':
                        typeArguments.add(new Wildcard(Wildcard.Bound.EXTENDS, argType));
                        break;
                    case '-':
                        typeArguments.add(new Wildcard(Wildcard.Bound.SUPER, argType));
                        break;
                    default: // '='
                        typeArguments.add(argType);
                        break;
                }
            });
        }

        @Override
        public void visitEnd() {
            if (className != null) {
                deliver(new ClassType(className, new ArrayList<>(typeArguments)));
                className = null;
                typeArguments.clear();
            }
        }

        private void deliver(TypeSignature sig) {
            callback.accept(sig);
        }
    }
}
