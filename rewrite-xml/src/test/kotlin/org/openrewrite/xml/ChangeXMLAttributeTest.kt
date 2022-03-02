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
package org.openrewrite.xml

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.xml.tree.Xml

class ChangeXMLAttributeTest: XmlRecipeTest {

    @Test
    fun alterAttributeWhenElementAndAttributeMatch() = assertChanged(
            recipe = ChangeXMLAttribute("bean","id","myBean2.subpackage","myBean.subpackage",null)
            ,
            before = """
            <beans>
                <bean id='myBean.subpackage.subpackage2'/>
                <other id='myBean.subpackage.subpackage2'/>
            </beans>
        """,
            after = """
            <beans>
                <bean id='myBean2.subpackage.subpackage2'/>
                <other id='myBean.subpackage.subpackage2'/>
            </beans>
        """

    )

    @Test
    fun alterAttributeWithNullOldValue() = assertChanged(
            recipe = ChangeXMLAttribute("bean","id","myBean2.subpackage",null,null),
            before = """
            <beans>
                <bean id='myBean.subpackage.subpackage2'/>
                <other id='myBean.subpackage.subpackage2'/>
            </beans>
        """,
            after = """
            <beans>
                <bean id='myBean2.subpackage'/>
                <other id='myBean.subpackage.subpackage2'/>
            </beans>
        """,
            cycles = 1,
            expectedCyclesThatMakeChanges = 1
    )



    @Test
    fun attributeNotMatched() = assertUnchanged(

            recipe = ChangeXMLAttribute("bean","id","myBean2.subpackage","not.matched",null),
            before = """
            <beans>
                <bean id='myBean.subpackage.subpackage2'/>
                <other id='myBean.subpackage.subpackage2'/>
            </beans>
        """
    )
}