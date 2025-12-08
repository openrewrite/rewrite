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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Reference;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class UsesType<P> extends TreeVisitor<Tree, P> {

    @Nullable
    @Getter
    private final String fullyQualifiedType;

    @Nullable
    @Getter
    private final Predicate<JavaType> typePattern;

    private final Reference.Matcher referenceMatcher;

    @Nullable
    private final Boolean includeImplicit;

    public UsesType(String fullyQualifiedType, @Nullable Boolean includeImplicit) {
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
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof JavaSourceFile || sourceFile instanceof SourceFileWithReferences;
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
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
        } else if (tree instanceof SourceFileWithReferences) {
            SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
            SourceFileWithReferences.References references = sourceFile.getReferences();
            for (Reference ignored : references.findMatches(referenceMatcher)) {
                return SearchResult.found(sourceFile);
            }
        }
        return tree;
    }

    private JavaSourceFile maybeMark(JavaSourceFile c, @Nullable JavaType type) {
        if (type == null) {
            return c;
        }

        if (typePattern != null && TypeUtils.isAssignableTo(typePattern, type) ||
            fullyQualifiedType != null && TypeUtils.isAssignableTo(fullyQualifiedType, type)) {
            return SearchResult.found(c);
        }

        return c;
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
                String packageName = ((JavaType.FullyQualified) type).getPackageName();
                return packageName.equals(prefix) || packageName.startsWith(subPackagePrefix);
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
            return reference.getKind() == Reference.Kind.TYPE && qualifiedName.equals(reference.getValue());
        }

        @Override
        public Reference.Renamer createRenamer(String newName) {
            return reference -> newName;
        }
    }
}
