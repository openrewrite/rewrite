package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

public class ChangeMethodTargetToVariable extends ScopedJavaRefactorVisitor {
    private final String varName;

    @Nullable
    private final JavaType.Class type;

    public ChangeMethodTargetToVariable(J.MethodInvocation scope, J.VariableDecls.NamedVar namedVar) {
        this(scope, namedVar.getSimpleName(), TypeUtils.asClass(namedVar.getType()));
    }

    public ChangeMethodTargetToVariable(J.MethodInvocation scope, String varName, @Nullable JavaType.Class type) {
        super(scope.getId());
        this.varName = varName;
        this.type = type;
    }

    @Override
    public String getName() {
        return "core.ChangeMethodTargetToVariable{to=" + varName + "}";
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        if (isScope()) {
            Expression select = method.getSelect();

            JavaType.Method methodType = null;
            if (method.getType() != null) {
                // if the original is a static method invocation, the import on it's type may no longer be needed
                maybeRemoveImport(method.getType().getDeclaringType());

                Set<Flag> flags = new LinkedHashSet<>(method.getType().getFlags());
                flags.remove(Flag.Static);
                methodType = method.getType().withDeclaringType(this.type).withFlags(flags);
            }

            return method
                    .withSelect(J.Ident.build(randomId(), varName, type, select == null ? Formatting.EMPTY : select.getFormatting()))
                    .withType(methodType);
        }

        return super.visitMethodInvocation(method);
    }
}
