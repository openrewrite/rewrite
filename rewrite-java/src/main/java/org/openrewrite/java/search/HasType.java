package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@RequiredArgsConstructor
public class HasType extends JavaSourceVisitor<Boolean> {
    private final String clazz;

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitIdentifier(J.Ident ident) {
        JavaType.Class asClass = TypeUtils.asClass(ident.getType());
        return asClass != null && asClass.getFullyQualifiedName().equals(clazz);
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation method) {
        if(firstMethodInChain(method).getSelect() == null) {
            // either a same-class instance method or a statically imported method
            return method.getType() != null && method.getType().getDeclaringType().getFullyQualifiedName().equals(clazz);
        }
        return super.visitMethodInvocation(method);
    }

    private J.MethodInvocation firstMethodInChain(J.MethodInvocation method) {
        return method.getSelect() instanceof J.MethodInvocation ?
                firstMethodInChain((J.MethodInvocation) method.getSelect()) :
                method;
    }
}
