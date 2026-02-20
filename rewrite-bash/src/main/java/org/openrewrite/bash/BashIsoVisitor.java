/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.bash;

import org.openrewrite.bash.tree.Bash;

public class BashIsoVisitor<P> extends BashVisitor<P> {

    @Override
    public Bash.Script visitScript(Bash.Script script, P p) {
        return (Bash.Script) super.visitScript(script, p);
    }

    @Override
    public Bash.Literal visitLiteral(Bash.Literal literal, P p) {
        return (Bash.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public Bash.Command visitCommand(Bash.Command command, P p) {
        return (Bash.Command) super.visitCommand(command, p);
    }

    @Override
    public Bash.Pipeline visitPipeline(Bash.Pipeline pipeline, P p) {
        return (Bash.Pipeline) super.visitPipeline(pipeline, p);
    }

    @Override
    public Bash.Assignment visitAssignment(Bash.Assignment assignment, P p) {
        return (Bash.Assignment) super.visitAssignment(assignment, p);
    }

    @Override
    public Bash.IfStatement visitIfStatement(Bash.IfStatement ifStmt, P p) {
        return (Bash.IfStatement) super.visitIfStatement(ifStmt, p);
    }

    @Override
    public Bash.ForLoop visitForLoop(Bash.ForLoop forLoop, P p) {
        return (Bash.ForLoop) super.visitForLoop(forLoop, p);
    }

    @Override
    public Bash.WhileLoop visitWhileLoop(Bash.WhileLoop whileLoop, P p) {
        return (Bash.WhileLoop) super.visitWhileLoop(whileLoop, p);
    }

    @Override
    public Bash.CaseStatement visitCaseStatement(Bash.CaseStatement caseStmt, P p) {
        return (Bash.CaseStatement) super.visitCaseStatement(caseStmt, p);
    }

    @Override
    public Bash.Function visitFunction(Bash.Function function, P p) {
        return (Bash.Function) super.visitFunction(function, p);
    }
}
