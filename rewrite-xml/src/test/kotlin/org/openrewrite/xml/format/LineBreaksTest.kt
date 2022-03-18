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
package org.openrewrite.xml.format

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.xml.XmlRecipeTest

class LineBreaksTest : XmlRecipeTest {
    override val recipe: Recipe
        get() = LineBreaks()

    @Suppress("CheckTagEmptyBody")
    @Test
    fun tags() = assertChanged(
        before = """
            <project>
              <dependencies><dependency></dependency><dependency/>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <dependencies>
            <dependency></dependency>
            <dependency/>
              </dependencies>
            </project>
        """
    )

    @Suppress("CheckTagEmptyBody")
    @Test
    fun comments() = assertChanged(
        before = """
            <project>
              <dependencies><!--comment-->
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <dependencies>
            <!--comment-->
              </dependencies>
            </project>
        """
    )

    @Suppress("CheckTagEmptyBody")
    @Test
    fun docTypeDecl() = assertChanged(
        before = """
            <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd"><beans/>
        """,
        after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd">
            <beans/>
        """
    )

    @Suppress("CheckTagEmptyBody")
    @Test
    fun prolog() = assertChanged(
        before = """<?xml version="1.0" encoding="UTF-8"?><?xml-stylesheet href="mystyle.css" type="text/css"?><beans/>""",
        after = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet href="mystyle.css" type="text/css"?>
            <beans/>
        """
    )
}
