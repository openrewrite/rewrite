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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum Flag {

    Public("public", 1),
    Private("private", 1<<1),
    Protected("protected", 1<<2),
    Static("static", 1<<3),
    Final("final", 1<<4),
    Synchronized("synchronized",1<<5),
    Volatile("volatile",1<<6),
    Transient("transient", 1<<7),
    Abstract("abstract", 1<<10);

    private final String keyword;
    private final int bitMask;

    Flag(String keyword, int bitMask) {
        this.keyword = keyword;
        this.bitMask = bitMask;
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Nullable
    public static Flag fromKeyword(String keyword) {
        for (Flag flag : values()) {
            if (flag.keyword.equals(keyword)) {
                return flag;
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------
    // This code is similar to the code in the com.sun.tools.javac.code.Flags.
    // ----------------------------------------------------------------------------------------------
    private static final Map<Integer, Set<Flag>> flagSets = new ConcurrentHashMap<>(64);

    /**
     * Convert the Java language specification's access flags bitmap into a set of Flag enumerations.
     *
     * @param flagsBitMap The flag from the Javac symbol into a set of rewrite's Flag enum
     * @return A set of Flag enums.
     */
    public static Set<Flag> bitMapToFlags(int flagsBitMap) {
        Set<Flag> flags = flagSets.get(flagsBitMap);
        if (flags == null) {
            flags = java.util.EnumSet.noneOf(Flag.class);
            if (0 != (flagsBitMap & Public.bitMask)) flags.add(Flag.Public);
            if (0 != (flagsBitMap & Protected.bitMask)) flags.add(Flag.Protected);
            if (0 != (flagsBitMap & Private.bitMask))   flags.add(Flag.Private);
            if (0 != (flagsBitMap & Abstract.bitMask))  flags.add(Flag.Abstract);
            if (0 != (flagsBitMap & Static.bitMask))    flags.add(Flag.Static);
            if (0 != (flagsBitMap & Final.bitMask))     flags.add(Flag.Final);
            if (0 != (flagsBitMap & Transient.bitMask)) flags.add(Flag.Transient);
            if (0 != (flagsBitMap & Volatile.bitMask))  flags.add(Flag.Volatile);
            if (0 != (flagsBitMap & Synchronized.bitMask)) flags.add(Flag.Synchronized);
            //No mappings for NATIVE, STRICTFP, or DEFAULT
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
    public static int flagsToBitMap(@Nullable  Set<Flag> flags) {
        int mask = 0;
        if (flags != null) {
            for (Flag flag : flags) {
                mask = mask | flag.bitMask;
            }
        }
        return mask;
    }

    /**
     * @param flagsBitMap Java Language Specification's access flags bitmap
     * @param flags A set of flags to test
     * @return Returns true if the access flags bitmap contains all the flags passed to this method.
     */
    public static boolean hasFlags(int flagsBitMap, Flag... flags) {
        for (Flag flag : flags) {
            if ((flag.bitMask & flagsBitMap) == 0) {
                return false;
            }
        }
        return true;
    }
}
