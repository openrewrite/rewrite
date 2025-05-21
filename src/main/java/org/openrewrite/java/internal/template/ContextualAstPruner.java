package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContextualAstPruner {

    public J.CompilationUnit prune(J.CompilationUnit cu, Cursor insertionPointCursor) {
        PruningVisitor visitor = new PruningVisitor(insertionPointCursor);
        return (J.CompilationUnit) visitor.visit(cu, cu);
    }

import java.util.Stack;

    private static class PruningVisitor extends JavaVisitor<J> {
        private final Cursor insertionPointCursor;
        private final Stack<Boolean> containsTemplateStack = new Stack<>(); // true if node or its children contain/are IP/ancestor of IP

        public PruningVisitor(Cursor insertionPointCursor) {
            this.insertionPointCursor = insertionPointCursor;
        }
        
        private boolean isIpOrAncestor(J treeElement) {
            if (treeElement == null) return false;
            if (treeElement == insertionPointCursor.getValue()) return true;
            return isAncestor(treeElement);
        }

        private boolean isAncestor(J treeElement) {
            if (treeElement == null) return false;
            return insertionPointCursor.getPathAsStream().anyMatch(ancestor -> ancestor == treeElement);
        }

        // This method is now less used, replaced by stack logic.
        // Keeping it for now for potential direct checks or if isIpOrAncestor is too broad.
        private boolean oldContainsInsertionPoint(J treeElement) {
            if (treeElement == null) return false;
            J ipValue = insertionPointCursor.getValue();
            if (ipValue == treeElement) return true;
            Cursor current = insertionPointCursor;
            while (current != null) {
                if (current.getValue() == treeElement) return true;
                current = current.getParent();
            }
            return false;
        }


        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, J context) {
            setCursor(new Cursor(null, cu)); // Set initial cursor for potential getCursor() calls by children
            containsTemplateStack.push(isIpOrAncestor(cu)); // CU's own relevance

            // Package and imports are always kept for now.
            // Visiting them won't change their relevance status in the stack unless IP is inside them.
            J.CompilationUnit c = cu.withPackageDeclaration(cu.getPackageDeclaration()); // No visit, keep as is
            c = c.withImports(cu.getImports()); // No visit, keep as is

            List<J> newClasses = new ArrayList<>();
            for (J classDecl : c.getClasses()) {
                J visitedClass = visit(classDecl, context); // This will use the stack mechanism
                if (visitedClass != null) {
                    newClasses.add(visitedClass);
                }
            }
            c = c.withClasses(newClasses);

            boolean subtreeRelevant = containsTemplateStack.pop(); // Pop CU's relevance
            // CU itself is never pruned, but this maintains stack discipline.
            // No parent to propagate to.
            return c;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, J context) {
            containsTemplateStack.push(isIpOrAncestor(classDecl));
            J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, context);
            boolean subtreeRelevant = containsTemplateStack.pop();

            if (!subtreeRelevant) {
                // If this class and none of its content (body, type params etc.) are relevant, prune it.
                return null;
            }

            if (subtreeRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return cd;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, J context) {
            containsTemplateStack.push(isIpOrAncestor(method));
            J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, context);
            boolean subtreeRelevant = containsTemplateStack.pop();

            if (!subtreeRelevant) {
                // If method itself, its params, or body are not relevant:
                // Check if its enclosing class is relevant (i.e. an ancestor of IP).
                // This requires looking up the parent's relevance from the stack, which is tricky.
                // Simpler: If the class *itself* is an ancestor, then we must keep this method signature.
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                if (isIpOrAncestor(enclosingClass)) { // If enclosing class is an ancestor or IP
                    return m.withBody(emptyBlock())
                            .withLeadingAnnotations(visitList(method.getLeadingAnnotations(), context)); // Visit annotations too
                } else {
                    // Class is not an ancestor (it might be kept if a sibling method is relevant).
                    // If this method is not relevant, prune it.
                    return null;
                }
            }
            
            // If relevant, ensure body is not emptied unless it truly contained nothing relevant.
            // The super.visitMethodDeclaration -> visitBlock for body would have handled body's content.
            // If body became empty and method is relevant (e.g. IP in signature), it's fine.

            if (subtreeRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return m;
        }
        
        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, J context) {
            containsTemplateStack.push(isIpOrAncestor(multiVariable));
            // Special handling for initializers: visit them under their own stack frame if complex.
            // For now, super.visitVariableDeclarations will handle it.
            J.VariableDeclarations mv = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, context);
            boolean subtreeRelevant = containsTemplateStack.pop();

            if (!subtreeRelevant) {
                // If not relevant, can it be pruned?
                // If it's a field in a relevant class, maybe keep decl, prune init?
                // If it's a local var, and nothing after it in the block is relevant, prune.
                // Current logic: if this var decl statement itself isn't IP/ancestor,
                // and its initializer isn't IP/ancestor, then it's not relevant.
                
                // Check if it's a field in a class that IS an ancestor/IP.
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && isIpOrAncestor(enclosingClass) && mv.getVariables().get(0).getKind() == J.VariableDeclarations.NamedVariable.Kind.FIELD) {
                    // It's a field in a relevant class. Keep the declaration, but prune initializer if it wasn't relevant.
                    // The initializer's visit would have pushed/popped. If initializer was relevant, subtreeRelevant would be true.
                    // So, if subtreeRelevant is false here, initializer was also not relevant.
                    List<J.VariableDeclarations.NamedVariable> prunedVars = new ArrayList<>();
                    for (J.VariableDeclarations.NamedVariable nv : mv.getVariables()) {
                        prunedVars.add(nv.withInitializer(null)); // Prune initializer
                    }
                    return mv.withVariables(prunedVars);
                }
                // Otherwise, if it's a local variable or a field in a non-relevant class context, prune it.
                return null;
            }

            if (subtreeRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return mv;
        }

        @Override
        public J visitBlock(J.Block block, J context) {
            containsTemplateStack.push(isIpOrAncestor(block));
            
            List<J.Statement> keptStatements = new ArrayList<>();
            boolean foundIpOrAncestorStatementInBlock = false;

            for (J.Statement statement : block.getStatements()) {
                // Before visiting statement, current stack top is for `block` (or statement if isIpOrAncestor(statement))
                // We need to know if the statement ITSELF is relevant or leads to IP
                
                // This is where the logic for "declarations needed by template" will be hard.
                // A statement is kept if:
                // 1. It is an IP or an ancestor of IP.
                // 2. It contains the IP (its visit will make containsTemplateStack true for it).
                // 3. It's a declaration that precedes a statement that meets (1) or (2) in the same block.

                // Simulate statement visit for relevance check without full visit if it's simple.
                // This is tricky. Let's use the standard visit pattern.
                
                J visitedStatement = visit(statement, context); // This will push, visit children, pop for statement.
                                                                // And it will OR its relevance into the block's stack entry.
                
                // After visit(statement), the statement's own subtreeRelevance has been determined and propagated to block's entry.
                // We need that specific relevance for the statement itself.
                // This requires `visit` to return not just J, but also relevance, or store it.
                // This is the main difficulty of the stack propagation.

                // Let's simplify: the `PruningVisitor`'s `visit` method (the overridden one) should handle the stack.
                // `visitBlock` will rely on `visitedStatement` being non-null if it's relevant.
                // This means `visit(statement, context)` must return `null` if `statement` subtree is not relevant.

                // Revisit this: The main `visit` override idea was complex.
                // Standard approach: each visitXYZ pushes, super.visitXYZ, pops, decides, propagates.

                // Let's assume after `visit(statement, context)`, `statement`'s `postVisit` has run.
                // The problem is `visitBlock` iterates and calls `visit(statement)`.
                // The `containsTemplateStack` will be modified by `visit(statement)`.
                // We need to check the relevance of `visitedStatement` specifically.

                // Simpler model for visitBlock:
                // 1. isIpOrAncestor(block) is pushed for the block.
                // 2. Children statements are visited. Each statement's visit will push its own relevance,
                //    process its children, pop its own subtree relevance, and then OR into the block's stack value.
                // 3. After all statements, block.pop() gives overall relevance.
                // This doesn't help filter individual statements *within* the block easily using the stack alone.

                // Fallback to previous logic for statement filtering within a relevant block,
                // but use isIpOrAncestor instead of old containsInsertionPoint.
                // This part does NOT yet use the stack for individual statements within the block.

                if (isIpOrAncestor(statement)) { // Direct relevance
                    if (visitedStatement != null) keptStatements.add((J.Statement) visitedStatement);
                    foundIpOrAncestorStatementInBlock = true;
                    // If this block is the direct parent of the IP statement, stop.
                    Cursor parentCursor = insertionPointCursor.getParent();
                    if (parentCursor != null && parentCursor.getValue() == block && statement == insertionPointCursor.getValue()) {
                        break;
                    }
                } else if (visitedStatement != null) { // Statement itself is not IP/ancestor, but its children might be (making visitedStatement non-null)
                    // How to know if visitedStatement was kept because its children were relevant?
                    // This implies that `visit(statement, context)` needs to return null if it and its children are not relevant.
                    // This needs to be consistently applied in all visitXYZ methods.
                    // Assume for now: if visitedStatement is not null, it's relevant.
                    keptStatements.add((J.Statement) visitedStatement);
                    // If we've already found the main IP/ancestor statement, and this statement is *after* it,
                    // but it's kept (e.g. a declaration needed later), this is complex.
                    // The current logic is to stop after the IP statement. This needs to change for "keep dependencies".
                    // For now, let's stick to "keep statements before IP, and IP itself".
                    // And any statement whose children make it relevant (visitedStatement != null).
                    // This part is the core of the "dependency-aware pruning" and is hard with current structure.
                    // The simple "stop at IP" is from previous logic.
                    if (foundIpOrAncestorStatementInBlock) {
                        // If we already added the IP statement, and this one is not IP/ancestor, but non-null (kept for other reasons like children)
                        // This means it's after the IP. Should it be here?
                        // The original logic would break here. For now, let's also break.
                        // This means we are not yet keeping declarations after IP that might be needed.
                        break;
                    }
                } else { // visitedStatement is null
                    // Statement was pruned. If we already found IP, that's fine.
                    // If we haven't found IP, and this statement before IP was pruned, also fine.
                }
            }
            J.Block b = block.withStatements(keptStatements);
            boolean subtreeRelevant = containsTemplateStack.pop(); // Pops the block's own relevance

            if (!subtreeRelevant && !keptStatements.isEmpty()) {
                 // This can happen if block itself isn't IP/ancestor, but contains statements that are.
                 // In this case, subtreeRelevant should have been true.
                 // This implies the logic for statement relevance affecting block relevance needs refinement.
                 // The `visit(statement)` calls should have OR'd into the block's stack entry.
                 // So, if keptStatements is not empty due to relevant children, subtreeRelevant should be true.
            }
            
            if (!subtreeRelevant && keptStatements.isEmpty()) { // Block not IP/ancestor, and no relevant children statements
                return b.withStatements(java.util.Collections.emptyList()); // Empty it completely
            }

            if (subtreeRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return b;
        }
        
        // visitNonNull and other specific visit methods (J.If, J.Try etc.) will follow this pattern:
        // 1. Push self_is_ip_or_ancestor.
        // 2. super.visitXYZ() or manual child visits.
        // 3. Pop subtree_relevance.
        // 4. If !subtree_relevance, return null or simplified_empty_version.
        // 5. If subtree_relevance, propagate to parent stack.
        // 6. Return (potentially simplified based on children's relevance) node.

        @Override
        public <T extends J> J visitNonNull(T tree, J p) {
            boolean isTreeIpOrAncestor = isIpOrAncestor(tree);
            containsTemplateStack.push(isTreeIpOrAncestor);
            
            J result = super.visitNonNull(tree, p); // Default behavior: visits children
            
            boolean subtreeRelevant = containsTemplateStack.pop();

            if (!subtreeRelevant && result != null) {
                // Generic pruning for unhandled types: if not relevant, try to remove.
                // This is too broad. Most J types don't have a generic "empty" form.
                // Returning null here can break AST structure if parent expects a node.
                // Specific visit methods are better.
                // For now, if not handled by specific visit, and not relevant, it passes through
                // and relies on its parent being pruned if the parent is also not relevant.
            }

            if (subtreeRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return result;
        }
        
        // Helper to create an empty block if needed
        private J.Block emptyBlock() {
            return new J.Block(
                    UUID.randomUUID(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    J.RightPadded.build(false), 
                    java.util.Collections.emptyList(),
                    Space.EMPTY
            );
        }

        // General visit override to manage the stack for all J nodes
        @Override
        public J visit(@Nullable org.openrewrite.Tree tree, J context) {
            if (tree == null) {
                return null;
            }
            if (!(tree instanceof J)) { // Only operate on J types for pruning logic
                return (J) super.visit(tree, context); // Pass through non-J elements if any
            }

            J jTree = (J) tree;
            boolean isNodeIpOrAncestor = isIpOrAncestor(jTree);
            containsTemplateStack.push(isNodeIpOrAncestor);

            J result = super.visit(jTree, context); // Calls specific visitXYZ or visitNonNull

            boolean subtreeWasRelevant = containsTemplateStack.pop();

            if (!subtreeWasRelevant && result != null) {
                // If this node (and its children) were not relevant, attempt to prune.
                // Specific visitXYZ methods should ideally return null or an empty form if they determine
                // irrelevance based on their children. This is a fallback.
                if (result instanceof J.ClassDeclaration || 
                    result instanceof J.MethodDeclaration ||
                    result instanceof J.VariableDeclarations ||
                    (result instanceof J.Statement && !(result instanceof J.Block))) { // Most statements can be nulled
                    return null;
                } else if (result instanceof J.Block) { // Blocks are emptied
                    return ((J.Block) result).withStatements(java.util.Collections.emptyList());
                }
                // For other types (expressions, type references, etc.), returning null might be invalid.
                // They rely on their parent statement/declaration being pruned.
                // If such an element `result` is not null here, it means its specific visit method kept it.
            }
            
            if (subtreeWasRelevant && !containsTemplateStack.isEmpty()) {
                containsTemplateStack.push(containsTemplateStack.pop() || true);
            }
            return result;
        }


        @Override
        public J visitBlock(J.Block block, J context) {
            J.Block mBlock = (J.Block) super.visitBlock(block, context);
            if (mBlock == null) return null;

            List<J.Statement> newStatements = new ArrayList<>();
            boolean ipEncounteredInThisBlock = false;
            boolean isBlockDirectIpParent = false;
            Cursor parentCursor = insertionPointCursor.getParent();
            if (parentCursor != null && parentCursor.getValue() == block) {
                isBlockDirectIpParent = true;
            }

            for (J.Statement stmt : mBlock.getStatements()) {
                if (stmt == null) continue; // Already pruned by its own visit

                boolean stmtIsIp = (stmt == insertionPointCursor.getValue());
                boolean stmtIsAncestor = isAncestor(stmt);

                if (stmtIsIp || stmtIsAncestor) {
                    newStatements.add(stmt);
                    ipEncounteredInThisBlock = true;
                    if (isBlockDirectIpParent && stmtIsIp) {
                        break; // Stop after IP if block is direct parent
                    }
                } else { // Statement is not IP or an ancestor
                    if (ipEncounteredInThisBlock && isBlockDirectIpParent) {
                        // If IP encountered AND this block is direct parent, stop.
                        break;
                    }
                    // If IP not yet encountered, or if block is not direct parent (meaning IP is deeper),
                    // we keep this statement as it's part of the (grand)parent context before the deeper IP.
                    // Or, if IP was encountered, but this block is an ancestor (not direct parent),
                    // we are now processing statements *after* the child containing IP. These should be pruned.
                    if(ipEncounteredInThisBlock && isAncestor(block) && !isBlockDirectIpParent) {
                        // This statement is in an ancestral block, after the element that contained the IP. Prune.
                        continue;
                    }
                    newStatements.add(stmt);
                }
            }
            // If no relevant statements were kept and the block itself is not the IP or an ancestor,
            // the generic visit() will handle pruning the block.
            return mBlock.withStatements(newStatements);
        }

        @Override
        public J visitIf(J.If iff, J context) {
            J.If mIf = (J.If) super.visitIf(iff, context);
            if (mIf == null) return null;

            J.Expression condition = mIf.getControlParentheses().getTree();
            J.Statement thenPart = mIf.getThenPart();
            J.Statement elsePart = (mIf.getElsePart() != null) ? mIf.getElsePart().getBody() : null;

            boolean conditionIsRelevant = (condition != null && isIpOrAncestor(condition));
            boolean thenIsRelevant = (thenPart != null && isIpOrAncestor(thenPart));
            // For thenIsRelevant/elseIsRelevant, we also need to consider if their *children* were relevant.
            // This is hard if they are already pruned to empty blocks.
            // A simple check: if the visited part is not null and not an empty block, it might contain relevance.
            if (thenPart instanceof J.Block && !((J.Block)thenPart).getStatements().isEmpty() && !thenIsRelevant) thenIsRelevant = true; // Kept for its content
            if (elsePart instanceof J.Block && !((J.Block)elsePart).getStatements().isEmpty() && !elseIsRelevant) elseIsRelevant = true; // Kept for its content


            if (isIpOrAncestor(mIf)) { // If the 'if' statement itself is an ancestor path or IP.
                J.Expression newCondition = conditionIsRelevant ? condition : simplifyExpression(condition);
                J.Statement newThen = thenIsRelevant ? thenPart : emptyBlockSt(thenPart);
                J.If.Else newElse = null;
                if (mIf.getElsePart() != null) {
                    newElse = mIf.getElsePart().withBody(elseIsRelevant ? elsePart : emptyBlockSt(elsePart));
                }
                return mIf.withControlParentheses(mIf.getControlParentheses().withTree(newCondition))
                          .withThenPart(newThen)
                          .withElsePart(newElse);
            } else { // 'if' statement is not an ancestor (but some child made it relevant, so mIf is not null).
                if (conditionIsRelevant) {
                    return mIf.withControlParentheses(mIf.getControlParentheses().withTree(condition))
                              .withThenPart(emptyBlockSt(thenPart))
                              .withElsePart(null);
                } else if (thenIsRelevant) {
                    return mIf.withControlParentheses(simplifyExpression(mIf.getControlParentheses()))
                              .withThenPart(thenPart)
                              .withElsePart(null);
                } else if (elseIsRelevant) {
                     return mIf.withControlParentheses(simplifyExpression(mIf.getControlParentheses()))
                              .withThenPart(emptyBlockSt(thenPart))
                              .withElsePart(mIf.getElsePart().withBody(elsePart)); // elsePart must be non-null
                } else {
                     // mIf is not IP/ancestor, but was kept by generic visit (subtreeRelevant=true).
                     // This means some deeper part, not directly condition/then/else, was relevant.
                     // This is rare. For safety, return as is, assuming children handled their pruning.
                    return mIf;
                }
            }
        }
        
        private J.Expression simplifyExpression(J.Expression expression) {
            if (expression == null) return defaultTrueCondition(); // Default to true if no expr
            // For now, always return true literal when simplifying.
            return new J.Literal(UUID.randomUUID(), expression.getPrefix(), Markers.EMPTY, true, "true", null, org.openrewrite.java.tree.JavaType.Primitive.Boolean);
        }
        private J.ControlParentheses<J.Expression> simplifyExpression(J.ControlParentheses<J.Expression> controlParentheses) {
            if (controlParentheses == null) { // Should not happen in a valid If
                J.Expression trueLit = defaultTrueCondition();
                return new J.ControlParentheses<>(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, J.RightPadded.build(trueLit));
            }
            return controlParentheses.withTree(simplifyExpression(controlParentheses.getTree()));
        }
        
        private J.Statement emptyBlockSt(J.Statement originalStatement) {
            if (originalStatement == null) return emptyBlock();
            // If it was a block that became empty through its children pruning, it's already what we want.
            if (originalStatement instanceof J.Block && ((J.Block)originalStatement).getStatements().isEmpty()) {
                return originalStatement;
            }
            return emptyBlock(); 
        }

        @Override
        public J visitForLoop(J.ForLoop forLoop, J context) {
            J.ForLoop mFor = (J.ForLoop) super.visitForLoop(forLoop, context);
            if (mFor == null) return null;

            J.ForLoop.Control ctrl = mFor.getControl();
            List<J.Statement> init = ctrl.getInit(); 
            J.Expression condition = ctrl.getCondition();
            List<J.Statement> update = ctrl.getUpdate();
            J.Statement body = mFor.getBody();

            boolean initIsRelevant = init.stream().anyMatch(s -> s != null && isIpOrAncestor(s)); // Check visited children
            boolean conditionIsRelevant = (condition != null && isIpOrAncestor(condition));
            boolean updateIsRelevant = update.stream().anyMatch(s -> s != null && isIpOrAncestor(s));
            boolean bodyIsRelevant = (body != null && isIpOrAncestor(body));
            // Also consider if a child part was kept because *its* children were relevant
            if (!initIsRelevant && init.stream().anyMatch(s -> s != null && !(s instanceof J.Empty))) initIsRelevant = true;
            if (!conditionIsRelevant && condition != null && !(condition instanceof J.Literal)) conditionIsRelevant = true; // Non-trivial conditions kept
            if (!updateIsRelevant && update.stream().anyMatch(s -> s != null && !(s instanceof J.Empty))) updateIsRelevant = true;
            if (!bodyIsRelevant && body instanceof J.Block && !((J.Block)body).getStatements().isEmpty()) bodyIsRelevant = true;


            if (isIpOrAncestor(mFor)) {
                return mFor.withControl(
                            ctrl.withInit(initIsRelevant ? init : java.util.Collections.emptyList())
                                .withCondition(conditionIsRelevant ? condition : defaultTrueCondition())
                                .withUpdate(updateIsRelevant ? update : java.util.Collections.emptyList())
                        )
                           .withBody(bodyIsRelevant ? body : emptyBlockSt(body));
            } else { 
                if (initIsRelevant || conditionIsRelevant || updateIsRelevant || bodyIsRelevant) {
                    J.Expression newCondition;
                    if (conditionIsRelevant) newCondition = condition;
                    else if (bodyIsRelevant || initIsRelevant || updateIsRelevant) newCondition = defaultTrueCondition();
                    else newCondition = defaultFalseCondition();

                    return mFor.withControl(
                                ctrl.withInit(initIsRelevant ? init : java.util.Collections.emptyList())
                                    .withCondition(newCondition)
                                    .withUpdate(updateIsRelevant ? update : java.util.Collections.emptyList())
                            )
                           .withBody(bodyIsRelevant ? body : emptyBlockSt(body));
                } else {
                     if (mFor == insertionPointCursor.getValue()) {
                         return mFor.withControl(
                                ctrl.withInit(java.util.Collections.emptyList())
                                    .withCondition(defaultTrueCondition()) 
                                    .withUpdate(java.util.Collections.emptyList())
                            )
                           .withBody(emptyBlockSt(body));
                     }
                    return null; 
                }
            }
        }
        
        private J.Expression defaultTrueCondition() {
            return new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, true, "true", null, org.openrewrite.java.tree.JavaType.Primitive.Boolean);
        }
        private J.Expression defaultFalseCondition() {
            return new J.Literal(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, false, "false", null, org.openrewrite.java.tree.JavaType.Primitive.Boolean);
        }
        
        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, J context) {
            J.ForEachLoop mForEach = (J.ForEachLoop) super.visitForEachLoop(forEachLoop, context);
            if (mForEach == null) return null;

            J.ForEachLoop.Control ctrl = mForEach.getControl();
            J.VariableDeclarations variable = ctrl.getVariable(); // Control variable
            J.Expression iterable = ctrl.getIterable();
            J.Statement body = mForEach.getBody();

            boolean varIsRelevant = (variable != null && isIpOrAncestor(variable));
            boolean iterIsRelevant = (iterable != null && isIpOrAncestor(iterable));
            boolean bodyIsRelevant = (body != null && isIpOrAncestor(body));
             if (!varIsRelevant && variable != null && !(variable.getVariables().isEmpty())) varIsRelevant = true; // Kept for content
             if (!iterIsRelevant && iterable != null && !(iterable instanceof J.Literal)) iterIsRelevant = true; // Kept for content
             if (!bodyIsRelevant && body instanceof J.Block && !((J.Block)body).getStatements().isEmpty()) bodyIsRelevant = true;


            if (isIpOrAncestor(mForEach)) {
                return mForEach.withControl(
                                ctrl.withVariable(varIsRelevant ? variable : simplifyVariable(variable))
                                    .withIterable(iterIsRelevant ? iterable : simplifyExpression(iterable))
                                )
                               .withBody(bodyIsRelevant ? body : emptyBlockSt(body));
            } else {
                if (varIsRelevant || iterIsRelevant || bodyIsRelevant) {
                    return mForEach.withControl(
                                ctrl.withVariable(varIsRelevant ? variable : simplifyVariable(variable))
                                    .withIterable(iterIsRelevant ? iterable : simplifyExpression(iterable))
                                )
                               .withBody(bodyIsRelevant ? body : emptyBlockSt(body));
                } else {
                    if (mForEach == insertionPointCursor.getValue()) { // IP is the loop itself
                         return mForEach.withControl(
                                ctrl.withVariable(simplifyVariable(variable))
                                    .withIterable(simplifyExpression(iterable))
                            )
                           .withBody(emptyBlockSt(body));
                    }
                    return null;
                }
            }
        }
        
        private J.VariableDeclarations simplifyVariable(J.VariableDeclarations var) {
            if (var == null) return null; // Should not happen in valid ForEach
            // Create a dummy variable like "Object templateVar"
             J.Identifier typeIdent = new J.Identifier(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, "Object", org.openrewrite.java.tree.JavaType.buildType("java.lang.Object"), null);
             J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                     UUID.randomUUID(), Space.SINGLE_SPACE, Markers.EMPTY, J.Identifier.build(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, "templateVar", null), null, null, null);
            return var.withTypeExpression(typeIdent).withVariables(java.util.Collections.singletonList(namedVar));
        }


        @Override
        public J visitSwitch(J.Switch switchee, J context) {
            J.Switch mSwitch = (J.Switch) super.visitSwitch(switchee, context);
            if (mSwitch == null) return null;

            J.ControlParentheses<J.Expression> selector = mSwitch.getSelector();
            J.Block casesBlock = mSwitch.getCases(); // This block contains J.Case statements

            boolean selectorIsRelevant = (selector != null && isIpOrAncestor(selector.getTree()));
            // Cases block relevance: true if itself is ancestor, or any case within it is relevant (checked by its statements being non-empty after visit)
            boolean casesAreRelevant = (casesBlock != null && isIpOrAncestor(casesBlock)) || 
                                       (casesBlock != null && !casesBlock.getStatements().isEmpty());


            if (isIpOrAncestor(mSwitch)) {
                return mSwitch.withSelector(selectorIsRelevant ? selector : simplifyExpression(selector))
                              .withCases(casesAreRelevant ? casesBlock : emptyBlock()); // Keep cases block structure, content handled by J.Case visits
            } else {
                if (selectorIsRelevant || casesAreRelevant) {
                     // If selector is relevant, keep it, empty cases.
                     // If cases are relevant, simplify selector, keep cases.
                     // If both, keep both.
                    return mSwitch.withSelector(selectorIsRelevant ? selector : simplifyExpression(selector))
                                  .withCases(casesAreRelevant ? casesBlock : emptyBlock());
                } else {
                    if (mSwitch == insertionPointCursor.getValue()) { // IP is the switch itself
                        return mSwitch.withSelector(simplifyExpression(selector))
                                      .withCases(emptyBlock());
                    }
                    return null;
                }
            }
        }

        @Override
        public J visitCase(J.Case casee, J context) {
            J.Case mCase = (J.Case) super.visitCase(casee, context);
            if (mCase == null) return null;

            // A J.Case has patterns (expressions) and a body of statements.
            // If the J.Case itself is an ancestor, or one of its patterns, or its body is relevant, keep.
            boolean patternsRelevant = mCase.getExpressions().stream().anyMatch(p -> p != null && isIpOrAncestor(p));
            // Body (J.Block) relevance is determined by its statements after visit.
            boolean bodyRelevant = !mCase.getStatements().isEmpty(); // If statements were kept, body is relevant.


            if (isIpOrAncestor(mCase) || patternsRelevant || bodyRelevant) {
                // If J.Case is an ancestor, keep patterns and body (they would be simplified/emptied by their own visits if not relevant).
                // If not an ancestor, but patterns or body relevant, also keep.
                J.Case resultCase = mCase;
                if (isIpOrAncestor(mCase)) { // Case itself is ancestor
                    if (!patternsRelevant) {
                        // Create dummy "default:" or simplified pattern if possible, or keep original patterns if complex simplification is hard.
                        // For now, keep patterns if case is ancestor. Simplification of case patterns is tricky.
                    }
                    if (!bodyRelevant) {
                        resultCase = resultCase.withStatements(java.util.Collections.emptyList());
                    }
                } else { // Case not ancestor, but pattern or body is.
                    if (!patternsRelevant) { 
                        // This case is tricky: body is relevant, but patterns are not. This might be an invalid switch case.
                        // Default to keeping patterns to maintain structure if body is relevant.
                    }
                    if (!bodyRelevant) { // patterns are relevant, body is not.
                         resultCase = resultCase.withStatements(java.util.Collections.emptyList());
                    }
                }
                return resultCase;
            } else {
                 if (mCase == insertionPointCursor.getValue()) { // Case itself is IP
                    return mCase.withStatements(java.util.Collections.emptyList()); // Empty body
                 }
                return null; // Prune case
            }
        }

        @Override
        public J visitSynchronized(J.Synchronized synch, J context) {
            J.Synchronized mSynch = (J.Synchronized) super.visitSynchronized(synch, context);
            if (mSynch == null) return null;

            J.ControlParentheses<J.Expression> lock = mSynch.getLock();
            J.Block body = mSynch.getBody();

            boolean lockIsRelevant = (lock != null && isIpOrAncestor(lock.getTree()));
            boolean bodyIsRelevant = (body != null && isIpOrAncestor(body)) || (body != null && !body.getStatements().isEmpty());

            if (isIpOrAncestor(mSynch)) {
                return mSynch.withLock(lockIsRelevant ? lock : simplifyExpression(lock))
                             .withBody(bodyIsRelevant ? body : emptyBlock());
            } else {
                if (lockIsRelevant || bodyIsRelevant) {
                    return mSynch.withLock(lockIsRelevant ? lock : simplifyExpression(lock))
                                 .withBody(bodyIsRelevant ? body : emptyBlock());
                } else {
                    if (mSynch == insertionPointCursor.getValue()) {
                        return mSynch.withLock(simplifyExpression(lock)).withBody(emptyBlock());
                    }
                    return null;
                }
            }
        }
        
        @Override
        public J visitNewClass(J.NewClass newClass, J context) {
            J.NewClass mNewClass = (J.NewClass) super.visitNewClass(newClass, context);
            if (mNewClass == null) return null;

            // J.NewClass has: enclosing, arguments, class body (for anonymous), type (constructor type)
            J.Expression enclosing = mNewClass.getEnclosing();
            List<J.Expression> args = mNewClass.getArguments(); // These are J trees
            J.Block body = mNewClass.getBody(); // Anonymous class body
            J.Identifier classType = mNewClass.getClazz(); // Type being instantiated

            boolean enclosingIsRelevant = (enclosing != null && isIpOrAncestor(enclosing));
            boolean argsRelevant = args.stream().anyMatch(arg -> arg != null && isIpOrAncestor(arg));
            boolean bodyIsRelevant = (body != null && isIpOrAncestor(body)) || (body != null && !body.getStatements().isEmpty());
            boolean typeIsRelevant = (classType != null && isIpOrAncestor(classType));


            if (isIpOrAncestor(mNewClass)) {
                J.NewClass result = mNewClass;
                if (!argsRelevant) {
                    // Simplify args to empty list or list of default values if count matters.
                    // For now, if NewClass is ancestor, keep original args unless they were pruned.
                    // result = result.withArguments(java.util.Collections.emptyList());
                }
                if (body != null && !bodyIsRelevant) { // Anonymous class body not relevant
                    result = result.withBody(emptyBlock());
                }
                // Enclosing and type are usually essential, keep if NewClass is ancestor.
                return result;
            } else { // NewClass not ancestor, but some part is relevant
                if (enclosingIsRelevant || argsRelevant || bodyIsRelevant || typeIsRelevant) {
                    J.NewClass result = mNewClass;
                    if (!argsRelevant) result = result.withArguments(java.util.Collections.emptyList()); // Simplify args
                    if (body != null && !bodyIsRelevant) result = result.withBody(emptyBlock());
                    if (!enclosingIsRelevant && enclosing != null) result = result.withEnclosing(null); // Risky, might invalidate
                    if (!typeIsRelevant && classType != null) {
                        // Changing type is very risky. Keep original type if any part is relevant.
                    }
                    return result;
                } else {
                    if (mNewClass == insertionPointCursor.getValue()) { // IP is the new class expression itself
                         J.NewClass minimal = mNewClass.withArguments(java.util.Collections.emptyList());
                         if (minimal.getBody() != null) minimal = minimal.withBody(emptyBlock());
                         return minimal;
                    }
                    return null;
                }
            }
        }

        @Override
        public J visitLambda(J.Lambda lambda, J context) {
            J.Lambda mLambda = (J.Lambda) super.visitLambda(lambda, context);
            if (mLambda == null) return null;

            J.Lambda.Parameters params = mLambda.getParameters();
            J body = mLambda.getBody(); // J.Block or J.Expression

            boolean paramsRelevant = isIpOrAncestor(params); // Parameters object itself
            if (!paramsRelevant && params.getParameters().stream().anyMatch(p -> p!=null && isIpOrAncestor(p))) paramsRelevant = true;
            
            boolean bodyIsRelevant = (body != null && isIpOrAncestor(body));
            if (!bodyIsRelevant && body instanceof J.Block && !((J.Block)body).getStatements().isEmpty()) bodyIsRelevant = true;
            if (!bodyIsRelevant && !(body instanceof J.Block) && body != null) bodyIsRelevant = true; // Expression body is relevant if present and not IP/ancestor


            if (isIpOrAncestor(mLambda)) {
                J.Lambda result = mLambda;
                if (!paramsRelevant) {
                    // Create empty params or simplified params
                    // result = result.withParameters(new J.Lambda.Parameters(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, false, java.util.Collections.emptyList()));
                }
                if (!bodyIsRelevant) {
                    result = result.withBody(body instanceof J.Block ? emptyBlock() : simplifyExpression((J.Expression)body));
                }
                return result;
            } else {
                if (paramsRelevant || bodyIsRelevant) {
                     J.Lambda.Parameters newParams = params;
                     if(!paramsRelevant) newParams = new J.Lambda.Parameters(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, false, java.util.Collections.emptyList());
                     
                     J newBody = body;
                     if(!bodyIsRelevant) newBody = body instanceof J.Block ? emptyBlock() : simplifyExpression((J.Expression)body);

                    return mLambda.withParameters(newParams).withBody(newBody);
                } else {
                    if (mLambda == insertionPointCursor.getValue()) { // IP is the lambda itself
                         return mLambda.withParameters(new J.Lambda.Parameters(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, false, java.util.Collections.emptyList()))
                                       .withBody(body instanceof J.Block ? emptyBlock() : simplifyExpression((J.Expression)body));
                    }
                    return null;
                }
            }
        }

        @Override
        public J visitMemberReference(J.MemberReference memberRef, J context) {
            J.MemberReference mMemberRef = (J.MemberReference) super.visitMemberReference(memberRef, context);
            if (mMemberRef == null) return null; // Pruned if not relevant

            // Member reference components: containing expression, type parameters, reference name.
            // Typically, if the member reference itself isn't an IP or ancestor,
            // and none of its components are, it's pruned by the generic visit.
            // If it IS relevant (IP or ancestor), we keep it as is, because simplifying parts
            // (like the reference name or containing expression) often makes it invalid.
            // Children (containing expression, type parameters) would have been visited.
            // If they were not relevant, they might be simplified by their own visits.
            
            // So, if mMemberRef is not null, it means it or a component was relevant.
            // We generally don't simplify member references further due to their atomic nature.
            return mMemberRef;
        }

        // TODO: Implement visitWhileLoop, visitDoWhileLoop, visitTry (catches, finally, resources)
        // TODO: Implement visitTernary, visitAssert, visitEnumValueSet, visitEnumValue
        // TODO: Implement visitAssignment, visitAssignmentOperation, visitReturn, visitThrow, visitParentheses
    }
}
