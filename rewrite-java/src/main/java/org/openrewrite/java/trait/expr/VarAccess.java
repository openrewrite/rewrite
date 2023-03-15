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
package org.openrewrite.java.trait.expr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.trait.Top;
import org.openrewrite.java.trait.TraitFactory;
import org.openrewrite.java.trait.internal.MaybeParenthesesPair;
import org.openrewrite.java.trait.member.FieldDeclaration;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.trait.variable.Field;
import org.openrewrite.java.trait.variable.Variable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public interface VarAccess extends Expr {

    String getName();

    /** Gets the variable accessed by this variable access. */
    Variable getVariable();


    boolean hasQualifier();

    Optional<Expr> getQualifier();

    /**
     * True if this access refers to a local variable or a field of
     * the receiver of the enclosing method or constructor.
     */
    boolean isLocal();

    /**
     * Holds if this variable access is an l-value.
     * </p>
     * An l-value is a write access to a variable, which occurs as the destination of an assignment.
     */
    boolean isLValue();

    /**
     * Holds if this variable access is an r-value.
     * </p>
     * An r-value is a read access to a variable.
     * In other words, it is a variable access that does _not_ occur as the destination of
     * a simple assignment, but it may occur as the destination of a compound assignment
     * or a unary assignment.
     */
    boolean isRValue();

    enum Factory implements TraitFactory<VarAccess> {
        F;
        @Override
        public Validation<TraitErrors, VarAccess> viewOf(Cursor cursor) {
            if (cursor.getValue() instanceof J.Identifier) {
                J.Identifier ident = cursor.getValue();
                return VarAccessBase.viewOf(cursor, ident);
            }
            return TraitErrors.invalidTraitCreationType(VarAccess.class, cursor, J.Identifier.class);
        }
    }

    static Validation<TraitErrors, VarAccess> viewOf(Cursor cursor) {
        return Factory.F.viewOf(cursor);
    }

    /**
     * Better to use {@link Variable#getVarAccesses()} as the correct 'scope' can be difficult to determine.
     */
    static Collection<VarAccess> findAllInScope(Cursor scope, Variable variable) {
        List<VarAccess> varAccesses = new ArrayList<>();
        new JavaVisitor<List<VarAccess>>() {
            @Override
            public J visitIdentifier(J.Identifier ident, List<VarAccess> varAccesses) {
                Validation<TraitErrors, VarAccess> varAccess = VarAccess.viewOf(getCursor());
                if (varAccess.map(v -> v.getVariable().equals(variable)).orSuccess(false)) {
                    varAccesses.add(varAccess.success());
                }
                return ident;
            }
        }.visit(scope.getValue(), varAccesses, scope.getParentOrThrow());
        return varAccesses;
    }
}

@AllArgsConstructor
class VarAccessBase implements VarAccess {
    private final Cursor cursor;
    private final J.Identifier identifier;
    @Getter(lazy = true, onMethod =  @__(@Override))
    private final Variable variable = computeVariable(this, cursor, identifier);

    @Override
    public UUID getId() {
        return identifier.getId();
    }

    @Override
    public String getName() {
        return identifier.getSimpleName();
    }

    @Override
    public boolean hasQualifier() {
        return cursor.getParentTreeCursor().getValue() instanceof J.FieldAccess;
    }

    @Override
    public Optional<Expr> getQualifier() {
        return InstanceAccess.viewOf(cursor.getParentTreeCursor()).map(e -> (Expr) e).toOptional();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean isLValue() {
        MaybeParenthesesPair pair = MaybeParenthesesPair.from(cursor);
        if (pair.getParent() instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) pair.getParent();
            return assignment.getVariable() == pair.getTree();
        }
        if (pair.getParent() instanceof J.Unary) {
            J.Unary unary = (J.Unary) pair.getParent();
            return unary.getExpression() == pair.getTree();
        }
        return false;
    }

