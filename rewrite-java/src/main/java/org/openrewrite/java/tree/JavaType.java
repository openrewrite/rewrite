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
import lombok.Getter;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.*;
import static org.openrewrite.internal.ListUtils.nullIfEmpty;
import static org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder.TO_STRING;
import static org.openrewrite.java.tree.TypeUtils.unknownIfNull;

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

    default JavaType unsafeSetManagedReference(Integer id) {
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

    default boolean isAssignableFrom(Pattern pattern) {
        if (this instanceof FullyQualified) {
            FullyQualified fq = (FullyQualified) this;
            if (pattern.matcher(fq.getFullyQualifiedName()).matches()) {
                return true;
            }
            if (fq.getSupertype() != null && fq.getSupertype().isAssignableFrom(pattern)) {
                return true;
            }
            for (FullyQualified anInterface : fq.getInterfaces()) {
                if (anInterface.isAssignableFrom(pattern)) {
                    return true;
                }
            }
            return false;
        } else if (this instanceof GenericTypeVariable) {
            GenericTypeVariable generic = (GenericTypeVariable) this;
            for (JavaType bound : generic.getBounds()) {
                if (bound.isAssignableFrom(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    class MultiCatch implements JavaType {
        public MultiCatch(@Nullable List<JavaType> throwableTypes) {
            this.throwableTypes = nullIfEmpty(throwableTypes);
        }

        private List<JavaType> throwableTypes;

        public List<JavaType> getThrowableTypes() {
            if (throwableTypes == null) {
                return Collections.emptyList();
            }
            return throwableTypes;
        }

        public MultiCatch withThrowableTypes(@Nullable List<JavaType> throwableTypes) {
            throwableTypes = nullIfEmpty(throwableTypes);
            if (this.throwableTypes == throwableTypes) {
                return this;
            }
            return new MultiCatch(throwableTypes);
        }

        public MultiCatch unsafeSet(List<JavaType> throwableTypes) {
            this.throwableTypes = nullIfEmpty(throwableTypes);
            return this;
        }
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

        public abstract List<JavaType> getTypeParameters();

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
            private Iterator<E> supertype;

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
                    if (((Flag.Public.getBitMask() | Flag.Protected.getBitMask()) & peekFlags) != 0) {
                        return true;
                    } else if ((Flag.Private.getBitMask() & peekFlags) == 0 && rec.getPackageName().equals(visibleFromPackage)) {
                        // package private in the same package
                        return true;
                    }

                    return true;
                } else {
                    if (supertype == null) {
                        supertype = fq.getSupertype() == null ? emptyIterator() : recursive.apply(fq.getSupertype());
                        current = supertype;
                        rec = fq.getSupertype();
                        return hasNext();
                    } else if (interfaces == null) {
                        interfaces = fq.getInterfaces().iterator();
                        return hasNext();
                    } else if (interfaces.hasNext()) {
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
            return TypeUtils.fullyQualifiedNamesAreEqual(getFullyQualifiedName(), fullyQualifiedName) ||
                   getInterfaces().stream().anyMatch(anInterface -> anInterface.isAssignableTo(fullyQualifiedName))
                   || (getSupertype() != null && getSupertype().isAssignableTo(fullyQualifiedName));
        }

        public boolean isAssignableFrom(@Nullable JavaType type) {
            if (type instanceof FullyQualified) {
                FullyQualified clazz = (FullyQualified) type;
                return TypeUtils.fullyQualifiedNamesAreEqual(getFullyQualifiedName(), clazz.getFullyQualifiedName()) ||
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
            Annotation,
            Record
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Class extends FullyQualified {
        @With
        @Nullable
        @NonFinal
        Integer managedReference;

        @With(AccessLevel.NONE)
        long flagsBitMap;

        @With
        String fullyQualifiedName;

        @With
        Kind kind;

        @NonFinal
        @Nullable
        List<JavaType> typeParameters;

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
                     Kind kind, @Nullable List<JavaType> typeParameters, @Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                     @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                     @Nullable List<Variable> members, @Nullable List<Method> methods) {
            this.managedReference = managedReference;
            this.flagsBitMap = flagsBitMap & Flag.VALID_CLASS_FLAGS;
            this.fullyQualifiedName = fullyQualifiedName;
            this.kind = kind;
            //noinspection DuplicatedCode
            this.typeParameters = nullIfEmpty(typeParameters);
            this.supertype = supertype;
            this.owningClass = owningClass;
            this.annotations = nullIfEmpty(annotations);
            this.interfaces = nullIfEmpty(interfaces);
            this.members = nullIfEmpty(members);
            this.methods = nullIfEmpty(methods);
        }

        @Deprecated
        public static Class build(String fullyQualifiedName) {
            return ShallowClass.build(fullyQualifiedName);
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public Class withAnnotations(@Nullable List<FullyQualified> annotations) {
            annotations = nullIfEmpty(annotations);
            if (annotations == this.annotations) {
                return this;
            }
            return new Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.typeParameters,
                    this.supertype, this.owningClass, annotations, this.interfaces, this.members, this.methods);
        }

        @Nullable
        @NonFinal
        List<FullyQualified> interfaces;

        public List<FullyQualified> getInterfaces() {
            return interfaces == null ? emptyList() : interfaces;
        }

        public Class withInterfaces(@Nullable List<FullyQualified> interfaces) {
            interfaces = nullIfEmpty(interfaces);
            if (interfaces == this.interfaces) {
                return this;
            }
            return new Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.typeParameters,
                    this.supertype, this.owningClass, this.annotations, interfaces, this.members, this.methods);
        }

        @Nullable
        @NonFinal
        List<Variable> members;

        public List<Variable> getMembers() {
            return members == null ? emptyList() : members;
        }

        public Class withMembers(@Nullable List<Variable> members) {
            members = nullIfEmpty(members);
            if (members == this.members) {
                return this;
            }
            return new Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.typeParameters,
                    this.supertype, this.owningClass, this.annotations, this.interfaces, members, this.methods);
        }

        @Nullable
        @NonFinal
        List<Method> methods;

        public List<Method> getMethods() {
            return methods == null ? emptyList() : methods;
        }

        public Class withMethods(@Nullable List<Method> methods) {
            methods = nullIfEmpty(methods);
            if (methods == this.methods) {
                return this;
            }
            return new Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, this.typeParameters,
                    this.supertype, this.owningClass, this.annotations, this.interfaces, this.members, methods);
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        @Override
        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        @Override
        public List<JavaType> getTypeParameters() {
            return typeParameters == null ? emptyList() : typeParameters;
        }

        public Class withTypeParameters(@Nullable List<JavaType> typeParameters) {
            typeParameters = nullIfEmpty(typeParameters);
            if (typeParameters == this.typeParameters) {
                return this;
            }
            return new Class(this.managedReference, this.flagsBitMap, this.fullyQualifiedName, this.kind, typeParameters,
                    this.supertype, this.owningClass, this.annotations, this.interfaces, this.members, methods);
        }

        public boolean isParameterized() {
            return typeParameters != null && !typeParameters.isEmpty();
        }

        @Override
        public Class unsafeSetManagedReference(Integer id) {
            this.managedReference = id;
            return this;
        }

        public Class unsafeSet(@Nullable List<JavaType> typeParameters, @Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                               @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                               @Nullable List<Variable> members, @Nullable List<Method> methods) {
            //noinspection DuplicatedCode
            this.typeParameters = nullIfEmpty(typeParameters);
            this.supertype = supertype;
            this.owningClass = owningClass;
            this.annotations = nullIfEmpty(annotations);
            this.interfaces = nullIfEmpty(interfaces);
            this.members = nullIfEmpty(members);
            this.methods = nullIfEmpty(methods);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Class aClass = (Class) o;
            return TypeUtils.fullyQualifiedNamesAreEqual(fullyQualifiedName, aClass.fullyQualifiedName) &&
                   (typeParameters == null && aClass.typeParameters == null || typeParameters != null && typeParameters.equals(aClass.typeParameters));
        }

        @Override
        public String toString() {
            return TO_STRING.signature(this);
        }
    }

    class ShallowClass extends Class {
        public ShallowClass(@Nullable Integer managedReference, long flagsBitMap, String fullyQualifiedName, Kind kind,
                            @Nullable List<JavaType> typeParameters, @Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                            @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces, @Nullable List<Variable> members, @Nullable List<Method> methods) {
            super(managedReference, flagsBitMap, fullyQualifiedName, kind, typeParameters, supertype, owningClass, annotations, interfaces, members, methods);
        }

        /**
         * Build a class type only from the class' fully qualified name.
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

            return new ShallowClass(null, 1, fullyQualifiedName, Kind.Class, emptyList(), null, owningClass,
                    emptyList(), emptyList(), emptyList(), emptyList());
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Parameterized extends FullyQualified {
        @Getter
        @With
        @Nullable
        @NonFinal
        Integer managedReference;

        @With
        @NonFinal
        FullyQualified type;

        @NonFinal
        @Nullable
        List<JavaType> typeParameters;

        public Parameterized(@Nullable Integer managedReference, @Nullable FullyQualified type,
                             @Nullable List<JavaType> typeParameters) {
            this.managedReference = managedReference;
            this.type = unknownIfNull(type);
            this.typeParameters = nullIfEmpty(typeParameters);
        }

        public FullyQualified getType() {
            return type;
        }

        @Override
        public List<JavaType> getTypeParameters() {
            return typeParameters == null ? emptyList() : typeParameters;
        }

        public Parameterized withTypeParameters(@Nullable List<JavaType> typeParameters) {
            typeParameters = nullIfEmpty(typeParameters);
            if (typeParameters == this.typeParameters) {
                return this;
            }
            return new Parameterized(managedReference, type, typeParameters);
        }

        @Override
        public Parameterized unsafeSetManagedReference(Integer id) {
            this.managedReference = id;
            return this;
        }

        public Parameterized unsafeSet(@Nullable FullyQualified type, @Nullable List<JavaType> typeParameters) {
            assert type != this;
            this.type = unknownIfNull(type);
            this.typeParameters = nullIfEmpty(typeParameters);
            return this;
        }

        @Override
        public String getFullyQualifiedName() {
            return type.getFullyQualifiedName();
        }

        public FullyQualified withFullyQualifiedName(String fullyQualifiedName) {
            return type.withFullyQualifiedName(fullyQualifiedName);
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            return type.getAnnotations();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return type.hasFlags(test);
        }

        @Override
        public Set<Flag> getFlags() {
            return type.getFlags();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return type.getInterfaces();
        }

        @Override
        public Kind getKind() {
            return type.getKind();
        }

        @Override
        public List<Variable> getMembers() {
            return type.getMembers();
        }

        @Override
        public List<Method> getMethods() {
            return type.getMethods();
        }

        @Nullable
        public FullyQualified getOwningClass() {
            return type.getOwningClass();
        }

        @Override
        public FullyQualified getSupertype() {
            return type.getSupertype();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parameterized that = (Parameterized) o;
            return Objects.equals(type, that.type) && Objects.equals(typeParameters, that.typeParameters);
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
        @NonFinal
        Integer managedReference;

        @With
        @NonFinal
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

        public GenericTypeVariable withBounds(@Nullable List<JavaType> bounds) {
            bounds = nullIfEmpty(bounds);
            if (bounds == this.bounds) {
                return this;
            }
            return new GenericTypeVariable(managedReference, name, variance, bounds);
        }

        @Override
        public GenericTypeVariable unsafeSetManagedReference(@Nullable Integer id) {
            this.managedReference = id;
            return this;
        }

        public GenericTypeVariable unsafeSet(String name, Variance variance, @Nullable List<JavaType> bounds) {
            this.name = name;
            this.variance = variance;
            this.bounds = nullIfEmpty(bounds);
            return this;
        }

        /**
         * @param variance The new variance
         * @param bounds The new bounds
         * @return This instance, with modifications/
         * @deprecated Use {@link #unsafeSet(String, Variance, List)} instead.
         */
        @Deprecated
        public GenericTypeVariable unsafeSet(Variance variance, @Nullable List<JavaType> bounds) {
            this.variance = variance;
            this.bounds = nullIfEmpty(bounds);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericTypeVariable that = (GenericTypeVariable) o;
            return name.equals(that.name) && variance == that.variance &&
                   (variance == Variance.INVARIANT && bounds == null && that.bounds == null || bounds != null && bounds.equals(that.bounds));
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
        @Getter
        @Nullable
        @NonFinal
        Integer managedReference;

        @NonFinal
        JavaType elemType;

        public Array(@Nullable Integer managedReference, @Nullable JavaType elemType) {
            this.managedReference = managedReference;
            this.elemType = unknownIfNull(elemType);
        }

        public JavaType getElemType() {
            return elemType;
        }

        @Override
        public Array unsafeSetManagedReference(Integer id) {
            this.managedReference = id;
            return this;
        }

        public Array unsafeSet(JavaType elemType) {
            this.elemType = unknownIfNull(elemType);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Array array = (Array) o;
            return Objects.equals(elemType, array.elemType);
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

        public String getClassName() {
            switch (this) {
                case Boolean:
                    return "java.lang.Boolean";
                case Byte:
                    return "java.lang.Byte";
                case Char:
                    return "java.lang.Character";
                case Double:
                    return "java.lang.Double";
                case Float:
                    return "java.lang.Float";
                case Int:
                    return "java.lang.Integer";
                case Long:
                    return "java.lang.Long";
                case Short:
                    return "java.lang.Short";
                case Void:
                    return "java.lang.Void";
                case String:
                    return "java.lang.String";
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

        public boolean isNumeric() {
            return this == Double || this == Int || this == Float || this == Long || this == Short;
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Method implements JavaType {
        @With
        @Nullable
        @NonFinal
        Integer managedReference;

        @With(AccessLevel.PRIVATE)
        long flagsBitMap;

        @With
        @NonFinal
        FullyQualified declaringType;

        @With
        String name;

        @With
        @NonFinal
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

        @Incubating(since = "7.34.0")
        @Nullable
        List<String> defaultValue;

        public Method(@Nullable Integer managedReference, long flagsBitMap, @Nullable FullyQualified declaringType, String name,
                      @Nullable JavaType returnType, @Nullable List<String> parameterNames,
                      @Nullable List<JavaType> parameterTypes, @Nullable List<FullyQualified> thrownExceptions,
                      @Nullable List<FullyQualified> annotations) {
            this(managedReference, flagsBitMap, declaringType, name, returnType, parameterNames, parameterTypes,
                    thrownExceptions, annotations, null);
        }

        @JsonCreator
        public Method(@Nullable Integer managedReference, long flagsBitMap, @Nullable FullyQualified declaringType, String name,
                      @Nullable JavaType returnType, @Nullable List<String> parameterNames,
                      @Nullable List<JavaType> parameterTypes, @Nullable List<FullyQualified> thrownExceptions,
                      @Nullable List<FullyQualified> annotations, @Nullable List<String> defaultValue) {
            this.managedReference = managedReference;
            this.flagsBitMap = flagsBitMap & Flag.VALID_FLAGS;
            this.declaringType = unknownIfNull(declaringType);
            this.name = name;
            this.returnType = unknownIfNull(returnType);
            this.parameterNames = nullIfEmpty(parameterNames);
            this.parameterTypes = nullIfEmpty(parameterTypes);
            this.thrownExceptions = nullIfEmpty(thrownExceptions);
            this.annotations = nullIfEmpty(annotations);
            this.defaultValue = defaultValue;
        }

        @Override
        public Method unsafeSetManagedReference(Integer id) {
            this.managedReference = id;
            return this;
        }

        public Method unsafeSet(@Nullable FullyQualified declaringType,
                                @Nullable JavaType returnType,
                                @Nullable List<JavaType> parameterTypes,
                                @Nullable List<FullyQualified> thrownExceptions,
                                @Nullable List<FullyQualified> annotations) {
            this.declaringType = unknownIfNull(declaringType);
            this.returnType = unknownIfNull(returnType);
            this.parameterTypes = nullIfEmpty(parameterTypes);
            this.thrownExceptions = nullIfEmpty(thrownExceptions);
            this.annotations = nullIfEmpty(annotations);
            return this;
        }

        public boolean isConstructor() {
            return "<constructor>".equals(name);
        }

        public FullyQualified getDeclaringType() {
            return declaringType;
        }

        public boolean isOverride() {
            if (declaringType instanceof JavaType.Unknown) {
                return false;
            }

            Stack<FullyQualified> interfaces = new Stack<>();
            interfaces.addAll(declaringType.getInterfaces());

            while (!interfaces.isEmpty()) {
                FullyQualified declaring = interfaces.pop();
                interfaces.addAll(declaring.getInterfaces());

                nextMethod:
                for (Method method : declaring.getMethods()) {
                    if (method.getName().equals(name)) {
                        List<JavaType> params = method.getParameterTypes();
                        if (getParameterTypes().size() != method.getParameterTypes().size()) {
                            continue;
                        }
                        for (int i = 0; i < params.size(); i++) {
                            if (!TypeUtils.isOfType(getParameterTypes().get(i), params.get(i))) {
                                continue nextMethod;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isInheritedFrom(String fullyQualifiedTypeName) {
            if (declaringType instanceof JavaType.Unknown) {
                return false;
            }

            Stack<FullyQualified> interfaces = new Stack<>();
            interfaces.addAll(declaringType.getInterfaces());

            while (!interfaces.isEmpty()) {
                FullyQualified declaring = interfaces.pop();
                interfaces.addAll(declaring.getInterfaces());

                if (declaring.getFullyQualifiedName().equals(fullyQualifiedTypeName)) {
                    continue;
                }

                nextMethod:
                for (Method method : declaring.getMethods()) {
                    if (method.getName().equals(name)) {
                        List<JavaType> params = method.getParameterTypes();
                        if (getParameterTypes().size() != method.getParameterTypes().size()) {
                            continue;
                        }
                        for (int i = 0; i < params.size(); i++) {
                            if (!TypeUtils.isOfType(getParameterTypes().get(i), params.get(i))) {
                                continue nextMethod;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        public List<String> getParameterNames() {
            return parameterNames == null ? emptyList() : parameterNames;
        }

        public Method withParameterNames(@Nullable List<String> parameterNames) {
            parameterNames = nullIfEmpty(parameterNames);
            if (parameterNames == this.parameterNames) {
                return this;
            }
            return new Method(this.managedReference, this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    parameterNames, this.parameterTypes, this.thrownExceptions, this.annotations, this.defaultValue);
        }

        public List<JavaType> getParameterTypes() {
            return parameterTypes == null ? emptyList() : parameterTypes;
        }

        public Method withParameterTypes(@Nullable List<JavaType> parameterTypes) {
            parameterTypes = nullIfEmpty(parameterTypes);
            if (parameterTypes == this.parameterTypes) {
                return this;
            }
            return new Method(this.managedReference, this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, parameterTypes, this.thrownExceptions, this.annotations, this.defaultValue);
        }

        public List<FullyQualified> getThrownExceptions() {
            return thrownExceptions == null ? emptyList() : thrownExceptions;
        }

        public Method withThrownExceptions(@Nullable List<FullyQualified> thrownExceptions) {
            thrownExceptions = nullIfEmpty(thrownExceptions);
            if (thrownExceptions == this.thrownExceptions) {
                return this;
            }
            return new Method(this.managedReference, this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, this.parameterTypes, thrownExceptions, this.annotations, this.defaultValue);
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public Method withAnnotations(@Nullable List<FullyQualified> annotations) {
            annotations = nullIfEmpty(annotations);
            if (annotations == this.annotations) {
                return this;
            }
            return new Method(this.managedReference, this.flagsBitMap, this.declaringType, this.name, this.returnType,
                    this.parameterNames, this.parameterTypes, this.thrownExceptions, annotations, this.defaultValue);
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
            return Objects.equals(declaringType, method.declaringType) &&
                   name.equals(method.name) &&
                   Objects.equals(returnType, method.returnType) &&
                   Objects.equals(parameterTypes, method.parameterTypes);
        }

        @Override
        public String toString() {
            return TO_STRING.methodSignature(this);
        }

    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    class Variable implements JavaType {
        @With
        @Nullable
        @NonFinal
        Integer managedReference;

        @With(AccessLevel.PRIVATE)
        long flagsBitMap;

        @With
        String name;

        @With
        @NonFinal
        @Nullable
        JavaType owner;

        @With
        @NonFinal
        JavaType type;

        @NonFinal
        @Nullable
        List<FullyQualified> annotations;

        public Variable(@Nullable Integer managedReference, long flagsBitMap, String name, @Nullable JavaType owner,
                        @Nullable JavaType type, @Nullable List<FullyQualified> annotations) {
            this.managedReference = managedReference;
            this.flagsBitMap = flagsBitMap & Flag.VALID_FLAGS;
            this.name = name;
            this.owner = owner;
            this.type = unknownIfNull(type);
            this.annotations = nullIfEmpty(annotations);
        }

        @Nullable
        public JavaType getOwner() {
            return owner;
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public Variable withAnnotations(@Nullable List<FullyQualified> annotations) {
            annotations = nullIfEmpty(annotations);
            if (this.annotations == annotations) {
                return this;
            }
            return new Variable(this.managedReference, this.flagsBitMap, this.name, this.owner, this.type, annotations);
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public Variable withFlags(Set<Flag> flags) {
            return withFlagsBitMap(Flag.flagsToBitMap(flags));
        }

        @Override
        public Variable unsafeSetManagedReference(Integer id) {
            this.managedReference = id;
            return this;
        }

        public Variable unsafeSet(JavaType owner, @Nullable JavaType type,
                                  @Nullable List<FullyQualified> annotations) {
            this.owner = owner;
            this.type = unknownIfNull(type);
            this.annotations = nullIfEmpty(annotations);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Variable variable = (Variable) o;
            return Objects.equals(name, variable.name) && Objects.equals(owner, variable.owner);
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
        public List<JavaType> getTypeParameters() {
            return emptyList();
        }

        @Override
        public String toString() {
            return "Unknown";
        }

        @Override
        public boolean isAssignableFrom(Pattern pattern) {
            return false;
        }

        @Override
        public boolean isAssignableFrom(@Nullable JavaType type) {
            return false;
        }

        @Override
        public boolean isAssignableTo(String fullyQualifiedName) {
            return false;
        }
    }
}
