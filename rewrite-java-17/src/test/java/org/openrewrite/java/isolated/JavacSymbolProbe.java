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

/**
 * Reads javac symbol state out of a {@link ReloadableJava17Parser}. Tests load this class by name through the
 * parser's class loader, the only loader that can link against javac's internals.
 */
public class JavacSymbolProbe {

    /**
     * @return whether javac has completed the class symbol for {@code fqn}, or {@code null} when the parser's
     * symbol table has no symbol for it.
     */
    public static @Nullable Boolean isCompleted(Object parser, String fqn) throws ReflectiveOperationException {
        Field contextField = ReloadableJava17Parser.class.getDeclaredField("context");
        contextField.setAccessible(true);
        Context context = (Context) contextField.get(parser);
        Names names = Names.instance(context);
        for (Symbol.ClassSymbol sym : Symtab.instance(context).getClassesForName(names.fromString(fqn))) {
            return sym.completer.isTerminal();
        }
        return null;
    }
}
