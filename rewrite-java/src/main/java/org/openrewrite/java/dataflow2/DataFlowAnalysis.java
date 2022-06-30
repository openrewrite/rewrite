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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.java.dataflow2.ProgramPoint.EXIT;

@Incubating(since = "7.25.0")
public abstract class DataFlowAnalysis<T> {

    protected final Joiner<T> joiner;

    private final DataFlowGraph dfg;

    // The state AFTER given program point
    private final Map<ProgramPoint, ProgramState<T>> analysisMap = new HashMap<>();

    // In the worklist, we only consider nodes of interest, namely we skip the nodes
    // that have exactly 1 previous nodes. Such linear chains are handled in inputState().
    private final Set<ProgramPoint> nodesOfInterest = new HashSet<>();

    // Keep a Curosr for each node of interest.
    private final Map<ProgramPoint, Cursor> cursors = new HashMap<>();

    // Track dependencies between nodes of interest
    private final Map<ProgramPoint, List<ProgramPoint>> nexts = new HashMap<>();

    private final Deque<Cursor> workList = new ArrayDeque<>();

    public DataFlowAnalysis(DataFlowGraph dfg, Joiner<T> joiner) {
        this.dfg = dfg;
        this.joiner = joiner;
    }

    public void performAnalysis(Cursor from) {

        initNexts(from);

        Set<ProgramPoint> visited = new HashSet<>();
        initWorkList(from, visited);
        assert visited.equals(nodesOfInterest);

        while (!workList.isEmpty()) {

            System.out.println(workList.size());

            Cursor c = workList.remove();
            ProgramPoint pp = c.getValue();

            System.out.println(pp.getClass().getSimpleName() + "   " + Utils.print(c));

            ProgramState<T> previousState = analysisMap.get(pp);

            ProgramState<T> newState = transferLinear(c);

            if (previousState == null) {
                analysisMap.put(pp, newState);
                // no need to add the nexts since they're necessarily already in the worklist
            } else if (!previousState.equals(newState)) {
                List<ProgramPoint> nn = nexts.get(pp);
                if (nn != null) {
                    for (ProgramPoint p : nn) {
                        Cursor next = cursors.get(p);
                        assert nodesOfInterest.contains(next);
                        workList.add(next);
                    }
                }
            }
        }
    }

    private void initNexts(Cursor c) {
        ProgramPoint to = c.getValue();
        if (nodesOfInterest.contains(to)) return;
        nodesOfInterest.add(to);
        cursors.put(to, c);

        List<Cursor> sources = dfg.previous(c);

        // This loop must terminate because every node is reachable from the entry node.
        while (sources.size() == 1) {
            sources = dfg.previous(sources.get(0));
        }

        List<Cursor> primitivesSources = sources.stream().flatMap(s -> dfg.previousIn(s, EXIT).stream()).collect(Collectors.toList());

        for (Cursor source : primitivesSources) {
            ProgramPoint from = source.getValue();
            // There is a DFG edge from->to
            // The analysis at 'to' depends on the analysis at 'from'
            nexts.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            initNexts(source);
        }
    }

    private void initWorkList(Cursor c, Set<ProgramPoint> visited) {
        ProgramPoint to = c.getValue();
        if (visited.contains(to)) return;
        visited.add(to);
        workList.addFirst(c);

        List<Cursor> sources = dfg.previous(c);

        while (sources.size() == 1) {
            sources = dfg.previous(sources.get(0));
        }

        List<Cursor> primitivesSources = sources.stream().flatMap(s -> dfg.previousIn(s, EXIT).stream()).collect(Collectors.toList());

        for (Cursor source : primitivesSources) {
            ProgramPoint from = source.getValue();
            initWorkList(source, visited);
        }
    }

    private ProgramState<T> analysisMapGetOrCreate(ProgramPoint p) {
        ProgramState<T> res = analysisMap.get(p);
        if (res == null) {
            // it is a mistake to ask the analysis for a node not of interest
            assert nodesOfInterest.contains(p);

            res = new ProgramState<>(joiner.lowerBound());
            analysisMap.put(p, res);
        }
        return res;
    }

