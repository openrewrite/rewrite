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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("FunctionName")
interface DateFormatWeekYearTest: JavaRecipeTest {
    override val recipe: Recipe
        get() = DateFormatWeekYear()

    @Test
    fun `correct format is not modified`(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
        import java.text.SimpleDateFormat;
        import java.time.format.DateTimeFormatter;

        class A {
            void a() {
                Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                String r = new SimpleDateFormat("yyyy/MM/dd").format(d);   // Correct; returns '2015/12/31' as expected
                r = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(d); // Correct; returns '2015/12/31' as expected
            }
        }
        """
    )

    @Test
    fun `incorrect use of week year is fixed`(jp: JavaParser) = assertChanged(
        jp,
        before = """
        import java.text.SimpleDateFormat;
        import java.time.format.DateTimeFormatter;

        class A {
            void a() {
                Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                String r = new SimpleDateFormat("YYYY/MM/dd").format(d); // Incorrect; returns '2016/12/31'
                r = DateTimeFormatter.ofPattern("YYYY/MM/dd").format(d); // Incorrect; returns '2016/12/31'
            }
        }
        """,
        after = """
        import java.text.SimpleDateFormat;
        import java.time.format.DateTimeFormatter;

        class A {
            void a() {
                Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                String r = new SimpleDateFormat("yyyy/MM/dd").format(d); // Correct; returns '2015/12/31'
                r = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(d); // Correct; returns '2015/12/31'
            }
        }
        """
    )

    @Test
    @Disabled
    fun `correct use of week year is unmodified`(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
        import java.text.SimpleDateFormat;
        import java.time.format.DateTimeFormatter;

        class A {
            void a() {
                Date d = new SimpleDateFormat("yyyy/MM/dd").parse("2015/12/31");
                String r = new SimpleDateFormat("YYYY-ww").format(d); // Correct, 'Week year' is used with 'Week of year'. r = '2016-01'
                r = DateTimeFormatter.ofPattern("YYYY-ww").format(d); // Correct; returns '2016-01' as expected
            }
        }
        """
    )
}
