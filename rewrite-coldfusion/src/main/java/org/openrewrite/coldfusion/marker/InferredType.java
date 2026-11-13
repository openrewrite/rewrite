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
package org.openrewrite.coldfusion.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.coldfusion.TypeInference;
import org.openrewrite.marker.Marker;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Says that a tree's type rests on a {@link TypeInference} rather than on the parser's own rules, and on what
 * evidence. A recipe that must be certain can leave such trees out. In {@link TypeInference#attribute} an inference
 * marks every tree whose type rests on it:
 * <ul>
 *     <li>a call whose method type it gave, or whose function's return type it gave</li>
 *     <li>a function's declaration, when it gave the function's return type or a parameter's type</li>
 *     <li>a parameter whose type it gave</li>
 *     <li>any other tree whose type it gave, or that the parser typed through the types it gave</li>
 * </ul>
 * The name of a call or of a declaration shares the marker of its call or declaration. Any other tree without the
 * marker has only the types the parser's own rules give.
 * <p>
 * Every tree with the same evidence shares one marker, which {@link #of} gives and which reading an LST gives back. So
 * an LST stores each kind once, and a marked tree costs only a reference to it.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InferredType implements Marker {
    private static final Map<Evidence, InferredType> SHARED = new EnumMap<>(Evidence.class);

    static {
        for (Evidence evidence : Evidence.values()) {
            UUID id = UUID.nameUUIDFromBytes((InferredType.class.getName() + "." + evidence.name()).getBytes(UTF_8));
            SHARED.put(evidence, new InferredType(id, evidence));
        }
    }

    @With
    UUID id;

    Evidence evidence;

    /**
     * The marker that every tree whose type rests on this evidence shares.
     */
    public static InferredType of(Evidence evidence) {
        return SHARED.get(evidence);
    }

    // the id stays a parameter so that a reader never writes one into the shared marker's field
    @JsonCreator
    private static InferredType read(@JsonProperty("id") UUID id, @JsonProperty("evidence") Evidence evidence) {
        return of(evidence);
    }

    /**
     * What an inferred type rests on, strongest first. A type that rests on several kinds rests on the weakest of them.
     */
    public enum Evidence {
        /**
         * The values that flow into a variable, a parameter or a return across the repository, which all agree:
         * assignments, the arguments of every call that reaches a function, and what its returns return.
         */
        FLOW,

        /**
         * A framework's documented convention that names a component in a string, as {@code entityNew("User")} does.
         */
        FRAMEWORK,

        /**
         * The functions code calls on a value, when one component alone declares them all.
         */
        USAGE
    }
}
