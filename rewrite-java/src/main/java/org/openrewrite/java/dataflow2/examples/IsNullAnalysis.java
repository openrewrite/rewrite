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
package org.openrewrite.java.dataflow2.examples;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow2.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.java.dataflow2.examples.ModalBoolean.*;

@Incubating(since = "7.25.0")
public class IsNullAnalysis extends DataFlowAnalysis<ModalBoolean> {

    public IsNullAnalysis(DataFlowGraph dfg) {
        super(dfg, ModalBoolean.JOINER);
    }

    //private static final Joiner<ModalBoolean> JOINER = ModalBoolean.JOINER;

    /**
     * @return Whether the variable v is known to be null before given program point.
     */
    public ModalBoolean isNullBefore(Cursor programPoint, JavaType.Variable v)
    {
        ProgramState<ModalBoolean> state = inputState(programPoint, new TraversalControl<>());
        ModalBoolean result = state.get(v);
        return result;
    }

    @Override
    public ProgramState<ModalBoolean> defaultTransfer(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        return inputState;
    }

    @Override
    public ProgramState<ModalBoolean> transferBinary(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        return inputState.pop(2).push(False);
    }

//    @Override
//    public ProgramState<ModalBoolean> transferNamedVariable(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> tc) {
//        J.VariableDeclarations.NamedVariable v = c.getValue();
//        JavaType.Variable t = v.getVariableType();
//        if(v.getInitializer() != null) {
//            //ProgramState<ModalBoolean> s = analysis(v.getInitializer());
//            //ModalBoolean e = inputState.expr();
//            return inputState.set(t, inputState.expr());
//        } else {
//            assert !inputState.getMap().containsKey(t);
//            return inputState.set(t, True);
//        }
//    }

    @Override
    public ProgramState<ModalBoolean> transferAssignment(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {

        J.Assignment a = c.getValue();
        if (a.getVariable() instanceof J.Identifier) {
            J.Identifier ident = (J.Identifier) a.getVariable();
            ProgramState<ModalBoolean> s = inputState; //analysis(a.getAssignment());
            return s.set(ident.getFieldType(), s.expr()); // .pop().push(s.expr());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static final String[] definitelyNonNullReturningMethodSignatures = new String[] {
        "java.lang.String toUpperCase()"
    };

    private static final List<MethodMatcher> definitelyNonNullReturningMethodMatchers =
            Arrays.stream(definitelyNonNullReturningMethodSignatures).map(MethodMatcher::new).collect(Collectors.toList());

    @Override
    public ProgramState<ModalBoolean> transferMethodInvocation(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        J.MethodInvocation method = c.getValue();
        List<Expression> args = method.getArguments();
        ProgramPoint select = method.getSelect();

        int d = stackSizeBefore(method);
        ProgramState<ModalBoolean> result = inputState.pop(d);
//        if(args.size() == 1 && args.get(0) instanceof J.Empty) {
//                result = result.pop();
//        } else {
//            result = result.pop(args.size());
//        }
//        if(select != null) {
//            result = result.pop();
//        }

        for(MethodMatcher matcher : definitelyNonNullReturningMethodMatchers) {
            if (matcher.matches(method)) {
                return result.push(False);
            }
        }
        return result.push(NoIdea);
    }

    @Override
    public ProgramState<ModalBoolean> transferLiteral(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        J.Literal pp = c.getValue();
        //ProgramState<ModalBoolean> s = inputState(c, t);
        if (pp.getValue() == null) {
            return inputState.push(True);
        } else {
            return inputState.push(False);
        }
    }

    @Override
    public ProgramState<ModalBoolean> transferIdentifier(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        J.Identifier i = c.getValue();
        //ProgramState<ModalBoolean> s = inputState(c, t);
        ModalBoolean v = inputState.get(i.getFieldType());
        return inputState.push(v);
    }

    @Override
    public ProgramState<ModalBoolean> transferIfElse(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        J.If.Else ifElse = c.getValue();
        //ProgramPoint body = ifElse.getBody();
        //return analysis(body);
        return inputState;
    }

    @Override
    public ProgramState<ModalBoolean> transferBlock(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        return inputState;
//        J.Block block = c.getValue();
//        List<Statement> stmts = block.getStatements();
//        if (stmts.size() > 0) {
//            ProgramPoint stmt = stmts.get(stmts.size() - 1);
//            return analysis(stmt);
//        } else {
//            throw new UnsupportedOperationException(); // TODO
//        }
    }

    @Override
    public ProgramState<ModalBoolean> transferParentheses(Cursor c, ProgramState<ModalBoolean> inputState, TraversalControl<ProgramState<ModalBoolean>> t) {
        return inputState;
//        J.Parentheses<?> paren = c.getValue();
//        ProgramPoint tree = (ProgramPoint) paren.getTree();
//        return analysis(tree);
    }

    @Override
    public ProgramState transferToIfThenElseBranches(J.If ifThenElse, ProgramState s, String ifThenElseBranch) {
        Expression cond = ifThenElse.getIfCondition().getTree();
        if(cond instanceof J.Binary) {
            J.Binary binary = (J.Binary)cond;
            if(binary.getOperator() == J.Binary.Type.Equal) {
                if(binary.getLeft() instanceof J.Identifier) {
                    J.Identifier left = (J.Identifier) binary.getLeft();
                    if (binary.getRight() instanceof J.Literal) {
                        // condition has the form 's == literal'
                        boolean isNull = ((J.Literal) binary.getRight()).getValue() == null;
                        if(ifThenElseBranch.equals("then")) {
                            // in the 'then' branch
                            s = s.set(left.getFieldType(), isNull ? True : False);
                        } else {
                            // in the 'else' branch or the 'exit' branch
                            s = s.set(left.getFieldType(), isNull ? False : True);
                        }
                    }
                }
            }
        }
        return s;
    }

}

