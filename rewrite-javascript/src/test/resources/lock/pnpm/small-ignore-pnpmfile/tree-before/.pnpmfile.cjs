module.exports = { hooks: { readPackage (pkg) { pkg.dependencies = { ...pkg.dependencies, "is-positive": "3.1.0" }; return pkg } } }
