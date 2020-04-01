package org.openrewrite.java.refactor;

import org.openrewrite.Cursor;
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.refactor.Formatter;

public class JavaFormatter extends Formatter {
    public JavaFormatter(J.CompilationUnit cu) {
        super(cu);
    }

    public Formatting format(Tree relativeToEnclosing) {
        Tree[] siblings = new Tree[0];
        if(relativeToEnclosing instanceof J.Block) {
            siblings = ((J.Block<?>) relativeToEnclosing).getStatements().toArray(Tree[]::new);
        }
        else if(relativeToEnclosing instanceof J.Case) {
            siblings = ((J.Case) relativeToEnclosing).getStatements().toArray(Tree[]::new);
        }

        Result indentation = findIndent(enclosingIndent(relativeToEnclosing), siblings);
        return Formatting.format(indentation.getPrefix());
    }

    public Formatting format(Cursor cursor) {
        return format(cursor.firstEnclosing(J.Block.class));
    }

    /**
     * @param moving The tree that is moving
     * @param into   The block the tree is moving into
     * @return A shift right format visitor that can be appended to a refactor visitor pipeline
     */
    public ShiftFormatRightVisitor shiftRight(Tree moving, Tree into, Tree enclosesBoth) {
        // NOTE: This isn't absolutely perfect... suppose the block moving was indented with tabs and the surrounding source was spaces.
        // Should be close enough in the vast majority of cases.
        int shift = enclosingIndent(into) - findIndent(enclosingIndent(enclosesBoth), moving).getEnclosingIndent();
        return new ShiftFormatRightVisitor(moving.getId(), shift, wholeSourceIndent().isIndentedWithSpaces());
    }

    public static int enclosingIndent(Tree enclosesBoth) {
        return enclosesBoth instanceof J.Block ? ((J.Block<?>) enclosesBoth).getIndent() :
                (int) enclosesBoth.getFormatting().getPrefix().chars().dropWhile(c -> c == '\n' || c == '\r')
                        .takeWhile(Character::isWhitespace).count();
    }

    public boolean isIndentedWithSpaces() {
        return wholeSourceIndent().isIndentedWithSpaces();
    }
}
