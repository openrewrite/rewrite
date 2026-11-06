/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.isolated;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

// Loaded through the parser's class loader, the only one that links against javac's internals.
public class JavacSymbolProbe {

    public static @Nullable Boolean isCompleted(Object parser, String fqn) throws ReflectiveOperationException {
        Field contextField = ReloadableJava17Parser.class.getDeclaredField("context");
        contextField.setAccessible(true);
        Context context = (Context) contextField.get(parser);
        Symtab syms = Symtab.instance(context);
        Symbol.ClassSymbol sym = syms.getClass(syms.unnamedModule, Names.instance(context).fromString(fqn));
        return sym == null ? null : sym.completer.isTerminal();
    }
}
