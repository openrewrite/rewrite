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
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strings"
	"testing"
)

// TestResolveTidyViaJavaWireProtocol exercises the Go half of the
// GoModResolveTidy RPC: it feeds a canned Java response and asserts both that
// resolveTidyViaJava parses it correctly AND that the request it writes carries
// the exact field names the Java GoModResolveTidyRequest expects. This guards
// the cross-process contract that the in-Java handler test cannot see.
func TestResolveTidyViaJavaWireProtocol(t *testing.T) {
	s := newServer(serverConfig{logFile: filepath.Join(t.TempDir(), "server.log")})

	// Canned host response: direct/indirect/complete, JSON-RPC framed.
	respBody := `{"jsonrpc":"2.0","id":"go-GoModResolveTidy","result":` +
		`{"direct":{"github.com/a/x":"v1.2.3"},"indirect":{"github.com/b/y":"v0.4.0"},"complete":true}}`
	framed := fmt.Sprintf("Content-Length: %d\r\n\r\n%s", len(respBody), respBody)
	s.reader = bufio.NewReader(strings.NewReader(framed))
	var written bytes.Buffer
	s.writer = &written

	rs, ok := s.resolveTidyViaJava("module example.com/m\n\ngo 1.21\n",
		[]string{"github.com/a/x", "fmt"}, "example.com/m", true)

	// Response parsed correctly.
	if !ok {
		t.Fatal("expected ok=true")
	}
	if !rs.Complete {
		t.Error("expected Complete=true")
	}
	if rs.Direct["github.com/a/x"] != "v1.2.3" {
		t.Errorf("direct: got %v", rs.Direct)
	}
	if rs.Indirect["github.com/b/y"] != "v0.4.0" {
		t.Errorf("indirect: got %v", rs.Indirect)
	}

	// Request written with the method and param field names the Java side expects.
	body := written.String()
	if i := strings.Index(body, "\r\n\r\n"); i >= 0 {
		body = body[i+4:]
	}
	var req struct {
		Method string `json:"method"`
		Params struct {
			GoMod            string   `json:"goMod"`
			MainImports      []string `json:"mainImports"`
			ModulePath       string   `json:"modulePath"`
			SeparateIndirect bool     `json:"separateIndirect"`
			Goproxy          string   `json:"goproxy"`
			Gomodcache       string   `json:"gomodcache"`
		} `json:"params"`
	}
	if err := json.Unmarshal([]byte(body), &req); err != nil {
		t.Fatalf("request JSON: %v\nbody=%s", err, body)
	}
	if req.Method != "GoModResolveTidy" {
		t.Errorf("method: got %q", req.Method)
	}
	if !strings.Contains(req.Params.GoMod, "module example.com/m") {
		t.Errorf("goMod not propagated: %q", req.Params.GoMod)
	}
	if req.Params.ModulePath != "example.com/m" {
		t.Errorf("modulePath: got %q", req.Params.ModulePath)
	}
	if !req.Params.SeparateIndirect {
		t.Error("separateIndirect should be true")
	}
	if len(req.Params.MainImports) != 2 || req.Params.MainImports[0] != "github.com/a/x" {
		t.Errorf("mainImports: got %v", req.Params.MainImports)
	}
	if req.Params.Goproxy == "" || req.Params.Gomodcache == "" {
		t.Errorf("goproxy/gomodcache should be populated from env: %q / %q",
			req.Params.Goproxy, req.Params.Gomodcache)
	}
}
