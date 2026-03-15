/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class BatchVisitResponse {
    List<BatchVisitResult> results;

    @JsonCreator
    public BatchVisitResponse(@JsonProperty("results") List<BatchVisitResult> results) {
        this.results = results;
    }

    @Value
    public static class BatchVisitResult {
        boolean modified;
        boolean deleted;
        boolean hasNewMessages;
        List<String> newSearchResultIds;

        @JsonCreator
        public BatchVisitResult(@JsonProperty("modified") boolean modified,
                                @JsonProperty("deleted") boolean deleted,
                                @JsonProperty("hasNewMessages") boolean hasNewMessages,
                                @JsonProperty("newSearchResultIds") List<String> newSearchResultIds) {
            this.modified = modified;
            this.deleted = deleted;
            this.hasNewMessages = hasNewMessages;
            this.newSearchResultIds = newSearchResultIds;
        }
    }
}
