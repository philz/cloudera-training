//<!-- Yahoo! Web Analytics Code v5.09 - All rights reserved -->
/*globals window, document, screen, location, top, encodeURIComponent, navigator, Image */
/*globals YWA, YWAT */
/*global sVBSwfVer */

if (typeof YWA === "undefined") {
	YWA = {};
	YWA.initialize = function () {
		var nan;
		YWA.ud = "undefined";
		YWA.EXCLPRM = "";
		YWA.ONLOAD = true;
		YWA.PID = YWA.ud;
		YWA.flashVer = "";
		YWA.isOnloadOverwrite = false;
		YWA.windowOnload = null;
		YWA.errorId = "";
		YWA.isNewYWATEnabled = false;
		YWA.windowOnerror = null;
		if (typeof window.ITTs === "undefined") {
			window.ITTs = [];
		}
	    nan = navigator.appName;
	    YWA.net = (nan === "Netscape");
	    YWA.mic = (nan.substring(0, 9) === "Microsoft" && (typeof navigator.plugins === YWA.ud || navigator.plugins.length === 0));
	    YWA.mac = (navigator.userAgent.indexOf("Mac") >= 0);
	    YWA.gec = (navigator.userAgent.indexOf("Firefox") >= 0 || navigator.userAgent.indexOf("Netscape") >= 0);
		YWA.addOLH();
	};

    YWA.ywaOLH = function (evt) {
        if (YWA.windowOnload) {
            YWA.windowOnload.call(window, evt);
        }
        YWA.addOCHs();
    };

	YWA.addOLH = function () {
		if (!YWA.ONLOAD || YWA.isOnloadOverwrite) {
			return;
		}
		var a = [];
		if (window.screen || a.toSource || (a.shift && YWA.mic)) {
			if (window.onload) {
				if (!YWA.windowOnload) {
					YWA.windowOnload = window.onload;
				}
			}
			window.onload = YWA.ywaOLH;
		}
		YWA.isOnloadOverwrite = true;
	};

	YWA.initialize();
}

YWA.ywaOCH = function (evt) {
	var idx, rv, ittl;
	rv = true;
	for (idx = 0, ittl = window.ITTs.length; idx < ittl; idx += 1) {
		window.ITTs[idx].oco(this);
	}
	if (this.ywaOnClick) {
		rv = this.ywaOnClick(evt);
	}
	return rv;
};

YWA.addOCHs = function () {
	var i, ln;
	for (i = 0, ln = document.links.length; i < ln; i += 1) {
		if (!document.links[i].ywaOnclickOverride) {
			document.links[i].ywaOnclickOverride = true;
			if (document.links[i].onclick) {
				document.links[i].ywaOnClick = document.links[i].onclick;
			}
			document.links[i].onclick = YWA.ywaOCH;
		}
	}
};

YWA.ywaOEH = function () {
    if (YWA.errorId !== "") {
        window.ITTs[YWA.errorId].track(false, true);
    }
};

YWA.getTrackerIdx = function (pid) {
    var idx, ittl;
	for (idx = 0, ittl = window.ITTs.length; idx < ittl; idx += 1) {
		if (window.ITTs[idx].PID === pid) {
			return idx;
		}
	}
	return -1;
};

YWA.getTracker = function (pid) {
    var rst, idx;
    YWA.isNewYWATEnabled = true;
    if (!YWA.is(pid)) {
        pid = YWA.PID;
    }
    idx = YWA.getTrackerIdx(pid);
    if (idx >= 0) {
        rst = window.ITTs[idx];
    } else {
        rst = new YWAT(pid);
    }
    YWA.isNewYWATEnabled = false;
    return rst;
};

YWA.is = function (o) {
	return (typeof o !== YWA.ud);
};

YWA.getExcludePrm =  function () {
    return YWA.EXCLPRM;
};

YWA.setExcludePrm = function (e) {
    YWA.EXCLPRM = YWA.is(e) ? e : "";
};

YWA.getOnload = function () {
    return YWA.ONLOAD;
};

YWA.setOnload = function (o) {
    YWA.ONLOAD = YWA.is(o) ? o : true;
};

YWA.getPID = function () {
    if (YWA.is(document.scripts)) {
        var idx, src, jsFile, prePath, sl;
        jsFile = "ywa.js";
        prePath = "ywa-";
        for (sl = document.scripts.length, idx = sl - 1; idx >= 0 ; idx -= 1) {
            src = document.scripts[idx].src;
            if (YWA.is(src) && src.indexOf(jsFile) === src.length - jsFile.length) {
                src = src.substr(0, src.length - jsFile.length - 1);
                src = src.substr(src.lastIndexOf("/") + 1 + prePath.length);
                return "1000" + src;
            }
        }
    }
    return YWA.ud;
};
YWA.getPID();

YWA.getExcludeDomains = function () {
    return "";
};

YWA.getExcludeProtocol = function () {
	return "";
};

