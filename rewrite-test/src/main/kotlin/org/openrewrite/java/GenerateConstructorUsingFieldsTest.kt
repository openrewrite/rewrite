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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.whenParsedBy

interface GenerateConstructorUsingFieldsTest {
    @Test
    fun generateConstructorUsingFields(jp: JavaParser) {
        """
            public class UsersController {
                private final UsersService usersService;
                private final UsernameService usernameService;
                
                public User findUser(String name) {
                }
            }
        """
                .whenParsedBy(jp)
                .whenVisitedByMapped { a -> GenerateConstructorUsingFields(jp, a.classes[0], a.classes[0].fields) }
                .isRefactoredTo("""
                    public class UsersController {
                        private final UsersService usersService;
                        private final UsernameService usernameService;
                    
                        public UsersController(UsersService usersService, UsernameService usernameService) {
                            this.usersService = usersService;
                            this.usernameService = usernameService;
                        }
                        
                        public User findUser(String name) {
                        }
                    }
                """)
    }

    @Test
    fun emptyListOfFields(jp: JavaParser) {
        """
            public class UsersController {
            }
        """
                .whenParsedBy(jp)
                .whenVisitedByMapped { a -> GenerateConstructorUsingFields(jp, a.classes[0], emptyList()) }
                .isRefactoredTo("""
                    public class UsersController {
                    
                        public UsersController() {
                        }
                    }
                """)
    }
}
