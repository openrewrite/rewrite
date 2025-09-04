/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.groovy;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.openrewrite.ParseWarning;
import org.openrewrite.Tree;
import org.openrewrite.internal.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

public class ParseWarningCollector extends ErrorCollector {

    private final List<ParseWarning> warningMarkers = new ArrayList<>();
    private final Class<? extends GroovyParser> parserClass;

    public ParseWarningCollector(CompilerConfiguration configuration, GroovyParser parser) {
        super(configuration);
        this.parserClass = parser.getClass();
    }

    @Override
    public void addErrorAndContinue(Message message) throws CompilationFailedException {
        super.addErrorAndContinue(message);

        Throwable t = null;
        if (message instanceof SyntaxErrorMessage) {
            t = ((SyntaxErrorMessage) message).getCause();
        } else if (message instanceof ExceptionMessage) {
            t = ((ExceptionMessage) message).getCause();
        }

        if (t != null) {
            warningMarkers.add(new ParseWarning(Tree.randomId(), ExceptionUtils.sanitizeStackTrace(t, parserClass)));
        }
    }

    @Override
    protected void failIfErrors() throws CompilationFailedException {
        // don't fail on phase complete
    }

    public List<ParseWarning> getWarningMarkers() {
        return warningMarkers;
    }
}
