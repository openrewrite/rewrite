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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("CharsetObjectCanBeUsed")
interface UseStandardCharsetTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseStandardCharset()


    @Test
    fun notAStandardCharset() = assertUnchanged(
        before = """
            import java.nio.charset.Charset;

            class Test {
                Charset WINDOWS_1252 = Charset.forName("Windows-1252");
            }
        """
    )

    @Test
    fun changeCharsetForName() = assertChanged(
        before = """
            import java.nio.charset.Charset;

            class Test {
                Charset US_ASCII = Charset.forName("US-ASCII");
                Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
                Charset UTF_8 = Charset.forName("UTF-8");
                Charset UTF_16 = Charset.forName("UTF-16");
                Charset UTF_16BE = Charset.forName("UTF-16BE");
                Charset UTF_16LE = Charset.forName("UTF-16LE");
            }
        """,
        after = """
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;

            class Test {
                Charset US_ASCII = StandardCharsets.US_ASCII;
                Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
                Charset UTF_8 = StandardCharsets.UTF_8;
                Charset UTF_16 = StandardCharsets.UTF_16;
                Charset UTF_16BE = StandardCharsets.UTF_16BE;
                Charset UTF_16LE = StandardCharsets.UTF_16LE;
            }
        """
    )
}
