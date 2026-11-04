# Error Handling

## Exception handling strategies and logging patterns

Error handling patterns detected in the codebase including try-catch usage, exception types, handling strategies (log, rethrow, wrap, recover), and logging frameworks. Use this to handle errors consistently with existing patterns.

## Data Tables

### Error handling patterns

**File:** [`error-handling-patterns.csv`](error-handling-patterns.csv)

Error and exception handling patterns detected in the codebase.

| Column | Description |
|--------|-------------|
| Source path | The path to the source file. |
| Class name | The class containing the error handling. |
| Method name | The method containing the error handling. |
| Pattern type | The type of error handling pattern (try-catch, throws, global-handler, exception-handler-method). |
| Exception types | The exception types being handled or thrown. |
| Handling strategy | How the error is handled (log, rethrow, wrap, suppress, propagate, handle, ignore). |
| Logging framework | The logging framework used, if detected. |
| Log level | The log level used for error logging. |

