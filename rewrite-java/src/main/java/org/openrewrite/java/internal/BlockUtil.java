package org.openrewrite.java.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.LoathingOfOthers;
import org.openrewrite.internal.SelfLoathing;
import org.openrewrite.java.tree.J;

import java.util.Iterator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockUtil {

    /**
     * Determines if the passed cursor is an {@link J.Block} that is an initializer block.
     * @param cursor Must point to a {@link J.Block}
     * @return True if the cursor represents an initializer block, false otherwise.
     */
    @SelfLoathing(name = "Jonathan Leitschuh")
    @LoathingOfOthers("Who didn't encode this in the model?!")
    public static boolean isInitBlock(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.Block)) {
            throw new IllegalArgumentException("Cursor must point to a J.Block!");
        }
        J.Block block = cursor.getValue();
        if (block.isStatic()) {
            return false;
        }
        J.Block parentBlock = null;
        Iterator<Object> path = cursor.getPath();
        if (path.hasNext()) {
            path.next(); // skip the first element, which is the block itself
        }
        while (path.hasNext()) {
            Object next = path.next();
            if (parentBlock != null && next instanceof J.Block) {
                // If we find an outer block before a ClassDeclaration or NewClass, we're not in an initializer block.
                return false;
            } else if (next instanceof J.Block) {
                parentBlock = (J.Block) next;
                if (!parentBlock.getStatements().contains(block)) {
                    return false;
                }
            } else if (next instanceof J.ClassDeclaration) {
                J.ClassDeclaration classDeclaration = (J.ClassDeclaration) next;
                return classDeclaration.getBody() == parentBlock;
            } else if (next instanceof J.NewClass) {
                J.NewClass newClass = (J.NewClass) next;
                return newClass.getBody() == parentBlock;
            }
        }
        return false;
    }

    /**
     * Determines if the passed cursor is an {@link J.Block} that is static or initializer block.
     * @param cursor Must point to a {@link J.Block}
     * @return True if the cursor represents a static or initializer block, false otherwise.
     */
    public static boolean isStaticOrInitBlock(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.Block)) {
            throw new IllegalArgumentException("Cursor must point to a J.Block!");
        }
        J.Block block = cursor.getValue();
        return block.isStatic() || isInitBlock(cursor);
    }
}
