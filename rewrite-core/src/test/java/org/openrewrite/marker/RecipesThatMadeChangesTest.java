/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.marker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.FindAndReplace;

import java.time.Duration;
import java.util.*;


import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.tuple;

class RecipesThatMadeChangesTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void recipeStackTravelsAsIdentity() {
        List<RpcObjectData> sent = send(RecipesThatMadeChanges.create(List.of(new ChangeText("hello"))));

        // The codec decomposes the marker, so no message may carry a Recipe instance inline.
        assertThat(sent).extracting(RpcObjectData::getValue)
          .noneMatch(v -> v instanceof Recipe);
        assertThat(sent).extracting(RpcObjectData::getValueType)
          .filteredOn(Objects::nonNull)
          .containsOnly(RecipesThatMadeChanges.class.getName(), RecipeIdentity.class.getName());
        assertThat(sent).extracting(RpcObjectData::getValue)
          .contains("org.openrewrite.text.ChangeText", "Change text", "Change text to `hello`");
    }

    @Test
    void recipeRunStateIsUnreachableFromThePayload() {
        RecipesThatMadeChanges marker = RecipesThatMadeChanges.create(List.of(new RecipeHoldingRunState()));

        assertThatNoException().isThrownBy(() -> mapper.writeValueAsString(send(marker)));
    }

    @Test
    void identitySurvivesTheRoundTrip() {
        Recipe received = roundTrip(RecipesThatMadeChanges.create(List.of(new ChangeText("hello"))))
          .getRecipes().iterator().next().get(0);

        assertThat(received.getName()).isEqualTo("org.openrewrite.text.ChangeText");
        assertThat(received.getDisplayName()).isEqualTo("Change text");
        assertThat(received.getInstanceName()).isEqualTo("Change text to `hello`");
        assertThat(received.getEstimatedEffortPerOccurrence()).isEqualTo(Duration.ofMinutes(5));
        assertThat(received.getDescriptor().getOptions())
          .singleElement()
          .satisfies(option -> {
              assertThat(option.getName()).isEqualTo("toText");
              assertThat(option.getValue()).isEqualTo("hello");
          });
    }

    @Test
    void aConfiguredRecipeKeepsItsRenderedInstanceNameAndOptions() {
        FindAndReplace findAndReplace = new FindAndReplace("blacklist", "denylist", true, false,
          null, null, "**/*.java", null);

        // What a rendered diff attributes a change to, so a degraded form here is user-visible.
        Recipe received = roundTrip(RecipesThatMadeChanges.create(List.of(findAndReplace)))
          .getRecipes().iterator().next().get(0);

        assertThat(received.getInstanceName()).isEqualTo(findAndReplace.getInstanceName());
        assertThat(received.getDescriptor().getOptions())
          .extracting(OptionDescriptor::getName, OptionDescriptor::getValue)
          .containsExactlyInAnyOrder(
            tuple("find", "blacklist"),
            tuple("replace", "denylist"),
            tuple("regex", true),
            tuple("caseSensitive", false),
            tuple("filePattern", "**/*.java"));
    }

    @Test
    void aMultiFrameStackKeepsItsOrder() {
        RecipesThatMadeChanges marker = RecipesThatMadeChanges.create(
          List.of(new ChangeText("outer"), new ChangeText("inner")));

        assertThat(roundTrip(marker).getRecipes()).singleElement().satisfies(stack ->
          assertThat(stack).extracting(Recipe::getInstanceName)
            .containsExactly("Change text to `outer`", "Change text to `inner`"));
    }

    @Test
    void aReceivedMarkerCanBeSentOnUnchanged() {
        RecipesThatMadeChanges received = roundTrip(
          RecipesThatMadeChanges.create(List.of(new ChangeText("hello"))));

        // A host may forward a marker it received; identity must survive the second hop rather
        // than degrading each time.
        assertThat(roundTrip(received).getRecipes().iterator().next().get(0).getInstanceName())
          .isEqualTo("Change text to `hello`");
    }

    @Test
    void aStackAddedToAnExistingMarkerArrivesIntact() {
        ChangeText first = new ChangeText("first");
        RecipesThatMadeChanges before = RecipesThatMadeChanges.create(List.of(first));
        RecipesThatMadeChanges after = before.withRecipes(
          List.of(List.of(first), List.of(new ChangeText("second"))));

        // Stacks are keyed by recipe name, so two configurations of one recipe key alike and the
        // frame diff, not the position, is what separates them.
        RecipesThatMadeChanges received = roundTrip(after, before);

        assertThat(received.getRecipes())
          .extracting(stack -> stack.get(0).getInstanceName())
          .containsExactly("Change text to `first`", "Change text to `second`");
    }

    @Test
    void anUntouchedMarkerIsNotRebuiltFromTheWire() {
        RecipesThatMadeChanges marker = RecipesThatMadeChanges.create(List.of(new ChangeText("hello")));

        // Peers leave this marker alone, so it diffs as NO_CHANGE even when its neighbours
        // change and the host keeps its own live recipes rather than the identities it served.
        assertThat(roundTripAlongsideAnAddedMarker(marker)).isSameAs(marker);
    }

    @Test
    void theIdentityFrameIsNotEnumeratedAsARecipe() {
        ClasspathScanningLoader loader = new ClasspathScanningLoader(
          new Properties(), new String[]{RecipeIdentity.class.getPackageName()});

        // It is a Recipe only so that a received marker satisfies getRecipes(). Enumerating it
        // would construct one with no fields set, putting a null-named descriptor into every
        // catalog built from this classpath.
        assertThat(loader.listRecipeDescriptors())
          .extracting(RecipeDescriptor::getName)
          .doesNotContainNull();
    }

    /** Stands in for an {@link org.openrewrite.rpc.RpcRecipe}, which holds live run state. */
    @Getter
    static class RecipeHoldingRunState extends Recipe {
        String displayName = "Recipe holding run state";
        String description = "Reaches live run state that no JSON mapper can serialize.";
        Object runState = new Object();
    }

    private static List<RpcObjectData> send(RecipesThatMadeChanges marker) {
        return send(marker, null);
    }

    private static List<RpcObjectData> send(RecipesThatMadeChanges after,
                                            @Nullable RecipesThatMadeChanges before) {
        List<RpcObjectData> sent = new ArrayList<>();
        RpcSendQueue q = new RpcSendQueue(100, sent::addAll, new IdentityHashMap<>(), null, false);
        q.send(after, before, null);
        q.flush();
        return sent;
    }

    private static RecipesThatMadeChanges roundTrip(RecipesThatMadeChanges marker) {
        return roundTrip(marker, null);
    }

    private static RecipesThatMadeChanges roundTrip(RecipesThatMadeChanges after,
                                                    @Nullable RecipesThatMadeChanges before) {
        // Passing through JSON is what turns the sender's live instance into the maps the receiver
        // sees; handing the messages over in memory would prove nothing.
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        batches.addLast(send(after, before).stream().map(RecipesThatMadeChangesTest::reserialize).collect(toList()));
        return new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null).receive(before);
    }

    /** Sends a marker list that gained an entry, as a peer that marked up the source file would. */
    private static RecipesThatMadeChanges roundTripAlongsideAnAddedMarker(RecipesThatMadeChanges marker) {
        Markers before = Markers.build(List.of(marker));
        Markers after = before.add(new SearchResult(Tree.randomId(), "found"));

        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue q = new RpcSendQueue(100, batches::addLast, new IdentityHashMap<>(), null, false);
        q.send(after, before, null);
        q.flush();

        List<RpcObjectData> wire = batches.removeFirst().stream()
          .map(RecipesThatMadeChangesTest::reserialize).collect(toList());
        batches.addLast(wire);
        Markers received = new RpcReceiveQueue(new HashMap<>(), batches::removeFirst, null, null).receive(before);
        return received.findFirst(RecipesThatMadeChanges.class).orElseThrow(AssertionError::new);
    }

    private static RpcObjectData reserialize(RpcObjectData data) {
        try {
            return mapper.readValue(mapper.writeValueAsString(data), RpcObjectData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
