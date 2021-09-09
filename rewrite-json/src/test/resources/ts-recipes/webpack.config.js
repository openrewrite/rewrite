const path = require('path');
const NodePolyfillPlugin = require('node-polyfill-webpack-plugin');
const fs = require('fs');
const camelCase = require('lodash.camelcase');

const getFiles = (dir) => {
  const directoryPath = path.join(__dirname, dir);
  const files = fs.readdirSync(directoryPath);
  return files.map((file) => {
    return path.join(dir, file);
  });
};

const targetFiles = [...getFiles('./ts')];

const entries = targetFiles.reduce((result, file) => {
  const filePath = path.join(__dirname, file);
  const fileName = path.basename(filePath);
  const fileNameWithoutExtension = path.basename(filePath, path.extname(fileName));
  const fileNameCamelCase = camelCase(fileNameWithoutExtension);

  result[fileNameCamelCase] = filePath;

  return result;
}, {});

module.exports = {
  cache: false,
  mode: 'production',
  entry: {
    ...entries
  },
  module: {
    rules: [
      {
        test: /\.ts?$/,
        use: [
          {
            loader: 'babel-loader'
          },
          {
            loader: 'ts-loader',
            options: { allowTsInNodeModules: true }
          }
        ]
      }
    ]
  },
  resolve: {
    extensions: ['.ts', '.js'],
    fallback: {
      fs: false
    }
  },
  output: {
    library: {
      name: '[name]',
      type: 'umd'
    },
    libraryTarget: 'umd',
    umdNamedDefine: true,
    globalObject: 'this',
    path: path.resolve(__dirname, 'nashornjs'),
    environment: {
      arrowFunction: false
    }
  },
  plugins: [new NodePolyfillPlugin()],
  optimization: {
    minimize: false
  }
};
