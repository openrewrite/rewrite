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
        private static final Map<String, Integer> QUALIFIERS;
        private final String version;
        private int index;
        private String token;
        private boolean number;
        private boolean terminatedByNumber;

        Tokenizer(String version) {
            this.version = version.length() > 0 ? version : "0";
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
                    this.token = this.version.substring(start, end);
                    this.number = state >= 0;
                } else {
                    this.token = "0";
                    this.number = true;
                }

                return true;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(this.token);
        }

        public Version.Item toItem() {
            if (this.number) {
                try {
                    return this.token.length() < 10 ? new Version.Item(4, Integer.parseInt(this.token)) : new Version.Item(5, new BigInteger(this.token));
                } catch (NumberFormatException var2) {
                    throw new IllegalStateException(var2);
                }
            } else {
                if (this.index >= this.version.length()) {
                    if ("min".equalsIgnoreCase(this.token)) {
                        return Version.Item.MIN;
                    }

                    if ("max".equalsIgnoreCase(this.token)) {
                        return Version.Item.MAX;
                    }
                }

                if (this.terminatedByNumber && this.token.length() == 1) {
                    switch (this.token.charAt(0)) {
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

                Integer qualifier = QUALIFIERS.get(this.token);
                return qualifier != null ? new Version.Item(2, qualifier) : new Version.Item(3, this.token.toLowerCase(Locale.ENGLISH));
            }
        }

        static {
            QUALIFIERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            QUALIFIERS.put("alpha", QUALIFIER_ALPHA);
            QUALIFIERS.put("beta", QUALIFIER_BETA);
            QUALIFIERS.put("milestone", QUALIFIER_MILESTONE);
            QUALIFIERS.put("cr", -2);
            QUALIFIERS.put("rc", -2);
            QUALIFIERS.put("snapshot", -1);
            QUALIFIERS.put("ga", 0);
            QUALIFIERS.put("final", 0);
            QUALIFIERS.put("release", 0);
            QUALIFIERS.put("", 0);
            QUALIFIERS.put("sp", 1);
        }
    }
}
