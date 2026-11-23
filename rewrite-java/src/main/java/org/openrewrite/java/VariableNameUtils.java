/*
 * Copyright 2022 the original author or authors.
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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.*;

import java.text.Normalizer;
import java.util.*;

/**
 * Names for variables a recipe declares, answered from {@link Scope}. A recipe that needs more than a name, such as the
 * declaration a name reads or whether a rename keeps every name reading the same thing, asks {@link Scope} directly.
 */
@Incubating(since = "7.25.0")
@Value
public class VariableNameUtils {

    private static final String ENUM_NAMES = VariableNameUtils.class.getName() + ".enumNames";

    private VariableNameUtils() {
    }

    /**
     * Generates a variable name without namespace conflicts in the scope of a cursor.
     * <p>
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
     * <p>
     * Since the client recipe may have modified the LST element represented by {@code scope}, this method will
     * visit the value held by {@code scope} once it encounters it in the tree. For this to work, the client recipe
     * must use {@link Cursor#Cursor(Cursor, Object)} to pass a cursor holding the modified LST element.
     * <p>
     * {@link Scope#freeName(String, java.util.function.Predicate)} answers the same question, and also passes over the
     * names a visit has handed out but not yet written into the tree.
     *
     * @param baseName baseName for the variable.
     * @param scope The cursor position of a JavaVisitor, {@link org.openrewrite.java.JavaVisitor#getCursor()}.
     * @param strategy {@link GenerationStrategy} to use if a name already exists with the baseName.
     * @return either the baseName if a namespace conflict does not exist or a name generated with the provided strategy.
     */
    public static String generateVariableName(String baseName, Cursor scope, GenerationStrategy strategy) {
        Set<String> namesInScope = findNamesInScope(scope);
        // Generate a new name to prevent namespace shadowing.
        String newName = baseName;
        if (GenerationStrategy.INCREMENT_NUMBER == strategy) {
            StringBuilder postFix = new StringBuilder();
            char[] charArray = baseName.toCharArray();
            for (int i = charArray.length - 1; i >= 0; i--) {
                char c = charArray[i];
                if (Character.isDigit(c)) {
                    postFix.append(c);
                } else {
                    break;
                }
            }

            baseName = baseName.substring(0, baseName.length() - postFix.length());
            int count = postFix.length() == 0 ? 0 : Integer.parseInt(postFix.reverse().toString());
            while (namesInScope.contains(newName) || JavaKeywordUtils.isReservedKeyword(newName) || JavaKeywordUtils.isReservedLiteral(newName)) {
                newName = baseName + (count += 1);
            }
        }

        return newName;
    }

    /**
     * Replace accent and diacritics with normalized characters.
     * @param name variable name to normalize.
     * @return normalized name.
     */
    public static String normalizeName(String name) {
        if (name.isEmpty() || Normalizer.isNormalized(name, Normalizer.Form.NFKD)) {
            return name;
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * Find the names of variables that already exist in the scope of a cursor.
     * A JavaSourceFile must be available in the Cursor path to account for all names
     * available in the cursor scope.
     * <p>
     * They include the names of the bindings {@link Scope#visible()} at the cursor and {@link Scope#declaredWithin(Cursor)
     * declared within} its tree, which {@link Scope} gives with the declaration of each.
     *
     * @param scope The cursor position of a JavaVisitor, {@link org.openrewrite.java.JavaVisitor#getCursor()}.
     * @return Variable names available in the name scope of the cursor.
     */
    public static Set<String> findNamesInScope(Cursor scope) {
        if (scope.firstEnclosing(JavaSourceFile.class) == null) {
            throw new IllegalStateException("A JavaSourceFile is required in the cursor path.");
        }
        Set<String> names = new HashSet<>();
        Scope.at(scope).visible()
                .filter(binding -> binding.getNamespace() == Scope.Namespace.VARIABLE)
                .forEach(binding -> names.add(binding.getName()));
        Scope.declaredWithin(scope)
                .filter(binding -> binding.getNamespace() == Scope.Namespace.VARIABLE)
                .forEach(binding -> names.add(binding.getName()));
        // A class at or under the cursor has the fields it inherits too, which nothing under it declares.
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J preVisit(J tree, Set<String> found) {
                if (tree instanceof J.ClassDeclaration ||
                    tree instanceof J.NewClass && ((J.NewClass) tree).getBody() != null) {
                    for (Scope.Binding binding : Scope.at(getCursor()).getBindings()) {
                        if (binding.getNamespace() == Scope.Namespace.VARIABLE &&
                            binding.getKind() == Scope.Kind.INHERITED) {
                            found.add(binding.getName());
                        }
                    }
                }
                return tree;
            }
        }.visit(scope.<J>getValue(), names, scope.getParentOrThrow());
        // A variable named like an enum the file uses would obscure that enum wherever it qualifies a constant
        // (JLS 6.4.2).
        Cursor sourceFile = scope.getValue() instanceof JavaSourceFile ? scope :
                scope.dropParentUntil(JavaSourceFile.class::isInstance);
        names.addAll(sourceFile.computeMessageIfAbsent(ENUM_NAMES, k -> {
            Set<String> enums = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> found) {
                    if (identifier.getType() instanceof JavaType.Class &&
                        ((JavaType.Class) identifier.getType()).getKind() == JavaType.FullyQualified.Kind.Enum) {
                        found.add(identifier.getSimpleName());
                    }
                    return identifier;
                }
            }.visit(sourceFile.<JavaSourceFile>getValue(), enums);
            return enums;
        }));
        return names;
    }

    /**
     * Collects class field names inherited from super classes.
     * <p>
     * Note: Does not currently account for {@link JavaType.Unknown}.
     * <p>
     * The {@link Scope} of a class lists its inherited fields as bindings of kind {@link Scope.Kind#INHERITED}, with
     * those of its interfaces and its inherited methods.
     */
    public static Set<String> findInheritedNames(J.ClassDeclaration classDeclaration) {
        Set<String> names = new HashSet<>();
        if (classDeclaration.getType() != null) {
            addInheritedClassFields(classDeclaration, classDeclaration.getType().getSupertype(), names);
        }
        return names;
    }

    private static void addInheritedClassFields(J.ClassDeclaration classDeclaration, JavaType.@Nullable FullyQualified superClass, Set<String> names) {
        if (superClass != null) {
            boolean isSamePackage = classDeclaration.getType() != null && classDeclaration.getType().getPackageName().equals(superClass.getPackageName());
            superClass.getMembers().forEach(m -> {
                if ((Flag.hasFlags(m.getFlagsBitMap(), Flag.Public) ||
                        Flag.hasFlags(m.getFlagsBitMap(), Flag.Protected)) ||
                        // Member is accessible as package-private.
                        !Flag.hasFlags(m.getFlagsBitMap(), Flag.Private) && isSamePackage) {
                    names.add(m.getName());
                }
            });
            addInheritedClassFields(classDeclaration, superClass.getSupertype(), names);
        }
    }

    public enum GenerationStrategy {
        INCREMENT_NUMBER
    }
}