    private ProgramState<T> analysisMapGetOrCreate(Cursor c) {
        return analysisMapGetOrCreate((ProgramPoint) c.getValue());
    }

    public ProgramState<T> getStateAfter(ProgramPoint p) {
        return analysisMapGetOrCreate(p);
    }

    public ProgramState<T> getStateAfter(Cursor c) {
        return analysisMapGetOrCreate(c);
    }


    protected ProgramState<T> inputState(Cursor c, TraversalControl<ProgramState<T>> t) {
        List<Cursor> sources = dfg.previous(c);
        return inputState(sources, t);
    }

    private ProgramState<T> inputState(List<Cursor> sources, TraversalControl<ProgramState<T>> t) {

//        if("s = null".equals(Utils.print(c))) {
//            System.out.println();
//        }

        List<ProgramState<T>> outs = new ArrayList<>();
        for (Cursor source : sources) {
            // Since program points are represented by cursors with a tree node value,
            // it is impossible to add program points when there is no corresponding tree node.
            // To work around this limitation, we use cursor messages to express that a given
            // edge goes through a virtual program point.

            if (source.getMessage("ifThenElseBranch") != null) {
                J.If ifThenElse = source.firstEnclosing(J.If.class);
                ProgramState<T> s1 = analysisMapGetOrCreate(source); // outputState(source, t, pp);
                ProgramState<T> s2 = transferToIfThenElseBranches(ifThenElse, s1, source.getMessage("ifThenElseBranch"));
                outs.add(s2);
            } else {
                outs.add(analysisMapGetOrCreate(source)); // outputState(source, t, pp));
            }
        }
        return join(outs);
    }

//    public ProgramState<T> getStateBefore(Cursor c, TraversalControl<ProgramState<T>> t) {
//        return inputState(c, t);
//    }

    private ProgramState<T> join(List<ProgramState<T>> outs) {
        return ProgramState.join(joiner, outs);
    }

    @SafeVarargs
    private final ProgramState<T> join(ProgramState<T>... outs) {
        return join(Arrays.asList(outs));
    }

    /**
     * Compose all transfers to c from its linear chain of predecessors.
     */
    private ProgramState<T> transferLinear(Cursor c) {

        Cursor previous = c;
        List<Cursor> sources;
        Stack<Cursor> stack = new Stack<>();
        while ((sources = dfg.previous(previous)).size() == 1) {

            //System.out.println("previous(< " + Utils.print(previous) + " >) = < " + Utils.print(sources.get(0)) + " >");

            previous = sources.get(0);
            stack.push(previous);
        }

        List<Cursor> primitivesSources = sources.stream().flatMap(s -> dfg.previousIn(s, EXIT).stream()).collect(Collectors.toList());

        ProgramState<T> inputState = inputState(primitivesSources, null);
        while (!stack.isEmpty()) {
            previous = stack.pop();
            inputState = transfer(previous, inputState, null);
        }

        ProgramState<T> newState = transfer(c, inputState, null);

        System.out.println(inputState + "  ->  " + newState);

        return newState;
    }

    private ProgramState<T> transfer(Cursor pp, ProgramState inputState, TraversalControl<ProgramState<T>> t) {
        ProgramState<T> result = transfer2(pp, inputState, t);
        int before = inputState.stackSize();
        int after = result.stackSize();
        int pop = stackSizeBefore(pp.getValue());
        int push = stackSizeAfter(pp.getValue());
        int d = push - pop;
        if (after - before != d) {
            System.out.println(className(pp) + ": before=" + before + ", after=" + after + ", pop=" + pop + ", push=" + push);
            System.out.println(inputState + "  ->  " + result);
            System.out.println();
            stackSizeBefore(pp.getValue());
            stackSizeAfter(pp.getValue());
            transfer2(pp, inputState, t);
        }
//        if(pp.getParent().getValue() instanceof  J.Block) {
//            result = result.clearStack();
//        }
        return result;
    }

    private String className(Cursor c) {
        return c.getValue();
    }

