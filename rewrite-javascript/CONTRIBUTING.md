### Development Setup

For the time being the setup is unfortunately a bit complicated due to the dependencies between `rewrite-javascript` and `rewrite-remote`.
To be able to make changes to both these projects and make sure that they work together, we use `npm link`.

1. Start by cloning both `openrewrite/rewrite-javascript` (this repo) and `moderneinc/rewrite-remote`
2. For both repos perform the following steps:
   1. Run the build using `npm run build`
   2. Run `npm link`. This last step only needs to be done once
3. Now `npm link` needs to be run in either project to link it into `node_modules`:
   1. For `openrewrite/rewrite-javascript` run `npm link @openrewrite/rewrite-remote`
   2. For `moderneinc/rewrite-remote` run `npm link @openrewrite/rewrite`
4. Now you are basically all set. As the tests read the production source files from `dist` instead of `src` it is recommended to have `tsc --watch` running which you can do using `npm run dev`

For development it is recommended to open the `openrewrite` subdirectory as a project in WebStorm.

### Running Tests

The parser tests typically use LST remoting to print the LST back to text in order to verify the print idempotence.
In order to get this working the Java remoting server must already be running.
Start the Java remoting server directly from IntelliJ IDEA (`rewrite-javascript` repo) by running the shared `RemotingServer` configuration.
Now it should be possible to run the Jest tests from the terminal or from the WebStorm IDE.