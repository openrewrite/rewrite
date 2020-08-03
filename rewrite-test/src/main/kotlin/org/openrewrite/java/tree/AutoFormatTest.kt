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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.AutoFormat
import org.openrewrite.java.JavaParser
import org.openrewrite.whenParsedBy

interface AutoFormatTest {
    @Test
    fun methodDeclaration(jp: JavaParser) {
        """
            import java.util.*;
            
            public class A {
                List<String> l = new ArrayList<>();
                
            @Deprecated
            public void method() {
              if(true) {
                l.add("value");
              }
            }
            }
        """
                .whenParsedBy(jp)
                .whenVisitedByMapped { a -> AutoFormat(a.classes[0].methods[0]) }
                .isRefactoredTo("""
                    import java.util.*;
                    
                    public class A {
                        List<String> l = new ArrayList<>();
                        
                        @Deprecated
                        public void method() {
                            if(true) {
                                l.add("value");
                            }
                        }
                    }
                """)
    }
}
