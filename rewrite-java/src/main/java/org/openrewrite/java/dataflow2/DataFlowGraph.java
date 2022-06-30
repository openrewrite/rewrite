/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow2;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.java.dataflow2.ProgramPoint.ENTRY;
import static org.openrewrite.java.dataflow2.ProgramPoint.EXIT;

@Incubating(since = "7.25.0")
public class DataFlowGraph {

    final J.CompilationUnit cu;

    // This is a temporary hack to compute next from previous
    // until the next() methods are implemented.
    private Map<Cursor, List<Cursor>> previousMap;

    // NLE statement -> target
    Map<ProgramPoint, Cursor> nonLocalExitsForward;
    // target -> list(NLE statements targeting target)
    Map<ProgramPoint, ArrayList<Cursor>> nonLocalExitsBackward;

    public DataFlowGraph(J.CompilationUnit cu) {
        this.cu = cu;

        nonLocalExitsForward = new HashMap<>();
        nonLocalExitsBackward = new HashMap<>();
        new NonLocalExitsVisitor().visit(cu, 0);
        // add an implicit final return when the last statement is not an explicit return

        //previousMap = new HashMap<>();
        //new PreviousMapVisitor().visit(cu, null);
    }

//    /**
//     * @param c A cursor whose value is a program point.
//     * @return The set of program points, possibly composite (i.e. containing other program points, such as
//     * a while loop), preceding given program point in the dataflow graph.
//     */
//    public List<Cursor> previous(Cursor c) {
//        //return previousIn(programPoint, ENTRY);
//        return previousIn(c.getParentOrThrow(), c.getValue());
//    }

    public List<Cursor> previousIn(Cursor parentCursor, ProgramPoint current) {
        while (!(parentCursor.getValue() instanceof J)) {
            parentCursor = parentCursor.getParentOrThrow();
        }

        J parent = parentCursor.getValue();
        switch (parent.getClass().getName().replaceAll("^org.openrewrite.java.tree.", "")) {
            case "J$Assignment":
                return previousInAssignment(parentCursor, current);
            case "J$Block":
                return previousInBlock(parentCursor, current);
            case "J$MethodInvocation":
                return previousInMethodInvocation(parentCursor, current);
            case "J$NewClass":
                return previousInNewClass(parentCursor, current);
            case "J$If":
                return previousInIf(parentCursor, current);
            case "J$If$Else":
                return previousInIfElse(parentCursor, current);
            case "J$WhileLoop":
                return previousInWhileLoop(parentCursor, current);
            case "J$ForLoop":
                return previousInForLoop(parentCursor, current);
            case "J$ForLoop$Control":
                return previousInForLoopControl(parentCursor, current);
            case "J$VariableDeclarations":
                return previousInVariableDeclarations(parentCursor, current);
            case "J$Unary":
                return previousInUnary(parentCursor, current);
            case "J$Binary":
                return previousInBinary(parentCursor, current);
            case "J$Parentheses":
                return previousInParentheses(parentCursor, current);
            case "J$ControlParentheses":
                return previousInControlParentheses(parentCursor, current);
            case "J$VariableDeclarations$NamedVariable":
                return previousInNamedVariable(parentCursor, current);
            case "J$Return":
                return previousInReturn(parentCursor, current);
            case "J$Throw":
                return previousInThrow(parentCursor, current);
            case "J$Try":
                return previousInTry(parentCursor, current);
            case "J$Try$Catch":
                return previousInTryCatch(parentCursor, current);
            case "J$MethodDeclaration":
                return previousInMethodDeclaration(parentCursor, current);
            case "J$ArrayAccess":
                return previousInArrayAccess(parentCursor, current);
            case "J$ArrayDimension":
                return previousInArrayDimension(parentCursor, current);
            case "J$Assert":
                return previousInAssert(parentCursor, current);
            case "J$CompilationUnit":
            case "J$ClassDeclaration":
                return Collections.emptyList();
            case "J$Literal":
            case "J$Identifier":
            case "J$Empty":
                // all these are terminal nodes
                return previousInTerminalNode(parentCursor, current);
            case "J$Primitive":
                // not actually a program point, but implements Expression or Statement
                return previousInTerminalNode(parentCursor, current);
            default:
                // Assert
                // ArrayAccess
                // AssignmentOperation
                // Break
                // Case
                // Continue
                // DoWhileLoop
                // EnumValue
                // EnumValueSet
                // FieldAccess
                // ForeachLoop
                // InstanceOf
                // Label
                // Lambda
                // MemberReference
                // MultiCatch
                // NewArray
                // ArrayDimension
                // Return
                // Switch
                // Ternary
                // Throw
                // Try
                // TypeCast
                // WhileLoop
                throw new Error("Not implemented : " + parent.getClass().getName());
        }
    }

