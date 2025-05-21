package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

// Assuming a Mode enum or similar concept might be needed.
// For now, this is a placeholder. If a standard OpenRewrite enum exists for this, it should be used.
enum TemplateInsertionMode {
    BEFORE_STATEMENT,
    AFTER_STATEMENT,
    REPLACE_STATEMENT 
    // Potentially other modes for expressions, etc. but this generator is BlockStatement focused.
}

public class BlockStatementTemplateGenerator {

    private final ContextualAstPruner astPruner;

    public BlockStatementTemplateGenerator() {
        this.astPruner = new ContextualAstPruner();
    }

    /**
     * Generates a template string with the provided code snippet inserted at the specified
     * insertion point within a pruned context of the original compilation unit.
     *
     * @param insertionPoint The cursor indicating where to insert the template code.
     *                       The value of the cursor is expected to be a J.Statement.
     * @param code           The code snippet to be inserted.
     * @param mode           The mode determining how the code is inserted relative to the statement
     *                       at the insertion point (e.g., before, after, replacing it).
     * @return A string representing the templated code within its pruned context.
     * @throws IllegalArgumentException if the insertionPoint cursor value is not a J.Statement
     *                                  or if the mode is not applicable.
     */
    public String contextTemplate(Cursor insertionPoint, String code, TemplateInsertionMode mode) {
        J treeElement = insertionPoint.getValue();
        if (!(treeElement instanceof Statement || treeElement instanceof J.Block)) {
            // J.Block can be an insertion point if inserting into an empty block.
            // J.Case can also be an insertion point if we are in a switch.
            // For now, let's restrict to Statement for simplicity, can be expanded.
             if (!(treeElement instanceof J.Case)) { // J.Case is a Statement in some contexts
                throw new IllegalArgumentException("Insertion point value must be a J.Statement, J.Block, or J.Case. Found: " + treeElement.getClass());
             }
        }

        J.CompilationUnit originalCu = insertionPoint.firstEnclosingOrThrow(J.CompilationUnit.class);
        J originalTargetAstNode = insertionPoint.getValue(); // This is the J element in the original CU
        if (originalTargetAstNode == null) {
            throw new IllegalArgumentException("Insertion point cursor must point to a valid J element.");
        }
        UUID originalTargetId = originalTargetAstNode.getId();

        J.CompilationUnit prunedCu = astPruner.prune(originalCu, insertionPoint);

        // Find the equivalent of the originalTargetAstNode in the prunedCu using its ID
        AtomicReference<J> foundInPrunedCu = new AtomicReference<>();
        new JavaIsoVisitor<AtomicReference<J>>() {
            @Override
            public <T extends J> J visitTree(J tree, AtomicReference<J> targetHolder) {
                if (tree != null && tree.getId().equals(originalTargetId)) {
                    targetHolder.set(tree);
                    // Stop further traversal once found by returning the tree itself, not super.visitTree
                    return tree; 
                }
                // Only continue traversal if not found yet
                return targetHolder.get() == null ? super.visitTree(tree, targetHolder) : tree;
            }
        }.visit(prunedCu, foundInPrunedCu);

        J prunedTargetElement = foundInPrunedCu.get();

        if (prunedTargetElement == null) {
            // This means the pruner removed the exact AST node that was the insertion point.
            // This could be problematic if the pruner is too aggressive or if the insertion point
            // was on an element that was legitimately removed entirely.
            // Fallback: try to use the original element for printing, but this is risky.
            // Or, if the insertion point was, e.g., a J.Block, and it's gone,
            // we might try to find its parent if that was kept.
            System.err.println("Warning: The exact insertion point AST node (ID: " + originalTargetId +
                               ") was not found in the pruned CU. Template placement might be compromised.");
            // Attempt to use the *original* insertion point's parent block if the IP itself is gone.
            // This is a heuristic for cases where the IP statement was removed but its block context remains.
            if (originalTargetAstNode instanceof Statement) {
                J.Block parentBlockOfOriginalIp = insertionPoint.firstEnclosing(J.Block.class);
                if (parentBlockOfOriginalIp != null) {
                    AtomicReference<J> foundParentBlockInPrunedCu = new AtomicReference<>();
                     new JavaIsoVisitor<AtomicReference<J>>() {
                        @Override
                        public J visitBlock(J.Block block, AtomicReference<J> holder) {
                            if (block.getId().equals(parentBlockOfOriginalIp.getId())) {
                                holder.set(block);
                                return block;
                            }
                            return super.visitBlock(block, holder);
                        }
                    }.visit(prunedCu, foundParentBlockInPrunedCu);
                    if (foundParentBlockInPrunedCu.get() != null) {
                        prunedTargetElement = foundParentBlockInPrunedCu.get(); // Target insertion inside this block
                        System.err.println("Fallback: Using parent block (ID: " + prunedTargetElement.getId() + ") as insertion context.");
                         // For modes like BEFORE/AFTER, this needs careful handling.
                         // For REPLACE_STATEMENT on a block, this means inserting into the block.
                         // If mode is BEFORE/AFTER original (now gone) statement, we might insert at start/end of parent block.
                         // This logic gets very complex. For now, we'll let it try to print this block and search for it.
                    } else {
                        // Parent block also not found. Very difficult to place template.
                        System.err.println("Error: Parent block of original IP also not found. Cannot reliably place template.");
                        return "// ERROR: Could not locate insertion point or its parent block in pruned context.\n" +
                               "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCu.printAll();
                    }
                } else {
                     System.err.println("Error: Original IP had no parent block. Cannot reliably place template.");
                     return "// ERROR: Could not locate insertion point in pruned context.\n" +
                               "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCu.printAll();
                }
            } else {
                 System.err.println("Error: Could not locate non-statement insertion point in pruned context.");
                 return "// ERROR: Could not locate insertion point in pruned context.\n" +
                           "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCu.printAll();
            }
        }

        String prunedCuString = prunedCu.printAll();
        String targetElementString = prunedTargetElement.printAll().trim();

        if (targetElementString.isEmpty() && !(prunedTargetElement instanceof J.Block && ((J.Block)prunedTargetElement).getStatements().isEmpty())) {
             // If it's not an empty block but prints empty (e.g. J.Empty), handle similarly to not found,
             // as indexOf("") is problematic.
             System.err.println("Warning: Target element prints as empty string. Fallback for J.Empty or similar.");
             // This case needs very careful handling. For now, assume it's like node not found.
             // If the insertion point was J.Empty, we might want to insert into the parent block at its location.
             // This is covered by the "prunedTargetElement == null" fallback to parent block if that logic is robust.
             // For now, error out or use a very broad context.
             return "// WARNING: Target element string is empty. Template may be misplaced.\n" +
                    "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCuString;
        }
        
        // Special handling for inserting into an empty block when it's the target
        if (prunedTargetElement instanceof J.Block && ((J.Block)prunedTargetElement).getStatements().isEmpty() &&
            (mode == TemplateInsertionMode.REPLACE_STATEMENT || targetElementString.equals("{}") || targetElementString.equals("{\n}"))) {
            // If mode is REPLACE_STATEMENT for an empty block, or if the target string is just empty braces.
            // We want to insert *inside* the braces.
            String blockRepresentation = prunedTargetElement.printAll(); // Get exact representation
            int openingBraceIndex = prunedCuString.indexOf(blockRepresentation);
            if (openingBraceIndex != -1) {
                openingBraceIndex = prunedCuString.indexOf('{', openingBraceIndex); // Find the actual brace
                 if (openingBraceIndex != -1) {
                    String before = prunedCuString.substring(0, openingBraceIndex + 1);
                    String after = prunedCuString.substring(openingBraceIndex + 1);
                    return before + "\n//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + after;
                }
            }
            // Fallback if block representation not found (should not happen if prunedTargetElement is from prunedCu)
            System.err.println("Warning: Could not find empty block representation for targeted insertion. Defaulting to string search.");
            // Proceed to normal indexOf search for targetElementString, which might be "{}"
        }


        if (targetElementString.isEmpty()) { // Catch-all for empty target string after block check
            // This can happen for J.Empty or if the element prints as nothing.
            System.err.println("Warning: Target element for template insertion prints as empty. " +
                               "Template markers will be placed based on mode relative to an assumed point.");
            // Fallback: if we can't find an empty string, we can't place relative to it.
            // This case needs specific handling based on what J.Empty means for insertion.
            // For now, let's assume this means we append to the start of the pruned CU for safety,
            // though this is likely not what the user wants for BEFORE/AFTER/REPLACE on an empty statement.
            // A better approach for J.Empty might be to find the end of the previous statement or start of next.
            // This part of the logic is complex and depends on desired behavior for J.Empty.
            // The original naive version effectively appended to the start if string was not found.
            // Let's try to find the block and insert there.
            if (insertionPoint.getValue() instanceof J.Block || (insertionPoint.getValue() instanceof Statement && ((Statement)insertionPoint.getValue()).getPrefix().getWhitespace().isEmpty()) ) {
                 J.Block parentBlock = insertionPoint.firstEnclosing(J.Block.class);
                 if(parentBlock != null) {
                    Object prunedParentBlock = parentBlock;
                     try {
                        Cursor prunedCuRootCursor = new Cursor(null, prunedCu);
                        Cursor targetCursorInPrunedCu = new Cursor(prunedCuRootCursor, new Cursor(insertionPoint, parentBlock).getPath());
                        prunedParentBlock = targetCursorInPrunedCu.getValue();
                     } catch (Exception e) { /* use original parentBlock */ }

                     String parentBlockString = ((J)prunedParentBlock).printAll().trim();
                     int blockStartIndex = prunedCuString.indexOf(parentBlockString);
                     if(blockStartIndex != -1) {
                        int openingBraceIndex = prunedCuString.indexOf('{', blockStartIndex);
                        if(openingBraceIndex != -1) {
                            String before = prunedCuString.substring(0, openingBraceIndex + 1);
                            String after = prunedCuString.substring(openingBraceIndex + 1);
                            return before + "\n//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + after;
                        }
                     }
                 }
            }
             System.err.println("Fallback: Target element string is empty, wrapping entire pruned CU with template markers.");
             return "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCuString;
        }


        int insertionIndex = prunedCuString.indexOf(targetElementString);

        if (insertionIndex == -1) {
            // If the target element string (even after attempting to find it in prunedCu) is not found.
            System.err.println("Error: Could not find target element string '" + targetElementString + "' in pruned CU string. " +
                               "Template generation will be a fallback. This may indicate issues with pruning or element printing.");
            // Fallback: Wrap the entire pruned CU content with the template markers.
            // This is a last resort and usually indicates a problem.
            return "//__TEMPLATE__\n" + code + "\n//__TEMPLATE_STOP__\n" + prunedCuString;
        }

        // Assemble the final template string based on the insertion mode
        StringBuilder result = new StringBuilder();
        String beforeTarget = prunedCuString.substring(0, insertionIndex);
        String afterTarget = prunedCuString.substring(insertionIndex + targetElementString.length());

        switch (mode) {
            case BEFORE_STATEMENT:
                result.append(beforeTarget)
                      .append("//__TEMPLATE__\n").append(code).append("\n//__TEMPLATE_STOP__\n")
                      .append(targetElementString) // Add the original element's string representation
                      .append(afterTarget);
                break;
            case AFTER_STATEMENT:
                result.append(beforeTarget)
                      .append(targetElementString) // Add the original element's string representation
                      .append("\n//__TEMPLATE__\n").append(code).append("\n//__TEMPLATE_STOP__")
                      .append(afterTarget);
                break;
            case REPLACE_STATEMENT:
                // For REPLACE_STATEMENT, the targetElementString is omitted from the reconstruction.
                result.append(beforeTarget)
                      .append("//__TEMPLATE__\n").append(code).append("\n//__TEMPLATE_STOP__")
                      .append(afterTarget);
                break;
            default:
                // Should be caught by enum type safety, but as a guard:
                throw new IllegalArgumentException("Unsupported template insertion mode: " + mode);
        }
        return result.toString();
    }
}
