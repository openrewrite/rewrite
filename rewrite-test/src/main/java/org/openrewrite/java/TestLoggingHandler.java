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

public class TestLoggingHandler implements LoggingHandler {

    private static final Logger logger = LoggerFactory.getLogger(TestLoggingHandler.class);

    @Override
    public void onError(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void onError(String message) {
        logger.error(message);
    }

    @Override
    public void onWarn(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void onWarn(String message) {
        logger.error(message);
    }
}
