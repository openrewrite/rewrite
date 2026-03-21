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
package org.openrewrite.maven.tree;

import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.*;

/**
 * Modified from <code>org.eclipse.aether.util.version.GenericVersion</code>.
 */
public class Version implements Comparable<Version> {
    private final String version;
    private final Version.Item[] items;

    public Version(String version) {
        this.version = version;
        this.items = parse(version);
    }

    private static Version.Item[] parse(String version) {
        List<Item> items = new ArrayList<>();
        Version.Tokenizer tokenizer = new Version.Tokenizer(version);

        while (tokenizer.next()) {
            Version.Item item = tokenizer.toItem();
            items.add(item);
        }

        trimPadding(items);
        return items.toArray(new Item[0]);
    }

    private static void trimPadding(List<Version.Item> items) {
        Boolean number = null;
        int end = items.size() - 1;

        for (int i = end; i > 0; --i) {
            Version.Item item = items.get(i);
            if (!Boolean.valueOf(item.isNumber()).equals(number)) {
                end = i;
                number = item.isNumber();
            }

            if (end == i && (i == items.size() - 1 || items.get(i - 1).isNumber() == item.isNumber()) && item.compareTo(null) == 0) {
                items.remove(i);
                --end;
            }
        }

    }

    @Override
    public int compareTo(Version obj) {
        Version.Item[] these = this.items;
        Version.Item[] those = obj.items;
        boolean number = true;

        for (int index = 0; index < these.length || index < those.length; ++index) {
            if (index >= these.length) {
                return -comparePadding(those, index, null);
            }

            if (index >= those.length) {
                return comparePadding(these, index, null);
            }

            Version.Item thisItem = these[index];
            Version.Item thatItem = those[index];
            if (thisItem.isNumber() != thatItem.isNumber()) {
                if (number == thisItem.isNumber()) {
                    return comparePadding(these, index, number);
                }

                return -comparePadding(those, index, number);
            }

            int rel = thisItem.compareTo(thatItem);
            if (rel != 0) {
                return rel;
            }

            number = thisItem.isNumber();
        }

        return 0;
    }

