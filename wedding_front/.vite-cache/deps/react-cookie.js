import { r as __toESM } from "./chunk-B-1-B7_t.js";
import { t as require_react } from "./react.js";
//#region node_modules/universal-cookie/esm/index.mjs
var dist = {};
var hasRequiredDist;
function requireDist() {
	if (hasRequiredDist) return dist;
	hasRequiredDist = 1;
	Object.defineProperty(dist, "__esModule", { value: true });
	dist.parseCookie = parseCookie;
	dist.parse = parseCookie;
	dist.stringifyCookie = stringifyCookie;
	dist.stringifySetCookie = stringifySetCookie;
	dist.serialize = stringifySetCookie;
	dist.parseSetCookie = parseSetCookie;
	dist.stringifySetCookie = stringifySetCookie;
	dist.serialize = stringifySetCookie;
	/**
	* RegExp to match cookie-name in RFC 6265 sec 4.1.1
	* This refers out to the obsoleted definition of token in RFC 2616 sec 2.2
	* which has been replaced by the token definition in RFC 7230 appendix B.
	*
	* cookie-name       = token
	* token             = 1*tchar
	* tchar             = "!" / "#" / "$" / "%" / "&" / "'" /
	*                     "*" / "+" / "-" / "." / "^" / "_" /
	*                     "`" / "|" / "~" / DIGIT / ALPHA
	*
	* Note: Allowing more characters - https://github.com/jshttp/cookie/issues/191
	* Allow same range as cookie value, except `=`, which delimits end of name.
	*/
	const cookieNameRegExp = /^[\u0021-\u003A\u003C\u003E-\u007E]+$/;
	/**
	* RegExp to match cookie-value in RFC 6265 sec 4.1.1
	*
	* cookie-value      = *cookie-octet / ( DQUOTE *cookie-octet DQUOTE )
	* cookie-octet      = %x21 / %x23-2B / %x2D-3A / %x3C-5B / %x5D-7E
	*                     ; US-ASCII characters excluding CTLs,
	*                     ; whitespace DQUOTE, comma, semicolon,
	*                     ; and backslash
	*
	* Allowing more characters: https://github.com/jshttp/cookie/issues/191
	* Comma, backslash, and DQUOTE are not part of the parsing algorithm.
	*/
	const cookieValueRegExp = /^[\u0021-\u003A\u003C-\u007E]*$/;
	/**
	* RegExp to match domain-value in RFC 6265 sec 4.1.1
	*
	* domain-value      = <subdomain>
	*                     ; defined in [RFC1034], Section 3.5, as
	*                     ; enhanced by [RFC1123], Section 2.1
	* <subdomain>       = <label> | <subdomain> "." <label>
	* <label>           = <let-dig> [ [ <ldh-str> ] <let-dig> ]
	*                     Labels must be 63 characters or less.
	*                     'let-dig' not 'letter' in the first char, per RFC1123
	* <ldh-str>         = <let-dig-hyp> | <let-dig-hyp> <ldh-str>
	* <let-dig-hyp>     = <let-dig> | "-"
	* <let-dig>         = <letter> | <digit>
	* <letter>          = any one of the 52 alphabetic characters A through Z in
	*                     upper case and a through z in lower case
	* <digit>           = any one of the ten digits 0 through 9
	*
	* Keep support for leading dot: https://github.com/jshttp/cookie/issues/173
	*
	* > (Note that a leading %x2E ("."), if present, is ignored even though that
	* character is not permitted, but a trailing %x2E ("."), if present, will
	* cause the user agent to ignore the attribute.)
	*/
	const domainValueRegExp = /^([.]?[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)([.][a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$/i;
	/**
	* RegExp to match path-value in RFC 6265 sec 4.1.1
	*
	* path-value        = <any CHAR except CTLs or ";">
	* CHAR              = %x01-7F
	*                     ; defined in RFC 5234 appendix B.1
	*/
	const pathValueRegExp = /^[\u0020-\u003A\u003D-\u007E]*$/;
	/**
	* RegExp to match max-age-value in RFC 6265 sec 5.6.2
	*/
	const maxAgeRegExp = /^-?\d+$/;
	const __toString = Object.prototype.toString;
	const NullObject = /* @__PURE__ */ (() => {
		const C = function() {};
		C.prototype = Object.create(null);
		return C;
	})();
	/**
	* Parse a `Cookie` header.
	*
	* Parse the given cookie header string into an object
	* The object has the various cookies as keys(names) => values
	*/
	function parseCookie(str, options) {
		const obj = new NullObject();
		const len = str.length;
		if (len < 2) return obj;
		const dec = options?.decode || decode;
		let index = 0;
		do {
			const eqIdx = eqIndex(str, index, len);
			if (eqIdx === -1) break;
			const endIdx = endIndex(str, index, len);
			if (eqIdx > endIdx) {
				index = str.lastIndexOf(";", eqIdx - 1) + 1;
				continue;
			}
			const key = valueSlice(str, index, eqIdx);
			if (obj[key] === void 0) obj[key] = dec(valueSlice(str, eqIdx + 1, endIdx));
			index = endIdx + 1;
		} while (index < len);
		return obj;
	}
	/**
	* Stringifies an object into an HTTP `Cookie` header.
	*/
	function stringifyCookie(cookie, options) {
		const enc = options?.encode || encodeURIComponent;
		const cookieStrings = [];
		for (const name of Object.keys(cookie)) {
			const val = cookie[name];
			if (val === void 0) continue;
			if (!cookieNameRegExp.test(name)) throw new TypeError(`cookie name is invalid: ${name}`);
			const value = enc(val);
			if (!cookieValueRegExp.test(value)) throw new TypeError(`cookie val is invalid: ${val}`);
			cookieStrings.push(`${name}=${value}`);
		}
		return cookieStrings.join("; ");
	}
	function stringifySetCookie(_name, _val, _opts) {
		const cookie = typeof _name === "object" ? _name : {
			..._opts,
			name: _name,
			value: String(_val)
		};
		const enc = (typeof _val === "object" ? _val : _opts)?.encode || encodeURIComponent;
		if (!cookieNameRegExp.test(cookie.name)) throw new TypeError(`argument name is invalid: ${cookie.name}`);
		const value = cookie.value ? enc(cookie.value) : "";
		if (!cookieValueRegExp.test(value)) throw new TypeError(`argument val is invalid: ${cookie.value}`);
		let str = cookie.name + "=" + value;
		if (cookie.maxAge !== void 0) {
			if (!Number.isInteger(cookie.maxAge)) throw new TypeError(`option maxAge is invalid: ${cookie.maxAge}`);
			str += "; Max-Age=" + cookie.maxAge;
		}
		if (cookie.domain) {
			if (!domainValueRegExp.test(cookie.domain)) throw new TypeError(`option domain is invalid: ${cookie.domain}`);
			str += "; Domain=" + cookie.domain;
		}
		if (cookie.path) {
			if (!pathValueRegExp.test(cookie.path)) throw new TypeError(`option path is invalid: ${cookie.path}`);
			str += "; Path=" + cookie.path;
		}
		if (cookie.expires) {
			if (!isDate(cookie.expires) || !Number.isFinite(cookie.expires.valueOf())) throw new TypeError(`option expires is invalid: ${cookie.expires}`);
			str += "; Expires=" + cookie.expires.toUTCString();
		}
		if (cookie.httpOnly) str += "; HttpOnly";
		if (cookie.secure) str += "; Secure";
		if (cookie.partitioned) str += "; Partitioned";
		if (cookie.priority) switch (typeof cookie.priority === "string" ? cookie.priority.toLowerCase() : void 0) {
			case "low":
				str += "; Priority=Low";
				break;
			case "medium":
				str += "; Priority=Medium";
				break;
			case "high":
				str += "; Priority=High";
				break;
			default: throw new TypeError(`option priority is invalid: ${cookie.priority}`);
		}
		if (cookie.sameSite) switch (typeof cookie.sameSite === "string" ? cookie.sameSite.toLowerCase() : cookie.sameSite) {
			case true:
			case "strict":
				str += "; SameSite=Strict";
				break;
			case "lax":
				str += "; SameSite=Lax";
				break;
			case "none":
				str += "; SameSite=None";
				break;
			default: throw new TypeError(`option sameSite is invalid: ${cookie.sameSite}`);
		}
		return str;
	}
	/**
	* Deserialize a `Set-Cookie` header into an object.
	*
	* deserialize('foo=bar; httpOnly')
	*   => { name: 'foo', value: 'bar', httpOnly: true }
	*/
	function parseSetCookie(str, options) {
		const dec = options?.decode || decode;
		const len = str.length;
		const endIdx = endIndex(str, 0, len);
		const eqIdx = eqIndex(str, 0, endIdx);
		const setCookie = eqIdx === -1 ? {
			name: "",
			value: dec(valueSlice(str, 0, endIdx))
		} : {
			name: valueSlice(str, 0, eqIdx),
			value: dec(valueSlice(str, eqIdx + 1, endIdx))
		};
		let index = endIdx + 1;
		while (index < len) {
			const endIdx = endIndex(str, index, len);
			const eqIdx = eqIndex(str, index, endIdx);
			const attr = eqIdx === -1 ? valueSlice(str, index, endIdx) : valueSlice(str, index, eqIdx);
			const val = eqIdx === -1 ? void 0 : valueSlice(str, eqIdx + 1, endIdx);
			switch (attr.toLowerCase()) {
				case "httponly":
					setCookie.httpOnly = true;
					break;
				case "secure":
					setCookie.secure = true;
					break;
				case "partitioned":
					setCookie.partitioned = true;
					break;
				case "domain":
					setCookie.domain = val;
					break;
				case "path":
					setCookie.path = val;
					break;
				case "max-age":
					if (val && maxAgeRegExp.test(val)) setCookie.maxAge = Number(val);
					break;
				case "expires":
					if (!val) break;
					const date = new Date(val);
					if (Number.isFinite(date.valueOf())) setCookie.expires = date;
					break;
				case "priority":
					if (!val) break;
					const priority = val.toLowerCase();
					if (priority === "low" || priority === "medium" || priority === "high") setCookie.priority = priority;
					break;
				case "samesite":
					if (!val) break;
					const sameSite = val.toLowerCase();
					if (sameSite === "lax" || sameSite === "strict" || sameSite === "none") setCookie.sameSite = sameSite;
					break;
			}
			index = endIdx + 1;
		}
		return setCookie;
	}
	/**
	* Find the `;` character between `min` and `len` in str.
	*/
	function endIndex(str, min, len) {
		const index = str.indexOf(";", min);
		return index === -1 ? len : index;
	}
	/**
	* Find the `=` character between `min` and `max` in str.
	*/
	function eqIndex(str, min, max) {
		const index = str.indexOf("=", min);
		return index < max ? index : -1;
	}
	/**
	* Slice out a value between startPod to max.
	*/
	function valueSlice(str, min, max) {
		let start = min;
		let end = max;
		do {
			const code = str.charCodeAt(start);
			if (code !== 32 && code !== 9) break;
		} while (++start < end);
		while (end > start) {
			const code = str.charCodeAt(end - 1);
			if (code !== 32 && code !== 9) break;
			end--;
		}
		return str.slice(start, end);
	}
	/**
	* URL-decode string value. Optimized to skip native call when no %.
	*/
	function decode(str) {
		if (str.indexOf("%") === -1) return str;
		try {
			return decodeURIComponent(str);
		} catch (e) {
			return str;
		}
	}
	/**
	* Determine if value is a Date.
	*/
	function isDate(val) {
		return __toString.call(val) === "[object Date]";
	}
	return dist;
}
var distExports = /*@__PURE__*/ requireDist();
function hasDocumentCookie() {
	const testingValue = typeof globalThis === "undefined" ? void 0 : globalThis.TEST_HAS_DOCUMENT_COOKIE;
	if (typeof testingValue === "boolean") return testingValue;
	return typeof document === "object" && typeof document.cookie === "string";
}
function parseCookies(cookies) {
	if (typeof cookies === "string") return distExports.parse(cookies);
	else if (typeof cookies === "object" && cookies !== null) return cookies;
	else return {};
}
function readCookie(value, options = {}) {
	const cleanValue = cleanupCookieValue(value);
	if (!options.doNotParse) try {
		return JSON.parse(cleanValue);
	} catch (e) {}
	return value;
}
function cleanupCookieValue(value) {
	if (value && value[0] === "j" && value[1] === ":") return value.substr(2);
	return value;
}
var Cookies = class {
	constructor(cookies, defaultSetOptions = {}) {
		this.changeListeners = [];
		this.HAS_DOCUMENT_COOKIE = false;
		this.update = () => {
			if (!this.HAS_DOCUMENT_COOKIE) return;
			const previousCookies = this.cookies;
			this.cookies = distExports.parse(document.cookie);
			this._checkChanges(previousCookies);
		};
		const domCookies = typeof document === "undefined" ? "" : document.cookie;
		this.cookies = parseCookies(cookies || domCookies);
		this.defaultSetOptions = defaultSetOptions;
		this.HAS_DOCUMENT_COOKIE = hasDocumentCookie();
	}
	_emitChange(params) {
		for (let i = 0; i < this.changeListeners.length; ++i) this.changeListeners[i](params);
	}
	_checkChanges(previousCookies) {
		new Set(Object.keys(previousCookies).concat(Object.keys(this.cookies))).forEach((name) => {
			if (previousCookies[name] !== this.cookies[name]) this._emitChange({
				name,
				value: readCookie(this.cookies[name])
			});
		});
	}
	_startPolling() {
		this.pollingInterval = setInterval(this.update, 300);
	}
	_stopPolling() {
		if (this.pollingInterval) clearInterval(this.pollingInterval);
	}
	get(name, options = {}) {
		if (!options.doNotUpdate) this.update();
		return readCookie(this.cookies[name], options);
	}
	getAll(options = {}) {
		if (!options.doNotUpdate) this.update();
		const result = {};
		for (let name in this.cookies) result[name] = readCookie(this.cookies[name], options);
		return result;
	}
	set(name, value, options) {
		if (options) options = Object.assign(Object.assign({}, this.defaultSetOptions), options);
		else options = this.defaultSetOptions;
		const stringValue = typeof value === "string" ? value : JSON.stringify(value);
		this.cookies = Object.assign(Object.assign({}, this.cookies), { [name]: stringValue });
		if (this.HAS_DOCUMENT_COOKIE) document.cookie = distExports.serialize(name, stringValue, options);
		this._emitChange({
			name,
			value,
			options
		});
	}
	remove(name, options) {
		const finalOptions = options = Object.assign(Object.assign(Object.assign({}, this.defaultSetOptions), options), {
			expires: new Date(1970, 1, 1, 0, 0, 1),
			maxAge: 0
		});
		this.cookies = Object.assign({}, this.cookies);
		delete this.cookies[name];
		if (this.HAS_DOCUMENT_COOKIE) document.cookie = distExports.serialize(name, "", finalOptions);
		this._emitChange({
			name,
			value: void 0,
			options
		});
	}
	addChangeListener(callback) {
		this.changeListeners.push(callback);
		if (this.HAS_DOCUMENT_COOKIE && this.changeListeners.length === 1) if (typeof window === "object" && "cookieStore" in window) window.cookieStore.addEventListener("change", this.update);
		else this._startPolling();
	}
	removeChangeListener(callback) {
		const idx = this.changeListeners.indexOf(callback);
		if (idx >= 0) this.changeListeners.splice(idx, 1);
		if (this.HAS_DOCUMENT_COOKIE && this.changeListeners.length === 0) if (typeof window === "object" && "cookieStore" in window) window.cookieStore.removeEventListener("change", this.update);
		else this._stopPolling();
	}
	removeAllChangeListeners() {
		while (this.changeListeners.length > 0) this.removeChangeListener(this.changeListeners[0]);
	}
};
//#endregion
//#region node_modules/react-cookie/esm/index.mjs
var import_react = /* @__PURE__ */ __toESM(require_react(), 1);
var CookiesContext = import_react.createContext(null);
var { Provider, Consumer } = CookiesContext;
var CookiesProvider = (props) => {
	const cookies = import_react.useMemo(() => {
		if (props.cookies) return props.cookies;
		else return new Cookies(void 0, props.defaultSetOptions);
	}, [props.cookies, props.defaultSetOptions]);
	return import_react.createElement(Provider, { value: cookies }, props.children);
};
/******************************************************************************
Copyright (c) Microsoft Corporation.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
***************************************************************************** */
function __rest(s, e) {
	var t = {};
	for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0) t[p] = s[p];
	if (s != null && typeof Object.getOwnPropertySymbols === "function") {
		for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i])) t[p[i]] = s[p[i]];
	}
	return t;
}
function getDefaultExportFromCjs(x) {
	return x && x.__esModule && Object.prototype.hasOwnProperty.call(x, "default") ? x["default"] : x;
}
var reactIs = { exports: {} };
/** @license React v16.13.1
* react-is.production.min.js
*
* Copyright (c) Facebook, Inc. and its affiliates.
*
* This source code is licensed under the MIT license found in the
* LICENSE file in the root directory of this source tree.
*/
var reactIs_development = {};
/** @license React v16.13.1
* react-is.development.js
*
* Copyright (c) Facebook, Inc. and its affiliates.
*
* This source code is licensed under the MIT license found in the
* LICENSE file in the root directory of this source tree.
*/
var hasRequiredReactIs_development;
function requireReactIs_development() {
	if (hasRequiredReactIs_development) return reactIs_development;
	hasRequiredReactIs_development = 1;
	(function() {
		var hasSymbol = typeof Symbol === "function" && Symbol.for;
		var REACT_ELEMENT_TYPE = hasSymbol ? Symbol.for("react.element") : 60103;
		var REACT_PORTAL_TYPE = hasSymbol ? Symbol.for("react.portal") : 60106;
		var REACT_FRAGMENT_TYPE = hasSymbol ? Symbol.for("react.fragment") : 60107;
		var REACT_STRICT_MODE_TYPE = hasSymbol ? Symbol.for("react.strict_mode") : 60108;
		var REACT_PROFILER_TYPE = hasSymbol ? Symbol.for("react.profiler") : 60114;
		var REACT_PROVIDER_TYPE = hasSymbol ? Symbol.for("react.provider") : 60109;
		var REACT_CONTEXT_TYPE = hasSymbol ? Symbol.for("react.context") : 60110;
		var REACT_ASYNC_MODE_TYPE = hasSymbol ? Symbol.for("react.async_mode") : 60111;
		var REACT_CONCURRENT_MODE_TYPE = hasSymbol ? Symbol.for("react.concurrent_mode") : 60111;
		var REACT_FORWARD_REF_TYPE = hasSymbol ? Symbol.for("react.forward_ref") : 60112;
		var REACT_SUSPENSE_TYPE = hasSymbol ? Symbol.for("react.suspense") : 60113;
		var REACT_SUSPENSE_LIST_TYPE = hasSymbol ? Symbol.for("react.suspense_list") : 60120;
		var REACT_MEMO_TYPE = hasSymbol ? Symbol.for("react.memo") : 60115;
		var REACT_LAZY_TYPE = hasSymbol ? Symbol.for("react.lazy") : 60116;
		var REACT_BLOCK_TYPE = hasSymbol ? Symbol.for("react.block") : 60121;
		var REACT_FUNDAMENTAL_TYPE = hasSymbol ? Symbol.for("react.fundamental") : 60117;
		var REACT_RESPONDER_TYPE = hasSymbol ? Symbol.for("react.responder") : 60118;
		var REACT_SCOPE_TYPE = hasSymbol ? Symbol.for("react.scope") : 60119;
		function isValidElementType(type) {
			return typeof type === "string" || typeof type === "function" || type === REACT_FRAGMENT_TYPE || type === REACT_CONCURRENT_MODE_TYPE || type === REACT_PROFILER_TYPE || type === REACT_STRICT_MODE_TYPE || type === REACT_SUSPENSE_TYPE || type === REACT_SUSPENSE_LIST_TYPE || typeof type === "object" && type !== null && (type.$$typeof === REACT_LAZY_TYPE || type.$$typeof === REACT_MEMO_TYPE || type.$$typeof === REACT_PROVIDER_TYPE || type.$$typeof === REACT_CONTEXT_TYPE || type.$$typeof === REACT_FORWARD_REF_TYPE || type.$$typeof === REACT_FUNDAMENTAL_TYPE || type.$$typeof === REACT_RESPONDER_TYPE || type.$$typeof === REACT_SCOPE_TYPE || type.$$typeof === REACT_BLOCK_TYPE);
		}
		function typeOf(object) {
			if (typeof object === "object" && object !== null) {
				var $$typeof = object.$$typeof;
				switch ($$typeof) {
					case REACT_ELEMENT_TYPE:
						var type = object.type;
						switch (type) {
							case REACT_ASYNC_MODE_TYPE:
							case REACT_CONCURRENT_MODE_TYPE:
							case REACT_FRAGMENT_TYPE:
							case REACT_PROFILER_TYPE:
							case REACT_STRICT_MODE_TYPE:
							case REACT_SUSPENSE_TYPE: return type;
							default:
								var $$typeofType = type && type.$$typeof;
								switch ($$typeofType) {
									case REACT_CONTEXT_TYPE:
									case REACT_FORWARD_REF_TYPE:
									case REACT_LAZY_TYPE:
									case REACT_MEMO_TYPE:
									case REACT_PROVIDER_TYPE: return $$typeofType;
									default: return $$typeof;
								}
						}
					case REACT_PORTAL_TYPE: return $$typeof;
				}
			}
		}
		var AsyncMode = REACT_ASYNC_MODE_TYPE;
		var ConcurrentMode = REACT_CONCURRENT_MODE_TYPE;
		var ContextConsumer = REACT_CONTEXT_TYPE;
		var ContextProvider = REACT_PROVIDER_TYPE;
		var Element = REACT_ELEMENT_TYPE;
		var ForwardRef = REACT_FORWARD_REF_TYPE;
		var Fragment = REACT_FRAGMENT_TYPE;
		var Lazy = REACT_LAZY_TYPE;
		var Memo = REACT_MEMO_TYPE;
		var Portal = REACT_PORTAL_TYPE;
		var Profiler = REACT_PROFILER_TYPE;
		var StrictMode = REACT_STRICT_MODE_TYPE;
		var Suspense = REACT_SUSPENSE_TYPE;
		var hasWarnedAboutDeprecatedIsAsyncMode = false;
		function isAsyncMode(object) {
			if (!hasWarnedAboutDeprecatedIsAsyncMode) {
				hasWarnedAboutDeprecatedIsAsyncMode = true;
				console["warn"]("The ReactIs.isAsyncMode() alias has been deprecated, and will be removed in React 17+. Update your code to use ReactIs.isConcurrentMode() instead. It has the exact same API.");
			}
			return isConcurrentMode(object) || typeOf(object) === REACT_ASYNC_MODE_TYPE;
		}
		function isConcurrentMode(object) {
			return typeOf(object) === REACT_CONCURRENT_MODE_TYPE;
		}
		function isContextConsumer(object) {
			return typeOf(object) === REACT_CONTEXT_TYPE;
		}
		function isContextProvider(object) {
			return typeOf(object) === REACT_PROVIDER_TYPE;
		}
		function isElement(object) {
			return typeof object === "object" && object !== null && object.$$typeof === REACT_ELEMENT_TYPE;
		}
		function isForwardRef(object) {
			return typeOf(object) === REACT_FORWARD_REF_TYPE;
		}
		function isFragment(object) {
			return typeOf(object) === REACT_FRAGMENT_TYPE;
		}
		function isLazy(object) {
			return typeOf(object) === REACT_LAZY_TYPE;
		}
		function isMemo(object) {
			return typeOf(object) === REACT_MEMO_TYPE;
		}
		function isPortal(object) {
			return typeOf(object) === REACT_PORTAL_TYPE;
		}
		function isProfiler(object) {
			return typeOf(object) === REACT_PROFILER_TYPE;
		}
		function isStrictMode(object) {
			return typeOf(object) === REACT_STRICT_MODE_TYPE;
		}
		function isSuspense(object) {
			return typeOf(object) === REACT_SUSPENSE_TYPE;
		}
		reactIs_development.AsyncMode = AsyncMode;
		reactIs_development.ConcurrentMode = ConcurrentMode;
		reactIs_development.ContextConsumer = ContextConsumer;
		reactIs_development.ContextProvider = ContextProvider;
		reactIs_development.Element = Element;
		reactIs_development.ForwardRef = ForwardRef;
		reactIs_development.Fragment = Fragment;
		reactIs_development.Lazy = Lazy;
		reactIs_development.Memo = Memo;
		reactIs_development.Portal = Portal;
		reactIs_development.Profiler = Profiler;
		reactIs_development.StrictMode = StrictMode;
		reactIs_development.Suspense = Suspense;
		reactIs_development.isAsyncMode = isAsyncMode;
		reactIs_development.isConcurrentMode = isConcurrentMode;
		reactIs_development.isContextConsumer = isContextConsumer;
		reactIs_development.isContextProvider = isContextProvider;
		reactIs_development.isElement = isElement;
		reactIs_development.isForwardRef = isForwardRef;
		reactIs_development.isFragment = isFragment;
		reactIs_development.isLazy = isLazy;
		reactIs_development.isMemo = isMemo;
		reactIs_development.isPortal = isPortal;
		reactIs_development.isProfiler = isProfiler;
		reactIs_development.isStrictMode = isStrictMode;
		reactIs_development.isSuspense = isSuspense;
		reactIs_development.isValidElementType = isValidElementType;
		reactIs_development.typeOf = typeOf;
	})();
	return reactIs_development;
}
var hasRequiredReactIs;
function requireReactIs() {
	if (hasRequiredReactIs) return reactIs.exports;
	hasRequiredReactIs = 1;
	reactIs.exports = requireReactIs_development();
	return reactIs.exports;
}
var hoistNonReactStatics_cjs;
var hasRequiredHoistNonReactStatics_cjs;
function requireHoistNonReactStatics_cjs() {
	if (hasRequiredHoistNonReactStatics_cjs) return hoistNonReactStatics_cjs;
	hasRequiredHoistNonReactStatics_cjs = 1;
	var reactIs = requireReactIs();
	/**
	* Copyright 2015, Yahoo! Inc.
	* Copyrights licensed under the New BSD License. See the accompanying LICENSE file for terms.
	*/
	var REACT_STATICS = {
		childContextTypes: true,
		contextType: true,
		contextTypes: true,
		defaultProps: true,
		displayName: true,
		getDefaultProps: true,
		getDerivedStateFromError: true,
		getDerivedStateFromProps: true,
		mixins: true,
		propTypes: true,
		type: true
	};
	var KNOWN_STATICS = {
		name: true,
		length: true,
		prototype: true,
		caller: true,
		callee: true,
		arguments: true,
		arity: true
	};
	var FORWARD_REF_STATICS = {
		"$$typeof": true,
		render: true,
		defaultProps: true,
		displayName: true,
		propTypes: true
	};
	var MEMO_STATICS = {
		"$$typeof": true,
		compare: true,
		defaultProps: true,
		displayName: true,
		propTypes: true,
		type: true
	};
	var TYPE_STATICS = {};
	TYPE_STATICS[reactIs.ForwardRef] = FORWARD_REF_STATICS;
	TYPE_STATICS[reactIs.Memo] = MEMO_STATICS;
	function getStatics(component) {
		if (reactIs.isMemo(component)) return MEMO_STATICS;
		return TYPE_STATICS[component["$$typeof"]] || REACT_STATICS;
	}
	var defineProperty = Object.defineProperty;
	var getOwnPropertyNames = Object.getOwnPropertyNames;
	var getOwnPropertySymbols = Object.getOwnPropertySymbols;
	var getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;
	var getPrototypeOf = Object.getPrototypeOf;
	var objectPrototype = Object.prototype;
	function hoistNonReactStatics(targetComponent, sourceComponent, blacklist) {
		if (typeof sourceComponent !== "string") {
			if (objectPrototype) {
				var inheritedComponent = getPrototypeOf(sourceComponent);
				if (inheritedComponent && inheritedComponent !== objectPrototype) hoistNonReactStatics(targetComponent, inheritedComponent, blacklist);
			}
			var keys = getOwnPropertyNames(sourceComponent);
			if (getOwnPropertySymbols) keys = keys.concat(getOwnPropertySymbols(sourceComponent));
			var targetStatics = getStatics(targetComponent);
			var sourceStatics = getStatics(sourceComponent);
			for (var i = 0; i < keys.length; ++i) {
				var key = keys[i];
				if (!KNOWN_STATICS[key] && !(blacklist && blacklist[key]) && !(sourceStatics && sourceStatics[key]) && !(targetStatics && targetStatics[key])) {
					var descriptor = getOwnPropertyDescriptor(sourceComponent, key);
					try {
						defineProperty(targetComponent, key, descriptor);
					} catch (e) {}
				}
			}
		}
		return targetComponent;
	}
	hoistNonReactStatics_cjs = hoistNonReactStatics;
	return hoistNonReactStatics_cjs;
}
var hoistStatics = /*@__PURE__*/ getDefaultExportFromCjs(requireHoistNonReactStatics_cjs());
function withCookies(WrappedComponent) {
	const name = WrappedComponent.displayName || WrappedComponent.name;
	class CookieWrapper extends import_react.Component {
		constructor() {
			super(...arguments);
			this.onChange = () => {
				this.forceUpdate();
			};
		}
		listen() {
			this.props.cookies.addChangeListener(this.onChange);
		}
		unlisten(cookies) {
			(cookies || this.props.cookies).removeChangeListener(this.onChange);
		}
		componentDidMount() {
			this.listen();
		}
		componentDidUpdate(prevProps) {
			if (prevProps.cookies !== this.props.cookies) {
				this.unlisten(prevProps.cookies);
				this.listen();
			}
		}
		componentWillUnmount() {
			this.unlisten();
		}
		render() {
			const _a = this.props, { forwardedRef, cookies } = _a, restProps = __rest(_a, ["forwardedRef", "cookies"]);
			const allCookies = cookies.getAll({ doNotUpdate: true });
			return import_react.createElement(WrappedComponent, Object.assign({}, restProps, {
				ref: forwardedRef,
				cookies,
				allCookies
			}));
		}
	}
	CookieWrapper.displayName = `withCookies(${name})`;
	CookieWrapper.WrappedComponent = WrappedComponent;
	const ForwardedComponent = import_react.forwardRef((props, ref) => {
		return import_react.createElement(Consumer, null, (cookies) => import_react.createElement(CookieWrapper, Object.assign({ cookies }, props, { forwardedRef: ref })));
	});
	ForwardedComponent.displayName = CookieWrapper.displayName;
	ForwardedComponent.WrappedComponent = CookieWrapper.WrappedComponent;
	return hoistStatics(ForwardedComponent, WrappedComponent);
}
function isInBrowser() {
	return typeof window !== "undefined" && typeof window.document !== "undefined" && typeof window.document.createElement !== "undefined";
}
var useIsomorphicLayoutEffect = isInBrowser() ? import_react.useLayoutEffect : import_react.useEffect;
function useCookies(dependencies, options) {
	const cookies = (0, import_react.useContext)(CookiesContext);
	if (!cookies) throw new Error("Missing <CookiesProvider>");
	const getOptions = Object.assign(Object.assign({}, { doNotUpdate: true }), options);
	const [allCookies, setCookies] = (0, import_react.useState)(() => cookies.getAll(getOptions));
	useIsomorphicLayoutEffect(() => {
		function onChange() {
			if (!cookies) throw new Error("Missing <CookiesProvider>");
			const newCookies = cookies.getAll(getOptions);
			if (shouldUpdate(dependencies || null, newCookies, allCookies)) setCookies(newCookies);
		}
		cookies.addChangeListener(onChange);
		return () => {
			cookies.removeChangeListener(onChange);
		};
	}, [cookies, allCookies]);
	return [
		allCookies,
		(0, import_react.useMemo)(() => cookies.set.bind(cookies), [cookies]),
		(0, import_react.useMemo)(() => cookies.remove.bind(cookies), [cookies]),
		(0, import_react.useMemo)(() => cookies.update.bind(cookies), [cookies])
	];
}
function shouldUpdate(dependencies, newCookies, oldCookies) {
	if (!dependencies) return true;
	for (let dependency of dependencies) if (newCookies[dependency] !== oldCookies[dependency]) return true;
	return false;
}
//#endregion
export { Cookies, CookiesProvider, useCookies, withCookies };

//# sourceMappingURL=react-cookie.js.map