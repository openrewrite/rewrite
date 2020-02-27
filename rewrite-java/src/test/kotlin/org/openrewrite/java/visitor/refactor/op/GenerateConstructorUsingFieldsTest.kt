/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor.op

import org.openrewrite.java.assertRefactored
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

class GenerateConstructorUsingFieldsTest : JavaParser() {
    @Test
    fun generateConstructorUsingFields() {
        val a = parse("""
            public class UsersController {
                private final UsersService usersService;
                private final UsernameService usernameService;
            }
        """.trimIndent())

        val fixed = a.refactor()
                .visit(GenerateConstructorUsingFields(a.classes[0].id, a.classes[0].fields))
                .fix().fixed

        assertRefactored(fixed, """
            public class UsersController {
                private final UsersService usersService;
                private final UsernameService usernameService;
            
                public UsersController(UsersService usersService, UsernameService usernameService) {
                    this.usersService = usersService;
                    this.usernameService = usernameService;
                }
            }
        """)
    }
}