YWA.getDownloadExts = function () {
	return "\\.pdf$|\\.doc$|\\.dot$|\\.xls$|\\.xlt$|\\.xlw$|\\.ppt$|\\.pps$|\\.zip$|\\.rar$|\\.gz$|\\.gzip$|\\.wav$|\\.mp[3-4]?$|\\.mpeg$";
};

YWA.getErrorObj = function (msg) {
	var err;
	err = new Error(msg);
    if (!err.msg) {
        err.msg = msg;
    }
    return err;
};

YWA.gcpn = function (x) {
    var k, l, z, i, j; 
    z = location.search;
    i = z.indexOf("?" + x + "=");
    j = z.indexOf("&" + x + "=");
    if ((i === 0) || (j > -1)) {
        k = (i === 0) ? 0 : j;
        l = z.indexOf("&", k + 1);
        return z.substring(k + 2 + x.length, (l > -1) ? l : z.length);
    }
    return "";
};

YWA.getFileName = function (x) {
	var i;
    i = x.indexOf("?");
    if (i > 0) {
        x = x.substring(0, i);
    }
    return x.substring(x.lastIndexOf("/") + 1, x.length);
};

YWA.gh = function (x) {
	var i;
    i = x.host.indexOf(":");
    return (i >= 0) ? x.host.substring(0, i) : x.host;
};

YWA.ghs = function (x) {
	var i;
    i = x.indexOf("//");
    if (i >= 0) {
        x = x.substring(i + 2, x.length);
        i = x.indexOf("/");
        if (i >= 0) {
            return x.substring(0, i);
        }
        return x.substring(i + 2, x.length);
    }
    return "";
};

YWA.gpr = function (x) {
	var y, i;
    y = x.protocol;
    i = y.indexOf(":");
    return (i >= 0) ? y : y + ":";
};

YWA.gp = function (x) {
	var y, i;
    y = x.pathname;
    i = y.indexOf("/");
    return (i === 0) ? y : "/" + y;
};

YWA.mxDmnRGXP = function (v) {
    if (v.toUpperCase().indexOf("REGEXP:") === 0) {
        return new RegExp(v.substring(7), "i");
    } else {
        return new RegExp(YWA.mxRgXpStr(v), "i");
    }
};

YWA.mxRgXpStr = function (e) {
    while (e.indexOf(" ") >= 0) {
        e = e.replace(" ", "");
    }
    var i, j, b, bl, r, a, al; 
    r = "";
    a = e.split(",");
    for (i = 0, al = a.length; i < al; i += 1) {
        b = a[i].split(".");
        for (j = 0, bl = b.length; j < bl; j += 1) {
            if (b[j].indexOf("*") >= 0) {
                b[j] = ".+";
            }
        }
        if (bl > 0) {
            a[i] = b.join("\\.");
        }
    }
    if (al > 0) {
        r += a.join("$|^");
    }
    if (r.length > 0) {
        return "^" + r + "$";
    }
    return "";
};

