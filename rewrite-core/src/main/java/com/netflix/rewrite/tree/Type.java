/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree;

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import com.netflix.rewrite.internal.lang.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface Type extends Serializable {
    boolean deepEquals(@Nullable Type type);

    @Data
    class MultiCatch implements Type {
        private final List<Type> throwableTypes;

        @Override
        public boolean deepEquals(@Nullable Type type) {
            return type instanceof MultiCatch &&
                    TypeUtils.deepEquals(throwableTypes, ((MultiCatch) type).throwableTypes);
        }
    }

    /**
     * Reduces memory and CPU footprint when deep class insight isn't necessary, such as
     * for the type parameters of a Type.Class
     */
    @Data
    class ShallowClass implements Type {
        private final String fullyQualifiedName;

        @Override
        public boolean deepEquals(Type type) {
            return type instanceof ShallowClass &&
                    fullyQualifiedName.equals(((ShallowClass) type).fullyQualifiedName);
        }
    }

    @Getter
    class Class implements Type {
        // there shouldn't be too many distinct types represented by the same fully qualified name
        private static final Map<String, HashObjSet<Class>> flyweights = HashObjObjMaps.newMutableMap();

        public static final Class OBJECT = build("java.lang.Object");

        private final String fullyQualifiedName;
        private final List<Var> members;
        private final List<Type> typeParameters;
        private final List<Type> interfaces;

        @Nullable
        private final Class supertype;

        private Class(String fullyQualifiedName, List<Var> members, List<Type> typeParameters, List<Type> interfaces, @Nullable Class supertype) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.members = members;
            this.typeParameters = typeParameters;
            this.interfaces = interfaces;
            this.supertype = supertype;
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
            return build(fullyQualifiedName, emptyList(), emptyList(), emptyList(), null, true);
        }

        @JsonCreator
        public static Class build(@JsonProperty("fullyQualifiedName") String fullyQualifiedName,
                                  @JsonProperty("members") List<Var> members,
                                  @JsonProperty("typeParameters") List<Type> typeParameters,
                                  @JsonProperty("interfaces") List<Type> interfaces,
                                  @JsonProperty("supertype") @Nullable Class supertype,
                                  boolean relaxedClassTypeMatching) {

            // when class type matching is NOT relaxed, the variants are the various versions of this fully qualified
            // name, where equality is determined by whether the supertype hierarchy and members through the entire
            // supertype hierarchy are equal
            var test = new Class(fullyQualifiedName,
                    members.stream().sorted(comparing(Var::getName)).collect(toList()),
                    typeParameters, interfaces, supertype);

            synchronized (flyweights) {
                var variants = flyweights.computeIfAbsent(fullyQualifiedName, fqn -> HashObjSets.newMutableSet());
                return (!relaxedClassTypeMatching ? variants.stream().filter(v -> v.deepEquals(test)) : variants.stream())
                        .findAny()
                        .orElseGet(() -> {
                            variants.add(test);
                            return test;
                        });
            }
        }

        @JsonIgnore
        public String getClassName() {
            return Arrays.stream(fullyQualifiedName.split("\\."))
                    .dropWhile(part -> Character.isLowerCase(part.charAt(0)))
                    .collect(joining("."));
        }

        @JsonIgnore
        public String getPackageName() {
            return Arrays.stream(fullyQualifiedName.split("\\."))
                    .takeWhile(part -> part.length() > 0 && !Character.isUpperCase(part.charAt(0)))
                    .collect(joining("."));
        }

        @JsonIgnore
        public boolean isAssignableFrom(@Nullable Type.Class clazz) {
            return clazz != null && (this == OBJECT ||
                    this.fullyQualifiedName.equals(clazz.fullyQualifiedName) ||
                    isAssignableFrom(clazz.getSupertype()) ||
                    clazz.getInterfaces().stream().anyMatch(i -> i instanceof Class && isAssignableFrom((Class) i))
            );
        }

        @JsonIgnore
        public List<Type.Var> getVisibleSupertypeMembers() {
            List<Type.Var> members = new ArrayList<>();
            if(supertype != null) {
                supertype.getMembers().stream()
                        .filter(member -> !member.hasFlags(Flag.Private))
                        .forEach(members::add);
                members.addAll(supertype.getVisibleSupertypeMembers());
            }
            return members;
        }

        @Override
        public boolean deepEquals(@Nullable Type type) {
            if (!(type instanceof Class)) {
                return false;
            }

            Class c = (Class) type;
            return fullyQualifiedName.equals(c.fullyQualifiedName) &&
                    TypeUtils.deepEquals(members, c.members) &&
                    TypeUtils.deepEquals(supertype, c.supertype) &&
                    TypeUtils.deepEquals(typeParameters, c.typeParameters);
        }
    }

    @Data
    class Cyclic implements Type {
        private final String fullyQualifiedName;

        @Override
        public boolean deepEquals(Type type) {
            return this.equals(type);
        }
    }

    @Data
    class Var implements Type {
        private final String name;

        @Nullable
        private final Type type;

        private final Set<Flag> flags;

        public boolean hasFlags(Flag... test) {
            return Arrays.stream(test).allMatch(flags::contains);
        }

        @Override
        public boolean deepEquals(@Nullable Type type) {
            if (!(type instanceof Var)) {
                return false;
            }

            Var v = (Var) type;
            return name.equals(v.name) && TypeUtils.deepEquals(this.type, v.type) &&
                    flags.equals(v.flags);
        }
    }

    @Getter
    class Method implements Type {
        private static final Map<Class, Map<String, Set<Method>>> flyweights = HashObjObjMaps.newMutableMap();

        @With
        private final Class declaringType;

        private final String name;
        private final Signature genericSignature;
        private final Signature resolvedSignature;
        private final List<String> paramNames;

        @With
        private final Set<Flag> flags;

        private Method(Class declaringType, String name, Signature genericSignature, Signature resolvedSignature, List<String> paramNames, Set<Flag> flags) {
            this.declaringType = declaringType;
            this.name = name;
            this.genericSignature = genericSignature;
            this.resolvedSignature = resolvedSignature;
            this.paramNames = paramNames;
            this.flags = flags;
        }

        @JsonCreator
        public static Method build(@JsonProperty("declaringType") Class declaringType,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("genericSignature") Signature genericSignature,
                                   @JsonProperty("resolvedSignature") Signature resolvedSignature,
                                   @JsonProperty("paramNames") List<String> paramNames,
                                   @JsonProperty("flags") Set<Flag> flags) {
            var test = new Method(declaringType, name, genericSignature, resolvedSignature, paramNames, flags);

            synchronized (flyweights) {
                Set<Method> methods = flyweights
                        .computeIfAbsent(declaringType, dt -> HashObjObjMaps.newMutableMap())
                        .computeIfAbsent(name, n -> HashObjSets.newMutableSet());

                return methods
                        .stream()
                        .filter(m -> m.deepEquals(test))
                        .findAny()
                        .orElseGet(() -> {
                            methods.add(test);
                            return test;
                        });
            }
        }

        @Data
        public static class Signature implements Serializable {
            @Nullable
            private final Type returnType;

            private final List<Type> paramTypes;
        }

        private static boolean signatureDeepEquals(@Nullable Signature s1, @Nullable Signature s2) {
            return s1 == null ? s2 == null : s2 != null &&
                    TypeUtils.deepEquals(s1.returnType, s2.returnType) &&
                    TypeUtils.deepEquals(s1.paramTypes, s2.paramTypes);
        }

        public boolean hasFlags(Flag... test) {
            return Arrays.stream(test).allMatch(flags::contains);
        }

        @Override
        public boolean deepEquals(Type type) {
            if (!(type instanceof Method)) {
                return false;
            }

            Method m = (Method) type;
            return paramNames.equals(m.paramNames) &&
                    flags.equals(m.flags) &&
                    declaringType.deepEquals(m.declaringType) &&
                    signatureDeepEquals(genericSignature, m.genericSignature) &&
                    signatureDeepEquals(resolvedSignature, m.resolvedSignature);
        }
    }

    @Data
    class GenericTypeVariable implements Type {
        private final String fullyQualifiedName;

        @Nullable
        private final Class bound;

        @Override
        public boolean deepEquals(Type type) {
            if (!(type instanceof GenericTypeVariable)) {
                return false;
            }

            GenericTypeVariable generic = (GenericTypeVariable) type;
            return fullyQualifiedName.equals(generic.fullyQualifiedName) &&
                    TypeUtils.deepEquals(bound, generic.bound);
        }
    }

    @Data
    class Array implements Type {
        private final Type elemType;

        @Override
        public boolean deepEquals(Type type) {
            return type instanceof Array && elemType.deepEquals(((Array) type).elemType);
        }
    }

    enum Primitive implements Type {
        Boolean, Byte, Char, Double, Float, Int, Long, Short, Void, String, None, Wildcard, Null;

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
                case "":
                    return None;
                case "*":
                    return Wildcard;
                case "null":
                    return Null;
                default:
                    throw new IllegalArgumentException("Invalid primitive type " + keyword);
            }
        }

        @JsonIgnore
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

        @Override
        public boolean deepEquals(Type type) {
            return this == type;
        }
    }
}
