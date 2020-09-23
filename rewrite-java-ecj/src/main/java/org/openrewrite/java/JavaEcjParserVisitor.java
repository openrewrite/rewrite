package org.openrewrite.java;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Tree.randomId;

class JavaEcjParserVisitor {
    private final Visitor visitor = new Visitor();

    public J.CompilationUnit visit(CompilationUnitDeclaration declaration) {
        return visitor.visit(declaration);
    }

    private static class Visitor extends ASTVisitor {
        private J tree;

        @Nullable
        private <T extends J> T visit(ASTNode node) {
            tree = null;

            if (node instanceof CompilationUnitDeclaration) {
                visit((CompilationUnitDeclaration) node, ((CompilationUnitDeclaration) node).scope);
            }
            else if(node instanceof TypeDeclaration) {
                visit((TypeDeclaration) node, ((TypeDeclaration) node).scope);
            }

            //noinspection unchecked
            return (T) tree;
        }

        @Nullable
        private <T extends J> List<T> visitAll(@Nullable ASTNode[] nodes) {
            if(nodes == null) {
                return null;
            }

            //noinspection unchecked
            return Arrays.stream(nodes).map(this::visit)
                    .map(j -> (T) j)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration,
                             CompilationUnitScope scope) {
            J.Package mappedPkg = null;

            ImportReference pkg = compilationUnitDeclaration.currentPackage;
            if (pkg != null) {
                mappedPkg = new J.Package(randomId(),
                        identOrFieldAccess(compilationUnitDeclaration.currentPackage.tokens),
                        EMPTY);
            }

            tree = new J.CompilationUnit(
                    randomId(),
                    new String(compilationUnitDeclaration.getFileName()),
                    emptyList(),
                    visit(compilationUnitDeclaration.currentPackage),
                    visitAll(compilationUnitDeclaration.imports),
                    visitAll(compilationUnitDeclaration.types),
                    EMPTY,
                    emptyList()
            );

            return false;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
            return super.visit(typeDeclaration, scope);
        }

        private Expression identOrFieldAccess(char[][] names) {
            if (names.length == 0) {
                return null;
            }

            Expression fa = J.Ident.build(randomId(), new String(names[names.length - 1]), null, EMPTY);
            for (int i = names.length - 2; i >= 0; i--) {
                fa = new J.FieldAccess(randomId(), fa,
                        J.Ident.build(randomId(), new String(names[i]), null, EMPTY),
                        null, EMPTY);
            }
            return fa;
        }
    }
}
