/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceConstantWithAnotherConstant extends Recipe {

    @Option(displayName = "Fully qualified name of the constant to replace",
            example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String existingFullyQualifiedConstantName;

    @Option(displayName = "Fully qualified name of the constant to use in place of existing constant",
            example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String fullyQualifiedConstantName;

    String displayName = "Replace constant with another constant";

    String description = "Replace a constant with another constant, adding/removing import on class if needed.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.')), false),
                new ReplaceConstantWithAnotherConstantVisitor(existingFullyQualifiedConstantName, fullyQualifiedConstantName));
    }

    private static class ReplaceConstantWithAnotherConstantVisitor extends JavaVisitor<ExecutionContext> {

        private final String existingOwningType;
        private final String constantName;
        private final JavaType.FullyQualified existingOwningTypeFqn;
        private final JavaType.FullyQualified newOwningType;
        private final JavaType.FullyQualified newOutermostType;
        private final JavaType.FullyQualified newOwningEnumType;
        private final JavaType.FullyQualified newTargetType;
        private final String newConstantName;

        public ReplaceConstantWithAnotherConstantVisitor(String existingFullyQualifiedConstantName, String fullyQualifiedConstantName) {
            this.existingOwningType = existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.'));
            this.constantName = existingFullyQualifiedConstantName.substring(existingFullyQualifiedConstantName.lastIndexOf('.') + 1);
            this.existingOwningTypeFqn = JavaType.ShallowClass.build(existingOwningType);
            this.newOwningType = ownerType(fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.')));
            this.newOutermostType = ChangeType.getTopLevelClassName(newOwningType);
            this.newOwningEnumType = new JavaType.ShallowClass(null, 1, newOwningType.getFullyQualifiedName(),
                    JavaType.FullyQualified.Kind.Enum, null, null, newOwningType.getOwningClass(), null, null, null, null);
            this.newTargetType = JavaType.ShallowClass.build(fullyQualifiedConstantName);
            this.newConstantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
            if (isConstant(fieldType)) {
                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                return replaceFieldAccess(fieldAccess, fieldType, qualifiedReferenceImport(sf));
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            JavaType.Variable fieldType = ident.getFieldType();
            if (isConstant(fieldType)) {
                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                return replaceFieldAccess(ident, fieldType, hasNoConflictingImports(sf, newOwningType) ? newOwningType : null);
            }
            return super.visitIdentifier(ident, ctx);
        }

        /// The class to import and start a qualified reference from, or null to fully qualify it. The owner stands in when
        /// an import takes the outermost class's name, for instance the import of an old owner of that name.
        private JavaType.@Nullable FullyQualified qualifiedReferenceImport(@Nullable JavaSourceFile sf) {
            if (hasNoConflictingImports(sf, newOutermostType)) {
                return newOutermostType;
            }
            if (hasNoConflictingImports(sf, newOwningType) && !importsOtherClassOfSameSimpleName(sf, newOwningType)) {
                return newOwningType;
            }
            return null;
        }

        private J replaceFieldAccess(Expression expression, JavaType.Variable fieldType, JavaType.@Nullable FullyQualified importedType) {
            JavaType owner = fieldType.getOwner();
            while (owner instanceof JavaType.FullyQualified) {
                maybeRemoveImport(((JavaType.FullyQualified) owner).getFullyQualifiedName());
                owner = ((JavaType.FullyQualified) owner).getOwningClass();
            }

            if (expression instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) expression;
                identifier = identifier
                        .withSimpleName(newConstantName)
                        .withFieldType(newFieldType(fieldType))
                        .withType(newType(identifier.getType()));
                if (importedType != null) {
                    maybeAddImport(newOwningType.getFullyQualifiedName(), newConstantName, false);
                    return identifier;
                }
                return new J.FieldAccess(Tree.randomId(), identifier.getPrefix(), Markers.EMPTY,
                        FullyQualifyTypeReference.qualifiedName(newOwningType, Space.EMPTY, emptyList()),
                        JLeftPadded.build(identifier.withPrefix(Space.EMPTY)),
                        identifier.getType());
            } else if (expression instanceof J.FieldAccess) {
                if (importedType != null) {
                    maybeAddImport(importedType.getFullyQualifiedName(), false);
                }
                J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                Expression target = fieldAccess.getTarget();
                J.Identifier name = fieldAccess.getName();
                target = importedType != null ?
                        typeReference(newOwningType, importedType, target.getPrefix()) :
                        FullyQualifyTypeReference.qualifiedName(newOwningType, target.getPrefix(), emptyList());
                name = name
                        .withFieldType(newFieldType(fieldType))
                        .withSimpleName(newConstantName)
                        .withType(newType(name.getType()));
                return fieldAccess
                        .withType(newType(fieldAccess.getType()))
                        .withTarget(target)
                        .withName(name);
            }
            return expression;
        }

        /// The owner as javac names it, nested classes separated by `$`, whichever separator the option used.
        private static JavaType.FullyQualified ownerType(String fullyQualifiedName) {
            JavaType.FullyQualified type = JavaType.ShallowClass.build(fullyQualifiedName);
            JavaType.FullyQualified owningClass = type.getOwningClass();
            if (owningClass == null) {
                return type;
            }
            JavaType.FullyQualified owner = ownerType(owningClass.getFullyQualifiedName());
            return new JavaType.ShallowClass(null, 1, owner.getFullyQualifiedName() + '$' + simpleName(fullyQualifiedName),
                    JavaType.FullyQualified.Kind.Class, null, null, owner, null, null, null, null);
        }

        private static String simpleName(String fullyQualifiedName) {
            return fullyQualifiedName.substring(Math.max(fullyQualifiedName.lastIndexOf('.'), fullyQualifiedName.lastIndexOf('$')) + 1);
        }

        /// A reference to `type` starting from `importedType`, which is `type` or one of its enclosing classes.
        private static Expression typeReference(JavaType.FullyQualified type, JavaType.FullyQualified importedType, Space prefix) {
            J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                    simpleName(type.getFullyQualifiedName()), type, null);
            JavaType.FullyQualified owningClass = type.getOwningClass();
            if (owningClass == null || TypeUtils.isOfType(type, importedType)) {
                return name.withPrefix(prefix);
            }
            return new J.FieldAccess(Tree.randomId(), prefix, Markers.EMPTY,
                    typeReference(owningClass, importedType, Space.EMPTY), JLeftPadded.build(name), type);
        }

        private JavaType.Variable newFieldType(JavaType.Variable fieldType) {
            return fieldType.withOwner(newOwningType).withName(newConstantName).withType(newType(fieldType.getType()));
        }

        /**
         * An enum constant's type is its enum, so the one replacing it has the new enum's type. Other constants keep
         * theirs, as one of a class's own type may be replaced with one of that type held elsewhere.
         * The new type is of kind enum, which is how a later replacement of the new constant recognizes it as one.
         */
        private @Nullable JavaType newType(@Nullable JavaType type) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            return fq != null && fq.getKind() == JavaType.FullyQualified.Kind.Enum &&
                   TypeUtils.isOfClassType(type, existingOwningType) ? newOwningEnumType : type;
        }

        private boolean isConstant(JavaType.@Nullable Variable varType) {
            return varType != null && TypeUtils.isOfClassType(varType.getOwner(), existingOwningType) &&
                    varType.getName().equals(constantName);
        }

        private boolean hasNoConflictingImports(@Nullable JavaSourceFile sf, JavaType.FullyQualified referencedType) {
            return hasNoConflictingImport(sf, referencedType) && hasNoConflictingImport(sf, newTargetType);
        }

        /// [#hasNoConflictingImport] compares class names, which for a nested class include its enclosing classes. So it
        /// misses that `Outer.Inner` clashes with an import of another `Inner`.
        private static boolean importsOtherClassOfSameSimpleName(@Nullable JavaSourceFile sf, JavaType.FullyQualified type) {
            if (sf == null) {
                return false;
            }
            String simpleName = simpleName(type.getFullyQualifiedName());
            for (J.Import anImport : sf.getImports()) {
                if (!anImport.isStatic() && anImport.getQualid().getSimpleName().equals(simpleName) &&
                        !TypeUtils.isOfType(anImport.getQualid().getType(), type)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasNoConflictingImport(@Nullable JavaSourceFile sf, JavaType.FullyQualified targetType) {
            if (sf == null || targetType == null) {
                return true;
            }

            for (J.Import anImport : sf.getImports()) {
                if (anImport.isStatic()) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                    if (anImport.getQualid().getSimpleName().equals(newConstantName) && !TypeUtils.isOfClassType(fqn, newOwningType.getFullyQualifiedName())) {
                        if (!anImport.getQualid().getSimpleName().equals(constantName) || !TypeUtils.isOfClassType(fqn, existingOwningTypeFqn.getFullyQualifiedName())) {
                            return false;
                        }
                    }
                } else {
                    JavaType.FullyQualified currType = TypeUtils.asFullyQualified(anImport.getQualid().getType());
                    if (currType != null &&
                            !TypeUtils.isOfType(currType, targetType) &&
                            currType.getClassName().equals(targetType.getClassName())) {
                        return false;
                    }
                }

            }
            return true;
        }
    }
}
