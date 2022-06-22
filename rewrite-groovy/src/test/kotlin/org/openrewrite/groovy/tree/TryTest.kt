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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

@Suppress("GroovyUnusedCatchParameter")
class TryTest : GroovyTreeTest {

    @Test
    fun severalCatchBlocks() = assertParsePrintAndProcess(
        """
           try {

           } catch (RuntimeException e) {

           } catch (Exception e) {
           
           }
        """
    )

    @Test
    fun catchOmittingType() = assertParsePrintAndProcess(
        """
            try {
            
            } catch (all) {
            
            }
        """
    )

    @Test
    fun tryFinally() = assertParsePrintAndProcess(
        """
            try {
            
            } finally {
                // some comment
            }
        """
    )

    @Test
    fun tryCatchFinally() = assertParsePrintAndProcess(
        """
           try {
           
           } catch (Exception e) {
           
           } finally {
               def a = ""
           }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1944")
    @Disabled
    @Test
    fun multiCatch() = assertParsePrintAndProcess(
        """
            try {
            
            } catch (RuntimeException | Exception e) {
            
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1945")
    @Disabled
    @Test
    fun tryWithResource() = assertParsePrintAndProcess(
        """
            try(ByteArrayInputStream a = new ByteArrayInputStream("".getBytes())) {
            
            } catch (Exception e) {
            
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1945")
    @Disabled
    @Test
    fun tryWithResources() = assertParsePrintAndProcess(
        """
            try(ByteArrayInputStream a = new ByteArrayInputStream("".getBytes()); ByteArrayInputStream b = new ByteArrayInputStream("".getBytes())) {
            
            } catch (Exception e) {
            
            }
        """
    )
}
