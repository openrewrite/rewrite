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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface JavaType extends Serializable {
    boolean deepEquals(@Nullable JavaType type);

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

    @Data
    class MultiCatch implements JavaType {
        private final List<JavaType> throwableTypes;

        public MultiCatch(List<JavaType> throwableTypes) {
            this.throwableTypes = throwableTypes;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type || (type instanceof MultiCatch &&
                    TypeUtils.deepEquals(throwableTypes, ((MultiCatch) type).throwableTypes));
        }
    }

    abstract class FullyQualified implements JavaType {

        public abstract String getFullyQualifiedName();

        public abstract boolean hasFlags(Flag... test);

        public abstract Set<Flag> getFlags();

        public abstract List<FullyQualified> getInterfaces();

        public abstract Class.Kind getKind();

        public abstract List<Variable> getMembers();

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

        public boolean isAssignableFrom(@Nullable JavaType.FullyQualified clazz) {
            //TODO This does not take into account type parameters.
            return clazz != null && (this == Class.OBJECT ||
                    getFullyQualifiedName().equals(clazz.getFullyQualifiedName()) ||
                    isAssignableFrom(clazz.getSupertype()) ||
                    clazz.getInterfaces().stream().anyMatch(this::isAssignableFrom));
        }
    }

    /**
     * Reduces memory and CPU footprint when deep class insight isn't necessary, such as
     * for the type parameters of a Type.Class
     */
    @EqualsAndHashCode(callSuper = false)
    @Data
    class ShallowClass extends FullyQualified {
        private final String fullyQualifiedName;

        public ShallowClass(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public Class.Kind getKind() {
            return Class.Kind.Class;
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return test.length == 1 && test[0] == Flag.Public;
        }

        @Override
        public Set<Flag> getFlags() {
            return Collections.singleton(Flag.Public);
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return Collections.emptyList();
        }

        @Override
        public List<Variable> getMembers() {
            return Collections.emptyList();
        }

        @Override
        public FullyQualified getOwningClass() {
            return null;
        }

        @Override
        public FullyQualified getSupertype() {
            return Class.OBJECT;
        }

        @Override
        public List<Variable> getVisibleSupertypeMembers() {
            return Collections.emptyList();
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type || (type instanceof ShallowClass &&
                    fullyQualifiedName.equals(((ShallowClass) type).fullyQualifiedName));
        }

        @Override
        public String toString() {
            return "ShallowClass(" + fullyQualifiedName + ")";
        }
    }

    @Getter
    class Class extends FullyQualified {
        // there shouldn't be too many distinct types represented by the same fully qualified name
        private static final Map<String, Set<Class>> flyweights = new WeakHashMap<>();

        public static final Class OBJECT = build("java.lang.Object");

        private final String fullyQualifiedName;

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;
        private final Kind kind;

        private final List<Variable> members;
        private final List<FullyQualified> interfaces;

        @Nullable
        private volatile List<Method> constructors;

        @Nullable
        private final FullyQualified supertype;

        @Nullable
        private final FullyQualified owningClass;

        private Class(int flagsBitMap,
                      String fullyQualifiedName,
                      Kind kind,
                      List<Variable> members,
                      List<FullyQualified> interfaces,
                      @Nullable List<Method> constructors,
                      @Nullable FullyQualified supertype,
                      @Nullable FullyQualified owningClass) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.flagsBitMap = flagsBitMap;
            this.kind = kind;
            this.members = members;
            this.interfaces = interfaces;
            this.constructors = constructors;
            this.supertype = supertype;
            this.owningClass = owningClass;
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
            return build(1, fullyQualifiedName, Kind.Class, emptyList(), emptyList(), null, null, null, true);
        }

        /**
         * Build a class type only from the class' fully qualified name and kind. Since we are not providing any member,
         * type parameter, interface, or supertype information, this fully qualified name could potentially match on
         * more than one version of the class found in the type cache. This method will simply pick one of them, because
         * there is no way of selecting between the versions of the class based solely on the fully qualified class name.
         *
         * @param fullyQualifiedName The fully qualified name of the class to build
         * @param kind               The class kind : Class, Annotation, Enum, or Interface
         * @return Any class found in the type cache
         */
        public static Class build(String fullyQualifiedName, Kind kind) {
            return build(1, fullyQualifiedName, kind, emptyList(), emptyList(), null, null, null, true);
        }

        public static Class build(Set<Flag> flags,
                                  String fullyQualifiedName,
                                  Kind kind,
                                  List<Variable> members,
                                  List<FullyQualified> interfaces,
                                  List<Method> constructors,
                                  @Nullable FullyQualified supertype,
                                  @Nullable FullyQualified owningClass) {
            return build(Flag.flagsToBitMap(flags), fullyQualifiedName, kind, members, interfaces, constructors, supertype, owningClass, false);
        }

        @JsonCreator
        protected static Class build(int flagsBitMap,
                                     String fullyQualifiedName,
                                     Kind kind,
                                     List<Variable> members,
                                     List<FullyQualified> interfaces,
                                     @Nullable List<Method> constructors,
                                     @Nullable FullyQualified supertype,
                                     @Nullable FullyQualified owningClass) {
            return build(flagsBitMap, fullyQualifiedName, kind, members, interfaces, constructors, supertype, owningClass, false);
        }

        public static Class build(int flagsBitMap,
                                  String fullyQualifiedName,
                                  Kind kind,
                                  List<Variable> members,
                                  List<FullyQualified> interfaces,
                                  @Nullable List<Method> constructors,
                                  @Nullable FullyQualified supertype,
                                  @Nullable FullyQualified owningClass,
                                  boolean relaxedClassTypeMatching) {

            List<Variable> sortedMembers;
            if (fullyQualifiedName.equals("java.lang.String")) {
                //There is a "serialPersistentFields" member within the String class which is used in normal Java
                //serialization to customize how the String field is serialized. This field is tripping up Jackson
                //serialization and is intentionally filtered to prevent errors.
                sortedMembers = members.stream().filter(m -> !m.getName().equals("serialPersistentFields")).collect(Collectors.toList());
            } else {
                sortedMembers = new ArrayList<>(members);
            }
            sortedMembers.sort(comparing(Variable::getName));

            JavaType.Class candidate = new Class(flagsBitMap, fullyQualifiedName, kind, sortedMembers, interfaces, constructors, supertype, owningClass);

            // This logic will attempt to match the candidate against a candidate in the flyweights. If a match is found,
            // that instance is used over the new candidate to prevent a large memory footprint. If relaxed class type
            // matching is "true" any variant with the same ID will be used. If the relaxed class type matching is "false",
            // equality is determined by comparing the immediate structure of the class and also comparing the supertype
            // hierarchies.

            synchronized (flyweights) {
                Set<JavaType.Class> variants = flyweights.computeIfAbsent(candidate.fullyQualifiedName, fqn -> new HashSet<>());

                if (relaxedClassTypeMatching) {
                    if (variants.isEmpty()) {
                        variants.add(candidate);
                        return candidate;
                    }
                    return variants.iterator().next();
                } else {
                    for (Class v : variants) {
                        if (v.deepEquals(candidate)) {
                            return v;
                        }
                    }

                    if (candidate.supertype == null) {

                        return variants.stream().filter(v -> v.supertype != null).findFirst().orElseGet(() -> {
                            variants.add(candidate);
                            return candidate;
                        });
                    }
                    variants.add(candidate);
                    return candidate;
                }
            }
        }

        /**
         * Lazily built so that a {@link org.openrewrite.java.JavaParser} operating over a set of code
         * has an opportunity to build {@link Class} instances for sources found in the repo that can provide richer information
         * for constructor parameter types.
         *
         * @return The set of public constructors for a class.
         */
        public List<Method> getConstructors() {
            List<Method> constructorsTemp = constructors;
            if (constructorsTemp != null) {
                return constructorsTemp;
            }

            synchronized (flyweights) {
                //Double checked locking.
                constructorsTemp = constructors;
                if (constructorsTemp != null) {
                    return constructorsTemp;
                }
                List<Method> reflectedConstructors = new ArrayList<>();
                try {
                    java.lang.Class<?> reflectionClass = java.lang.Class.forName(fullyQualifiedName, false, JavaType.class.getClassLoader());
                    for (Constructor<?> constructor : reflectionClass.getConstructors()) {
                        ShallowClass selfType = new ShallowClass(fullyQualifiedName);

                        // TODO can we generate a generic signature as well?
                        Method.Signature resolvedSignature = new Method.Signature(selfType, Arrays.stream(constructor.getParameterTypes())
                                .map(Class::resolveTypeFromClass)
                                .collect(toList()));

                        List<String> parameterNames = Arrays.stream(constructor.getParameters()).map(Parameter::getName).collect(toList());

                        // Name each constructor "<reflection_constructor>" to intentionally disambiguate from method signatures parsed
                        // by JavaParser, which may have richer information but which would only be available for types found in the source
                        // repository.
                        reflectedConstructors.add(Method.build(singleton(Flag.Public), selfType, "<reflection_constructor>",
                                resolvedSignature, resolvedSignature, parameterNames, Collections.emptyList()));
                    }
                    constructors = reflectedConstructors;
                } catch (ClassNotFoundException ignored) {
                    // oh well, we tried
                }
                return reflectedConstructors;
            }
        }

        private static JavaType resolveTypeFromClass(java.lang.Class<?> clazz) {
            if (!clazz.isPrimitive() && !clazz.isArray()) {
                return Class.build(clazz.getName());
            } else if (clazz.isPrimitive()) {
                if (clazz == boolean.class) {
                    return Primitive.Boolean;
                } else if (clazz == String.class) {
                    return Primitive.String;
                } else if (clazz == int.class) {
                    return Primitive.Int;
                } else if (clazz == long.class) {
                    return Primitive.Long;
                } else if (clazz == double.class) {
                    return Primitive.Double;
                } else if (clazz == char.class) {
                    return Primitive.Char;
                } else if (clazz == byte.class) {
                    return Primitive.Byte;
                } else if (clazz == float.class) {
                    return Primitive.Float;
                } else if (clazz == short.class) {
                    return Primitive.Short;
                } else {
                    throw new IllegalArgumentException("Unknown primitive argument");
                }
            } else {
                return new JavaType.Array(resolveTypeFromClass(clazz.getComponentType()));
            }
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

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Class)) {
                return false;
            }

            Class c = (Class) type;
            return
                    this == c || (kind == c.kind && flagsBitMap == c.flagsBitMap &&
                            fullyQualifiedName.equals(c.fullyQualifiedName) &&
                            TypeUtils.deepEquals(members, c.members) &&
                            TypeUtils.deepEquals(interfaces, c.interfaces) &&
                            TypeUtils.deepEquals(supertype, c.supertype));
        }

        @Override
        public String toString() {
            return "Class{" + fullyQualifiedName + '}';
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    class Parameterized extends FullyQualified {
        private static class TypeTrieNode {
            private Map<JavaType, TypeTrieNode> children;
            private Parameterized parameterized;

            public Parameterized find(FullyQualified type, List<JavaType> typeParameters) {
                TypeTrieNode node = find(type, typeParameters, -1);
                return node.parameterized;
            }

            private TypeTrieNode find(FullyQualified type, List<JavaType> typeParameters, int index) {
                if (children == null) {
                    children = new IdentityHashMap<>(4);
                }

                TypeTrieNode node;
                if (index == -1) {
                    node = children.computeIfAbsent(type, t -> new TypeTrieNode());
                } else {
                    node = children.computeIfAbsent(typeParameters.get(index), t -> new TypeTrieNode());
                }

                if (index == typeParameters.size() - 1) {
                    if (node.parameterized == null) {
                        node.parameterized = new Parameterized(type, typeParameters);
                    }
                    return node;
                } else {
                    return node.find(type, typeParameters, index + 1);
                }
            }
        }

        private static final TypeTrieNode flyweight = new TypeTrieNode();

        private final FullyQualified type;
        private final List<JavaType> typeParameters;

        @JsonCreator
        public static Parameterized build(FullyQualified type, List<JavaType> typeParameters) {
            return flyweight.find(type, typeParameters);
        }

        private Parameterized(FullyQualified type, List<JavaType> typeParameters) {
            this.type = type;
            this.typeParameters = typeParameters;
        }

        @Override
        public String getFullyQualifiedName() {
            return type.getFullyQualifiedName();
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
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Parameterized)) {
                return false;
            }

            Parameterized p = (Parameterized) type;
            return this == p || (TypeUtils.deepEquals(this.type, p.type) &&
                    TypeUtils.deepEquals(this.typeParameters, p.typeParameters));
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class Cyclic extends FullyQualified {
        private final String fullyQualifiedName;

        public Cyclic(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public Class.Kind getKind() {
            return Class.Kind.Class;
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return test.length == 1 && test[0] == Flag.Public;
        }

        @Override
        public Set<Flag> getFlags() {
            return Collections.singleton(Flag.Public);
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return Collections.emptyList();
        }

        @Override
        public List<Variable> getMembers() {
            return Collections.emptyList();
        }

        @Override
        public Class getOwningClass() {
            return null;
        }

        @Override
        public Class getSupertype() {
            return Class.OBJECT;
        }

        @Override
        public List<Variable> getVisibleSupertypeMembers() {
            return Collections.emptyList();
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this.equals(type);
        }

        @Override
        public String toString() {
            return "Cyclic{" + fullyQualifiedName + '}';
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    class Variable implements JavaType {
        private final String name;

        @Nullable
        private final JavaType type;

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;

        public Variable(Set<Flag> flags, String name, @Nullable JavaType type) {
            this(Flag.flagsToBitMap(flags), name, type);
        }

        @JsonCreator
        public Variable(int flagsBitMap, String name, @Nullable JavaType type) {
            this.name = name;
            this.type = type;
            this.flagsBitMap = flagsBitMap;
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Variable)) {
                return false;
            }

            Variable v = (Variable) type;
            return this == v || (name.equals(v.name) && TypeUtils.deepEquals(this.type, v.type) && flagsBitMap == v.flagsBitMap);
        }
    }

    @Getter
    class Method implements JavaType {
        private static final Map<FullyQualified, Map<String, Set<Method>>> flyweights = new WeakHashMap<>();

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;

        private final FullyQualified declaringType;
        private final String name;
        private final Signature genericSignature;
        private final Signature resolvedSignature;
        private final List<String> paramNames;
        private final List<FullyQualified> thrownExceptions;

        private Method(int flagsBitMap, FullyQualified declaringType, String name,
                       @Nullable Signature genericSignature, Signature resolvedSignature, List<String> paramNames,
                       List<FullyQualified> thrownExceptions) {
            this.flagsBitMap = flagsBitMap;
            this.declaringType = declaringType;
            this.name = name;
            this.genericSignature = genericSignature;
            this.resolvedSignature = resolvedSignature;
            this.paramNames = paramNames;
            this.thrownExceptions = thrownExceptions;
        }

        public static Method build(Set<Flag> flags, FullyQualified declaringType, String name,
                                   @Nullable Signature genericSignature, Signature resolvedSignature,
                                   List<String> paramNames, List<FullyQualified> thrownExceptions) {
            return build(Flag.flagsToBitMap(flags), declaringType, name, genericSignature, resolvedSignature, paramNames, thrownExceptions);
        }

        @JsonCreator
        public static Method build(int flagsBitMap, FullyQualified declaringType, String name,
                                   @Nullable Signature genericSignature, Signature resolvedSignature, List<String> paramNames,
                                   List<FullyQualified> thrownExceptions) {

            Method test = new Method(flagsBitMap, declaringType, name, genericSignature, resolvedSignature, paramNames, thrownExceptions);

            synchronized (flyweights) {
                Set<Method> methods = flyweights
                        .computeIfAbsent(declaringType, dt -> new HashMap<>())
                        .computeIfAbsent(name, n -> new HashSet<>());

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
            private final JavaType returnType;

            private final List<JavaType> paramTypes;
        }

        private static boolean signatureDeepEquals(@Nullable Signature s1, @Nullable Signature s2) {
            return s1 == null ? s2 == null : s1 == s2 || (s2 != null &&
                    TypeUtils.deepEquals(s1.returnType, s2.returnType) &&
                    TypeUtils.deepEquals(s1.paramTypes, s2.paramTypes));
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public Method withFlags(Set<Flag> flags) {
            return new Method(Flag.flagsToBitMap(flags), this.declaringType, this.name, this.genericSignature, this.resolvedSignature, this.paramNames, this.thrownExceptions);
        }

        public Method withDeclaringType(FullyQualified declaringType) {
            return new Method(this.flagsBitMap, declaringType, this.name, this.genericSignature, this.resolvedSignature, this.paramNames, this.thrownExceptions);
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Method)) {
                return false;
            }

            Method m = (Method) type;
            return this == m || (paramNames.equals(m.paramNames) &&
                    flagsBitMap == m.flagsBitMap &&
                    declaringType.deepEquals(m.declaringType) &&
                    signatureDeepEquals(genericSignature, m.genericSignature) &&
                    signatureDeepEquals(resolvedSignature, m.resolvedSignature) &&
                    TypeUtils.deepEquals(thrownExceptions, m.thrownExceptions));
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class GenericTypeVariable extends FullyQualified {
        private final String fullyQualifiedName;

        @Nullable
        private final FullyQualified bound;

        @Override
        public Class.Kind getKind() {
            return bound == null ? Class.Kind.Class : bound.getKind();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return bound != null && bound.hasFlags(test);
        }

        @Override
        public Set<Flag> getFlags() {
            return bound == null ? Collections.emptySet() : bound.getFlags();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return bound == null ? Collections.emptyList() : bound.getInterfaces();
        }

        @Override
        public List<Variable> getMembers() {
            return bound == null ? Collections.emptyList() : bound.getMembers();
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
            return bound == null ? Collections.emptyList() : bound.getVisibleSupertypeMembers();
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof GenericTypeVariable)) {
                return false;
            }

            GenericTypeVariable generic = (GenericTypeVariable) type;
            return this == generic || (fullyQualifiedName.equals(generic.fullyQualifiedName) &&
                    TypeUtils.deepEquals(bound, generic.bound));
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class Wildcard extends FullyQualified {

        private final String fullyQualifiedName;
        private final BoundKind boundKind;
        @Nullable
        private final FullyQualified bound;

        @Override
        public Class.Kind getKind() {
            return bound == null ? Class.Kind.Class : bound.getKind();
        }

        @Override
        public boolean hasFlags(Flag... test) {
            return bound != null && bound.hasFlags(test);
        }

        @Override
        public Set<Flag> getFlags() {
            return bound == null ? Collections.emptySet() : bound.getFlags();
        }

        @Override
        public List<FullyQualified> getInterfaces() {
            return bound == null ? Collections.emptyList() : bound.getInterfaces();
        }

        @Override
        public List<Variable> getMembers() {
            return bound == null ? Collections.emptyList() : bound.getMembers();
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
            return bound == null ? Collections.emptyList() : bound.getVisibleSupertypeMembers();
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Wildcard)) {
                return false;
            }

            Wildcard wildcard = (Wildcard) type;
            return this == wildcard || (
                    this.boundKind == wildcard.boundKind &&
                            this.fullyQualifiedName.equals(wildcard.fullyQualifiedName) &&
                            TypeUtils.deepEquals(this.bound, wildcard.bound));
        }

        public enum BoundKind {
            Extends,
            Super,
            Unbound,
        }
    }

    @Data
    class Array implements JavaType {
        @Nullable
        private final JavaType elemType;

        public Array(@Nullable JavaType elemType) {
            this.elemType = elemType;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return type instanceof Array && (this == type || (elemType != null && elemType.deepEquals(((Array) type).elemType)));
        }
    }

    enum Primitive implements JavaType {
        Boolean("boolean"),
        Byte("byte"),
        Char("char"),
        Double("double"),
        Float("float"),
        Int("int"),
        Long("long"),
        Short("short"),
        Void("void"),
        String("String"),
        None(""),
        Wildcard("*"),
        Null("null");

        private final String keyword;

        Primitive(String keyword) {
            this.keyword = keyword;
        }

        @Nullable
        public static Primitive fromKeyword(String keyword) {
            for (Primitive p : values()) {
                if (p.keyword.equals(keyword)) {
                    return p;
                }
            }
            return null;
        }

        public String getKeyword() {
            return this.keyword;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type;
        }
    }
}