    private static int comparePadding(Version.Item[] items, int index, @Nullable Boolean number) {
        int rel = 0;

        for (int i = index; i < items.length; ++i) {
            Version.Item item = items[i];
            if (number != null && number != item.isNumber()) {
                break;
            }

            rel = item.compareTo(null);
            if (rel != 0) {
                break;
            }
        }

        return rel;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Version && this.compareTo((Version) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.version.hashCode();
    }

    @Override
    public String toString() {
        return this.version;
    }

    static final class Item {
        private static final Version.Item MAX = new Version.Item(8, "max");
        private static final Version.Item MIN = new Version.Item(0, "min");

        private final int kind;
        private final Object value;

        Item(int kind, Object value) {
            this.kind = kind;
            this.value = value;
        }

        public boolean isNumber() {
            return (this.kind & 2) == 0;
        }

        public int compareTo(Version.@Nullable Item that) {
            int rel;
            if (that == null) {
                switch (this.kind) {
                    case 0:
                        rel = -1;
                        break;
                    case 1:
                    case 6:
                    case 7:
                    default:
                        throw new IllegalStateException("unknown version item kind " + this.kind);
                    case 2:
                    case 4:
                        rel = (Integer) this.value;
                        break;
                    case 3:
                    case 5:
                    case 8:
                        rel = 1;
                }
            } else {
                rel = this.kind - that.kind;
                if (rel == 0) {
                    switch (this.kind) {
                        case 0:
                        case 8:
                            break;
                        case 1:
                        case 6:
                        case 7:
                        default:
                            throw new IllegalStateException("unknown version item kind " + this.kind);
                        case 2:
                        case 4:
                            rel = ((Integer) this.value).compareTo((Integer) that.value);
                            break;
                        case 3:
                            rel = ((String) this.value).compareToIgnoreCase((String) that.value);
                            break;
                        case 5:
                            rel = ((BigInteger) this.value).compareTo((BigInteger) that.value);
                    }
                }
            }

            return rel;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Version.Item && this.compareTo((Version.Item) obj) == 0;
        }

        @Override
        public int hashCode() {
            return this.value.hashCode() + this.kind * 31;
        }

        @Override
        public String toString() {
            return String.valueOf(this.value);
        }
    }

    static final class Tokenizer {
        private static final Integer QUALIFIER_ALPHA = -5;
        private static final Integer QUALIFIER_BETA = -4;
        private static final Integer QUALIFIER_MILESTONE = -3;
        private final String version;
        private int index;
        private int tokenStart;
        private int tokenEnd;
        private boolean number;
        private boolean terminatedByNumber;

        Tokenizer(String version) {
            this.version = !version.isEmpty() ? version : "0";
        }

        public boolean next() {
            int n = this.version.length();
            if (this.index >= n) {
                return false;
            } else {
                int state = -2;
                int start = this.index;
                int end = n;

                for (this.terminatedByNumber = false; this.index < n; ++this.index) {
                    char c = this.version.charAt(this.index);
                    if (c == '.' || c == '-' || c == '_') {
                        end = this.index++;
                        break;
                    }

                    int digit = Character.digit(c, 10);
                    if (digit >= 0) {
                        if (state == -1) {
                            end = this.index;
                            this.terminatedByNumber = true;
                            break;
                        }

                        if (state == 0) {
                            ++start;
                        }

                        state = state <= 0 && digit <= 0 ? 0 : 1;
                    } else {
                        if (state >= 0) {
                            end = this.index;
                            break;
                        }

                        state = -1;
                    }
                }

                if (end > start) {
                    this.tokenStart = start;
                    this.tokenEnd = end;
                    this.number = state >= 0;
                } else {
                    this.tokenStart = 0;
                    this.tokenEnd = 1;
                    this.number = true;
                }

                return true;
            }
        }

        @Override
        public String toString() {
            return this.version.substring(this.tokenStart, this.tokenEnd);
        }

        public Version.Item toItem() {
            if (this.number) {
                try {
                    int tokenLength = this.tokenEnd - this.tokenStart;
                    return tokenLength < 10 ? new Version.Item(4, parseInt(version, tokenStart, tokenEnd)) :
                            new Version.Item(5, new BigInteger(version.substring(this.tokenStart, this.tokenEnd)));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal version: " + version.substring(this.tokenStart, this.tokenEnd), e);
                }
            } else {
                int tokenLength = this.tokenEnd - this.tokenStart;
                if (this.index >= this.version.length()) {
                    if (tokenLength == 3 && matches("min")) {
                        return Version.Item.MIN;
                    }

                    if (tokenLength == 3 && matches("max")) {
                        return Version.Item.MAX;
                    }
                }

                if (this.terminatedByNumber && tokenLength == 1) {
                    char c = this.version.charAt(this.tokenStart);
                    switch (c) {
                        case 'A':
                        case 'a':
                            return new Version.Item(2, QUALIFIER_ALPHA);
                        case 'B':
                        case 'b':
                            return new Version.Item(2, QUALIFIER_BETA);
                        case 'M':
                        case 'm':
                            return new Version.Item(2, QUALIFIER_MILESTONE);
                    }
                }

                // Fast path for common qualifiers (avoiding substring allocation and map lookup)
                switch (tokenLength) {
                    case 0:
                        return new Version.Item(2, 0);
                    case 2:
                        if (matches("ga")) {
                            return new Version.Item(2, 0);
                        }
                        if (matches("rc")) {
                            return new Version.Item(2, -2);
                        }
                        if (matches("cr")) {
                            return new Version.Item(2, -2);
                        }
                        if (matches("sp")) {
                            return new Version.Item(2, 1);
                        }
                        break;
                    case 4:
                        if (matches("beta")) {
                            return new Version.Item(2, QUALIFIER_BETA);
                        }
                        break;
                    case 5:
                        if (matches("alpha")) {
                            return new Version.Item(2, QUALIFIER_ALPHA);
                        }
                        if (matches("final")) {
                            return new Version.Item(2, 0);
                        }
                        break;
                    case 7:
                        if (matches("release")) {
                            return new Version.Item(2, 0);
                        }
                        break;
                    case 8:
                        if (matches("snapshot")) {
                            return new Version.Item(2, -1);
                        }
                        break;
                    case 9:
                        if (matches("milestone")) {
                            return new Version.Item(2, QUALIFIER_MILESTONE);
                        }
                        break;
                }

                // Unknown qualifier - treat as string
                String token = this.version.substring(this.tokenStart, this.tokenEnd);
                return new Version.Item(3, token.toLowerCase(Locale.ENGLISH));
            }
        }

        // Adapted from Java 9's `Integer#parseInt(CharSequence, int, int, int)`
        private static int parseInt(String s, int beginIndex, int endIndex)
                throws NumberFormatException {

            if (beginIndex >= endIndex) {
                throw new NumberFormatException("Empty string");
            }

            int result = 0;
            int i = beginIndex;
            int limit = -Integer.MAX_VALUE;
            int multmin = limit / 10;

            char firstChar = s.charAt(i);
            if (firstChar < '0' || firstChar > '9') {
                throw new NumberFormatException("Invalid character");
            }

            // Accumulating negatively avoids surprises near MAX_VALUE
            while (i < endIndex) {
                int digit = s.charAt(i++) - '0';
                if (digit < 0 || digit > 9) {
                    throw new NumberFormatException("Invalid character");
                }
                if (result < multmin) {
                    throw new NumberFormatException("Value out of range");
                }
                result *= 10;
                if (result < limit + digit) {
                    throw new NumberFormatException("Value out of range");
                }
                result -= digit;
            }
            return -result;
        }

        private boolean matches(String target) {
            return this.version.regionMatches(true, this.tokenStart, target, 0, target.length());
        }
    }
}
