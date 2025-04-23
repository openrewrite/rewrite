/*
 * Copyright (C) 2009-2019 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.openrewrite.java.isolated.internal;


import org.openrewrite.java.isolated.internal.JavacTreeMaker.TypeTag;

import static org.openrewrite.java.isolated.internal.JavacTreeMaker.TypeTag.typeTag;
import static org.openrewrite.java.isolated.internal.JavacTreeMaker.TypeTag.typeTagPermissive;

public final class Javac {
    private Javac() {
        // prevent instantiation
    }

    public static final TypeTag CTC_VOID = typeTag("VOID");
    public static final TypeTag CTC_UNKNOWN = typeTagPermissive("UNKNOWN"); // UNKNOWN has been removed in JDK24, hence, we need to look it up permissively (just make it `null` if it does not exist).

    static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) {
            throw new NullPointerException("t");
        }
        Javac.sneakyThrow0(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}
