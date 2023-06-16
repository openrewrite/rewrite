package org.openrewrite.java;

import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.RecipeSpec;

import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.Assertions.java;

class AddScopeToInjectedClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddScopeToInjectedClass());
    }

    @DocumentExample
    @Test
    void scopeRequired() {
        rewriteRun(java("""
        package com.sample;
        public class Bar{}
        """,
        """
        package com.sample;
        import javax.enterprise.context.Dependent;
        @Dependent
        public class Bar{}
        """),java("""
        package com.sample;
        import javax.inject.Inject;
        public class Foo{
            @Inject
            Bar service;
        }
        """));
    }

/**  @Test
  void serviceInjected() {
      JavaType.Class controller = TypeUtils.asClass(TypeTree.<J.FieldAccess>build("org.openrewrite.Test.Controller").getType().);
      JavaType.Class service = TypeUtils.asClass(TypeTree.<J.FieldAccess>build("org.openrewrite.Test.Service").getType());
      JavaType.FullyQualified inject = TypeUtils.asClass(TypeTree.<J.FieldAccess>build("javax.inject.Inject").getType().getFullyQualified());
      List<JavaType.FullyQualified> annotations = new ArrayList<>();
      annotations.add(inject);
      J.ClassDeclaration classDeclaration = new J.ClassDeclaration();
      classDeclaration.withType(controller);
      JavaType.Variable variable = new JavaType.Variable(null,0,"serviceVariable" , controller, service, annotations);
      TestRecipe.
      var replaced = new AddScopeToInjectedClass().getVisitor().visit(classDeclaration, );
      assertThat(requireNonNull(TypeUtils.asClass(replaced)).getOwningClass()).isEqualTo(t2t.getOwningClass());
  }*/

}