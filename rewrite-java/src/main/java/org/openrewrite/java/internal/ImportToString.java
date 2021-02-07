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
package org.openrewrite.java.internal;

import org.openrewrite.TreePrinter;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

public class ImportToString {
    public static String toString(J.Import impoort) {
        //noinspection ConstantConditions
        return IMPORT_PRINTER.print(impoort, null);
    }

    private static final JavaPrinter<Void> IMPORT_PRINTER = new JavaPrinter<Void>(TreePrinter.identity()) {
        @Override
        public J visitImport(J.Import impoort, Void unused) {
            J.Import i = impoort.withPrefix(Space.EMPTY);
            i = i.withQualid(i.getQualid().withPrefix(i.getQualid().getPrefix().withWhitespace(" ")));
            i = i.getPadding().withStatic(i.getPadding().getStatic().withBefore(i.getPadding().getStatic().getBefore().withWhitespace(" ")));
            return super.visitImport(i, unused);
        }
    };
}
