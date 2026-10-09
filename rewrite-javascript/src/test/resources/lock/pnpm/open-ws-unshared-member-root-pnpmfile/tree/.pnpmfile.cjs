module.exports = { hooks: { readPackage (pkg) { pkg.dependencies = { ...pkg.dependencies, 'is-positive': '1.0.0' }; return pkg } } }
