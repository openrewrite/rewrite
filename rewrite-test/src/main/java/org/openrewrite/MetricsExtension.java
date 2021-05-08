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
package org.openrewrite;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openrewrite.internal.LoggingMeterRegistry;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import static org.junit.jupiter.api.extension.ExtensionContext.Store;

public class MetricsExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String METER_REGISTRY = "loggingMeterRegistry";

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        LoggingMeterRegistry meterRegistry = LoggingMeterRegistry.builder().build();
        meterRegistry.config().meterFilter(MeterFilter.ignoreTags("repo.id", "exception.line",
                "exception.declaring.class", "exception", "step"));

        Metrics.addRegistry(meterRegistry);
        getStore(context).put(METER_REGISTRY, meterRegistry);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        LoggingMeterRegistry meterRegistry = getStore(context).remove(METER_REGISTRY, LoggingMeterRegistry.class);
        meterRegistry.print();
        Metrics.removeRegistry(meterRegistry);
    }

    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}
