package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

// Assuming a Mode enum or similar concept might be needed.
// For now, this is a placeholder. If a standard OpenRewrite enum exists for this, it should be used.
enum TemplateInsertionMode {
    BEFORE_STATEMENT,
    AFTER_STATEMENT,
    REPLACE_STATEMENT 
    // Potentially other modes for expressions, etc. but this generator is BlockStatement focused.
}

public class BlockStatementTemplateGenerator {

    private final AstPruner astPruner;

    public BlockStatementTemplateGenerator() {
        this.astPruner = new AstPruner();
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
        J.CompilationUnit prunedCu = astPruner.prune(originalCu, insertionPoint);

        Object prunedTargetElement = insertionPoint.getValue(); // Default to original if path re-navigation fails
        try {
            // Attempt to re-navigate the cursor path within the pruned CompilationUnit.
            // This provides the AST element in the pruned CU corresponding to the original insertion point.
            Cursor prunedCuRootCursor = new Cursor(null, prunedCu);
            Cursor targetCursorInPrunedCu = new Cursor(prunedCuRootCursor, insertionPoint.getPath());
            prunedTargetElement = targetCursorInPrunedCu.getValue();
        } catch (Exception e) {
            // This catch block is a fallback. If re-navigation fails, `prunedTargetElement` remains
            // the original element. Printing this might lead to inaccuracies if it was altered by pruning.
            // A robust solution might need more sophisticated ways to locate the element or mark it during pruning.
            System.err.println("Warning: Failed to re-navigate to insertion point in pruned CU. " +
                               "Template generation may be inaccurate. Error: " + e.getMessage());
        }

        String prunedCuString = prunedCu.printAll(); // Print the entire pruned Compilation Unit

        String targetElementString;
        if (prunedTargetElement instanceof J) {
            // Print the specific target element found in the pruned CU (or original if lookup failed).
            // Using .printAll() for the element to get its full representation.
            targetElementString = ((J) prunedTargetElement).printAll().trim();
        } else {
            // Should not happen if insertionPoint.getValue() is a J element.
            throw new IllegalStateException("Target element for template insertion is not a J instance: " +
                                            prunedTargetElement.getClass().getName());
        }

        if (targetElementString.isEmpty()) {
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