    private ProgramState<T> transfer2(Cursor pp, ProgramState inputState, TraversalControl<ProgramState<T>> t) {
        switch (className(pp)) {
            case "J$MethodInvocation":
                return transferMethodInvocation(pp, inputState, t);
            case "J$ArrayAccess":
                return transferArrayAccess(pp, inputState, t);
            case "J$Assert":
                return transferAssert(pp, inputState, t);
            case "J$NewClass":
                return transferNewClass(pp, inputState, t);
            case "J$If":
                return transferIf(pp, inputState, t);
            case "J$If$Else":
                return transferIfElse(pp, inputState, t);
            case "J$WhileLoop":
                return transferWhileLoop(pp, inputState, t);
            case "J$ForLoop":
                return transferForLoop(pp, inputState, t);
            case "J$ForLoop$Control":
                return transferForLoopControl(pp, inputState, t);
            case "J$Block":
                return transferBlock(pp, inputState, t);
            case "J$VariableDeclarations":
                return transferVariableDeclarations(pp, inputState, t);
            case "J$VariableDeclarations$NamedVariable":
                return transferNamedVariable(pp, inputState, t);
            case "J$Unary":
                return transferUnary(pp, inputState, t);
            case "J$Binary":
                return transferBinary(pp, inputState, t);
            case "J$Assignment":
                return transferAssignment(pp, inputState, t);
            case "J$Parentheses":
                return transferParentheses(pp, inputState, t);
            case "J$ControlParentheses":
                return transferControlParentheses(pp, inputState, t);
            case "J$Literal":
                return transferLiteral(pp, inputState, t);
            case "J$Identifier":
                return transferIdentifier(pp, inputState, t);
            case "J$Empty":
                return transferEmpty(pp, inputState, t);
            case "J$FieldAccess":
                return transferFieldAccess(pp, inputState, t);
            case "J$CompilationUnit":
            case "J$ClassDeclaration":
            case "J$MethodDeclaration":

                // AssignmentOperation x+=1
                // EnumValue
                // EnumValueSet
                // ForeachLoop
                // DoWhileLoop
                // InstanceOf like binary
                // NewArray like new class with dimension
                // Ternary (? :)
                // TypeCast like parenthesis evaluate expression not type
                // MemberReference

                // Switch
                // Case
                // Lambda
                // Break
                // Continue
                // Label
                // Return
                // Throw
                // Try
                // MultiCatch

            default:
                throw new Error("Not implemented: " + pp.getValue().getClass().getName());
        }
    }

    protected final int stackSizeBefore(ProgramPoint pp) {
        if (pp instanceof J.MethodInvocation) {
            J.MethodInvocation invoke = (J.MethodInvocation) pp;
            List<Expression> args = invoke.getArguments();
            return (args.size() == 1 && args.get(0) instanceof J.Empty ? 0 : args.size())
                    + (invoke.getSelect() == null ? 0 : 1);
        } else if (pp instanceof J.Assert) {
            J.Assert assertStmt = (J.Assert) pp;
            return 1 + (assertStmt.getDetail() == null ? 0 : 1);
        } else if (pp instanceof J.NewClass) {
            J.NewClass newClass = (J.NewClass) pp;
            // FIXME what if there is one argument and it is `J.Empty`?
            return newClass.getArguments() == null ? 0 :
                    newClass.getArguments().size();
        } else if (pp instanceof J.If || pp instanceof J.If.Else || pp instanceof J.WhileLoop ||
                pp instanceof J.ForLoop || pp instanceof J.ForLoop.Control || pp instanceof J.Block ||
                pp instanceof J.VariableDeclarations || pp instanceof J.Literal || pp instanceof J.Identifier ||
                pp instanceof J.Empty || pp instanceof J.ClassDeclaration ||
                pp instanceof J.MethodDeclaration) {
            return 0;
        } else if (pp instanceof J.VariableDeclarations.NamedVariable || pp instanceof J.Unary ||
                pp instanceof J.Assignment || pp instanceof J.Parentheses || pp instanceof J.ControlParentheses ||
                pp instanceof J.FieldAccess) {
            return 1;
        } else if (pp instanceof J.Binary || pp instanceof J.ArrayAccess) {
            return 2;
        }
        throw new IllegalArgumentException("Not implemented: " + pp.getClass().getName());
    }

