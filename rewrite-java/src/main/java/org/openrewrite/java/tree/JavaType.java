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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface JavaType {
    /**
     * Return a JavaType for the specified string.
     * The string is expected to be either a primitive type like "int" or a fully-qualified-class name like "java.lang.String"
     */
    static JavaType buildType(String typeName) {
        Primitive primitive = Primitive.fromKeyword(typeName);
        if (primitive != null) {
            return primitive;
        } else {
            return Class.build(typeName);
        }
    }

    @Value
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

        public abstract Class.Kind getKind();

        public abstract List<Variable> getMembers();

        public abstract List<Method> getMethods();

        @Nullable
        public abstract FullyQualified getOwningClass();

        @Nullable
        public abstract FullyQualified getSupertype();

        public abstract List<Variable> getVisibleSupertypeMembers();

        public String getClassName() {
            AtomicBoolean dropWhile = new AtomicBoolean(false);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        dropWhile.set(dropWhile.get() || !Character.isLowerCase(part.charAt(0)));
                        return dropWhile.get();
                    })
                    .collect(joining("."));
        }

        public String getPackageName() {
            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        takeWhile.set(takeWhile.get() && !Character.isUpperCase(part.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

        public boolean isAssignableTo(String fullyQualifiedName) {
            return getFullyQualifiedName().equals(fullyQualifiedName) ||
                    getInterfaces().stream().anyMatch(anInterface -> anInterface.isAssignableTo(fullyQualifiedName));
        }

        public boolean isAssignableFrom(@Nullable FullyQualified clazz) {
            // TODO This does not take into account type parameters.
            return clazz != null && (
                    getFullyQualifiedName().equals(clazz.getFullyQualifiedName()) ||
                            isAssignableFrom(clazz.getSupertype()) ||
                            clazz.getInterfaces().stream().anyMatch(this::isAssignableFrom));
        }
    }

    @Value
    @With
    class Class extends FullyQualified {
        public static final Class CLASS = new Class(Flag.Public.getBitMask(), "java.lang.Class", Kind.Class,
                null, null, null, null, null, null);
        public static final Class ENUM = new Class(Flag.Public.getBitMask(), "java.lang.Enum", Kind.Class,
                null, null, null, null, null, null);

        @With(AccessLevel.NONE)
        long flagsBitMap;

        String fullyQualifiedName;
        Kind kind;

        @Nullable
        @NonFinal
        FullyQualified supertype;

        @Nullable
        @NonFinal
        FullyQualified owningClass;

        @Nullable
        @NonFinal
        List<FullyQualified> annotations;

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        @Nullable
        @NonFinal
        List<FullyQualified> interfaces;

        public List<FullyQualified> getInterfaces() {
            return interfaces == null ? emptyList() : interfaces;
        }

        @Nullable
        @NonFinal
        List<Variable> members;

        public List<Variable> getMembers() {
            return members == null ? emptyList() : members;
        }

        @Nullable
        @NonFinal
        List<Method> methods;

        public List<Method> getMethods() {
            return methods == null ? emptyList() : methods;
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
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
        public static Class build(String fullyQualifiedName) {
            Class owningClass = null;

            int firstClassNameIndex = 0;
            int lastDot = 0;
            char[] fullyQualifiedNameChars = fullyQualifiedName.toCharArray();
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

            return new JavaType.Class(1, fullyQualifiedName, Kind.Class, null, owningClass,
                    emptyList(), emptyList(), emptyList(), emptyList());
        }

        public List<Variable> getVisibleSupertypeMembers() {
            List<Variable> members = new ArrayList<>();
            if (this.supertype != null) {
                for (Variable member : this.supertype.getMembers()) {
                    if (!member.hasFlags(Flag.Private)) {
                        members.add(member);
                    }
                }
                members.addAll(supertype.getVisibleSupertypeMembers());
            }
            return members;
        }

        /**
         * Only meant to be used by parsers to avoid infinite recursion when building Class instances.
         */
        public void unsafeSet(@Nullable FullyQualified supertype, @Nullable FullyQualified owningClass,
                              @Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                              @Nullable List<Variable> members, @Nullable List<Method> methods) {
            this.supertype = supertype;
            this.owningClass = owningClass;
            this.annotations = annotations;
            this.interfaces = interfaces;
            this.members = members;
            this.methods = methods;
        }

        @Override
        public String toString() {
            return "Class{" + fullyQualifiedName + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Class aClass = (Class) o;
            return Objects.equals(fullyQualifiedName, aClass.fullyQualifiedName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullyQualifiedName);
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }
    }

    @Value
    @With
    class Parameterized extends FullyQualified {
        @NonFinal
        @Nullable
        FullyQualified type;

        @NonFinal
        List<JavaType> typeParameters;

        /**
         * Only meant to be used by parsers to avoid infinite recursion when building Class instances.
         */
        public void unsafeSet(FullyQualified type, List<JavaType> typeParameters) {
            this.type = type;
            this.typeParameters = typeParameters;
        }

        @Override
        public String getFullyQualifiedName() {
            assert type != null;
            return type.getFullyQualifiedName();
        }

        @Override
        public Parameterized withFullyQualifiedName(String fullyQualifiedName) {
            assert type != null;
            if (type.getFullyQualifiedName().equals(fullyQualifiedName)) {
                return this;
            }
            return new Parameterized(type.withFullyQualifiedName(fullyQualifiedName), typeParameters);
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            assert type != null;
            return type.getAnnotations();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            assert type != null;
            return type.hasFlags();
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
        public JavaType.Class.Kind getKind() {
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

        @Override
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
        public List<Variable> getVisibleSupertypeMembers() {
            assert type != null;
            return type.getVisibleSupertypeMembers();
        }

        @Override
        public boolean equals(Object o) {
            assert type != null;
            assert typeParameters != null;
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parameterized that = (Parameterized) o;
            return type.equals(that.type) && typeParameters.equals(that.typeParameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, typeParameters);
        }

        @Override
        public String toString() {
            return "Parameterized{" + getFullyQualifiedName() + "}";
        }
    }

    @Value
    @With
    class GenericTypeVariable extends FullyQualified {
        String name;

        @NonFinal
        @Nullable
        FullyQualified bound;

        @Override
        public String getFullyQualifiedName() {
            return bound == null ? "java.lang.Object" : bound.getFullyQualifiedName();
        }

        public GenericTypeVariable withFullyQualifiedName(String fullyQualifiedName) {
            return bound == null || bound.getFullyQualifiedName().equals(fullyQualifiedName) ?
                    this :
                    withBound(JavaType.Class.build(fullyQualifiedName));
        }

        public void unsafeSet(@Nullable FullyQualified bound) {
            this.bound = bound;
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            return bound == null ? emptyList() : bound.getAnnotations();
        }

        @Override
        public Class.Kind getKind() {
            return Class.Kind.Class;
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return bound != null && bound.hasFlags(test);
        }

        @Override
        public Set<Flag> getFlags() {
            return bound == null ? emptySet() : bound.getFlags();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return bound == null ? emptyList() : bound.getInterfaces();
        }

        @Override
        public List<Variable> getMembers() {
            return bound == null ? emptyList() : bound.getMembers();
        }

        @Override
        public List<Method> getMethods() {
            return bound == null ? emptyList() : bound.getMethods();
        }

        @Override
        public FullyQualified getOwningClass() {
            return bound == null ? null : bound.getOwningClass();
        }

        @Override
        public FullyQualified getSupertype() {
            return bound == null ? null : bound.getSupertype();
        }

        @Override
        public List<Variable> getVisibleSupertypeMembers() {
            return bound == null ? emptyList() : bound.getVisibleSupertypeMembers();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenericTypeVariable that = (GenericTypeVariable) o;
            return name.equals(that.name) && Objects.equals(bound == null ? null : bound.getFullyQualifiedName(),
                    that.bound == null ? null : that.bound.getFullyQualifiedName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, bound == null ? null : bound.getFullyQualifiedName());
        }

        @Override
        public String toString() {
            return "GenericTypeVariable{" + name + " extends " + getFullyQualifiedName() + "}";
        }
    }

    @Value
    class Array implements JavaType {
        JavaType elemType;

        @Override
        public String toString() {
            return "Array{" + elemType + "}";
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
        Wildcard,
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
                case "*":
                    return Wildcard;
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
                case Wildcard:
                    return "*";
                case Null:
                    return "null";
                case None:
                default:
                    return "";
            }
        }
    }

    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    class Method {
        @With(AccessLevel.PRIVATE)
        long flagsBitMap;

        @NonFinal
        FullyQualified declaringType;

        String name;

        @Nullable
        List<String> paramNames;

        @NonFinal
        @Nullable
        JavaType.Method.Signature genericSignature;

        @NonFinal
        @Nullable
        JavaType.Method.Signature resolvedSignature;

        @NonFinal
        @Nullable
        List<FullyQualified> thrownExceptions;

        @NonFinal
        @Nullable
        List<FullyQualified> annotations;

        public void unsafeSet(FullyQualified declaringType,
                              @Nullable JavaType.Method.Signature genericSignature,
                              @Nullable JavaType.Method.Signature resolvedSignature,
                              @Nullable List<FullyQualified> thrownExceptions,
                              @Nullable List<FullyQualified> annotations) {
            this.declaringType = declaringType;
            this.genericSignature = genericSignature;
            this.resolvedSignature = resolvedSignature;
            this.thrownExceptions = thrownExceptions != null && thrownExceptions.isEmpty() ? null : thrownExceptions;
            this.annotations = annotations != null && annotations.isEmpty() ? null : annotations;
        }

        public List<String> getParamNames() {
            return paramNames == null ? emptyList() : paramNames;
        }

        public List<FullyQualified> getThrownExceptions() {
            return thrownExceptions == null ? emptyList() : thrownExceptions;
        }

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        @Value
        @With
        public static class Signature {
            @Nullable
            JavaType returnType;

            List<JavaType> paramTypes;
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
        public String toString() {
            return "Method{" + (declaringType == null ? "<unknown>" : declaringType) + "#" + name + "(" + String.join(", ", (paramNames == null ? emptyList() : paramNames)) + ")}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Method method = (Method) o;
            return declaringType.equals(method.declaringType) && name.equals(method.name) && Objects.equals(genericSignature, method.genericSignature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaringType, name, genericSignature);
        }
    }

    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    class Variable {
        @With(AccessLevel.NONE)
        long flagsBitMap;

        String name;

        @NonFinal
        FullyQualified owner;

        @NonFinal
        @Nullable
        JavaType type;

        @NonFinal
        @Nullable
        List<FullyQualified> annotations;

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public void unsafeSet(FullyQualified owner, @Nullable JavaType type,
                              @Nullable List<FullyQualified> annotations) {
            this.owner = owner;
            this.type = type;
            this.annotations = annotations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Variable variable = (Variable) o;
            return owner.equals(variable.owner) && name.equals(variable.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, name);
        }

        @Override
        public String toString() {
            return "Variable{" + (owner == null ? "<unknown>" : owner) + "#" + name + "}";
        }
    }
}
