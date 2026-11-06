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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.marker.InferredType;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.coldfusion.Assertions.cfc;

class TypeInferenceTest implements RewriteTest {

    /**
     * Records the steps the parser calls it for and the calls each tree has by their keys. It types each call that the
     * parser left untyped and is named {@code greet} as {@code models.Greeter}'s, by the key collect saw it under.
     */
    static class RecordingTypeInference implements TypeInference {
        final TypeContext context;
        final List<String> steps = new ArrayList<>();
        final Map<CallKey, String> collected = new LinkedHashMap<>();
        final Map<CallKey, String> attributed = new LinkedHashMap<>();
        final Set<CallKey> greetings = new HashSet<>();
        final List<String> typedWhenAttributed = new ArrayList<>();

        RecordingTypeInference(TypeContext context) {
            this.context = context;
        }

        @Override
        public void collect(Cf.CompilationUnit typed) {
            steps.add("collect " + typed.getSourcePath());
            calls(typed, collected);
            Map<UUID, CallKey> keys = context.callKeys(typed);
            for (J.MethodInvocation call : TypeInferenceTest.calls(typed)) {
                if ("greet".equals(call.getSimpleName()) && call.getMethodType() == null) {
                    greetings.add(keys.get(call.getId()));
                }
            }
        }

        @Override
        public void solve() {
            steps.add("solve");
        }