function YWAT(pid) {
    if (!YWA.isNewYWATEnabled) {
        throw YWA.getErrorObj("Invalid method to get a tracking object.");
    }
    var i, heads;
    this.version = "5.09";
    this.EXCLDOMAINS = "";
    this.DWNLEXTS = "";
    this.EXCLPRM = YWA.getExcludePrm();
    this.EXCLPRTCL = "";
    this.ONLOAD = YWA.getOnload();
    this.DOMAINS = "";
    this.DEBUG = false;
    this.RUN = false;
    if (!YWA.is(pid)) {
        this.PID = YWA.getPID();
        this.RUN = this.PID === YWA.ud;
    } else {
        this.PID = pid;
    }
    this.BD = (window.location.protocol.indexOf("https:") === 0 ? "https://" : "http://")  + "a.analytics.yahoo.com";
    this.BU = this.BD + "/p.pl?a=" + this.PID + "&v=" + this.version;
    this.FU = "";
    this.URL = this.getClnUrl(document.URL ? document.URL : document.location);
    this.REFERRER = "";
    this.TOPLOCATION = "";
    this.cfn = [];
    this.cfv = [];
    this.IT = "";
    this.date = new Date();
    this.PIXELDELAY = false;
    this.DOCUMENTNAME = document.title;
    this.CAMPAIGN = "";
    this.CMPPARM = "";
    this.PROMO = "";
    this.PROMOPARM = "";
    this.TPSC = true;
    this.EXCL = "";
    this.FPCR = "";
    this.FPCN = "fpc" + this.PID;
    this.FPCV = "";
    this.FPCD = "";
    this.ENC = "";
    this.itvs = "";
    this.itsid = "";
    this.itvid = "";
    this.place = document.body;
    try {
        heads = document.getElementsByTagName("head");
        if (YWA.is(heads) && heads.length > 0) {
            this.place = heads[0];
        }
    } catch (e) {
    }
    this.FLV = this.flash();
    if (!YWA.is(window.ITTs)) {
        window.ITTs = [];
    }
    this.idx = window.ITTs.length;
    window.ITTs[this.idx] = this;
    this.ita = ["DOCUMENTNAME", "b", "DOCUMENTGROUP", "c", "MEMBERID", "m", "URL", "f", "ACTION", "x", "AMOUNT", "xa", "ORDERID", "oc", "TAX", "xt", "SHIPPING", "xs", "DISCOUNT", "xd", "SKU", "p", "PRODUCTS", "u", "UNITS", "q", "AMOUNTS", "r", "CMPQUERY", "cq", "ISK", "isk", "ISR", "isr"];
    this.prmord = ["a", "v", "b", "c", "m", "f", "e", "t", "n", "d", "cp", "cq", "scp", "ci", "enc", "x", "sid", "ca", "oc", "p", "q", "r", "xa", "xd", "xs", "xt", "el", "fn", "flv", "fpc", "isk", "isr", "g", "h", "ittidx", "ix", "j", "k", "l", "tp", "nr", "js", "cf01", "cf02", "cf03", "cf04", "cf05", "cf06", "cf07", "cf08", "cf09", "cf10", "cf11", "cf12", "cf13", "cf14", "cf15", "cf16", "cf17", "cf18", "cf19", "cf20", "cf21", "cf22", "cf23", "cf24", "cf25", "cf26", "cf27", "cf28", "cf29", "cf30", "cf31", "cf32", "cf33", "cf34", "cf35", "cf36", "cf37", "cf38", "cf39", "cf40", "cf41", "cf42", "cf43", "cf44", "cf45", "cf46", "cf47", "cf48", "cf49", "cf50", "cf51", "cf52", "cf53", "cf54", "cf55", "cf56", "cf57", "cf58", "cf59", "cf60", "cf61", "cf62", "cf63", "cf64", "cf65", "cf66", "cf67", "cf68", "cf69", "cf70", "cf71", "cf72", "cf73", "cf74", "cf75", "cf76", "cf77", "cf78", "cf79", "cf80", "cf81", "cf82", "cf83", "cf84", "cf85", "cf86", "cf87", "cf88", "cf89", "cf90", "cf91", "cf92", "cf93", "cf94", "cf95", "cf96", "cf97", "cf98", "cf99", "cf100"];
    for (i = 0; i < 10; i += 1) {
        this.ita[this.ita.length] = "P" + (1 + i);
        this.ita[this.ita.length] = "p" + (1 + i);
    }
    for (i = 0; i < 99; i += 1) {
        this.ita[this.ita.length] = "CF" + ((i < 9) ? "0" : "") + (1 + i);
        this.ita[this.ita.length] = "cf" + (1 + i);
    }
    this.imgsl = 0;
    if (YWA.is(document.charset)) {
        this.ENC = document.charset;
    } else {
        if (YWA.is(document.characterSet)) {
            this.ENC = document.characterSet;
        } else {
            this.ENC = "";
        }
    }
    this.FPCR = "&ittidx=" + this.idx + "&fpc=" + encodeURIComponent(this.getCookie(this.FPCN));
    this.hasFPC = false;
}

YWAT.prototype.getDocumentName = function () {
    return this.DOCUMENTNAME;
};

YWAT.prototype.setDocumentName = function (dn) {
    this.DOCUMENTNAME = dn;
};

YWAT.prototype.getDocumentGroup = function () {
    return this.DOCUMENTGROUP;
};

YWAT.prototype.setDocumentGroup = function (dg) {
    this.DOCUMENTGROUP = dg;
};

YWAT.prototype.getMemberId = function () {
    return this.MEMBERID;
};

YWAT.prototype.setMemberId = function (m) {
    this.MEMBERID = m;
};

YWAT.prototype.getAction = function () {
    return this.ACTION;
};

YWAT.prototype.setAction = function (a) {
    this.ACTION = a;
};

YWAT.prototype.getAmount = function () {
    return this.AMOUNT;
};

YWAT.prototype.setAmount = function (a) {
    this.AMOUNT = a;
};

YWAT.prototype.getOrderId = function () {
    return this.ORDERID;
};

YWAT.prototype.setOrderId = function (oi) {
    this.ORDERID = oi;
};

YWAT.prototype.getTax = function () {
    return this.TAX;
};

YWAT.prototype.setTax = function (t) {
    this.TAX = t;
};

YWAT.prototype.getShipping = function () {
    return this.SHIPPING;
};

YWAT.prototype.setShipping = function (s) {
    this.SHIPPING = s;
};

YWAT.prototype.getDiscount = function () {
    return this.DISCOUNT;
};

YWAT.prototype.setDiscount = function (d) {
    this.DISCOUNT = d;
};

YWAT.prototype.getSKU = function () {
    return this.SKU;
};

YWAT.prototype.setSKU = function (s) {
    this.SKU = s;
};

YWAT.prototype.getUnits = function () {
    return this.UNITS;
};

