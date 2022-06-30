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

import static org.openrewrite.java.dataflow2.examples.ZipSlipValue.UNKNOWN;

@Incubating(since = "7.25.0")
public class ZipSlip extends DataFlowAnalysis<ZipSlipValue> {

    public ZipSlip(DataFlowGraph dfg) {
        super(dfg, ZipSlipValue.JOINER);
    }


    @Override
    public ProgramState<ZipSlipValue> transferToIfThenElseBranches(J.If ifThenElse, ProgramState<ZipSlipValue> state, String ifThenElseBranch) {
        Expression cond = ifThenElse.getIfCondition().getTree();
        // look for file.toPath().startsWith(dir.toPath()) or its negation in the condition

        boolean negate = false;
        if(cond instanceof J.Unary) {
            J.Unary unary = (J.Unary)cond;
            if(unary.getOperator() == J.Unary.Type.Not) {
                cond = unary.getExpression();
                negate = true;
            }
        }
        if(cond instanceof J.MethodInvocation) {
            J.MethodInvocation startsWithInvocation = (J.MethodInvocation)cond;
            MethodMatcher startsWithMatcher = new MethodMatcher("java.nio.file.Path startsWith(java.nio.file.Path)");
            if(startsWithMatcher.matches(startsWithInvocation)) {
                Expression startsWithSelect = startsWithInvocation.getSelect();
                if(startsWithSelect instanceof J.MethodInvocation) {
                    J.MethodInvocation selectToPathInvocation = (J.MethodInvocation)startsWithSelect;
                    MethodMatcher toPathMatcher = new MethodMatcher("java.io.File toPath()");
                    if(toPathMatcher.matches(selectToPathInvocation)) {
                        Expression toPathSelect = selectToPathInvocation.getSelect();
                        if(toPathSelect instanceof J.Identifier) {
                            JavaType.Variable file = ((J.Identifier) toPathSelect).getFieldType();
                            Expression arg = startsWithInvocation.getArguments().get(0);
                            if(arg instanceof J.MethodInvocation) {
                                J.MethodInvocation argToPathInvocation = (J.MethodInvocation)arg;
                                if(toPathMatcher.matches(argToPathInvocation)) {
                                    Expression dir = argToPathInvocation.getSelect();
                                    ZipSlipValue v = state.get(file);
                                    if(v instanceof ZipSlipValue.NewFileFromZipEntry
                                            && ZipSlipValue.equal(((ZipSlipValue.NewFileFromZipEntry)v).dir, dir)) {
                                        // found file.toPath().startsWith(dir.toPath())
                                        // with state(file) = isBuiltFrom(dir)
                                        if(ifThenElseBranch == "then" ^ negate) {
                                            state = state.set(file, ZipSlipValue.SAFE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return state;
    }


//    private static final String[] definitelyNonNullReturningMethodSignatures = new String[] {
//            "java.lang.String toUpperCase()"
//    };
//
//    private static final List<MethodMatcher> definitelyNonNullReturningMethodMatchers =
//            Arrays.stream(definitelyNonNullReturningMethodSignatures).map(MethodMatcher::new).collect(Collectors.toList());

    @Override
    public ProgramState<ZipSlipValue> transferNewClass(Cursor c, ProgramState<ZipSlipValue> inputState, TraversalControl<ProgramState<ZipSlipValue>> t) {
        J.NewClass newClass = c.getValue();
        // new File(dir, fileName)
        MethodMatcher m = new MethodMatcher("java.io.File <constructor>(java.io.File, java.lang.String)");
        if(m.matches(newClass)) {
            Expression dir = newClass.getArguments().get(0);
            Expression fileName = newClass.getArguments().get(1);
            ZipSlipValue fileNameValue = inputState.expr(0); //analysis(fileName).expr();
            //ProgramState<ZipSlipValue> s = inputState(c, t);
            if(fileNameValue == ZipSlipValue.ZIP_ENTRY_NAME) {
                // fileName has been obtained from ZipEntry.getName()
                int d = stackSizeBefore(newClass);
                ProgramState<ZipSlipValue> result = inputState.pop(d);
                return result.push(new ZipSlipValue.NewFileFromZipEntry(dir));
            }
        }
        return super.transferNewClass(c, inputState, t);
    }

    @Override
    public ProgramState<ZipSlipValue> transferMethodInvocation(Cursor c, ProgramState<ZipSlipValue> inputState, TraversalControl<ProgramState<ZipSlipValue>> t) {
        J.MethodInvocation methodInvocation = c.getValue();
        // zipEntry.getName()

        MethodMatcher m = new MethodMatcher("java.util.zip.ZipEntry getName()");
        if(m.matches(methodInvocation)) {
            int d = stackSizeBefore(methodInvocation);
            ProgramState<ZipSlipValue> result = inputState.pop(d);
            return result.push(ZipSlipValue.ZIP_ENTRY_NAME);
        }
        return super.transferMethodInvocation(c, inputState, t);
    }

    @Override
    public ProgramState<ZipSlipValue> transferLiteral(Cursor c, ProgramState<ZipSlipValue> inputState, TraversalControl<ProgramState<ZipSlipValue>> t) {
        J.Literal literal = c.getValue();
        if(true) {
            // TODO Check if string literal is safe
            return inputState.push(ZipSlipValue.SAFE);
        }
        return super.transferLiteral(c, inputState, t);
    }
}

