package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@RequiredArgsConstructor
public class FindFields extends JavaSourceVisitor<List<J.VariableDecls>> {
    private final String fullyQualifiedName;

    @Override
    public List<J.VariableDecls> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<J.VariableDecls> visitMultiVariable(J.VariableDecls multiVariable) {
        if(multiVariable.getTypeExpr() instanceof J.MultiCatch) {
            return emptyList();
        }
        if(multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr().getType(), fullyQualifiedName)) {
            return singletonList(multiVariable);
        }
        return emptyList();
    }
}
