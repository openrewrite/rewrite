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

import org.jspecify.annotations.Nullable;
import org.openrewrite.config.LicenseKey;

public class ModerneLicenseKeyExecutionContextView extends DelegatingExecutionContext {
    private static final String LICENSE_KEY = "moderne.io.licenseKey";

    public ModerneLicenseKeyExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static ModerneLicenseKeyExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof ModerneLicenseKeyExecutionContextView) {
            return (ModerneLicenseKeyExecutionContextView) ctx;
        }
        return new ModerneLicenseKeyExecutionContextView(ctx);
    }

    public ModerneLicenseKeyExecutionContextView setLicenseKey(LicenseKey key) {
        putMessage(LICENSE_KEY, key);
        return this;
    }

    public @Nullable LicenseKey getLicenseKey() {
        return getMessage(LICENSE_KEY);
    }
}
