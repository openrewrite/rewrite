/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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

    @Test
    void classMethods() {
        rewriteRun(
          ruby(
            """
              class Point
                  def Point.sum(*points)
                  end
              end
              """
          )
        );
    }

    @Test
    void extendClass() {
        rewriteRun(
          ruby(
            """
              class Point2D < Point
              end
              """
          )
        );
    }

    @Test
    void extendStruct() {
        rewriteRun(
          ruby(
            """
              class Point2D < Struct.new("Point2D", :x, :y)
              end
              """
          )
        );
    }

    @Test
    void superCall() {
        rewriteRun(
          ruby(
            """
              class Point3D < Point2D
                  def initialize(x,y,z)
                      super(x,y)
                      @z = z
                  end
              end
              """
          )
        );
    }

    @Test
    void zeroLengthSuperCall() {
        rewriteRun(
          ruby(
            """
              class Point3D < Point2D
                  def initialize(x,y,z)
                      super
                      @z = z
                  end
              end
              """
          )
        );
    }

    @Test
    void classVariableAssignment() {
        rewriteRun(
          ruby(
            """
              class Point
                  @@n = 0
                  
                  def initialize(x,y)
                      @x, @y = x, y
                      @@n += 1
                  end
                  
                  def count
                      @@n
                  end
              end
              """
          )
        );
    }

    @Test
    void constants() {
        rewriteRun(
          ruby(
            """
              class Point
                  def initialize(x,y)
                      @x, @y = x, y
                      @@n += 1
                  end
                            
                  ORIGIN = Point.new(0,0)
              end
              """
          )
        );
    }

    /**
     * Also called "opening the eigenclass".
     */
    @Test
    void singletonClass() {
        rewriteRun(
          ruby(
            """
              class << recv
              end
              """
          )
        );
    }
}
