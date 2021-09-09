(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define("changeLicenseRecipe", [], factory);
	else if(typeof exports === 'object')
		exports["changeLicenseRecipe"] = factory();
	else
		root["changeLicenseRecipe"] = factory();
})(this, function() {
return /******/ (function() { // webpackBootstrap
/******/ 	"use strict";
/******/ 	// The require scope
/******/ 	var __webpack_require__ = {};
/******/ 	
/************************************************************************/
/******/ 	/* webpack/runtime/define property getters */
/******/ 	!function() {
/******/ 		// define getter functions for harmony exports
/******/ 		__webpack_require__.d = function(exports, definition) {
/******/ 			for(var key in definition) {
/******/ 				if(__webpack_require__.o(definition, key) && !__webpack_require__.o(exports, key)) {
/******/ 					Object.defineProperty(exports, key, { enumerable: true, get: definition[key] });
/******/ 				}
/******/ 			}
/******/ 		};
/******/ 	}();
/******/ 	
/******/ 	/* webpack/runtime/hasOwnProperty shorthand */
/******/ 	!function() {
/******/ 		__webpack_require__.o = function(obj, prop) { return Object.prototype.hasOwnProperty.call(obj, prop); }
/******/ 	}();
/******/ 	
/******/ 	/* webpack/runtime/make namespace object */
/******/ 	!function() {
/******/ 		// define __esModule on exports
/******/ 		__webpack_require__.r = function(exports) {
/******/ 			if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 				Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 			}
/******/ 			Object.defineProperty(exports, '__esModule', { value: true });
/******/ 		};
/******/ 	}();
/******/ 	
/************************************************************************/
var __webpack_exports__ = {};
// ESM COMPAT FLAG
__webpack_require__.r(__webpack_exports__);

// EXPORTS
__webpack_require__.d(__webpack_exports__, {
  "Options": function() { return /* binding */ Options; },
  "default": function() { return /* binding */ change_license_recipe; }
});

;// CONCATENATED MODULE: ./node_modules/@openrewrite/types/lib/openrewrite.ts
var OpenRewrite;

(function (OpenRewrite) {
  var Cursor = function () {
    function Cursor(parent, value) {
      this.parent = parent;
      this.value = value;
    }

    Cursor.prototype.getRoot = function () {
      var cursor = this;

      while (cursor.parent !== null) {
        cursor = cursor.parent;
      }

      return cursor;
    };

    Cursor.prototype.getParent = function (levels) {
      if (levels === void 0) {
        levels = 1;
      }

      var cursor = this;

      for (var i = 0; i < levels && cursor != null; i++) {
        cursor = cursor.parent;
      }

      return cursor;
    };

    Cursor.prototype.getValue = function () {
      return this.value;
    };

    Cursor.prototype.fork = function () {
      return new Cursor(parent == null ? null : this.parent.fork(), this.value);
    };

    Cursor.isScopeInCursorPath = function (scope, cursor) {
      if (!scope || !cursor || !isTree(scope) || !isTree(cursor.value)) {
        return false;
      }

      return scope.getId() === cursor.value.getId() ? true : Cursor.isScopeInCursorPath(scope, cursor.getParent());
    };

    Cursor.prototype.isScopeInPath = function (scope) {
      return Cursor.isScopeInCursorPath(scope, this);
    };

    return Cursor;
  }();

  OpenRewrite.Cursor = Cursor;

  function isTree(tree) {
    return tree.getId && tree.print;
  }

  OpenRewrite.isTree = isTree;

  var TreeVisitor = function () {
    function TreeVisitor() {}

    TreeVisitor.prototype.setCursor = function (cursor) {
      this.cursor = cursor;
    };

    TreeVisitor.prototype.doAfterVisit = function (visitor) {
      if (isRecipe(visitor)) {
        this.afterVisit.push(visitor.getVisitor());
      } else {
        this.afterVisit.push(visitor);
      }
    };

    TreeVisitor.prototype.getAfterVisit = function () {
      return [];
    };

    TreeVisitor.prototype.getLanguage = function () {
      return null;
    };

    TreeVisitor.prototype.getCursor = function () {
      if (this.cursor == null) {
        throw new Error('Cursoring is not enabled for this visitor.');
      }

      return this.cursor;
    };

    TreeVisitor.prototype.preVisit = function (tree, p) {
      return Delegate.preVisit(tree, p);
    };

    TreeVisitor.prototype.postVisit = function (tree, p) {
      return Delegate.postVisit(tree, p);
    };

    TreeVisitor.prototype.visit = function (tree, p, parent) {
      return Delegate.visit(tree, p, parent);
    };

    TreeVisitor.IS_DEBUGGING = false;
    return TreeVisitor;
  }();

  OpenRewrite.TreeVisitor = TreeVisitor;

  function isRecipe(recipe) {
    return Boolean(recipe.getVisitor);
  }

  OpenRewrite.isRecipe = isRecipe;

  var Recipe = function () {
    function Recipe(options) {}

    Recipe.prototype.getName = function () {
      return '';
    };

    Recipe.prototype.getDisplayName = function () {
      return '';
    };

    Recipe.prototype.getDescription = function () {
      return '';
    };

    Recipe.prototype.getTags = function () {
      return [];
    };

    Recipe.prototype.getLanguages = function () {
      return [];
    };

    Recipe.prototype.getRecipeList = function () {
      return this.recipeList;
    };

    Recipe.prototype.doNext = function (recipe) {
      if (!this.recipeList) {
        this.recipeList = [];
      }

      this.recipeList.push(recipe);
      return this;
    };

    Recipe.getOptions = function (recipe) {
      return recipe.options;
    };

    Recipe.prototype.getDescriptor = function () {
      return {
        name: this.getName(),
        displayName: this.getDisplayName(),
        description: this.getDescription(),
        tags: this.getTags(),
        options: this.options,
        languages: this.getLanguages(),
        recipeList: this.getRecipeList()
      };
    };

    Recipe.prototype.getSingleSourceApplicableTest = function () {
      return null;
    };

    Recipe.prototype.getVisitor = function () {
      return null;
    };

    return Recipe;
  }();

  OpenRewrite.Recipe = Recipe;

  var Markers = function () {
    function Markers(id, markers) {
      this.id = id;
      this.markers = markers;
    }
    /**
     * An id that can be used to identify a particular AST element, even after
     * transformations have taken place on it
     */


    Markers.prototype.getId = function () {
      return this.id;
    };

    Markers.prototype.print = function () {
      return '';
    };

    Markers.prototype.printTrimmed = function () {
      return '';
    };

    Markers.prototype.isAcceptable = function (v, p) {
      return false;
    };

    Markers.prototype.entries = function () {
      return this.markers;
    };

    Markers.prototype.add = function (marker) {
      this.markers.push(marker);
    };

    Markers.prototype.findAll = function (markerType) {
      return this.markers.filter(function (marker) {
        return marker instanceof markerType;
      });
    };

    Markers.prototype.findFirst = function (markerType) {
      return this.markers.find(function (marker) {
        return marker instanceof markerType;
      });
    };

    return Markers;
  }();

  OpenRewrite.Markers = Markers;
})(OpenRewrite || (OpenRewrite = {}));
;// CONCATENATED MODULE: ./node_modules/@openrewrite/types/index.ts
/// <reference path="./lib/java.d.ts" />

;// CONCATENATED MODULE: ./ts/change-license-recipe.ts
var __extends = undefined && undefined.__extends || function () {
  var _extendStatics = function extendStatics(d, b) {
    _extendStatics = Object.setPrototypeOf || {
      __proto__: []
    } instanceof Array && function (d, b) {
      d.__proto__ = b;
    } || function (d, b) {
      for (var p in b) {
        if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p];
      }
    };

    return _extendStatics(d, b);
  };

  return function (d, b) {
    if (typeof b !== "function" && b !== null) throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");

    _extendStatics(d, b);

    function __() {
      this.constructor = d;
    }

    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
  };
}();



var Options = function () {
  function Options() {}

  Options.prototype.getOptionsDescriptors = function () {
    return [{
      displayName: 'license',
      description: 'Enter the license',
      required: true
    }];
  };

  Options.prototype.setOptionsValue = function (values) {
    this.license = values['license'];
  };

  return Options;
}();



var ChangeLicenseRecipe = function (_super) {
  __extends(ChangeLicenseRecipe, _super);

  function ChangeLicenseRecipe(options) {
    var _this = _super.call(this, options) || this;

    _this.getDisplayName = function () {
      return "Change License";
    };

    _this.getDescription = function () {
      return 'Changes License in package.json';
    };

    _super.prototype.doNext.call(_this, {
      name: 'org.openrewrite.json.ChangeValue',
      options: {
        oldKeyPath: '$.license',
        value: (options === null || options === void 0 ? void 0 : options.license) || '',
        fileMatcher: '**/package.json'
      }
    });

    return _this;
  }

  ChangeLicenseRecipe.prototype.getTags = function () {
    return [''];
  };

  ChangeLicenseRecipe.prototype.getLanguages = function () {
    return ['json'];
  };

  return ChangeLicenseRecipe;
}(OpenRewrite.Recipe);

/* harmony default export */ const change_license_recipe = (ChangeLicenseRecipe);
/******/ 	return __webpack_exports__;
/******/ })()
;
});