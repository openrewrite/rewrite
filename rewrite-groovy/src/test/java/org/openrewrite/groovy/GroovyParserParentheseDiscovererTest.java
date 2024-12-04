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
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ReflectionUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GroovyParserParentheseDiscovererTest {

    @Test
    void simpleInvoke() {
        //language=groovy
        String input = "(a.invoke())";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(1, result);
    }

    @Test
    void getInsideParenthesesLevelForMethodCalls4() {
        //language=groovy
        String input = "(something?.'equals' \"s\" )";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(1, result);
    }

    @Test
    void getInsideParenthesesLevelForMethodCalls1() {
        //language=groovy
        String input = "((((((someMap.get(\"baz\"))))).equals(\"baz\")))";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(2, result);
    }

    @Test
    void getInsideParenthesesLevelForMethodCalls2() {
        //language=groovy
        String input = "((((someMap.get(\"(bar\")))))";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(4, result);
    }

    @Test
    void getInsideParenthesesLevelForMethodCalls3() {
        //language=groovy
        String input = "((someMap.containsKey(\"foo\")) && ((someMap.get(\"foo\")).'equals' \"\"\"bar\n" +
          "\"\"\" ))";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(1, result);
    }

    @Test
    void methodInvocationWithArgumentParentheses() {
        //language=groovy
        String input = "(((((obj))).'equals'((\"baz\"))))";
        int result = getInsideParenthesesLevelForMethodCalls(input);

        assertEquals(2, result);
    }

    @SneakyThrows
    private static int getInsideParenthesesLevelForMethodCalls(String input) {
        Method method = ReflectionUtils.findMethod(GroovyParserParentheseDiscoverer.class, "getInsideParenthesesLevelForMethodCalls", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(null, input);
    }
}
