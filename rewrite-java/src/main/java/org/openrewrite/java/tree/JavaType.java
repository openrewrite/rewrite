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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.*;
import static org.openrewrite.internal.ListUtils.nullIfEmpty;
import static org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder.TO_STRING;

@SuppressWarnings("unused")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface JavaType {
    @JsonProperty("@c")
    default String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }

    @Nullable
    default Integer getManagedReference() {
        return null;
    }

    default JavaType withManagedReference(Integer id) {
        return this;
    }

    /**
     * Return a JavaType for the specified string.
     * The string is expected to be either a primitive type like "int" or a fully-qualified-class name like "java.lang.String"
     */
    static JavaType buildType(String typeName) {
        Primitive primitive = Primitive.fromKeyword(typeName);
        if (primitive != null) {
            return primitive;
        }
        return ShallowClass.build(typeName);
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    @With
    class MultiCatch implements JavaType {
        List<JavaType> throwableTypes;
    }

    abstract class FullyQualified implements JavaType {
        public abstract String getFullyQualifiedName();

        public abstract FullyQualified withFullyQualifiedName(String fullyQualifiedName);

        public abstract List<FullyQualified> getAnnotations();

        public abstract boolean hasFlags(Flag... test);

        public abstract Set<Flag> getFlags();

        public abstract List<FullyQualified> getInterfaces();

        public abstract Kind getKind();

        public abstract List<Variable> getMembers();

        public abstract List<Method> getMethods();

        public Iterator<Method> getVisibleMethods() {
            return getVisibleMethods(getPackageName());
        }

        private Iterator<Method> getVisibleMethods(String packageName) {
            return new FullyQualifiedIterator<>(
                    this,
                    packageName,
                    Method::getFlagsBitMap,
                    FullyQualified::getMethods,
                    fq -> fq.getVisibleMethods(packageName)
            );
        }

        public Iterator<Variable> getVisibleMembers() {
            return getVisibleMembers(getPackageName());
        }

        private Iterator<Variable> getVisibleMembers(String packageName) {
            return new FullyQualifiedIterator<>(
                    this,
                    packageName,
                    Variable::getFlagsBitMap,
                    FullyQualified::getMembers,
                    fq -> fq.getVisibleMembers(packageName)
            );
        }

        private static class FullyQualifiedIterator<E> implements Iterator<E> {
            private final FullyQualified fq;
            private final String visibleFromPackage;
            private final Function<E, Long> flags;
            private final Function<FullyQualified, Iterator<E>> recursive;

            private FullyQualified rec;
            private E peek;

            private Iterator<E> current;

            @Nullable
            private Iterator<E> supertypeE;

            @Nullable
            private Iterator<FullyQualified> interfaces;

            @Nullable
            private Iterator<E> interfaceE;

            private FullyQualifiedIterator(FullyQualified fq,
                                           String visibleFromPackage,
                                           Function<E, Long> flags,
                                           Function<FullyQualified, List<E>> base,
                                           Function<FullyQualified, Iterator<E>> recursive) {
                this.fq = fq;
                this.rec = fq;
                this.visibleFromPackage = visibleFromPackage;
                this.flags = flags;
                this.recursive = recursive;
                this.current = base.apply(fq).iterator();
            }

            @Override
            public boolean hasNext() {
                if (current.hasNext()) {
                    peek = current.next();

                    long peekFlags = flags.apply(peek);
                    if(((Flag.Public.getBitMask() | Flag.Protected.getBitMask()) & peekFlags) != 0) {
                        return true;
                    } else if((Flag.Private.getBitMask() & peekFlags) == 0 && rec.getPackageName().equals(visibleFromPackage)) {
                        // package private in the same package
                        return true;
                    }

                    return true;
                } else {
                    if (supertypeE == null) {
                        supertypeE = fq.getSupertype() == null ? emptyIterator() : recursive.apply(fq.getSupertype());
                        current = supertypeE;
                        rec = fq.getSupertype();
                        return hasNext();
                    } else if(interfaces == null) {
                        interfaces = fq.getInterfaces().iterator();
                        return hasNext();
                    } else if(interfaces.hasNext()) {
                        rec = interfaces.next();
                        current = recursive.apply(rec);
                        return hasNext();
                    }
                }
                return false;
            }

            @Override
            public E next() {
                return peek;
            }
        }

        @Nullable
        public abstract FullyQualified getOwningClass();

        @Nullable
        public abstract FullyQualified getSupertype();

        /**
         * @return The class name without package qualification. If an inner class, outer/inner classes are separated by '.'.
         */
        public String getClassName() {
            String fqn = getFullyQualifiedName();
            String className = fqn.substring(fqn.lastIndexOf('.') + 1);
            return className.replace('$', '.');
        }

        public String getPackageName() {
            String fqn = getFullyQualifiedName();
            int endPackage = fqn.lastIndexOf('.');
            return endPackage < 0 ? "" : fqn.substring(0, endPackage);
        }

        public boolean isAssignableTo(String fullyQualifiedName) {
            return getFullyQualifiedName().equals(fullyQualifiedName) ||
                    getInterfaces().stream().anyMatch(anInterface -> anInterface.isAssignableTo(fullyQualifiedName))
                    || (getSupertype() != null && getSupertype().isAssignableTo(fullyQualifiedName));
        }

        public boolean isAssignableFrom(@Nullable JavaType type) {
            if (type instanceof FullyQualified) {
                FullyQualified clazz = (FullyQualified) type;
                return getFullyQualifiedName().equals(clazz.getFullyQualifiedName()) ||
                        isAssignableFrom(clazz.getSupertype()) ||
                        clazz.getInterfaces().stream().anyMatch(this::isAssignableFrom);
            } else if (type instanceof GenericTypeVariable) {
                GenericTypeVariable generic = (GenericTypeVariable) type;
                for (JavaType bound : generic.getBounds()) {
                    if (isAssignableFrom(bound)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Class extends FullyQualified {
        @With
        @Nullable
        Integer managedReference;

        @With(AccessLevel.NONE)
        long flagsBitMap;

        @With
        String fullyQualifiedName;

        @With
        Kind kind;

        @With
        @Nullable
        @NonFinal
        FullyQualified supertype;

        @With
        @Nullable
        @NonFinal
        FullyQualified owningClass;

        @Nullable
        @NonFinal
        List<FullyQualified> annotations;

        public Class(@Nullable Integer managedReference, long flagsBitMap, String fullyQualifiedName,
                     Kind kind, @Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                     @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                     @Nullable List<Variable> members, @Nullable List<Method> methods) {
            this.managedReference = managedReference;
            this.flagsBitMap = flagsBitMap & Flag.VALID_FLAGS;
            this.fullyQualifiedName = fullyQualifiedName;
            this.kind = kind;
            this.supertype = supertype;
            this.owningClass = owningClass;
            this.annotations = nullIfEmpty(annotations);
            this.interfaces = nullIfEmpty(interfaces);
            this.members = nullIfEmpty(members);
            this.methods = nullIfEmpty(methods);
        }

        @Deprecated
        public static ShallowClass build(String fullyQualifiedName) {
            return ShallowClass.build(fullyQualifiedName);
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public JavaType.Class withAnnotations(@Nullable List<FullyQualified> annotations) {
            if (annotations != null && annotations.isEmpty()) {
                annotations = null;
            }
            if (annotations == this.annotations) {
                return this;
            }
            return new JavaType.Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.supertype,
                    this.owningClass, annotations, this.interfaces, this.members, this.methods);
        }

        @Nullable
        @NonFinal
        List<FullyQualified> interfaces;

        public List<FullyQualified> getInterfaces() {
            return interfaces == null ? emptyList() : interfaces;
        }

        public JavaType.Class withInterfaces(@Nullable List<FullyQualified> interfaces) {
            if (interfaces != null && interfaces.isEmpty()) {
                interfaces = null;
            }
            if (interfaces == this.interfaces) {
                return this;
            }
            return new JavaType.Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.supertype,
                    this.owningClass, this.annotations, interfaces, this.members, this.methods);
        }

        @Nullable
        @NonFinal
        List<Variable> members;

        public List<Variable> getMembers() {
            return members == null ? emptyList() : members;
        }

        public JavaType.Class withMembers(@Nullable List<Variable> members) {
            if (members != null && members.isEmpty()) {
                members = null;
            }
            if (members == this.members) {
                return this;
            }
            return new JavaType.Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.supertype,
                    this.owningClass, this.annotations, this.interfaces, members, this.methods);
        }

        @Nullable
        @NonFinal
        List<Method> methods;

        public List<Method> getMethods() {
            return methods == null ? emptyList() : methods;
        }

        public JavaType.Class withMethods(@Nullable List<Method> methods) {
            if (methods != null && methods.isEmpty()) {
                methods = null;
            }
            if (methods == this.methods) {
                return this;
            }
            return new JavaType.Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.supertype,
                    this.owningClass, this.annotations, this.interfaces, this.members, methods);
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        @Override
        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        /**
         * Only meant to be used by parsers to avoid infinite recursion when building Class instances.
         */
        public void unsafeSet(@Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                              @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                              @Nullable List<Variable> members, @Nullable List<Method> methods) {
            this.supertype = supertype;
            this.owningClass = owningClass;
            this.annotations = nullIfEmpty(annotations);
            this.interfaces = nullIfEmpty(interfaces);
            this.members = nullIfEmpty(members);
            this.methods = nullIfEmpty(methods);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Class aClass = (Class) o;
            return fullyQualifiedName.equals(aClass.fullyQualifiedName);
        }

        @Override
        public String toString() {
            return TO_STRING.signature(this);
        }
    }

    class ShallowClass extends Class {
        public ShallowClass(@Nullable Integer managedReference, long flagsBitMap, String fullyQualifiedName, Kind kind, @Nullable FullyQualified supertype, @Nullable FullyQualified owningClass, @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces, @Nullable List<Variable> members, @Nullable List<Method> methods) {
            super(managedReference, flagsBitMap, fullyQualifiedName, kind, supertype, owningClass, annotations, interfaces, members, methods);
        }

        /**
         * Build a class type only from the class' fully qualified name. Since we are not providing any member, type parameter,
         * interface, or supertype information, this fully qualified name could potentially match on more than one version of
         * the class found in the type cache. This method will simply pick one of them, because there is no way of selecting
         * between the versions of the class based solely on the fully qualified class name.
         *
         * @param fullyQualifiedName The fully qualified name of the class to build
         * @return Any class found in the type cache
         */
        public static ShallowClass build(String fullyQualifiedName) {
            ShallowClass owningClass = null;

            int firstClassNameIndex = 0;
            int lastDot = 0;
            char[] fullyQualifiedNameChars = fullyQualifiedName.replace('$', '.').toCharArray();
            char prev = ' ';
            for (int i = 0; i < fullyQualifiedNameChars.length; i++) {
                char c = fullyQualifiedNameChars[i];

                if (firstClassNameIndex == 0 && prev == '.' && Character.isUpperCase(c)) {
                    firstClassNameIndex = i;
                } else if (c == '.') {
                    lastDot = i;
                }
                prev = c;
            }

            if (lastDot > firstClassNameIndex) {
                owningClass = build(fullyQualifiedName.substring(0, lastDot));
            }

            return new JavaType.ShallowClass(null, 1, fullyQualifiedName, Kind.Class, null, owningClass,
                    emptyList(), emptyList(), emptyList(), emptyList());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Parameterized extends FullyQualified {
        @Getter
        @With
        @Nullable
        Integer managedReference;

        @With
        @NonFinal
        @Nullable
        FullyQualified type;

        @NonFinal
        @Nullable
        List<JavaType> typeParameters;

        public Parameterized(@Nullable Integer managedReference, @Nullable FullyQualified type,
                             @Nullable List<JavaType> typeParameters) {
            this.managedReference = managedReference;
            this.type = type;
            this.typeParameters = nullIfEmpty(typeParameters);
        }

        public FullyQualified getType() {
            assert type != null;
            return type;
        }

        public List<JavaType> getTypeParameters() {
            return typeParameters == null ? emptyList() : typeParameters;
        }

        public JavaType.Parameterized withTypeParameters(@Nullable List<JavaType> typeParameters) {
            if (typeParameters != null && typeParameters.isEmpty()) {
                typeParameters = null;
            }
            if (typeParameters == this.typeParameters) {
                return this;
            }
            return new JavaType.Parameterized(managedReference, type, typeParameters);
        }

        /**
         * Only meant to be used by parsers to avoid infinite recursion when building Class instances.
         */
        public void unsafeSet(FullyQualified type, List<JavaType> typeParameters) {
            assert type != this;
            this.type = type;
            this.typeParameters = nullIfEmpty(typeParameters);
        }

        @Override
        public String getFullyQualifiedName() {
            return type == null ? "" : type.getFullyQualifiedName();
        }

        public FullyQualified withFullyQualifiedName(String fullyQualifiedName) {
            assert type != null;
            return type.withFullyQualifiedName(fullyQualifiedName);
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            assert type != null;
            return type.getAnnotations();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            assert type != null;
            return type.hasFlags(test);
        }

        @Override
        public Set<Flag> getFlags() {
            assert type != null;
            return type.getFlags();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            assert type != null;
            return type.getInterfaces();
        }

        @Override
        public Kind getKind() {
            assert type != null;
            return type.getKind();
        }

        @Override
        public List<Variable> getMembers() {
            assert type != null;
            return type.getMembers();
        }

        @Override
        public List<Method> getMethods() {
            assert type != null;
            return type.getMethods();
        }

        @Nullable
        public FullyQualified getOwningClass() {
            assert type != null;
            return type.getOwningClass();
        }

        @Override
        public FullyQualified getSupertype() {
            assert type != null;
            return type.getSupertype();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parameterized that = (Parameterized) o;
            assert type != null && typeParameters != null;
            return type.equals(that.type) && typeParameters.equals(that.typeParameters);
        }

        @Override
        public String toString() {
            return TO_STRING.signature(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class GenericTypeVariable implements JavaType {
        @With
        @Getter
        @Nullable
        Integer managedReference;

        @With
        @Getter
        String name;

        @With
        @NonFinal
        @Getter
        Variance variance;

        @NonFinal
        @Nullable
        List<JavaType> bounds;

        public GenericTypeVariable(@Nullable Integer managedReference, String name, Variance variance, @Nullable List<JavaType> bounds) {
            this.managedReference = managedReference;
            this.name = name;
            this.variance = variance;
            this.bounds = nullIfEmpty(bounds);
        }

        public List<JavaType> getBounds() {
            return bounds == null ? emptyList() : bounds;
        }

        public JavaType.GenericTypeVariable withBounds(@Nullable List<JavaType> bounds) {
            if (bounds != null && bounds.isEmpty()) {
                bounds = null;
            }
            if (bounds == this.bounds) {
                return this;
            }
            return new JavaType.GenericTypeVariable(managedReference, name, variance, bounds);
        }

        public void unsafeSet(Variance variance, @Nullable List<JavaType> bounds) {
            this.variance = variance;
            this.bounds = nullIfEmpty(bounds);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericTypeVariable that = (GenericTypeVariable) o;
            assert bounds != null;
            return name.equals(that.name) && variance == that.variance && bounds.equals(that.bounds);
        }

        @Override
        public String toString() {
            return TO_STRING.signature(this);
        }

        public enum Variance {
            INVARIANT,
            COVARIANT,
            CONTRAVARIANT
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @With
    class Array implements JavaType {
        @Nullable
        @NonFinal
        JavaType elemType;

        public Array(@Nullable JavaType elemType) {
            this.elemType = elemType;
        }

        public JavaType getElemType() {
            assert elemType != null;
            return elemType;
        }

        public void unsafeSet(JavaType elemType) {
            this.elemType = elemType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Array array = (Array) o;
            assert elemType != null;
            return elemType.equals(array.elemType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elemType);
        }

        @Override
        public String toString() {
            return TO_STRING.signature(this);
        }
    }

    enum Primitive implements JavaType {
        Boolean,
        Byte,
        Char,
        Double,
        Float,
        Int,
        Long,
        Short,
        Void,
        String,
        None,
        Null;

        @Nullable
        public static Primitive fromKeyword(String keyword) {
            switch (keyword) {
                case "boolean":
                    return Boolean;
                case "byte":
                    return Byte;
                case "char":
                    return Char;
                case "double":
                    return Double;
                case "float":
                    return Float;
                case "int":
                    return Int;
                case "long":
                    return Long;
                case "short":
                    return Short;
                case "void":
                    return Void;
                case "String":
                    return String;
                case "null":
                    return Null;
                case "":
                    return None;
            }
            return null;
        }

        public String getKeyword() {
            switch (this) {
                case Boolean:
                    return "boolean";
                case Byte:
                    return "byte";
                case Char:
                    return "char";
                case Double:
                    return "double";
                case Float:
                    return "float";
                case Int:
                    return "int";
                case Long:
                    return "long";
                case Short:
                    return "short";
                case Void:
                    return "void";
                case String:
                    return "String";
                case Null:
                    return "null";
                case None:
                default:
                    return "";
            }
        }

        @Override
        public String toString() {
            return getKeyword();
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    class Method implements JavaType {
        @With(AccessLevel.PRIVATE)
        long flagsBitMap;

        @With
        @NonFinal
        @Nullable
        FullyQualified declaringType;

        @With
        String name;

        @With
        @NonFinal
        @Nullable
        JavaType returnType;

        @Nullable
        List<String> parameterNames;

        @NonFinal
        @Nullable
        List<JavaType> parameterTypes;

        @NonFinal
        @Nullable
        List<FullyQualified> thrownExceptions;

        @NonFinal
        @Nullable
        List<FullyQualified> annotations;

        public Method(long flagsBitMap, @Nullable FullyQualified declaringType, String name,
                      @Nullable JavaType returnType, @Nullable List<String> parameterNames,
                      @Nullable List<JavaType> parameterTypes, @Nullable List<FullyQualified> thrownExceptions,
                      @Nullable List<FullyQualified> annotations) {
            this.flagsBitMap = flagsBitMap & Flag.VALID_FLAGS;
            this.declaringType = declaringType;
            this.name = name;
            this.returnType = returnType;
            this.parameterNames = nullIfEmpty(parameterNames);
            this.parameterTypes = nullIfEmpty(parameterTypes);
            this.thrownExceptions = nullIfEmpty(thrownExceptions);
            this.annotations = nullIfEmpty(annotations);
        }

        public void unsafeSet(FullyQualified declaringType,
                              JavaType returnType,
                              @Nullable List<JavaType> parameterTypes,
                              @Nullable List<FullyQualified> thrownExceptions,
                              @Nullable List<FullyQualified> annotations) {
            this.declaringType = declaringType;
            this.returnType = returnType;
            this.parameterTypes = nullIfEmpty(parameterTypes);
            this.thrownExceptions = nullIfEmpty(thrownExceptions);
            this.annotations = nullIfEmpty(annotations);
        }

        public boolean isConstructor() {
            return name.equals("<constructor>");
        }

        public FullyQualified getDeclaringType() {
            assert declaringType != null;
            return declaringType;
        }

        public List<String> getParameterNames() {
            return parameterNames == null ? emptyList() : parameterNames;
        }

        public JavaType.Method withParameterNames(@Nullable List<String> parameterNames) {
            if (parameterNames != null && parameterNames.isEmpty()) {
                parameterNames = null;
            }
            if (parameterNames == this.parameterNames) {
                return this;
            }
            return new JavaType.Method(this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    parameterNames, this.parameterTypes, this.thrownExceptions, this.annotations);
        }

        public List<JavaType> getParameterTypes() {
            return parameterTypes == null ? emptyList() : parameterTypes;
        }

        public JavaType.Method withParameterTypes(@Nullable List<JavaType> parameterTypes) {
            if (parameterTypes != null && parameterTypes.isEmpty()) {
                parameterTypes = null;
            }
            if (parameterTypes == this.parameterTypes) {
                return this;
            }
            return new JavaType.Method(this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, parameterTypes, this.thrownExceptions, this.annotations);
        }

        public List<FullyQualified> getThrownExceptions() {
            return thrownExceptions == null ? emptyList() : thrownExceptions;
        }

        public JavaType.Method withThrownExceptions(@Nullable List<FullyQualified> thrownExceptions) {
            if (thrownExceptions != null && thrownExceptions.isEmpty()) {
                thrownExceptions = null;
            }
            if (thrownExceptions == this.thrownExceptions) {
                return this;
            }
            return new JavaType.Method(this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, this.parameterTypes, thrownExceptions, this.annotations);
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public JavaType.Method withAnnotations(@Nullable List<FullyQualified> annotations) {
            if (annotations != null && annotations.isEmpty()) {
                annotations = null;
            }
            if (annotations == this.annotations) {
                return this;
            }
            return new JavaType.Method(this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, this.parameterTypes, this.thrownExceptions, annotations);
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public Method withFlags(Set<Flag> flags) {
            return withFlagsBitMap(Flag.flagsToBitMap(flags));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Method method = (Method) o;
            assert declaringType != null;
            assert returnType != null;
            return declaringType.equals(method.declaringType) && name.equals(method.name) &&
                    returnType.equals(method.returnType) && Objects.equals(parameterTypes, method.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaringType, name, returnType, parameterTypes);
        }

        @Override
        public String toString() {
            return TO_STRING.methodSignature(this);
        }

    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    class Variable implements JavaType {
        @With(AccessLevel.NONE)
        long flagsBitMap;

        @With
        String name;

        @With
        @NonFinal
        @Nullable
        JavaType owner;

        @With
        @NonFinal
        @Nullable
        JavaType type;

        @NonFinal
        @Nullable
        List<FullyQualified> annotations;

        public Variable(long flagsBitMap, String name, @Nullable JavaType owner,
                        @Nullable JavaType type, @Nullable List<FullyQualified> annotations) {
            this.flagsBitMap = flagsBitMap & Flag.VALID_FLAGS;
            this.name = name;
            this.owner = owner;
            this.type = type;
            this.annotations = nullIfEmpty(annotations);
        }

        @Nullable
        public JavaType getOwner() {
            return owner;
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public JavaType.Variable withAnnotations(@Nullable List<FullyQualified> annotations) {
            if (annotations != null && annotations.isEmpty()) {
                annotations = null;
            }
            if (this.annotations == annotations) {
                return this;
            }
            return new JavaType.Variable(this.flagsBitMap, this.name, this.owner, this.type, annotations);
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public void unsafeSet(JavaType owner, @Nullable JavaType type,
                              @Nullable List<FullyQualified> annotations) {
            this.owner = owner;
            this.type = type;
            this.annotations = nullIfEmpty(annotations);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Variable variable = (Variable) o;
            assert owner != null;
            return name.equals(variable.name) && owner.equals(variable.owner);
        }

        @Override
        public String toString() {
            return TO_STRING.variableSignature(this);
        }
    }

    final class Unknown extends FullyQualified {
        private static final Unknown INSTANCE = new Unknown();

        private Unknown() {
        }

        @JsonCreator
        public static Unknown getInstance() {
            return INSTANCE;
        }

        @Override
        public String getFullyQualifiedName() {
            return "<unknown>";
        }

        @Override
        public FullyQualified withFullyQualifiedName(String fullyQualifiedName) {
            return this;
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            return emptyList();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return false;
        }

        @Override
        public Set<Flag> getFlags() {
            return emptySet();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return emptyList();
        }

        @Override
        public Kind getKind() {
            return Kind.Class;
        }

        @Override
        public List<Variable> getMembers() {
            return emptyList();
        }

        @Override
        public List<Method> getMethods() {
            return emptyList();
        }

        @Nullable
        @Override
        public FullyQualified getOwningClass() {
            return null;
        }

        @Nullable
        @Override
        public FullyQualified getSupertype() {
            return null;
        }

        @Override
        public String toString() {
            return "Unknown";
        }
    }
}