        @Override
        public Cf.CompilationUnit attribute(Cf.CompilationUnit typed) {
            steps.add("attribute " + typed.getSourcePath());
            calls(typed, attributed);
            for (J.MethodInvocation call : TypeInferenceTest.calls(typed)) {
                if (call.getMethodType() != null) {
                    typedWhenAttributed.add(call.getMethodType().getDeclaringType().getFullyQualifiedName() + "." +
                                            call.getMethodType().getName());
                }
            }
            JavaType.Method greet = context.method(context.resolve("models.Greeter", null), "greet");
            Map<UUID, CallKey> keys = context.callKeys(typed);
            return (Cf.CompilationUnit) new ColdFusionIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, p);
                    if (greet == null || !greetings.contains(keys.get(m.getId()))) {
                        return m;
                    }
                    return m.withName(m.getName().withType(greet)).withMethodType(greet)
                      .withMarkers(m.getMarkers().add(InferredType.of(InferredType.Evidence.FLOW)));
                }
            }.visitNonNull(typed, 0);
        }

        private void calls(Cf.CompilationUnit cu, Map<CallKey, String> byKey) {
            Map<UUID, CallKey> keys = context.callKeys(cu);
            for (J.MethodInvocation call : TypeInferenceTest.calls(cu)) {
                byKey.put(keys.get(call.getId()), call.getSimpleName() + "/" + call.getArguments().size());
            }
        }
    }

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
    void stepsRunInOrderOnTypedTreesWhoseCallsKeepTheirKeys() {
        List<RecordingTypeInference> inferences = new ArrayList<>();
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().typeInference(context -> {
              RecordingTypeInference inference = new RecordingTypeInference(context);
              inferences.add(inference);
              return inference;
          })),
          cfc("component { function greet() { return \"hello\"; } }", spec -> spec.path("models/Greeter.cfc")),
          cfc(
            """
              component {
                  function run(greeter) {
                      greeter.greet();
                      return arrayLen([greeter.greet()]);
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu -> {
                List<J.MethodInvocation> calls = calls(cu);
                // the calls the parser left untyped have the type the inference gave them, by their keys
                assertThat(calls).extracting(TypeInferenceTest::declaredBy)
                  .containsExactly("models.Greeter.greet", "cfml.arrayLen", "models.Greeter.greet");
                assertThat(calls).extracting(call -> call.getMarkers().findFirst(InferredType.class)
                    .map(InferredType::getEvidence).orElse(null))
                  .containsExactly(InferredType.Evidence.FLOW, null, InferredType.Evidence.FLOW);
            })
          )
        );
        assertThat(inferences).hasSize(1);
        RecordingTypeInference inference = inferences.get(0);
        assertThat(inference.steps).containsExactly("collect models/Greeter.cfc", "collect handlers/Main.cfc", "solve",
          "attribute models/Greeter.cfc", "attribute handlers/Main.cfc");
        // attribute sees the tree the parser typed
        assertThat(inference.typedWhenAttributed).containsExactly("cfml.arrayLen");
        assertThat(inference.attributed).isEqualTo(inference.collected);
        assertThat(inference.greetings).hasSize(2);
    }

    /**
     * Gives the repository's functions the types it is told to in solve, and records what the context says to each.
     */
    static class SignatureTypeInference implements TypeInference {
        final TypeContext context;
        final List<Object> answers = new ArrayList<>();

        SignatureTypeInference(TypeContext context) {
            this.context = context;
        }

        @Override
        public void collect(Cf.CompilationUnit typed) {
            JavaType.Method find = context.method(context.resolve("models.Repository", null), "find");
            try {
                context.setReturnType(Objects.requireNonNull(find), Objects.requireNonNull(context.resolve("models.User", null)));
            } catch (IllegalStateException e) {
                answers.add("collect may not");
            }
        }

        @Override
        public void solve() {
            JavaType.FullyQualified user = Objects.requireNonNull(context.resolve("models.User", null));
            JavaType.FullyQualified repository = context.resolve("models.Repository", null);
            answers.add(context.setReturnType(Objects.requireNonNull(context.method(repository, "find")), user));
            answers.add(context.setReturnType(Objects.requireNonNull(context.method(repository, "count")), user));
            answers.add(context.setParameterType(Objects.requireNonNull(context.method(repository, "save")), 0, user));
            answers.add(context.setParameterType(Objects.requireNonNull(context.method(repository, "save")), 1,
              JavaType.Primitive.String));
            JavaType.FullyQualified spec = context.resolve("testbox.system.BaseSpec", null);
            answers.add(context.isDependency(spec));
            answers.add(context.setReturnType(Objects.requireNonNull(context.method(spec, "expect")), user));
        }

        @Override
        public Cf.CompilationUnit attribute(Cf.CompilationUnit typed) {
            return typed;
        }
    }

    @Test
    void solveGivesFunctionsTheTypesTheParserTypesFilesWith(@TempDir Path testbox) throws IOException {
        Files.createDirectories(testbox.resolve("system"));
        Files.write(testbox.resolve("system/BaseSpec.cfc"),
          "component { function expect(actual) {} }\n".getBytes(StandardCharsets.UTF_8));
        List<SignatureTypeInference> inferences = new ArrayList<>();
        rewriteRun(
          spec -> spec.parser(ColdFusionParser.builder().mapping("testbox", testbox).typeInference(context -> {
              SignatureTypeInference inference = new SignatureTypeInference(context);
              inferences.add(inference);
              return inference;
          })),
          cfc("component { function save() {} }", spec -> spec.path("models/User.cfc")),
          cfc(
            """
              component {
                  function find(id) { return lookup(id); }
                  numeric function count() { return 0; }
                  function save(user, name) {}
                  function lookup(id) {}
              }
              """,
            spec -> spec.path("models/Repository.cfc")
          ),
          cfc(
            """
              component {
                  function run(required models.Repository repository) {
                      repository.find(1).save();
                  }
              }
              """,
            spec -> spec.path("handlers/Main.cfc").beforeRecipe(cu ->
              // the parser types the call on what find returns from the signature the inference gave it
              assertThat(calls(cu)).extracting(TypeInferenceTest::declaredBy)
                .containsExactly("models.User.save", "models.Repository.find"))
          )
        );
        // a declared type, a parameter's string, and a dependency's function keep what they have
        assertThat(inferences.get(0).answers).containsExactly(
          "collect may not", "collect may not", "collect may not", true, false, true, false, true, false);
    }

    @Test
    void theContextKnowsTheFunctionsAComponentAssignsToOtherObjects(@TempDir Path testbox) throws IOException {
        Files.createDirectories(testbox.resolve("system"));
        Files.write(testbox.resolve("system/MockBox.cfc"), (
          "component {\n" +
          "    function init() { return this; }\n" +
          "    function $(method, returns) { return this; }\n" +
          "    private function decorateMock(target) {\n" +
          "        var obj = target;\n" +
          "        obj.$ = variables.$;\n" +
          "    }\n" +
          "}\n").getBytes(StandardCharsets.UTF_8));
        Map<String, Boolean> assigned = new LinkedHashMap<>();
        ColdFusionParser.builder().mapping("testbox", testbox).typeInference(context -> new TypeInference() {
            @Override
            public void collect(Cf.CompilationUnit typed) {
            }

            @Override
            public void solve() {
                JavaType.FullyQualified mockBox = context.resolve("testbox.system.MockBox", null);
                for (String name : Arrays.asList("$", "init")) {
                    assigned.put(name, context.isAssignedToOtherObjects(Objects.requireNonNull(context.method(mockBox, name))));
                }
            }

            @Override
            public Cf.CompilationUnit attribute(Cf.CompilationUnit typed) {
                return typed;
            }
        }).build().parseInputs(singletonList(Parser.Input.fromString(Paths.get("tests/UserTest.cfc"), "component {}")),
          null, new InMemoryExecutionContext()).collect(Collectors.toList());
        // a mock calls $, which MockBox puts on it, so calling $ does not make a value a MockBox
        assertThat(assigned).containsExactly(entry("$", true), entry("init", false));
    }

    @Test
    void indexedFilesResolveWithoutBeingTypedOrReturned() {
        List<RecordingTypeInference> inferences = new ArrayList<>();
        ColdFusionParser parser = ColdFusionParser.builder().typeInference(context -> {
            RecordingTypeInference inference = new RecordingTypeInference(context);
            inferences.add(inference);
            return inference;
        }).build();
        List<SourceFile> parsed = parser.parseInputs(
          singletonList(Parser.Input.fromString(Paths.get("handlers/Main.cfc"),
            "component { function run() { new models.Greeter().greet(); } }")),
          singletonList(Parser.Input.fromString(Paths.get("models/Greeter.cfc"),
            "component { function greet() { return \"hello\"; } }")),
          null, new InMemoryExecutionContext(t -> {
              throw new AssertionError(t);
          })).collect(Collectors.toList());
        assertThat(parsed).extracting(SourceFile::getSourcePath).containsExactly(Paths.get("handlers/Main.cfc"));
        assertThat(calls((Cf.CompilationUnit) parsed.get(0))).extracting(TypeInferenceTest::declaredBy)
          .containsExactly("models.Greeter.greet");
        assertThat(inferences.get(0).steps)
          .containsExactly("collect handlers/Main.cfc", "solve", "attribute handlers/Main.cfc");
    }

    @Test
    void inferredTypeMarkersSurviveSerialization() throws IOException {
        // fields rather than getters, as an LST is serialized
        ObjectMapper mapper = JsonMapper.builder()
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
          .build()
          .registerModule(new ParameterNamesModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
          .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        InferredType marker = InferredType.of(InferredType.Evidence.USAGE);
        String json = mapper.writeValueAsString(new InferredType[]{marker, marker});
        assertThat(json).containsOnlyOnce("USAGE");
        assertThat(mapper.readValue(json, InferredType[].class)).containsExactly(marker, marker)
          .allSatisfy(read -> assertThat(read).isSameAs(marker));
        // a reader may give markers ids of its own
        InferredType renamed = marker.withId(Tree.randomId());
        assertThat(mapper.readValue(mapper.writeValueAsString(renamed), InferredType.class)).isSameAs(marker);
    }

    @Test
    void anInferenceThatFailsLeavesTheParsersTypes() {
        List<Throwable> errors = new ArrayList<>();
        List<SourceFile> parsed = ColdFusionParser.builder().typeInference(context -> new TypeInference() {
            @Override
            public void collect(Cf.CompilationUnit typed) {
            }

            @Override
            public void solve() {
            }

            @Override
            public Cf.CompilationUnit attribute(Cf.CompilationUnit typed) {
                throw new IllegalStateException("boom");
            }
        }).build().parseInputs(
          singletonList(Parser.Input.fromString(Paths.get("index.cfm"), "<cfset n = arrayLen([])>")),
          null, new InMemoryExecutionContext(errors::add)).collect(Collectors.toList());
        assertThat(errors).extracting(Throwable::getMessage).containsExactly("boom");
        assertThat(calls((Cf.CompilationUnit) parsed.get(0))).extracting(TypeInferenceTest::declaredBy)
          .containsExactly("cfml.arrayLen");
    }
}
