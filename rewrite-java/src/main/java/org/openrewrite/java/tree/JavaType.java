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
import lombok.*;
import lombok.experimental.NonFinal;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.ClassIdResolver;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface JavaType extends Serializable {
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

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
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

        public boolean isAssignableFrom(@Nullable FullyQualified clazz) {
            // TODO This does not take into account type parameters.
            return clazz != null && (
                    getFullyQualifiedName().equals(clazz.getFullyQualifiedName()) ||
                            isAssignableFrom(clazz.getSupertype()) ||
                            clazz.getInterfaces().stream().anyMatch(this::isAssignableFrom));
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
            resolver = ClassIdResolver.class,
            property = "fullyQualifiedName")
    @Value
    @With
    class Class extends FullyQualified {
        public static final Class CLASS = new Class(Flag.Public.getBitMask(), "java.lang.Class", Kind.Class,
                null, null, null, null, null, null);
        public static final Class ENUM = new Class(Flag.Public.getBitMask(), "java.lang.Class", Kind.Class,
                null, null, null, null, null, null);

        @Getter(AccessLevel.NONE)
        @With(AccessLevel.NONE)
        long flagsBitMap;

        String fullyQualifiedName;
        Kind kind;

        @Nullable
        @NonFinal
        List<FullyQualified> annotations;

        public List<FullyQualified> getAnnotations() {
            return annotations == null ? emptyList() : annotations;
        }

        @Nullable
        @NonFinal
        @JsonManagedReference
        List<Variable> members;

        public List<Variable> getMembers() {
            return members == null ? emptyList() : members;
        }

        @Nullable
        @NonFinal
        List<FullyQualified> interfaces;

        public List<FullyQualified> getInterfaces() {
            return interfaces == null ? emptyList() : interfaces;
        }

        @Nullable
        @NonFinal
        List<Method> methods;

        @JsonManagedReference
        public List<Method> getMethods() {
            return methods == null ? emptyList() : methods;
        }

        @Nullable
        FullyQualified supertype;

        @Nullable
        FullyQualified owningClass;

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

            return new JavaType.Class(1, fullyQualifiedName, Kind.Class, emptyList(),
                    emptyList(), emptyList(), emptyList(),
                    null, owningClass);
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
        public void unsafeSet(@Nullable List<FullyQualified> annotations, @Nullable List<FullyQualified> interfaces,
                              @Nullable List<Variable> members, @Nullable List<Method> methods) {
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

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    @With
    class Parameterized extends FullyQualified {
        FullyQualified type;

        @NonFinal
        List<JavaType> typeParameters;

        /**
         * Only meant to be used by parsers to avoid infinite recursion when building Class instances.
         */
        public void unsafeSet(List<JavaType> typeParameters) {
            this.typeParameters = typeParameters;
        }

        @Override
        public String getFullyQualifiedName() {
            return type.getFullyQualifiedName();
        }

        @Override
        public Parameterized withFullyQualifiedName(String fullyQualifiedName) {
            if (type.getFullyQualifiedName().equals(fullyQualifiedName)) {
                return this;
            }
            return new Parameterized(type.withFullyQualifiedName(fullyQualifiedName), typeParameters);
        }

        @Override
        public List<FullyQualified> getAnnotations() {
            return type.getAnnotations();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return type.hasFlags();
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
        public JavaType.Class.Kind getKind() {
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

        @Override
        public FullyQualified getOwningClass() {
            return type.getOwningClass();
        }

        @Override
        public FullyQualified getSupertype() {
            return type.getSupertype();
        }

        @Override
        public List<Variable> getVisibleSupertypeMembers() {
            return type.getVisibleSupertypeMembers();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parameterized that = (Parameterized) o;
            return type.equals(that.type) && typeParameters.equals(that.typeParameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, typeParameters);
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    @EqualsAndHashCode(callSuper = false)
    class GenericTypeVariable extends FullyQualified {
        String name;

        @Nullable
        @With
        FullyQualified bound;

        public GenericTypeVariable withName(String fullyQualifiedName) {
            if (this.name.equals(fullyQualifiedName)) {
                return this;
            }
            return new GenericTypeVariable(fullyQualifiedName, bound);
        }

        @Override
        public String getFullyQualifiedName() {
            return bound == null ? "java.lang.Object" : bound.getFullyQualifiedName();
        }

        public GenericTypeVariable withFullyQualifiedName(String fullyQualifiedName) {
            return bound == null || bound.getFullyQualifiedName().equals(fullyQualifiedName) ?
                    this :
                    withBound(JavaType.Class.build(fullyQualifiedName));
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
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    class Array implements JavaType {
        @Nullable
        JavaType elemType;
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
    class Method {
        @Getter(AccessLevel.NONE)
        @With(AccessLevel.PRIVATE)
        long flagsBitMap;

        @JsonBackReference
        FullyQualified declaringType;

        String name;

        @Nullable
        JavaType.Method.Signature genericSignature;

        @Nullable
        JavaType.Method.Signature resolvedSignature;

        List<String> paramNames;
        List<FullyQualified> thrownExceptions;
        List<FullyQualified> annotations;

        @Value
        @With
        public static class Signature implements Serializable {
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
            return "Method{" + name + "(" + String.join(", ", paramNames) + ")}";
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
    class Variable {
        @With(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        long flagsBitMap;

        @JsonBackReference
        FullyQualified owner;

        String name;

        @Nullable
        JavaType type;

        List<FullyQualified> annotations;

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
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
    }
}
