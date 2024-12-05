/*
 * Copyright 2024 the original author or authors.
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

import lombok.SneakyThrows;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.junit.jupiter.api.Test;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroovyParserParentheseDiscovererTest {

    @Test
    void invoke() {
        MethodCallExpression node = MockMethodCallExpression.of("invoke");

        //language=groovy
        String input = "(a.invoke())";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(1, result);
    }

    @Test
    void invokeWithArguments() {
        MethodCallExpression node = MockMethodCallExpression.of("invoke");

        //language=groovy
        String input = "(a.invoke(\"A\", \"\\$\", \"C?)\"))";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(1, result);
    }

    @Test
    void invokeWithSpacesInParenthesis() {
        MethodCallExpression node = MockMethodCallExpression.of("invoke");

        //language=groovy
        String input = "( ( (((a.invoke()) ) ) ) )";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(5, result);
    }

    @Test
    void invokeWithSpacesInParenthesis2() {
        MethodCallExpression node = MockMethodCallExpression.of("equals");

        //language=groovy
        String input = "(((((((someMap.get(\"(bar\"))))).equals(\"baz\") )   ) )";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(3, result);
    }

    @Test
    void invokeWithNullSafeAndInQuotesAndArgumentsWithoutParenthesis() {
        MethodCallExpression node = MockMethodCallExpression.of("invoke");

        //language=groovy
        String input = "(something?.'invoke' \"s\" \"a\" )";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(1, result);
    }

    @Test
    void multipleParenthesesLeftBiggerThanRight() {
        MethodCallExpression node = MockMethodCallExpression.of("equals");

        //language=groovy
        String input = "((((((someMap.get(\"baz\"))))).equals(\"baz\")))";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(2, result);
    }

    @Test
    void multipleParenthesesRightBiggerThanLeft() {
        MethodCallExpression node = MockMethodCallExpression.of("get");

        //language=groovy
        String input = "((((someMap.get(\"(bar\")))))";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(4, result);
    }

    @Test
    void binaryWithLinebreakInArgument() {
        MethodCallExpression node = MockMethodCallExpression.of("equals");

        //language=groovy
        String input = "((someMap.containsKey(\"foo\")) && ((someMap.get(\"foo\")).'equals' \"\"\"bar\n" +
          "\"\"\" ))";
        Integer result = GroovyParserParentheseDiscoverer.getInsideParenthesesLevel(node, input);

        assertEquals(1, result);
    }

    private static class MockMethodCallExpression extends MethodCallExpression {
        String method = "";

        @SneakyThrows
        private static MockMethodCallExpression of(String method) {
            Constructor<?> constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(MockMethodCallExpression.class, Object.class.getConstructor());
            MockMethodCallExpression expression = (MockMethodCallExpression) constructor.newInstance();
            expression.method = method;
            return expression;
        }

        public MockMethodCallExpression(Expression objectExpression, String method, Expression arguments) {
            super(null, method, null);
        }

        @Override
        public String getMethodAsString() {
            return method;
        }
    }
}