    List<Cursor> previousInBlock(Cursor parentCursor, ProgramPoint p) {
        J.Block parent = parentCursor.getValue();
        List<Statement> stmts = parent.getStatements();
        if (p == EXIT) {
            if (stmts.size() > 0) {
                //return Collections.singletonList(new Cursor(parentCursor, stmts.get(stmts.size() - 1)));
                return previousIn(new Cursor(parentCursor, stmts.get(stmts.size() - 1)), EXIT);
            } else {
                return previousIn(parentCursor.getParent(), parentCursor.getValue());
            }
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parent);
        } else {
            int index = stmts.indexOf(p);
            if (index > 0) {
                return previousIn(new Cursor(parentCursor, stmts.get(index - 1)), EXIT);
            } else if (index == 0) {
                return previousIn(parentCursor.getParent(), parentCursor.getValue());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public List<Cursor> previousInVariableDeclarations(Cursor parentCursor, ProgramPoint p) {
        J.VariableDeclarations parent = parentCursor.getValue();
        List<J.VariableDeclarations.NamedVariable> variables = parent.getVariables();
        if (p == EXIT) {
            if (variables.size() > 0) {
                return previousIn(new Cursor(parentCursor, variables.get(variables.size() - 1)), EXIT);
            } else {
                return previousIn(parentCursor.getParent(), parentCursor.getValue());
            }
        } else if (p == ENTRY) {
            //return DataFlowGraph.previous(parentCursor);
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == parent.getTypeExpression()) {
            // p is not a program point
            return Collections.emptyList();
        } else {
            int index = parent.getVariables().indexOf(p);
            if (index > 0) {
                //return Collections.singletonList(new Cursor(parentCursor, variables.get(index - 1)));
                return previousIn(new Cursor(parentCursor, variables.get(index - 1)), EXIT);
            } else if (index == 0) {
                //return DataFlowGraph.previous(parentCursor);
                return previousIn(parentCursor.getParent(), parentCursor.getValue());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public List<Cursor> previous(Cursor c) {
        List<Cursor> prev = previousIn(c, EXIT);
        if(prev.size() == 1 && prev.contains(c)) {
            return previousIn(c, ENTRY);
        } else {
            return prev;
        }
    }

    public List<Cursor> previous(List<Cursor> cs) {
        return cs.stream().flatMap(c -> previous(c).stream()).collect(Collectors.toList());
    }

    enum ForLoopPosition {
        INIT, CONDITION, UPDATE
    }

    /**
     * @return The last program point(s) in the for-loop at given position, which might be
     * last of the previous position if given position is empty, or even the previous program point
     * of the for-loop if all preceding positions are empty.
     */
    List<Cursor> lastInForLoop(Cursor forLoopCursor, ForLoopPosition position) {

        J.ForLoop forLoop = forLoopCursor.getValue();
        J.ForLoop.Control control = forLoop.getControl();

        List<Statement> init = control.getInit();
        List<Statement> update = control.getUpdate();

        if (position == ForLoopPosition.UPDATE) {
            if (update.size() > 0) {
                return Collections.singletonList(new Cursor(forLoopCursor, update.get(update.size() - 1)));
            } else {
                return lastInForLoop(forLoopCursor, ForLoopPosition.INIT);
            }
        }
        if (position == ForLoopPosition.INIT) {
            if (init.size() > 0) {
                return previousIn(new Cursor(forLoopCursor, init.get(init.size() - 1)), EXIT);
            } else {
                return previousIn(forLoopCursor.getParent(), forLoopCursor.getValue());
            }
        }
        throw new IllegalStateException();
    }

    List<Cursor> previousInMethodDeclaration(Cursor parentCursor, ProgramPoint p) {
        J.MethodDeclaration parent = parentCursor.getValue();
        if (p == EXIT) {
            List<Cursor> nonLocalExits = nonLocalExitsBackward.get(parent);
            List<Cursor> result = new ArrayList<>(nonLocalExits);
            List<Cursor> normalExits = previousIn(new Cursor(parentCursor, parent.getBody()), EXIT);
            for (Cursor normalExit : normalExits) {
                if (!nonLocalExits.contains(normalExit)) {
                    result.add(normalExit);
                }
            }
            return result;
        } else if (p == ENTRY) {
            return Collections.emptyList();
        } else if (p == parent.getBody()) {
            return Collections.emptyList();
        } else {
            throw new IllegalStateException();
        }
    }

    List<Cursor> previousInArrayAccess(Cursor parentCursor, ProgramPoint p) {
        J.ArrayAccess ac = parentCursor.getValue();
        if (p == EXIT) {
            return Collections.singletonList(parentCursor);
        } else if (p == ENTRY) {
            return previousIn(new Cursor(parentCursor.getParent(), ac.getDimension().getIndex()), EXIT);
        } else if (p == ac.getIndexed()) {
            return previousIn(parentCursor.getParentOrThrow(), ac);
        }
        throw new IllegalStateException();
    }

    List<Cursor> previousInArrayDimension(Cursor parentCursor, ProgramPoint p) {
        J.ArrayDimension dim = parentCursor.getValue();
        if (p == EXIT) {
            return Collections.singletonList(parentCursor);
        } else if (p == ENTRY) {
            return previousIn(new Cursor(parentCursor, dim.getIndex()), EXIT);
        } else if (p == dim.getIndex()) {
            J.ArrayAccess ac = parentCursor.getParentOrThrow().getValue();
            return previousIn(new Cursor(parentCursor, ac.getIndexed()), EXIT);
        }
        throw new IllegalStateException();
    }

    List<Cursor> previousInMethodInvocation(Cursor parentCursor, ProgramPoint p) {

        J.MethodInvocation parent = parentCursor.getValue();
        Expression select = parent.getSelect();
        List<Expression> args = parent.getArguments();

        if (p == EXIT) {
            return Collections.singletonList(parentCursor);
        } else if (p == ENTRY) {
            if (args.size() > 0 && !(args.get(0) instanceof J.Empty)) {
                return previousIn(new Cursor(parentCursor, args.get(args.size() - 1)), EXIT);
            } else if (select != null) {
                return previousIn(new Cursor(parentCursor, select), EXIT);
            } else {
                return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
            }
        } else if (p == parent.getSelect()) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == parent.getName()) {
            // Not actually a program point
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else {
            int index = args.indexOf(p);
            if (index > 0) {
                return Collections.singletonList(new Cursor(parentCursor, args.get(index - 1)));
            } else if (index == 0) {
                if (parent.getSelect() != null) {
                    return Collections.singletonList(new Cursor(parentCursor, parent.getSelect()));
                } else {
                    // implicit this
                    return previousIn(parentCursor.getParent(), parent);
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    List<Cursor> previousInNewClass(Cursor parentCursor, ProgramPoint p) {

        J.NewClass parent = parentCursor.getValue();
        List<Expression> args = parent.getArguments();

        if (p == EXIT) {
            return Collections.singletonList(parentCursor);
        } else if (p == ENTRY) {
            if (args != null && args.size() > 0 && !(args.get(0) instanceof J.Empty)) {
                return previousIn(new Cursor(parentCursor, args.get(args.size() - 1)), EXIT);
            } else {
                return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
            }
        } else if (p == parent.getClazz()) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else {
            int index = args.indexOf(p);
            if (index > 0) {
                return Collections.singletonList(new Cursor(parentCursor, args.get(index - 1)));
            } else if (index == 0) {
                return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
            } else {
                throw new IllegalStateException();
            }
        }
    }

    List<Cursor> previousInAssert(Cursor parentCursor, ProgramPoint p) {
        J.Assert assrt = parentCursor.getValue();
        if (p == EXIT) {
            List<Cursor> result = new ArrayList<>();
            result.add(new Cursor(parentCursor, assrt.getCondition()));
            if (assrt.getDetail() != null && assrt.getDetail().getElement() != null) {
                result.add(new Cursor(parentCursor, assrt.getDetail().getElement()));
            }
            return result;
        } else if (p == ENTRY) {
            return previousIn(new Cursor(parentCursor, assrt.getCondition()), EXIT);
        } else if (p == assrt.getCondition()) {
            if (assrt.getDetail() != null) {
                return previousIn(new Cursor(parentCursor, assrt.getDetail()), EXIT);
            } else {
                return previousIn(new Cursor(parentCursor, assrt), EXIT);
            }
        } else if (assrt.getDetail() != null && p == assrt.getDetail().getElement()) {
            return previousIn(new Cursor(parentCursor, assrt), ENTRY);
        }
        throw new UnsupportedOperationException();
    }

    List<Cursor> previousInIf(Cursor ifCursor, ProgramPoint p) {

        J.If ifThenElse = ifCursor.getValue();
        J.ControlParentheses<Expression> cond = ifThenElse.getIfCondition();
        Statement thenPart = ifThenElse.getThenPart();
        J.If.@Nullable Else elsePart = ifThenElse.getElsePart();

        if (p == EXIT) {
            List<Cursor> result = new ArrayList<>();
            result.add(new Cursor(ifCursor, thenPart));
            if (elsePart == null) {
                Cursor c = new Cursor(ifCursor, cond);
                c.putMessage("ifThenElseBranch", "exit");
                result.add(c);
            } else {
                result.add(new Cursor(ifCursor, elsePart));
            }
            return result;
        } else if (p == ENTRY) {
            return previousIn(ifCursor.getParentOrThrow(), ifThenElse);
        } else if (p == thenPart) {
            Cursor c = new Cursor(ifCursor, cond);
            c.putMessage("ifThenElseBranch", "then");
            return Collections.singletonList(c);
        } else if (p == elsePart) {
            Cursor c = new Cursor(ifCursor, cond);
            c.putMessage("ifThenElseBranch", "else");
            return Collections.singletonList(c);
        } else if (p == cond) {
            //return Collections.singletonList(new Cursor(ifCursor, cond));
            return previousIn(ifCursor.getParentOrThrow(), ifThenElse);
        }
        throw new IllegalStateException();
    }

    List<Cursor> previousInIfElse(Cursor ifElseCursor, ProgramPoint p) {

        J.If.Else ifElse = ifElseCursor.getValue();
        Statement body = ifElse.getBody();

        if (p == EXIT) {
            return Collections.singletonList(new Cursor(ifElseCursor, body));
        } else if (p == ENTRY) {
            return previousIn(ifElseCursor.getParentOrThrow(), ifElse);
        } else if (p == body) {
            return previousIn(ifElseCursor.getParent(), ifElseCursor.getValue());
        }
        throw new IllegalStateException();
    }

    List<Cursor> previousInWhileLoop(Cursor whileCursor, ProgramPoint p) {

        J.WhileLoop _while = whileCursor.getValue();
        J.ControlParentheses<Expression> cond = _while.getCondition();
        Statement body = _while.getBody();

        // while(cond: Expression) {
        //   body: Statement
        // }

        if (p == EXIT) {
            //List<Cursor> result = new ArrayList<>();
            //result.add(new Cursor(whileCursor, body));
            //result.add(new Cursor(whileCursor, cond));
            return Collections.singletonList(new Cursor(whileCursor, cond));
        } else if (p == ENTRY) {
            return previousIn(whileCursor.getParentOrThrow(), _while);
        } else if (p == body) {
            return Collections.singletonList(new Cursor(whileCursor, cond));
        } else if (p == cond) {
            List<Cursor> result = new ArrayList<>();
            result.add(new Cursor(whileCursor, body));
            result.addAll(previousIn(whileCursor.getParentOrThrow(), _while));
            return result;
        }

        throw new IllegalStateException();
    }

    List<Cursor> previousInForLoop(Cursor forLoopCursor, ProgramPoint p) {

        J.ForLoop forLoop = forLoopCursor.getValue();
        List<Statement> init = forLoop.getControl().getInit();
        Expression cond = forLoop.getControl().getCondition();
        Statement body = forLoop.getBody();
        List<Statement> update = forLoop.getControl().getUpdate();

        // init: List<Statement>
        // while(cond: Expression) {
        //   body: Statement
        //   update: List<Statement>
        // }

        if (p == EXIT) {
            return ListUtils.concat(lastInForLoop(forLoopCursor, ForLoopPosition.UPDATE),
                    new Cursor(forLoopCursor, cond));
        } else if (p == ENTRY) {
            return previousIn(forLoopCursor.getParentOrThrow(), forLoop);

        } else if (p == body) {
            return Collections.singletonList(new Cursor(forLoopCursor, cond));

        } else if (p == cond) {
            List<Cursor> result = new ArrayList<>();
            result.addAll(lastInForLoop(forLoopCursor, ForLoopPosition.INIT));
            result.addAll(lastInForLoop(forLoopCursor, ForLoopPosition.UPDATE));
            return result;

        } else {
            int index;

            index = init.indexOf(p);
            if (index > 0) {
                return Collections.singletonList(new Cursor(forLoopCursor, init.get(index - 1)));
            } else if (index == 0) {
                return previousIn(forLoopCursor.getParent(), forLoopCursor.getValue());
            }

            index = update.indexOf(p);
            if (index > 0) {
                return Collections.singletonList(new Cursor(forLoopCursor, update.get(index - 1)));
            } else if (index == 0) {
                return previousIn(new Cursor(forLoopCursor, body), EXIT);
            }

            throw new IllegalStateException();
        }
    }

    List<Cursor> previousInForLoopControl(Cursor forLoopControlCursor, ProgramPoint p) {

        J.ForLoop.Control forLoopControl = forLoopControlCursor.getValue();
        return previousInForLoop(forLoopControlCursor.getParentOrThrow(), p);
    }

    public List<Cursor> previousInParentheses(Cursor parenthesesCursor, ProgramPoint p) {
        J.Parentheses<?> parentheses = parenthesesCursor.getValue();
        J tree = parentheses.getTree();

        if (p == ENTRY) {
            return Collections.singletonList(new Cursor(parenthesesCursor, tree));
        } else if (p == EXIT) {
            return Collections.singletonList(parenthesesCursor);
        } else if (p == tree) {
            return previousIn(parenthesesCursor.getParent(), parenthesesCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInControlParentheses(Cursor parenthesesCursor, ProgramPoint p) {
        J.ControlParentheses<?> parentheses = parenthesesCursor.getValue();
        J tree = parentheses.getTree();

        if (p == ENTRY) {
            return Collections.singletonList(new Cursor(parenthesesCursor, tree));
        } else if (p == EXIT) {
            return Collections.singletonList(parenthesesCursor);
        } else if (p == tree) {
            return previousIn(parenthesesCursor.getParent(), parenthesesCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInNamedVariable(Cursor namedVariableCursor, ProgramPoint p) {
        J.VariableDeclarations.NamedVariable namedVariable = namedVariableCursor.getValue();
        J.Identifier name = namedVariable.getName();
        Expression initializer = namedVariable.getInitializer();

        if (p == EXIT) {
            return Collections.singletonList(namedVariableCursor);
        } else if (p == ENTRY) {
            //return DataFlowGraph.previousIn(namedVariableCursor.getParentOrThrow(), namedVariableCursor.getValue());
            if (initializer != null) {
                return previousIn(new Cursor(namedVariableCursor, initializer), EXIT);
            } else {
                return previousIn(namedVariableCursor.getParentOrThrow(), namedVariable);
            }
        } else if (p == name) {
            //return Collections.emptyList();
            return previousIn(namedVariableCursor.getParentOrThrow(), namedVariableCursor.getValue());
        } else if (p == initializer) {
            return previousIn(namedVariableCursor.getParentOrThrow(), namedVariableCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInUnary(Cursor unaryCursor, ProgramPoint p) {
        J.Unary unary = unaryCursor.getValue();
        Expression expr = unary.getExpression();

        if (p == ENTRY) {
            return Collections.singletonList(new Cursor(unaryCursor, expr));
        } else if (p == EXIT) {
            //return previousIn(new Cursor(unaryCursor, expr), EXIT);
            return Collections.singletonList(unaryCursor);
        } else if (p == unary.getExpression()) {
            return previousIn(unaryCursor.getParentOrThrow(), unary);
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInBinary(Cursor binaryCursor, ProgramPoint p) {
        J.Binary binary = binaryCursor.getValue();

        Expression left = binary.getLeft();
        Expression right = binary.getRight();
        J.Binary.Type op = binary.getOperator();

        // ENTRY -> left
        // left -> right
        // right -> binary
        // binary -> EXIT

        if (p == ENTRY) {
            return Collections.singletonList(new Cursor(binaryCursor, right));
        } else if (p == EXIT) {
            return Collections.singletonList(binaryCursor);
        } else if (p == right) {
            return Collections.singletonList(new Cursor(binaryCursor, left));
        } else if (p == left) {
            return previousIn(binaryCursor.getParentOrThrow(), binaryCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInAssignment(Cursor assignmentCursor, ProgramPoint p) {
        J.Assignment assignment = assignmentCursor.getValue();
        Expression a = assignment.getAssignment();
        Expression v = assignment.getVariable();

        if (p == EXIT) {
            return Collections.singletonList(assignmentCursor);
        } else if (p == ENTRY) {
            return Collections.singletonList(new Cursor(assignmentCursor, a));
        } else if (p == a) {
            return previousIn(assignmentCursor.getParentOrThrow(), assignmentCursor.getValue());
        } else if (p == v) {
            // Not actually an expression
            return Collections.singletonList(new Cursor(assignmentCursor, a));
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInReturn(Cursor parentCursor, ProgramPoint p) {
        J.Return _return = parentCursor.getValue();
        @Nullable Expression expr = _return.getExpression();
        if (p == EXIT) {
            if (expr == null) {
                return Collections.singletonList(parentCursor);
            } else {
                return Collections.singletonList(new Cursor(parentCursor, expr));
            }
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == expr) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInThrow(Cursor parentCursor, ProgramPoint p) {
        J.Throw _throw = parentCursor.getValue();
        @Nullable Expression expr = _throw.getException();
        if (p == EXIT) {
            if (expr == null) {
                return Collections.singletonList(parentCursor);
            } else {
                return Collections.singletonList(new Cursor(parentCursor, expr));
            }
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == expr) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInTry(Cursor parentCursor, ProgramPoint p) {
        J.Try _try = parentCursor.getValue();

        // try(ressources?) body catches finally?
        @Nullable List<J.Try.Resource> ressources = _try.getResources();
        J.Block body = _try.getBody();
        List<J.Try.Catch> catches = _try.getCatches();
        J.@Nullable Block _finally = _try.getFinally();

        if (ressources != null) {
            throw new UnsupportedOperationException("TODO");
        }

        if (p == EXIT) {
            if (_finally != null) {
                return previousIn(new Cursor(parentCursor, _finally), EXIT);
            } else {
                List<Cursor> result = new ArrayList<>();
                result.addAll(previousIn(new Cursor(parentCursor, body), EXIT));
                for (J.Try.Catch _catch : catches) {
                    result.addAll(previousIn(new Cursor(parentCursor, _catch), EXIT));
                }
                return result;
            }
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == _finally) {
            List<Cursor> result = new ArrayList<>();
            result.addAll(previousIn(new Cursor(parentCursor, body), EXIT));
            for (J.Try.Catch _catch : catches) {
                result.addAll(previousIn(new Cursor(parentCursor, _catch), EXIT));
            }
            return result;
        } else if (p == body) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else {
            for (J.Try.Catch _catch : catches) {
                if (p == _catch) {
                    return nonLocalExitsBackward.get(p);
                }
            }
        }
        throw new IllegalStateException();
    }


    public List<Cursor> previousInTryCatch(Cursor parentCursor, ProgramPoint p) {
        J.Try.Catch _catch = parentCursor.getValue();

        // catch(parameter) body
        J.ControlParentheses<J.VariableDeclarations> parameter = _catch.getParameter();
        J.Block body = _catch.getBody();

        if (p == EXIT) {
            return previousIn(new Cursor(parentCursor, body), EXIT);
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        } else if (p == body) {
            return Collections.singletonList(new Cursor(parentCursor, parameter));
        } else if (p == parameter) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public List<Cursor> previousInTerminalNode(Cursor parentCursor, ProgramPoint p) {
        if (p == EXIT) {
            return Collections.singletonList(parentCursor);
        } else if (p == ENTRY) {
            return previousIn(parentCursor.getParentOrThrow(), parentCursor.getValue());
        }
        throw new IllegalStateException();
    }

    public String print(Cursor c) {
        if (c.getValue() instanceof ProgramPoint) {
            ProgramPoint p = c.getValue();
            return p.printPP(c).replace("\n", " ").replaceAll("[ ]+", " ").trim();
        } else if (c.getValue() instanceof Collection) {
            return ((Collection<?>) c.getValue()).stream().map(e -> print(new Cursor(c, e))).collect(Collectors.joining("; "));
        } else {
            throw new IllegalStateException();
        }
    }

    // Poor man's next(), to be replaced by the equivalent of the previousXXX() methods
    public List<Cursor> next(Cursor cursor) {
        List<Cursor> r1 = new ArrayList<>();
        for (Cursor k : previousMap.keySet()) {
            List<Cursor> v = previousMap.get(k);
            if (v.contains(cursor)) {
                r1.add(k);
            }
        }
        List<Cursor> r2 = new ArrayList<>();
        for (int i = 0; i < r1.size(); i++) {
            Cursor p = r1.get(i);
            boolean add = true;
            for (int j = i + 1; j < r1.size(); j++) {
                Cursor q = r1.get(j);
                if (p == q) {
                    add = false;
                }
                if (p.isScopeInPath(q.getValue())) {
                    add = false;
                } else if (q.isScopeInPath(p.getValue())) {
                    r1.remove(q);
                }
            }
            if (add) {
                r2.add(p);
            }
        }
        return r2;
    }

    class PreviousMapVisitor extends JavaIsoVisitor<Integer> {

        private void process(ProgramPoint pp) {
            Cursor c = getCursor();
            List<Cursor> p = previousIn(c, ENTRY);
            DataFlowGraph.this.previousMap.put(c, p);
        }

        @Override
        public Statement visitStatement(Statement statement, Integer p) {
            super.visitStatement(statement, p);
            process(statement);
            return statement;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            super.visitVariable(variable, p);
            process(variable);
            return variable;
        }

        @Override
        public Expression visitExpression(Expression expression, Integer p) {
            super.visitExpression(expression, p);
            process(expression);
            return expression;
        }
    }

    class NonLocalExitsVisitor extends JavaIsoVisitor<Integer> {

        @Override
        public Statement visitStatement(Statement statement, Integer p) {
            if (statement instanceof J.Return) {
                Cursor target = getCursor().dropParentUntil(v -> v instanceof J.MethodDeclaration);
                addNonLocalExit(getCursor(), target);
            } else if (statement instanceof J.Throw) {
                Cursor target = target((J.Throw) statement);
                addNonLocalExit(getCursor(), target);
            } else if (statement instanceof J.Break) {
                // FIXME handle break?
            } else if (statement instanceof J.Continue) {
                // FIXME handle continue?
            }
            return super.visitStatement(statement, p);
        }

        @Override
        public Expression visitExpression(Expression expr, Integer p) {
            return super.visitExpression(expr, p);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            return super.visitVariable(variable, p);
        }

        private Cursor target(J.Throw statement) {
            Expression expr = statement.getException();
            JavaType thrownType = expr.getType();

            Cursor c = getCursor();
            while ((c = c.dropParentUntil(v -> v instanceof J.Try || v instanceof J.MethodDeclaration)) != null) {
                if (c.getValue() instanceof J.MethodDeclaration) {
                    return c;
                }
                J.Try aTry = c.getValue();
                // does the try block catch exception ?
                for (J.Try.Catch aCatch : aTry.getCatches()) {
                    assert aCatch.getParameter().getTree().getVariables().size() == 1;
//                    J.VariableDeclarations.NamedVariable v = _catch.getParameter().getTree().getVariables().get(0);
//                    JavaType caughtType = v.getVariableType().getType();
                    JavaType caughtType = aCatch.getParameter().getTree().getTypeExpression().getType();  //getVariables().get(0);
                    if (TypeUtils.isAssignableTo(caughtType, thrownType)) {
                        return new Cursor(getCursor(), aCatch);
                    }
                }
                // if not, try the enclosing try blocks or method declaration
            }
            throw new IllegalStateException();
        }

        private void addNonLocalExit(Cursor from, Cursor to) {
            nonLocalExitsForward.put(from.getValue(), to);
            ArrayList<Cursor> l = nonLocalExitsBackward.get(to.getValue());
            if (l == null) {
                l = new ArrayList<>();
                ProgramPoint value = to.getValue();
                nonLocalExitsBackward.put(value, l);
            }
            l.add(from);
        }
    }
}
