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
package org.openrewrite.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateLoggingEventHandler implements JavaTemplate.TemplateEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(TemplateLoggingEventHandler.class);

    @Override
    public void afterVariableSubstitution(String substitutedTemplate) {
        logger.trace("\n------------------- Template After Parameter Substitution  -------------------\n{}\n" +
                       "------------------------------------------------------------------------------",
                substitutedTemplate);
    }

    @Override
    public void beforeParseTemplate(String generatedTemplate) {
        logger.trace("\n------------------- Generated Template ---------------------------------------\n{}\n" +
                       "------------------------------------------------------------------------------",
                generatedTemplate);
    }
}
