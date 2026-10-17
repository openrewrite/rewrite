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
package org.openrewrite.xml.internal.grammar;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

/** Per-target lexer state, keeping the shared grammar free of Java members. */
public abstract class XMLLexerBase extends Lexer {
    /** Standard JSP attribute quoting; disabled for XML, HTML, and JSPX. */
    public boolean jspMode = false;

    protected XMLLexerBase(CharStream input) {
        super(input);
    }

    /** Do not turn an unterminated scripting construct into ordinary string data. */
    public boolean isJspLiteralCharacter() {
        return _input.LA(1) != '<' || _input.LA(2) != '%';
    }
}
