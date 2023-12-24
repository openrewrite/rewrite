/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ClassDeclarationTest implements RewriteTest {

    @Test
    void classDeclaration() {
        rewriteRun(
          ruby(
            """
              class Customer
              end
              """
          )
        );
    }

    @Test
    void classExtends() {
        rewriteRun(
          ruby(
            """
              class Box
              end
                            
              class BigBox < Box
              end
              """
          )
        );
    }

    @Test
    void initializeAndInstanceMethod() {
        rewriteRun(
          ruby(
            """
              class Box
                 def initialize(w,h)
                    @width, @height = w, h
                 end
                            
                 def getArea
                    @width * @height
                 end
              end
              """
          )
        );
    }

    @Test
    void newInstanceAndCall() {
        rewriteRun(
          ruby(
            """
              class Box
                 def initialize(w,h)
                    @width, @height = w, h
                 end
                 
                 def printArea
                    @area = @width * @height
                    puts "Big box area is : #{@area}"
                 end
              end
              
              box = BigBox.new(10, 20)
              box.printArea()
              """
          )
        );
    }
}
