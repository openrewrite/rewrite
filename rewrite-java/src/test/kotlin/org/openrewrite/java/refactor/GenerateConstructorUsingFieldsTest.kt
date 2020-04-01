package org.openrewrite.java.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.assertRefactored

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
                .visit(GenerateConstructorUsingFields(a.classes[0], a.classes[0].fields))
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