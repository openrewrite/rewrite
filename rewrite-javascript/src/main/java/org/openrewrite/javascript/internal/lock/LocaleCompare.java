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
package org.openrewrite.javascript.internal.lock;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * {@code Intl.Collator('en').compare} as Node 24's ICU answers it: the UCA over the CLDR 48 root collation to the
 * third level; a string whose answer ICU gives differently from its NFD form's is refused.
 */
public final class LocaleCompare {

    /**
     * A string whose order under ICU is not known: not in normalization form D, with a contraction
     * matched through combining marks or across a composite from beyond the Basic Multilingual Plane.
     */
    public static final class UnknownOrder extends IllegalArgumentException {
        private final String string;

        UnknownOrder(String string) {
            super("The order of " + EcmaScript.quote(string) + " under Intl.Collator is not known: it is not in " +
                  "normalization form D where a contraction meets a character beyond the Basic Multilingual Plane");
            this.string = string;
        }

        String getString() {
            return string;
        }
    }

    private static final int RANKED_BASE = 0x10000;
    private static final int UNASSIGNED_BASE = 0x1000000;
    private static final long COMMON_WEIGHTS = 0x20 << 5 | 0x02;
    private static final int LONGEST_CONTRACTION = 3;

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_COUNT = 11172;
    private static final int HANGUL_VOWELS = 21;
    private static final int HANGUL_TRAILS = 28;

    private LocaleCompare() {
    }

