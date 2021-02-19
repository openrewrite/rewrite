package com.squareup.rewrite.passIdsInsteadOfObjects

import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser
import org.openrewrite.java.cleanup.DeencapsulateObjects
import org.openrewrite.java.tree.J

class DeencapsulateObjectsTest : RecipeTest {
  override val parser: Parser<J.CompilationUnit> = JavaParser.fromJavaVersion()
//    .classpath("junit", "assertj-core", "apiguardian-api")
    .build()
    override val recipe:Recipe
        get() = DeencapsulateObjects()

  @Test
  @org.intellij.lang.annotations.Language("java")

  fun basicRename() = assertChanged(

      dependsOn = arrayOf(
      """
        class DTO {
          private String id = "4";
          public String getId(){
            return id;
          }
        }
      """.trimIndent()
    ),
    before = """
      class A {
        public void doThing(DTO dto){
          dto.getId();
        }
      }
    """.trimIndent(),
    after = """
      class A {
        public void doThing(String id){
          id;
        }
      }
    """.trimIndent()
  )
    @org.intellij.lang.annotations.Language("java")

  fun full() = assertChanged(

        dependsOn = arrayOf(
      """
        class DTO {
          private String id = "4";
          public String getId(){
            return id;
          }
        }
      """.trimIndent()
    ),
    before = """
      class A {
        public void doThing(DTO dto){
          dto.getId();
        }
      }
      
      class B {
        A a = new A();
        public void callDoThing(DTO dto){
          a.doThing(dto);
        }
      }
      
      class C {
        public static void main(String[] args){
          B b = new B();
          DTO dto=new DTO();
          b.callDoThing(dto);
        }
      }
    """.trimIndent(),
    after = """
      class A {
        public void doThing(String id){
          id;
        }
      }
      
      class B {
        A a = new A();
        public void callDoThing(String id){
          a.doThing(id);
        }
      }
      
      class C {
        public static void main(String[] args){
          B b = new B();
          DTO dto=new DTO();
          b.callDoThing(dto.getId());
        }
      }
    """.trimIndent()
  )
}
