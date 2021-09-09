/**
 * Removes specific Map case from final output that is part of webpack tools
 * but unused.  We don't want to shim this for nashorn.
 */

const fs = require('fs');
const path = require('path');

const WEBPACK_DIR = './nashornjs';

function removeErrorCache(content) {
  const target = 'var errorCache = new Map();';
  return content.replace(target, '');
}

fs.readdirSync(WEBPACK_DIR).map((file) => {
  const filePath = path.join(WEBPACK_DIR, file);
  const content = fs.readFileSync(filePath, 'utf-8');

  const newContent = removeErrorCache(content);

  fs.writeFileSync(filePath, newContent, 'utf-8');
});
