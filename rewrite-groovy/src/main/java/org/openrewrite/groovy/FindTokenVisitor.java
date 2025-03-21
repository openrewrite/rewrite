/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import lombok.Getter;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class FindTokenVisitor extends CodeVisitorSupport {

    private final int[] sourceLineNumberOffsets;
    private final int index;
    private final String tokenToFind;

    @Getter
    private boolean found = false;

    public FindTokenVisitor(String tokenToFind, int index, int[] sourceLineNumberOffsets) {
        this.index = index;
        this.tokenToFind = tokenToFind;
        this.sourceLineNumberOffsets = sourceLineNumberOffsets;
    }

    @Override
    public void visitBinaryExpression(BinaryExpression be) {
        if (toSourceIndex(be.getRightExpression().getLineNumber(), be.getRightExpression().getColumnNumber()) <= index) {
            be.getRightExpression().visit(this);
        }
        else if (toSourceIndex(be.getLeftExpression().getLastLineNumber(), be.getLeftExpression().getLastColumnNumber()) > index) {
            be.getLeftExpression().visit(this);
        } else if (tokenToFind.equals(be.getOperation().getText())){
            found = true;
        }
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
        for (Statement statement : block.getStatements()) {
            int start = toSourceIndex(statement.getLineNumber(), statement.getColumnNumber());
            int end = toSourceIndex(statement.getLastLineNumber(), statement.getLastColumnNumber());
            if (start >= index && end < index) {
                statement.visit(this);
            }
        }
    }

    private int toSourceIndex(int lineNumber, int columnNumber) {
        return sourceLineNumberOffsets[lineNumber - 1] + columnNumber - 1;
    }
}