YWAT.prototype.setUnits = function (u) {
    this.UNITS = u;
};

YWAT.prototype.getAmounts = function () {
    return this.AMOUNTS;
};

YWAT.prototype.setAmounts = function (a) {
    this.AMOUNTS = a;
};

YWAT.prototype.getCmpQuery = function () {
    return this.CMPQUERY;
};

YWAT.prototype.setCmpQuery = function (c) {
    this.CMPQUERY = c;
};

YWAT.prototype.getISK = function () {
    return this.ISK;
};

YWAT.prototype.setISK = function (i) {
    this.ISK = i;
};

YWAT.prototype.getISR = function () {
    return this.ISR;
};

YWAT.prototype.setISR = function (i) {
    this.ISR = i;
};

YWAT.prototype.getEF = function (n) {
    n = parseInt(n, 10);
    return this["P" + n];
};

YWAT.prototype.setEF = function (n, v) {
    n = parseInt(n, 10);
    this["P" + n] = v;
};

YWAT.prototype.getCF = function (n) {
    n = parseInt(n, 10);
    return this["CF" + ((n < 10) ? "0" : "") + n];
};

YWAT.prototype.setCF = function (n, v) {
    n = parseInt(n, 10);
    this["CF" + ((n < 10) ? "0" : "") + n] = v;
};

YWAT.prototype.getDebug = function () {
    return this.DEBUG;
};

YWAT.prototype.setDebug = function (d) {
    this.DEBUG = YWA.is(d) ? d : false;
};

YWAT.prototype.getRun = function () {
    return this.RUN;
};

YWAT.prototype.setRun = function (r) {
    if (!YWA.is(r) || r) {
        this.RUN = this.PID !== YWA.ud;
    } else {
        this.RUN = false;
    }
};

YWAT.prototype.getUrl = function () {
    return this.URL;
};

YWAT.prototype.setUrl = function (u) {
    this.URL = this.getClnUrl(u);
};

YWAT.prototype.getEncoding = function () {
    return this.ENC;
};

YWAT.prototype.setEncoding = function (e) {
    if (YWA.is(e)) {
        this.ENC = e;
    } else {
        this.ENC = "";
    }
};

YWAT.prototype.getCookieDomain = function () {
    return this.FPCD;
};

YWAT.prototype.setCookieDomain = function (d) {
    if (YWA.is(d) && d !== "") {
        this.FPCD = d;
    } else {
        this.FPCD = "";
    }
};

YWAT.prototype.getTPSC = function () {
    return this.TPSC;
};

YWAT.prototype.setTPSC = function (n) {
    this.TPSC = YWA.is(n) ? n : true;
};

YWAT.prototype.getReferrer = function () {
    return this.REFERRER;
};

YWAT.prototype.setReferrer = function (r) {
    if (YWA.is(r) && r.length > 0) {
        this.REFERRER = r;
    }
};

YWAT.prototype.getPixelDelay = function () {
    return this.PIXELDELAY;
};

YWAT.prototype.setPixelDelay = function (d) {
    this.PIXELDELAY = YWA.is(d) ? d : false;
};

YWAT.prototype.getDomains = function () {
    return this.DOMAINS;
};

YWAT.prototype.setDomains = function (d) {
    this.DOMAINS = (YWA.is(d) && d !== "") ? d : YWA.ud;
};

YWAT.prototype.getFlashUrl = function () {
    return this.FU;
};

YWAT.prototype.setFlashUrl = function (u) {
    this.FU = YWA.is(u) ? u : "";
};

YWAT.prototype.getExcludeDomains = function () {
	var d;
    d = YWA.getExcludeDomains();
    d += (d.length > 0 && this.EXCLDOMAINS.length > 0 ? "," : "") + this.EXCLDOMAINS;
    return d;
};

YWAT.prototype.addExcludeDomain = function (d) {
    this.EXCLDOMAINS += (this.EXCLDOMAINS.length > 0 ? "," : "") + d;
};

YWAT.prototype.removeExcluseDomain = function (d) {
    var i, ds1, ds2, ds1l;
    ds1 = this.EXCLDOMAINS.split(",");
    ds2 = [];
    for (i = 0, ds1l = ds1.length; i < ds1l; i += 1) {
        if (ds1[i] !== d) {
            ds2[ds2.length] = ds1[i];
        }
    }
    this.EXCLDOMAINS = ds2.join(",");
};

YWAT.prototype.getDownloadExts = function () {
	var e;
    e = YWA.getDownloadExts();
    e += (e.length > 0 && this.DWNLEXTS.length > 0 ? "|" : "") + this.DWNLEXTS;
    return e;
};

YWAT.prototype.addDownloadExt = function (e) {
	var i, c, e1, e2, e1l;
	e1 = e.toLowerCase();
	e2 = "";
	for (i = 0, e1l = e1.length; i < e1l; i += 1) {
		c = e1.charAt(i);
		if ((c >= "a" && c <= "z") || (c >= "0" && c <= "9") || c === "-" || c === "_") {
			e2 += c;
		}
	}
    this.DWNLEXTS += (this.DWNLEXTS.length > 0 ? "|" : "") + "\\." + e2 + "$";
};

