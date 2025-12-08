/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.trait.Reference;

@Getter
public class TypeMatcher implements Reference.Matcher {

    private final String signature;
    private final TypeNameMatcher typeNameMatcher;

    /**
     * Whether to match on subclasses.
     */
    @Getter
    private final boolean matchInherited;

    public TypeMatcher(@Nullable String typePattern) {
        this(typePattern, false);
    }

    public TypeMatcher(@Nullable String typePattern, boolean matchInherited) {
        this.signature = typePattern == null ? ".*" : typePattern;
        this.matchInherited = matchInherited;

        if (StringUtils.isBlank(typePattern)) {
            // Blank means wildcard - use PatternTypeNameMatcher with FullWildcard type
            typeNameMatcher = PatternTypeNameMatcher.fullWildcard("*");
        } else {
            // Parse the type pattern
            ParsedType parsed = parseTypePattern(typePattern);
            String pattern = parsed.getFullPattern();

            // Determine if it's a plain identifier (no wildcards)
            if (!pattern.contains("*") && !pattern.contains("..")) {
                typeNameMatcher = new ExactTypeNameMatcher(pattern);
            } else {
                // All patterns (including "*" and "*..*") use PatternTypeNameMatcher
                typeNameMatcher = PatternTypeNameMatcher.fromPattern(pattern);
            }
        }
    }

    public boolean matches(@Nullable TypeTree tt) {
        return tt != null && matches(tt.getType());
    }

    public boolean matchesPackage(String packageName) {
        // Direct match first
        if (typeNameMatcher.matches(packageName)) {
            return true;
        }

        // If packageName ends with .*, try matching with the class name from signature
        if (packageName.endsWith(".*")) {
            String packagePrefix = packageName.substring(0, packageName.length() - 2);
            int lastDot = signature.lastIndexOf('.');
            if (lastDot >= 0) {
                String fullName = packagePrefix + "." + signature.substring(lastDot + 1);
                return typeNameMatcher.matches(fullName);
            }
        }

        return false;
    }

    public boolean matches(@Nullable JavaType type) {
        if (type instanceof JavaType.FullyQualified && typeNameMatcher.matches(((JavaType.FullyQualified) type).getFullyQualifiedName())) {
            return true;
        }

        if (matchInherited && type instanceof JavaType.FullyQualified) {
            return TypeUtils.isOfTypeWithName(
                    TypeUtils.asFullyQualified(type),
                    matchInherited,
                    this::matchesTargetTypeName
            );
        }

        if (type instanceof JavaType.Primitive && typeNameMatcher.matches(type.toString())) {
            return true;
        }

        return false;
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return typeNameMatcher.matches(fullyQualifiedTypeName);
    }

    @Override
    public boolean matchesReference(Reference reference) {
        return reference.getKind() == Reference.Kind.TYPE && matchesTargetTypeName(reference.getValue());
    }

    @Override
    public Reference.Renamer createRenamer(String newName) {
        return reference -> newName;
    }

    /**
     * Parses a type pattern, handling array dimensions and qualifying simple type names.
     * Package-private for use by MethodMatcher.
     */
    static ParsedType parseTypePattern(String pattern) {
        // Handle array types by preserving [] suffixes
        int arrayDimensions = 0;
        int pos = pattern.length();
        while (pos >= 2 && pattern.charAt(pos - 2) == '[' && pattern.charAt(pos - 1) == ']') {
            arrayDimensions++;
            pos -= 2;
        }

        String baseType = arrayDimensions > 0 ? pattern.substring(0, pos).trim() : pattern;

        // Special handling for simple type names
        if (!baseType.contains(".") && !baseType.contains("*")) {
            // Check if it's a primitive
            if (Character.isLowerCase(baseType.charAt(0)) && JavaType.Primitive.fromKeyword(baseType) != null) {
                // It's a primitive, keep as-is
            } else {
                // Check if it's a java.lang type
                if (TypeUtils.findQualifiedJavaLangTypeName(baseType) != null) {
                    baseType = "java.lang." + baseType;
                }
            }
        }

        return new ParsedType(baseType, arrayDimensions);
    }

    /**
     * Represents a parsed type pattern with its base type and array dimensions.
     * Package-private for use by MethodMatcher.
     */
    static class ParsedType {
        private final String baseType;
        private final int arrayDimensions;

        ParsedType(String baseType, int arrayDimensions) {
            this.baseType = baseType;
            this.arrayDimensions = arrayDimensions;
        }

        public String getBaseType() {
            return baseType;
        }

        public int getArrayDimensions() {
            return arrayDimensions;
        }

        public String getFullPattern() {
            if (arrayDimensions == 0) {
                return baseType;
            }
            StringBuilder result = new StringBuilder(baseType);
            for (int i = 0; i < arrayDimensions; i++) {
                result.append("[]");
            }
            return result.toString();
        }
    }
}
