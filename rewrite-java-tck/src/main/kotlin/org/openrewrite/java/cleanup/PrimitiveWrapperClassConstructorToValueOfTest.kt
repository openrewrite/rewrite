/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("UnnecessaryBoxing", "deprecation", "BooleanConstructorCall", "ConstantConditions",
    "StringOperationCanBeSimplified", "CachedNumberConstructorCall"
)
interface PrimitiveWrapperClassConstructorToValueOfTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = PrimitiveWrapperClassConstructorToValueOf()


    @Test
    fun integerValueOf(jp: JavaParser) = assertUnchanged(
        before = """
            class A {
                Integer i = Integer.valueOf(1);
                String hello = new String("Hello" + " world " + i);
                Long l = 11L;
            }
        """
    )

    @Test
    fun newClassToValueOf(jp: JavaParser) = assertChanged(
        before = """
            class A {
                Boolean bool = new Boolean(true);
                Byte b = new Byte("1");
                Character c = new Character('c');
                Double d = new Double(1.0);
                Float f = new Float(1.1f);
                Long l = new Long(1);
                Short sh = new Short("12");
                short s3 = 3;
                Short sh3 = new Short(s3);
                Integer i = new Integer(1);
            }
        """,
        after = """
            class A {
                Boolean bool = Boolean.valueOf(true);
                Byte b = Byte.valueOf("1");
                Character c = Character.valueOf('c');
                Double d = Double.valueOf(1.0);
                Float f = Float.valueOf(1.1f);
                Long l = Long.valueOf(1);
                Short sh = Short.valueOf("12");
                short s3 = 3;
                Short sh3 = Short.valueOf(s3);
                Integer i = Integer.valueOf(1);
            }
        """
    )

    @Test
    fun newIntegerToValueOfValueRef(jp: JavaParser) = assertChanged(
        before = """
            class A {
                boolean fls = true;
                Boolean b2 = new Boolean(fls);
                char ch = 'c';
                Character c = new Character(ch);
                double d1 = 1.1;
                Double d = new Double(d1);
                int k = 1;
                Integer k2 = new Integer(k);
            }
        """,
        after = """
            class A {
                boolean fls = true;
                Boolean b2 = Boolean.valueOf(fls);
                char ch = 'c';
                Character c = Character.valueOf(ch);
                double d1 = 1.1;
                Double d = Double.valueOf(d1);
                int k = 1;
                Integer k2 = Integer.valueOf(k);
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/901")
    @Test
    fun templateIsNewClassArgumentForNewClass() = assertChanged(
        before = """
            import java.util.Date;
            public class A {
                public static void main(String[] args) {
                    Date d = new Date(new Long(0));
                    Long l = new Long(new Integer(0));
                }
            }
        """,
        after = """
            import java.util.Date;
            public class A {
                public static void main(String[] args) {
                    Date d = new Date(Long.valueOf(0));
                    Long l = Long.valueOf(Integer.valueOf(0));
                }
            }
        """
    )

    @Test
    fun doubleToFloat(jp: JavaParser) = assertChanged(
        before = """
            class T {
                Double d1 = Double.valueOf(1.0);
                void makeFloats() {
                    Float f = new Float(2.0d);
                    Float f2 = new Float(getD());
                    Float f3 = new Float(d1);
                }
                Double getD() {
                    return Double.valueOf(2.0d);
                }
            }
        """,
        after = """
            class T {
                Double d1 = Double.valueOf(1.0);
                void makeFloats() {
                    Float f = Float.valueOf("2.0");
                    Float f2 = Float.valueOf(getD().floatValue());
                    Float f3 = Float.valueOf(d1.floatValue());
                }
                Double getD() {
                    return Double.valueOf(2.0d);
                }
            }
        """
    )
}