YWAT.prototype.removeDownloadExt = function (e) {
    var i, es1, es2, es1l;
    es1 = this.DWNLEXTS.split("|");
    es2 = [];
    for (i = 0, es1l = es1.length; i < es1l; i += 1) {
        if (es1[i] !== "\\." + e.toLowerCase() + "$") {
            es2[es2.length] = es1[i];
        }
    }
    this.DWNLEXTS = es2.join("|");
};

YWAT.prototype.getExcludeProtocol = function () {
	var p;
    p = YWA.getExcludeProtocol();
    p += (p.length > 0 && this.EXCLPRTCL.length > 0 ? "," : "") + this.EXCLPRTCL;
    return p;
};

YWAT.prototype.addExcludeProtocol = function (e) {
	this.EXCLPRTCL += (this.EXCLPRTCL.length > 0 ? "," : "") + e.toLowerCase();
};

YWAT.prototype.removeExcludeProtocol = function (e) {
    var i, es1, es2, es1l;
    es1 = this.EXCLPRTCL.split(",");
    es2 = [];
    for (i = 0, es1l = es1.length; i < es1l; i += 1) {
        if (es1[i] !== e.toLowerCase()) {
            es2[es2.length] = es1[i];
        }
    }
    this.EXCLPRTCL = es2.join(",");
};

YWAT.prototype.isProtocolExcluded = function () {
	var ep;
	ep = "," + this.getExcludeProtocol() + ",";
	return ep.indexOf("," + window.location.protocol + ",") >= 0;
};

YWAT.prototype.pp = function () {
    var i, ital, its;
    its = [];
    for (i = 0, ital = this.ita.length; i + 1 < ital; i += 2) {
        if ((YWA.is(this[this.ita[i]])) && (this[this.ita[i]] !== "")) {
            its[i] = "&" + this.ita[i + 1] + "=" + encodeURIComponent(this[this.ita[i]]);
        }
    }
    this.IT += its.join("");
};

YWAT.prototype.reset = function () {
    var i, ital;
    for (i = 8, ital = this.ita.length; i + 1 < ital; i += 2) {
        if ((YWA.is(this[this.ita[i]])) && (this[this.ita[i]] !== "")) {
            this[this.ita[i]] = "";
        }
    }
};

YWAT.prototype.flash = function () {
    if (YWA.flashVer === "") {
        var swVer2, vb, fd, np;
        fd = "";
        np = navigator.plugins;
        if (np !== null && np.length > 0) {
            if (np["Shockwave Flash 2.0"] || np["Shockwave Flash"]) {
                swVer2 = np["Shockwave Flash 2.0"] ? " 2.0" : "";
                fd = np["Shockwave Flash" + swVer2].description;
            }
        } else {
            vb = document.createElement("script");
            vb.language = "VBScript";
            vb.text = '\nFunction sVBSwfVer(i)\non error resume next\nDim swC,swV\nswV=0\nset swC=CreateObject("ShockwaveFlash.ShockwaveFlash."+CStr(i))\nif(IsObject(swC))then\nswV=swC.GetVariable("$version")\nend if\nsVBSwfVer=swV\nEnd Function\n';
            this.place.appendChild(vb);
            fd = sVBSwfVer(1);
        }
        YWA.flashVer = fd;
    }
    return YWA.flashVer;
};

YWAT.prototype.setCookie = function (name, value, off) {
    var expiry, cookie, d;
    d = new Date();
    d.setTime(d.getTime() + (off * 1000));
    expiry = (off > 0) ? "; expires=" + d.toGMTString() : "";
    if (off < 0) {
        expiry = "; expires=Thu, 01-Jan-1970 00:00:01 GMT";
    }
    cookie = name + "=" + value + expiry + "; path=/" + ((this.FPCD !== "") ? ("; domain=" + this.FPCD) : (""));
    document.cookie = cookie;
};

YWAT.prototype.deleteCookie = function (name) {
    return this.setCookie(name, "1", -1);
};

YWAT.prototype.getCookie = function (name) {
    var start, end, dc, pos;
    dc = document.cookie;
    pos = dc.indexOf(name + "=");
    if (pos !== -1) {
        start = pos + name.length + 1;
        end = dc.indexOf(";", start);
        if (end === -1) {
            end = dc.length;
        }
        return dc.substring(start, end);
    }
    return "";
};

YWAT.prototype.FPCSupport = function () {
    if (this.getCookie(this.FPCN) !== "") {
        return true;
    }
    var dr, dn, d, dt;
    dn = "itfpctmp";
    d = new Date();
    dt = "fpc-" + d.getTime();
    this.setCookie(dn, dt, 180);
    dr = this.getCookie(dn);
    if (dr === dt) {
        this.deleteCookie(dn);
        return true;
    }
    return false;
};

