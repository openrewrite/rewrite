/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.search.FindMethods

interface RemoveUnusedPrivateMethodsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveUnusedPrivateMethods()

    @Test
    fun priam() = assertUnchanged(
//        recipe = FindMethods("com.netflix.priam.backupv2.MetaFileWriterBuilder.MetaFileWriter toFileUploadResult(..)"),
        moderneAstLink = "https://api.moderne.io/worker/cmVjaXBld29ya2VyLXByb2QtdjEwMC0wZ3di/ast/file/Netflix%3APriam/a05db7288cb828c12394c9f2d73184ff2e1a36de/cHJpYW0vc3JjL21haW4vamF2YS9jb20vbmV0ZmxpeC9wcmlhbS9iYWNrdXB2Mi9NZXRhRmlsZVdyaXRlckJ1aWxkZXIuamF2YQ=="
    )

    @Test
    fun removeUnusedPrivateMethods() = assertChanged(
        before = """
            class Test {
                private void unused() {
                }
            
                public void dontRemove() {
                    dontRemove2();
                }
                
                private void dontRemove2() {
                }
            }
        """,
        after = """
            class Test {
            
                public void dontRemove() {
                    dontRemove2();
                }
                
                private void dontRemove2() {
                }
            }
        """
    )
}
