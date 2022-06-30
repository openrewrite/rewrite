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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.java.dataflow2.examples.ConstantPropagationValue.*;
import static org.openrewrite.java.dataflow2.examples.ConstantPropagationValue.Understanding.Known;
import static org.openrewrite.java.dataflow2.examples.ConstantPropagationValue.Understanding.Unknown;

@Incubating(since = "7.25.0")
public class ConstantPropagation extends DataFlowAnalysis<ConstantPropagationValue> {

    public ConstantPropagation(DataFlowGraph dfg) {
        super(dfg, ConstantPropagationValue.JOINER);
    }

    public ConstantPropagationValue value(Cursor programPoint, JavaType.Variable v)
    {
        ProgramState<ConstantPropagationValue> state = inputState(programPoint, new TraversalControl<>());
        ConstantPropagationValue result = state.get(v);
        return result;
    }

    @Override
    public ProgramState transferToIfThenElseBranches(J.If ifThenElse, ProgramState s, String ifThenElseBranch) {
//        Expression cond = ifThenElse.getIfCondition().getTree();
//        if(cond instanceof J.Binary) {
//            J.Binary binary = (J.Binary)cond;
//            if(binary.getOperator() == J.Binary.Type.Equal) {
//                if(binary.getLeft() instanceof J.Identifier) {
//                    J.Identifier left = (J.Identifier) binary.getLeft();
//                    if (binary.getRight() instanceof J.Literal) {
//                        // condition has the form 's == literal'
//                        boolean isNull = ((J.Literal) binary.getRight()).getValue() == null;
//                        if(ifThenElseBranch.equals("then")) {
//                            // in the 'then' branch
//                            s = s.set(left.getFieldType(), isNull ? True : False);
//                        } else {
//                            // in the 'else' branch or the 'exit' branch
//                            s = s.set(left.getFieldType(), isNull ? False : True);
//                        }
//                    }
//                }
//            }
//        }
        return s;
    }

    @Override
    public ProgramState<ConstantPropagationValue> defaultTransfer(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        return inputState;
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferBinary(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        J.Binary binary = c.getValue();
        ConstantPropagationValue cp0 = inputState.expr(1);
        ConstantPropagationValue cp1 = inputState.expr(0);

        inputState = inputState.pop().pop();

        if(cp0 == null || cp1 == null) {
            return inputState.push(CONFLICT);
        }

        if(cp0.getUnderstanding() == Known && cp1.getUnderstanding() == Known) {
            if(binary.getOperator() == J.Binary.Type.Addition) {
                if(cp0.getValue() instanceof Integer && cp1.getValue() instanceof Integer) {
                    Integer result = ((Integer)cp0.getValue()) + ((Integer)cp1.getValue());
                    return inputState.push(new ConstantPropagationValue(Known, result));
                } else if(cp0.getValue() instanceof String && cp1.getValue() instanceof String) {
                    String result = ((String)cp0.getValue()) + ((String)cp1.getValue());
                    return inputState.push(new ConstantPropagationValue(Known, result));
                } else {
                    return inputState.push(CONFLICT);
                }
            } else {
                return inputState.push(CONFLICT);
            }
        } else if(cp0.getUnderstanding() == Unknown || cp1.getUnderstanding() == Unknown) {
            return inputState.push(UNKNOWN);
        } else {
            return inputState.push(CONFLICT);
        }
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferNamedVariable(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> tc) {
        J.VariableDeclarations.NamedVariable v = c.getValue();
        JavaType.Variable t = v.getVariableType();
        if(v.getInitializer() != null) {
            //ProgramState<ConstantPropagationValue> s = analysis(v.getInitializer());
            //ConstantPropagationValue e = inputState.expr();
            return inputState.set(t, inputState.expr());
        } else {
            assert !inputState.getMap().containsKey(t);
            return inputState.set(t, null);
        }
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferAssignment(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {

        J.Assignment a = c.getValue();
        if (a.getVariable() instanceof J.Identifier) {
            J.Identifier ident = (J.Identifier) a.getVariable();
            ProgramState<ConstantPropagationValue> s = inputState; //analysis(a.getAssignment());
            return s.set(ident.getFieldType(), s.expr()).push(s.expr());
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
    public ProgramState<ConstantPropagationValue> transferMethodInvocation(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
//        J.MethodInvocation method = c.getValue();
//        for(MethodMatcher matcher : definitelyNonNullReturningMethodMatchers) {
//            if (matcher.matches(method)) {
//                return inputState.push(False);
//            }
//        }
        return inputState.push(UNKNOWN);
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferLiteral(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        J.Literal pp = c.getValue();
        //ProgramState<ConstantPropagationValue> s = inputState(c, t);
        if (pp.getType() == JavaType.Primitive.String) {
            String value = pp.getValueSource();
            value = value.substring(1, value.length() - 1).replaceAll("\\\'", "'");
            return inputState.push(new ConstantPropagationValue(Known, value));
        } else if (pp.getType() == JavaType.Primitive.Int) {
            Integer value = Integer.parseInt(pp.getValueSource());
            return inputState.push(new ConstantPropagationValue(Known, value));
        } else {
            return inputState.push(UNKNOWN);
        }
    }

    public static String unquote(String value) {
        return value;
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferIdentifier(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        J.Identifier i = c.getValue();
        //ProgramState<ConstantPropagationValue> s = inputState(c, t);
        ConstantPropagationValue v = inputState.get(i.getFieldType());
        return inputState.push(v);
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferIfElse(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        J.If.Else ifElse = c.getValue();
        //ProgramPoint body = ifElse.getBody();
        //return analysis(body);
        return inputState;
    }

    @Override
    public ProgramState<ConstantPropagationValue> transferBlock(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
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
    public ProgramState<ConstantPropagationValue> transferParentheses(Cursor c, ProgramState<ConstantPropagationValue> inputState, TraversalControl<ProgramState<ConstantPropagationValue>> t) {
        return inputState;
//        J.Parentheses<?> paren = c.getValue();
//        ProgramPoint tree = (ProgramPoint) paren.getTree();
//        return analysis(tree);
    }
}

