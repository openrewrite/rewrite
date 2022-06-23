package org.openrewrite.java.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.*;

@AllArgsConstructor
public class NamesInScope {
    @Getter(AccessLevel.PACKAGE)
    private Set<String> names;

    public static NamesInScope create(Cursor cursor) {
        Queue<J.Block> blocks = new LinkedList<>();
        Iterator<Cursor> pathIterator = cursor.getPathAsCursors(J.Block.class::isInstance);
        Cursor outerBlock = null;
        while (pathIterator.hasNext()) {
            outerBlock = pathIterator.next();
            blocks.add(outerBlock.getValue());
        }
        if (outerBlock == null) {
            throw new IllegalStateException("No outer block was found");
        }
        Set<String> names = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {

            @Override
            public J.Block visitBlock(J.Block block, Set<String> strings) {
                J.Block nextBlock = blocks.poll();
                if (nextBlock == block || nextBlock == null) {
                    return super.visitBlock(block, strings);
                }
                return block;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> strings) {
                strings.add(identifier.getSimpleName());
                return identifier;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> strings) {
                strings.add(variable.getSimpleName());
                return variable;
            }
        }.visit(outerBlock.getValue(), names, outerBlock.getParentOrThrow());
        return new NamesInScope(names);
    }

    private boolean isVariableAccess(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.Identifier)) {
            return false;
        }
        J.Identifier ident = cursor.getValue();
        J parent = cursor.getParent().firstEnclosing(J.class);
        if (parent instanceof J.FieldAccess) {
            J.FieldAccess parentFieldAccess = (J.FieldAccess) parent;
            if (parentFieldAccess.getName() == ident) {
                return false;
            }
        }
        // If the identifier is a new class name, then it is not a variable access
        if (parent instanceof J.NewClass) {
            J.NewClass newParentClass = (J.NewClass) parent;
            if (newParentClass.getClazz() == ident) {
                return false;
            }
        }
        // If the identifier is a method name, then it is not local flow
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation methodInvocation = (J.MethodInvocation) parent;
            if (methodInvocation.getName() == ident) {
                return false;
            }
        }
        if (parent instanceof J.NewArray) {
            J.NewArray newArray = (J.NewArray) parent;
            if (newArray.getTypeExpression() == ident) {
                return false;
            }
        }
        return true;
    }
}
