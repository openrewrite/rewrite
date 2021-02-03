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
