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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The descriptors whose resolution dependencies the resolution step is resolving, each with the resolver that takes
 * it; Yarn has no such check, so a chain that comes back to a step never ends there.
 */
final class ResolutionChain {

    /**
     * How Yarn ends a resolution that never does, with what it prints.
     */
    enum Ending {
        /**
         * The call that recursed throws, and its resolution fails like any other.
         */
        STACK_EXHAUSTED("YN0001 RangeError: Maximum call stack size exceeded"),

        /**
         * The resolution awaits itself and never settles; Yarn exits once nothing else is left to run.
         */
        EMPTY_EVENT_LOOP("ERROR: Yarn is terminating due to an unexpected empty event loop."),

        /**
         * The resolution never settles and keeps scheduling, even after Yarn has failed on something else.
         */
        HEAP_EXHAUSTED("crash: JavaScript heap out of memory in the resolution step");

        final String message;

        Ending(String message) {
            this.message = message;
        }
    }

    private final List<Descriptor> descriptors = new ArrayList<>();
    private final List<Resolver> resolvers = new ArrayList<>();

    // where the scheduled descriptor resolving now entered the chain: what follows it is one synchronous call in Yarn
    private int step;

    /**
     * A resolution that never ends, with what Yarn prints when it gives up and the chain that comes back.
     */
    @Getter
    static final class Endless extends RuntimeException {
        private final Ending ending;

        Endless(Ending ending, String chain) {
            super(ending.message + " (" + chain + ")");
            this.ending = ending;
        }
    }

    /**
     * A scheduled descriptor starts resolving; {@link #endStep} takes the value back when it is done.
     */
    int startStep() {
        int outer = step;
        step = descriptors.size();
        return outer;
    }

    void endStep(int outer) {
        descriptors.subList(step, descriptors.size()).clear();
        resolvers.subList(step, resolvers.size()).clear();
        step = outer;
    }

    /**
     * Throws Endless for a descriptor already on the chain, or for a range the same resolver takes a third time,
     * under a new name each time.
     */
    void enter(Descriptor descriptor, Resolver resolver) {
        int first = -1;
        int passes = 0;
        for (int i = 0; i < descriptors.size(); i++) {
            Descriptor earlier = descriptors.get(i);
            if (resolvers.get(i) != resolver || !earlier.range.equals(descriptor.range)) {
                continue;
            }
            if (earlier.descriptorHash.equals(descriptor.descriptorHash)) {
                // within one call Yarn recurses; through the scheduler it awaits its own pending resolution
                throw new Endless(i >= step ? Ending.STACK_EXHAUSTED : Ending.EMPTY_EVENT_LOOP,
                        "the resolution chain " + names(i, descriptor) + " revisits " +
                        StructUtils.stringifyDescriptor(descriptor));
            }
            if (first < 0) {
                first = i;
            }
            passes++;
        }
        if (passes == 2) {
            throw new Endless(first >= step ? Ending.STACK_EXHAUSTED : Ending.HEAP_EXHAUSTED,
                    "the resolution chain " + names(first, descriptor) + " revisits " + descriptor.range +
                    " under a new name each time");
        }
        descriptors.add(descriptor);
        resolvers.add(resolver);
    }

    // a descriptor the lockfile resolver hands on is on the chain twice in a row
    private String names(int from, Descriptor last) {
        StringBuilder names = new StringBuilder();
        for (int i = from; i < descriptors.size(); i++) {
            if (i == from || !descriptors.get(i).descriptorHash.equals(descriptors.get(i - 1).descriptorHash)) {
                names.append(StructUtils.stringifyDescriptor(descriptors.get(i))).append(" -> ");
            }
        }
        return names.append(StructUtils.stringifyDescriptor(last)).toString();
    }
}
