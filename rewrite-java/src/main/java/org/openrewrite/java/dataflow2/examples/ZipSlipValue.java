/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow2.examples;

import lombok.AllArgsConstructor;
import org.openrewrite.java.dataflow2.Joiner;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collection;

@AllArgsConstructor
public abstract class ZipSlipValue {

    // ZipEntry entry, File dir
    // String fileName = entry.getName();
    // File file = new File(dir, fileName);

    public final String name;

    public static final Joiner<ZipSlipValue> JOINER = new Joiner<ZipSlipValue>() {
        @Override
        public ZipSlipValue join(Collection<ZipSlipValue> values) {
            return ZipSlipValue.join(values);
        }

        @Override
        public ZipSlipValue lowerBound() {
            return UNKNOWN;
        }

        @Override
        public ZipSlipValue defaultInitialization() {
            return SAFE;
        }
    };

    // all other values have a non-null dir
    // lower bound (initial value) : nothing is know about the value
    public static final ZipSlipValue UNKNOWN = new Unknown();
    // upper bound : conflicting information about the value
    public static final ZipSlipValue UNSAFE = new Unsafe();
    // the value is known to be safe
    public static final ZipSlipValue SAFE = new Safe();
    // the string value is returned by ZipEntry.entryName()
    public static final ZipSlipValue ZIP_ENTRY_NAME = new ZipEntryName();

    private static ZipSlipValue join(Collection<ZipSlipValue> values) {
        ZipSlipValue result = UNKNOWN;
        for (ZipSlipValue value : values) {
            if (result == UNKNOWN) {
                result = value;
            } else if (value == SAFE && result == SAFE) {
                // do nothing
            } else if (value == ZIP_ENTRY_NAME && result == ZIP_ENTRY_NAME) {
                // do nothing
            } else if (value instanceof NewFileFromZipEntry && result instanceof NewFileFromZipEntry) {
                if (!((NewFileFromZipEntry) value).dir.equals(((NewFileFromZipEntry) result).dir)) {
                    return UNSAFE;
                }
            } else {
                return UNSAFE;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static boolean equal(Expression a, Expression b) {
        if (a instanceof J.Identifier && b instanceof J.Identifier) {
            return ((J.Identifier) a).getFieldType() == ((J.Identifier) b).getFieldType();
        }
        return false;
    }

    public static class Unknown extends ZipSlipValue {
        protected Unknown() {
            super("UNKNOWN");
        }
    }

    public static class Unsafe extends ZipSlipValue {
        protected Unsafe() {
            super("UNSAFE");
        }
    }

    public static class Safe extends ZipSlipValue {
        protected Safe() {
            super("SAFE");
        }
    }

    public static class ZipEntryName extends ZipSlipValue {
        protected ZipEntryName() {
            super("ZIP_ENTRY_NAME");
        }
    }

    public static class NewFileFromZipEntry extends ZipSlipValue {
        public final Expression dir;

        protected NewFileFromZipEntry(Expression dir) {
            super("NewFileFromZipEntry");
            this.dir = dir;
        }

        @Override
        public String toString() {
            return "NewFileFromZipEntry(" + dir + ")";
        }
    }
}
