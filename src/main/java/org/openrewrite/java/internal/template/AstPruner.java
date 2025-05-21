package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AstPruner {

    public J.CompilationUnit prune(J.CompilationUnit cu, Cursor insertionPointCursor) {
        PruningVisitor visitor = new PruningVisitor(insertionPointCursor);
        return (J.CompilationUnit) visitor.visit(cu, cu);
    }

    private static class PruningVisitor extends JavaVisitor<J> {
        private final Cursor insertionPointCursor;

        public PruningVisitor(Cursor insertionPointCursor) {
            this.insertionPointCursor = insertionPointCursor;
        }

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, J context) {
            // Ensure we're visiting with the correct cursor context
            setCursor(new Cursor(null, cu));

            J.CompilationUnit c = cu;
            // Keep package declaration and imports by default, they are generally non-prunable.
            // Pruning of imports might be a separate step if some become unused after pruning.
            // c = c.withPackageDeclaration((J.Package) visit(cu.getPackageDeclaration(), context));
            // c = c.withImports(visit(cu.getImports(), context));


            // Retain only relevant class declarations
            List<J> newClasses = new ArrayList<>();
            for (J classDecl : cu.getClasses()) {
                J visitedClass = visit(classDecl, context);
                if (visitedClass != null) {
                    newClasses.add(visitedClass);
                }
            }
            c = c.withClasses(newClasses);

            // If no classes containing the insertion point are left, and the CU itself is not the insertion point,
            // it implies the insertion point might be directly in the CU (e.g. for comments or package decl).
            // Or, all classes were pruned, which shouldn't happen if insertion point is valid and within a class.
            if (newClasses.isEmpty() && !isAncestor(cu) && insertionPointCursor.getValue() != cu) {
                 // If the insertion point was not in any class, this CU might become empty of classes.
                 // This logic might need refinement based on where the insertion point can be.
            }
            return c;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, J context) {
            if (!isAncestor(classDecl) && !containsInsertionPoint(classDecl)) {
                return null; // Prune this class if it's not an ancestor and doesn't contain the insertion point
            }
            // If the class is an ancestor or contains the insertion point, proceed with visiting its body
            J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, context);
            return cd;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, J context) {
            if (isAncestor(method) || containsInsertionPoint(method)) {
                // If the method is an ancestor or contains the insertion point, visit it normally.
                return super.visitMethodDeclaration(method, context);
            } else if (isAncestor(getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class))) {
                // If the method is in a relevant class but is not an ancestor and does not contain the insertion point,
                // keep the method signature but empty its body.
                return method.withBody(emptyBlock()).withLeadingAnnotations(visitAndCast(method.getLeadingAnnotations(), context));
            }
            // Otherwise, prune the method.
            return null;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, J context) {
            if (isAncestor(multiVariable) || containsInsertionPoint(multiVariable)) {
                // If the variable declaration statement is an ancestor or contains the insertion point, visit normally.
                return super.visitVariableDeclarations(multiVariable, context);
            }

            // If the variable is in a block that's an ancestor and appears before or at the insertion point, keep it.
            Cursor parentBlockCursor = getCursor().dropParentUntil(J.Block.class::isInstance);
            if (parentBlockCursor.getValue() instanceof J.Block) {
                J.Block parentBlock = parentBlockCursor.getValue();
                if (isAncestor(parentBlock)) {
                    for (J.Statement stmt : parentBlock.getStatements()) {
                        if (stmt == multiVariable) { // Found the variable declaration
                            return super.visitVariableDeclarations(multiVariable, context); // Keep and visit
                        }
                        if (stmt == insertionPointCursor.getValue() || containsInsertionPoint(stmt)) {
                            // Reached the insertion point or statement containing it, and this variable decl is before it.
                            break; // Stop, variable was not found before insertion point in this specific check.
                                   // This path implies multiVariable is NOT the insertion point or its ancestor.
                        }
                    }
                }
            }
            
            // If it's a field in a relevant class, keep its declaration but prune initializer if not ancestor.
            // (super.visitVariableDeclarations will handle visiting the initializer, which might be pruned by other rules)
            if (getCursor().firstEnclosing(J.ClassDeclaration.class) != null && 
                isAncestor(getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class))) {
                // For fields, we generally want to keep them if the class is kept.
                // The initializer pruning can be handled when visitVariable is called if needed.
                // For now, let's keep the variable declaration and its initializer.
                // Consider more granular pruning for initializers later.
                 J.VariableDeclarations visited = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, context);
                 // Example: Prune initializer if not ancestor (and not part of insertion itself)
                 // List<J.VariableDeclarations.NamedVariable> prunedVariables = new ArrayList<>();
                 // for(J.VariableDeclarations.NamedVariable nv : visited.getVariables()) {
                 //    if (nv.getInitializer() != null && !isAncestor(nv.getInitializer()) && !containsInsertionPoint(nv.getInitializer())) {
                 //        prunedVariables.add(nv.withInitializer(null));
                 //    } else {
                 //        prunedVariables.add(nv);
                 //    }
                 // }
                 // return visited.withVariables(prunedVariables);
                return visited;
            }

            return null; // Prune the variable declaration by default.
        }


        @Override
        public J visitBlock(J.Block block, J context) {
            if (!isAncestor(block) && !containsInsertionPoint(block)) {
                 // If the block is not an ancestor and does not contain the insertion point,
                 // prune it by returning an empty block, unless it's a method body of a relevant method (handled in visitMethodDeclaration).
                 // For a generic block not part of a method body (e.g. static initializer, or simple block statement)
                 // if it's not an ancestor, it should be emptied or removed.
                 // Returning empty list of statements effectively prunes it.
                return block.withStatements(java.util.Collections.emptyList());
            }

            // If the block is an ancestor or contains the insertion point, keep statements up to and including the insertion point.
            List<J.Statement> keptStatements = new ArrayList<>();
            boolean insertionPointEncountered = false;
            for (J.Statement statement : block.getStatements()) {
                J visitedStatement = visit(statement, context); // Visit the statement first

                if (visitedStatement == null) continue; // Statement was pruned by its own visit method

                if (insertionPointEncountered) {
                    // If we are past the insertion point, and this block is an ancestor (not the direct container of IP),
                    // we might want to prune further statements or keep them as stubs (e.g. empty method bodies for method calls).
                    // For now, let's just stop adding more statements if this block directly contains the IP.
                    // If this block is an ancestor but higher up, its children blocks will be further processed.
                    if (statement == insertionPointCursor.getValue() || containsInsertionPoint(statement)) {
                        // This was the direct statement containing the insertion point.
                    } else {
                        // For statements after the insertion point in an ancestral block,
                        // we might want to convert them to stubs, e.g. if they are method calls.
                        // This part needs more advanced logic for creating meaningful stubs.
                        // For now, we are stopping after the IP statement.
                    }
                     // If we already added the IP statement, we break. The current logic adds then checks.
                }


                if (isAncestor(statement) || statement == insertionPointCursor.getValue() || containsInsertionPoint(statement)) {
                    keptStatements.add((J.Statement) visitedStatement);
                    if (statement == insertionPointCursor.getValue() || containsInsertionPoint(statement)) {
                        insertionPointEncountered = true;
                        // If this specific block is the direct parent of the insertion point, stop here.
                        Cursor parentCursor = insertionPointCursor.getParent();
                        if (parentCursor != null && parentCursor.getValue() == block) {
                           break;
                        }
                    }
                } else if (!keptStatements.isEmpty() && insertionPointEncountered) {
                    // If we have passed the insertion point statement within this block,
                    // and subsequent statements are not ancestors (they shouldn't be if IP was correctly identified),
                    // then we stop.
                    break;
                } else if (keptStatements.isEmpty() && insertionPointEncountered) {
                    // This case means the IP itself was the first relevant thing, and it has been added.
                    // Any further statements in this block are after IP and not ancestors.
                    break;
                }
                // If the statement is not an ancestor, not the IP, and we haven't encountered IP yet,
                // it means this statement is before the IP in an ancestral block. So, keep it.
                 else if (isAncestor(block) && !insertionPointEncountered) {
                    keptStatements.add((J.Statement) visitedStatement);
                }
            }
            return block.withStatements(keptStatements);
        }
        
        private boolean isAncestor(J treeElement) {
            if (treeElement == null) return false;
            return insertionPointCursor.getPathAsStream().anyMatch(ancestor -> ancestor == treeElement);
        }

        private boolean containsInsertionPoint(J treeElement) {
            if (treeElement == null) return false;
            // Check if the insertion point value is a descendant of or the same as treeElement
            J ipValue = insertionPointCursor.getValue();
            if (ipValue == treeElement) return true;

            // Walk up from the insertion point's cursor to see if treeElement is an ancestor
            Cursor current = insertionPointCursor;
            while (current != null) {
                if (current.getValue() == treeElement) {
                    return true;
                }
                current = current.getParent();
            }
            return false;
        }
        
        @Override
        public <T extends J> T visitNonNull(T tree, J p) {
            // A catch-all for nodes that are not explicitly handled.
            // If it's an ancestor or contains the insertion point, keep it by calling super.
            // Otherwise, it might be pruned (return null).
            // This is a bit broad. Specific visit methods are safer.
            if (isAncestor(tree) || containsInsertionPoint(tree)) {
                return super.visitNonNull(tree, p);
            }
            // For unhandled types that are not ancestors and don't contain the insertion point,
            // returning null is the general pruning strategy.
            // However, this needs to be done carefully as returning null for certain required elements
            // (like a TypeReference in a VariableDeclaration) can invalidate the AST.
            // It's often better to handle specific types or rely on parent nodes to prune children.
            // For now, let unknown non-ancestors pass through, relying on parent rules to prune.
            return super.visitNonNull(tree, p); 
        }


        // Helper to create an empty block if needed, e.g. for method bodies
        private J.Block emptyBlock() {
            return new J.Block(
                    UUID.randomUUID(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    J.RightPadded.build(false), // J.Block.End.create(Space.EMPTY) before 16.x
                    java.util.Collections.emptyList(),
                    Space.EMPTY
            );
        }
    }
}
