/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.tree;

import org.openrewrite.Parser;
import org.openrewrite.SourceFile;

public interface ParsingEventListener {
    ParsingEventListener NOOP = new ParsingEventListener() {
    };

    /**
     * Parsers may call this one or more times before parsing begins on any one source file
     * to indicate to listeners preparatory steps that are about to be taken.
     * @param stateMessage The message to the listener.
     */
    default void intermediateMessage(String stateMessage) {
    }

    default void startedParsing(Parser.Input input) {
    }

    default void parsed(Parser.Input input, SourceFile sourceFile) {
    }
}
