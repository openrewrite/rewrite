/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.stream.Stream;

public enum Flag {
    Public(1L),
    Private(1L << 1),
    Protected(1L << 2),
    Static(1L << 3),
    Final(1L << 4),
    Synchronized(1L << 5),
    Volatile(1L << 6),
    Transient(1L << 7),
    Native(1L << 8),
    Interface(1L << 9),
    Abstract(1L << 10),
    Strictfp(1L << 11),
    /**
     * Flag is set for a variable symbol if the variable's definition
     * has an initializer part.
     */
    HasInit(1L << 18),
    Varargs(1L << 34),
    Union(1L << 39),
    Default(1L << 43),
    SignaturePolymorphic(1L << 46),
    PotentiallyAmbiguous(1L << 48),
    Sealed(1L << 62),
    NonSealed(1L << 63);

    private final long bitMask;

    public static final long VALID_CLASS_FLAGS = Stream.of(Public, Private, Protected, Static,  Final, Interface, Abstract, Strictfp)
            .map(Flag::getBitMask).reduce(0L, (m1, m2) -> m1 | m2);
    public static final long VALID_FLAGS = Arrays.stream(Flag.values())
            .map(Flag::getBitMask)
            .reduce(0L, (m1, m2) -> m1 | m2);

    Flag(long bitMask) {
        this.bitMask = bitMask;
    }

    private static final Map<Long, Set<Flag>> flagSets = new HashMap<>(64);

    public long getBitMask() {
        return bitMask;
    }

    /**
     * Convert the Java language specification's access flags bitmap into a set of Flag enumerations.
     *
     * @param flagsBitMap The flag from the Javac symbol into a set of rewrite's Flag enum
     * @return A set of Flag enums.
     */
    public static Set<Flag> bitMapToFlags(long flagsBitMap) {
        Set<Flag> flags = flagSets.get(flagsBitMap);
        if (flags == null) {
            flags = java.util.EnumSet.noneOf(Flag.class);
            for (Flag flag : values()) {
                if ((flagsBitMap & flag.bitMask) != 0) {
                    flags.add(flag);
                }
            }
            flags = Collections.unmodifiableSet(flags);
            flagSets.put(flagsBitMap, flags);
        }
        return flags;
    }

    /**
     * Converts a set of flag enumerations into the Java Language Specification's access_flags bitmap
     *
     * @param flags A set of Flag enumerations
     * @return The bitmask representation of those flags.
     */
    public static long flagsToBitMap(@Nullable Set<Flag> flags) {
        long mask = 0;
        if (flags != null) {
            for (Flag flag : flags) {
                mask = mask | flag.bitMask;
            }
        }
        return mask;
    }

    /**
     * @param flagsBitMap Java Language Specification's access flags bitmap
     * @param flags       A set of flags to test
     * @return Returns true if the access flags bitmap contains all the flags passed to this method.
     */
    public static boolean hasFlags(long flagsBitMap, Flag... flags) {
        for (Flag flag : flags) {
            if ((flag.bitMask & flagsBitMap) == 0) {
                return false;
            }
        }
        return true;
    }
}
