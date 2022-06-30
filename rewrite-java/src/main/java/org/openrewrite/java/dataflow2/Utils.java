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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    /**
     * @return The first program point in given compilation unit whose print representation is equal to `ppToFind`.
     */
    public static Cursor findProgramPoint(J.CompilationUnit cu, String ppToFind) {
        Visitor visitor = new Visitor(ppToFind);
        visitor.visit(cu, 0);
        return visitor.result;
    }

    public static JavaType.Variable findVariable(J.CompilationUnit cu, String variableToFind) {
        FindVariableVisitor visitor = new FindVariableVisitor(variableToFind);
        visitor.visit(cu, 0);
        return visitor.result;
    }

    static class Visitor extends JavaIsoVisitor<Integer> {

        final String ppToFind;
        Cursor result;


        public Visitor(String ppToFind) {
            this.ppToFind = ppToFind;
        }

        @Override
        public Statement visitStatement(Statement statement, Integer p) {
            super.visitStatement(statement, p);
            if (result == null && ppToFind.equals(print(statement, getCursor()))) {
                result = getCursor();
            }
            return statement;
        }

        @Override
        public Expression visitExpression(Expression expression, Integer p) {
            super.visitExpression(expression, p);
            if (result == null && ppToFind.equals(print(expression, getCursor()))) {
                result = getCursor();
            }
            return expression;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            super.visitVariable(variable, p);
            if (result == null && ppToFind.equals(print(variable, getCursor()))) {
                result = getCursor();
            }
            return variable;
        }

        @Override
        public J.If.Else visitElse(J.If.Else elze, Integer p) {
            super.visitElse(elze, p);
            if (result == null && ppToFind.equals(print(elze, getCursor()))) {
                result = getCursor();
            }
            return elze;
        }
    }

    static class FindVariableVisitor extends JavaIsoVisitor<Integer> {

        final String variableToFind;
        JavaType.Variable result;


        public FindVariableVisitor(String variableToFind) {
            this.variableToFind = variableToFind;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
            super.visitVariable(variable, p);
            if (result == null && variableToFind.equals(variable.getName().getSimpleName())) {
                result = variable.getVariableType();
            }
            return variable;
        }
    }

    public static String print(Cursor c) {
        if(c.getValue() instanceof ProgramPoint) {
            ProgramPoint p = c.getValue();
            return print(p,c);
        } else if(c.getValue() instanceof List) {
            List<ProgramPoint> l = c.getValue();
            return "[" + l.stream().map(e -> print(e, c)).collect(Collectors.joining(", ")) + "]";
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static String print(ProgramPoint p, Cursor c) {
        return ((J) p).print(c).replace("\n", " ").replaceAll("[ ]+", " ").trim();
    }

    public static String print(ProgramPoint p) {
        return ((J) p).print().replace("\n", " ").replaceAll("[ ]+", " ").trim();
    }
}
