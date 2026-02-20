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
package org.openrewrite.bash.internal;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.bash.BashVisitor;
import org.openrewrite.bash.tree.Bash;
import org.openrewrite.bash.tree.Comment;
import org.openrewrite.bash.tree.Space;
import org.openrewrite.marker.Marker;

public class BashPrinter<P> extends BashVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        for (Comment comment : space.getComments()) {
            p.append(comment.getPrefix());
            p.append(comment.getText());
        }
        p.append(space.getWhitespace());
        return space;
    }

    @Override
    public Bash visitScript(Bash.Script script, PrintOutputCapture<P> p) {
        beforeSyntax(script, p);

        if (script.getShebang() != null) {
            visit(script.getShebang(), p);
        }

        for (Bash.Statement stmt : script.getStatements()) {
            visit(stmt, p);
        }

        visitSpace(script.getEof(), p);
        afterSyntax(script, p);
        return script;
    }

    @Override
    public Bash visitShebang(Bash.Shebang shebang, PrintOutputCapture<P> p) {
        beforeSyntax(shebang, p);
        p.append(shebang.getText());
        afterSyntax(shebang, p);
        return shebang;
    }

    @Override
    public Bash visitLiteral(Bash.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getText());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Bash visitCommand(Bash.Command command, PrintOutputCapture<P> p) {
        beforeSyntax(command, p);
        for (Bash.Assignment a : command.getAssignments()) {
            visit(a, p);
        }
        for (Bash.Expression arg : command.getArguments()) {
            visit(arg, p);
        }
        afterSyntax(command, p);
        return command;
    }

    @Override
    public Bash visitPipeline(Bash.Pipeline pipeline, PrintOutputCapture<P> p) {
        beforeSyntax(pipeline, p);
        if (pipeline.isNegated()) {
            p.append("!");
        }
        for (int i = 0; i < pipeline.getCommands().size(); i++) {
            visit(pipeline.getCommands().get(i), p);
            if (i < pipeline.getPipeOperators().size()) {
                Bash.Pipeline.PipeEntry pe = pipeline.getPipeOperators().get(i);
                visitSpace(pe.getPrefix(), p);
                p.append(pe.getOperator() == Bash.Pipeline.PipeOp.PIPE_AND ? "|&" : "|");
            }
        }
        afterSyntax(pipeline, p);
        return pipeline;
    }

    @Override
    public Bash visitCommandList(Bash.CommandList commandList, PrintOutputCapture<P> p) {
        beforeSyntax(commandList, p);
        for (int i = 0; i < commandList.getCommands().size(); i++) {
            visit(commandList.getCommands().get(i), p);
            if (i < commandList.getOperators().size()) {
                Bash.CommandList.OperatorEntry oe = commandList.getOperators().get(i);
                visitSpace(oe.getPrefix(), p);
                p.append(oe.getOperator() == Bash.CommandList.Operator.AND ? "&&" : "||");
            }
        }
        afterSyntax(commandList, p);
        return commandList;
    }

    @Override
    public Bash visitAssignment(Bash.Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, p);
        visit(assignment.getName(), p);
        p.append(assignment.getOperator());
        if (assignment.getValue() != null) {
            visit(assignment.getValue(), p);
        }
        afterSyntax(assignment, p);
        return assignment;
    }

    @Override
    public Bash visitIfStatement(Bash.IfStatement ifStmt, PrintOutputCapture<P> p) {
        beforeSyntax(ifStmt, p);
        visit(ifStmt.getIfKeyword(), p);
        for (Bash.Statement s : ifStmt.getCondition()) {
            visit(s, p);
        }
        visit(ifStmt.getThenKeyword(), p);
        for (Bash.Statement s : ifStmt.getThenBody()) {
            visit(s, p);
        }
        for (Bash.Elif elif : ifStmt.getElifs()) {
            visit(elif, p);
        }
        if (ifStmt.getElseClause() != null) {
            visit(ifStmt.getElseClause(), p);
        }
        visit(ifStmt.getFiKeyword(), p);
        afterSyntax(ifStmt, p);
        return ifStmt;
    }

    @Override
    public Bash visitElif(Bash.Elif elif, PrintOutputCapture<P> p) {
        beforeSyntax(elif, p);
        visit(elif.getElifKeyword(), p);
        for (Bash.Statement s : elif.getCondition()) {
            visit(s, p);
        }
        visit(elif.getThenKeyword(), p);
        for (Bash.Statement s : elif.getBody()) {
            visit(s, p);
        }
        afterSyntax(elif, p);
        return elif;
    }

    @Override
    public Bash visitElse(Bash.Else elseClause, PrintOutputCapture<P> p) {
        beforeSyntax(elseClause, p);
        visit(elseClause.getElseKeyword(), p);
        for (Bash.Statement s : elseClause.getBody()) {
            visit(s, p);
        }
        afterSyntax(elseClause, p);
        return elseClause;
    }

    @Override
    public Bash visitForLoop(Bash.ForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        visit(forLoop.getForKeyword(), p);
        visit(forLoop.getVariable(), p);
        if (forLoop.getInKeyword() != null) {
            visit(forLoop.getInKeyword(), p);
        }
        for (Bash.Expression e : forLoop.getIterables()) {
            visit(e, p);
        }
        visit(forLoop.getSeparator(), p);
        visit(forLoop.getDoKeyword(), p);
        for (Bash.Statement s : forLoop.getBody()) {
            visit(s, p);
        }
        visit(forLoop.getDoneKeyword(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public Bash visitCStyleForLoop(Bash.CStyleForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        visit(forLoop.getHeader(), p);
        visit(forLoop.getSeparator(), p);
        visit(forLoop.getDoKeyword(), p);
        for (Bash.Statement s : forLoop.getBody()) {
            visit(s, p);
        }
        visit(forLoop.getDoneKeyword(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public Bash visitWhileLoop(Bash.WhileLoop whileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(whileLoop, p);
        visit(whileLoop.getKeyword(), p);
        for (Bash.Statement s : whileLoop.getCondition()) {
            visit(s, p);
        }
        visit(whileLoop.getDoKeyword(), p);
        for (Bash.Statement s : whileLoop.getBody()) {
            visit(s, p);
        }
        visit(whileLoop.getDoneKeyword(), p);
        afterSyntax(whileLoop, p);
        return whileLoop;
    }

    @Override
    public Bash visitCaseStatement(Bash.CaseStatement caseStmt, PrintOutputCapture<P> p) {
        beforeSyntax(caseStmt, p);
        visit(caseStmt.getCaseKeyword(), p);
        visit(caseStmt.getWord(), p);
        visit(caseStmt.getInKeyword(), p);
        for (Bash.CaseItem item : caseStmt.getItems()) {
            visit(item, p);
        }
        visit(caseStmt.getEsacKeyword(), p);
        afterSyntax(caseStmt, p);
        return caseStmt;
    }

    @Override
    public Bash visitCaseItem(Bash.CaseItem caseItem, PrintOutputCapture<P> p) {
        beforeSyntax(caseItem, p);
        visit(caseItem.getPattern(), p);
        for (Bash.Statement s : caseItem.getBody()) {
            visit(s, p);
        }
        if (caseItem.getSeparator() != null) {
            visit(caseItem.getSeparator(), p);
        }
        afterSyntax(caseItem, p);
        return caseItem;
    }

    @Override
    public Bash visitFunction(Bash.Function function, PrintOutputCapture<P> p) {
        beforeSyntax(function, p);
        visit(function.getHeader(), p);
        visit(function.getBody(), p);
        afterSyntax(function, p);
        return function;
    }

    @Override
    public Bash visitSubshell(Bash.Subshell subshell, PrintOutputCapture<P> p) {
        beforeSyntax(subshell, p);
        p.append("(");
        for (Bash.Statement s : subshell.getBody()) {
            visit(s, p);
        }
        if (subshell.getClosingParen() != null) {
            visitSpace(subshell.getClosingParen(), p);
            p.append(")");
        }
        afterSyntax(subshell, p);
        return subshell;
    }

    @Override
    public Bash visitBraceGroup(Bash.BraceGroup braceGroup, PrintOutputCapture<P> p) {
        beforeSyntax(braceGroup, p);
        p.append("{");
        for (Bash.Statement s : braceGroup.getBody()) {
            visit(s, p);
        }
        if (braceGroup.getClosingBrace() != null) {
            visitSpace(braceGroup.getClosingBrace(), p);
            p.append("}");
        }
        afterSyntax(braceGroup, p);
        return braceGroup;
    }

    @Override
    public Bash visitRedirect(Bash.Redirect redirect, PrintOutputCapture<P> p) {
        beforeSyntax(redirect, p);
        p.append(redirect.getText());
        afterSyntax(redirect, p);
        return redirect;
    }

    @Override
    public Bash visitHereDoc(Bash.HereDoc hereDoc, PrintOutputCapture<P> p) {
        beforeSyntax(hereDoc, p);
        p.append(hereDoc.getOpening());
        for (String line : hereDoc.getContentLines()) {
            p.append(line);
        }
        p.append(hereDoc.getClosing());
        afterSyntax(hereDoc, p);
        return hereDoc;
    }

    @Override
    public Bash visitHereString(Bash.HereString hereString, PrintOutputCapture<P> p) {
        beforeSyntax(hereString, p);
        p.append("<<<");
        visit(hereString.getWord(), p);
        afterSyntax(hereString, p);
        return hereString;
    }

    @Override
    public Bash visitWord(Bash.Word word, PrintOutputCapture<P> p) {
        beforeSyntax(word, p);
        for (Bash.Expression part : word.getParts()) {
            visit(part, p);
        }
        afterSyntax(word, p);
        return word;
    }

    @Override
    public Bash visitSingleQuoted(Bash.SingleQuoted singleQuoted, PrintOutputCapture<P> p) {
        beforeSyntax(singleQuoted, p);
        p.append("'").append(singleQuoted.getText()).append("'");
        afterSyntax(singleQuoted, p);
        return singleQuoted;
    }

    @Override
    public Bash visitDoubleQuoted(Bash.DoubleQuoted doubleQuoted, PrintOutputCapture<P> p) {
        beforeSyntax(doubleQuoted, p);
        p.append("\"");
        for (Bash.Expression part : doubleQuoted.getParts()) {
            visit(part, p);
        }
        p.append("\"");
        afterSyntax(doubleQuoted, p);
        return doubleQuoted;
    }

    @Override
    public Bash visitDollarSingleQuoted(Bash.DollarSingleQuoted dsq, PrintOutputCapture<P> p) {
        beforeSyntax(dsq, p);
        p.append(dsq.getText());
        afterSyntax(dsq, p);
        return dsq;
    }

    @Override
    public Bash visitVariableExpansion(Bash.VariableExpansion ve, PrintOutputCapture<P> p) {
        beforeSyntax(ve, p);
        p.append(ve.getText());
        afterSyntax(ve, p);
        return ve;
    }

    @Override
    public Bash visitCommandSubstitution(Bash.CommandSubstitution cs, PrintOutputCapture<P> p) {
        beforeSyntax(cs, p);
        p.append(cs.isDollar() ? "$(" : "`");
        for (Bash.Statement s : cs.getBody()) {
            visit(s, p);
        }
        visitSpace(cs.getClosingDelimiter(), p);
        p.append(cs.isDollar() ? ")" : "`");
        afterSyntax(cs, p);
        return cs;
    }

    @Override
    public Bash visitArithmeticExpansion(Bash.ArithmeticExpansion ae, PrintOutputCapture<P> p) {
        beforeSyntax(ae, p);
        p.append(ae.isDollar() ? "$((" : "((");
        p.append(ae.getExpression());
        p.append("))");
        afterSyntax(ae, p);
        return ae;
    }

    @Override
    public Bash visitProcessSubstitution(Bash.ProcessSubstitution ps, PrintOutputCapture<P> p) {
        beforeSyntax(ps, p);
        p.append(ps.isInput() ? "<(" : ">(");
        for (Bash.Statement s : ps.getBody()) {
            visit(s, p);
        }
        if (ps.getClosingParen() != null) {
            visitSpace(ps.getClosingParen(), p);
            p.append(")");
        }
        afterSyntax(ps, p);
        return ps;
    }

    @Override
    public Bash visitConditionalExpression(Bash.ConditionalExpression ce, PrintOutputCapture<P> p) {
        beforeSyntax(ce, p);
        p.append("[[");
        visit(ce.getExpression(), p);
        visit(ce.getClosingBracket(), p);
        afterSyntax(ce, p);
        return ce;
    }

    @Override
    public Bash visitRedirected(Bash.Redirected redirected, PrintOutputCapture<P> p) {
        beforeSyntax(redirected, p);
        visit(redirected.getCommand(), p);
        for (Bash.Expression r : redirected.getRedirections()) {
            visit(r, p);
        }
        afterSyntax(redirected, p);
        return redirected;
    }

    @Override
    public Bash visitBackground(Bash.Background background, PrintOutputCapture<P> p) {
        beforeSyntax(background, p);
        visit(background.getCommand(), p);
        visitSpace(background.getAmpersandPrefix(), p);
        p.append("&");
        afterSyntax(background, p);
        return background;
    }

    @Override
    public Bash visitArrayLiteral(Bash.ArrayLiteral al, PrintOutputCapture<P> p) {
        beforeSyntax(al, p);
        p.append("(");
        for (Bash.Expression e : al.getElements()) {
            visit(e, p);
        }
        visitSpace(al.getClosingParen(), p);
        p.append(")");
        afterSyntax(al, p);
        return al;
    }

    private static final java.util.function.UnaryOperator<String> BASH_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Bash b, PrintOutputCapture<P> p) {
        beforeSyntax(b.getPrefix(), b.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, org.openrewrite.marker.Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new org.openrewrite.Cursor(getCursor(), marker), BASH_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new org.openrewrite.Cursor(getCursor(), marker), BASH_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Bash b, PrintOutputCapture<P> p) {
        afterSyntax(b.getMarkers(), p);
    }

    private void afterSyntax(org.openrewrite.marker.Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new org.openrewrite.Cursor(getCursor(), marker), BASH_MARKER_WRAPPER));
        }
    }
}
