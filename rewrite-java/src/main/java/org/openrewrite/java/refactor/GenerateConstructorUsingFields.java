package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TreeBuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static org.openrewrite.Formatting.*;
import static org.openrewrite.Tree.randomId;

public class GenerateConstructorUsingFields extends ScopedJavaRefactorVisitor {
    private final List<J.VariableDecls> fields;

    public GenerateConstructorUsingFields(J.ClassDecl scope, List<J.VariableDecls> fields) {
        super(scope.getId());
        this.fields = fields;
    }

    @Override
    public String getName() {
        return "core.GenerateConstructorUsingFields";
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        if (isScope() && !hasRequiredArgsConstructor(classDecl)) {
            List<J> statements = classDecl.getBody().getStatements();

            int lastField = 0;
            for (int i = 0; i < statements.size(); i++) {
                if (statements.get(i) instanceof J.VariableDecls) {
                    lastField = i;
                }
            }

            List<Statement> constructorParams = fields.stream()
                    .map(mv -> new J.VariableDecls(randomId(),
                            emptyList(),
                            emptyList(),
                            mv.getTypeExpr() != null ? mv.getTypeExpr().withFormatting(EMPTY) : null,
                            null,
                            formatFirstPrefix(mv.getDimensionsBeforeName(), ""),
                            formatFirstPrefix(mv.getVars(), " "),
                            EMPTY))
                    .collect(toList());

            for (int i = 1; i < constructorParams.size(); i++) {
                constructorParams.set(i, constructorParams.get(i).withFormatting(format(" ")));
            }

            Formatting constructorFormatting = formatter.format(classDecl.getBody());
            J.MethodDecl constructor = new J.MethodDecl(randomId(), emptyList(),
                    singletonList(new J.Modifier.Public(randomId(), EMPTY)),
                    null,
                    null,
                    J.Ident.build(randomId(), classDecl.getSimpleName(), classDecl.getType(), format(" ")),
                    new J.MethodDecl.Parameters(randomId(), constructorParams, EMPTY),
                    null,
                    new J.Block<>(randomId(), null, emptyList(), format(" "),
                            formatter.findIndent(classDecl.getBody().getIndent(), classDecl.getBody().getStatements().toArray(Tree[]::new)).getPrefix()),
                    null,
                    constructorFormatting.withPrefix("\n" + constructorFormatting.getPrefix()));

            // add assignment statements to constructor
            andThen(new AddAssignmentsToConstructor(constructor.getId()));

            statements.add(lastField + 1, constructor);

            return classDecl.withBody(classDecl.getBody().withStatements(statements));
        }

        return super.visitClassDecl(classDecl);
    }

    private boolean hasRequiredArgsConstructor(J.ClassDecl cd) {
        Set<String> injectedFieldNames = fields.stream().map(f -> f.getVars().get(0).getSimpleName()).collect(toSet());

        return cd.getBody().getStatements().stream().anyMatch(stat -> stat.whenType(J.MethodDecl.class)
                .filter(J.MethodDecl::isConstructor)
                .map(md -> md.getParams().getParams().stream()
                        .map(p -> p.whenType(J.VariableDecls.class)
                                .map(mv -> mv.getVars().get(0).getSimpleName())
                                .orElseThrow(() -> new RuntimeException("not possible to get here")))
                        .allMatch(injectedFieldNames::contains))
                .orElse(false));
    }

    private class AddAssignmentsToConstructor extends ScopedJavaRefactorVisitor {
        private AddAssignmentsToConstructor(UUID scope) {
            super(scope);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public J visitMethod(J.MethodDecl method) {
            if (isScope()) {
                return method.withBody(method.getBody().withStatements(
                        TreeBuilder.buildSnippet(enclosingCompilationUnit(),
                                getCursor(),
                                fields.stream().map(mv -> {
                                    String name = mv.getVars().get(0).getSimpleName();
                                    return "this." + name + " = " + name + ";";
                                }).collect(joining("\n", "", "\n"))
                        ))
                );
            }

            return super.visitMethod(method);
        }
    }
}
