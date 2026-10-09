module.exports = { hooks: { readPackage (pkg) { if (pkg.name === 'once') pkg.dependencies = { ...pkg.dependencies, ms: '2.1.3' }; return pkg } } }