YWAT.prototype.trunc = function (x, z) {
    var url, qry, prms, x2, idx1, idx2, isAmp, prmordl, prmsl;
    if (x.length <= z) {
        return x;
    }
    url = x.split("?");
    if (url.length > 1) {
        x2 = url[0] + "?";
        qry = url[1];
        prms = qry.split("&");
        prms.sort();
        isAmp = false;
        prmsl = prms.length;
        for (idx1 = 0, prmordl = this.prmord.length; idx1 < prmordl; idx1 += 1) {
            for (idx2 = 0; idx2 < prmsl; idx2 += 1) {
                if (prms[idx2].indexOf(this.prmord[idx1] + "=") === 0) {
		    		if (x2.length + "&".length + prms[idx2].length > z) {
						return x2 + "&trnc=1";
		    		}
                    x2 += (isAmp ? "&" : "") + prms[idx2];
                    isAmp = true;
                    break;
                }
            }
        }
        return x2;
    } else {
        return x;
    }
};

YWAT.prototype.chkl0 = function (x, y, z, Z, r) {
    var i, d, l1, l2, k, bbf, iq, ik, x2, xl, yl;
    for (i = 0, yl = y.length, xl = x.length; i < yl && xl > z; i += 1) {
        d = x.length - z;
        l1 = x.indexOf("&" + y[i] + "=");
        if (l1 > 0) {
            l1 += y[i].length + 2;
            l2 = x.indexOf("&", l1);
            if (l2 > 0) {
                bbf = l1;
                iq = x.toLowerCase().indexOf("%3f", l1);
                ik = x.toLowerCase().indexOf("%3d", l1);
                if (l2 - l1 > d + r.length + Z) {
                    l1 += l2 - l1 - d - r.length;
                    for (k = 1; k < 10; k += 1) {
                        if (x.charAt(l1 - k) === "%") {
                            l1 -= k;
                            break;
                        }
                    }
                } else {
                    if (l2 - l1 > Z) {
                        l1 += Z;
                        for (k = 1; k < 10; k += 1) {
                            if (x.charAt(l1 - k) === "%") {
                                l1 -= k;
                                break;
                            }
                        }
                    } else {
                        continue;
                    }
                }
                x2 = x.substring(0, l1);
                if (iq > 0 && iq < l2) {
                    if (ik < 0 || ik > l2) {
                        x2 += "%3D";
                    }
                    x2 += "%26";
                }
                x2 += r;
                x2 += x.substring(l2);
                x = x2;
            }
        }
    }
    if (x.length > z) {
        return this.chkl(x, y, z, Z / 2, r);
    }
    return x;
};

YWAT.prototype.chkl = function (x, y, z, Z, r) {
    x = this.chkl0(x, y, z, Z, r);
    if (x.length > z) {
        x = this.chkl0(x, y, z, Z / 2, r);
    }
    return x;
};

YWAT.prototype.trk = function (s) {
    var s2, i;
    s2 = this.trunc(this.BU + (!this.TPSC ? "&tp=0" : "") + "&enc=" + encodeURIComponent(this.ENC) + this.IT + s + "&ix=" + this.imgsl + this.FPCR, 2000);
    if (!this.isProtocolExcluded()) {
    	this.imgsl += 1;
	    if (this.DEBUG) {
	        window.alert(s2);
	    } else {
	    	i = new Image();
        	if (YWA.net || this.PIXELDELAY) {
	            window.setTimeout(function () {
	            	i.src = s2;
	            }, 1);
        	} else {
	            i.src = s2;
        	}
	    	window.setTimeout(function () {
    			i = null;
    		}, 1e4);
    	}
    } else {
	    if (this.DEBUG) {
	        window.alert("'" + s2 + "' isn't tracked because of excluded protocol.");
	    }
    }
	this.reset();
	this.IT = "";
	return s2;
};

YWAT.prototype.gcpn = function (x) {
	return YWA.gcpn(x);
};

YWAT.prototype.getFileName = function (x) {
	return YWA.getFileName(x);
};

YWAT.prototype.gh = function (x) {
	return YWA.gh(x);
};

YWAT.prototype.ghs = function (x) {
	return YWA.ghs(x);
};

YWAT.prototype.gpr = function (x) {
	return YWA.gpr(x);
};

YWAT.prototype.gp = function (x) {
	return YWA.gp(x);
};

YWAT.prototype.mxDmnRGXP = function (v) {
	return YWA.mxDmnRGXP(v);
};

YWAT.prototype.mxRgXpStr = function (e) {
	return YWA.mxRgXpStr(e);
};

YWAT.prototype.customfield_reset = function () {
    this.cfn.length = 0;
    this.cfv.length = 0;
};

YWAT.prototype.customfield_set = function (n, v) {
    this.cfn[this.cfn.length] = n;
    this.cfv[this.cfv.length] = v;
};

