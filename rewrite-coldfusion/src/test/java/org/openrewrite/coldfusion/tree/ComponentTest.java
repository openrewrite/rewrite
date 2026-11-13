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
package org.openrewrite.coldfusion.tree;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.coldfusion.marker.AttributeClause;
import org.openrewrite.coldfusion.marker.OmitComma;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;

class ComponentTest implements RewriteTest {

    private static J.ClassDeclaration component(Cf.CompilationUnit cu) {
        return cu.getClasses().get(0);
    }

    private static @Nullable String typeName(J.Annotation annotation) {
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
        return type == null ? null : type.getFullyQualifiedName();
    }

    @Test
    void scriptComponent() {
        rewriteRun(
          cfc(
            """
              /**
               * A user service.
               */
              component extends="BaseService" accessors=true singleton {

                  property name="userDAO" inject="UserDAO";
                  property string firstName;
                  property name="dateTimeHelper"
                      inject="DateTimeHelper"
                      delegate;

                  public UserService function init() {
                      return this;
                  }

                  private string function fullName( required string first, string last = "" ) {
                      return first & " " & last;
                  }

                  remote any function list() returnformat="json" {
                      return userDAO.list();
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                // a component is named for its file, which a test names Component.cfc
                assertThat(component.getSimpleName()).isEqualTo("Component");
                assertThat(component.getType()).isNotNull();
                assertThat(component.getType().getFullyQualifiedName()).isEqualTo("Component");
                assertThat(((J.Identifier) component.getExtends()).getSimpleName()).isEqualTo("BaseService");
                assertThat(component.getLeadingAnnotations()).extracting(J.Annotation::getSimpleName)
                  .containsExactly("accessors", "singleton");
                assertThat(typeName(component.getLeadingAnnotations().get(0))).isEqualTo("cfcomponent.accessors");
                // WireBox's, which the language gives no meaning
                assertThat(typeName(component.getLeadingAnnotations().get(1))).isEqualTo("singleton");

                List<Statement> body = component.getBody().getStatements();
                // a property is a field, named by its name attribute or its shorthand
                J.VariableDeclarations userDAO = (J.VariableDeclarations) body.get(0);
                assertThat(userDAO.getVariables().get(0).getSimpleName()).isEqualTo("userDAO");
                assertThat(userDAO.getModifiers()).extracting(J.Modifier::getKeyword).containsExactly("property");
                assertThat(userDAO.getLeadingAnnotations()).extracting(ComponentTest::typeName).containsExactly("inject");
                J.VariableDeclarations shorthand = (J.VariableDeclarations) body.get(1);
                assertThat(((J.Identifier) shorthand.getTypeExpression()).getSimpleName()).isEqualTo("string");
                assertThat(shorthand.getVariables().get(0).getSimpleName()).isEqualTo("firstName");
                assertThat(((J.VariableDeclarations) body.get(2)).getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("inject", "delegate");
                J.MethodDeclaration init = (J.MethodDeclaration) body.get(3);
                assertThat(init.getSimpleName()).isEqualTo("init");
                assertThat(init.getModifiers()).extracting(J.Modifier::getType).containsExactly(J.Modifier.Type.Public);
                J.MethodDeclaration fullName = (J.MethodDeclaration) body.get(4);
                assertThat(fullName.getParameters()).hasSize(2);
                J.MethodDeclaration list = (J.MethodDeclaration) body.get(5);
                assertThat(list.getModifiers().get(0).getKeyword()).isEqualTo("remote");
                assertThat(list.getLeadingAnnotations()).singleElement()
                  .satisfies(a -> assertThat(typeName(a)).isEqualTo("cffunction.returnformat"));
            })
          )
        );
    }

    @Test
    void supertypesAmongAttributes() {
        rewriteRun(
          cfc(
            """
              component output = false extends = 'models.Base' Implements="IFoo , models.IBar" {
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                assertThat(component.getExtends()).isInstanceOf(J.FieldAccess.class);
                assertThat(component.getPadding().getExtends().getMarkers().findFirst(AttributeClause.class)).get()
                  .satisfies(clause -> {
                      assertThat(clause.getPosition()).isEqualTo(1);
                      assertThat(clause.getQuote()).isEqualTo("'");
                  });
                assertThat(component.getImplements()).hasSize(2);
                assertThat(component.getPadding().getImplements().getMarkers().findFirst(AttributeClause.class)).get()
                  .extracting(AttributeClause::getName).isEqualTo("Implements");
                assertThat(component.getLeadingAnnotations()).extracting(J.Annotation::getSimpleName).containsExactly("output");
            })
          ),
          cfc(
            """
              component implements="IValidator" extends="BaseObject" {
              }
              """,
            spec -> spec.path("Validator.cfc")
          )
        );
    }

    @Test
    void interfaceAndModifiers() {
        rewriteRun(
          cfc(
            """
              import models.*;

              abstract component implements="IService" {
                  public abstract void function run();
                  static string function name() { return "x"; }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                assertThat(component.getModifiers()).extracting(J.Modifier::getType).containsExactly(J.Modifier.Type.Abstract);
                assertThat(component.getImplements()).singleElement()
                  .satisfies(i -> assertThat(((J.Identifier) i).getSimpleName()).isEqualTo("IService"));
            })
          ),
          cfc(
            """
              interface extends="IBase" {
                  function run();
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                assertThat(component.getKind()).isEqualTo(J.ClassDeclaration.Kind.Type.Interface);
                // an interface extends interfaces, which J.ClassDeclaration holds as what it implements
                assertThat(component.getExtends()).isNull();
                assertThat(component.getImplements()).hasSize(1);
            })
          )
        );
    }

    @Test
    void parametersWithoutCommas() {
        rewriteRun(
          cfc(
            """
              component {
                  function href(
                      href = "",
                      title = ""
                      target = ""
                      struct data boolean ssl,
                      required string path hint="where it lives",
                  ) {
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.MethodDeclaration href = (J.MethodDeclaration) component(cu).getBody().getStatements().get(0);
                assertThat(href.getParameters()).hasSize(6);
                assertThat(href.getPadding().getParameters().getPadding().getElements().get(1).getMarkers()
                  .findFirst(OmitComma.class)).isPresent();
                J.VariableDeclarations path = (J.VariableDeclarations) href.getParameters().get(5);
                assertThat(path.getLeadingAnnotations()).singleElement()
                  .satisfies(a -> assertThat(typeName(a)).isEqualTo("cfargument.hint"));
            })
          )
        );
    }

    @Test
    void tagComponent() {
        rewriteRun(
          cfc(
            """
              <cfcomponent displayname="Greeter" output="false">
                  <cfproperty name="greeting" type="string" default="Hello">

                  <cffunction name="greet" access="public" returntype="string" output="false">
                      <cfargument name="who" type="string" required="true">
                      <cfreturn variables.greeting & ", " & arguments.who>
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.beforeRecipe(cu -> {
                // the same class a script component is, named for its file
                J.ClassDeclaration component = component(cu);
                assertThat(component.getSimpleName()).isEqualTo("Component");
                assertThat(component.getMarkers().findFirst(TagSyntax.class)).isPresent();
                assertThat(component.getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("cfcomponent.displayname", "cfcomponent.output");

                List<Statement> body = component.getBody().getStatements();
                J.VariableDeclarations greeting = (J.VariableDeclarations) body.get(0);
                assertThat(greeting.getVariables().get(0).getSimpleName()).isEqualTo("greeting");
                assertThat(((J.Identifier) greeting.getTypeExpression()).getSimpleName()).isEqualTo("string");
                assertThat(greeting.getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("cfproperty.default");

                J.MethodDeclaration greet = (J.MethodDeclaration) body.get(1);
                assertThat(greet.getSimpleName()).isEqualTo("greet");
                assertThat(((J.Identifier) greet.getReturnTypeExpression()).getSimpleName()).isEqualTo("string");
                assertThat(greet.getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("cffunction.access", "cffunction.output");
                J.VariableDeclarations who = (J.VariableDeclarations) greet.getParameters().get(0);
                assertThat(who.getVariables().get(0).getSimpleName()).isEqualTo("who");
                assertThat(who.getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("cfargument.required");
                assertThat(greet.getBody().getStatements()).singleElement()
                  .satisfies(s -> assertThat(((Cf.Tag) s).isNamed("cfreturn")).isTrue());
            })
          )
        );
    }

    @Test
    void tagAttributesKeepTheirPlaces() {
        rewriteRun(
          cfc(
            """
              <CFCOMPONENT Output=false Extends = 'models.Base' >
                  <CFFUNCTION access="private" NAME = 'helper' returnType=void >
                      <CFARGUMENT required="true" type = "numeric" Name="n"/>
                      <cfset x = 1>
                  </CFFUNCTION>
              </CFCOMPONENT>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                assertThat(component.getExtends()).isInstanceOf(J.FieldAccess.class);
                J.MethodDeclaration helper = (J.MethodDeclaration) component.getBody().getStatements().get(0);
                assertThat(helper.getSimpleName()).isEqualTo("helper");
                assertThat(helper.getMarkers().getMarkers()).filteredOn(AttributeClause.class::isInstance)
                  .extracting(m -> ((AttributeClause) m).getName()).containsExactly("NAME", "returnType");
                J.VariableDeclarations n = (J.VariableDeclarations) helper.getParameters().get(0);
                assertThat(n.getVariables().get(0).getSimpleName()).isEqualTo("n");
                assertThat(((J.Identifier) n.getTypeExpression()).getSimpleName()).isEqualTo("numeric");
            })
          )
        );
    }

    @Test
    void tagInterface() {
        rewriteRun(
          cfc(
            """
              <cfinterface extends="IBase">
                  <cffunction name="run" returntype="void" />
                  <cffunction name="stop">
                      <cfargument name="force" type="boolean">
                  </cffunction>
              </cfinterface>
              """,
            spec -> spec.beforeRecipe(cu -> {
                J.ClassDeclaration component = component(cu);
                assertThat(component.getKind()).isEqualTo(J.ClassDeclaration.Kind.Type.Interface);
                assertThat(component.getImplements()).hasSize(1);
                J.MethodDeclaration run = (J.MethodDeclaration) component.getBody().getStatements().get(0);
                assertThat(run.getBody()).isNull();
                assertThat(run.getParameters()).singleElement().isInstanceOf(J.Empty.class);
                J.MethodDeclaration stop = (J.MethodDeclaration) component.getBody().getStatements().get(1);
                assertThat(stop.getParameters()).singleElement().isInstanceOf(J.VariableDeclarations.class);
                assertThat(stop.getBody().getStatements()).isEmpty();
            })
          )
        );
    }

    @Test
    void onlyLeadingArgumentsAreParameters() {
        rewriteRun(
          cfc(
            """
              <cfcomponent>
                  <cffunction name="f">
                      <cfargument name="a">
                      <!--- a comment between them is space --->
                      <cfargument name="b" default="#now()#">
                      <cfset local.x = 1>
                      <cfargument name="c">
                  </cffunction>
                  <cffunction name="#dynamic#">
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> body = component(cu).getBody().getStatements();
                J.MethodDeclaration f = (J.MethodDeclaration) body.get(0);
                assertThat(f.getParameters()).extracting(s -> ((J.VariableDeclarations) s).getVariables().get(0).getSimpleName())
                  .containsExactly("a", "b");
                assertThat(f.getBody().getStatements()).hasSize(2);
                assertThat(((Cf.Tag) f.getBody().getStatements().get(1)).isNamed("cfargument")).isTrue();
                // a name the printer can't hold as a name stays a tag
                assertThat(body.get(1)).isInstanceOf(Cf.Tag.class);
            })
          )
        );
    }

    @Test
    void functionTagsInTemplates() {
        rewriteRun(
          cfm(
            """
              <cffunction name="helper" output="false">
                  <cfreturn 1>
              </cffunction>
              <cfcomponent>
              </cfcomponent>
              """,
            spec -> spec.beforeRecipe(cu -> {
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.MethodDeclaration.class);
                // only a component file declares a class
                assertThat(cu.getStatements().get(1)).isInstanceOf(Cf.Tag.class);
            })
          )
        );
    }

    @Test
    void propertyWithoutAName() {
        rewriteRun(
          cfc(
            """
              component {
                  property name="#dynamic#" type="string";
                  property name="id" type="numeric" fieldtype="id";
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<Statement> body = component(cu).getBody().getStatements();
                assertThat(body.get(0)).isInstanceOf(Cf.ScriptTag.class);
                J.VariableDeclarations id = (J.VariableDeclarations) body.get(1);
                assertThat(((J.Identifier) id.getTypeExpression()).getSimpleName()).isEqualTo("numeric");
                assertThat(id.getLeadingAnnotations()).extracting(ComponentTest::typeName)
                  .containsExactly("cfproperty.fieldtype");
            })
          )
        );
    }
}
