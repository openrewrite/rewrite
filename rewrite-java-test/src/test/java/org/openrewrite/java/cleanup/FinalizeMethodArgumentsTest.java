package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FinalizeMethodArgumentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizeMethodArguments());
    }

    @Test
    void doNotAddFinalIfAssigned() {
        rewriteRun(
          java(
            """
                  package a;
                   class A {
                       SubeventUtils(String a) {
                           a = "abc";
                       }
                   }
              """
          )
        );
    }

    @Test
    void replaceWithFinalModifier() {
        rewriteRun(
          java(
            """
                  package com.test;
                  
                  class TestClass {
                  
                      private void getAccaCouponData(String responsiveRequestConfig, String card) {
                       
                      }
                  }
              """,
            """
                  package com.test;
                  
                  class TestClass {
                  
                      private void getAccaCouponData(final String responsiveRequestConfig, final String card) {
                       
                      }
                  }
              """
          )
        );
    }

    @Test
    void replaceWithFinalModifierNoArguments() {
        rewriteRun(
          java(
            """
                  package com.test;
                  
                  class TestClass {
                  
                      private void getAccaCouponData() {
                       
                      }
                  }
              """
          )
        );
    }

    @Test
    void doNotReplaceWithFinalModifier() {
        rewriteRun(
          java(
            """
                  package responsive.utils.subevent;
                   
                   public class SubeventUtils {
                   
                       public SubeventUtils(final List<Integer> categoryGroupIdForChangeSubeventName) {
                           
                       }
                   
                       public static boolean isSubeventOfSpecifiedType(final Object subEvent, final List<Object> requiredTypes) {
                           return false;
                       }
                       
                       public String getSubeventNameForCategoryGroupId(final String subeventName, final Integer categoryGroupId) {
                           return subeventName;
                       }
                   }
              """
          )
        );
    }
}