YWAT.prototype.cf_ts = function () {
    var i, u, cfnl, cfvl;
    u = "&cf=1";
    for (i = 0, cfnl = this.cfn.length, cfvl = this.cfv.length; i < cfnl  && i < cfvl; i += 1) {
        u += "&cn" + i + "=" + encodeURIComponent(this.cfn[i]) + "&cv" + i + "=" + encodeURIComponent(this.cfv[i]);
    }
    return u;
};

YWAT.prototype.submit_customfield = function () {
    if (this.cfn.length > 0 && this.cfv.length > 0) {
        var u;
        u = this.cf_ts();
        this.customfield_reset();
        this.pp();
        this.trk(u);
    }
};

YWAT.prototype.submit_action = function () {
    this.pp();
    this.trk("&ca=1");
};

YWAT.prototype.submit_icmp = function () {
    this.pp();
    this.trk("&ci=1");
};

YWAT.prototype.exitlink = function (ln) {
    this.pp();
    this.trk("&el=" + encodeURIComponent(ln));
};

YWAT.prototype.el = function (x) {
	var pt;
    if (YWA.gh(location) === YWA.gh(x)) {
        return true;
    }
    if (!this.isProtocolExcluded()) {
    	pt = (YWA.is(this.DOMAINS) && this.DOMAINS !== "") ? YWA.mxDmnRGXP(this.DOMAINS) : YWA.mxDmnRGXP(YWA.gh(location));
    } else {
    	pt = YWA.mxDmnRGXP("protocolexclusion");
    }
    if (pt.test(YWA.gh(x))) {
        return true;
    }
    if (x.href.indexOf("java") !== 0) {
        this.exitlink(x.href);
    }
    return true;
};

YWAT.prototype.download = function (fn) {
    this.pp();
    this.trk("&fn=" + encodeURIComponent(fn));
};

YWAT.prototype.oco = function (x) {
    var pt, fn, excl;
    excl = this.getExcludeDomains();
    if (excl !== "") {
        pt = YWA.mxDmnRGXP(excl);
        if (pt.test(YWA.gh(x))) {
            if (this.DEBUG) {
                window.alert("'" + YWA.gh(x) + "' isn't tracked as exitlink.");
            }
            return true;
        }
    }
    if (YWA.is(x.pathname)) {
        fn = YWA.getFileName(x.pathname);
        if (fn !== "") {
            pt = new RegExp(this.getDownloadExts(), "i");
            if (pt.test(fn) && (fn.indexOf(".") !== -1)) {
                if (((this.EXCL !== "") && (!YWA.mxDmnRGXP(this.EXCL).test(x.pathname))) || (this.EXCL.length === 0)) {
                    this.download(x.href);
                }
            } else {
                this.el(x);
            }
        } else {
            this.el(x);
        }
    }
};

YWAT.prototype.track = function (d, i) {
    var cs, pt, hasTopAccess, t, r, its;
    t = "";
    r = document.referrer;
    YWA.errorId = this.idx;
    its = [];
    if (YWA.is(this.REFERRER) && this.REFERRER.length > 0) {
        r = this.REFERRER;
    } else {
        if ((navigator.userAgent.indexOf("Mac") >= 0) && (navigator.userAgent.indexOf("MSIE 4") >= 0)) {
            r = document.referrer;
        } else {
            if (d) {
                YWA.windowOnerror = window.onerror;
                window.onerror = YWA.ywaOEH;
                hasTopAccess = true;
                try {
                	t = top.location.href;
                	t = "";
                }
                catch (e1) {
                	hasTopAccess = false;
                }
                if (hasTopAccess && document.location !== top.location) {
                    r = top.document.referrer;
                    t = top.location.href;
                }
            } else {
            	its[0] = "&nr=t";
            }
        }
    }
    if (YWA.windowOnerror) {
        window.onerror = YWA.windowOnerror;
    } else {
        window.onerror = null;
    }
    this.pp();
    if (r.length > 0) {
        pt = YWA.is(this.DOMAINS) ? YWA.mxDmnRGXP(this.DOMAINS) : YWA.mxDmnRGXP(YWA.gh(location));
        its[1] = "&e=" + encodeURIComponent(pt.test(YWA.ghs(r)) ? this.getClnUrl(r) : r);
    }
    if (t.length > 0) {
        its[2] = "&t=" + encodeURIComponent(t);
    }
    cs = this.FPCSupport();
    this.date = new Date();
    its[3] = "&flv=" + encodeURIComponent(this.FLV);
    its[4] = "&d=" + encodeURIComponent(this.date.toGMTString());
    its[5] = "&n=" + encodeURIComponent(parseInt(this.date.getTimezoneOffset() / 60, 10));
    its[6] = "&g=" + encodeURIComponent(YWA.net ? navigator.language : navigator.userLanguage);
    its[7] = "&h=" + encodeURIComponent((navigator.javaEnabled() ? "Y" : "N"));
    try {
        its[8] = "&j=" + encodeURIComponent(screen.width + "x" + screen.height);
        its[9] = "&k=" + encodeURIComponent(YWA.mic ? screen.colorDepth : screen.pixelDepth);
    } catch (e) {
    }
    its[10] = "&l=" + ((cs) ? "true" : "false");
    if (this.CAMPAIGN !== "") {
        its[11] = "&cp=" + encodeURIComponent(this.CAMPAIGN);
    }
    if (this.CMPPARM !== "") {
        its[12] = "&cp=" + encodeURIComponent(YWA.gcpn(this.CMPPARM));
    }
    if (this.PROMO !== "") {
        its[13] = "&scp=" + encodeURIComponent(this.PROMO);
    }
    if (this.PROMOPARM !== "") {
        its[14] = "&scp=" + encodeURIComponent(YWA.gcpn(this.PROMOPARM));
    }
    this.IT += its.join("");
    if (this.RUN) {
        return;
    }
    if (cs && i) {
        this.fpc();
    } else {
        this.trk("");
    }
    YWA.errorId = "";
};

