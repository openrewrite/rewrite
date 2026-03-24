/*
 * Copyright 2025 the original author or authors.
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

// Package main implements a JSON-RPC 2.0 server for OpenRewrite Go language support.
// It communicates over stdin/stdout using Content-Length framed messages (LSP protocol).
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	goparser "github.com/openrewrite/rewrite/pkg/parser"
	"github.com/openrewrite/rewrite/pkg/printer"
	"github.com/openrewrite/rewrite/pkg/rpc"
	"github.com/openrewrite/rewrite/pkg/tree"
)

// jsonRPCRequest represents an incoming JSON-RPC 2.0 message (request or response).
type jsonRPCRequest struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id"`
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params"`
	Result  json.RawMessage `json:"result"` // present in responses
}

// jsonRPCResponse represents an outgoing JSON-RPC 2.0 response.
type jsonRPCResponse struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id"`
	Result  any             `json:"result,omitempty"`
	Error   *rpcError       `json:"error,omitempty"`
}

// rpcError is the error object in a JSON-RPC response.
type rpcError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    string `json:"data,omitempty"`
}

// server holds the RPC state.
type server struct {
	localObjects  map[string]any
	remoteObjects map[string]any
	localRefs     map[uintptr]int
	remoteRefs    map[int]any
	batchSize     int

	reader *bufio.Reader
	writer io.Writer
	logger *log.Logger
}

func newServer() *server {
	logFile, err := os.CreateTemp("", "go-rpc-*.log")
	if err != nil {
		logFile = os.Stderr
	}
	return &server{
		localObjects:  make(map[string]any),
		remoteObjects: make(map[string]any),
		localRefs:     make(map[uintptr]int),
		remoteRefs:    make(map[int]any),
		batchSize:     1000,
		reader:        bufio.NewReader(os.Stdin),
		writer:        os.Stdout,
		logger:        log.New(logFile, "", log.LstdFlags),
	}
}

func main() {
	s := newServer()
	s.logger.Println("Go RPC server starting...")

	for {
		req, err := s.readMessage()
		if err != nil {
			if err == io.EOF {
				break
			}
			s.logger.Printf("Error reading message: %v", err)
			break
		}

		resp := s.handleRequest(req)
		if err := s.writeMessage(resp); err != nil {
			s.logger.Printf("Error writing response: %v", err)
			break
		}
	}

	s.logger.Println("Go RPC server shutting down...")
}

// readMessage reads a Content-Length framed JSON-RPC message from stdin.
func (s *server) readMessage() (*jsonRPCRequest, error) {
	// Read Content-Length header
	headerLine, err := s.reader.ReadString('\n')
	if err != nil {
		return nil, err
	}
	headerLine = strings.TrimSpace(headerLine)
	if !strings.HasPrefix(headerLine, "Content-Length:") {
		return nil, fmt.Errorf("invalid header: %s", headerLine)
	}
	lengthStr := strings.TrimSpace(strings.TrimPrefix(headerLine, "Content-Length:"))
	contentLength, err := strconv.Atoi(lengthStr)
	if err != nil {
		return nil, fmt.Errorf("invalid content length: %s", lengthStr)
	}

	// Read empty separator line
	if _, err := s.reader.ReadString('\n'); err != nil {
		return nil, err
	}

	// Read content body
	body := make([]byte, contentLength)
	if _, err := io.ReadFull(s.reader, body); err != nil {
		return nil, err
	}

	var req jsonRPCRequest
	if err := json.Unmarshal(body, &req); err != nil {
		return nil, fmt.Errorf("invalid JSON: %w", err)
	}
	return &req, nil
}

// writeMessage writes a Content-Length framed JSON-RPC message to stdout.
func (s *server) writeMessage(resp *jsonRPCResponse) error {
	body, err := json.Marshal(resp)
	if err != nil {
		return err
	}
	header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))
	_, err = s.writer.Write(append([]byte(header), body...))
	return err
}

// handleRequest dispatches to the appropriate handler.
func (s *server) handleRequest(req *jsonRPCRequest) *jsonRPCResponse {
	s.logger.Printf("Handling: %s", req.Method)

	var result any
	var rpcErr *rpcError

	switch req.Method {
	case "GetLanguages":
		result = s.handleGetLanguages()
	case "Parse":
		result, rpcErr = s.handleParse(req.Params)
	case "GetObject":
		result, rpcErr = s.handleGetObject(req.Params)
	case "Print":
		result, rpcErr = s.handlePrint(req.Params)
	case "InstallRecipes":
		result, rpcErr = s.handleInstallRecipes(req.Params)
	case "Reset":
		result = s.handleReset()
	default:
		rpcErr = &rpcError{
			Code:    -32601,
			Message: fmt.Sprintf("Unknown method: %s", req.Method),
		}
	}

	return &jsonRPCResponse{
		JSONRPC: "2.0",
		ID:      req.ID,
		Result:  result,
		Error:   rpcErr,
	}
}

// handleGetLanguages returns the supported language types.
func (s *server) handleGetLanguages() []string {
	return []string{"org.openrewrite.golang.tree.G$CompilationUnit"}
}

// parseRequest is the parameter type for Parse.
type parseRequest struct {
	Inputs     []parseInput `json:"inputs"`
	RelativeTo *string      `json:"relativeTo"`
}

// parseInput can be a path-based or text-based input.
type parseInput struct {
	Path       string `json:"path"`
	Text       string `json:"text"`
	SourcePath string `json:"sourcePath"`
}

// handleParse parses Go source files and returns their IDs.
func (s *server) handleParse(params json.RawMessage) (any, *rpcError) {
	var req parseRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	p := goparser.NewGoParser()
	ids := make([]string, 0, len(req.Inputs))
	for _, input := range req.Inputs {
		var sourcePath string
		var source string

		if input.Path != "" {
			absPath := input.Path
			data, err := os.ReadFile(absPath)
			if err != nil {
				return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Failed to read file: %v", err)}
			}
			source = string(data)
			if req.RelativeTo != nil && *req.RelativeTo != "" {
				rel, err := filepath.Rel(*req.RelativeTo, absPath)
				if err == nil {
					sourcePath = rel
				} else {
					sourcePath = absPath
				}
			} else {
				sourcePath = absPath
			}
		} else if input.Text != "" {
			source = input.Text
			sourcePath = input.SourcePath
			if sourcePath == "" {
				sourcePath = "<unknown>"
			}
		} else {
			continue
		}

		cu, err := p.Parse(sourcePath, source)
		if err != nil {
			s.logger.Printf("Parse error for %s: %v", sourcePath, err)
			return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Parse error: %v", err)}
		}
		id := cu.ID.String()
		s.localObjects[id] = cu
		ids = append(ids, id)
	}

	return ids, nil
}

// getObjectRequest is the parameter type for GetObject.
type getObjectRequest struct {
	ID             string `json:"id"`
	SourceFileType string `json:"sourceFileType"`
}

// handleGetObject serializes a local object for transfer to Java.
func (s *server) handleGetObject(params json.RawMessage) (any, *rpcError) {
	var req getObjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	obj := s.localObjects[req.ID]
	if obj == nil {
		return []rpc.RpcObjectData{
			{State: rpc.Delete},
			{State: rpc.EndOfObject},
		}, nil
	}

	before := s.remoteObjects[req.ID]

	// Collect all batches into a single result
	var result []rpc.RpcObjectData
	q := rpc.NewSendQueue(s.batchSize, func(batch []rpc.RpcObjectData) {
		result = append(result, batch...)
	}, s.localRefs)

	sender := &rpc.GoSender{}
	q.Send(obj, before, func(v any) {
		sender.Visit(v, q)
	})
	q.Put(rpc.RpcObjectData{State: rpc.EndOfObject})
	q.Flush()

	// Update remote tracking
	s.remoteObjects[req.ID] = obj

	return result, nil
}

// printRequest is the parameter type for Print.
type printRequest struct {
	TreeID         string `json:"treeId"`
	SourcePath     string `json:"sourcePath"`
	SourceFileType string `json:"sourceFileType"`
}

// handlePrint retrieves a tree from Java and prints it to source.
func (s *server) handlePrint(params json.RawMessage) (any, *rpcError) {
	var req printRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// Get the object from Java via bidirectional RPC
	obj := s.getObjectFromJava(req.TreeID, req.SourceFileType)
	if obj == nil {
		return "", nil
	}

	if t, ok := obj.(tree.Tree); ok {
		return printer.Print(t), nil
	}

	return "", &rpcError{Code: -32603, Message: "Object is not a Tree"}
}

// getObjectFromJava fetches an object from the Java side via bidirectional RPC.
// For Print, Java holds the (potentially modified) tree. We need to request it back.
func (s *server) getObjectFromJava(id string, sourceFileType string) any {
	before := s.remoteObjects[id]

	// Send GetObject request to Java
	reqParams := getObjectRequest{ID: id, SourceFileType: sourceFileType}
	paramsJSON, _ := json.Marshal(reqParams)
	rpcReq := map[string]any{
		"jsonrpc": "2.0",
		"id":      "go-GetObject",
		"method":  "GetObject",
		"params":  json.RawMessage(paramsJSON),
	}
	body, _ := json.Marshal(rpcReq)
	header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))
	s.writer.Write(append([]byte(header), body...))

	// Read response from Java
	resp, err := s.readMessage()
	if err != nil {
		s.logger.Printf("Error reading bidirectional response: %v", err)
		return nil
	}

	// Parse the result which contains RpcObjectData array
	resultData := resp.Result
	if resultData == nil {
		resultData = resp.Params // fallback for request-style messages
	}
	if resultData == nil {
		s.logger.Printf("No result data in bidirectional response")
		return nil
	}
	var respResult any
	if err := json.Unmarshal(resultData, &respResult); err != nil {
		s.logger.Printf("Error parsing response result: %v", err)
		return nil
	}

	batchData, ok := respResult.([]any)
	if !ok || len(batchData) == 0 {
		return nil
	}

	// Convert to RpcObjectData
	batch := make([]rpc.RpcObjectData, 0, len(batchData))
	for _, item := range batchData {
		if m, ok := item.(map[string]any); ok {
			batch = append(batch, rpc.ParseObjectData(m))
		}
	}

	batchIdx := 0
	q := rpc.NewReceiveQueue(s.remoteRefs, func() []rpc.RpcObjectData {
		if batchIdx >= len(batch) {
			return nil
		}
		result := batch[batchIdx:]
		batchIdx = len(batch)
		return result
	})

	receiver := &rpc.GoReceiver{}
	obj := q.Receive(before, func(v any) any {
		return receiver.Visit(v, q)
	})

	if obj != nil {
		s.remoteObjects[id] = obj
		s.localObjects[id] = obj
	}

	return obj
}

// installRecipesResponse is the response type for InstallRecipes.
type installRecipesResponse struct {
	RecipesInstalled int     `json:"recipesInstalled"`
	Version          *string `json:"version"`
}

// handleInstallRecipes handles recipe installation requests.
// The request params contain a "recipes" field that is either:
//   - a string (local file path)
//   - an object with "packageName" and optional "version" fields
func (s *server) handleInstallRecipes(params json.RawMessage) (any, *rpcError) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(params, &raw); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	recipesRaw, ok := raw["recipes"]
	if !ok {
		return nil, &rpcError{Code: -32602, Message: "Missing 'recipes' parameter"}
	}

	// Try to parse as a string (local path) first
	var localPath string
	if err := json.Unmarshal(recipesRaw, &localPath); err == nil {
		s.logger.Printf("InstallRecipes from local path: %s", localPath)
		// TODO: Load recipes from local Go module at localPath
		return &installRecipesResponse{RecipesInstalled: 0, Version: nil}, nil
	}

	// Otherwise parse as a package spec
	var pkg struct {
		PackageName string  `json:"packageName"`
		Version     *string `json:"version"`
	}
	if err := json.Unmarshal(recipesRaw, &pkg); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid recipes parameter: %v", err)}
	}

	s.logger.Printf("InstallRecipes package: %s version: %v", pkg.PackageName, pkg.Version)
	// TODO: Fetch and install Go recipe module from Git
	return &installRecipesResponse{RecipesInstalled: 0, Version: pkg.Version}, nil
}

// handleReset clears all cached state.
func (s *server) handleReset() bool {
	s.localObjects = make(map[string]any)
	s.remoteObjects = make(map[string]any)
	s.localRefs = make(map[uintptr]int)
	s.remoteRefs = make(map[int]any)
	return true
}
