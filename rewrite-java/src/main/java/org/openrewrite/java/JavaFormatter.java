/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.search.FindIndentJava;
import org.openrewrite.java.tree.J;
import org.openrewrite.refactor.Formatter;

import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFormatter extends Formatter {
    public JavaFormatter(J.CompilationUnit cu) {
        super(cu, FindIndentJava::new);
    }

    public Formatting format(Tree relativeToEnclosing) {
        Tree[] siblings = new Tree[0];
        if(relativeToEnclosing instanceof J.Block) {
            siblings = ((J.Block<?>) relativeToEnclosing).getStatements().toArray(new Tree[0]);
        }
        else if(relativeToEnclosing instanceof J.Case) {
            siblings = ((J.Case) relativeToEnclosing).getStatements().toArray(new Tree[0]);
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
        return new ShiftFormatRightVisitor(moving, shift, wholeSourceIndent().isIndentedWithSpaces());
    }

    public static int enclosingIndent(Tree enclosesBoth) {
        AtomicBoolean dropWhile = new AtomicBoolean(false);
        AtomicBoolean takeWhile = new AtomicBoolean(true);
        return enclosesBoth instanceof J.Block ?
                ((J.Block<?>) enclosesBoth).getIndent() :
                (int) enclosesBoth.getPrefix().chars()
                        .filter(c -> {
                            dropWhile.set(dropWhile.get() || !(c == '\n' || c == '\r'));
                            return dropWhile.get();
                        })
                        .filter(c -> {
                            takeWhile.set(takeWhile.get() && Character.isWhitespace(c));
                            return takeWhile.get();
                        })
                        .count();
    }

    public boolean isIndentedWithSpaces() {
        return wholeSourceIndent().isIndentedWithSpaces();
    }
}