    protected final int stackSizeAfter(ProgramPoint pp) {
        if (pp instanceof J.ClassDeclaration || pp instanceof J.MethodDeclaration || pp instanceof J.Assert ||
                pp instanceof J.If || pp instanceof J.If.Else || pp instanceof J.WhileLoop || pp instanceof J.ForLoop ||
                pp instanceof J.ForLoop.Control || pp instanceof J.Block || pp instanceof J.VariableDeclarations ||
                pp instanceof J.VariableDeclarations.NamedVariable || pp instanceof J.Empty) {
            return 0;
        } else if (pp instanceof J.MethodInvocation || pp instanceof J.ArrayAccess || pp instanceof J.NewClass ||
                pp instanceof J.Unary || pp instanceof J.Binary || pp instanceof J.Assignment ||
                pp instanceof J.Parentheses || pp instanceof J.ControlParentheses || pp instanceof J.Literal ||
                pp instanceof J.Identifier || pp instanceof J.FieldAccess) {
            return 1;
        }
        throw new IllegalArgumentException("Not implemented: " + pp.getClass().getName());
    }

    public ProgramState<T> defaultTransfer(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        throw new UnsupportedOperationException();
    }


    public ProgramState<T> transferArrayAccess(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.ArrayAccess ac = c.getValue();
        // TODO
        return inputState.push(joiner.lowerBound());
    }

    public ProgramState<T> transferAssert(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return inputState.push(joiner.lowerBound());
    }

    public ProgramState<T> transferAssignment(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.Assignment a = c.getValue();
        if (a.getVariable() instanceof J.Identifier) {
            J.Identifier ident = (J.Identifier) a.getVariable();
            return inputState.set(ident.getFieldType(), inputState.expr());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public ProgramState<T> transferBinary(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.pop(2).push(joiner.lowerBound());
    }

    public ProgramState<T> transferBlock(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferControlParentheses(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferEmpty(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferForLoop(Cursor pp, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return defaultTransfer(pp, inputState, t);
    }

    public ProgramState<T> transferForLoopControl(Cursor pp, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return defaultTransfer(pp, inputState, t);
    }

    public ProgramState<T> transferFieldAccess(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        // TODO
        return inputState.push(joiner.lowerBound());
    }

    public ProgramState<T> transferIdentifier(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.Identifier i = c.getValue();
        ProgramState<T> s = inputState;
        T v = s.get(i.getFieldType());
        return s.push(v);
    }

    public ProgramState<T> transferIf(Cursor pp, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferIfElse(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferLiteral(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.push(joiner.lowerBound());
    }

    public ProgramState<T> transferMethodInvocation(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.MethodInvocation method = c.getValue();
        int d = stackSizeBefore(method);
        ProgramState<T> result = inputState.pop(d);
        return result.push(joiner.lowerBound());
    }

    public ProgramState<T> transferNamedVariable(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> tc) {
        J.VariableDeclarations.NamedVariable v = c.getValue();
        JavaType.Variable t = v.getVariableType();
        if (v.getInitializer() != null) {
            //ProgramState s = analysis(v.getInitializer());
            return inputState.set(t, inputState.expr()).pop();
        } else {
            //ProgramState s = inputState(c, tc);
            assert !inputState.getMap().containsKey(t);
            return inputState.set(t, joiner.defaultInitialization()).pop();
        }
    }

    public ProgramState<T> transferNewClass(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        J.NewClass newClass = c.getValue();
        int d = stackSizeBefore(newClass);
        ProgramState<T> result = inputState.pop(d);
        return result.push(joiner.lowerBound());
    }

    public ProgramState<T> transferParentheses(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState;
    }

    public ProgramState<T> transferUnary(Cursor c, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return inputState.pop().push(joiner.lowerBound());
    }

    public ProgramState<T> transferVariableDeclarations(Cursor pp, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return defaultTransfer(pp, inputState, t);
    }

    public ProgramState<T> transferWhileLoop(Cursor pp, ProgramState<T> inputState, TraversalControl<ProgramState<T>> t) {
        return defaultTransfer(pp, inputState, t);
    }

    // Transfer functions for virtual nodes

    public ProgramState<T> transferToIfThenElseBranches(J.If ifThenElse, ProgramState<T> s, String ifThenElseBranch) {
        return s;
    }
}
