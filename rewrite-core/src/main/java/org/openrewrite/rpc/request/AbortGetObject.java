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
package org.openrewrite.rpc.request;

import lombok.Value;

/**
 * Sent by the receiver of a {@link GetObject} transfer that it failed to take, which the sender
 * cannot otherwise know. The sender ends the transfer if it is still under way and forgets both
 * that the remote holds the object and the refs it assigned while sending it. The receiver has
 * done the same before sending this, and waits for the answer before asking for anything else.
 */
@Value
public class AbortGetObject implements RpcRequest {
    String id;
}
