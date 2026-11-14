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
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.ColdFusionParser;
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.coldfusion.Assertions.cfc;
import static org.openrewrite.coldfusion.Assertions.cfm;

class TypeAttributionTest implements RewriteTest {

    private static List<J.MethodInvocation> calls(Cf.CompilationUnit cu) {
        List<J.MethodInvocation> calls = new ArrayList<>();
        new ColdFusionIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                calls.add(method);
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);
        return calls;
    }

    private static @Nullable String declaredBy(J.MethodInvocation call) {
        JavaType.Method type = call.getMethodType();
        return type == null ? null : type.getDeclaringType().getFullyQualifiedName() + "." + type.getName();
    }

    @Test
    void builtInFunctions() {
        rewriteRun(
          cfm(
            """
              <cfset n = ARRAYLEN(items)>
              <cfoutput>#len(name)# #helper()#</cfoutput>
              <cfset image = imageNew()>
              """,
            spec -> spec.beforeRecipe(cu ->
              // a built-in function is a method of cfml, named as the language spells it, as are an extension's
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("cfml.arrayLen", "cfml.len", null, "cfml.imageNew"))
          )
        );
    }

    @Test
    void componentsAcrossFiles() {
        rewriteRun(
          cfc(
            """
              component {
                  function init() { return this; }
                  function save() {}
              }
              """,
            spec -> spec.path("models/Base.cfc")
          ),
          cfc(
            """
              component extends="Base" {
                  function go() {
                      save();
                      this.save();
                      super.init();
                      validate();
                      return new User().go();
                  }
                  function validate() {}
              }
              """,
            spec -> spec.path("models/User.cfc").beforeRecipe(cu -> {
                J.ClassDeclaration user = cu.getClasses().get(0);
                assertThat(user.getType().getFullyQualifiedName()).isEqualTo("models.User");
                // Base is beside User, in models
                assertThat(user.getType().getSupertype().getFullyQualifiedName()).isEqualTo("models.Base");
                assertThat(TypeUtils.asFullyQualified(user.getExtends().getType()).getFullyQualifiedName()).isEqualTo("models.Base");
                J.MethodDeclaration go = (J.MethodDeclaration) user.getBody().getStatements().get(0);
                assertThat(go.getMethodType().getDeclaringType()).isSameAs(user.getType());
                assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                  "models.Base.save", "models.Base.save", "models.Base.init", "models.User.validate", "models.User.go");
            })
          ),
          cfc(
            """
              <cfcomponent extends="models.Base">
                  <cffunction name="run" returntype="models.User">
                      <cfargument name="user" type="User">
                      <cfreturn new models.User()>
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu -> {
                J.ClassDeclaration main = cu.getClasses().get(0);
                // models.Base is named from the root of the repository
                assertThat(main.getType().getSupertype().getFullyQualifiedName()).isEqualTo("models.Base");
                J.MethodDeclaration run = (J.MethodDeclaration) main.getBody().getStatements().get(0);
                assertThat(TypeUtils.asFullyQualified(run.getReturnTypeExpression().getType()).getFullyQualifiedName())
                  .isEqualTo("models.User");
                // User names the one component whose path ends with it
                J.VariableDeclarations user = (J.VariableDeclarations) run.getParameters().get(0);
                assertThat(TypeUtils.asFullyQualified(user.getTypeExpression().getType()).getFullyQualifiedName())
                  .isEqualTo("models.User");
                J.NewClass newUser = (J.NewClass) ((Cf.Tag) run.getBody().getStatements().get(0)).getExpression();
                assertThat(newUser.getConstructorType().getDeclaringType().getFullyQualifiedName()).isEqualTo("models.User");
            })
          )
        );
    }

    @Test
    void callsOnVariablesHoldingComponents() {
        rewriteRun(
          cfc(
            """
              component {
                  function save() {}
              }
              """,
            spec -> spec.path("models/User.cfc")
          ),
          cfc(
            """
              component {
                  property name="repository" type="models.User";

                  function init() {
                      variables.helper = createObject("component", "models.User");
                      return this;
                  }

                  function run(required models.User user) {
                      var made = new models.User();
                      user.save();
                      arguments.user.save();
                      made.save();
                      repository.save();
                      variables.helper.save();
                      createObject("component", "models.User").save();
                  }
              }
              """,
            spec -> spec.path("services/Runner.cfc").beforeRecipe(cu -> {
                List<J.MethodInvocation> calls = calls(cu);
                assertThat(calls).extracting(TypeAttributionTest::declaredBy).containsExactly(
                  "cfml.createObject", "models.User.save", "models.User.save", "models.User.save", "models.User.save",
                  "models.User.save", "models.User.save", "cfml.createObject");
                // createObject returns the component it names
                assertThat(TypeUtils.asFullyQualified(calls.get(0).getMethodType().getReturnType()).getFullyQualifiedName())
                  .isEqualTo("models.User");
            })
          )
        );
    }

    @Test
    void mappingsAreTheClasspath(@TempDir Path testbox) throws IOException {
        Files.createDirectories(testbox.resolve("system"));
        Files.write(testbox.resolve("system/BaseSpec.cfc"), (
          "component {\n" +
          "    function describe(title, body) {}\n" +
          "    any function expect(actual) {}\n" +
          "}\n").getBytes(StandardCharsets.UTF_8));
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("/testbox", testbox)),
          cfc(
            """
              component extends="testbox.system.BaseSpec" {
                  function run() {
                      describe("a spec", function() {
                          expect(1);
                      });
                  }
              }
              """,
            spec -> spec.path("tests/specs/UserSpec.cfc").beforeRecipe(cu -> {
                J.ClassDeclaration userSpec = cu.getClasses().get(0);
                assertThat(userSpec.getType().getSupertype().getFullyQualifiedName()).isEqualTo("testbox.system.BaseSpec");
                assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                  .containsExactly("testbox.system.BaseSpec.describe", "testbox.system.BaseSpec.expect");
            })
          )
        );
    }

    @Test
    void wireBoxInjectsWhatItsIdNames(@TempDir Path coldbox) throws IOException {
        Files.createDirectories(coldbox.resolve("system/logging"));
        Files.createDirectories(coldbox.resolve("system/web/flash"));
        Files.write(coldbox.resolve("system/FrameworkSupertype.cfc"),
          "component { function getInstance(name) {} }\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/EventHandler.cfc"),
          "component extends=\"coldbox.system.FrameworkSupertype\" {}\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/logging/Logger.cfc"),
          "component { function info(message) {} }\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/web/flash/AbstractFlashScope.cfc"),
          "component { function put(name, value) {} }\n".getBytes(StandardCharsets.UTF_8));
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("coldbox", coldbox)),
          cfc(
            """
              component {
                  function getUser() {}
              }
              """,
            spec -> spec.path("models/UserService.cfc")
          ),
          cfc(
            """
              component extends="coldbox.system.EventHandler" {
                  property name="userService" inject="UserService";
                  property name="log" inject="logbox:logger:{this}";
                  property name="flash" inject="coldbox:flash";

                  function index(event) {
                      userService.getUser();
                      log.info("index");
                      getInstance("UserService").getUser();
                      flash.put("notice", "saved");
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu -> {
                List<J.MethodInvocation> calls = calls(cu);
                assertThat(calls).extracting(TypeAttributionTest::declaredBy).containsExactly(
                  "models.UserService.getUser", "coldbox.system.logging.Logger.info",
                  "models.UserService.getUser", "coldbox.system.FrameworkSupertype.getInstance",
                  "coldbox.system.web.flash.AbstractFlashScope.put");
                // getInstance returns what WireBox maps the ID to
                assertThat(TypeUtils.asFullyQualified(calls.get(3).getMethodType().getReturnType()).getFullyQualifiedName())
                  .isEqualTo("models.UserService");
            })
          )
        );
    }

    @Test
    void wireBoxDslIdsWhoseObjectsHaveOneType(@TempDir Path coldbox) throws IOException {
        for (String[] stub : new String[][]{
          {"system/web/services/RequestService.cfc", "component { function getContext() {} }"},
          {"system/web/services/InterceptorService.cfc", "component { function announce(state) {} }"},
          {"system/core/dynamic/ObjectPopulator.cfc", "component { function populateFromStruct(target, memento) {} }"},
          {"system/cache/providers/ICacheProvider.cfc", "interface { function get(objectKey); }"},
          {"system/async/executors/Executor.cfc", "component { function submit(task) {} }"},
          {"system/ioc/Provider.cfc", "component { function $get() {} }"}}) {
            Files.createDirectories(coldbox.resolve(stub[0]).getParent());
            Files.write(coldbox.resolve(stub[0]), (stub[1] + "\n").getBytes(StandardCharsets.UTF_8));
        }
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("coldbox", coldbox)),
          cfc(
            """
              component {
                  property name="requestService" inject="coldbox:requestService";
                  property name="interceptorService" inject="box:interceptorService";
                  property name="populator" inject="wirebox:populator";
                  property name="cache" inject="cachebox:template";
                  property name="mailer" inject="executor:mail";
                  property name="users" inject="provider:UserService";
                  property name="security" inject="coldbox:interceptor:Security";

                  function index() {
                      requestService.getContext();
                      interceptorService.announce("preProcess");
                      populator.populateFromStruct(this, {});
                      cache.get("key");
                      mailer.submit(function() {});
                      users.$get();
                      security.check();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              // an interceptor's type is the application's own
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "coldbox.system.web.services.RequestService.getContext",
                "coldbox.system.web.services.InterceptorService.announce",
                "coldbox.system.core.dynamic.ObjectPopulator.populateFromStruct",
                "coldbox.system.cache.providers.ICacheProvider.get",
                "coldbox.system.async.executors.Executor.submit",
                "coldbox.system.ioc.Provider.$get",
                null))
          )
        );
    }

    @Test
    void mappingsTheRepositoryDeclares() {
        rewriteRun(
          cfc(
            """
              component {
                  rootPath = getDirectoryFromPath(getCurrentTemplatePath());
                  this.mappings["/lib"] = rootPath & "vendor/lib";
                  this.mappings["/root"] = expandPath("..");
              }
              """,
            spec -> spec.path("app/Application.cfc")
          ),
          cfc("component { function help() {} }", spec -> spec.path("app/vendor/lib/Helper.cfc")),
          cfc("component { function share() {} }", spec -> spec.path("common/Shared.cfc")),
          cfc(
            """
              component {
                  this.cfmapping = "cbauth";
              }
              """,
            spec -> spec.path("modules/cbauth/ModuleConfig.cfc")
          ),
          cfc("component { function login() {} }", spec -> spec.path("modules/cbauth/models/AuthService.cfc")),
          cfc(
            """
              component extends="lib.Helper" {
                  property name="auth" inject="AuthService@cbauth";

                  function run() {
                      help();
                      new root.common.Shared().share();
                      new cbauth.models.AuthService().login();
                      auth.login();
                  }
              }
              """,
            spec -> spec.path("app/handlers/Main.cfc").beforeRecipe(cu -> {
                // the nearest Application.cfc's mappings, and the module's, reach these
                assertThat(cu.getClasses().get(0).getType().getSupertype().getFullyQualifiedName())
                  .isEqualTo("app.vendor.lib.Helper");
                assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                  "app.vendor.lib.Helper.help", "common.Shared.share",
                  "modules.cbauth.models.AuthService.login", "modules.cbauth.models.AuthService.login");
            })
          )
        );
    }

    @Test
    void generatedAccessors() {
        rewriteRun(
          cfc(
            """
              component accessors="true" {
                  property name="firstName" type="string";
                  property name="address" type="Address";
                  property name="secret" getter="false";

                  function getFullName() { return firstName; }
              }
              """,
            spec -> spec.path("models/User.cfc")
          ),
          cfc("component accessors=true { property name=\"city\"; }", spec -> spec.path("models/Address.cfc")),
          cfc(
            """
              component persistent="true" {
                  property name="orders" fieldtype="one-to-many" cfc="Order" singularname="order";
              }
              """,
            spec -> spec.path("models/Customer.cfc")
          ),
          cfc("component persistent=\"true\" {}", spec -> spec.path("models/Order.cfc")),
          cfc(
            """
              component {
                  function run(required models.User user, required models.Customer customer) {
                      user.setFirstName("Ada").getAddress().getCity();
                      user.getFullName();
                      user.getSecret();
                      customer.addOrder(new models.Order());
                      customer.hasOrders();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              // a setter returns its component, so calls chain through it
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.Address.getCity", "models.User.getAddress", "models.User.setFirstName",
                "models.User.getFullName", null, "models.Customer.addOrder", "models.Customer.hasOrders"))
          )
        );
    }

    @Test
    void functionsReturningThisReturnTheirComponent() {
        rewriteRun(
          cfc(
            """
              component {
                  function init() { return this; }
                  function save() {}
              }
              """,
            spec -> spec.path("models/Order.cfc")
          ),
          cfc(
            """
              component {
                  function onApplicationStart() {
                      application.orders = createObject("component", "models.Order").init();
                  }
              }
              """,
            spec -> spec.path("Application.cfc")
          ),
          cfc(
            """
              component {
                  function run() {
                      new models.Order().init().save();
                      application.orders.save();
                  }
              }
              """,
            spec -> spec.path("handlers/Orders.cfc").beforeRecipe(cu ->
              // what Application.cfc assigns to the application scope types it everywhere
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.Order.save", "models.Order.init", "models.Order.save"))
          )
        );
    }

    @Test
    void interfacesDeclareTheirImplementersMethods() {
        rewriteRun(
          cfc("interface { function greet(); }", spec -> spec.path("models/IGreeter.cfc")),
          cfc(
            """
              component {
                  function run(required models.IGreeter greeter) {
                      greeter.greet();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly("models.IGreeter.greet"))
          )
        );
    }

    @Test
    void returnTypesFromWhatFunctionsReturn() {
        rewriteRun(
          cfc(
            """
              component {
                  property name="dao" type="UserDAO";
                  function getDAO() { return variables.dao; }
                  function create() { return new User(); }
              }
              """,
            spec -> spec.path("models/UserService.cfc")
          ),
          cfc("component { function find() {} }", spec -> spec.path("models/UserDAO.cfc")),
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfc(
            """
              component {
                  function run(required models.UserService service) {
                      service.getDAO().find();
                      service.create().save();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.UserDAO.find", "models.UserService.getDAO", "models.User.save", "models.UserService.create"))
          )
        );
    }

    @Test
    void templateVariablesAndJavaClasses() {
        rewriteRun(
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfm(
            """
              <cfset user = new models.User()>
              <cfset builder = createObject("java", "java.lang.StringBuilder").init()>
              <cfoutput>#user.save()# #builder.append("x").toString()#</cfoutput>
              <cfset home = createObject("java", "java.lang.System").getProperty("user.home")>
              """,
            spec -> spec.path("views/index.cfm").beforeRecipe(cu ->
              // what a template assigns at its top level types the calls after it, and Java's classes are Java's types
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "java.lang.StringBuilder.<constructor>", "cfml.createObject", "models.User.save",
                "java.lang.StringBuilder.toString", "java.lang.StringBuilder.append",
                "java.lang.System.getProperty", "cfml.createObject"))
          )
        );
    }

    @Test
    void aRepositoryNamesItsComponentsByItsOwnName() {
        rewriteRun(
          spec -> spec.relativeTo(Paths.get("jsonutil")),
          cfc("component { function serialize(data) {} }", spec -> spec.path("jsonutil/JSONUtil.cfc")),
          cfc(
            """
              component {
                  function run() {
                      createObject("component", "jsonutil.JSONUtil").serialize({});
                  }
              }
              """,
            spec -> spec.path("jsonutil/tests/SerializeTest.cfc").beforeRecipe(cu ->
              // deployed into a web root under its name, a library names its components that way
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("JSONUtil.serialize", "cfml.createObject"))
          )
        );
    }

    private static void coldBox(Path coldbox) throws IOException {
        Files.createDirectories(coldbox.resolve("system/web/context"));
        Files.write(coldbox.resolve("system/FrameworkSupertype.cfc"),
          "component { function relocate(event) {} }\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/EventHandler.cfc"),
          "component extends=\"coldbox.system.FrameworkSupertype\" {}\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/web/Renderer.cfc"),
          "component extends=\"coldbox.system.FrameworkSupertype\" { function view(view) {} }\n".getBytes(StandardCharsets.UTF_8));
        Files.write(coldbox.resolve("system/web/context/RequestContext.cfc"),
          "component { function getValue(name) {} function setView(view) {} }\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void coldBoxConventions(@TempDir Path coldbox) throws IOException {
        coldBox(coldbox);
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("coldbox", coldbox)),
          cfc(
            """
              component {
                  function configure() {
                      coldbox = {appName: "app"};
                  }
              }
              """,
            spec -> spec.path("config/ColdBox.cfc")
          ),
          cfc(
            """
              component {
                  function index(event, rc, prc) {
                      event.setView("main/index");
                      relocate("main");
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu -> {
                // a handler extends EventHandler virtually, and its event is the request context
                assertThat(cu.getClasses().get(0).getType().getSupertype().getFullyQualifiedName())
                  .isEqualTo("coldbox.system.EventHandler");
                assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                  "coldbox.system.web.context.RequestContext.setView", "coldbox.system.FrameworkSupertype.relocate");
            })
          ),
          cfm(
            """
              <cfoutput>#view("partials/nav")# #event.getValue("name")#</cfoutput>
              """,
            spec -> spec.path("views/main/index.cfm").beforeRecipe(cu ->
              // a view runs in the renderer
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "coldbox.system.web.Renderer.view", "coldbox.system.web.context.RequestContext.getValue"))
          )
        );
    }

    @Test
    void coldBoxConventionsHoldOnlyInColdBoxApplications(@TempDir Path coldbox) throws IOException {
        coldBox(coldbox);
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("coldbox", coldbox)),
          // another framework's application, where ColdBox only resolves
          cfc(
            "component { function index(event) { event.getValue(\"name\"); } }",
            spec -> spec.path("other/handlers/Main.cfc").beforeRecipe(cu -> {
                assertThat(cu.getClasses().get(0).getType().getSupertype()).isNull();
                assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly((String) null);
            })
          ),
          cfm(
            "<cfoutput>#view(\"nav\")# #event.getValue(\"name\")#</cfoutput>",
            spec -> spec.path("other/views/main/index.cfm").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(null, null))
          ),
          // a ColdBox module
          cfc("component { function configure() {} }", spec -> spec.path("modules/blog/ModuleConfig.cfc")),
          cfm(
            "<cfoutput>#event.getValue(\"name\")#</cfoutput>",
            spec -> spec.path("modules/blog/views/post/show.cfm").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("coldbox.system.web.context.RequestContext.getValue"))
          ),
          // an application whose configuration has another name but sets coldbox all the same
          cfc(
            "component { function configure() { variables.coldbox = {appName: \"site\"}; } }",
            spec -> spec.path("site/config/Config.cfc")
          ),
          cfm(
            "<cfoutput>#event.getValue(\"name\")#</cfoutput>",
            spec -> spec.path("site/layouts/Main.cfm").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("coldbox.system.web.context.RequestContext.getValue"))
          ),
          // a site whose configuration extends that of a framework built on ColdBox
          cfc(
            "component { function configure() { variables.coldbox = {appName: \"cms\"}; } }",
            spec -> spec.path("cms/BaseConfig.cfc")
          ),
          cfc("component extends=\"cms.BaseConfig\" {}", spec -> spec.path("mysite/config/Config.cfc")),
          cfc(
            "component { function index(event) { event.getValue(\"name\"); } }",
            spec -> spec.path("mysite/handlers/Main.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("coldbox.system.web.context.RequestContext.getValue"))
          ),
          // older ColdBox applications, configured in XML or run from an index.cfm that includes the framework
          cfm("<Config><Settings /></Config>", spec -> spec.path("legacy/config/coldbox.xml.cfm")),
          cfc(
            "component { function index(event) { event.getValue(\"name\"); } }",
            spec -> spec.path("legacy/handlers/Main.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("coldbox.system.web.context.RequestContext.getValue"))
          ),
          cfm("<cfinclude template=\"/coldbox/system/coldbox.cfm\">", spec -> spec.path("oldest/index.cfm")),
          cfm(
            "<cfoutput>#event.getValue(\"name\")#</cfoutput>",
            spec -> spec.path("oldest/views/home.cfm").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("coldbox.system.web.context.RequestContext.getValue"))
          )
        );
    }

    @Test
    void mockBoxFunctionsReturnTheMockTheyAreCalledOn(@TempDir Path testbox) throws IOException {
        Files.createDirectories(testbox.resolve("system"));
        Files.write(testbox.resolve("system/MockBox.cfc"), (
          "component {\n" +
          "    function init() { return this; }\n" +
          "    function $(method, returns) { return this; }\n" +
          "    function $results() { return this; }\n" +
          "    private function decorateMock(target) {\n" +
          "        var obj = target;\n" +
          "        obj.$ = variables.$;\n" +
          "        obj.$results = variables.$results;\n" +
          "    }\n" +
          "}\n").getBytes(StandardCharsets.UTF_8));
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("testbox", testbox)),
          cfc(
            """
              component {
                  function run() {
                      var mockBox = new testbox.system.MockBox().init();
                      var mock = mockBox.$("save", true);
                      mock.$results(false);
                  }
              }
              """,
            spec -> spec.path("tests/UserTest.cfc").beforeRecipe(cu ->
              // MockBox puts $ on each mock, whose this is then the mock, so what it returns is no MockBox
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy)
                .containsExactly("testbox.system.MockBox.init", "testbox.system.MockBox.$", null))
          )
        );
    }

    @Test
    void queriesAreQueriesUnlessTheyAskForAnotherType() {
        rewriteRun(
          cfc(
            """
              <cfcomponent>
                  <cffunction name="findUsers">
                      <cfquery name="local.users" datasource="app">select * from users</cfquery>
                      <cfreturn local.users>
                  </cffunction>
                  <cffunction name="run">
                      <cfquery name="q">select 1</cfquery>
                      <cfset q.recordCount()>
                      <cfquery name="rows" returntype="array">select 1</cfquery>
                      <cfset rows.len()>
                      <cfset findUsers().columnList()>
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.path("models/UserGateway.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "cfml.queryRecordCount", "cfml.arrayLen", "cfml.queryColumnList", "models.UserGateway.findUsers"))
          ),
          cfc(
            """
              component {
                  function run(sql, options) {
                      var users = queryExecute("select * from users");
                      users.recordCount();
                      var byId = queryExecute(sql, {}, {datasource: "app"});
                      byId.each(function(row) {});
                      var asArray = queryExecute(sql, {}, {returntype: "array"});
                      asArray.len();
                      var unknown = queryExecute(sql, {}, options);
                      unknown.recordCount();
                  }
              }
              """,
            spec -> spec.path("models/UserService.cfc").beforeRecipe(cu ->
              // options only known when the function runs may ask for any type
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "cfml.queryExecute", "cfml.queryRecordCount", "cfml.queryExecute", "cfml.queryEach",
                "cfml.queryExecute", "cfml.arrayLen", "cfml.queryExecute", null))
          )
        );
    }

    @Test
    void memberFunctionsAreTheBuiltInsTheyAlias() {
        rewriteRun(
          cfc(
            """
              component {
                  function run(required array items, struct options) {
                      items.append("x").len();
                      var names = [];
                      names.sort("text");
                      options.keyExists("a");
                      var greeting = "hello";
                      greeting.trim().toUpperCase();
                      queryNew("id").addRow();
                  }
              }
              """,
            spec -> spec.beforeRecipe(cu ->
              // arrayAppend returns its array, and a string is a java.lang.String too
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "cfml.arrayLen", "cfml.arrayAppend", "cfml.arraySort", "cfml.structKeyExists",
                "java.lang.String.toUpperCase", "cfml.trim", "cfml.queryAddRow", "cfml.queryNew"))
          )
        );
    }

    @Test
    void structKeysAndVariablesWhoseAssignmentsAgree() {
        rewriteRun(
          cfc("component { models.User function find() {} }", spec -> spec.path("models/UserService.cfc")),
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfc(
            """
              component {
                  function run(required models.UserService userService, prc) {
                      prc.user = userService.find();
                      prc.user.save();
                      var thing = new models.User();
                      thing = "other";
                      thing.save();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              // a variable assigned two types has neither
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.UserService.find", "models.User.save", null))
          )
        );
    }

    @Test
    void otherTagsAndFunctionsThatGiveQueries() {
        rewriteRun(
          cfc(
            """
              <cfcomponent>
                  <cffunction name="run">
                      <cfdirectory action="list" directory="/tmp" name="files">
                      <cfset files.recordCount()>
                      <cfdirectory directory="/tmp" name="listed">
                      <cfset listed.recordCount()>
                      <cfdbinfo type="tables" name="tables">
                      <cfset tables.recordCount()>
                      <cfstoredproc procedure="findUsers"><cfprocresult name="rows"></cfstoredproc>
                      <cfset rows.recordCount()>
                      <cfldap action="query" name="people" server="ldap" start="dc=example" attributes="cn">
                      <cfset people.recordCount()>
                      <cfzip action="list" file="/tmp/a.zip" name="entries">
                      <cfset entries.recordCount()>
                      <cfzip file="/tmp/a.zip" source="/tmp/b" name="zipped">
                      <cfset zipped.recordCount()>
                  </cffunction>
              </cfcomponent>
              """,
            spec -> spec.path("models/Files.cfc").beforeRecipe(cu ->
              // a zip that is made, not listed, is no query
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "cfml.queryRecordCount", "cfml.queryRecordCount", "cfml.queryRecordCount", "cfml.queryRecordCount",
                "cfml.queryRecordCount", "cfml.queryRecordCount", null))
          ),
          cfc(
            """
              component {
                  function run(path, info) {
                      directory action="list" directory=path name="found";
                      found.recordCount();
                      var paths = directoryList(path);
                      paths.len();
                      var all = directoryList(path, true, "query");
                      all.recordCount();
                      var some = directoryList(path, false, info);
                      some.len();
                  }
              }
              """,
            spec -> spec.path("models/Finder.cfc").beforeRecipe(cu ->
              // directoryList gives an array of paths unless its listInfo asks for a query
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "cfml.queryRecordCount", "cfml.directoryList", "cfml.arrayLen", "cfml.directoryList",
                "cfml.queryRecordCount", "cfml.directoryList", null))
          )
        );
    }

    @Test
    void aFileHasTheTypeItIs() {
        rewriteRun(
          cfm(
            "<p>About us</p>",
            spec -> spec.path("pages/about.cfm").beforeRecipe(cu ->
              // a template is a type even when it declares no function
              assertThat(cu.getType().getFullyQualifiedName()).isEqualTo("pages.about"))
          ),
          cfc(
            "component {}",
            spec -> spec.path("models/User.cfc").beforeRecipe(cu ->
              assertThat(cu.getType()).isSameAs(cu.getClasses().get(0).getType()))
          )
        );
    }

    @Test
    void aFilesOwnTypeIsDeclaredNotUsed() {
        rewriteRun(
          cfc(
            "component { function save() {} }",
            spec -> spec.path("models/User.cfc").beforeRecipe(cu ->
              assertThat(cu.getTypesInUse().getTypesInUse()).noneMatch(t -> TypeUtils.isOfClassType(t, "models.User")))
          ),
          cfm(
            "<p>About us</p>",
            spec -> spec.path("pages/about.cfm").beforeRecipe(cu ->
              assertThat(cu.getTypesInUse().getTypesInUse()).noneMatch(t -> TypeUtils.isOfClassType(t, "pages.about")))
          ),
          cfc(
            "component { function run() { new models.User().save(); } }",
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              assertThat(cu.getTypesInUse().getTypesInUse()).anyMatch(t -> TypeUtils.isOfClassType(t, "models.User")))
          )
        );
    }

    @Test
    void templatesAreTypesAndIncludesMixThemIn() {
        rewriteRun(
          cfm(
            """
              <cfscript>
              function formatName(first, last) {
                  return first & " " & last;
              }
              </cfscript>
              """,
            spec -> spec.path("includes/udfs.cfm")
          ),
          cfm(
            """
              <cfinclude template="../includes/udfs.cfm">
              <cffunction name="greet"><cfreturn "hi"></cffunction>
              <cfoutput>#formatName("Ada", "Lovelace")# #greet()#</cfoutput>
              """,
            spec -> spec.path("pages/index.cfm").beforeRecipe(cu ->
              // a template's own functions are its methods, and an included template's are too
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "includes.udfs.formatName", "pages.index.greet"))
          ),
          cfc(
            """
              component {
                  include "/includes/udfs.cfm";

                  function run() {
                      return formatName("a", "b");
                  }
              }
              """,
            spec -> spec.path("models/Person.cfc").beforeRecipe(cu ->
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly("includes.udfs.formatName"))
          )
        );
    }

    @Test
    void returnTypesFollowCallsAcrossTheRepository() {
        rewriteRun(
          cfc("component { models.User function find(id) {} }", spec -> spec.path("models/UserDAO.cfc")),
          cfc(
            """
              component {
                  property name="dao" type="models.UserDAO";

                  function get(id) {
                      return variables.dao.find(id);
                  }

                  function current() {
                      var user = get(1);
                      return user;
                  }

                  function either(flag) {
                      if (flag) {
                          return get(1);
                      }
                      return "nobody";
                  }
              }
              """,
            spec -> spec.path("models/UserService.cfc")
          ),
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfc(
            """
              component {
                  function run(required models.UserService service) {
                      service.get(1).save();
                      service.current().save();
                      service.either(true).save();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              // returns that disagree give no type
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.User.save", "models.UserService.get", "models.User.save", "models.UserService.current",
                null, "models.UserService.either"))
          )
        );
    }

    @Test
    void placeholderDeclarationsTakeTheTypeAssignedLater() {
        rewriteRun(
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfc("component { function ship() {} }", spec -> spec.path("models/Order.cfc")),
          cfc(
            """
              component {
                  function findUser() {
                      var result = "";
                      result = new models.User();
                      return result;
                  }

                  function label() {
                      var text = "";
                      text = text & "order ";
                      text &= "label";
                      return text;
                  }

                  function run() {
                      var user = "";
                      var order = 0;
                      var thing = "";
                      var html = "";
                      user = new models.User();
                      user.save();
                      order = new models.Order();
                      order.ship();
                      thing = new models.User();
                      thing = new models.Order();
                      thing.ship();
                      html &= "<li>";
                      html.len();
                      findUser().save();
                      label().trim();
                  }
              }
              """,
            spec -> spec.path("models/Repository.cfc").beforeRecipe(cu ->
              // a placeholder's type stands until the function assigns the variable, and then what it assigns has to agree
              assertThat(calls(cu)).extracting(TypeAttributionTest::declaredBy).containsExactly(
                "models.User.save", "models.Order.ship", null, "cfml.stringLen",
                "models.User.save", "models.Repository.findUser", "cfml.trim", "models.Repository.label"))
          )
        );
    }

    @Test
    void returnTypesInferredTheSameWhenParsedInParallel() throws Exception {
        // each function returns what the next one returns, so inference has to follow forty of them
        List<Parser.Input> inputs = new ArrayList<>();
        inputs.add(Parser.Input.fromString(Paths.get("models/Leaf.cfc"), "component { function done() {} }"));
        for (int i = 0; i < 40; i++) {
            inputs.add(Parser.Input.fromString(Paths.get("models/C" + i + ".cfc"), i < 39 ?
              "component {\n    property name=\"next\" type=\"models.C" + (i + 1) + "\";\n" +
              "    function get() { return variables.next.get(); }\n}\n" :
              "component { function get() { return new models.Leaf(); } }\n"));
        }
        inputs.add(Parser.Input.fromString(Paths.get("models/Caller.cfc"),
          "component { function run(required models.C0 c) { c.get().done(); } }\n"));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<List<String>>> parses = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                parses.add(pool.submit(() -> {
                    List<String> typed = new ArrayList<>();
                    ColdFusionParser.builder().build().parseInputs(inputs, null, new InMemoryExecutionContext())
                      .forEach(source -> {
                          for (J.MethodInvocation call : calls((Cf.CompilationUnit) source)) {
                              JavaType.FullyQualified returned = call.getMethodType() == null ? null :
                                TypeUtils.asFullyQualified(call.getMethodType().getReturnType());
                              typed.add(source.getSourcePath() + ": " + declaredBy(call) + " returns " +
                                        (returned == null ? null : returned.getFullyQualifiedName()));
                          }
                      });
                    return typed;
                }));
            }
            List<String> first = parses.get(0).get();
            assertThat(first).contains("models/Caller.cfc: models.Leaf.done returns null",
              "models/Caller.cfc: models.C0.get returns models.Leaf");
            for (Future<List<String>> parse : parses) {
                assertThat(parse.get()).isEqualTo(first);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ambiguousNamesStayUntyped() {
        rewriteRun(
          cfc("component {}", spec -> spec.path("a/Service.cfc")),
          cfc("component {}", spec -> spec.path("b/Service.cfc")),
          cfc(
            """
              component extends="Service" {
                  function run() { return new Missing(); }
              }
              """,
            spec -> spec.path("c/Handler.cfc").beforeRecipe(cu -> {
                J.ClassDeclaration handler = cu.getClasses().get(0);
                assertThat(handler.getType().getSupertype()).isNull();
                assertThat(handler.getExtends().getType()).isNull();
            })
          )
        );
    }
}
