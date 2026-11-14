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
package org.openrewrite.java.search;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.SourceFileWithReferences;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.TypeNameMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class UsesType<P> extends TreeVisitor<Tree, P> {

    /**
     * Whether {@link TypesInUse} on the current runtime exposes the trie-backed query methods.
     * Recipes are commonly built against a newer {@code rewrite-java} than is deployed at runtime
     * (parent-loaded), so we probe once at class load and fall back to the legacy iteration when
     * the new methods aren't there. Once enough time has passed for older runtimes to fall out of
     * support, the probe and the fallback path can be removed.
     */
    private static final boolean TYPES_IN_USE_HAS_TRIE_API = probeTypesInUse(String.class, boolean.class);

    /**
     * Whether {@link TypesInUse} on the current runtime can answer without walking supertypes, probed for the same
     * reason as {@link #TYPES_IN_USE_HAS_TRIE_API}. Without it, {@link #named} falls back to the legacy iteration with
     * exact name comparisons.
     */
    private static final boolean TYPES_IN_USE_HAS_EXACT_API = probeTypesInUse(String.class, boolean.class, boolean.class);

    private static boolean probeTypesInUse(Class<?>... hasTypeParameters) {
        try {
            TypesInUse.class.getMethod("hasType", hasTypeParameters);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Nullable
    @Getter
    private final String fullyQualifiedType;

    @Nullable
    @Getter
    private final Predicate<JavaType> typePattern;

    private final Reference.Matcher referenceMatcher;

    @Nullable
    private final Boolean includeImplicit;

    private final boolean includeSubtypes;

    /**
     * Matches a compilation unit that uses the type itself: it appears in the unit's type attribution or imports.
     * A type the unit only reaches through a supertype of a type it uses doesn't count, so neither does
     * {@code java.util.Collection} for a unit that only uses {@code java.util.List}.
     *
     * @param fullyQualifiedType a fully qualified type name, or a pattern such as {@code com.foo.*} or {@code com.foo..*}
     */
    public static <P> UsesType<P> named(String fullyQualifiedType) {
        return new UsesType<>(fullyQualifiedType, false, false);
    }

    /**
     * Matches a compilation unit that uses the type or any type assignable to it, such as {@code java.util.Collection}
     * for a unit that uses {@code java.util.List}. For a visitor that matches by assignability or overrides.
     *
     * @param fullyQualifiedType a fully qualified type name, or a pattern such as {@code com.foo.*} or {@code com.foo..*}
     */
    public static <P> UsesType<P> assignableTo(String fullyQualifiedType) {
        return new UsesType<>(fullyQualifiedType, false, true);
    }

    /**
     * Matches a compilation unit that uses the type or any type assignable to it, and with {@code includeImplicit}
     * also one where it appears only in the signature of a method the unit calls.
     */
    public UsesType(String fullyQualifiedType, @Nullable Boolean includeImplicit) {
        this(fullyQualifiedType, includeImplicit, true);
    }

    private UsesType(String fullyQualifiedType, @Nullable Boolean includeImplicit, boolean includeSubtypes) {
        if (fullyQualifiedType.contains("*")) {
            this.fullyQualifiedType = null;
            final int length = fullyQualifiedType.length();
            if (fullyQualifiedType.indexOf('*') == length - 1) {
                int dotdot = fullyQualifiedType.indexOf("..");
                if (dotdot == -1 && length > 1 && fullyQualifiedType.charAt(length - 2) == '.') {
                    PackagePattern packagePattern = new PackagePattern(fullyQualifiedType.substring(0, fullyQualifiedType.length() - 2));
                    this.typePattern = packagePattern;
                    this.referenceMatcher = packagePattern;
                } else if (dotdot == length - 3) {
                    PackagePrefixPattern packagePrefixPattern = new PackagePrefixPattern(fullyQualifiedType.substring(0, dotdot));
                    this.typePattern = packagePrefixPattern;
                    this.referenceMatcher = packagePrefixPattern;
                } else {
                    GenericPattern genericPattern = new GenericPattern(TypeNameMatcher.fromPattern(fullyQualifiedType));
                    this.typePattern = genericPattern;
                    this.referenceMatcher = genericPattern;
                }
            } else {
                GenericPattern genericPattern = new GenericPattern(TypeNameMatcher.fromPattern(fullyQualifiedType));
                this.typePattern = genericPattern;
                this.referenceMatcher = genericPattern;
            }
        } else {
            this.fullyQualifiedType = fullyQualifiedType;
            this.typePattern = null;
            this.referenceMatcher = new ExactMatch(fullyQualifiedType);
        }
        this.includeImplicit = includeImplicit;
        this.includeSubtypes = includeSubtypes;
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof JavaSourceFile || sourceFile instanceof SourceFileWithReferences;
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
            Tree found = visitTypes(cu);
            return found != cu || !importsUnresolvedMatch(cu) ? found : SearchResult.found(cu);
        } else if (tree instanceof SourceFileWithReferences) {
            SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
            SourceFileWithReferences.References references = sourceFile.getReferences();
            for (Reference ignored : references.findMatches(referenceMatcher)) {
                return SearchResult.found(sourceFile);
            }
        }
        return tree;
    }

    /// An unresolved import still names a type whose uses [TypesInUse] can't see, as when parsed without it on
    /// the classpath. Package imports are skipped, as they have no type even when resolved.
    private boolean importsUnresolvedMatch(JavaSourceFile cu) {
        for (J.Import anImport : cu.getImports()) {
            J.FieldAccess qualid = anImport.getQualid();
            if ("*".equals(qualid.getSimpleName())) {
                continue;
            }
            JavaType type = anImport.isStatic() ? qualid.getTarget().getType() : qualid.getType();
            if (type != null && !(type instanceof JavaType.Unknown)) {
                continue;
            }
            if (nameMatches(anImport.getTypeName(), anImport.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean nameMatches(String typeName, String packageName) {
        if (fullyQualifiedType != null) {
            return TypeUtils.fullyQualifiedNamesAreEqual(fullyQualifiedType, typeName);
        }
        if (typePattern instanceof GenericPattern) {
            return ((GenericPattern) typePattern).getMatcher().matches(typeName.replace('$', '.')) ||
                   ((GenericPattern) typePattern).getMatcher().matches(typeName);
        }
        if (typePattern instanceof PackagePattern) {
            return packageName.equals(((PackagePattern) typePattern).getName());
        }
        if (typePattern instanceof PackagePrefixPattern) {
            String prefix = ((PackagePrefixPattern) typePattern).getPrefix();
            return packageName.equals(prefix) || packageName.startsWith(prefix + '.');
        }
        return false;
    }

    private Tree visitTypes(JavaSourceFile cu) {
        boolean implicit = Boolean.TRUE.equals(includeImplicit);

        if (!includeSubtypes && TYPES_IN_USE_HAS_EXACT_API) {
            TypesInUse typesInUse = cu.getTypesInUse();
            if (fullyQualifiedType != null) {
                return typesInUse.hasType(fullyQualifiedType, implicit, false) ? SearchResult.found(cu) : cu;
            }
            if (typePattern instanceof PackagePattern) {
                return typesInUse.hasTypeInPackage(((PackagePattern) typePattern).getName(), implicit, false) ?
                        SearchResult.found(cu) : cu;
            }
            if (typePattern instanceof PackagePrefixPattern) {
                return typesInUse.hasTypeInPackageOrSubpackage(((PackagePrefixPattern) typePattern).getPrefix(), implicit, false) ?
                        SearchResult.found(cu) : cu;
            }
            if (typePattern instanceof GenericPattern) {
                return typesInUse.hasTypeMatching(((GenericPattern) typePattern).getMatcher(), implicit, false) ?
                        SearchResult.found(cu) : cu;
            }
            return cu;
        }

        if (includeSubtypes && TYPES_IN_USE_HAS_TRIE_API) {
            // Exact FQN: O(1) closure lookup on TypesInUse.
            if (fullyQualifiedType != null) {
                return cu.getTypesInUse().hasType(fullyQualifiedType, implicit) ? SearchResult.found(cu) : cu;
            }
            // Package-shaped wildcards (com.foo.* / com.foo..*): O(prefix-depth) trie lookup.
            if (typePattern instanceof PackagePattern) {
                return cu.getTypesInUse().hasTypeInPackage(((PackagePattern) typePattern).getName(), implicit)
                        ? SearchResult.found(cu) : cu;
            }
            if (typePattern instanceof PackagePrefixPattern) {
                return cu.getTypesInUse().hasTypeInPackageOrSubpackage(((PackagePrefixPattern) typePattern).getPrefix(), implicit)
                        ? SearchResult.found(cu) : cu;
            }
            // GenericPattern (regex-like): one trie walk, memoized per (matcher, implicit).
            if (typePattern instanceof GenericPattern) {
                return cu.getTypesInUse().hasTypeMatching(((GenericPattern) typePattern).getMatcher(), implicit)
                        ? SearchResult.found(cu) : cu;
            }
            return cu;
        }

        // Legacy fallback: TypesInUse on the runtime classpath predates the query this needs. Iterate
        // types-in-use, imports, and (when implicit) used-method types directly.
        return legacyVisit(cu);
    }

    private JavaSourceFile legacyVisit(JavaSourceFile cu) {
        JavaSourceFile c = cu;
        for (JavaType type : c.getTypesInUse().getTypesInUse()) {
            JavaType checkType = type instanceof JavaType.Primitive ? type : TypeUtils.asFullyQualified(type);
            if ((c = maybeMark(c, checkType)) != cu) {
                return c;
            }
        }
        for (J.Import anImport : c.getImports()) {
            if (anImport.isStatic()) {
                if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType()))) != cu) {
                    return c;
                }
            } else if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getType()))) != cu) {
                return c;
            }
        }
        if (Boolean.TRUE.equals(includeImplicit)) {
            for (JavaType.Method method : c.getTypesInUse().getUsedMethods()) {
                if ((c = maybeMark(c, method.getDeclaringType())) != cu) {
                    return c;
                }
                if ((c = maybeMark(c, method.getReturnType())) != cu) {
                    return c;
                }
                for (JavaType parameterType : method.getParameterTypes()) {
                    if ((c = maybeMark(c, parameterType)) != cu) {
                        return c;
                    }
                }
            }
        }
        return c;
    }

    private JavaSourceFile maybeMark(JavaSourceFile c, @Nullable JavaType type) {
        if (type == null) {
            return c;
        }
        if (includeSubtypes ? isAssignable(type) : isNamed(type)) {
            return SearchResult.found(c);
        }
        return c;
    }

    private boolean isAssignable(JavaType type) {
        return typePattern != null && TypeUtils.isAssignableTo(typePattern, type) ||
               fullyQualifiedType != null && TypeUtils.isAssignableTo(fullyQualifiedType, type);
    }

    private boolean isNamed(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            JavaType.Primitive p = (JavaType.Primitive) type;
            if (p == JavaType.Primitive.String && isNamed(JavaType.ShallowClass.build("java.lang.String"))) {
                return true;
            }
            return typePattern != null ? typePattern.test(p) : fullyQualifiedType != null && fullyQualifiedType.equals(p.getKeyword());
        }
        if (!(type instanceof JavaType.FullyQualified)) {
            return false;
        }
        if (typePattern != null) {
            return typePattern.test(type);
        }
        return TypeUtils.fullyQualifiedNamesAreEqual(fullyQualifiedType, ((JavaType.FullyQualified) type).getFullyQualifiedName()) ||
               type instanceof JavaType.Parameterized && type.toString().equals(fullyQualifiedType);
    }

    @Value
    private static class PackagePrefixPattern implements Predicate<JavaType>, Reference.Matcher {
        String prefix;
        String subPackagePrefix;

        public PackagePrefixPattern(String prefix) {
            this.prefix = prefix;
            this.subPackagePrefix = prefix + '.';
        }

        @Override
        public boolean test(JavaType type) {
            if (type instanceof JavaType.FullyQualified) {
                String fqn = ((JavaType.FullyQualified) type).getFullyQualifiedName();
                if (!fqn.startsWith(prefix)) {
                    return false;
                }
                int lastDot = fqn.lastIndexOf('.');
                // packageName.equals(prefix): true when lastDot is exactly at prefix.length()
                // packageName.startsWith(subPackagePrefix): true when lastDot > prefix.length()
                //   and the char at prefix.length() is '.' (completing the subPackagePrefix match)
                return lastDot == prefix.length() ||
                       (lastDot > prefix.length() && fqn.charAt(prefix.length()) == '.');
            }
            return false;
        }

        @Override
        public boolean matchesReference(Reference reference) {
            return reference.getKind() == Reference.Kind.TYPE && reference.getValue().startsWith(subPackagePrefix);
        }

        @Override
        public Reference.Renamer createRenamer(String newName) {
            return reference -> newName;
        }
    }

    @Value
    private static class PackagePattern implements Predicate<JavaType>, Reference.Matcher {
        String name;

        @Override
        public boolean test(JavaType type) {
            return type instanceof JavaType.FullyQualified &&
                   // optimization to avoid unnecessary memory allocations
                   ((JavaType.FullyQualified) type).getFullyQualifiedName().startsWith(name) &&
                   ((JavaType.FullyQualified) type).getPackageName().equals(name);
        }

        @Override
        public boolean matchesReference(Reference reference) {
            return reference.getKind() == Reference.Kind.TYPE && reference.getValue().startsWith(name + '.');
        }

        @Override
        public Reference.Renamer createRenamer(String newName) {
            return reference -> newName;
        }
    }

    @Value
    private static class GenericPattern implements Predicate<JavaType>, Reference.Matcher {
        TypeNameMatcher matcher;

        @Override
        public boolean test(JavaType type) {
            if (type instanceof JavaType.FullyQualified) {
                return matcher.matches(((JavaType.FullyQualified) type).getFullyQualifiedName());
            } else if (type instanceof JavaType.Primitive) {
                return matcher.matches(((JavaType.Primitive) type).getKeyword());
            }
            return false;
        }

        @Override
        public boolean matchesReference(Reference reference) {
            return reference.getKind() == Reference.Kind.TYPE && matcher.matches(reference.getValue());
        }

        @Override
        public Reference.Renamer createRenamer(String newName) {
            return reference -> newName;
        }
    }

    @Value
    private static class ExactMatch implements Reference.Matcher {
        String qualifiedName;

        @Override
        public boolean matchesReference(Reference reference) {
            return reference.getKind() == Reference.Kind.TYPE &&
                   TypeUtils.fullyQualifiedNamesAreEqual(qualifiedName, reference.getValue());
        }

        @Override
        public Reference.Renamer createRenamer(String newName) {
            return reference -> newName;
        }
    }
}
