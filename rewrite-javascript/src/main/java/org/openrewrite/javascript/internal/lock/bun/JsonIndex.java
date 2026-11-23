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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Port of bun's src/parsers/json_index.rs {@code StructuralIndex} (bun 1.4.0, {@link BunRelease#indexesJsonBeforeParsingIt}):
 * stage 1 of the JSON parser, the position of every token-significant byte of the UTF-8 document, produced as stage 2
 * asks for it. A position is indexed at each structural byte ({@code {}[]:,}), at each unescaped quote that opens or
 * closes a string, and at the start of each run of other bytes outside strings; whitespace, comments and string bodies
 * index nothing. A {@code /} that starts no comment, or a {@code /*} without its end, stops the index there.
 * <p>
 * The production schedule is the native build's (src/jsc/bindings/highway_json.cpp): 8 KiB of input at a time, until
 * a chunk holds a {@code /} or {@code '} outside a string, from which the scalar indexer re-scans the document, a
 * window's worth of positions at a time. What stage 2 has not asked for when it stops is never indexed, so an error
 * past that point goes unseen.
 */
final class JsonIndex {

    private static final int REFILL_INPUT = 8 * 1024;
    private static final int SENTINELS = 2;
    static final int LOOKBEHIND = 16;

    /**
     * Errors the indexer itself detects; the index ends there.
     */
    enum Error {
        UNTERMINATED_BLOCK_COMMENT, UNEXPECTED_SLASH
    }

    private final byte[] s;
    private final int n;
    private final int capacity;

    // every position produced so far; the window stage 2 may still ask for is [base, produced)
    private int[] positions = new int[64];
    private int produced;
    private int base;
    private boolean done;

    // the SIMD kernel's position and the state it carries between chunks
    private int srcOff;
    private boolean simdEscaped;
    private boolean simdInString;
    private boolean simdPrevScalar;

    // the scalar indexer, once a chunk held an oddity
    private boolean useScalar;
    private int sI;
    private boolean sPrevScalar;
    private boolean sPendingEscape;
    private int sSkip;

    @Nullable Error indexError;
    int indexErrorPos;

    /**
     * Where the first comment indexed starts; -1 for none.
     */
    int firstComment = -1;

    JsonIndex(byte[] contents) {
        this.s = contents;
        this.n = contents.length;
        this.capacity = Math.min(n, REFILL_INPUT) + 66 + SENTINELS + LOOKBEHIND;
    }

    /**
     * The byte position of index {@code logical}, producing more of the index as needed; past the end, the length.
     */
    int at(int logical) {
        if (logical >= produced) {
            fillTo(logical);
        }
        return positions[logical];
    }

    private void fillTo(int logical) {
        while (logical >= produced) {
            if (done) {
                push(positions[produced - 1]);
                continue;
            }
            int keepFrom = Math.max(Math.max(logical - LOOKBEHIND, 0), base) - base;
            if (keepFrom > 0) {
                base += keepFrom;
            }
            refillOnce();
        }
    }

    private void refillOnce() {
        if (!useScalar) {
            if (srcOff >= n) {
                finish();
                return;
            }
            int chunkLen = Math.min(n - srcOff, REFILL_INPUT);
            if (!simdChunk(srcOff, srcOff + chunkLen)) {
                useScalar = true;
                sSkip = produced;
                return;
            }
            srcOff += chunkLen;
            return;
        }
        scalarRefill();
    }

    /**
     * {@code JsonIndexImpl} over one chunk, carrying its state to the next: the chunk's positions, or nothing and
     * false where a {@code /} or {@code '} is outside a string in it.
     */
    private boolean simdChunk(int from, int to) {
        int[] emitted = new int[16];
        int count = 0;
        boolean escaped = simdEscaped;
        boolean inString = simdInString;
        boolean prevScalar = simdPrevScalar;
        for (int p = from; p < to; p++) {
            int c = s[p] & 0xFF;
            boolean escapedHere = escaped;
            escaped = c == '\\' && !escapedHere;
            boolean rq = c == '"' && !escapedHere;
            boolean inStr = inString ^ rq;
            inString = inStr;
            if ((c == '/' || c == '\'') && !inStr) {
                return false;
            }
            boolean structural = isStructural(c);
            boolean scalar = !structural && !isWhitespace(c) && !inStr && !rq;
            boolean scalarStart = scalar && !prevScalar;
            prevScalar = scalar;
            if (structural && !inStr || rq || scalarStart) {
                if (count == emitted.length) {
                    emitted = Arrays.copyOf(emitted, count * 2);
                }
                emitted[count++] = p;
            }
        }
        for (int i = 0; i < count; i++) {
            push(emitted[i]);
        }
        simdEscaped = escaped;
        simdInString = inString;
        simdPrevScalar = prevScalar;
        return true;
    }

    private void scalarRefill() {
        int emitCap = capacity - SENTINELS - 2;
        int i = sI;
        while (i < n) {
            if (produced - base >= emitCap) {
                sI = i;
                return;
            }
            int c = s[i] & 0xFF;
            boolean wasEscaped = sPendingEscape;
            sPendingEscape = false;
            if ((c == '"' || c == '\'') && !wasEscaped) {
                emit(i);
                sPrevScalar = false;
                int quote = c;
                i++;
                while (i < n) {
                    int b = s[i] & 0xFF;
                    if (b == quote) {
                        emit(i);
                        i++;
                        break;
                    }
                    if (b == '\\') {
                        i += 2;
                        continue;
                    }
                    i++;
                }
            } else if (c == '/') {
                sPrevScalar = false;
                int start = i;
                int next = i + 1 < n ? s[i + 1] & 0xFF : -1;
                if (next == '/') {
                    i += 2;
                    while (i < n) {
                        int b = s[i] & 0xFF;
                        if (b == '\n' || b == '\r' || isLsPs(s, i)) {
                            break;
                        }
                        i++;
                    }
                } else if (next == '*') {
                    i += 2;
                    while (true) {
                        if (i >= n) {
                            fail(Error.UNTERMINATED_BLOCK_COMMENT, start);
                            return;
                        }
                        if (s[i] == '*' && i + 1 < n && s[i + 1] == '/') {
                            i += 2;
                            break;
                        }
                        i++;
                    }
                } else {
                    fail(Error.UNEXPECTED_SLASH, i);
                    return;
                }
                if (firstComment < 0) {
                    firstComment = start;
                }
            } else {
                if (isStructural(c)) {
                    emit(i);
                    sPrevScalar = false;
                } else if (isWhitespace(c)) {
                    sPrevScalar = false;
                } else {
                    if (c == '\\' && !wasEscaped) {
                        sPendingEscape = true;
                    }
                    if (!sPrevScalar) {
                        emit(i);
                    }
                    sPrevScalar = true;
                }
                i++;
            }
        }
        sI = i;
        finish();
    }

    private void emit(int pos) {
        if (sSkip > 0) {
            sSkip--;
        } else {
            push(pos);
        }
    }

    private void fail(Error e, int pos) {
        indexError = e;
        indexErrorPos = pos;
        finish();
    }

    private void finish() {
        push(n);
        push(n);
        done = true;
    }

    private void push(int pos) {
        if (produced == positions.length) {
            positions = Arrays.copyOf(positions, produced * 2);
        }
        positions[produced++] = pos;
    }

    // scripts/build/jsonByteClass.ts: structural { } [ ] : , and whitespace space \t \n \r
    private static boolean isStructural(int c) {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',';
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * {@code is_ls_ps}: U+2028 or U+2029 starts at {@code i}.
     */
    static boolean isLsPs(byte[] s, int i) {
        return (s[i] & 0xFF) == 0xE2 && i + 2 < s.length && (s[i + 1] & 0xFF) == 0x80 &&
               ((s[i + 2] & 0xFF) == 0xA8 || (s[i + 2] & 0xFF) == 0xA9);
    }
}
