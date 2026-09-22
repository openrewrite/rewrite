/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestPerCallLoggingRequiresTraceRpcMessages(t *testing.T) {
	logOfOneRequest := func(cfg serverConfig) string {
		t.Helper()
		cfg.logFile = filepath.Join(t.TempDir(), "server.log")
		s := newServer(cfg)
		t.Cleanup(s.closeMetrics)
		s.safeHandleRequest(&jsonRPCRequest{JSONRPC: "2.0", ID: json.RawMessage("1"), Method: "GetLanguages"})
		out, err := os.ReadFile(cfg.logFile)
		require.NoError(t, err)
		return string(out)
	}

	assert.Empty(t, logOfOneRequest(serverConfig{}), "a successful RPC call should log nothing by default")

	assert.Contains(t, logOfOneRequest(serverConfig{traceRpcMessages: true}), "Handling: GetLanguages")
}