YWAT.prototype.submitPT = function () {
    return this.submit(false);
};

YWAT.prototype.submit = function (fpc) {
    var t;
    t = (typeof fpc).toLowerCase();
    if (t === "undefined" || t !== "boolean") {
		fpc = true;
    }
    fpc = (this.hasFPC ? false : fpc);
    return this.track(true, fpc);
};

YWAT.prototype.fpc = function () {
    this.getFPCvars();
};

YWAT.prototype.ywaW3C = function (mode) {
    var idScr, m_url, urls;
    urls = [];
    urls[0] = this.BD;
    urls[1] = "/fpc.pl?a=";
    urls[2] = this.PID;
    urls[3] = "&v=";
    urls[4] = this.version;
	if (!this.TPSC) {
		urls[5] = "&tp=0";
	}
	urls[6] = "&enc=";
	urls[7] = encodeURIComponent(this.ENC);
	urls[8] = this.IT;
	urls[9] = this.FPCR;
	m_url = this.trunc(urls.join(""), 2000);
	if (!this.isProtocolExcluded()) {
		if (mode === 0) {
			if (!this.DEBUG) {
				idScr = document.createElement("SCRIPT");
				idScr.defer = true;
				idScr.type = "text/javascript";
				idScr.src = m_url;
				this.place.appendChild(idScr);
				this.hasFPC = true;
			} else {
				window.alert(m_url);
			}
		} else {
			this.trk("");
		}
	} else {
	    if (this.DEBUG) {
	    	window.alert("'" + m_url + "' isn't tracked because of excluded protocol.");
	    }
	}
	this.reset();
    this.IT = "";
};

YWAT.prototype.getFPCvars = function () {
    var cs = this.FPCSupport();
    if (cs && !this.hasFPC) {
		this.ywaW3C(0);
    } else {
    	this.ywaW3C(1);
    }
};

YWAT.prototype.setFPCookies = function () {
    if (this.FPCV !== "") {
        this.setCookie(this.FPCN, this.FPCV, 31536000);
    }
};

YWAT.prototype.page = function (docName, docGroup, memberid, action, amount) {
	var u;
    u = this.URL;
    this.URL = this.FU !== "" ? this.FU : "FLASH";
    this.DOCUMENTNAME = docName;
    this.DOCUMENTGROUP = docGroup;
    this.MEMBERID = memberid;
    this.ACTION = action;
    this.AMOUNT = amount;
    this.submit();
    this.URL = u;
};

YWAT.prototype.getClnUrl = function (u) {
    var u2, p, exc, exc2, excl, i, r, k, pn, re, nonexc, pl, accept;
    nonexc = "_S_PEPOS,_S_PEPRM";
    if (!YWA.is(this.EXCLPRM)) {
        return u;
    }
    u2 = u.split("?");
    if (u2.length === 1) {
        return u;
    }
    p = u2[1].split("&");
    exc = ((this.EXCLPRM.indexOf(";") >= 0) ? this.EXCLPRM.split(";") : this.EXCLPRM.split(","));
    exc2 = [];
    i = 0;
    for (k = 0, excl = exc.length; k < excl; k += 1) {
        if (k < excl - 1 || k === excl - 1 && exc[k] !== "") {
	        exc2[i] = exc[k];
	        i += 1;
	    }
    }
    excl = i;
    r = "";
    for (k = 0, pl = p.length; k < pl; k += 1) {
        pn = p[k].split("=")[0];
        accept = true;
        re = new RegExp("\\b" + pn + "\\b", "gi");
        for (i = 0; i < excl; i += 1) {
            accept = accept && (pn.length > 0 && !re.test(exc2[i]) || pn.length === 0 && exc2[i] !== "");
        }
        if (accept) {
            r += ((r.length > 0) ? "&" : "") + p[k];
        }
    }
    return u2[0] + ((r.length > 0) ? "?" + r: "");
};
//<!-- End of Yahoo! Web Analytics Code -->
