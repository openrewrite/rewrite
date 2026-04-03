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
	"reflect"
	"runtime"
	"strconv"
	"strings"

	"github.com/google/uuid"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/installer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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
	remoteObjects map[string]any // forward direction: tracks what Java has from Go
	localRefs     map[uintptr]int
	remoteRefs    map[int]any
	batchSize     int

	// Separate state for reverse GetObject (Java→Go) to avoid conflating
	// with forward direction state
	reverseRemoteObjects map[string]any
	reverseRemoteRefs    map[int]any

	// Prepared recipe instances keyed by unique ID
	preparedRecipes map[string]recipe.Recipe

	// Tracing toggles for GetObject
	traceReceive bool
	traceSend    bool

	reader    *bufio.Reader
	writer    io.Writer
	logger    *log.Logger
	registry  *recipe.Registry
	installer *installer.Installer
}

func newServer() *server {
	logFile, err := os.CreateTemp("", "go-rpc-*.log")
	if err != nil {
		logFile = os.Stderr
	}

	reg := recipe.NewRegistry()
	reg.Activate(golang.Activate)

	logger := log.New(logFile, "", log.LstdFlags)

	inst := installer.NewInstaller()
	inst.Logger = logger.Printf

	return &server{
		localObjects:         make(map[string]any),
		remoteObjects:        make(map[string]any),
		localRefs:            make(map[uintptr]int),
		remoteRefs:           make(map[int]any),
		reverseRemoteObjects: make(map[string]any),
		reverseRemoteRefs:    make(map[int]any),
		preparedRecipes:      make(map[string]recipe.Recipe),
		batchSize:            1000,
		reader:    bufio.NewReader(os.Stdin),
		writer:    os.Stdout,
		logger:    logger,
		registry:  reg,
		installer: inst,
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

		resp := s.safeHandleRequest(req)
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

// safeHandleRequest wraps handleRequest with panic recovery.
func (s *server) safeHandleRequest(req *jsonRPCRequest) (resp *jsonRPCResponse) {
	defer func() {
		if r := recover(); r != nil {
			buf := make([]byte, 4096)
			n := runtime.Stack(buf, false)
			s.logger.Printf("PANIC in %s: %v\n%s", req.Method, r, buf[:n])
			resp = &jsonRPCResponse{
				JSONRPC: "2.0",
				ID:      req.ID,
				Error:   &rpcError{Code: -32603, Message: fmt.Sprintf("Internal error: %v", r)},
			}
		}
	}()
	return s.handleRequest(req)
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
	case "GetMarketplace":
		result, rpcErr = s.handleGetMarketplace(req.Params)
	case "PrepareRecipe":
		result, rpcErr = s.handlePrepareRecipe(req.Params)
	case "Visit":
		result, rpcErr = s.handleVisit(req.Params)
	case "Generate":
		result, rpcErr = s.handleGenerate(req.Params)
	case "TraceGetObject":
		result, rpcErr = s.handleTraceGetObject(req.Params)
	case "ParseProject":
		result, rpcErr = s.handleParseProject(req.Params)
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
	return []string{"org.openrewrite.golang.tree.Go$CompilationUnit"}
}

// parseRequest is the parameter type for Parse.
type parseRequest struct {
	Inputs     []parseInput `json:"inputs"`
	RelativeTo *string      `json:"relativeTo"`
}

// parseInput can be a path-based or text-based input.
// It supports two JSON forms:
//   - A bare string (treated as a file path)
//   - A structured object with path, text, and sourcePath fields
type parseInput struct {
	Path       string `json:"path"`
	Text       string `json:"text"`
	SourcePath string `json:"sourcePath"`
}

func (p *parseInput) UnmarshalJSON(data []byte) error {
	// Try bare string first (Java PathInput serializes as @JsonValue string)
	var s string
	if err := json.Unmarshal(data, &s); err == nil {
		p.Path = s
		return nil
	}
	// Otherwise unmarshal as a structured object
	type alias parseInput
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*p = parseInput(a)
	return nil
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

		if input.Text != "" {
			// Text-based input (inline source from tests or recipe framework)
			source = input.Text
			sourcePath = input.SourcePath
			if sourcePath == "" {
				sourcePath = "<unknown>"
			}
		} else {
			// File-path-based input (mod build sends file paths)
			filePath := input.Path
			if filePath == "" {
				filePath = input.SourcePath
			}
			if filePath == "" {
				continue
			}
			absPath := filePath
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
		}

		cu, parseErr := func() (cu *tree.CompilationUnit, err error) {
			defer func() {
				if r := recover(); r != nil {
					err = fmt.Errorf("panic: %v", r)
				}
			}()
			return p.Parse(sourcePath, source)
		}()
		if parseErr != nil {
			s.logger.Printf("Parse error for %s: %v", sourcePath, parseErr)
			pe := tree.NewParseError(sourcePath, source, parseErr)
			id := pe.Ident.String()
			s.localObjects[id] = pe
			ids = append(ids, id)
			continue
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
	// Use a fresh ref map for each GetObject to avoid ref ID collisions
	// between the reverse direction (Java→Go) and forward direction (Go→Java).
	localRefs := make(map[uintptr]int)

	// Collect all batches into a single result
	var result []rpc.RpcObjectData
	q := rpc.NewSendQueue(s.batchSize, func(batch []rpc.RpcObjectData) {
		result = append(result, batch...)
	}, localRefs)

	sender := rpc.NewGoSender()
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
	TreeID         string  `json:"treeId"`
	SourcePath     string  `json:"sourcePath"`
	SourceFileType string  `json:"sourceFileType"`
	MarkerPrinter  *string `json:"markerPrinter"`
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

	// Map markerPrinter from the request to the Go MarkerPrinter
	mp := mapMarkerPrinter(req.MarkerPrinter)

	if cu, ok := obj.(*tree.CompilationUnit); ok {
		if mp != nil {
			return printer.PrintWithMarkers(cu, mp), nil
		}
		return printer.Print(cu), nil
	}
	if t, ok := obj.(tree.Tree); ok {
		if mp != nil {
			return printer.PrintWithMarkers(t, mp), nil
		}
		return printer.Print(t), nil
	}

	return "", &rpcError{Code: -32603, Message: "Object is not a Tree"}
}

// mapMarkerPrinter maps the RPC marker printer string to the Go MarkerPrinter.
func mapMarkerPrinter(mp *string) printer.MarkerPrinter {
	if mp == nil {
		return nil
	}
	switch *mp {
	case "DEFAULT":
		return printer.DefaultMarkerPrinter
	case "SEARCH_MARKERS_ONLY":
		return printer.SearchOnlyMarkerPrinter
	case "FENCED":
		return printer.FencedMarkerPrinter
	case "SANITIZED":
		return printer.SanitizedMarkerPrinter
	default:
		return nil
	}
}

// getObjectFromJava fetches an object from the Java side via bidirectional RPC.
// For Print, Java holds the (potentially modified) tree. We need to request it back.
// Supports multi-batch transfers: each GetObject call returns one batch, and
// subsequent calls are made until the END_OF_OBJECT marker is received.
func (s *server) getObjectFromJava(id string, sourceFileType string) any {
	// Use reverse-direction tracking if available; otherwise fall back to
	// localObjects (the tree Go originally parsed and sent to Java via forward
	// GetObject). This matches Java's remoteObjects baseline.
	before := s.reverseRemoteObjects[id]
	if before == nil {
		before = s.localObjects[id]
	}

	// fetchBatch sends a GetObject request to Java and reads one batch of RpcObjectData.
	fetchBatch := func() []rpc.RpcObjectData {
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

		resp, err := s.readMessage()
		if err != nil {
			s.logger.Printf("Error reading bidirectional response: %v", err)
			return nil
		}

		resultData := resp.Result
		if resultData == nil {
			resultData = resp.Params
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

		batch := make([]rpc.RpcObjectData, 0, len(batchData))
		for _, item := range batchData {
			if m, ok := item.(map[string]any); ok {
				batch = append(batch, rpc.ParseObjectData(m))
			}
		}
		return batch
	}

	q := rpc.NewReceiveQueue(s.reverseRemoteRefs, fetchBatch)

	receiver := rpc.NewGoReceiver()
	obj := q.Receive(before, func(v any) any {
		return receiver.Visit(v, q)
	})

	// Consume the END_OF_OBJECT sentinel if present
	if len(q.PeekBatch()) > 0 && q.PeekBatch()[0].State == rpc.EndOfObject {
		q.Take()
	}

	if obj != nil {
		s.reverseRemoteObjects[id] = obj
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

	beforeCount := len(s.registry.AllRecipes())

	// Try to parse as a string (local path) first
	var localPath string
	if err := json.Unmarshal(recipesRaw, &localPath); err == nil {
		s.logger.Printf("InstallRecipes from local path: %s", localPath)
		_, err := s.installer.InstallFromPath(localPath, s.registry)
		if err != nil {
			return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Install from path failed: %v", err)}
		}
		afterCount := len(s.registry.AllRecipes())
		return &installRecipesResponse{
			RecipesInstalled: afterCount - beforeCount,
			Version:          nil,
		}, nil
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
	info, err := s.installer.InstallFromPackage(pkg.PackageName, pkg.Version, s.registry)
	if err != nil {
		return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Install package failed: %v", err)}
	}

	afterCount := len(s.registry.AllRecipes())
	var version *string
	if info.Version != "" {
		version = &info.Version
	}
	return &installRecipesResponse{
		RecipesInstalled: afterCount - beforeCount,
		Version:          version,
	}, nil
}

// handleReset clears all cached state.
func (s *server) handleReset() bool {
	s.localObjects = make(map[string]any)
	s.remoteObjects = make(map[string]any)
	s.localRefs = make(map[uintptr]int)
	s.remoteRefs = make(map[int]any)
	s.reverseRemoteObjects = make(map[string]any)
	s.reverseRemoteRefs = make(map[int]any)
	s.preparedRecipes = make(map[string]recipe.Recipe)
	return true
}

// marketplaceRow matches Java's GetMarketplaceResponse.Row.
type marketplaceRow struct {
	Descriptor    marketplaceDescriptor     `json:"descriptor"`
	CategoryPaths [][]marketplaceCategory   `json:"categoryPaths"`
}

// marketplaceDescriptor matches Java's RecipeDescriptor (minimal fields).
type marketplaceDescriptor struct {
	Name                         string                    `json:"name"`
	DisplayName                  string                    `json:"displayName"`
	InstanceName                 string                    `json:"instanceName"`
	Description                  string                    `json:"description"`
	Tags                         []string                  `json:"tags"`
	EstimatedEffortPerOccurrence *string                   `json:"estimatedEffortPerOccurrence"`
	Options                      []marketplaceOption       `json:"options"`
	Preconditions                []marketplaceDescriptor   `json:"preconditions"`
	RecipeList                   []marketplaceDescriptor   `json:"recipeList"`
	DataTables                   []any                     `json:"dataTables"`
	Maintainers                  []any                     `json:"maintainers"`
	Contributors                 []any                     `json:"contributors"`
	Examples                     []any                     `json:"examples"`
	Source                       *string                   `json:"source"`
}

type marketplaceOption struct {
	Name        string  `json:"name"`
	DisplayName string  `json:"displayName"`
	Description string  `json:"description"`
	Example     *string `json:"example"`
	Required    bool    `json:"required"`
	Type        string  `json:"type"`
	Value       any     `json:"value"`
	Valid       []any   `json:"valid"`
}

// marketplaceCategory matches Java's CategoryDescriptor.
type marketplaceCategory struct {
	DisplayName string   `json:"displayName"`
	PackageName string   `json:"packageName"`
	Description string   `json:"description"`
	Tags        []string `json:"tags"`
	Root        bool     `json:"root"`
	Priority    int      `json:"priority"`
}

// handleGetMarketplace returns available recipes from the Go registry.
// The Java side expects a JSON array of Row{descriptor, categoryPaths}.
func (s *server) handleGetMarketplace(params json.RawMessage) (any, *rpcError) {
	var rows []marketplaceRow
	for _, reg := range s.registry.AllRegistrations() {
		desc := reg.Descriptor

		var options []marketplaceOption
		for _, opt := range desc.Options {
			var example *string
			if opt.Example != "" {
				example = &opt.Example
			}
			options = append(options, marketplaceOption{
				Name:        opt.Name,
				DisplayName: opt.DisplayName,
				Description: opt.Description,
				Example:     example,
				Required:    opt.Required,
				Type:        "String",
			})
		}
		if options == nil {
			options = []marketplaceOption{}
		}

		var categoryPath []marketplaceCategory
		for _, cat := range reg.Categories {
			categoryPath = append(categoryPath, marketplaceCategory{
				DisplayName: cat.DisplayName,
				PackageName: "",
				Description: cat.Description,
				Tags:        []string{},
				Root:        false,
				Priority:    0,
			})
		}

		rows = append(rows, marketplaceRow{
			Descriptor: marketplaceDescriptor{
				Name:           desc.Name,
				DisplayName:    desc.DisplayName,
				InstanceName:   desc.DisplayName,
				Description:    desc.Description,
				Tags:           nonNil(desc.Tags),
				Options:        options,
				Preconditions:  []marketplaceDescriptor{},
				RecipeList:     []marketplaceDescriptor{},
				DataTables:     []any{},
				Maintainers:    []any{},
				Contributors:   []any{},
				Examples:       []any{},
			},
			CategoryPaths: [][]marketplaceCategory{categoryPath},
		})
	}
	if rows == nil {
		rows = []marketplaceRow{}
	}
	return rows, nil
}

func nonNil(s []string) []string {
	if s == nil {
		return []string{}
	}
	return s
}

// prepareRecipeRequest is the parameter type for PrepareRecipe.
type prepareRecipeRequest struct {
	ID      string         `json:"id"`
	Options map[string]any `json:"options"`
}

// prepareRecipeResponse contains the prepared recipe info.
type prepareRecipeResponse struct {
	ID                  string               `json:"id"`
	Descriptor          recipeDescResponse   `json:"descriptor"`
	EditVisitor         string               `json:"editVisitor"`
	EditPreconditions   []any                `json:"editPreconditions"`
	ScanVisitor         *string              `json:"scanVisitor,omitempty"`
	ScanPreconditions   []any                `json:"scanPreconditions"`
	DelegatesTo         *delegatesToResponse `json:"delegatesTo,omitempty"`
}

type recipeDescResponse struct {
	Name                         string               `json:"name"`
	DisplayName                  string               `json:"displayName"`
	InstanceName                 string               `json:"instanceName"`
	Description                  string               `json:"description"`
	Tags                         []string             `json:"tags"`
	EstimatedEffortPerOccurrence *string              `json:"estimatedEffortPerOccurrence"`
	Options                      []optionDescResponse `json:"options"`
	Preconditions                []any                `json:"preconditions"`
	RecipeList                   []any                `json:"recipeList"`
	DataTables                   []any                `json:"dataTables"`
	Maintainers                  []any                `json:"maintainers"`
	Contributors                 []any                `json:"contributors"`
	Examples                     []any                `json:"examples"`
	Source                       *string              `json:"source"`
}

type optionDescResponse struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
	Example     string `json:"example,omitempty"`
	Required    bool   `json:"required"`
}

type delegatesToResponse struct {
	RecipeName string         `json:"recipeName"`
	Options    map[string]any `json:"options"`
}

// handlePrepareRecipe instantiates a recipe by name with options.
func (s *server) handlePrepareRecipe(params json.RawMessage) (any, *rpcError) {
	var req prepareRecipeRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	reg, ok := s.registry.FindRecipe(req.ID)
	if !ok {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Unknown recipe: %s", req.ID)}
	}

	// Create recipe instance with options
	instance := reg.Constructor(req.Options)

	var desc recipe.RecipeDescriptor
	if instance != nil {
		desc = recipe.Describe(instance)
	} else {
		// Installer-loaded recipes have no constructor; use stored descriptor
		desc = reg.Descriptor
	}

	// Generate unique ID and store the prepared recipe
	recipeID := uuid.New().String()
	s.preparedRecipes[recipeID] = instance

	resp := prepareRecipeResponse{
		ID: req.ID,
		Descriptor: recipeDescResponse{
			Name:          desc.Name,
			DisplayName:   desc.DisplayName,
			InstanceName:  desc.DisplayName,
			Description:   desc.Description,
			Tags:          []string{},
			Options:       []optionDescResponse{},
			Preconditions: []any{},
			RecipeList:    []any{},
			DataTables:    []any{},
			Maintainers:   []any{},
			Contributors:  []any{},
			Examples:      []any{},
		},
		EditVisitor:       "edit:" + recipeID,
		EditPreconditions: []any{},
		ScanPreconditions: []any{},
	}

	// Check if this is a scanning recipe
	if instance != nil {
		if _, isScan := instance.(recipe.ScanningRecipe); isScan {
			scanVis := "scan:" + recipeID
			resp.ScanVisitor = &scanVis
		}
	}

	// Map options
	for _, opt := range desc.Options {
		resp.Descriptor.Options = append(resp.Descriptor.Options, optionDescResponse{
			Name:        opt.Name,
			DisplayName: opt.DisplayName,
			Description: opt.Description,
			Example:     opt.Example,
			Required:    opt.Required,
		})
	}

	// Check for delegation
	if instance != nil {
		if del, ok := instance.(recipe.DelegatesTo); ok {
			resp.DelegatesTo = &delegatesToResponse{
				RecipeName: del.JavaRecipeName(),
				Options:    del.JavaOptions(),
			}
		}
	}

	return resp, nil
}

// visitRequest is the parameter type for Visit.
type visitRequest struct {
	Visitor        string   `json:"visitor"`
	TreeID         string   `json:"treeId"`
	SourceFileType string   `json:"sourceFileType"`
	PID            *string  `json:"p"`
	Cursor         []string `json:"cursor"`
}

// visitResponse is the response type for Visit.
type visitResponse struct {
	Modified bool `json:"modified"`
}

// handleVisit applies a prepared recipe's visitor to a tree.
func (s *server) handleVisit(params json.RawMessage) (any, *rpcError) {
	var req visitRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// Parse visitor name: "edit:<recipeId>" or "scan:<recipeId>"
	parts := strings.SplitN(req.Visitor, ":", 2)
	if len(parts) != 2 {
		return nil, &rpcError{Code: -32602, Message: "Invalid visitor name: " + req.Visitor}
	}
	phase := parts[0]
	recipeID := parts[1]

	// Look up the prepared recipe
	r, ok := s.preparedRecipes[recipeID]
	if !ok {
		return nil, &rpcError{Code: -32602, Message: "Unknown recipe: " + recipeID}
	}
	if r == nil {
		// Installer-loaded recipes have no implementation in Go
		return &visitResponse{Modified: false}, nil
	}

	// Get the tree from Java via bidirectional RPC
	treeObj := s.getObjectFromJava(req.TreeID, req.SourceFileType)
	if treeObj == nil {
		return &visitResponse{Modified: false}, nil
	}

	// Get the visitor based on phase
	var v recipe.TreeVisitor
	switch phase {
	case "edit":
		v = r.Editor()
	case "scan":
		// ScanningRecipe not yet supported
		return &visitResponse{Modified: false}, nil
	default:
		return nil, &rpcError{Code: -32602, Message: "Unknown phase: " + phase}
	}

	if v == nil {
		return &visitResponse{Modified: false}, nil
	}

	// Apply the visitor
	ctx := recipe.NewExecutionContext()
	treeNode, ok := treeObj.(tree.Tree)
	if !ok {
		return &visitResponse{Modified: false}, nil
	}
	before := treeNode
	after := v.Visit(treeNode, ctx)

	// Check if modified by pointer identity (not value equality,
	// since tree nodes contain slices which are not comparable).
	modified := !treeIdentical(before, after)

	// Store the result — update both localObjects (for forward GetObject)
	// and reverseRemoteObjects (baseline for reverse getObjectFromJava in Print)
	if after != nil {
		s.localObjects[req.TreeID] = after
		s.reverseRemoteObjects[req.TreeID] = after
	} else {
		delete(s.localObjects, req.TreeID)
	}

	return &visitResponse{Modified: modified}, nil
}

// generateRequest is the parameter type for Generate.
type generateRequest struct {
	ID  string  `json:"id"`
	PID *string `json:"p"`
}

// generateResponse is the response type for Generate.
type generateResponse struct {
	IDs             []string `json:"ids"`
	SourceFileTypes []string `json:"sourceFileTypes"`
}

// handleGenerate returns any new source files generated by a scanning recipe.
func (s *server) handleGenerate(params json.RawMessage) (any, *rpcError) {
	var req generateRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// For now, Go doesn't support ScanningRecipe.generate()
	return &generateResponse{
		IDs:             []string{},
		SourceFileTypes: []string{},
	}, nil
}

// traceGetObjectRequest is the parameter type for TraceGetObject.
type traceGetObjectRequest struct {
	Receive bool `json:"receive"`
	Send    bool `json:"send"`
}

// handleTraceGetObject toggles tracing for GetObject calls.
func (s *server) handleTraceGetObject(params json.RawMessage) (any, *rpcError) {
	var req traceGetObjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	s.traceReceive = req.Receive
	s.traceSend = req.Send
	s.logger.Printf("TraceGetObject: receive=%v send=%v", req.Receive, req.Send)

	return true, nil
}

// parseProjectRequest is the parameter type for ParseProject.
type parseProjectRequest struct {
	ProjectPath string   `json:"projectPath"`
	Exclusions  []string `json:"exclusions"`
	RelativeTo  *string  `json:"relativeTo"`
}

// parseProjectResponseItem describes a parsed source file.
type parseProjectResponseItem struct {
	ID             string `json:"id"`
	SourceFileType string `json:"sourceFileType"`
}

// handleParseProject discovers and parses all Go files in a project directory.
func (s *server) handleParseProject(params json.RawMessage) (any, *rpcError) {
	var req parseProjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	s.logger.Printf("ParseProject: path=%s", req.ProjectPath)

	// Discover all .go files in the project directory
	var goFiles []string
	err := filepath.Walk(req.ProjectPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			base := filepath.Base(path)
			// Skip common non-source directories
			if base == "vendor" || base == "node_modules" || base == ".git" || base == "testdata" {
				return filepath.SkipDir
			}
			// Check exclusions
			for _, excl := range req.Exclusions {
				if matched, _ := filepath.Match(excl, base); matched {
					return filepath.SkipDir
				}
			}
			return nil
		}
		if strings.HasSuffix(path, ".go") && !strings.HasSuffix(path, "_test.go") {
			goFiles = append(goFiles, path)
		}
		return nil
	})
	if err != nil {
		return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Walk error: %v", err)}
	}

	// Parse each file
	p := goparser.NewGoParser()
	var items []parseProjectResponseItem
	for _, goFile := range goFiles {
		data, err := os.ReadFile(goFile)
		if err != nil {
			s.logger.Printf("Skip %s: %v", goFile, err)
			continue
		}

		sourcePath := goFile
		if req.RelativeTo != nil && *req.RelativeTo != "" {
			if rel, err := filepath.Rel(*req.RelativeTo, goFile); err == nil {
				sourcePath = rel
			}
		}

		cu, err := p.Parse(sourcePath, string(data))
		if err != nil {
			s.logger.Printf("Parse error %s: %v", goFile, err)
			continue
		}

		id := cu.ID.String()
		s.localObjects[id] = cu
		items = append(items, parseProjectResponseItem{
			ID:             id,
			SourceFileType: "org.openrewrite.golang.tree.Go$CompilationUnit",
		})
	}

	s.logger.Printf("ParseProject: parsed %d files", len(items))
	return items, nil
}

// treeIdentical compares two tree nodes by pointer identity.
// Tree nodes contain slices (uncomparable in Go), so we compare
// the underlying pointer addresses via reflect.
func treeIdentical(a, b any) bool {
	if a == nil && b == nil {
		return true
	}
	if a == nil || b == nil {
		return false
	}
	va := reflect.ValueOf(a)
	vb := reflect.ValueOf(b)
	if va.Kind() == reflect.Ptr && vb.Kind() == reflect.Ptr {
		return va.Pointer() == vb.Pointer()
	}
	return false
}
