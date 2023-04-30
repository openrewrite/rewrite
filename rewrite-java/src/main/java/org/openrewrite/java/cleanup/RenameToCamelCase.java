package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

public abstract class RenameToCamelCase extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public @Nullable J postVisit(J tree, ExecutionContext ctx) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) tree;
            Map<J.VariableDeclarations.NamedVariable, String> renameVariablesMap = getCursor().getMessage("RENAME_VARIABLES_KEY", emptyMap());
            Set<String> hasNameSet = getCursor().computeMessageIfAbsent("HAS_NAME_KEY", k -> new HashSet<>());
            for (Map.Entry<J.VariableDeclarations.NamedVariable, String> entry : renameVariablesMap.entrySet()) {
                J.VariableDeclarations.NamedVariable variable = entry.getKey();
                String toName = entry.getValue();
                if (shouldRename(hasNameSet, variable, toName)) {
                    cu = (JavaSourceFile) new RenameVariable<>(variable, toName).visitNonNull(cu, ctx);
                    hasNameSet.add(toName);
                }
            }
            return cu;
        }
        return super.postVisit(tree, ctx);
    }

    protected abstract boolean shouldRename(Set<String> hasNameKey, J.VariableDeclarations.NamedVariable variable,
                                            String toName);

    protected void renameVariable(J.VariableDeclarations.NamedVariable variable, String toName) {
        Cursor cu = getCursor().getPathAsCursors(c -> c.getValue() instanceof JavaSourceFile).next();
        cu.computeMessageIfAbsent("RENAME_VARIABLES_KEY", k -> new LinkedHashMap<>())
                .put(variable, toName);
    }

    protected void hasNameKey(String variableName) {
        getCursor().getPathAsCursors(c -> c.getValue() instanceof JavaSourceFile).next()
                .computeMessageIfAbsent("HAS_NAME_KEY", k -> new HashSet<>())
                .add(variableName);
    }
}