    public static int en(String a, String b) {
        long[] x = Data.INSTANCE.elements(a);
        long[] y = Data.INSTANCE.elements(b);
        for (int level = 0; level < 3; level++) {
            int order = compare(x, y, level);
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }

    private static int compare(long[] x, long[] y, int level) {
        int i = 0;
        int j = 0;
        while (true) {
            long wx = 0;
            while (wx == 0 && i < x.length) {
                wx = weight(x[i++], level);
            }
            long wy = 0;
            while (wy == 0 && j < y.length) {
                wy = weight(y[j++], level);
            }
            if (wx != wy || wx == 0) {
                return Long.compare(wx, wy);
            }
        }
    }

    private static long weight(long element, int level) {
        return level == 0 ? element >>> 16 : level == 1 ? (element >>> 5) & 0x7FF : element & 0x1F;
    }

    /**
     * The collation elements of each character and contraction, a primary weight above a secondary and a
     * tertiary, with the decompositions and combining classes that normalize a string first.
     */
    private static final class Data {
        static final Data INSTANCE = new Data();

        // one element each, and no contraction is of ASCII alone
        private final long[] ascii = new long[128];
        private final int[] characters;
        private final long[][] characterElements;
        private final Map<String, long[]> contractions = new HashMap<>();
        private final int[] contractionStarts;
        private final int[] contractionCharacters;
        private final int[] decomposable;
        private final int[][] decompositions;
        private final int[] combining;
        private final byte[] combiningClasses;
        private final int[] ideographs;
        private final int[] ideographRanks;

        private Data() {
            try (InputStream is = LocaleCompare.class.getResourceAsStream("root-collation.gz");
                 DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(is)))) {
                characters = new int[in.readInt()];
                characterElements = new long[characters.length][];
                for (int i = 0; i < characters.length; i++) {
                    characters[i] = in.readInt();
                    characterElements[i] = readElements(in);
                    if (characters[i] < ascii.length) {
                        ascii[characters[i]] = characterElements[i][0];
                    }
                }
                int[] starts = new int[in.readInt()];
                List<Integer> inContractions = new ArrayList<>();
                for (int i = 0; i < starts.length; i++) {
                    int[] codePoints = readCodePoints(in);
                    starts[i] = codePoints[0];
                    for (int codePoint : codePoints) {
                        inContractions.add(codePoint);
                    }
                    contractions.put(new String(codePoints, 0, codePoints.length), readElements(in));
                }
                contractionStarts = Arrays.stream(starts).sorted().distinct().toArray();
                contractionCharacters = inContractions.stream().mapToInt(Integer::intValue).sorted().distinct()
                        .toArray();
                decomposable = new int[in.readInt()];
                decompositions = new int[decomposable.length][];
                for (int i = 0; i < decomposable.length; i++) {
                    decomposable[i] = in.readInt();
                    decompositions[i] = readCodePoints(in);
                }
                combining = new int[in.readInt()];
                combiningClasses = new byte[combining.length];
                for (int i = 0; i < combining.length; i++) {
                    combining[i] = in.readInt();
                    combiningClasses[i] = in.readByte();
                }
                long[] byCodePoint = new long[in.readInt()];
                int codePoint = 0;
                for (int rank = 0; rank < byCodePoint.length; rank++) {
                    codePoint += readDelta(in);
                    byCodePoint[rank] = (long) codePoint << 32 | rank;
                }
                Arrays.sort(byCodePoint);
                ideographs = new int[byCodePoint.length];
                ideographRanks = new int[byCodePoint.length];
                for (int i = 0; i < byCodePoint.length; i++) {
                    ideographs[i] = (int) (byCodePoint[i] >>> 32);
                    ideographRanks[i] = (int) byCodePoint[i];
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static long[] readElements(DataInputStream in) throws IOException {
            long[] elements = new long[in.readUnsignedByte()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = (long) in.readInt() << 16 | in.readUnsignedShort();
            }
            return elements;
        }

        private static int[] readCodePoints(DataInputStream in) throws IOException {
            int[] codePoints = new int[in.readUnsignedByte()];
            for (int i = 0; i < codePoints.length; i++) {
                codePoints[i] = in.readInt();
            }
            return codePoints;
        }

        private static int readDelta(DataInputStream in) throws IOException {
            int zigzag = 0;
            for (int shift = 0; ; shift += 7) {
                int b = in.readUnsignedByte();
                zigzag |= (b & 0x7F) << shift;
                if (b < 0x80) {
                    return (zigzag >>> 1) ^ -(zigzag & 1);
                }
            }
        }

        long[] elements(String s) {
            long[] elements = new long[s.length()];
            for (int i = 0; i < elements.length; i++) {
                char c = s.charAt(i);
                if (c >= ascii.length) {
                    int[] codePoints = s.codePoints().toArray();
                    int[] decomposed = decompose(codePoints);
                    int[] normalized = reorder(decomposed);
                    if (s.length() > codePoints.length && orderUnknown(codePoints, decomposed, normalized)) {
                        throw new UnknownOrder(s);
                    }
                    return elements(normalized);
                }
                elements[i] = ascii[c];
            }
            return elements;
        }

        private long[] elements(int[] codePoints) {
            long[] elements = new long[codePoints.length + 8];
            int count = 0;
            for (int i = 0; i < codePoints.length; ) {
                long[] matched = null;
                int matchedLength = 1;
                if (Arrays.binarySearch(contractionStarts, codePoints[i]) >= 0) {
                    for (int l = Math.min(LONGEST_CONTRACTION, codePoints.length - i); l > 1 && matched == null; l--) {
                        matched = contractions.get(new String(codePoints, i, l));
                        matchedLength = matched == null ? 1 : l;
                    }
                    // a later combining mark joins the match unless a mark of its class or a higher one intervenes
                    int blocking = 0;
                    for (int k = i + matchedLength; k < codePoints.length; k++) {
                        int mark = codePoints[k];
                        int combiningClass = combiningClass(mark);
                        if (combiningClass == 0) {
                            break;
                        }
                        long[] longer = combiningClass <= blocking ? null : contractions.get(
                                new String(codePoints, i, matchedLength) + new String(Character.toChars(mark)));
                        if (longer == null) {
                            blocking = combiningClass;
                        } else {
                            System.arraycopy(codePoints, i + matchedLength, codePoints, i + matchedLength + 1,
                                    k - i - matchedLength);
                            codePoints[i + matchedLength++] = mark;
                            matched = longer;
                        }
                    }
                }
                if (matched == null) {
                    matched = character(codePoints[i]);
                }
                if (count + matched.length > elements.length) {
                    elements = Arrays.copyOf(elements, (count + matched.length) * 2);
                }
                System.arraycopy(matched, 0, elements, count, matched.length);
                count += matched.length;
                i += matchedLength;
            }
            return Arrays.copyOf(elements, count);
        }

        private long[] character(int codePoint) {
            int listed = Arrays.binarySearch(characters, codePoint);
            if (listed >= 0) {
                return characterElements[listed];
            }
            int ideograph = Arrays.binarySearch(ideographs, codePoint);
            long primary = ideograph >= 0 ? RANKED_BASE + ideographRanks[ideograph] : UNASSIGNED_BASE + codePoint;
            return new long[]{primary << 16 | COMMON_WEIGHTS};
        }

        private int combiningClass(int codePoint) {
            if (codePoint < 0x300) {
                return 0;
            }
            int i = Arrays.binarySearch(combining, codePoint);
            return i < 0 ? 0 : combiningClasses[i] & 0xFF;
        }

        /**
         * Where ICU answers a string not in NFD apart from its NFD form: a contraction through marks the form reorders,
         * or one a composite starts within, with a supplementary-plane character in the run or the composite.
         */
        private boolean orderUnknown(int[] codePoints, int[] decomposed, int[] normalized) {
            int[] rangeStart = new int[decomposed.length];
            int[] rangeEnd = new int[decomposed.length];
            boolean[] compositeNonStarter = new boolean[decomposed.length];
            boolean[] supplementaryComposite = new boolean[decomposed.length];
            int at = 0;
            for (int codePoint : codePoints) {
                int[] parts = decomposition(codePoint);
                for (int k = 0; k < parts.length; k++, at++) {
                    rangeStart[at] = at - k;
                    rangeEnd[at] = at - k + parts.length;
                    compositeNonStarter[at] = parts.length > 1 && combiningClass(parts[0]) != 0;
                    supplementaryComposite[at] = parts.length > 1 && codePoint >= 0x10000;
                }
            }
            int start = 0;
            for (int i = 0; i <= decomposed.length; i++) {
                if (i == decomposed.length || combiningClass(decomposed[i]) == 0) {
                    boolean changed = false;
                    boolean supplementary = false;
                    boolean contraction = start > 0 && inContraction(decomposed[start - 1]);
                    for (int k = start; k < i; k++) {
                        changed |= normalized[k] != decomposed[k] || compositeNonStarter[k];
                        supplementary |= decomposed[k] >= 0x10000;
                        contraction |= inContraction(decomposed[k]);
                    }
                    if (changed && supplementary && contraction) {
                        return true;
                    }
                    start = i + 1;
                }
            }
            for (int i = 0; i < decomposed.length; ) {
                int length = 1;
                if (Arrays.binarySearch(contractionStarts, decomposed[i]) >= 0) {
                    for (int l = Math.min(LONGEST_CONTRACTION, decomposed.length - i); l > 1; l--) {
                        if (contractions.containsKey(new String(decomposed, i, l))) {
                            length = l;
                            break;
                        }
                    }
                }
                for (int k = i + 1; k < i + length; k++) {
                    if (supplementaryComposite[k] && rangeStart[k] > i) {
                        return true;
                    }
                }
                i += length;
            }
            return false;
        }

        private boolean inContraction(int codePoint) {
            return Arrays.binarySearch(contractionCharacters, codePoint) >= 0;
        }

        private int[] decomposition(int codePoint) {
            int syllable = codePoint - HANGUL_BASE;
            if (syllable >= 0 && syllable < HANGUL_COUNT) {
                int lead = 0x1100 + syllable / (HANGUL_VOWELS * HANGUL_TRAILS);
                int vowel = 0x1161 + syllable % (HANGUL_VOWELS * HANGUL_TRAILS) / HANGUL_TRAILS;
                return syllable % HANGUL_TRAILS == 0 ? new int[]{lead, vowel} :
                        new int[]{lead, vowel, 0x11A7 + syllable % HANGUL_TRAILS};
            }
            int decomposed = codePoint < 0xC0 ? -1 : Arrays.binarySearch(decomposable, codePoint);
            return decomposed < 0 ? new int[]{codePoint} : decompositions[decomposed];
        }

        /**
         * The full canonical decomposition of each code point, in the order given.
         */
        private int[] decompose(int[] codePoints) {
            int[] out = new int[codePoints.length + 8];
            int count = 0;
            for (int codePoint : codePoints) {
                int[] parts = decomposition(codePoint);
                if (count + parts.length > out.length) {
                    out = Arrays.copyOf(out, out.length * 2 + 16);
                }
                System.arraycopy(parts, 0, out, count, parts.length);
                count += parts.length;
            }
            return Arrays.copyOf(out, count);
        }

        /**
         * Normalization form D: the decomposition in canonical order.
         */
        private int[] reorder(int[] decomposed) {
            int[] out = decomposed.clone();
            for (int i = 1; i < out.length; i++) {
                int mark = out[i];
                int combiningClass = combiningClass(mark);
                int j = i;
                while (combiningClass != 0 && j > 0 && combiningClass(out[j - 1]) > combiningClass) {
                    out[j] = out[j - 1];
                    j--;
                }
                out[j] = mark;
            }
            return out;
        }
    }
}