    @Override
    public boolean isRValue() {
        MaybeParenthesesPair pair = MaybeParenthesesPair.from(cursor);
        if (pair.getParent() instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) pair.getParent();
            return assignment.getVariable() != pair.getTree();
        }
        return true ;
    }

    private static Variable computeVariable(VarAccessBase varAccess, Cursor cursor, J.Identifier varAccessIdent) {
        Cursor compilationUnit = cursor.dropParentUntil(J.CompilationUnit.class::isInstance);
        AtomicBoolean found = new AtomicBoolean(false);
        Variable[] closestVariable = new Variable[1];
        new JavaVisitor<AtomicBoolean>() {
            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, AtomicBoolean atomicBoolean) {
                if (atomicBoolean.get()) {
                    // We've already found the variable, so we don't need to keep looking.
                    return variable;
                }
                if (variable.getName().getSimpleName().equals(varAccessIdent.getSimpleName())) {
                    assert varAccess.identifier.getFieldType() != null;
                    assert varAccess.identifier.getFieldType().getOwner() != null;
                    assert variable.getName().getFieldType() != null;
                    if (varAccess.identifier.getFieldType().getOwner().equals(variable.getName().getFieldType().getOwner())) {
                        closestVariable[0] = Variable.viewOf(getCursor()).orSuccess(TraitErrors::doThrow);
                    }
                }
                return super.visitVariable(variable, atomicBoolean);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, AtomicBoolean atomicBoolean) {
                if (ident == varAccessIdent) {
                    atomicBoolean.set(true);
                }
                return ident;
            }
        }.visit(compilationUnit.getValue(), found, compilationUnit.getParentOrThrow());
        if (closestVariable[0] != null) {
            return closestVariable[0];
        }
        if (varAccessIdent.getFieldType() != null) {
            return FieldFromJavaTypeVariable.create(varAccessIdent.getFieldType(), cursor);
        }
        throw new IllegalStateException("Unable to find variable for " + varAccessIdent.getSimpleName());
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }

    static Validation<TraitErrors, VarAccess> viewOf(Cursor cursor, J.Identifier ident) {
        assert cursor.getValue() == ident;

        if ("this".equals(ident.getSimpleName())) {
            return TraitErrors.invalidTraitCreationError("`this` is not a variable access");
        } else if ("super".equals(ident.getSimpleName())) {
            return TraitErrors.invalidTraitCreationError("`super` is not a variable access");
        }

        Cursor parent = cursor.getParentTreeCursor();
        if (checkType(parent, J.VariableDeclarations.NamedVariable.class, parentNamedVariable -> parentNamedVariable.getName() == ident)) {
            // The identifier getFieldType will not be null in this case.
            return TraitErrors.invalidTraitCreationError("J.Identifier on the name side of a variable declaration is not a variable access");
        }

        if (ident.getFieldType() != null) {
            return Validation.success(new VarAccessBase(cursor, ident));
        }

        // If the identifier is a new class name, those are not variable accesses.
        if (checkType(parent, J.NewClass.class, parentNewClass -> parentNewClass.getClazz() == ident)) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within a new class statement is not a variable access");
        }
        if (checkType(parent, J.MethodInvocation.class, parentMethodInvocation -> parentMethodInvocation.getName() == ident)) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within a method invocation name is not a variable access");
        }
        if (checkType(parent, J.MethodDeclaration.class, parentMethodDeclaration -> parentMethodDeclaration.getName() == ident)) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within a method declaration name is not a variable access");
        }

        // Special case for type casts, where the identifier is the class part of the type cast.
        if (checkType(parent, J.ControlParentheses.class, parentControlParentheses ->
                parentControlParentheses.getTree() == ident &&
                        checkType(parent.getParentTreeCursor(), J.TypeCast.class, parentParentTypeCast -> parentParentTypeCast.getClazz() == parentControlParentheses))) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within a type cast class part is not a variable access");
        }
        // Special case for annotations, where the left side of the assignment is the annotation field name, and not a variable access.
        if (checkType(parent, J.Assignment.class, parentAssignment ->
                parentAssignment.getVariable() == ident &&
                        checkType(parent.getParentTreeCursor(), J.Annotation.class, parentParentAnnotation -> parentParentAnnotation.getArguments() != null && parentParentAnnotation.getArguments().contains(parentAssignment)))) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within an annotation argument's argument label is not a variable access");
        }

        boolean isParentFieldAccess = checkType(parent, J.FieldAccess.class, parentFieldAccess -> parentFieldAccess.getName() == ident);
        boolean isParentMethodAccess = checkType(parent, J.MethodInvocation.class, parentMethodInvocation -> parentMethodInvocation.getSelect() == ident);
        if (ident.getFieldType() != null && isParentFieldAccess) {
            return Validation.success(new VarAccessBase(cursor, ident));
        }

        if (ident.getFieldType() == null && (isParentFieldAccess || isParentMethodAccess)) {
            // This logic may also be reached when type information is missing.
            // With type information missing, we can't determine the difference between `field.format()` and `String.format()` here.
            // So we'll just assume it isn't a variable access.
            return TraitErrors.invalidTraitCreationError("J.Identifier within a field access is not a variable access, or type information is missing.");
        }

        if (checkType(parent, J.MethodInvocation.class, parentMethodInvocation -> parentMethodInvocation.getSelect() == ident || parentMethodInvocation.getArguments().contains(ident)) ||
                checkType(parent, J.NewClass.class, parentNewClass -> parentNewClass.getEnclosing() == ident || parentNewClass.getArguments().contains(ident)) ||
                checkType(parent, J.Parentheses.class, parentParentheses -> parentParentheses.getTree() == ident) ||
                checkType(parent, J.Unary.class, parentUnary -> parentUnary.getExpression() == ident) ||
                checkType(parent, J.Binary.class, parentBinary -> parentBinary.getLeft() == ident || parentBinary.getRight() == ident) ||
                checkType(parent, J.VariableDeclarations.NamedVariable.class, parentNamedVariable -> parentNamedVariable.getInitializer() == ident) ||
                checkType(parent, J.Assignment.class, parentAssignment -> parentAssignment.getVariable() == ident || parentAssignment.getAssignment() == ident) ||
                checkType(parent, J.TypeCast.class, parentTypeCast -> parentTypeCast.getExpression() == ident) ||
                checkType(parent, J.ControlParentheses.class, parentControlParentheses -> parentControlParentheses.getTree() == ident) ||
                checkType(parent, J.ForEachLoop.Control.class, parentForEachLoopControl -> parentForEachLoopControl.getIterable() == ident) ||
                checkType(parent, J.ForLoop.Control.class, parentForLoopControl -> parentForLoopControl.getCondition() == ident) ||
                checkType(parent, J.NewArray.class, parentNewArray -> parentNewArray.getInitializer() != null && parentNewArray.getInitializer().contains(ident)) ||
                checkType(parent, J.ArrayDimension.class, parentArrayDimension -> parentArrayDimension.getIndex() == ident) ||
                checkType(parent, J.ArrayAccess.class, parentArrayAccess -> parentArrayAccess.getIndexed() == ident) ||
                checkType(parent, J.Ternary.class, parentTernary -> parentTernary.getCondition() == ident ||
                        parentTernary.getTruePart() == ident ||
                        parentTernary.getFalsePart() == ident) ||
                checkType(parent, J.Annotation.class, parentAnnotation -> parentAnnotation.getArguments() != null && parentAnnotation.getArguments().contains(ident))) {
            return Validation.success(new VarAccessBase(cursor, ident));
        }

        // Check if the ident appears within an import or package statement. Those are not variable accesses.
        if (cursor.getPathAsStream(o -> o instanceof J.Import || o instanceof J.Package).findAny().isPresent()) {
            assert ident.getFieldType() == null;
            return TraitErrors.invalidTraitCreationError("J.Identifier within an import or package statement is not a variable access");
        }

        // Catch all. Useful point for setting a breakpoint when debugging.
        assert ident.getFieldType() == null : "J.Identifier is not a variable access, but probably should be: " + ident;
        return TraitErrors.invalidTraitCreationError("J.Identifier is not a variable access");
    }

    static <T> boolean checkType(Cursor parent, Class<T> tClass, Predicate<T> predicate) {
        Object tree = parent.getValue();
        if (tClass.isInstance(tree)) {
            return predicate.test(tClass.cast(tree));
        }
        return false;
    }
}


@AllArgsConstructor
class FieldFromJavaTypeVariable implements Field {
    JavaType.Variable variable;
    Cursor compilationUnitCursor;

    @Override
    public String getName() {
        return variable.getName();
    }

    @Override
    public UUID getId() {
        return UUID.nameUUIDFromBytes(variable.toString().getBytes());
    }

    @Override
    public Optional<FieldDeclaration> getDeclaration() {
        return Optional.empty();
    }

    @Override
    public @Nullable JavaType getType() {
        return variable.getType();
    }

    @Override
    public Collection<VarAccess> getVarAccesses() {
        return VarAccess.findAllInScope(compilationUnitCursor, this);
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }

    static Field create(JavaType.Variable variable, Cursor anyCursor) {
        return new FieldFromJavaTypeVariable(variable, anyCursor.dropParentUntil(J.CompilationUnit.class::isInstance));
    }
}

