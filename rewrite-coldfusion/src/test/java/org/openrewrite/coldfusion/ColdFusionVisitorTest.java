/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Recipes see one model whichever syntax the CFML is written in: a call is a
 * {@link J.MethodInvocation} in a tag's expression, an interpolation or script.
 */
class ColdFusionVisitorTest implements RewriteTest {

    @Test
    void renameCallsInTagsInterpolationsAndScript() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if ("dateFormat".equalsIgnoreCase(m.getSimpleName())) {
                      return m.withName(m.getName().withSimpleName("formatDate"));
                  }
                  return m;
              }
          })),
          cfm(
            """
              <cfset today = dateFormat(now(), "yyyy-mm-dd")>
              <cfoutput>#dateFormat(created, "long")#</cfoutput>
              <cfscript>
                  stamp = "Made #dateFormat(now())#";
              </cfscript>
              """,
            """
              <cfset today = formatDate(now(), "yyyy-mm-dd")>
              <cfoutput>#formatDate(created, "long")#</cfoutput>
              <cfscript>
                  stamp = "Made #formatDate(now())#";
              </cfscript>
              """
          )
        );
    }

    @Test
    void changeTagAttribute() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public Cf.TagAttribute visitTagAttribute(Cf.TagAttribute attribute, ExecutionContext ctx) {
                  Cf.TagAttribute a = super.visitTagAttribute(attribute, ctx);
                  if ("datasource".equalsIgnoreCase(a.getName().getSimpleName()) && a.getValue() instanceof J.Literal &&
                      "legacy".equals(((J.Literal) a.getValue()).getValue())) {
                      return a.withValue(((J.Literal) a.getValue()).withValue("main").withValueSource("\"main\""));
                  }
                  return a;
              }
          })),
          cfm(
            """
              <cfquery name="q" datasource="legacy">SELECT 1</cfquery>
              """,
            """
              <cfquery name="q" datasource="main">SELECT 1</cfquery>
              """
          )
        );
    }

    @Test
    void addFunctionToComponent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ColdFusionIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if ("legacy".equals(m.getSimpleName())) {
                      return m.withName(m.getName().withSimpleName("modern")).withPrefix(Space.format("\n    "));
                  }
                  return m;
              }
          })),
          cfc(
            """
              component {
                  function legacy() {
                      return 1;
                  }
              }
              """,
            """
              component {
                  function modern() {
                      return 1;
                  }
              }
              """
          )
        );
    }
}
