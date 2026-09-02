import {
  $t as e,
  A as t,
  At as n,
  B as r,
  Bt as i,
  C as a,
  D as o,
  Dt as s,
  E as c,
  Et as l,
  F as u,
  Ft as d,
  G as f,
  H as p,
  Ht as m,
  I as h,
  It as g,
  J as _,
  Jt as v,
  K as y,
  Lt as b,
  M as x,
  Mt as S,
  N as C,
  Nt as w,
  O as T,
  Ot as E,
  P as D,
  Pt as O,
  Q as k,
  R as A,
  Rt as j,
  S as ee,
  St as te,
  T as M,
  Tn as ne,
  Tt as re,
  U as N,
  W as ie,
  Wt as ae,
  Xt as P,
  Yt as oe,
  Z as se,
  Zt as ce,
  _ as le,
  _n as ue,
  _t as F,
  a as de,
  b as I,
  bn as fe,
  c as pe,
  ct as me,
  d as he,
  dn as ge,
  dt as _e,
  et as ve,
  f as ye,
  fn as be,
  ft as L,
  g as xe,
  gn as Se,
  gt as Ce,
  h as we,
  hn as Te,
  ht as Ee,
  i as De,
  it as Oe,
  j as R,
  jt as ke,
  k as Ae,
  kt as z,
  l as je,
  ln as Me,
  lt as Ne,
  m as Pe,
  mn as Fe,
  mt as Ie,
  n as B,
  nn as Le,
  nt as V,
  o as Re,
  on as ze,
  p as Be,
  pn as Ve,
  pt as He,
  q as Ue,
  qt as We,
  r as H,
  rn as U,
  rt as Ge,
  sn as Ke,
  tt as qe,
  u as Je,
  un as Ye,
  ut as Xe,
  v as Ze,
  vn as Qe,
  w as $e,
  wt as et,
  x as tt,
  xn as nt,
  y as rt,
  yn as it
} from "./_plugin-vue_export-helper-CQMKH6SZ.js";
var at = Object.create,
  ot = Object.defineProperty,
  st = Object.getOwnPropertyDescriptor,
  ct = Object.getOwnPropertyNames,
  lt = Object.getPrototypeOf,
  ut = Object.prototype.hasOwnProperty,
  dt = (e, t) => () => (t || (e((t = {
    exports: {}
  }).exports, t), e = null), t.exports),
  ft = (e, t) => {
    let n = {};
    for (var r in e) ot(n, r, {
      get: e[r],
      enumerable: !0
    });
    return t || ot(n, Symbol.toStringTag, {
      value: `Module`
    }), n
  },
  pt = (e, t, n, r) => {
    if (t && typeof t == `object` || typeof t == `function`)
      for (var i = ct(t), a = 0, o = i.length, s; a < o; a++) s = i[a], !ut.call(e, s) && s !== n && ot(e, s, {
        get: (e => t[e]).bind(null, s),
        enumerable: !(r = st(t, s)) || r.enumerable
      });
    return e
  },
  mt = (e, t, n) => (n = e == null ? {} : at(lt(e)), pt(t || !e || !e.__esModule ? ot(n, `default`, {
    value: e,
    enumerable: !0
  }) : n, e)),
  ht = void 0,
  gt = typeof window < `u` && window.trustedTypes;
if (gt) try {
  ht = gt.createPolicy(`vue`, {
    createHTML: e => e
  })
} catch {}
var _t = ht ? e => ht.createHTML(e) : e => e,
  vt = `http://www.w3.org/2000/svg`,
  yt = `http://www.w3.org/1998/Math/MathML`,
  bt = typeof document < `u` ? document : null,
  xt = bt && bt.createElement(`template`),
  St = {
    insert: (e, t, n) => {
      t.insertBefore(e, n || null)
    },
    remove: e => {
      let t = e.parentNode;
      t && t.removeChild(e)
    },
    createElement: (e, t, n, r) => {
      let i = t === `svg` ? bt.createElementNS(vt, e) : t === `mathml` ? bt.createElementNS(yt, e) : n ? bt.createElement(e, {
        is: n
      }) : bt.createElement(e);
      return e === `select` && r && r.multiple != null && i.setAttribute(`multiple`, r.multiple), i
    },
    createText: e => bt.createTextNode(e),
    createComment: e => bt.createComment(e),
    setText: (e, t) => {
      e.nodeValue = t
    },
    setElementText: (e, t) => {
      e.textContent = t
    },
    parentNode: e => e.parentNode,
    nextSibling: e => e.nextSibling,
    querySelector: e => bt.querySelector(e),
    setScopeId(e, t) {
      e.setAttribute(t, ``)
    },
    insertStaticContent(e, t, n, r, i, a) {
      let o = n ? n.previousSibling : t.lastChild;
      if (i && (i === a || i.nextSibling))
        for (; t.insertBefore(i.cloneNode(!0), n), !(i === a || !(i = i.nextSibling)););
      else {
        xt.innerHTML = _t(r === `svg` ? `<svg>${e}</svg>` : r === `mathml` ? `<math>${e}</math>` : e);
        let i = xt.content;
        if (r === `svg` || r === `mathml`) {
          let e = i.firstChild;
          for (; e.firstChild;) i.appendChild(e.firstChild);
          i.removeChild(e)
        }
        t.insertBefore(i, n)
      }
      return [o ? o.nextSibling : t.firstChild, n ? n.previousSibling : t.lastChild]
    }
  },
  Ct = `transition`,
  wt = `animation`,
  Tt = Symbol(`_vtc`),
  Et = {
    name: String,
    type: String,
    css: {
      type: Boolean,
      default: !0
    },
    duration: [String, Number, Object],
    enterFromClass: String,
    enterActiveClass: String,
    enterToClass: String,
    appearFromClass: String,
    appearActiveClass: String,
    appearToClass: String,
    leaveFromClass: String,
    leaveActiveClass: String,
    leaveToClass: String
  },
  Dt = ge({}, Xe, Et),
  Ot = (e => (e.displayName = `Transition`, e.props = Dt, e))((e, {
    slots: t
  }) => z(Ne, jt(e), t)),
  kt = (e, t = []) => {
    Fe(e) ? e.forEach(e => e(...t)) : e && e(...t)
  },
  At = e => e ? Fe(e) ? e.some(e => e.length > 1) : e.length > 1 : !1;

function jt(e) {
  let t = {};
  for (let n in e) n in Et || (t[n] = e[n]);
  if (e.css === !1) return t;
  let {
    name: n = `v`,
    type: r,
    duration: i,
    enterFromClass: a = `${n}-enter-from`,
    enterActiveClass: o = `${n}-enter-active`,
    enterToClass: s = `${n}-enter-to`,
    appearFromClass: c = a,
    appearActiveClass: l = o,
    appearToClass: u = s,
    leaveFromClass: d = `${n}-leave-from`,
    leaveActiveClass: f = `${n}-leave-active`,
    leaveToClass: p = `${n}-leave-to`
  } = e, m = Mt(i), h = m && m[0], g = m && m[1], {
    onBeforeEnter: _,
    onEnter: v,
    onEnterCancelled: y,
    onLeave: b,
    onLeaveCancelled: x,
    onBeforeAppear: S = _,
    onAppear: C = v,
    onAppearCancelled: w = y
  } = t, T = (e, t, n, r) => {
    e._enterCancelled = r, Ft(e, t ? u : s), Ft(e, t ? l : o), n && n()
  }, E = (e, t) => {
    e._isLeaving = !1, Ft(e, d), Ft(e, p), Ft(e, f), t && t()
  }, D = e => (t, n) => {
    let i = e ? C : v,
      o = () => T(t, e, n);
    kt(i, [t, o]), It(() => {
      Ft(t, e ? c : a), Pt(t, e ? u : s), At(i) || Rt(t, r, h, o)
    })
  };
  return ge(t, {
    onBeforeEnter(e) {
      kt(_, [e]), Pt(e, a), Pt(e, o)
    },
    onBeforeAppear(e) {
      kt(S, [e]), Pt(e, c), Pt(e, l)
    },
    onEnter: D(!1),
    onAppear: D(!0),
    onLeave(e, t) {
      e._isLeaving = !0;
      let n = () => E(e, t);
      Pt(e, d), e._enterCancelled ? (Pt(e, f), Ht(e)) : (Ht(e), Pt(e, f)), It(() => {
        e._isLeaving && (Ft(e, d), Pt(e, p), At(b) || Rt(e, r, g, n))
      }), kt(b, [e, n])
    },
    onEnterCancelled(e) {
      T(e, !1, void 0, !0), kt(y, [e])
    },
    onAppearCancelled(e) {
      T(e, !0, void 0, !0), kt(w, [e])
    },
    onLeaveCancelled(e) {
      E(e), kt(x, [e])
    }
  })
}

function Mt(e) {
  if (e == null) return null;
  if (ue(e)) return [Nt(e.enter), Nt(e.leave)];
  {
    let t = Nt(e);
    return [t, t]
  }
}

function Nt(e) {
  return ne(e)
}

function Pt(e, t) {
  t.split(/\s+/).forEach(t => t && e.classList.add(t)), (e[Tt] || (e[Tt] = new Set)).add(t)
}

function Ft(e, t) {
  t.split(/\s+/).forEach(t => t && e.classList.remove(t));
  let n = e[Tt];
  n && (n.delete(t), n.size || (e[Tt] = void 0))
}

function It(e) {
  requestAnimationFrame(() => {
    requestAnimationFrame(e)
  })
}
var Lt = 0;

function Rt(e, t, n, r) {
  let i = e._endId = ++Lt,
    a = () => {
      i === e._endId && r()
    };
  if (n != null) return setTimeout(a, n);
  let {
    type: o,
    timeout: s,
    propCount: c
  } = zt(e, t);
  if (!o) return r();
  let l = o + `end`,
    u = 0,
    d = () => {
      e.removeEventListener(l, f), a()
    },
    f = t => {
      t.target === e && ++u >= c && d()
    };
  setTimeout(() => {
    u < c && d()
  }, s + 1), e.addEventListener(l, f)
}

function zt(e, t) {
  let n = window.getComputedStyle(e),
    r = e => (n[e] || ``).split(`, `),
    i = r(`${Ct}Delay`),
    a = r(`${Ct}Duration`),
    o = Bt(i, a),
    s = r(`${wt}Delay`),
    c = r(`${wt}Duration`),
    l = Bt(s, c),
    u = null,
    d = 0,
    f = 0;
  t === Ct ? o > 0 && (u = Ct, d = o, f = a.length) : t === wt ? l > 0 && (u = wt, d = l, f = c.length) : (d = Math.max(o, l), u = d > 0 ? o > l ? Ct : wt : null, f = u ? u === Ct ? a.length : c.length : 0);
  let p = u === Ct && /\b(?:transform|all)(?:,|$)/.test(r(`${Ct}Property`).toString());
  return {
    type: u,
    timeout: d,
    propCount: f,
    hasTransform: p
  }
}

function Bt(e, t) {
  for (; e.length < t.length;) e = e.concat(e);
  return Math.max(...t.map((t, n) => Vt(t) + Vt(e[n])))
}

function Vt(e) {
  return e === `auto` ? 0 : Number(e.slice(0, -1).replace(`,`, `.`)) * 1e3
}

function Ht(e) {
  return (e ? e.ownerDocument : document).body.offsetHeight
}

function Ut(e, t, n) {
  let r = e[Tt];
  r && (t = (t ? [t, ...r] : [...r]).join(` `)), t == null ? e.removeAttribute(`class`) : n ? e.setAttribute(`class`, t) : e.className = t
}
var Wt = Symbol(`_vod`),
  Gt = Symbol(`_vsh`),
  Kt = {
    name: `show`,
    beforeMount(e, {
      value: t
    }, {
      transition: n
    }) {
      e[Wt] = e.style.display === `none` ? `` : e.style.display, n && t ? n.beforeEnter(e) : qt(e, t)
    },
    mounted(e, {
      value: t
    }, {
      transition: n
    }) {
      n && t && n.enter(e)
    },
    updated(e, {
      value: t,
      oldValue: n
    }, {
      transition: r
    }) {
      !t != !n && (r ? t ? (r.beforeEnter(e), qt(e, !0), r.enter(e)) : r.leave(e, () => {
        qt(e, !1)
      }) : qt(e, t))
    },
    beforeUnmount(e, {
      value: t
    }) {
      qt(e, t)
    }
  };

function qt(e, t) {
  e.style.display = t ? e[Wt] : `none`, e[Gt] = !t
}
var Jt = Symbol(``),
  Yt = /(?:^|;)\s*display\s*:/;

function Xt(e, t, n) {
  let r = e.style,
    i = fe(n),
    a = !1;
  if (n && !i) {
    if (t)
      if (fe(t))
        for (let e of t.split(`;`)) {
          let t = e.slice(0, e.indexOf(`:`)).trim();
          n[t] == null && Qt(r, t, ``)
        } else
          for (let e in t) n[e] == null && Qt(r, e, ``);
    for (let i in n) {
      i === `display` && (a = !0);
      let o = n[i];
      o == null ? Qt(r, i, ``) : nn(e, i, !fe(t) && t ? t[i] : void 0, o) || Qt(r, i, o)
    }
  } else if (i) {
    if (t !== n) {
      let e = r[Jt];
      e && (n += `;` + e), r.cssText = n, a = Yt.test(n)
    }
  } else t && e.removeAttribute(`style`);
  Wt in e && (e[Wt] = a ? r.display : ``, e[Gt] && (r.display = `none`))
}
var Zt = /\s*!important$/;

function Qt(e, t, n) {
  if (Fe(n)) n.forEach(n => Qt(e, t, n));
  else if (n == null && (n = ``), t.startsWith(`--`)) e.setProperty(t, n);
  else {
    let r = tn(e, t);
    Zt.test(n) ? e.setProperty(be(r), n.replace(Zt, ``), `important`) : e[r] = n
  }
}
var $t = [`Webkit`, `Moz`, `ms`],
  en = {};

function tn(e, t) {
  let n = en[t];
  if (n) return n;
  let r = Me(t);
  if (r !== `filter` && r in e) return en[t] = r;
  r = Ye(r);
  for (let n = 0; n < $t.length; n++) {
    let i = $t[n] + r;
    if (i in e) return en[t] = i
  }
  return t
}

function nn(e, t, n, r) {
  return e.tagName === `TEXTAREA` && (t === `width` || t === `height`) && fe(r) && n === r
}
var rn = `http://www.w3.org/1999/xlink`;

function an(e, t, n, r, i, a = it(t)) {
  r && t.startsWith(`xlink:`) ? n == null ? e.removeAttributeNS(rn, t.slice(6, t.length)) : e.setAttributeNS(rn, t, n) : n == null || a && !Ve(n) ? e.removeAttribute(t) : e.setAttribute(t, a ? `` : nt(n) ? String(n) : n)
}

function on(e, t, n, r, i) {
  if (t === `innerHTML` || t === `textContent`) {
    n != null && (e[t] = t === `innerHTML` ? _t(n) : n);
    return
  }
  let a = e.tagName;
  if (t === `value` && a !== `PROGRESS` && !a.includes(`-`)) {
    let r = a === `OPTION` ? e.getAttribute(`value`) || `` : e.value,
      i = n == null ? e.type === `checkbox` ? `on` : `` : String(n);
    (r !== i || !(`_value` in e)) && (e.value = i), n == null && e.removeAttribute(t), e._value = n;
    return
  }
  let o = !1;
  if (n === `` || n == null) {
    let r = typeof e[t];
    r === `boolean` ? n = Ve(n) : n == null && r === `string` ? (n = ``, o = !0) : r === `number` && (n = 0, o = !0)
  }
  try {
    e[t] = n
  } catch {}
  o && e.removeAttribute(i || t)
}

function sn(e, t, n, r) {
  e.addEventListener(t, n, r)
}

function cn(e, t, n, r) {
  e.removeEventListener(t, n, r)
}
var ln = Symbol(`_vei`);

function un(e, t, n, r, i = null) {
  let a = e[ln] || (e[ln] = {}),
    o = a[t];
  if (r && o) o.value = r;
  else {
    let [n, s] = pn(t);
    r ? sn(e, n, a[t] = _n(r, i), s) : o && (cn(e, n, o, s), a[t] = void 0)
  }
}
var dn = /(Once|Passive|Capture)$/,
  fn = /^on:?(?:Once|Passive|Capture)$/;

function pn(e) {
  let t, n;
  for (;
    (n = e.match(dn)) && !fn.test(e);) t || (t = {}), e = e.slice(0, e.length - n[1].length), t[n[1].toLowerCase()] = !0;
  return [e[2] === `:` ? e.slice(3) : be(e.slice(2)), t]
}
var mn = 0,
  hn = Promise.resolve(),
  gn = () => mn || (hn.then(() => mn = 0), mn = Date.now());

function _n(e, t) {
  let n = e => {
    if (!e._vts) e._vts = Date.now();
    else if (e._vts <= n.attached) return;
    let r = n.value;
    if (Fe(r)) {
      let n = e.stopImmediatePropagation;
      e.stopImmediatePropagation = () => {
        n.call(e), e._stopped = !0
      };
      let i = r.slice(),
        a = [e];
      for (let n = 0; n < i.length && !e._stopped; n++) {
        let e = i[n];
        e && Ee(e, t, 5, a)
      }
    } else Ee(r, t, 5, [e])
  };
  return n.value = e, n.attached = gn(), n
}
var vn = e => e.charCodeAt(0) === 111 && e.charCodeAt(1) === 110 && e.charCodeAt(2) > 96 && e.charCodeAt(2) < 123,
  yn = (e, t, n, r, i, a) => {
    let o = i === `svg`;
    t === `class` ? Ut(e, r, o) : t === `style` ? Xt(e, n, r) : Qe(t) ? Se(t) || un(e, t, n, r, a) : (t[0] === `.` ? (t = t.slice(1), !0) : t[0] === `^` ? (t = t.slice(1), !1) : bn(e, t, r, o)) ? (on(e, t, r), !e.tagName.includes(`-`) && (t === `value` || t === `checked` || t === `selected`) && an(e, t, r, o, a, t !== `value`)) : e._isVueCE && (xn(e, t) || e._def.__asyncLoader && (/[A-Z]/.test(t) || !fe(r))) ? on(e, Me(t), r, a, t) : (t === `true-value` ? e._trueValue = r : t === `false-value` && (e._falseValue = r), an(e, t, r, o))
  };

function bn(e, t, n, r) {
  if (r) return !!(t === `innerHTML` || t === `textContent` || t in e && vn(t) && Te(n));
  if (t === `spellcheck` || t === `draggable` || t === `translate` || t === `autocorrect` || t === `sandbox` && e.tagName === `IFRAME` || t === `form` || t === `list` && e.tagName === `INPUT` || t === `type` && e.tagName === `TEXTAREA`) return !1;
  if (t === `width` || t === `height`) {
    let t = e.tagName;
    if (t === `IMG` || t === `VIDEO` || t === `CANVAS` || t === `SOURCE`) return !1
  }
  return vn(t) && fe(n) ? !1 : t in e
}

function xn(e, t) {
  let n = e._def.props;
  if (!n) return !1;
  let r = Me(t);
  return Array.isArray(n) ? n.some(e => Me(e) === r) : Object.keys(n).some(e => Me(e) === r)
}
var Sn = new WeakMap,
  Cn = new WeakMap,
  wn = Symbol(`_moveCb`),
  Tn = Symbol(`_enterCb`),
  En = (e => (delete e.props.mode, e))({
    name: `TransitionGroup`,
    props: ge({}, Dt, {
      tag: String,
      moveClass: String
    }),
    setup(e, {
      slots: t
    }) {
      let n = s(),
        r = oe(),
        a, o;
      return i(() => {
        if (!a.length) return;
        let t = e.moveClass || `${e.name||`v`}-move`;
        if (!jn(a[0].el, n.vnode.el, t)) {
          a = [];
          return
        }
        a.forEach(Dn), a.forEach(On);
        let r = a.filter(kn);
        Ht(n.vnode.el), r.forEach(e => {
          let n = e.el,
            r = n.style;
          Pt(n, t), r.transform = r.webkitTransform = r.transitionDuration = ``;
          let i = n[wn] = e => {
            e && e.target !== n || (!e || e.propertyName.endsWith(`transform`)) && (n.removeEventListener(`transitionend`, i), n[wn] = null, Ft(n, t))
          };
          n.addEventListener(`transitionend`, i)
        }), a = []
      }), () => {
        let i = ze(e),
          s = jt(i),
          c = i.tag || L;
        if (a = [], o)
          for (let e = 0; e < o.length; e++) {
            let t = o[e];
            t.el && t.el instanceof Element && !t.el[Gt] && (a.push(t), v(t, We(t, s, r, n)), Sn.set(t, An(t.el)))
          }
        o = t.default ? E(t.default()) : [];
        for (let e = 0; e < o.length; e++) {
          let t = o[e];
          t.key != null && v(t, We(t, s, r, n))
        }
        return re(c, null, o)
      }
    }
  });

function Dn(e) {
  let t = e.el;
  t[wn] && t[wn](), t[Tn] && t[Tn]()
}

function On(e) {
  Cn.set(e, An(e.el))
}

function kn(e) {
  let t = Sn.get(e),
    n = Cn.get(e),
    r = t.left - n.left,
    i = t.top - n.top;
  if (r || i) {
    let t = e.el,
      n = t.style,
      a = t.getBoundingClientRect(),
      o = 1,
      s = 1;
    return t.offsetWidth && (o = a.width / t.offsetWidth), t.offsetHeight && (s = a.height / t.offsetHeight), (!Number.isFinite(o) || o === 0) && (o = 1), (!Number.isFinite(s) || s === 0) && (s = 1), Math.abs(o - 1) < .01 && (o = 1), Math.abs(s - 1) < .01 && (s = 1), n.transform = n.webkitTransform = `translate(${r/o}px,${i/s}px)`, n.transitionDuration = `0s`, e
  }
}

function An(e) {
  let t = e.getBoundingClientRect();
  return {
    left: t.left,
    top: t.top
  }
}

function jn(e, t, n) {
  let r = e.cloneNode(),
    i = e[Tt];
  i && i.forEach(e => {
    e.split(/\s+/).forEach(e => e && r.classList.remove(e))
  }), n.split(/\s+/).forEach(e => e && r.classList.add(e)), r.style.display = `none`;
  let a = t.nodeType === 1 ? t : t.parentNode;
  a.appendChild(r);
  let {
    hasTransform: o
  } = zt(r);
  return a.removeChild(r), o
}
var Mn = [`ctrl`, `shift`, `alt`, `meta`],
  Nn = {
    stop: e => e.stopPropagation(),
    prevent: e => e.preventDefault(),
    self: e => e.target !== e.currentTarget,
    ctrl: e => !e.ctrlKey,
    shift: e => !e.shiftKey,
    alt: e => !e.altKey,
    meta: e => !e.metaKey,
    left: e => `button` in e && e.button !== 0,
    middle: e => `button` in e && e.button !== 1,
    right: e => `button` in e && e.button !== 2,
    exact: (e, t) => Mn.some(n => e[`${n}Key`] && !t.includes(n))
  },
  Pn = (e, t) => {
    if (!e) return e;
    let n = e._withMods || (e._withMods = {}),
      r = t.join(`.`);
    return n[r] || (n[r] = ((n, ...r) => {
      for (let e = 0; e < t.length; e++) {
        let r = Nn[t[e]];
        if (r && r(n, t)) return
      }
      return e(n, ...r)
    }))
  },
  Fn = ge({
    patchProp: yn
  }, St),
  In;

function Ln() {
  return In || (In = te(Fn))
}
var Rn = ((...e) => {
  let t = Ln().createApp(...e),
    {
      mount: n
    } = t;
  return t.mount = e => {
    let r = Bn(e);
    if (!r) return;
    let i = t._component;
    !Te(i) && !i.render && !i.template && (i.template = r.innerHTML), r.nodeType === 1 && (r.textContent = ``);
    let a = n(r, !1, zn(r));
    return r instanceof Element && (r.removeAttribute(`v-cloak`), r.setAttribute(`data-v-app`, ``)), a
  }, t
});

function zn(e) {
  if (e instanceof SVGElement) return `svg`;
  if (typeof MathMLElement == `function` && e instanceof MathMLElement) return `mathml`
}

function Bn(e) {
  return fe(e) ? document.querySelector(e) : e
}
var Vn = [],
  Hn = new WeakMap;

function Un() {
  Vn.forEach(e => e(...Hn.get(e))), Vn = []
}

function Wn(e, ...t) {
  Hn.set(e, t), !Vn.includes(e) && Vn.push(e) === 1 && requestAnimationFrame(Un)
}

function Gn(e) {
  return e.composedPath()[0] || null
}

function Kn(e) {
  return typeof e == `string` ? e.endsWith(`px`) ? Number(e.slice(0, e.length - 2)) : Number(e) : e
}

function qn(e) {
  if (e != null) return typeof e == `number` ? `${e}px` : e.endsWith(`px`) ? e : `${e}px`
}

function Jn(e, t) {
  let n = e.trim().split(/\s+/g),
    r = {
      top: n[0]
    };
  switch (n.length) {
    case 1:
      r.right = n[0], r.bottom = n[0], r.left = n[0];
      break;
    case 2:
      r.right = n[1], r.left = n[1], r.bottom = n[0];
      break;
    case 3:
      r.right = n[1], r.bottom = n[2], r.left = n[1];
      break;
    case 4:
      r.right = n[1], r.bottom = n[2], r.left = n[3];
      break;
    default:
      throw Error(`[seemly/getMargin]:` + e + ` is not a valid value.`)
  }
  return t === void 0 ? r : r[t]
}

function Yn(e, t) {
  let [n, r] = e.split(` `);
  return t ? t === `row` ? n : r : {
    row: n,
    col: r || n
  }
}

function Xn(e = 8) {
  return Math.random().toString(16).slice(2, 2 + e)
}

function Zn(e, t) {
  let n = [];
  for (let r = 0; r < e; ++r) n.push(t);
  return n
}

function Qn(e) {
  return e.composedPath()[0]
}
var $n = {
  mousemoveoutside: new WeakMap,
  clickoutside: new WeakMap
};

function er(e, t, n) {
  if (e === `mousemoveoutside`) {
    let e = e => {
      t.contains(Qn(e)) || n(e)
    };
    return {
      mousemove: e,
      touchstart: e
    }
  } else if (e === `clickoutside`) {
    let e = !1,
      r = n => {
        e = !t.contains(Qn(n))
      },
      i = r => {
        e && (t.contains(Qn(r)) || n(r))
      };
    return {
      mousedown: r,
      mouseup: i,
      touchstart: r,
      touchend: i
    }
  }
  return console.error(`[evtd/create-trap-handler]: name \`${e}\` is invalid. This could be a bug of evtd.`), {}
}

function tr(e, t, n) {
  let r = $n[e],
    i = r.get(t);
  i === void 0 && r.set(t, i = new WeakMap);
  let a = i.get(n);
  return a === void 0 && i.set(n, a = er(e, t, n)), a
}

function nr(e, t, n, r) {
  if (e === `mousemoveoutside` || e === `clickoutside`) {
    let i = tr(e, t, n);
    return Object.keys(i).forEach(e => {
      W(e, document, i[e], r)
    }), !0
  }
  return !1
}

function rr(e, t, n, r) {
  if (e === `mousemoveoutside` || e === `clickoutside`) {
    let i = tr(e, t, n);
    return Object.keys(i).forEach(e => {
      G(e, document, i[e], r)
    }), !0
  }
  return !1
}

function ir() {
  if (typeof window > `u`) return {
    on: () => {},
    off: () => {}
  };
  let e = new WeakMap,
    t = new WeakMap;

  function n() {
    e.set(this, !0)
  }

  function r() {
    e.set(this, !0), t.set(this, !0)
  }

  function i(e, t, n) {
    let r = e[t];
    return e[t] = function() {
      return n.apply(e, arguments), r.apply(e, arguments)
    }, e
  }

  function a(e, t) {
    e[t] = Event.prototype[t]
  }
  let o = new WeakMap,
    s = Object.getOwnPropertyDescriptor(Event.prototype, `currentTarget`);

  function c() {
    var e;
    return (e = o.get(this)) == null ? null : e
  }

  function l(e, t) {
    s !== void 0 && Object.defineProperty(e, "currentTarget", {
      configurable: !0,
      enumerable: !0,
      get: t == null ? s.get : t
    })
  }
  let u = {
      bubble: {},
      capture: {}
    },
    d = {};

  function f() {
    let s = function(s) {
      let {
        type: d,
        eventPhase: f,
        bubbles: p
      } = s, m = Qn(s);
      if (f === 2) return;
      let h = f === 1 ? `capture` : `bubble`,
        g = m,
        _ = [];
      for (; g === null && (g = window), _.push(g), g !== window;) g = g.parentNode || null;
      let v = u.capture[d],
        y = u.bubble[d];
      if (i(s, `stopPropagation`, n), i(s, `stopImmediatePropagation`, r), l(s, c), h === `capture`) {
        if (v === void 0) return;
        for (let n = _.length - 1; n >= 0 && !e.has(s); --n) {
          let e = _[n],
            r = v.get(e);
          if (r !== void 0) {
            o.set(s, e);
            for (let e of r) {
              if (t.has(s)) break;
              e(s)
            }
          }
          if (n === 0 && !p && y !== void 0) {
            let n = y.get(e);
            if (n !== void 0)
              for (let e of n) {
                if (t.has(s)) break;
                e(s)
              }
          }
        }
      } else if (h === `bubble`) {
        if (y === void 0) return;
        for (let n = 0; n < _.length && !e.has(s); ++n) {
          let e = _[n],
            r = y.get(e);
          if (r !== void 0) {
            o.set(s, e);
            for (let e of r) {
              if (t.has(s)) break;
              e(s)
            }
          }
        }
      }
      a(s, `stopPropagation`), a(s, `stopImmediatePropagation`), l(s)
    };
    return s.displayName = `evtdUnifiedHandler`, s
  }

  function p() {
    let e = function(e) {
      let {
        type: t,
        eventPhase: n
      } = e;
      if (n !== 2) return;
      let r = d[t];
      r !== void 0 && r.forEach(t => t(e))
    };
    return e.displayName = `evtdUnifiedWindowEventHandler`, e
  }
  let m = f(),
    h = p();

  function g(e, t) {
    let n = u[e];
    return n[t] === void 0 && (n[t] = new Map, window.addEventListener(t, m, e === `capture`)), n[t]
  }

  function _(e) {
    return d[e] === void 0 && (d[e] = new Set, window.addEventListener(e, h)), d[e]
  }

  function v(e, t) {
    let n = e.get(t);
    return n === void 0 && e.set(t, n = new Set), n
  }

  function y(e, t, n, r) {
    let i = u[t][n];
    if (i !== void 0) {
      let t = i.get(e);
      if (t !== void 0 && t.has(r)) return !0
    }
    return !1
  }

  function b(e, t) {
    let n = d[e];
    return !!(n !== void 0 && n.has(t))
  }

  function x(e, t, n, r) {
    let i;
    if (i = typeof r == `object` && r.once === !0 ? a => {
        S(e, t, i, r), n(a)
      } : n, nr(e, t, i, r)) return;
    let a = v(g(r === !0 || typeof r == `object` && r.capture === !0 ? `capture` : `bubble`, e), t);
    if (a.has(i) || a.add(i), t === window) {
      let t = _(e);
      t.has(i) || t.add(i)
    }
  }

  function S(e, t, n, r) {
    if (rr(e, t, n, r)) return;
    let i = r === !0 || typeof r == `object` && r.capture === !0,
      a = i ? `capture` : `bubble`,
      o = g(a, e),
      s = v(o, t);
    if (t === window && !y(t, i ? `bubble` : `capture`, e, n) && b(e, n)) {
      let t = d[e];
      t.delete(n), t.size === 0 && (window.removeEventListener(e, h), d[e] = void 0)
    }
    s.has(n) && s.delete(n), s.size === 0 && o.delete(t), o.size === 0 && (window.removeEventListener(e, m, a === `capture`), u[a][e] = void 0)
  }
  return {
    on: x,
    off: S
  }
}
var {
  on: W,
  off: G
} = ir();

function ar(e) {
  let t = U(!!e.value);
  if (t.value) return Le(t);
  let n = P(e, e => {
    e && (t.value = !0, n())
  });
  return Le(t)
}

function or(e) {
  let t = F(e),
    n = U(t.value);
  return P(t, e => {
    n.value = e
  }), typeof e == `function` ? n : {
    __v_isRef: !0,
    get value() {
      return n.value
    },
    set value(t) {
      e.set(t)
    }
  }
}

function sr() {
  return s() !== null
}
var cr = typeof window < `u`,
  lr, ur;
(() => {
  var e, t;
  lr = cr ? (t = (e = document) == null ? void 0 : e.fonts) == null ? void 0 : t.ready : void 0, ur = !1, lr === void 0 ? ur = !0 : lr.then(() => {
    ur = !0
  })
})();

function dr(e) {
  if (ur) return;
  let t = !1;
  j(() => {
    ur || lr == null || lr.then(() => {
      t || e()
    })
  }), g(() => {
    t = !0
  })
}

function fr(e, t) {
  return P(e, e => {
    e !== void 0 && (t.value = e)
  }), F(() => e.value === void 0 ? t.value : e.value)
}

function pr() {
  let e = U(!1);
  return j(() => {
    e.value = !0
  }), Le(e)
}

function mr(e, t) {
  return F(() => {
    for (let n of t)
      if (e[n] !== void 0) return e[n];
    return e[t[t.length - 1]]
  })
}
var hr = (typeof window > `u` ? !1 : /iPad|iPhone|iPod/.test(navigator.platform) || navigator.platform === `MacIntel` && navigator.maxTouchPoints > 1) && !window.MSStream;

function gr() {
  return hr
}
var _r = V(`n-internal-select-menu`),
  vr = V(`n-internal-select-menu-body`),
  yr = V(`n-drawer-body`);
V(`n-drawer`);
var br = V(`n-modal-body`),
  xr = V(`n-modal-provider`),
  Sr = V(`n-modal`),
  Cr = V(`n-popover-body`),
  wr = `__disabled__`;

function Tr(e) {
  let t = n(br, null),
    r = n(yr, null),
    i = n(Cr, null),
    a = n(vr, null),
    o = U();
  if (typeof document < `u`) {
    o.value = document.fullscreenElement;
    let e = () => {
      o.value = document.fullscreenElement
    };
    j(() => {
      W(`fullscreenchange`, document, e)
    }), g(() => {
      G(`fullscreenchange`, document, e)
    })
  }
  return or(() => {
    var n;
    let {
      to: s
    } = e;
    return s === void 0 ? t != null && t.value ? (n = t.value.$el) == null ? t.value : n : r != null && r.value ? r.value : i != null && i.value ? i.value : a != null && a.value ? a.value : s == null ? o.value || `body` : s : s === !1 ? wr : s === !0 ? o.value || `body` : s
  })
}
Tr.tdkey = wr, Tr.propTo = {
  type: [String, Object, Boolean],
  default: void 0
};
var Er = typeof document < `u` && typeof window < `u`;

function Dr(e) {
  let t = {
      isDeactivated: !1
    },
    n = !1;
  return O(() => {
    if (t.isDeactivated = !1, !n) {
      n = !0;
      return
    }
    e()
  }), b(() => {
    t.isDeactivated = !0, n || (n = !0)
  }), t
}

function Or(e, t, n = `default`) {
  let r = t[n];
  if (r === void 0) throw Error(`[vueuc/${e}]: slot[${n}] is empty.`);
  return r()
}

function kr(e, t = !0, n = []) {
  return e.forEach(e => {
    if (e !== null) {
      if (typeof e != `object`) {
        (typeof e == `string` || typeof e == `number`) && n.push(et(String(e)));
        return
      }
      if (Array.isArray(e)) {
        kr(e, t, n);
        return
      }
      if (e.type === L) {
        if (e.children === null) return;
        Array.isArray(e.children) && kr(e.children, t, n)
      } else e.type !== _e && n.push(e)
    }
  }), n
}

function Ar(e, t, n = `default`) {
  let r = t[n];
  if (r === void 0) throw Error(`[vueuc/${e}]: slot[${n}] is empty.`);
  let i = kr(r());
  if (i.length === 1) return i[0];
  throw Error(`[vueuc/${e}]: slot[${n}] should have exactly one child.`)
}
var jr = null;

function Mr() {
  if (jr === null && (jr = document.getElementById(`v-binder-view-measurer`), jr === null)) {
    jr = document.createElement(`div`), jr.id = `v-binder-view-measurer`;
    let {
      style: e
    } = jr;
    e.position = `fixed`, e.left = `0`, e.right = `0`, e.top = `0`, e.bottom = `0`, e.pointerEvents = `none`, e.visibility = `hidden`, document.body.appendChild(jr)
  }
  return jr.getBoundingClientRect()
}

function Nr(e, t) {
  let n = Mr();
  return {
    top: t,
    left: e,
    height: 0,
    width: 0,
    right: n.width - e,
    bottom: n.height - t
  }
}

function Pr(e) {
  let t = e.getBoundingClientRect(),
    n = Mr();
  return {
    left: t.left - n.left,
    top: t.top - n.top,
    bottom: n.height + n.top - t.bottom,
    right: n.width + n.left - t.right,
    width: t.width,
    height: t.height
  }
}

function Fr(e) {
  return e.nodeType === 9 ? null : e.parentNode
}

function Ir(e) {
  if (e === null) return null;
  let t = Fr(e);
  if (t === null) return null;
  if (t.nodeType === 9) return document;
  if (t.nodeType === 1) {
    let {
      overflow: e,
      overflowX: n,
      overflowY: r
    } = getComputedStyle(t);
    if (/(auto|scroll|overlay)/.test(e + r + n)) return t
  }
  return Ir(t)
}
var Lr = l({
    name: `Binder`,
    props: {
      syncTargetWithParent: Boolean,
      syncTarget: {
        type: Boolean,
        default: !0
      }
    },
    setup(e) {
      var t;
      m(`VBinder`, (t = s()) == null ? void 0 : t.proxy);
      let r = n(`VBinder`, null),
        i = U(null),
        a = t => {
          i.value = t, r && e.syncTargetWithParent && r.setTargetRef(t)
        },
        o = [],
        c = () => {
          let e = i.value;
          for (; e = Ir(e), e !== null;) o.push(e);
          for (let e of o) W(`scroll`, e, p, !0)
        },
        l = () => {
          for (let e of o) G(`scroll`, e, p, !0);
          o = []
        },
        u = new Set,
        d = e => {
          u.size === 0 && c(), u.has(e) || u.add(e)
        },
        f = e => {
          u.has(e) && u.delete(e), u.size === 0 && l()
        },
        p = () => {
          Wn(h)
        },
        h = () => {
          u.forEach(e => e())
        },
        _ = new Set,
        v = e => {
          _.size === 0 && W(`resize`, window, b), _.has(e) || _.add(e)
        },
        y = e => {
          _.has(e) && _.delete(e), _.size === 0 && G(`resize`, window, b)
        },
        b = () => {
          _.forEach(e => e())
        };
      return g(() => {
        G(`resize`, window, b), l()
      }), {
        targetRef: i,
        setTargetRef: a,
        addScrollListener: d,
        removeScrollListener: f,
        addResizeListener: v,
        removeResizeListener: y
      }
    },
    render() {
      return Or(`binder`, this.$slots)
    }
  }),
  Rr = l({
    name: `Target`,
    setup() {
      let {
        setTargetRef: e,
        syncTarget: t
      } = n(`VBinder`);
      return {
        syncTarget: t,
        setTargetDirective: {
          mounted: e,
          updated: e
        }
      }
    },
    render() {
      let {
        syncTarget: t,
        setTargetDirective: n
      } = this;
      return t ? e(Ar(`follower`, this.$slots), [
        [n]
      ]) : Ar(`follower`, this.$slots)
    }
  }),
  zr = `@@mmoContext`,
  Br = {
    mounted(e, {
      value: t
    }) {
      e[zr] = {
        handler: void 0
      }, typeof t == `function` && (e[zr].handler = t, W(`mousemoveoutside`, e, t))
    },
    updated(e, {
      value: t
    }) {
      let n = e[zr];
      typeof t == `function` ? n.handler ? n.handler !== t && (G(`mousemoveoutside`, e, n.handler), n.handler = t, W(`mousemoveoutside`, e, t)) : (e[zr].handler = t, W(`mousemoveoutside`, e, t)) : n.handler && (G(`mousemoveoutside`, e, n.handler), n.handler = void 0)
    },
    unmounted(e) {
      let {
        handler: t
      } = e[zr];
      t && G(`mousemoveoutside`, e, t), e[zr].handler = void 0
    }
  },
  Vr = `@@coContext`,
  Hr = {
    mounted(e, {
      value: t,
      modifiers: n
    }) {
      e[Vr] = {
        handler: void 0
      }, typeof t == `function` && (e[Vr].handler = t, W(`clickoutside`, e, t, {
        capture: n.capture
      }))
    },
    updated(e, {
      value: t,
      modifiers: n
    }) {
      let r = e[Vr];
      typeof t == `function` ? r.handler ? r.handler !== t && (G(`clickoutside`, e, r.handler, {
        capture: n.capture
      }), r.handler = t, W(`clickoutside`, e, t, {
        capture: n.capture
      })) : (e[Vr].handler = t, W(`clickoutside`, e, t, {
        capture: n.capture
      })) : r.handler && (G(`clickoutside`, e, r.handler, {
        capture: n.capture
      }), r.handler = void 0)
    },
    unmounted(e, {
      modifiers: t
    }) {
      let {
        handler: n
      } = e[Vr];
      n && G(`clickoutside`, e, n, {
        capture: t.capture
      }), e[Vr].handler = void 0
    }
  };

function Ur(e, t) {
  console.error(`[vdirs/${e}]: ${t}`)
}
var Wr = new class {
    constructor() {
      this.elementZIndex = new Map, this.nextZIndex = 2e3
    }
    get elementCount() {
      return this.elementZIndex.size
    }
    ensureZIndex(e, t) {
      let {
        elementZIndex: n
      } = this;
      if (t !== void 0) {
        e.style.zIndex = `${t}`, n.delete(e);
        return
      }
      let {
        nextZIndex: r
      } = this;
      n.has(e) && n.get(e) + 1 === this.nextZIndex || (e.style.zIndex = `${r}`, n.set(e, r), this.nextZIndex = r + 1, this.squashState())
    }
    unregister(e, t) {
      let {
        elementZIndex: n
      } = this;
      n.has(e) ? n.delete(e) : t === void 0 && Ur(`z-index-manager/unregister-element`, `Element not found when unregistering.`), this.squashState()
    }
    squashState() {
      let {
        elementCount: e
      } = this;
      e || (this.nextZIndex = 2e3), this.nextZIndex - e > 2500 && this.rearrange()
    }
    rearrange() {
      let e = Array.from(this.elementZIndex.entries());
      e.sort((e, t) => e[1] - t[1]), this.nextZIndex = 2e3, e.forEach(e => {
        let t = e[0],
          n = this.nextZIndex++;
        `${n}` !== t.style.zIndex && (t.style.zIndex = `${n}`)
      })
    }
  },
  Gr = `@@ziContext`,
  Kr = {
    mounted(e, t) {
      let {
        value: n = {}
      } = t, {
        zIndex: r,
        enabled: i
      } = n;
      e[Gr] = {
        enabled: !!i,
        initialized: !1
      }, i && (Wr.ensureZIndex(e, r), e[Gr].initialized = !0)
    },
    updated(e, t) {
      let {
        value: n = {}
      } = t, {
        zIndex: r,
        enabled: i
      } = n, a = e[Gr].enabled;
      i && !a && (Wr.ensureZIndex(e, r), e[Gr].initialized = !0), e[Gr].enabled = !!i
    },
    unmounted(e, t) {
      if (!e[Gr].initialized) return;
      let {
        value: n = {}
      } = t, {
        zIndex: r
      } = n;
      Wr.unregister(e, r)
    }
  };

function qr(e, t) {
  console.error(`[vueuc/${e}]: ${t}`)
}

function Jr(e, t) {
  if (e === void 0) return !1;
  if (t) {
    let {
      context: {
        ids: n
      }
    } = t;
    return n.has(e)
  }
  return ve(e) !== null
}
var {
  c: Yr
} = k(), Xr = `vueuc-style`;

function Zr(e) {
  return typeof e == `string` ? document.querySelector(e) : e() || null
}
var Qr = l({
    name: `LazyTeleport`,
    props: {
      to: {
        type: [String, Object],
        default: void 0
      },
      disabled: Boolean,
      show: {
        type: Boolean,
        required: !0
      }
    },
    setup(e) {
      return {
        showTeleport: ar(Ke(e, `show`)),
        mergedTo: F(() => {
          let {
            to: t
          } = e;
          return t == null ? `body` : t
        })
      }
    },
    render() {
      return this.showTeleport ? this.disabled ? Or(`lazy-teleport`, this.$slots) : z(He, {
        disabled: this.disabled,
        to: this.mergedTo
      }, Or(`lazy-teleport`, this.$slots)) : null
    }
  }),
  $r = {
    top: `bottom`,
    bottom: `top`,
    left: `right`,
    right: `left`
  },
  ei = {
    start: `end`,
    center: `center`,
    end: `start`
  },
  ti = {
    top: `height`,
    bottom: `height`,
    left: `width`,
    right: `width`
  },
  ni = {
    "bottom-start": `top left`,
    bottom: `top center`,
    "bottom-end": `top right`,
    "top-start": `bottom left`,
    top: `bottom center`,
    "top-end": `bottom right`,
    "right-start": `top left`,
    right: `center left`,
    "right-end": `bottom left`,
    "left-start": `top right`,
    left: `center right`,
    "left-end": `bottom right`
  },
  ri = {
    "bottom-start": `bottom left`,
    bottom: `bottom center`,
    "bottom-end": `bottom right`,
    "top-start": `top left`,
    top: `top center`,
    "top-end": `top right`,
    "right-start": `top right`,
    right: `center right`,
    "right-end": `bottom right`,
    "left-start": `top left`,
    left: `center left`,
    "left-end": `bottom left`
  },
  ii = {
    "bottom-start": `right`,
    "bottom-end": `left`,
    "top-start": `right`,
    "top-end": `left`,
    "right-start": `bottom`,
    "right-end": `top`,
    "left-start": `bottom`,
    "left-end": `top`
  },
  ai = {
    top: !0,
    bottom: !1,
    left: !0,
    right: !1
  },
  oi = {
    top: `end`,
    bottom: `start`,
    left: `end`,
    right: `start`
  };

function si(e, t, n, r, i, a) {
  if (!i || a) return {
    placement: e,
    top: 0,
    left: 0
  };
  let [o, s] = e.split(`-`), c = s == null ? `center` : s, l = {
    top: 0,
    left: 0
  }, u = (e, i, a) => {
    let o = 0,
      s = 0,
      c = n[e] - t[i] - t[e];
    return c > 0 && r && (a ? s = ai[i] ? c : -c : o = ai[i] ? c : -c), {
      left: o,
      top: s
    }
  }, d = o === `left` || o === `right`;
  if (c !== `center`) {
    let r = ii[e],
      i = $r[r],
      a = ti[r];
    if (n[a] > t[a]) {
      if (t[r] + t[a] < n[a]) {
        let e = (n[a] - t[a]) / 2;
        t[r] < e || t[i] < e ? t[r] < t[i] ? (c = ei[s], l = u(a, i, d)) : l = u(a, r, d) : c = `center`
      }
    } else n[a] < t[a] && t[i] < 0 && t[r] > t[i] && (c = ei[s])
  } else {
    let e = o === `bottom` || o === `top` ? `left` : `top`,
      r = $r[e],
      i = ti[e],
      a = (n[i] - t[i]) / 2;
    (t[e] < a || t[r] < a) && (t[e] > t[r] ? (c = oi[e], l = u(i, e, d)) : (c = oi[r], l = u(i, r, d)))
  }
  let f = o;
  return t[o] < n[ti[o]] && t[o] < t[$r[o]] && (f = $r[o]), {
    placement: c === `center` ? f : `${f}-${c}`,
    left: l.left,
    top: l.top
  }
}

function ci(e, t) {
  return t ? ri[e] : ni[e]
}

function li(e, t, n, r, i, a) {
  if (a) switch (e) {
    case `bottom-start`:
      return {
        top: `${Math.round(n.top-t.top+n.height)}px`, left: `${Math.round(n.left-t.left)}px`, transform: `translateY(-100%)`
      };
    case `bottom-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height)}px`, left: `${Math.round(n.left-t.left+n.width)}px`, transform: `translateX(-100%) translateY(-100%)`
      };
    case `top-start`:
      return {
        top: `${Math.round(n.top-t.top)}px`, left: `${Math.round(n.left-t.left)}px`, transform: ``
      };
    case `top-end`:
      return {
        top: `${Math.round(n.top-t.top)}px`, left: `${Math.round(n.left-t.left+n.width)}px`, transform: `translateX(-100%)`
      };
    case `right-start`:
      return {
        top: `${Math.round(n.top-t.top)}px`, left: `${Math.round(n.left-t.left+n.width)}px`, transform: `translateX(-100%)`
      };
    case `right-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height)}px`, left: `${Math.round(n.left-t.left+n.width)}px`, transform: `translateX(-100%) translateY(-100%)`
      };
    case `left-start`:
      return {
        top: `${Math.round(n.top-t.top)}px`, left: `${Math.round(n.left-t.left)}px`, transform: ``
      };
    case `left-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height)}px`, left: `${Math.round(n.left-t.left)}px`, transform: `translateY(-100%)`
      };
    case `top`:
      return {
        top: `${Math.round(n.top-t.top)}px`, left: `${Math.round(n.left-t.left+n.width/2)}px`, transform: `translateX(-50%)`
      };
    case `right`:
      return {
        top: `${Math.round(n.top-t.top+n.height/2)}px`, left: `${Math.round(n.left-t.left+n.width)}px`, transform: `translateX(-100%) translateY(-50%)`
      };
    case `left`:
      return {
        top: `${Math.round(n.top-t.top+n.height/2)}px`, left: `${Math.round(n.left-t.left)}px`, transform: `translateY(-50%)`
      };
    default:
      return {
        top: `${Math.round(n.top-t.top+n.height)}px`, left: `${Math.round(n.left-t.left+n.width/2)}px`, transform: `translateX(-50%) translateY(-100%)`
      }
  }
  switch (e) {
    case `bottom-start`:
      return {
        top: `${Math.round(n.top-t.top+n.height+r)}px`, left: `${Math.round(n.left-t.left+i)}px`, transform: ``
      };
    case `bottom-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height+r)}px`, left: `${Math.round(n.left-t.left+n.width+i)}px`, transform: `translateX(-100%)`
      };
    case `top-start`:
      return {
        top: `${Math.round(n.top-t.top+r)}px`, left: `${Math.round(n.left-t.left+i)}px`, transform: `translateY(-100%)`
      };
    case `top-end`:
      return {
        top: `${Math.round(n.top-t.top+r)}px`, left: `${Math.round(n.left-t.left+n.width+i)}px`, transform: `translateX(-100%) translateY(-100%)`
      };
    case `right-start`:
      return {
        top: `${Math.round(n.top-t.top+r)}px`, left: `${Math.round(n.left-t.left+n.width+i)}px`, transform: ``
      };
    case `right-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height+r)}px`, left: `${Math.round(n.left-t.left+n.width+i)}px`, transform: `translateY(-100%)`
      };
    case `left-start`:
      return {
        top: `${Math.round(n.top-t.top+r)}px`, left: `${Math.round(n.left-t.left+i)}px`, transform: `translateX(-100%)`
      };
    case `left-end`:
      return {
        top: `${Math.round(n.top-t.top+n.height+r)}px`, left: `${Math.round(n.left-t.left+i)}px`, transform: `translateX(-100%) translateY(-100%)`
      };
    case `top`:
      return {
        top: `${Math.round(n.top-t.top+r)}px`, left: `${Math.round(n.left-t.left+n.width/2+i)}px`, transform: `translateY(-100%) translateX(-50%)`
      };
    case `right`:
      return {
        top: `${Math.round(n.top-t.top+n.height/2+r)}px`, left: `${Math.round(n.left-t.left+n.width+i)}px`, transform: `translateY(-50%)`
      };
    case `left`:
      return {
        top: `${Math.round(n.top-t.top+n.height/2+r)}px`, left: `${Math.round(n.left-t.left+i)}px`, transform: `translateY(-50%) translateX(-100%)`
      };
    default:
      return {
        top: `${Math.round(n.top-t.top+n.height+r)}px`, left: `${Math.round(n.left-t.left+n.width/2+i)}px`, transform: `translateX(-50%)`
      }
  }
}
var ui = Yr([Yr(`.v-binder-follower-container`, {
    position: `absolute`,
    left: `0`,
    right: `0`,
    top: `0`,
    height: `0`,
    pointerEvents: `none`,
    zIndex: `auto`
  }), Yr(`.v-binder-follower-content`, {
    position: `absolute`,
    zIndex: `auto`
  }, [Yr(`> *`, {
    pointerEvents: `all`
  })])]),
  di = l({
    name: `Follower`,
    inheritAttrs: !1,
    props: {
      show: Boolean,
      enabled: {
        type: Boolean,
        default: void 0
      },
      placement: {
        type: String,
        default: `bottom`
      },
      syncTrigger: {
        type: Array,
        default: [`resize`, `scroll`]
      },
      to: [String, Object],
      flip: {
        type: Boolean,
        default: !0
      },
      internalShift: Boolean,
      x: Number,
      y: Number,
      width: String,
      minWidth: String,
      containerClass: String,
      teleportDisabled: Boolean,
      zindexable: {
        type: Boolean,
        default: !0
      },
      zIndex: Number,
      overlap: Boolean
    },
    setup(e) {
      let t = n(`VBinder`),
        r = or(() => e.enabled === void 0 ? e.show : e.enabled),
        i = U(null),
        a = U(null),
        o = () => {
          let {
            syncTrigger: n
          } = e;
          n.includes(`scroll`) && t.addScrollListener(l), n.includes(`resize`) && t.addResizeListener(l)
        },
        s = () => {
          t.removeScrollListener(l), t.removeResizeListener(l)
        };
      j(() => {
        r.value && (l(), o())
      });
      let c = qe();
      ui.mount({
        id: `vueuc/binder`,
        head: !0,
        anchorMetaName: Xr,
        ssr: c
      }), g(() => {
        s()
      }), dr(() => {
        r.value && l()
      });
      let l = () => {
        if (!r.value) return;
        let n = i.value;
        if (n === null) return;
        let o = t.targetRef,
          {
            x: s,
            y: c,
            overlap: l
          } = e,
          u = s !== void 0 && c !== void 0 ? Nr(s, c) : Pr(o);
        n.style.setProperty(`--v-target-width`, `${Math.round(u.width)}px`), n.style.setProperty(`--v-target-height`, `${Math.round(u.height)}px`);
        let {
          width: d,
          minWidth: f,
          placement: p,
          internalShift: m,
          flip: h
        } = e;
        n.setAttribute(`v-placement`, p), l ? n.setAttribute(`v-overlap`, ``) : n.removeAttribute(`v-overlap`);
        let {
          style: g
        } = n;
        d === `target` ? g.width = `${u.width}px` : d === void 0 ? g.width = `` : g.width = d, f === `target` ? g.minWidth = `${u.width}px` : f === void 0 ? g.minWidth = `` : g.minWidth = f;
        let _ = Pr(n),
          v = Pr(a.value),
          {
            left: y,
            top: b,
            placement: x
          } = si(p, u, _, m, h, l),
          S = ci(x, l),
          {
            left: C,
            top: w,
            transform: T
          } = li(x, v, u, b, y, l);
        n.setAttribute(`v-placement`, x), n.style.setProperty(`--v-offset-left`, `${Math.round(y)}px`), n.style.setProperty(`--v-offset-top`, `${Math.round(b)}px`), n.style.transform = `translateX(${C}) translateY(${w}) ${T}`, n.style.setProperty(`--v-transform-origin`, S), n.style.transformOrigin = S
      };
      P(r, e => {
        e ? (o(), u()) : s()
      });
      let u = () => {
        w().then(l).catch(e => console.error(e))
      };
      [`placement`, `x`, `y`, `internalShift`, `flip`, `width`, `overlap`, `minWidth`].forEach(t => {
        P(Ke(e, t), l)
      }), [`teleportDisabled`].forEach(t => {
        P(Ke(e, t), u)
      }), P(Ke(e, `syncTrigger`), e => {
        e.includes(`resize`) ? t.addResizeListener(l) : t.removeResizeListener(l), e.includes(`scroll`) ? t.addScrollListener(l) : t.removeScrollListener(l)
      });
      let d = pr();
      return {
        VBinder: t,
        mergedEnabled: r,
        offsetContainerRef: a,
        followerRef: i,
        mergedTo: or(() => {
          let {
            to: t
          } = e;
          if (t !== void 0) return t;
          d.value
        }),
        syncPosition: l
      }
    },
    render() {
      return z(Qr, {
        show: this.show,
        to: this.mergedTo,
        disabled: this.teleportDisabled
      }, {
        default: () => {
          var t, n;
          let r = z(`div`, {
            class: [`v-binder-follower-container`, this.containerClass],
            ref: `offsetContainerRef`
          }, [z(`div`, {
            class: `v-binder-follower-content`,
            ref: `followerRef`
          }, (n = (t = this.$slots).default) == null ? void 0 : n.call(t))]);
          return this.zindexable ? e(r, [
            [Kr, {
              enabled: this.mergedEnabled,
              zIndex: this.zIndex
            }]
          ]) : r
        }
      })
    }
  }),
  fi = [],
  pi = function() {
    return fi.some(function(e) {
      return e.activeTargets.length > 0
    })
  },
  mi = function() {
    return fi.some(function(e) {
      return e.skippedTargets.length > 0
    })
  },
  hi = `ResizeObserver loop completed with undelivered notifications.`,
  gi = function() {
    var e;
    typeof ErrorEvent == `function` ? e = new ErrorEvent(`error`, {
      message: hi
    }) : (e = document.createEvent(`Event`), e.initEvent(`error`, !1, !1), e.message = hi), window.dispatchEvent(e)
  },
  _i;
(function(e) {
  e.BORDER_BOX = `border-box`, e.CONTENT_BOX = `content-box`, e.DEVICE_PIXEL_CONTENT_BOX = `device-pixel-content-box`
})(_i || (_i = {}));
var vi = function(e) {
    return Object.freeze(e)
  },
  yi = function() {
    function e(e, t) {
      this.inlineSize = e, this.blockSize = t, vi(this)
    }
    return e
  }(),
  bi = function() {
    function e(e, t, n, r) {
      return this.x = e, this.y = t, this.width = n, this.height = r, this.top = this.y, this.left = this.x, this.bottom = this.top + this.height, this.right = this.left + this.width, vi(this)
    }
    return e.prototype.toJSON = function() {
      var e = this;
      return {
        x: e.x,
        y: e.y,
        top: e.top,
        right: e.right,
        bottom: e.bottom,
        left: e.left,
        width: e.width,
        height: e.height
      }
    }, e.fromRect = function(t) {
      return new e(t.x, t.y, t.width, t.height)
    }, e
  }(),
  xi = function(e) {
    return e instanceof SVGElement && `getBBox` in e
  },
  Si = function(e) {
    if (xi(e)) {
      var t = e.getBBox(),
        n = t.width,
        r = t.height;
      return !n && !r
    }
    var i = e,
      a = i.offsetWidth,
      o = i.offsetHeight;
    return !(a || o || e.getClientRects().length)
  },
  Ci = function(e) {
    var t;
    if (e instanceof Element) return !0;
    var n = (t = e == null ? void 0 : e.ownerDocument) == null ? void 0 : t.defaultView;
    return !!(n && e instanceof n.Element)
  },
  wi = function(e) {
    switch (e.tagName) {
      case `INPUT`:
        if (e.type !== `image`) break;
      case `VIDEO`:
      case `AUDIO`:
      case `EMBED`:
      case `OBJECT`:
      case `CANVAS`:
      case `IFRAME`:
      case `IMG`:
        return !0
    }
    return !1
  },
  Ti = typeof window < `u` ? window : {},
  Ei = new WeakMap,
  Di = /auto|scroll/,
  Oi = /^tb|vertical/,
  ki = /msie|trident/i.test(Ti.navigator && Ti.navigator.userAgent),
  Ai = function(e) {
    return parseFloat(e || `0`)
  },
  ji = function(e, t, n) {
    return e === void 0 && (e = 0), t === void 0 && (t = 0), n === void 0 && (n = !1), new yi((n ? t : e) || 0, (n ? e : t) || 0)
  },
  Mi = vi({
    devicePixelContentBoxSize: ji(),
    borderBoxSize: ji(),
    contentBoxSize: ji(),
    contentRect: new bi(0, 0, 0, 0)
  }),
  Ni = function(e, t) {
    if (t === void 0 && (t = !1), Ei.has(e) && !t) return Ei.get(e);
    if (Si(e)) return Ei.set(e, Mi), Mi;
    var n = getComputedStyle(e),
      r = xi(e) && e.ownerSVGElement && e.getBBox(),
      i = !ki && n.boxSizing === `border-box`,
      a = Oi.test(n.writingMode || ``),
      o = !r && Di.test(n.overflowY || ``),
      s = !r && Di.test(n.overflowX || ``),
      c = r ? 0 : Ai(n.paddingTop),
      l = r ? 0 : Ai(n.paddingRight),
      u = r ? 0 : Ai(n.paddingBottom),
      d = r ? 0 : Ai(n.paddingLeft),
      f = r ? 0 : Ai(n.borderTopWidth),
      p = r ? 0 : Ai(n.borderRightWidth),
      m = r ? 0 : Ai(n.borderBottomWidth),
      h = r ? 0 : Ai(n.borderLeftWidth),
      g = d + l,
      _ = c + u,
      v = h + p,
      y = f + m,
      b = s ? e.offsetHeight - y - e.clientHeight : 0,
      x = o ? e.offsetWidth - v - e.clientWidth : 0,
      S = i ? g + v : 0,
      C = i ? _ + y : 0,
      w = r ? r.width : Ai(n.width) - S - x,
      T = r ? r.height : Ai(n.height) - C - b,
      E = w + g + x + v,
      D = T + _ + b + y,
      O = vi({
        devicePixelContentBoxSize: ji(Math.round(w * devicePixelRatio), Math.round(T * devicePixelRatio), a),
        borderBoxSize: ji(E, D, a),
        contentBoxSize: ji(w, T, a),
        contentRect: new bi(d, c, w, T)
      });
    return Ei.set(e, O), O
  },
  Pi = function(e, t, n) {
    var r = Ni(e, n),
      i = r.borderBoxSize,
      a = r.contentBoxSize,
      o = r.devicePixelContentBoxSize;
    switch (t) {
      case _i.DEVICE_PIXEL_CONTENT_BOX:
        return o;
      case _i.BORDER_BOX:
        return i;
      default:
        return a
    }
  },
  Fi = function() {
    function e(e) {
      var t = Ni(e);
      this.target = e, this.contentRect = t.contentRect, this.borderBoxSize = vi([t.borderBoxSize]), this.contentBoxSize = vi([t.contentBoxSize]), this.devicePixelContentBoxSize = vi([t.devicePixelContentBoxSize])
    }
    return e
  }(),
  Ii = function(e) {
    if (Si(e)) return 1 / 0;
    for (var t = 0, n = e.parentNode; n;) t += 1, n = n.parentNode;
    return t
  },
  Li = function() {
    var e = 1 / 0,
      t = [];
    fi.forEach(function(n) {
      if (n.activeTargets.length !== 0) {
        var r = [];
        n.activeTargets.forEach(function(t) {
          var n = new Fi(t.target),
            i = Ii(t.target);
          r.push(n), t.lastReportedSize = Pi(t.target, t.observedBox), i < e && (e = i)
        }), t.push(function() {
          n.callback.call(n.observer, r, n.observer)
        }), n.activeTargets.splice(0, n.activeTargets.length)
      }
    });
    for (var n = 0, r = t; n < r.length; n++) {
      var i = r[n];
      i()
    }
    return e
  },
  Ri = function(e) {
    fi.forEach(function(t) {
      t.activeTargets.splice(0, t.activeTargets.length), t.skippedTargets.splice(0, t.skippedTargets.length), t.observationTargets.forEach(function(n) {
        n.isActive() && (Ii(n.target) > e ? t.activeTargets.push(n) : t.skippedTargets.push(n))
      })
    })
  },
  zi = function() {
    var e = 0;
    for (Ri(e); pi();) e = Li(), Ri(e);
    return mi() && gi(), e > 0
  },
  Bi, Vi = [],
  Hi = function() {
    return Vi.splice(0).forEach(function(e) {
      return e()
    })
  },
  Ui = function(e) {
    if (!Bi) {
      var t = 0,
        n = document.createTextNode(``);
      new MutationObserver(function() {
        return Hi()
      }).observe(n, {
        characterData: !0
      }), Bi = function() {
        n.textContent = `${t?t--:t++}`
      }
    }
    Vi.push(e), Bi()
  },
  Wi = function(e) {
    Ui(function() {
      requestAnimationFrame(e)
    })
  },
  Gi = 0,
  Ki = function() {
    return !!Gi
  },
  qi = 250,
  Ji = {
    attributes: !0,
    characterData: !0,
    childList: !0,
    subtree: !0
  },
  Yi = [`resize`, `load`, `transitionend`, `animationend`, `animationstart`, `animationiteration`, `keyup`, `keydown`, `mouseup`, `mousedown`, `mouseover`, `mouseout`, `blur`, `focus`],
  Xi = function(e) {
    return e === void 0 && (e = 0), Date.now() + e
  },
  Zi = !1,
  Qi = new(function() {
    function e() {
      var e = this;
      this.stopped = !0, this.listener = function() {
        return e.schedule()
      }
    }
    return e.prototype.run = function(e) {
      var t = this;
      if (e === void 0 && (e = qi), !Zi) {
        Zi = !0;
        var n = Xi(e);
        Wi(function() {
          var r = !1;
          try {
            r = zi()
          } finally {
            if (Zi = !1, e = n - Xi(), !Ki()) return;
            r ? t.run(1e3) : e > 0 ? t.run(e) : t.start()
          }
        })
      }
    }, e.prototype.schedule = function() {
      this.stop(), this.run()
    }, e.prototype.observe = function() {
      var e = this,
        t = function() {
          return e.observer && e.observer.observe(document.body, Ji)
        };
      document.body ? t() : Ti.addEventListener(`DOMContentLoaded`, t)
    }, e.prototype.start = function() {
      var e = this;
      this.stopped && (this.stopped = !1, this.observer = new MutationObserver(this.listener), this.observe(), Yi.forEach(function(t) {
        return Ti.addEventListener(t, e.listener, !0)
      }))
    }, e.prototype.stop = function() {
      var e = this;
      this.stopped || (this.observer && this.observer.disconnect(), Yi.forEach(function(t) {
        return Ti.removeEventListener(t, e.listener, !0)
      }), this.stopped = !0)
    }, e
  }()),
  $i = function(e) {
    !Gi && e > 0 && Qi.start(), Gi += e, !Gi && Qi.stop()
  },
  ea = function(e) {
    return !xi(e) && !wi(e) && getComputedStyle(e).display === `inline`
  },
  ta = function() {
    function e(e, t) {
      this.target = e, this.observedBox = t || _i.CONTENT_BOX, this.lastReportedSize = {
        inlineSize: 0,
        blockSize: 0
      }
    }
    return e.prototype.isActive = function() {
      var e = Pi(this.target, this.observedBox, !0);
      return ea(this.target) && (this.lastReportedSize = e), this.lastReportedSize.inlineSize !== e.inlineSize || this.lastReportedSize.blockSize !== e.blockSize
    }, e
  }(),
  na = function() {
    function e(e, t) {
      this.activeTargets = [], this.skippedTargets = [], this.observationTargets = [], this.observer = e, this.callback = t
    }
    return e
  }(),
  ra = new WeakMap,
  ia = function(e, t) {
    for (var n = 0; n < e.length; n += 1)
      if (e[n].target === t) return n;
    return -1
  },
  aa = function() {
    function e() {}
    return e.connect = function(e, t) {
      var n = new na(e, t);
      ra.set(e, n)
    }, e.observe = function(e, t, n) {
      var r = ra.get(e),
        i = r.observationTargets.length === 0;
      ia(r.observationTargets, t) < 0 && (i && fi.push(r), r.observationTargets.push(new ta(t, n && n.box)), $i(1), Qi.schedule())
    }, e.unobserve = function(e, t) {
      var n = ra.get(e),
        r = ia(n.observationTargets, t),
        i = n.observationTargets.length === 1;
      r >= 0 && (i && fi.splice(fi.indexOf(n), 1), n.observationTargets.splice(r, 1), $i(-1))
    }, e.disconnect = function(e) {
      var t = this,
        n = ra.get(e);
      n.observationTargets.slice().forEach(function(n) {
        return t.unobserve(e, n.target)
      }), n.activeTargets.splice(0, n.activeTargets.length)
    }, e
  }(),
  oa = function() {
    function e(e) {
      if (arguments.length === 0) throw TypeError(`Failed to construct 'ResizeObserver': 1 argument required, but only 0 present.`);
      if (typeof e != `function`) throw TypeError(`Failed to construct 'ResizeObserver': The callback provided as parameter 1 is not a function.`);
      aa.connect(this, e)
    }
    return e.prototype.observe = function(e, t) {
      if (arguments.length === 0) throw TypeError(`Failed to execute 'observe' on 'ResizeObserver': 1 argument required, but only 0 present.`);
      if (!Ci(e)) throw TypeError(`Failed to execute 'observe' on 'ResizeObserver': parameter 1 is not of type 'Element`);
      aa.observe(this, e, t)
    }, e.prototype.unobserve = function(e) {
      if (arguments.length === 0) throw TypeError(`Failed to execute 'unobserve' on 'ResizeObserver': 1 argument required, but only 0 present.`);
      if (!Ci(e)) throw TypeError(`Failed to execute 'unobserve' on 'ResizeObserver': parameter 1 is not of type 'Element`);
      aa.unobserve(this, e)
    }, e.prototype.disconnect = function() {
      aa.disconnect(this)
    }, e.toString = function() {
      return `function ResizeObserver () { [polyfill code] }`
    }, e
  }(),
  sa = new class {
    constructor() {
      this.handleResize = this.handleResize.bind(this), this.observer = new(typeof window < `u` && window.ResizeObserver || oa)(this.handleResize), this.elHandlersMap = new Map
    }
    handleResize(e) {
      for (let t of e) {
        let e = this.elHandlersMap.get(t.target);
        e !== void 0 && e(t)
      }
    }
    registerHandler(e, t) {
      this.elHandlersMap.set(e, t), this.observer.observe(e)
    }
    unregisterHandler(e) {
      this.elHandlersMap.has(e) && (this.elHandlersMap.delete(e), this.observer.unobserve(e))
    }
  },
  ca = l({
    name: `ResizeObserver`,
    props: {
      onResize: Function
    },
    setup(e) {
      let t = !1,
        n = s().proxy;

      function r(t) {
        let {
          onResize: n
        } = e;
        n !== void 0 && n(t)
      }
      j(() => {
        let e = n.$el;
        if (e === void 0) {
          qr(`resize-observer`, `$el does not exist.`);
          return
        }
        if (e.nextElementSibling !== e.nextSibling && e.nodeType === 3 && e.nodeValue !== ``) {
          qr(`resize-observer`, `$el can not be observed (it may be a text node).`);
          return
        }
        e.nextElementSibling !== null && (sa.registerHandler(e.nextElementSibling, r), t = !0)
      }), g(() => {
        t && sa.unregisterHandler(n.$el.nextElementSibling)
      })
    },
    render() {
      return ae(this.$slots, `default`)
    }
  });

function la(e) {
  return e instanceof HTMLElement
}

function ua(e) {
  for (let t = 0; t < e.childNodes.length; t++) {
    let n = e.childNodes[t];
    if (la(n) && (fa(n) || ua(n))) return !0
  }
  return !1
}

function da(e) {
  for (let t = e.childNodes.length - 1; t >= 0; t--) {
    let n = e.childNodes[t];
    if (la(n) && (fa(n) || da(n))) return !0
  }
  return !1
}

function fa(e) {
  if (!pa(e)) return !1;
  try {
    e.focus({
      preventScroll: !0
    })
  } catch {}
  return document.activeElement === e
}

function pa(e) {
  if (e.tabIndex > 0 || e.tabIndex === 0 && e.getAttribute(`tabIndex`) !== null) return !0;
  if (e.getAttribute(`disabled`)) return !1;
  switch (e.nodeName) {
    case `A`:
      return !!e.href && e.rel !== `ignore`;
    case `INPUT`:
      return e.type !== `hidden` && e.type !== `file`;
    case `SELECT`:
    case `TEXTAREA`:
      return !0;
    default:
      return !1
  }
}
var ma = [],
  ha = l({
    name: `FocusTrap`,
    props: {
      disabled: Boolean,
      active: Boolean,
      autoFocus: {
        type: Boolean,
        default: !0
      },
      onEsc: Function,
      initialFocusTo: [String, Function],
      finalFocusTo: [String, Function],
      returnFocusOnDeactivated: {
        type: Boolean,
        default: !0
      }
    },
    setup(e) {
      let t = Xn(),
        n = U(null),
        r = U(null),
        i = !1,
        a = !1,
        o = typeof document > `u` ? null : document.activeElement;

      function s() {
        return ma[ma.length - 1] === t
      }

      function c(t) {
        var n;
        t.code === `Escape` && s() && ((n = e.onEsc) == null || n.call(e, t))
      }
      j(() => {
        P(() => e.active, e => {
          e ? (d(), W(`keydown`, document, c)) : (G(`keydown`, document, c), i && f())
        }, {
          immediate: !0
        })
      }), g(() => {
        G(`keydown`, document, c), i && f()
      });

      function l(e) {
        if (!a && s()) {
          let t = u();
          if (t === null || t.contains(Gn(e))) return;
          p(`first`)
        }
      }

      function u() {
        let e = n.value;
        if (e === null) return null;
        let t = e;
        for (; t = t.nextSibling, !(t === null || t instanceof Element && t.tagName === `DIV`););
        return t
      }

      function d() {
        var n;
        if (!e.disabled) {
          if (ma.push(t), e.autoFocus) {
            let {
              initialFocusTo: t
            } = e;
            t === void 0 ? p(`first`) : (n = Zr(t)) == null || n.focus({
              preventScroll: !0
            })
          }
          i = !0, document.addEventListener(`focus`, l, !0)
        }
      }

      function f() {
        var n;
        if (e.disabled || (document.removeEventListener(`focus`, l, !0), ma = ma.filter(e => e !== t), s())) return;
        let {
          finalFocusTo: r
        } = e;
        r === void 0 ? e.returnFocusOnDeactivated && o instanceof HTMLElement && (a = !0, o.focus({
          preventScroll: !0
        }), a = !1) : (n = Zr(r)) == null || n.focus({
          preventScroll: !0
        })
      }

      function p(t) {
        if (s() && e.active) {
          let e = n.value,
            i = r.value;
          if (e !== null && i !== null) {
            let n = u();
            if (n == null || n === i) {
              a = !0, e.focus({
                preventScroll: !0
              }), a = !1;
              return
            }
            a = !0;
            let r = t === `first` ? ua(n) : da(n);
            a = !1, r || (a = !0, e.focus({
              preventScroll: !0
            }), a = !1)
          }
        }
      }

      function m(e) {
        if (a) return;
        let t = u();
        t !== null && (e.relatedTarget !== null && t.contains(e.relatedTarget) ? p(`last`) : p(`first`))
      }

      function h(e) {
        a || (e.relatedTarget !== null && e.relatedTarget === n.value ? p(`last`) : p(`first`))
      }
      return {
        focusableStartRef: n,
        focusableEndRef: r,
        focusableStyle: `position: absolute; height: 0; width: 0;`,
        handleStartFocus: m,
        handleEndFocus: h
      }
    },
    render() {
      let {
        default: e
      } = this.$slots;
      if (e === void 0) return null;
      if (this.disabled) return e();
      let {
        active: t,
        focusableStyle: n
      } = this;
      return z(L, null, [z(`div`, {
        "aria-hidden": `true`,
        tabindex: t ? `0` : `-1`,
        ref: `focusableStartRef`,
        style: n,
        onFocus: this.handleStartFocus
      }), e(), z(`div`, {
        "aria-hidden": `true`,
        style: n,
        ref: `focusableEndRef`,
        tabindex: t ? `0` : `-1`,
        onFocus: this.handleEndFocus
      })])
    }
  });

function ga(e) {
  return e.replace(/#|\(|\)|,|\s|\./g, `_`)
}

function _a(e) {
  let {
    left: t,
    right: n,
    top: r,
    bottom: i
  } = Jn(e);
  return `${r} ${t} ${i} ${n}`
}
var va;

function ya() {
  return va === void 0 && (va = navigator.userAgent.includes(`Node.js`) || navigator.userAgent.includes(`jsdom`)), va
}

function ba(e, ...t) {
  if (Array.isArray(e)) e.forEach(e => ba(e, ...t));
  else return e(...t)
}

function xa(e, t = !0, n = []) {
  return e.forEach(e => {
    if (e !== null) {
      if (typeof e != `object`) {
        (typeof e == `string` || typeof e == `number`) && n.push(et(String(e)));
        return
      }
      if (Array.isArray(e)) {
        xa(e, t, n);
        return
      }
      if (e.type === L) {
        if (e.children === null) return;
        Array.isArray(e.children) && xa(e.children, t, n)
      } else {
        if (e.type === _e && t) return;
        n.push(e)
      }
    }
  }), n
}

function Sa(e, t = `default`, n = void 0) {
  let i = e[t];
  if (!i) return r(`getFirstSlotVNode`, `slot[${t}] is empty`), null;
  let a = xa(i(n));
  return a.length === 1 ? a[0] : (r(`getFirstSlotVNode`, `slot[${t}] should have exactly one child`), null)
}

function Ca(e, t, n) {
  if (!t) return null;
  let i = xa(t(n));
  return i.length === 1 ? i[0] : (r(`getFirstSlotVNode`, `slot[${e}] should have exactly one child`), null)
}

function wa(e, t = [], n) {
  let r = {};
  return t.forEach(t => {
    r[t] = e[t]
  }), Object.assign(r, n)
}

function Ta(e) {
  return e.some(e => ke(e) ? !(e.type === _e || e.type === L && !Ta(e.children)) : !0) ? e : null
}

function Ea(e, t) {
  return e && Ta(e()) || t()
}

function Da(e, t, n) {
  return e && Ta(e(t)) || n(t)
}

function Oa(e, t) {
  return t(e && Ta(e()) || null)
}

function ka(e) {
  return !(e && Ta(e()))
}
var Aa = l({
    render() {
      var e, t;
      return (t = (e = this.$slots).default) == null ? void 0 : t.call(e)
    }
  }),
  ja = V(`n-form-item`);

function Ma(e, {
  defaultSize: t = `medium`,
  mergedSize: r,
  mergedDisabled: i
} = {}) {
  let a = n(ja, null);
  m(ja, null);
  let o = F(r ? () => r(a) : () => {
      let {
        size: n
      } = e;
      if (n) return n;
      if (a) {
        let {
          mergedSize: e
        } = a;
        if (e.value !== void 0) return e.value
      }
      return t
    }),
    s = F(i ? () => i(a) : () => {
      let {
        disabled: t
      } = e;
      return t === void 0 ? a ? a.disabled.value : !1 : t
    }),
    c = F(() => {
      let {
        status: t
      } = e;
      return t || (a == null ? void 0 : a.mergedValidationStatus.value)
    });
  return g(() => {
    a && a.restoreValidation()
  }), {
    mergedSizeRef: o,
    mergedDisabledRef: s,
    mergedStatusRef: c,
    nTriggerFormBlur() {
      a && a.handleContentBlur()
    },
    nTriggerFormChange() {
      a && a.handleContentChange()
    },
    nTriggerFormFocus() {
      a && a.handleContentFocus()
    },
    nTriggerFormInput() {
      a && a.handleContentInput()
    }
  }
}

function Na(e, t, r) {
  if (!t) return;
  let i = qe(),
    a = F(() => {
      let {
        value: n
      } = t;
      if (!n) return;
      let r = n[e];
      if (r) return r
    }),
    o = n(A, null),
    s = () => {
      ce(() => {
        let {
          value: t
        } = r, n = `${t}${e}Rtl`;
        if (Jr(n, i)) return;
        let {
          value: s
        } = a;
        s && s.style.mount({
          id: n,
          head: !0,
          anchorMetaName: D,
          props: {
            bPrefix: t ? `.${t}-` : void 0
          },
          ssr: i,
          parent: o == null ? void 0 : o.styleMountTarget
        })
      })
    };
  return i ? s() : d(s), a
}

function Pa(e, t, r) {
  if (!t) return;
  let i = qe(),
    a = n(A, null),
    o = () => {
      let n = r.value;
      t.mount({
        id: n === void 0 ? e : n + e,
        head: !0,
        anchorMetaName: D,
        props: {
          bPrefix: n ? `.${n}-` : void 0
        },
        ssr: i,
        parent: a == null ? void 0 : a.styleMountTarget
      }), a != null && a.preflightStyleDisabled || x.mount({
        id: `n-global`,
        head: !0,
        anchorMetaName: D,
        ssr: i,
        parent: a == null ? void 0 : a.styleMountTarget
      })
    };
  i ? o() : d(o)
}
var Fa = `[object Symbol]`;

function Ia(e) {
  return typeof e == `symbol` || o(e) && T(e) == Fa
}
var La = /\.|\[(?:[^[\]]*|(["'])(?:(?!\1)[^\\]|\\.)*?\1)\]/,
  Ra = /^\w*$/;

function za(e, t) {
  if (R(e)) return !1;
  var n = typeof e;
  return n == `number` || n == `symbol` || n == `boolean` || e == null || Ia(e) ? !0 : Ra.test(e) || !La.test(e) || t != null && e in Object(t)
}
var Ba = `Expected a function`;

function Va(e, t) {
  if (typeof e != `function` || t != null && typeof t != `function`) throw TypeError(Ba);
  var n = function() {
    var r = arguments,
      i = t ? t.apply(this, r) : r[0],
      a = n.cache;
    if (a.has(i)) return a.get(i);
    var o = e.apply(this, r);
    return n.cache = a.set(i, o) || a, o
  };
  return n.cache = new(Va.Cache || tt), n
}
Va.Cache = tt;
var Ha = 500;

function Ua(e) {
  var t = Va(e, function(e) {
      return n.size === Ha && n.clear(), e
    }),
    n = t.cache;
  return t
}
var Wa = /[^.[\]]+|\[(?:(-?\d+(?:\.\d+)?)|(["'])((?:(?!\2)[^\\]|\\.)*?)\2)\]|(?=(?:\.|\[\])(?:\.|\[\]|$))/g,
  Ga = /\\(\\)?/g,
  Ka = Ua(function(e) {
    var t = [];
    return e.charCodeAt(0) === 46 && t.push(``), e.replace(Wa, function(e, n, r, i) {
      t.push(r ? i.replace(Ga, `$1`) : n || e)
    }), t
  });

function qa(e, t) {
  for (var n = -1, r = e == null ? 0 : e.length, i = Array(r); ++n < r;) i[n] = t(e[n], n, e);
  return i
}
var Ja = 1 / 0,
  Ya = Ae ? Ae.prototype : void 0,
  Xa = Ya ? Ya.toString : void 0;

function Za(e) {
  if (typeof e == `string`) return e;
  if (R(e)) return qa(e, Za) + ``;
  if (Ia(e)) return Xa ? Xa.call(e) : ``;
  var t = e + ``;
  return t == `0` && 1 / e == -Ja ? `-0` : t
}

function Qa(e) {
  return e == null ? `` : Za(e)
}

function $a(e, t) {
  return R(e) ? e : za(e, t) ? [e] : Ka(Qa(e))
}
var eo = 1 / 0;

function to(e) {
  if (typeof e == `string` || Ia(e)) return e;
  var t = e + ``;
  return t == `0` && 1 / e == -eo ? `-0` : t
}

function no(e, t) {
  t = $a(t, e);
  for (var n = 0, r = t.length; e != null && n < r;) e = e[to(t[n++])];
  return n && n == r ? e : void 0
}

function ro(e, t, n) {
  var r = e == null ? void 0 : no(e, t);
  return r === void 0 ? n : r
}
var io = `__lodash_hash_undefined__`;

function ao(e) {
  return this.__data__.set(e, io), this
}

function oo(e) {
  return this.__data__.has(e)
}

function so(e) {
  var t = -1,
    n = e == null ? 0 : e.length;
  for (this.__data__ = new tt; ++t < n;) this.add(e[t])
}
so.prototype.add = so.prototype.push = ao, so.prototype.has = oo;

function co(e, t) {
  for (var n = -1, r = e == null ? 0 : e.length; ++n < r;)
    if (t(e[n], n, e)) return !0;
  return !1
}

function lo(e, t) {
  return e.has(t)
}
var uo = 1,
  fo = 2;

function po(e, t, n, r, i, a) {
  var o = n & uo,
    s = e.length,
    c = t.length;
  if (s != c && !(o && c > s)) return !1;
  var l = a.get(e),
    u = a.get(t);
  if (l && u) return l == t && u == e;
  var d = -1,
    f = !0,
    p = n & fo ? new so : void 0;
  for (a.set(e, t), a.set(t, e); ++d < s;) {
    var m = e[d],
      h = t[d];
    if (r) var g = o ? r(h, m, d, t, e, a) : r(m, h, d, e, t, a);
    if (g !== void 0) {
      if (g) continue;
      f = !1;
      break
    }
    if (p) {
      if (!co(t, function(e, t) {
          if (!lo(p, t) && (m === e || i(m, e, n, r, a))) return p.push(t)
        })) {
        f = !1;
        break
      }
    } else if (!(m === h || i(m, h, n, r, a))) {
      f = !1;
      break
    }
  }
  return a.delete(e), a.delete(t), f
}

function mo(e) {
  var t = -1,
    n = Array(e.size);
  return e.forEach(function(e, r) {
    n[++t] = [r, e]
  }), n
}

function ho(e) {
  var t = -1,
    n = Array(e.size);
  return e.forEach(function(e) {
    n[++t] = e
  }), n
}
var go = 1,
  _o = 2,
  vo = `[object Boolean]`,
  yo = `[object Date]`,
  bo = `[object Error]`,
  xo = `[object Map]`,
  So = `[object Number]`,
  Co = `[object RegExp]`,
  wo = `[object Set]`,
  To = `[object String]`,
  Eo = `[object Symbol]`,
  Do = `[object ArrayBuffer]`,
  Oo = `[object DataView]`,
  ko = Ae ? Ae.prototype : void 0,
  Ao = ko ? ko.valueOf : void 0;

function jo(e, t, n, r, i, o, s) {
  switch (n) {
    case Oo:
      if (e.byteLength != t.byteLength || e.byteOffset != t.byteOffset) return !1;
      e = e.buffer, t = t.buffer;
    case Do:
      return !(e.byteLength != t.byteLength || !o(new rt(e), new rt(t)));
    case vo:
    case yo:
    case So:
      return a(+e, +t);
    case bo:
      return e.name == t.name && e.message == t.message;
    case Co:
    case To:
      return e == t + ``;
    case xo:
      var c = mo;
    case wo:
      var l = r & go;
      if (c || (c = ho), e.size != t.size && !l) return !1;
      var u = s.get(e);
      if (u) return u == t;
      r |= _o, s.set(e, t);
      var d = po(c(e), c(t), r, i, o, s);
      return s.delete(e), d;
    case Eo:
      if (Ao) return Ao.call(e) == Ao.call(t)
  }
  return !1
}

function Mo(e, t) {
  for (var n = -1, r = t.length, i = e.length; ++n < r;) e[i + n] = t[n];
  return e
}

function No(e, t, n) {
  var r = t(e);
  return R(e) ? r : Mo(r, n(e))
}

function Po(e, t) {
  for (var n = -1, r = e == null ? 0 : e.length, i = 0, a = []; ++n < r;) {
    var o = e[n];
    t(o, n, e) && (a[i++] = o)
  }
  return a
}

function Fo() {
  return []
}
var Io = Object.prototype.propertyIsEnumerable,
  Lo = Object.getOwnPropertySymbols,
  Ro = Lo ? function(e) {
    return e == null ? [] : (e = Object(e), Po(Lo(e), function(t) {
      return Io.call(e, t)
    }))
  } : Fo,
  zo = he(Object.keys, Object),
  Bo = Object.prototype.hasOwnProperty;

function Vo(e) {
  if (!ye(e)) return zo(e);
  var t = [];
  for (var n in Object(e)) Bo.call(e, n) && n != `constructor` && t.push(n);
  return t
}

function Ho(e) {
  return Je(e) ? Be(e) : Vo(e)
}

function Uo(e) {
  return No(e, Ho, Ro)
}
var Wo = 1,
  Go = Object.prototype.hasOwnProperty;

function Ko(e, t, n, r, i, a) {
  var o = n & Wo,
    s = Uo(e),
    c = s.length;
  if (c != Uo(t).length && !o) return !1;
  for (var l = c; l--;) {
    var u = s[l];
    if (!(o ? u in t : Go.call(t, u))) return !1
  }
  var d = a.get(e),
    f = a.get(t);
  if (d && f) return d == t && f == e;
  var p = !0;
  a.set(e, t), a.set(t, e);
  for (var m = o; ++l < c;) {
    u = s[l];
    var h = e[u],
      g = t[u];
    if (r) var _ = o ? r(g, h, u, t, e, a) : r(h, g, u, e, t, a);
    if (!(_ === void 0 ? h === g || i(h, g, n, r, a) : _)) {
      p = !1;
      break
    }
    m || (m = u == `constructor`)
  }
  if (p && !m) {
    var v = e.constructor,
      y = t.constructor;
    v != y && `constructor` in e && `constructor` in t && !(typeof v == `function` && v instanceof v && typeof y == `function` && y instanceof y) && (p = !1)
  }
  return a.delete(e), a.delete(t), p
}
var qo = $e(t, `DataView`),
  Jo = $e(t, `Promise`),
  Yo = $e(t, `Set`),
  Xo = $e(t, `WeakMap`),
  Zo = `[object Map]`,
  Qo = `[object Object]`,
  $o = `[object Promise]`,
  es = `[object Set]`,
  ts = `[object WeakMap]`,
  ns = `[object DataView]`,
  rs = M(qo),
  is = M(ee),
  as = M(Jo),
  os = M(Yo),
  ss = M(Xo),
  cs = T;
(qo && cs(new qo(new ArrayBuffer(1))) != ns || ee && cs(new ee) != Zo || Jo && cs(Jo.resolve()) != $o || Yo && cs(new Yo) != es || Xo && cs(new Xo) != ts) && (cs = function(e) {
  var t = T(e),
    n = t == Qo ? e.constructor : void 0,
    r = n ? M(n) : ``;
  if (r) switch (r) {
    case rs:
      return ns;
    case is:
      return Zo;
    case as:
      return $o;
    case os:
      return es;
    case ss:
      return ts
  }
  return t
});
var ls = cs,
  us = 1,
  ds = `[object Arguments]`,
  fs = `[object Array]`,
  ps = `[object Object]`,
  ms = Object.prototype.hasOwnProperty;

function hs(e, t, n, r, i, a) {
  var o = R(e),
    s = R(t),
    c = o ? fs : ls(e),
    l = s ? fs : ls(t);
  c = c == ds ? ps : c, l = l == ds ? ps : l;
  var u = c == ps,
    d = l == ps,
    f = c == l;
  if (f && le(e)) {
    if (!le(t)) return !1;
    o = !0, u = !1
  }
  if (f && !u) return a || (a = new I), o || Pe(e) ? po(e, t, n, r, i, a) : jo(e, t, c, n, r, i, a);
  if (!(n & us)) {
    var p = u && ms.call(e, `__wrapped__`),
      m = d && ms.call(t, `__wrapped__`);
    if (p || m) {
      var h = p ? e.value() : e,
        g = m ? t.value() : t;
      return a || (a = new I), i(h, g, n, r, a)
    }
  }
  return f ? (a || (a = new I), Ko(e, t, n, r, i, a)) : !1
}

function gs(e, t, n, r, i) {
  return e === t ? !0 : e == null || t == null || !o(e) && !o(t) ? e !== e && t !== t : hs(e, t, n, r, gs, i)
}
var _s = 1,
  vs = 2;

function ys(e, t, n, r) {
  var i = n.length,
    a = i,
    o = !r;
  if (e == null) return !a;
  for (e = Object(e); i--;) {
    var s = n[i];
    if (o && s[2] ? s[1] !== e[s[0]] : !(s[0] in e)) return !1
  }
  for (; ++i < a;) {
    s = n[i];
    var c = s[0],
      l = e[c],
      u = s[1];
    if (o && s[2]) {
      if (l === void 0 && !(c in e)) return !1
    } else {
      var d = new I;
      if (r) var f = r(l, u, c, e, t, d);
      if (!(f === void 0 ? gs(u, l, _s | vs, r, d) : f)) return !1
    }
  }
  return !0
}

function bs(e) {
  return e === e && !c(e)
}

function xs(e) {
  for (var t = Ho(e), n = t.length; n--;) {
    var r = t[n],
      i = e[r];
    t[n] = [r, i, bs(i)]
  }
  return t
}

function Ss(e, t) {
  return function(n) {
    return n == null ? !1 : n[e] === t && (t !== void 0 || e in Object(n))
  }
}

function Cs(e) {
  var t = xs(e);
  return t.length == 1 && t[0][2] ? Ss(t[0][0], t[0][1]) : function(n) {
    return n === e || ys(n, e, t)
  }
}

function ws(e, t) {
  return e != null && t in Object(e)
}

function Ts(e, t, n) {
  t = $a(t, e);
  for (var r = -1, i = t.length, a = !1; ++r < i;) {
    var o = to(t[r]);
    if (!(a = e != null && n(e, o))) break;
    e = e[o]
  }
  return a || ++r != i ? a : (i = e == null ? 0 : e.length, !!i && we(i) && xe(o, i) && (R(e) || Ze(e)))
}

function Es(e, t) {
  return e != null && Ts(e, t, ws)
}
var Ds = 1,
  Os = 2;

function ks(e, t) {
  return za(e) && bs(t) ? Ss(to(e), t) : function(n) {
    var r = ro(n, e);
    return r === void 0 && r === t ? Es(n, e) : gs(t, r, Ds | Os)
  }
}

function As(e) {
  return function(t) {
    return t == null ? void 0 : t[e]
  }
}

function js(e) {
  return function(t) {
    return no(t, e)
  }
}

function Ms(e) {
  return za(e) ? As(to(e)) : js(e)
}

function Ns(e) {
  return typeof e == `function` ? e : e == null ? je : typeof e == `object` ? R(e) ? ks(e[0], e[1]) : Cs(e) : Ms(e)
}

function Ps(e, t) {
  return e && pe(e, t, Ho)
}

function Fs(e, t) {
  return function(n, r) {
    if (n == null) return n;
    if (!Je(n)) return e(n, r);
    for (var i = n.length, a = t ? i : -1, o = Object(n);
      (t ? a-- : ++a < i) && r(o[a], a, o) !== !1;);
    return n
  }
}
var Is = Fs(Ps);

function Ls(e, t) {
  var n = -1,
    r = Je(e) ? Array(e.length) : [];
  return Is(e, function(e, i, a) {
    r[++n] = t(e, i, a)
  }), r
}

function Rs(e, t) {
  return (R(e) ? qa : Ls)(e, Ns(t, 3))
}
var zs = l({
    name: `BaseIconSwitchTransition`,
    setup(e, {
      slots: t
    }) {
      let n = pr();
      return () => z(Ot, {
        name: `icon-switch-transition`,
        appear: n.value
      }, t)
    }
  }),
  {
    cubicBezierEaseInOut: Bs
  } = C;

function Vs({
  originalTransform: e = ``,
  left: t = 0,
  top: n = 0,
  transition: r = `all .3s ${Bs} !important`
} = {}) {
  return [p(`&.icon-switch-transition-enter-from, &.icon-switch-transition-leave-to`, {
    transform: `${e} scale(0.75)`,
    left: t,
    top: n,
    opacity: 0
  }), p(`&.icon-switch-transition-enter-to, &.icon-switch-transition-leave-from`, {
    transform: `scale(1) ${e}`,
    left: t,
    top: n,
    opacity: 1
  }), p(`&.icon-switch-transition-enter-active, &.icon-switch-transition-leave-active`, {
    transformOrigin: `center`,
    position: `absolute`,
    left: t,
    top: n,
    transition: r
  })]
}
var Hs = l({
    name: `FadeInExpandTransition`,
    props: {
      appear: Boolean,
      group: Boolean,
      mode: String,
      onLeave: Function,
      onAfterLeave: Function,
      onAfterEnter: Function,
      width: Boolean,
      reverse: Boolean
    },
    setup(e, {
      slots: t
    }) {
      function n(t) {
        e.width ? t.style.maxWidth = `${t.offsetWidth}px` : t.style.maxHeight = `${t.offsetHeight}px`, t.offsetWidth
      }

      function r(t) {
        e.width ? t.style.maxWidth = `0` : t.style.maxHeight = `0`, t.offsetWidth;
        let {
          onLeave: n
        } = e;
        n && n()
      }

      function i(t) {
        e.width ? t.style.maxWidth = `` : t.style.maxHeight = ``;
        let {
          onAfterLeave: n
        } = e;
        n && n()
      }

      function a(t) {
        if (t.style.transition = `none`, e.width) {
          let e = t.offsetWidth;
          t.style.maxWidth = `0`, t.offsetWidth, t.style.transition = ``, t.style.maxWidth = `${e}px`
        } else if (e.reverse) t.style.maxHeight = `${t.offsetHeight}px`, t.offsetHeight, t.style.transition = ``, t.style.maxHeight = `0`;
        else {
          let e = t.offsetHeight;
          t.style.maxHeight = `0`, t.offsetWidth, t.style.transition = ``, t.style.maxHeight = `${e}px`
        }
        t.offsetWidth
      }

      function o(t) {
        var n;
        e.width ? t.style.maxWidth = `` : e.reverse || (t.style.maxHeight = ``), (n = e.onAfterEnter) == null || n.call(e)
      }
      return () => {
        let {
          group: s,
          width: c,
          appear: l,
          mode: u
        } = e, d = s ? En : Ot, f = {
          name: c ? `fade-in-width-expand-transition` : `fade-in-height-expand-transition`,
          appear: l,
          onEnter: a,
          onAfterEnter: o,
          onBeforeLeave: n,
          onLeave: r,
          onAfterLeave: i
        };
        return s || (f.mode = u), z(d, f, t)
      }
    }
  }),
  Us = p([p(`@keyframes rotator`, `
 0% {
 -webkit-transform: rotate(0deg);
 transform: rotate(0deg);
 }
 100% {
 -webkit-transform: rotate(360deg);
 transform: rotate(360deg);
 }`), N(`base-loading`, `
 position: relative;
 line-height: 0;
 width: 1em;
 height: 1em;
 `, [f(`transition-wrapper`, `
 position: absolute;
 width: 100%;
 height: 100%;
 `, [Vs()]), f(`placeholder`, `
 position: absolute;
 left: 50%;
 top: 50%;
 transform: translateX(-50%) translateY(-50%);
 `, [Vs({
    left: `50%`,
    top: `50%`,
    originalTransform: `translateX(-50%) translateY(-50%)`
  })]), f(`container`, `
 animation: rotator 3s linear infinite both;
 `, [f(`icon`, `
 height: 1em;
 width: 1em;
 `)])])]),
  Ws = `1.6s`,
  Gs = {
    strokeWidth: {
      type: Number,
      default: 28
    },
    stroke: {
      type: String,
      default: void 0
    },
    scale: {
      type: Number,
      default: 1
    },
    radius: {
      type: Number,
      default: 100
    }
  },
  Ks = l({
    name: `BaseLoading`,
    props: Object.assign({
      clsPrefix: {
        type: String,
        required: !0
      },
      show: {
        type: Boolean,
        default: !0
      }
    }, Gs),
    setup(e) {
      Pa(`-base-loading`, Us, Ke(e, `clsPrefix`))
    },
    render() {
      let {
        clsPrefix: e,
        radius: t,
        strokeWidth: n,
        stroke: r,
        scale: i
      } = this, a = t / i;
      return z(`div`, {
        class: `${e}-base-loading`,
        role: `img`,
        "aria-label": `loading`
      }, z(zs, null, {
        default: () => this.show ? z(`div`, {
          key: `icon`,
          class: `${e}-base-loading__transition-wrapper`
        }, z(`div`, {
          class: `${e}-base-loading__container`
        }, z(`svg`, {
          class: `${e}-base-loading__icon`,
          viewBox: `0 0 ${2*a} ${2*a}`,
          xmlns: `http://www.w3.org/2000/svg`,
          style: {
            color: r
          }
        }, z(`g`, null, z(`animateTransform`, {
          attributeName: `transform`,
          type: `rotate`,
          values: `0 ${a} ${a};270 ${a} ${a}`,
          begin: `0s`,
          dur: Ws,
          fill: `freeze`,
          repeatCount: `indefinite`
        }), z(`circle`, {
          class: `${e}-base-loading__icon`,
          fill: `none`,
          stroke: `currentColor`,
          "stroke-width": n,
          "stroke-linecap": `round`,
          cx: a,
          cy: a,
          r: t - n / 2,
          "stroke-dasharray": 5.67 * t,
          "stroke-dashoffset": 18.48 * t
        }, z(`animateTransform`, {
          attributeName: `transform`,
          type: `rotate`,
          values: `0 ${a} ${a};135 ${a} ${a};450 ${a} ${a}`,
          begin: `0s`,
          dur: Ws,
          fill: `freeze`,
          repeatCount: `indefinite`
        }), z(`animate`, {
          attributeName: `stroke-dashoffset`,
          values: `${5.67*t};${1.42*t};${5.67*t}`,
          begin: `0s`,
          dur: Ws,
          fill: `freeze`,
          repeatCount: `indefinite`
        })))))) : z(`div`, {
          key: `placeholder`,
          class: `${e}-base-loading__placeholder`
        }, this.$slots)
      }))
    }
  }),
  qs = {
    railInsetHorizontalBottom: `auto 2px 4px 2px`,
    railInsetHorizontalTop: `4px 2px auto 2px`,
    railInsetVerticalRight: `2px 4px 2px auto`,
    railInsetVerticalLeft: `2px auto 2px 4px`,
    railColor: `transparent`
  };

function Js(e) {
  let {
    scrollbarColor: t,
    scrollbarColorHover: n,
    scrollbarHeight: r,
    scrollbarWidth: i,
    scrollbarBorderRadius: a
  } = e;
  return Object.assign(Object.assign({}, qs), {
    height: r,
    width: i,
    borderRadius: a,
    color: t,
    colorHover: n
  })
}
var Ys = {
    name: `Scrollbar`,
    common: De,
    self: Js
  },
  {
    cubicBezierEaseInOut: Xs
  } = C;

function Zs({
  name: e = `fade-in`,
  enterDuration: t = `0.2s`,
  leaveDuration: n = `0.2s`,
  enterCubicBezier: r = Xs,
  leaveCubicBezier: i = Xs
} = {}) {
  return [p(`&.${e}-transition-enter-active`, {
    transition: `all ${t} ${r}!important`
  }), p(`&.${e}-transition-leave-active`, {
    transition: `all ${n} ${i}!important`
  }), p(`&.${e}-transition-enter-from, &.${e}-transition-leave-to`, {
    opacity: 0
  }), p(`&.${e}-transition-leave-from, &.${e}-transition-enter-to`, {
    opacity: 1
  })]
}
var Qs = N(`scrollbar`, `
 overflow: hidden;
 position: relative;
 z-index: auto;
 height: 100%;
 width: 100%;
`, [p(`>`, [N(`scrollbar-container`, `
 width: 100%;
 overflow: scroll;
 height: 100%;
 min-height: inherit;
 max-height: inherit;
 scrollbar-width: none;
 `, [p(`&::-webkit-scrollbar, &::-webkit-scrollbar-track-piece, &::-webkit-scrollbar-thumb`, `
 width: 0;
 height: 0;
 display: none;
 `), p(`>`, [N(`scrollbar-content`, `
 box-sizing: border-box;
 min-width: 100%;
 `)])])]), p(`>, +`, [N(`scrollbar-rail`, `
 position: absolute;
 pointer-events: none;
 user-select: none;
 background: var(--n-scrollbar-rail-color);
 -webkit-user-select: none;
 `, [y(`horizontal`, `
 height: var(--n-scrollbar-height);
 `, [p(`>`, [f(`scrollbar`, `
 height: var(--n-scrollbar-height);
 border-radius: var(--n-scrollbar-border-radius);
 right: 0;
 `)])]), y(`horizontal--top`, `
 top: var(--n-scrollbar-rail-top-horizontal-top); 
 right: var(--n-scrollbar-rail-right-horizontal-top); 
 bottom: var(--n-scrollbar-rail-bottom-horizontal-top); 
 left: var(--n-scrollbar-rail-left-horizontal-top); 
 `), y(`horizontal--bottom`, `
 top: var(--n-scrollbar-rail-top-horizontal-bottom); 
 right: var(--n-scrollbar-rail-right-horizontal-bottom); 
 bottom: var(--n-scrollbar-rail-bottom-horizontal-bottom); 
 left: var(--n-scrollbar-rail-left-horizontal-bottom); 
 `), y(`vertical`, `
 width: var(--n-scrollbar-width);
 `, [p(`>`, [f(`scrollbar`, `
 width: var(--n-scrollbar-width);
 border-radius: var(--n-scrollbar-border-radius);
 bottom: 0;
 `)])]), y(`vertical--left`, `
 top: var(--n-scrollbar-rail-top-vertical-left); 
 right: var(--n-scrollbar-rail-right-vertical-left); 
 bottom: var(--n-scrollbar-rail-bottom-vertical-left); 
 left: var(--n-scrollbar-rail-left-vertical-left); 
 `), y(`vertical--right`, `
 top: var(--n-scrollbar-rail-top-vertical-right); 
 right: var(--n-scrollbar-rail-right-vertical-right); 
 bottom: var(--n-scrollbar-rail-bottom-vertical-right); 
 left: var(--n-scrollbar-rail-left-vertical-right); 
 `), y(`disabled`, [p(`>`, [f(`scrollbar`, `pointer-events: none;`)])]), p(`>`, [f(`scrollbar`, `
 z-index: 1;
 position: absolute;
 cursor: pointer;
 pointer-events: all;
 background-color: var(--n-scrollbar-color);
 transition: background-color .2s var(--n-scrollbar-bezier);
 `, [Zs(), p(`&:hover`, `background-color: var(--n-scrollbar-color-hover);`)])])])])]),
  $s = l({
    name: `Scrollbar`,
    props: Object.assign(Object.assign({}, Re.props), {
      duration: {
        type: Number,
        default: 0
      },
      scrollable: {
        type: Boolean,
        default: !0
      },
      xScrollable: Boolean,
      trigger: {
        type: String,
        default: `hover`
      },
      useUnifiedContainer: Boolean,
      triggerDisplayManually: Boolean,
      container: Function,
      content: Function,
      containerClass: String,
      containerStyle: [String, Object],
      contentClass: [String, Array],
      contentStyle: [String, Object],
      horizontalRailStyle: [String, Object],
      verticalRailStyle: [String, Object],
      onScroll: Function,
      onWheel: Function,
      onResize: Function,
      internalOnUpdateScrollLeft: Function,
      internalHoistYRail: Boolean,
      internalExposeWidthCssVar: Boolean,
      yPlacement: {
        type: String,
        default: `right`
      },
      xPlacement: {
        type: String,
        default: `bottom`
      }
    }),
    inheritAttrs: !1,
    setup(e) {
      let {
        mergedClsPrefixRef: t,
        inlineThemeDisabled: n,
        mergedRtlRef: r
      } = h(e), i = Na(`Scrollbar`, r, t), a = U(null), o = U(null), s = U(null), c = U(null), l = U(null), d = U(null), f = U(null), p = U(null), m = U(null), _ = U(null), v = U(null), y = U(0), b = U(0), x = U(!1), S = U(!1), C = !1, w = !1, T, E, D = 0, O = 0, k = 0, A = 0, ee = gr(), te = Re(`Scrollbar`, `-scrollbar`, Qs, Ys, e, t), M = F(() => {
        let {
          value: e
        } = p, {
          value: t
        } = d, {
          value: n
        } = _;
        return e === null || t === null || n === null ? 0 : Math.min(e, n * e / t + Kn(te.value.self.width) * 1.5)
      }), ne = F(() => `${M.value}px`), re = F(() => {
        let {
          value: e
        } = m, {
          value: t
        } = f, {
          value: n
        } = v;
        return e === null || t === null || n === null ? 0 : n * e / t + Kn(te.value.self.height) * 1.5
      }), N = F(() => `${re.value}px`), ie = F(() => {
        let {
          value: e
        } = p, {
          value: t
        } = y, {
          value: n
        } = d, {
          value: r
        } = _;
        if (e === null || n === null || r === null) return 0;
        {
          let i = n - e;
          return i ? t / i * (r - M.value) : 0
        }
      }), ae = F(() => `${ie.value}px`), P = F(() => {
        let {
          value: e
        } = m, {
          value: t
        } = b, {
          value: n
        } = f, {
          value: r
        } = v;
        if (e === null || n === null || r === null) return 0;
        {
          let i = n - e;
          return i ? t / i * (r - re.value) : 0
        }
      }), oe = F(() => `${P.value}px`), se = F(() => {
        let {
          value: e
        } = p, {
          value: t
        } = d;
        return e !== null && t !== null && t > e
      }), le = F(() => {
        let {
          value: e
        } = m, {
          value: t
        } = f;
        return e !== null && t !== null && t > e
      }), ue = F(() => {
        let {
          trigger: t
        } = e;
        return t === `none` || x.value
      }), de = F(() => {
        let {
          trigger: t
        } = e;
        return t === `none` || S.value
      }), I = F(() => {
        let {
          container: t
        } = e;
        return t ? t() : o.value
      }), fe = F(() => {
        let {
          content: t
        } = e;
        return t ? t() : s.value
      }), pe = (t, n) => {
        if (!e.scrollable) return;
        if (typeof t == `number`) {
          ve(t, n == null ? 0 : n, 0, !1, `auto`);
          return
        }
        let {
          left: r,
          top: i,
          index: a,
          elSize: o,
          position: s,
          behavior: c,
          el: l,
          debounce: u = !0
        } = t;
        (r !== void 0 || i !== void 0) && ve(r == null ? 0 : r, i == null ? 0 : i, 0, !1, c), l === void 0 ? a !== void 0 && o !== void 0 ? ve(0, a * o, o, u, c) : s === `bottom` ? ve(0, 2 ** 53 - 1, 0, !1, c) : s === `top` && ve(0, 0, 0, !1, c) : ve(0, l.offsetTop, l.offsetHeight, u, c)
      }, me = Dr(() => {
        e.container || pe({
          top: y.value,
          left: b.value
        })
      }), he = () => {
        me.isDeactivated || R()
      }, ge = t => {
        if (me.isDeactivated) return;
        let {
          onResize: n
        } = e;
        n && n(t), R()
      }, _e = (t, n) => {
        if (!e.scrollable) return;
        let {
          value: r
        } = I;
        r && (typeof t == `object` ? r.scrollBy(t) : r.scrollBy(t, n || 0))
      };

      function ve(e, t, n, r, i) {
        let {
          value: a
        } = I;
        if (a) {
          if (r) {
            let {
              scrollTop: r,
              offsetHeight: o
            } = a;
            if (t > r) {
              t + n <= r + o || a.scrollTo({
                left: e,
                top: t + n - o,
                behavior: i
              });
              return
            }
          }
          a.scrollTo({
            left: e,
            top: t,
            behavior: i
          })
        }
      }

      function ye() {
        Ce(), we(), R()
      }

      function be() {
        L()
      }

      function L() {
        xe(), Se()
      }

      function xe() {
        E !== void 0 && window.clearTimeout(E), E = window.setTimeout(() => {
          S.value = !1
        }, e.duration)
      }

      function Se() {
        T !== void 0 && window.clearTimeout(T), T = window.setTimeout(() => {
          x.value = !1
        }, e.duration)
      }

      function Ce() {
        T !== void 0 && window.clearTimeout(T), x.value = !0
      }

      function we() {
        E !== void 0 && window.clearTimeout(E), S.value = !0
      }

      function Te(t) {
        let {
          onScroll: n
        } = e;
        n && n(t), Ee()
      }

      function Ee() {
        let {
          value: e
        } = I;
        e && (y.value = e.scrollTop, b.value = e.scrollLeft * (i != null && i.value ? -1 : 1))
      }

      function De() {
        let {
          value: e
        } = fe;
        e && (d.value = e.offsetHeight, f.value = e.offsetWidth);
        let {
          value: t
        } = I;
        t && (p.value = t.offsetHeight, m.value = t.offsetWidth);
        let {
          value: n
        } = l, {
          value: r
        } = c;
        n && (v.value = n.offsetWidth), r && (_.value = r.offsetHeight)
      }

      function Oe() {
        let {
          value: e
        } = I;
        e && (y.value = e.scrollTop, b.value = e.scrollLeft * (i != null && i.value ? -1 : 1), p.value = e.offsetHeight, m.value = e.offsetWidth, d.value = e.scrollHeight, f.value = e.scrollWidth);
        let {
          value: t
        } = l, {
          value: n
        } = c;
        t && (v.value = t.offsetWidth), n && (_.value = n.offsetHeight)
      }

      function R() {
        e.scrollable && (e.useUnifiedContainer ? Oe() : (De(), Ee()))
      }

      function ke(e) {
        var t;
        return !((t = a.value) != null && t.contains(Gn(e)))
      }

      function Ae(e) {
        e.preventDefault(), e.stopPropagation(), w = !0, W(`mousemove`, window, z, !0), W(`mouseup`, window, je, !0), O = b.value, k = i != null && i.value ? window.innerWidth - e.clientX : e.clientX
      }

      function z(t) {
        if (!w) return;
        T !== void 0 && window.clearTimeout(T), E !== void 0 && window.clearTimeout(E);
        let {
          value: n
        } = m, {
          value: r
        } = f, {
          value: a
        } = re;
        if (n === null || r === null) return;
        let o = (i != null && i.value ? window.innerWidth - t.clientX - k : t.clientX - k) * (r - n) / (n - a),
          s = r - n,
          c = O + o;
        c = Math.min(s, c), c = Math.max(c, 0);
        let {
          value: l
        } = I;
        if (l) {
          l.scrollLeft = c * (i != null && i.value ? -1 : 1);
          let {
            internalOnUpdateScrollLeft: t
          } = e;
          t && t(c)
        }
      }

      function je(e) {
        e.preventDefault(), e.stopPropagation(), G(`mousemove`, window, z, !0), G(`mouseup`, window, je, !0), w = !1, R(), ke(e) && L()
      }

      function Me(e) {
        e.preventDefault(), e.stopPropagation(), C = !0, W(`mousemove`, window, Ne, !0), W(`mouseup`, window, Pe, !0), D = y.value, A = e.clientY
      }

      function Ne(e) {
        if (!C) return;
        T !== void 0 && window.clearTimeout(T), E !== void 0 && window.clearTimeout(E);
        let {
          value: t
        } = p, {
          value: n
        } = d, {
          value: r
        } = M;
        if (t === null || n === null) return;
        let i = (e.clientY - A) * (n - t) / (t - r),
          a = n - t,
          o = D + i;
        o = Math.min(a, o), o = Math.max(o, 0);
        let {
          value: s
        } = I;
        s && (s.scrollTop = o)
      }

      function Pe(e) {
        e.preventDefault(), e.stopPropagation(), G(`mousemove`, window, Ne, !0), G(`mouseup`, window, Pe, !0), C = !1, R(), ke(e) && L()
      }
      ce(() => {
        let {
          value: e
        } = le, {
          value: n
        } = se, {
          value: r
        } = t, {
          value: i
        } = l, {
          value: a
        } = c;
        i && (e ? i.classList.remove(`${r}-scrollbar-rail--disabled`) : i.classList.add(`${r}-scrollbar-rail--disabled`)), a && (n ? a.classList.remove(`${r}-scrollbar-rail--disabled`) : a.classList.add(`${r}-scrollbar-rail--disabled`))
      }), j(() => {
        e.container || R()
      }), g(() => {
        T !== void 0 && window.clearTimeout(T), E !== void 0 && window.clearTimeout(E), G(`mousemove`, window, Ne, !0), G(`mouseup`, window, Pe, !0)
      });
      let Fe = F(() => {
          let {
            common: {
              cubicBezierEaseInOut: e
            },
            self: {
              color: t,
              colorHover: n,
              height: r,
              width: a,
              borderRadius: o,
              railInsetHorizontalTop: s,
              railInsetHorizontalBottom: c,
              railInsetVerticalRight: l,
              railInsetVerticalLeft: u,
              railColor: d
            }
          } = te.value, {
            top: f,
            right: p,
            bottom: m,
            left: h
          } = Jn(s), {
            top: g,
            right: _,
            bottom: v,
            left: y
          } = Jn(c), {
            top: b,
            right: x,
            bottom: S,
            left: C
          } = Jn(i != null && i.value ? _a(l) : l), {
            top: w,
            right: T,
            bottom: E,
            left: D
          } = Jn(i != null && i.value ? _a(u) : u);
          return {
            "--n-scrollbar-bezier": e,
            "--n-scrollbar-color": t,
            "--n-scrollbar-color-hover": n,
            "--n-scrollbar-border-radius": o,
            "--n-scrollbar-width": a,
            "--n-scrollbar-height": r,
            "--n-scrollbar-rail-top-horizontal-top": f,
            "--n-scrollbar-rail-right-horizontal-top": p,
            "--n-scrollbar-rail-bottom-horizontal-top": m,
            "--n-scrollbar-rail-left-horizontal-top": h,
            "--n-scrollbar-rail-top-horizontal-bottom": g,
            "--n-scrollbar-rail-right-horizontal-bottom": _,
            "--n-scrollbar-rail-bottom-horizontal-bottom": v,
            "--n-scrollbar-rail-left-horizontal-bottom": y,
            "--n-scrollbar-rail-top-vertical-right": b,
            "--n-scrollbar-rail-right-vertical-right": x,
            "--n-scrollbar-rail-bottom-vertical-right": S,
            "--n-scrollbar-rail-left-vertical-right": C,
            "--n-scrollbar-rail-top-vertical-left": w,
            "--n-scrollbar-rail-right-vertical-left": T,
            "--n-scrollbar-rail-bottom-vertical-left": E,
            "--n-scrollbar-rail-left-vertical-left": D,
            "--n-scrollbar-rail-color": d
          }
        }),
        Ie = n ? u(`scrollbar`, void 0, Fe, e) : void 0;
      return Object.assign(Object.assign({}, {
        scrollTo: pe,
        scrollBy: _e,
        sync: R,
        syncUnifiedContainer: Oe,
        handleMouseEnterWrapper: ye,
        handleMouseLeaveWrapper: be
      }), {
        mergedClsPrefix: t,
        rtlEnabled: i,
        containerScrollTop: y,
        wrapperRef: a,
        containerRef: o,
        contentRef: s,
        yRailRef: c,
        xRailRef: l,
        needYBar: se,
        needXBar: le,
        yBarSizePx: ne,
        xBarSizePx: N,
        yBarTopPx: ae,
        xBarLeftPx: oe,
        isShowXBar: ue,
        isShowYBar: de,
        isIos: ee,
        handleScroll: Te,
        handleContentResize: he,
        handleContainerResize: ge,
        handleYScrollMouseDown: Me,
        handleXScrollMouseDown: Ae,
        containerWidth: m,
        cssVars: n ? void 0 : Fe,
        themeClass: Ie == null ? void 0 : Ie.themeClass,
        onRender: Ie == null ? void 0 : Ie.onRender
      })
    },
    render() {
      var e;
      let {
        $slots: t,
        mergedClsPrefix: n,
        triggerDisplayManually: r,
        rtlEnabled: i,
        internalHoistYRail: a,
        yPlacement: o,
        xPlacement: s,
        xScrollable: c
      } = this;
      if (!this.scrollable) return (e = t.default) == null ? void 0 : e.call(t);
      let l = this.trigger === `none`,
        u = (e, t) => z(`div`, {
          ref: `yRailRef`,
          class: [`${n}-scrollbar-rail`, `${n}-scrollbar-rail--vertical`, `${n}-scrollbar-rail--vertical--${o}`, e],
          "data-scrollbar-rail": !0,
          style: [t || ``, this.verticalRailStyle],
          "aria-hidden": !0
        }, z(l ? Aa : Ot, l ? null : {
          name: `fade-in-transition`
        }, {
          default: () => this.needYBar && this.isShowYBar && !this.isIos ? z(`div`, {
            class: `${n}-scrollbar-rail__scrollbar`,
            style: {
              height: this.yBarSizePx,
              top: this.yBarTopPx
            },
            onMousedown: this.handleYScrollMouseDown
          }) : null
        })),
        d = () => {
          var e, o;
          return (e = this.onRender) == null || e.call(this), z(`div`, S(this.$attrs, {
            role: `none`,
            ref: `wrapperRef`,
            class: [`${n}-scrollbar`, this.themeClass, i && `${n}-scrollbar--rtl`],
            style: this.cssVars,
            onMouseenter: r ? void 0 : this.handleMouseEnterWrapper,
            onMouseleave: r ? void 0 : this.handleMouseLeaveWrapper
          }), [this.container ? (o = t.default) == null ? void 0 : o.call(t) : z(`div`, {
            role: `none`,
            ref: `containerRef`,
            class: [`${n}-scrollbar-container`, this.containerClass],
            style: [this.containerStyle, this.internalExposeWidthCssVar ? {
              "--n-scrollbar-current-width": qn(this.containerWidth)
            } : void 0],
            onScroll: this.handleScroll,
            onWheel: this.onWheel
          }, z(ca, {
            onResize: this.handleContentResize
          }, {
            default: () => z(`div`, {
              ref: `contentRef`,
              role: `none`,
              style: [{
                width: this.xScrollable ? `fit-content` : null
              }, this.contentStyle],
              class: [`${n}-scrollbar-content`, this.contentClass]
            }, t)
          })), a ? null : u(void 0, void 0), c && z(`div`, {
            ref: `xRailRef`,
            class: [`${n}-scrollbar-rail`, `${n}-scrollbar-rail--horizontal`, `${n}-scrollbar-rail--horizontal--${s}`],
            style: this.horizontalRailStyle,
            "data-scrollbar-rail": !0,
            "aria-hidden": !0
          }, z(l ? Aa : Ot, l ? null : {
            name: `fade-in-transition`
          }, {
            default: () => this.needXBar && this.isShowXBar && !this.isIos ? z(`div`, {
              class: `${n}-scrollbar-rail__scrollbar`,
              style: {
                width: this.xBarSizePx,
                right: i ? this.xBarLeftPx : void 0,
                left: i ? void 0 : this.xBarLeftPx
              },
              onMousedown: this.handleXScrollMouseDown
            }) : null
          }))])
        },
        f = this.container ? d() : z(ca, {
          onResize: this.handleContainerResize
        }, {
          default: d
        });
      return a ? z(L, null, f, u(this.themeClass, this.cssVars)) : f
    }
  }),
  ec = $s,
  tc = {
    space: `6px`,
    spaceArrow: `10px`,
    arrowOffset: `10px`,
    arrowOffsetVertical: `10px`,
    arrowHeight: `6px`,
    padding: `8px 14px`
  };

function nc(e) {
  let {
    boxShadow2: t,
    popoverColor: n,
    textColor2: r,
    borderRadius: i,
    fontSize: a,
    dividerColor: o
  } = e;
  return Object.assign(Object.assign({}, tc), {
    fontSize: a,
    borderRadius: i,
    color: n,
    dividerColor: o,
    textColor: r,
    boxShadow: t
  })
}
var rc = de({
    name: `Popover`,
    common: De,
    peers: {
      Scrollbar: Ys
    },
    self: nc
  }),
  ic = {
    top: `bottom`,
    bottom: `top`,
    left: `right`,
    right: `left`
  },
  K = `var(--n-arrow-height) * 1.414`,
  ac = p([N(`popover`, `
 transition:
 box-shadow .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 position: relative;
 font-size: var(--n-font-size);
 color: var(--n-text-color);
 box-shadow: var(--n-box-shadow);
 word-break: break-word;
 `, [p(`>`, [N(`scrollbar`, `
 height: inherit;
 max-height: inherit;
 `)]), Ue(`raw`, `
 background-color: var(--n-color);
 border-radius: var(--n-border-radius);
 `, [Ue(`scrollable`, [Ue(`show-header-or-footer`, `padding: var(--n-padding);`)])]), f(`header`, `
 padding: var(--n-padding);
 border-bottom: 1px solid var(--n-divider-color);
 transition: border-color .3s var(--n-bezier);
 `), f(`footer`, `
 padding: var(--n-padding);
 border-top: 1px solid var(--n-divider-color);
 transition: border-color .3s var(--n-bezier);
 `), y(`scrollable, show-header-or-footer`, [f(`content`, `
 padding: var(--n-padding);
 `)])]), N(`popover-shared`, `
 transform-origin: inherit;
 `, [N(`popover-arrow-wrapper`, `
 position: absolute;
 overflow: hidden;
 pointer-events: none;
 `, [N(`popover-arrow`, `
 transition: background-color .3s var(--n-bezier);
 position: absolute;
 display: block;
 width: calc(${K});
 height: calc(${K});
 box-shadow: 0 0 8px 0 rgba(0, 0, 0, .12);
 transform: rotate(45deg);
 background-color: var(--n-color);
 pointer-events: all;
 `)]), p(`&.popover-transition-enter-from, &.popover-transition-leave-to`, `
 opacity: 0;
 transform: scale(.85);
 `), p(`&.popover-transition-enter-to, &.popover-transition-leave-from`, `
 transform: scale(1);
 opacity: 1;
 `), p(`&.popover-transition-enter-active`, `
 transition:
 box-shadow .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier),
 opacity .15s var(--n-bezier-ease-out),
 transform .15s var(--n-bezier-ease-out);
 `), p(`&.popover-transition-leave-active`, `
 transition:
 box-shadow .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier),
 opacity .15s var(--n-bezier-ease-in),
 transform .15s var(--n-bezier-ease-in);
 `)]), q(`top-start`, `
 top: calc(${K} / -2);
 left: calc(${oc(`top-start`)} - var(--v-offset-left));
 `), q(`top`, `
 top: calc(${K} / -2);
 transform: translateX(calc(${K} / -2)) rotate(45deg);
 left: 50%;
 `), q(`top-end`, `
 top: calc(${K} / -2);
 right: calc(${oc(`top-end`)} + var(--v-offset-left));
 `), q(`bottom-start`, `
 bottom: calc(${K} / -2);
 left: calc(${oc(`bottom-start`)} - var(--v-offset-left));
 `), q(`bottom`, `
 bottom: calc(${K} / -2);
 transform: translateX(calc(${K} / -2)) rotate(45deg);
 left: 50%;
 `), q(`bottom-end`, `
 bottom: calc(${K} / -2);
 right: calc(${oc(`bottom-end`)} + var(--v-offset-left));
 `), q(`left-start`, `
 left: calc(${K} / -2);
 top: calc(${oc(`left-start`)} - var(--v-offset-top));
 `), q(`left`, `
 left: calc(${K} / -2);
 transform: translateY(calc(${K} / -2)) rotate(45deg);
 top: 50%;
 `), q(`left-end`, `
 left: calc(${K} / -2);
 bottom: calc(${oc(`left-end`)} + var(--v-offset-top));
 `), q(`right-start`, `
 right: calc(${K} / -2);
 top: calc(${oc(`right-start`)} - var(--v-offset-top));
 `), q(`right`, `
 right: calc(${K} / -2);
 transform: translateY(calc(${K} / -2)) rotate(45deg);
 top: 50%;
 `), q(`right-end`, `
 right: calc(${K} / -2);
 bottom: calc(${oc(`right-end`)} + var(--v-offset-top));
 `), ...Rs({
    top: [`right-start`, `left-start`],
    right: [`top-end`, `bottom-end`],
    bottom: [`right-end`, `left-end`],
    left: [`top-start`, `bottom-start`]
  }, (e, t) => {
    let n = [`right`, `left`].includes(t),
      r = n ? `width` : `height`;
    return e.map(e => {
      let i = e.split(`-`)[1] === `end`,
        a = `calc((${`var(--v-target-${r}, 0px)`} - ${K}) / 2)`,
        o = oc(e);
      return p(`[v-placement="${e}"] >`, [N(`popover-shared`, [y(`center-arrow`, [N(`popover-arrow`, `${t}: calc(max(${a}, ${o}) ${i?`+`:`-`} var(--v-offset-${n?`left`:`top`}));`)])])])
    })
  })]);

function oc(e) {
  return [`top`, `bottom`].includes(e.split(`-`)[0]) ? `var(--n-arrow-offset)` : `var(--n-arrow-offset-vertical)`
}

function q(e, t) {
  let n = e.split(`-`)[0],
    r = [`top`, `bottom`].includes(n) ? `height: var(--n-space-arrow);` : `width: var(--n-space-arrow);`;
  return p(`[v-placement="${e}"] >`, [N(`popover-shared`, `
 margin-${ic[n]}: var(--n-space);
 `, [y(`show-arrow`, `
 margin-${ic[n]}: var(--n-space-arrow);
 `), y(`overlap`, `
 margin: 0;
 `), ie(`popover-arrow-wrapper`, `
 right: 0;
 left: 0;
 top: 0;
 bottom: 0;
 ${n}: 100%;
 ${ic[n]}: auto;
 ${r}
 `, [N(`popover-arrow`, t)])])])
}
var sc = Object.assign(Object.assign({}, Re.props), {
  to: Tr.propTo,
  show: Boolean,
  trigger: String,
  showArrow: Boolean,
  delay: Number,
  duration: Number,
  raw: Boolean,
  arrowPointToCenter: Boolean,
  arrowClass: String,
  arrowStyle: [String, Object],
  arrowWrapperClass: String,
  arrowWrapperStyle: [String, Object],
  displayDirective: String,
  x: Number,
  y: Number,
  flip: Boolean,
  overlap: Boolean,
  placement: String,
  width: [Number, String],
  keepAliveOnHover: Boolean,
  scrollable: Boolean,
  contentClass: String,
  contentStyle: [Object, String],
  headerClass: String,
  headerStyle: [Object, String],
  footerClass: String,
  footerStyle: [Object, String],
  internalDeactivateImmediately: Boolean,
  animated: Boolean,
  onClickoutside: Function,
  internalTrapFocus: Boolean,
  internalOnAfterLeave: Function,
  minWidth: Number,
  maxWidth: Number
});

function cc({
  arrowClass: e,
  arrowStyle: t,
  arrowWrapperClass: n,
  arrowWrapperStyle: r,
  clsPrefix: i
}) {
  return z(`div`, {
    key: `__popover-arrow__`,
    style: r,
    class: [`${i}-popover-arrow-wrapper`, n]
  }, z(`div`, {
    class: [`${i}-popover-arrow`, e],
    style: t
  }))
}
var lc = l({
    name: `PopoverBody`,
    inheritAttrs: !1,
    props: sc,
    setup(t, {
      slots: r,
      attrs: i
    }) {
      let {
        namespaceRef: a,
        mergedClsPrefixRef: o,
        inlineThemeDisabled: s,
        mergedRtlRef: c
      } = h(t), l = Re(`Popover`, `-popover`, ac, rc, t, o), d = Na(`Popover`, c, o), f = U(null), p = n(`NPopover`), _ = U(null), v = U(t.show), y = U(!1);
      ce(() => {
        let {
          show: e
        } = t;
        e && !ya() && !t.internalDeactivateImmediately && (y.value = !0)
      });
      let b = F(() => {
          let {
            trigger: e,
            onClickoutside: n
          } = t, r = [], {
            positionManuallyRef: {
              value: i
            }
          } = p;
          return i || (e === `click` && !n && r.push([Hr, k, void 0, {
            capture: !0
          }]), e === `hover` && r.push([Br, O])), n && r.push([Hr, k, void 0, {
            capture: !0
          }]), (t.displayDirective === `show` || t.animated && y.value) && r.push([Kt, t.show]), r
        }),
        x = F(() => {
          let {
            common: {
              cubicBezierEaseInOut: e,
              cubicBezierEaseIn: t,
              cubicBezierEaseOut: n
            },
            self: {
              space: r,
              spaceArrow: i,
              padding: a,
              fontSize: o,
              textColor: s,
              dividerColor: c,
              color: u,
              boxShadow: d,
              borderRadius: f,
              arrowHeight: p,
              arrowOffset: m,
              arrowOffsetVertical: h
            }
          } = l.value;
          return {
            "--n-box-shadow": d,
            "--n-bezier": e,
            "--n-bezier-ease-in": t,
            "--n-bezier-ease-out": n,
            "--n-font-size": o,
            "--n-text-color": s,
            "--n-color": u,
            "--n-divider-color": c,
            "--n-border-radius": f,
            "--n-arrow-height": p,
            "--n-arrow-offset": m,
            "--n-arrow-offset-vertical": h,
            "--n-padding": a,
            "--n-space": r,
            "--n-space-arrow": i
          }
        }),
        C = F(() => {
          let e = t.width === `trigger` ? void 0 : se(t.width),
            n = [];
          e && n.push({
            width: e
          });
          let {
            maxWidth: r,
            minWidth: i
          } = t;
          return r && n.push({
            maxWidth: se(r)
          }), i && n.push({
            maxWidth: se(i)
          }), s || n.push(x.value), n
        }),
        w = s ? u(`popover`, void 0, x, t) : void 0;
      p.setBodyInstance({
        syncPosition: T
      }), g(() => {
        p.setBodyInstance(null)
      }), P(Ke(t, `show`), e => {
        t.animated || (e ? v.value = !0 : v.value = !1)
      });

      function T() {
        var e;
        (e = f.value) == null || e.syncPosition()
      }

      function E(e) {
        t.trigger === `hover` && t.keepAliveOnHover && t.show && p.handleMouseEnter(e)
      }

      function D(e) {
        t.trigger === `hover` && t.keepAliveOnHover && p.handleMouseLeave(e)
      }

      function O(e) {
        t.trigger === `hover` && !A().contains(Gn(e)) && p.handleMouseMoveOutside(e)
      }

      function k(e) {
        (t.trigger === `click` && !A().contains(Gn(e)) || t.onClickoutside) && p.handleClickOutside(e)
      }

      function A() {
        return p.getTriggerElement()
      }
      m(Cr, _), m(yr, null), m(br, null);

      function j() {
        if (w == null || w.onRender(), !(t.displayDirective === `show` || t.show || t.animated && y.value)) return null;
        let n, a = p.internalRenderBodyRef.value,
          {
            value: s
          } = o;
        if (a) n = a([`${s}-popover-shared`, (d == null ? void 0 : d.value) && `${s}-popover--rtl`, w == null ? void 0 : w.themeClass.value, t.overlap && `${s}-popover-shared--overlap`, t.showArrow && `${s}-popover-shared--show-arrow`, t.arrowPointToCenter && `${s}-popover-shared--center-arrow`], _, C.value, E, D);
        else {
          let {
            value: e
          } = p.extraClassRef, {
            internalTrapFocus: a
          } = t, o = !ka(r.header) || !ka(r.footer), c = () => {
            var e, n;
            let i = o ? z(L, null, Oa(r.header, e => e ? z(`div`, {
              class: [`${s}-popover__header`, t.headerClass],
              style: t.headerStyle
            }, e) : null), Oa(r.default, e => e ? z(`div`, {
              class: [`${s}-popover__content`, t.contentClass],
              style: t.contentStyle
            }, r) : null), Oa(r.footer, e => e ? z(`div`, {
              class: [`${s}-popover__footer`, t.footerClass],
              style: t.footerStyle
            }, e) : null)) : t.scrollable ? (e = r.default) == null ? void 0 : e.call(r) : z(`div`, {
              class: [`${s}-popover__content`, t.contentClass],
              style: t.contentStyle
            }, r);
            return [t.scrollable ? z(ec, {
              themeOverrides: l.value.peerOverrides.Scrollbar,
              theme: l.value.peers.Scrollbar,
              contentClass: o ? void 0 : `${s}-popover__content ${(n=t.contentClass)==null?``:n}`,
              contentStyle: o ? void 0 : t.contentStyle
            }, {
              default: () => i
            }) : i, t.showArrow ? cc({
              arrowClass: t.arrowClass,
              arrowStyle: t.arrowStyle,
              arrowWrapperClass: t.arrowWrapperClass,
              arrowWrapperStyle: t.arrowWrapperStyle,
              clsPrefix: s
            }) : null]
          };
          n = z(`div`, S({
            class: [`${s}-popover`, `${s}-popover-shared`, (d == null ? void 0 : d.value) && `${s}-popover--rtl`, w == null ? void 0 : w.themeClass.value, e.map(e => `${s}-${e}`), {
              [`${s}-popover--scrollable`]: t.scrollable,
              [`${s}-popover--show-header-or-footer`]: o,
              [`${s}-popover--raw`]: t.raw,
              [`${s}-popover-shared--overlap`]: t.overlap,
              [`${s}-popover-shared--show-arrow`]: t.showArrow,
              [`${s}-popover-shared--center-arrow`]: t.arrowPointToCenter
            }],
            ref: _,
            style: C.value,
            onKeydown: p.handleKeydown,
            onMouseenter: E,
            onMouseleave: D
          }, i), a ? z(ha, {
            active: t.show,
            autoFocus: !0
          }, {
            default: c
          }) : c())
        }
        return e(n, b.value)
      }
      return {
        displayed: y,
        namespace: a,
        isMounted: p.isMountedRef,
        zIndex: p.zIndexRef,
        followerRef: f,
        adjustedTo: Tr(t),
        followerEnabled: v,
        renderContentNode: j
      }
    },
    render() {
      return z(di, {
        ref: `followerRef`,
        zIndex: this.zIndex,
        show: this.show,
        enabled: this.followerEnabled,
        to: this.adjustedTo,
        x: this.x,
        y: this.y,
        flip: this.flip,
        placement: this.placement,
        containerClass: this.namespace,
        overlap: this.overlap,
        width: this.width === `trigger` ? `target` : void 0,
        teleportDisabled: this.adjustedTo === Tr.tdkey
      }, {
        default: () => this.animated ? z(Ot, {
          name: `popover-transition`,
          appear: this.isMounted,
          onEnter: () => {
            this.followerEnabled = !0
          },
          onAfterLeave: () => {
            var e;
            (e = this.internalOnAfterLeave) == null || e.call(this), this.followerEnabled = !1, this.displayed = !1
          }
        }, {
          default: this.renderContentNode
        }) : this.renderContentNode()
      })
    }
  }),
  uc = Object.keys(sc),
  dc = {
    focus: [`onFocus`, `onBlur`],
    click: [`onClick`],
    hover: [`onMouseenter`, `onMouseleave`],
    manual: [],
    nested: [`onFocus`, `onBlur`, `onMouseenter`, `onMouseleave`, `onClick`]
  };

function fc(e, t, n) {
  dc[t].forEach(t => {
    e.props ? e.props = Object.assign({}, e.props) : e.props = {};
    let r = e.props[t],
      i = n[t];
    r ? e.props[t] = (...e) => {
      r(...e), i(...e)
    } : e.props[t] = i
  })
}
var pc = {
    show: {
      type: Boolean,
      default: void 0
    },
    defaultShow: Boolean,
    showArrow: {
      type: Boolean,
      default: !0
    },
    trigger: {
      type: String,
      default: `hover`
    },
    delay: {
      type: Number,
      default: 100
    },
    duration: {
      type: Number,
      default: 100
    },
    raw: Boolean,
    placement: {
      type: String,
      default: `top`
    },
    x: Number,
    y: Number,
    arrowPointToCenter: Boolean,
    disabled: Boolean,
    getDisabled: Function,
    displayDirective: {
      type: String,
      default: `if`
    },
    arrowClass: String,
    arrowStyle: [String, Object],
    arrowWrapperClass: String,
    arrowWrapperStyle: [String, Object],
    flip: {
      type: Boolean,
      default: !0
    },
    animated: {
      type: Boolean,
      default: !0
    },
    width: {
      type: [Number, String],
      default: void 0
    },
    overlap: Boolean,
    keepAliveOnHover: {
      type: Boolean,
      default: !0
    },
    zIndex: Number,
    to: Tr.propTo,
    scrollable: Boolean,
    contentClass: String,
    contentStyle: [Object, String],
    headerClass: String,
    headerStyle: [Object, String],
    footerClass: String,
    footerStyle: [Object, String],
    onClickoutside: Function,
    "onUpdate:show": [Function, Array],
    onUpdateShow: [Function, Array],
    internalDeactivateImmediately: Boolean,
    internalSyncTargetWithParent: Boolean,
    internalInheritedEventHandlers: {
      type: Array,
      default: () => []
    },
    internalTrapFocus: Boolean,
    internalExtraClass: {
      type: Array,
      default: () => []
    },
    onShow: [Function, Array],
    onHide: [Function, Array],
    arrow: {
      type: Boolean,
      default: void 0
    },
    minWidth: Number,
    maxWidth: Number
  },
  mc = l({
    name: `Popover`,
    inheritAttrs: !1,
    props: Object.assign(Object.assign(Object.assign({}, Re.props), pc), {
      internalOnAfterLeave: Function,
      internalRenderBody: Function
    }),
    slots: Object,
    __popover__: !0,
    setup(e) {
      let t = pr(),
        n = U(null),
        r = F(() => e.show),
        i = U(e.defaultShow),
        a = fr(r, i),
        o = or(() => e.disabled ? !1 : a.value),
        s = () => {
          if (e.disabled) return !0;
          let {
            getDisabled: t
          } = e;
          return !!(t != null && t())
        },
        c = () => s() ? !1 : a.value,
        l = mr(e, [`arrow`, `showArrow`]),
        u = F(() => e.overlap ? !1 : l.value),
        d = null,
        f = U(null),
        p = U(null),
        h = or(() => e.x !== void 0 && e.y !== void 0);

      function g(t) {
        let {
          "onUpdate:show": n,
          onUpdateShow: r,
          onShow: a,
          onHide: o
        } = e;
        i.value = t, n && ba(n, t), r && ba(r, t), t && a && ba(a, !0), t && o && ba(o, !1)
      }

      function _() {
        d && d.syncPosition()
      }

      function v() {
        let {
          value: e
        } = f;
        e && (window.clearTimeout(e), f.value = null)
      }

      function y() {
        let {
          value: e
        } = p;
        e && (window.clearTimeout(e), p.value = null)
      }

      function b() {
        let t = s();
        if (e.trigger === `focus` && !t) {
          if (c()) return;
          g(!0)
        }
      }

      function x() {
        let t = s();
        if (e.trigger === `focus` && !t) {
          if (!c()) return;
          g(!1)
        }
      }

      function S() {
        let t = s();
        if (e.trigger === `hover` && !t) {
          if (y(), f.value !== null || c()) return;
          let t = () => {
              g(!0), f.value = null
            },
            {
              delay: n
            } = e;
          n === 0 ? t() : f.value = window.setTimeout(t, n)
        }
      }

      function C() {
        let t = s();
        if (e.trigger === `hover` && !t) {
          if (v(), p.value !== null || !c()) return;
          let t = () => {
              g(!1), p.value = null
            },
            {
              duration: n
            } = e;
          n === 0 ? t() : p.value = window.setTimeout(t, n)
        }
      }

      function w() {
        C()
      }

      function T(t) {
        var n;
        c() && (e.trigger === `click` && (v(), y(), g(!1)), (n = e.onClickoutside) == null || n.call(e, t))
      }

      function E() {
        e.trigger === `click` && !s() && (v(), y(), g(!c()))
      }

      function D(t) {
        e.internalTrapFocus && t.key === `Escape` && (v(), y(), g(!1))
      }

      function O(e) {
        i.value = e
      }

      function k() {
        var e;
        return (e = n.value) == null ? void 0 : e.targetRef
      }

      function A(e) {
        d = e
      }
      return m(`NPopover`, {
        getTriggerElement: k,
        handleKeydown: D,
        handleMouseEnter: S,
        handleMouseLeave: C,
        handleClickOutside: T,
        handleMouseMoveOutside: w,
        setBodyInstance: A,
        positionManuallyRef: h,
        isMountedRef: t,
        zIndexRef: Ke(e, `zIndex`),
        extraClassRef: Ke(e, `internalExtraClass`),
        internalRenderBodyRef: Ke(e, `internalRenderBody`)
      }), ce(() => {
        a.value && s() && g(!1)
      }), {
        binderInstRef: n,
        positionManually: h,
        mergedShowConsideringDisabledProp: o,
        uncontrolledShow: i,
        mergedShowArrow: u,
        getMergedShow: c,
        setShow: O,
        handleClick: E,
        handleMouseEnter: S,
        handleMouseLeave: C,
        handleFocus: b,
        handleBlur: x,
        syncPosition: _
      }
    },
    render() {
      var t;
      let {
        positionManually: n,
        $slots: r
      } = this, i, a = !1;
      if (!n && (i = Sa(r, `trigger`), i)) {
        i = Ce(i), i = i.type === Ie ? z(`span`, [i]) : i;
        let e = {
          onClick: this.handleClick,
          onMouseenter: this.handleMouseEnter,
          onMouseleave: this.handleMouseLeave,
          onFocus: this.handleFocus,
          onBlur: this.handleBlur
        };
        if ((t = i.type) != null && t.__popover__) a = !0, i.props || (i.props = {
          internalSyncTargetWithParent: !0,
          internalInheritedEventHandlers: []
        }), i.props.internalSyncTargetWithParent = !0, i.props.internalInheritedEventHandlers ? i.props.internalInheritedEventHandlers = [e, ...i.props.internalInheritedEventHandlers] : i.props.internalInheritedEventHandlers = [e];
        else {
          let {
            internalInheritedEventHandlers: t
          } = this, r = [e, ...t];
          fc(i, t ? `nested` : n ? `manual` : this.trigger, {
            onBlur: e => {
              r.forEach(t => {
                t.onBlur(e)
              })
            },
            onFocus: e => {
              r.forEach(t => {
                t.onFocus(e)
              })
            },
            onClick: e => {
              r.forEach(t => {
                t.onClick(e)
              })
            },
            onMouseenter: e => {
              r.forEach(t => {
                t.onMouseenter(e)
              })
            },
            onMouseleave: e => {
              r.forEach(t => {
                t.onMouseleave(e)
              })
            }
          })
        }
      }
      return z(Lr, {
        ref: `binderInstRef`,
        syncTarget: !a,
        syncTargetWithParent: this.internalSyncTargetWithParent
      }, {
        default: () => {
          this.mergedShowConsideringDisabledProp;
          let t = this.getMergedShow();
          return [this.internalTrapFocus && t ? e(z(`div`, {
            style: {
              position: `fixed`,
              top: 0,
              right: 0,
              bottom: 0,
              left: 0
            }
          }), [
            [Kr, {
              enabled: t,
              zIndex: this.zIndex
            }]
          ]) : null, n ? null : z(Rr, null, {
            default: () => i
          }), z(lc, wa(this.$props, uc, Object.assign(Object.assign({}, this.$attrs), {
            showArrow: this.mergedShowArrow,
            show: t
          })), {
            default: () => {
              var e, t;
              return (t = (e = this.$slots).default) == null ? void 0 : t.call(e)
            },
            header: () => {
              var e, t;
              return (t = (e = this.$slots).header) == null ? void 0 : t.call(e)
            },
            footer: () => {
              var e, t;
              return (t = (e = this.$slots).footer) == null ? void 0 : t.call(e)
            }
          })]
        }
      })
    }
  }),
  {
    cubicBezierEaseInOut: hc
  } = C;

function gc({
  duration: e = `.2s`,
  delay: t = `.1s`
} = {}) {
  return [p(`&.fade-in-width-expand-transition-leave-from, &.fade-in-width-expand-transition-enter-to`, {
    opacity: 1
  }), p(`&.fade-in-width-expand-transition-leave-to, &.fade-in-width-expand-transition-enter-from`, `
 opacity: 0!important;
 margin-left: 0!important;
 margin-right: 0!important;
 `), p(`&.fade-in-width-expand-transition-leave-active`, `
 overflow: hidden;
 transition:
 opacity ${e} ${hc},
 max-width ${e} ${hc} ${t},
 margin-left ${e} ${hc} ${t},
 margin-right ${e} ${hc} ${t};
 `), p(`&.fade-in-width-expand-transition-enter-active`, `
 overflow: hidden;
 transition:
 opacity ${e} ${hc} ${t},
 max-width ${e} ${hc},
 margin-left ${e} ${hc},
 margin-right ${e} ${hc};
 `)]
}
var _c = N(`base-wave`, `
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 border-radius: inherit;
`),
  vc = l({
    name: `BaseWave`,
    props: {
      clsPrefix: {
        type: String,
        required: !0
      }
    },
    setup(e) {
      Pa(`-base-wave`, _c, Ke(e, `clsPrefix`));
      let t = U(null),
        n = U(!1),
        r = null;
      return g(() => {
        r !== null && window.clearTimeout(r)
      }), {
        active: n,
        selfRef: t,
        play() {
          r !== null && (window.clearTimeout(r), n.value = !1, r = null), w(() => {
            var e;
            (e = t.value) == null || e.offsetHeight, n.value = !0, r = window.setTimeout(() => {
              n.value = !1, r = null
            }, 1e3)
          })
        }
      }
    },
    render() {
      let {
        clsPrefix: e
      } = this;
      return z(`div`, {
        ref: `selfRef`,
        "aria-hidden": !0,
        class: [`${e}-base-wave`, this.active && `${e}-base-wave--active`]
      })
    }
  });

function yc(e) {
  return Oe(e, [255, 255, 255, .16])
}

function bc(e) {
  return Oe(e, [0, 0, 0, .12])
}
var xc = Er && `chrome` in window;
Er && navigator.userAgent.includes(`Firefox`);
var Sc = Er && navigator.userAgent.includes(`Safari`) && !xc,
  Cc = V(`n-button-group`),
  wc = {
    paddingTiny: `0 6px`,
    paddingSmall: `0 10px`,
    paddingMedium: `0 14px`,
    paddingLarge: `0 18px`,
    paddingRoundTiny: `0 10px`,
    paddingRoundSmall: `0 14px`,
    paddingRoundMedium: `0 18px`,
    paddingRoundLarge: `0 22px`,
    iconMarginTiny: `6px`,
    iconMarginSmall: `6px`,
    iconMarginMedium: `6px`,
    iconMarginLarge: `6px`,
    iconSizeTiny: `14px`,
    iconSizeSmall: `18px`,
    iconSizeMedium: `18px`,
    iconSizeLarge: `20px`,
    rippleDuration: `.6s`
  };

function Tc(e) {
  let {
    heightTiny: t,
    heightSmall: n,
    heightMedium: r,
    heightLarge: i,
    borderRadius: a,
    fontSizeTiny: o,
    fontSizeSmall: s,
    fontSizeMedium: c,
    fontSizeLarge: l,
    opacityDisabled: u,
    textColor2: d,
    textColor3: f,
    primaryColorHover: p,
    primaryColorPressed: m,
    borderColor: h,
    primaryColor: g,
    baseColor: _,
    infoColor: v,
    infoColorHover: y,
    infoColorPressed: b,
    successColor: x,
    successColorHover: S,
    successColorPressed: C,
    warningColor: w,
    warningColorHover: T,
    warningColorPressed: E,
    errorColor: D,
    errorColorHover: O,
    errorColorPressed: k,
    fontWeight: A,
    buttonColor2: j,
    buttonColor2Hover: ee,
    buttonColor2Pressed: te,
    fontWeightStrong: M
  } = e;
  return Object.assign(Object.assign({}, wc), {
    heightTiny: t,
    heightSmall: n,
    heightMedium: r,
    heightLarge: i,
    borderRadiusTiny: a,
    borderRadiusSmall: a,
    borderRadiusMedium: a,
    borderRadiusLarge: a,
    fontSizeTiny: o,
    fontSizeSmall: s,
    fontSizeMedium: c,
    fontSizeLarge: l,
    opacityDisabled: u,
    colorOpacitySecondary: `0.16`,
    colorOpacitySecondaryHover: `0.22`,
    colorOpacitySecondaryPressed: `0.28`,
    colorSecondary: j,
    colorSecondaryHover: ee,
    colorSecondaryPressed: te,
    colorTertiary: j,
    colorTertiaryHover: ee,
    colorTertiaryPressed: te,
    colorQuaternary: `#0000`,
    colorQuaternaryHover: ee,
    colorQuaternaryPressed: te,
    color: `#0000`,
    colorHover: `#0000`,
    colorPressed: `#0000`,
    colorFocus: `#0000`,
    colorDisabled: `#0000`,
    textColor: d,
    textColorTertiary: f,
    textColorHover: p,
    textColorPressed: m,
    textColorFocus: p,
    textColorDisabled: d,
    textColorText: d,
    textColorTextHover: p,
    textColorTextPressed: m,
    textColorTextFocus: p,
    textColorTextDisabled: d,
    textColorGhost: d,
    textColorGhostHover: p,
    textColorGhostPressed: m,
    textColorGhostFocus: p,
    textColorGhostDisabled: d,
    border: `1px solid ${h}`,
    borderHover: `1px solid ${p}`,
    borderPressed: `1px solid ${m}`,
    borderFocus: `1px solid ${p}`,
    borderDisabled: `1px solid ${h}`,
    rippleColor: g,
    colorPrimary: g,
    colorHoverPrimary: p,
    colorPressedPrimary: m,
    colorFocusPrimary: p,
    colorDisabledPrimary: g,
    textColorPrimary: _,
    textColorHoverPrimary: _,
    textColorPressedPrimary: _,
    textColorFocusPrimary: _,
    textColorDisabledPrimary: _,
    textColorTextPrimary: g,
    textColorTextHoverPrimary: p,
    textColorTextPressedPrimary: m,
    textColorTextFocusPrimary: p,
    textColorTextDisabledPrimary: d,
    textColorGhostPrimary: g,
    textColorGhostHoverPrimary: p,
    textColorGhostPressedPrimary: m,
    textColorGhostFocusPrimary: p,
    textColorGhostDisabledPrimary: g,
    borderPrimary: `1px solid ${g}`,
    borderHoverPrimary: `1px solid ${p}`,
    borderPressedPrimary: `1px solid ${m}`,
    borderFocusPrimary: `1px solid ${p}`,
    borderDisabledPrimary: `1px solid ${g}`,
    rippleColorPrimary: g,
    colorInfo: v,
    colorHoverInfo: y,
    colorPressedInfo: b,
    colorFocusInfo: y,
    colorDisabledInfo: v,
    textColorInfo: _,
    textColorHoverInfo: _,
    textColorPressedInfo: _,
    textColorFocusInfo: _,
    textColorDisabledInfo: _,
    textColorTextInfo: v,
    textColorTextHoverInfo: y,
    textColorTextPressedInfo: b,
    textColorTextFocusInfo: y,
    textColorTextDisabledInfo: d,
    textColorGhostInfo: v,
    textColorGhostHoverInfo: y,
    textColorGhostPressedInfo: b,
    textColorGhostFocusInfo: y,
    textColorGhostDisabledInfo: v,
    borderInfo: `1px solid ${v}`,
    borderHoverInfo: `1px solid ${y}`,
    borderPressedInfo: `1px solid ${b}`,
    borderFocusInfo: `1px solid ${y}`,
    borderDisabledInfo: `1px solid ${v}`,
    rippleColorInfo: v,
    colorSuccess: x,
    colorHoverSuccess: S,
    colorPressedSuccess: C,
    colorFocusSuccess: S,
    colorDisabledSuccess: x,
    textColorSuccess: _,
    textColorHoverSuccess: _,
    textColorPressedSuccess: _,
    textColorFocusSuccess: _,
    textColorDisabledSuccess: _,
    textColorTextSuccess: x,
    textColorTextHoverSuccess: S,
    textColorTextPressedSuccess: C,
    textColorTextFocusSuccess: S,
    textColorTextDisabledSuccess: d,
    textColorGhostSuccess: x,
    textColorGhostHoverSuccess: S,
    textColorGhostPressedSuccess: C,
    textColorGhostFocusSuccess: S,
    textColorGhostDisabledSuccess: x,
    borderSuccess: `1px solid ${x}`,
    borderHoverSuccess: `1px solid ${S}`,
    borderPressedSuccess: `1px solid ${C}`,
    borderFocusSuccess: `1px solid ${S}`,
    borderDisabledSuccess: `1px solid ${x}`,
    rippleColorSuccess: x,
    colorWarning: w,
    colorHoverWarning: T,
    colorPressedWarning: E,
    colorFocusWarning: T,
    colorDisabledWarning: w,
    textColorWarning: _,
    textColorHoverWarning: _,
    textColorPressedWarning: _,
    textColorFocusWarning: _,
    textColorDisabledWarning: _,
    textColorTextWarning: w,
    textColorTextHoverWarning: T,
    textColorTextPressedWarning: E,
    textColorTextFocusWarning: T,
    textColorTextDisabledWarning: d,
    textColorGhostWarning: w,
    textColorGhostHoverWarning: T,
    textColorGhostPressedWarning: E,
    textColorGhostFocusWarning: T,
    textColorGhostDisabledWarning: w,
    borderWarning: `1px solid ${w}`,
    borderHoverWarning: `1px solid ${T}`,
    borderPressedWarning: `1px solid ${E}`,
    borderFocusWarning: `1px solid ${T}`,
    borderDisabledWarning: `1px solid ${w}`,
    rippleColorWarning: w,
    colorError: D,
    colorHoverError: O,
    colorPressedError: k,
    colorFocusError: O,
    colorDisabledError: D,
    textColorError: _,
    textColorHoverError: _,
    textColorPressedError: _,
    textColorFocusError: _,
    textColorDisabledError: _,
    textColorTextError: D,
    textColorTextHoverError: O,
    textColorTextPressedError: k,
    textColorTextFocusError: O,
    textColorTextDisabledError: d,
    textColorGhostError: D,
    textColorGhostHoverError: O,
    textColorGhostPressedError: k,
    textColorGhostFocusError: O,
    textColorGhostDisabledError: D,
    borderError: `1px solid ${D}`,
    borderHoverError: `1px solid ${O}`,
    borderPressedError: `1px solid ${k}`,
    borderFocusError: `1px solid ${O}`,
    borderDisabledError: `1px solid ${D}`,
    rippleColorError: D,
    waveOpacity: `0.6`,
    fontWeight: A,
    fontWeightStrong: M
  })
}
var Ec = {
    name: `Button`,
    common: De,
    self: Tc
  },
  Dc = p([N(`button`, `
 margin: 0;
 font-weight: var(--n-font-weight);
 line-height: 1;
 font-family: inherit;
 padding: var(--n-padding);
 height: var(--n-height);
 font-size: var(--n-font-size);
 border-radius: var(--n-border-radius);
 color: var(--n-text-color);
 background-color: var(--n-color);
 width: var(--n-width);
 white-space: nowrap;
 outline: none;
 position: relative;
 z-index: auto;
 border: none;
 display: inline-flex;
 flex-wrap: nowrap;
 flex-shrink: 0;
 align-items: center;
 justify-content: center;
 user-select: none;
 -webkit-user-select: none;
 text-align: center;
 cursor: pointer;
 text-decoration: none;
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 opacity .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `, [y(`color`, [f(`border`, {
    borderColor: `var(--n-border-color)`
  }), y(`disabled`, [f(`border`, {
    borderColor: `var(--n-border-color-disabled)`
  })]), Ue(`disabled`, [p(`&:focus`, [f(`state-border`, {
    borderColor: `var(--n-border-color-focus)`
  })]), p(`&:hover`, [f(`state-border`, {
    borderColor: `var(--n-border-color-hover)`
  })]), p(`&:active`, [f(`state-border`, {
    borderColor: `var(--n-border-color-pressed)`
  })]), y(`pressed`, [f(`state-border`, {
    borderColor: `var(--n-border-color-pressed)`
  })])])]), y(`disabled`, {
    backgroundColor: `var(--n-color-disabled)`,
    color: `var(--n-text-color-disabled)`
  }, [f(`border`, {
    border: `var(--n-border-disabled)`
  })]), Ue(`disabled`, [p(`&:focus`, {
    backgroundColor: `var(--n-color-focus)`,
    color: `var(--n-text-color-focus)`
  }, [f(`state-border`, {
    border: `var(--n-border-focus)`
  })]), p(`&:hover`, {
    backgroundColor: `var(--n-color-hover)`,
    color: `var(--n-text-color-hover)`
  }, [f(`state-border`, {
    border: `var(--n-border-hover)`
  })]), p(`&:active`, {
    backgroundColor: `var(--n-color-pressed)`,
    color: `var(--n-text-color-pressed)`
  }, [f(`state-border`, {
    border: `var(--n-border-pressed)`
  })]), y(`pressed`, {
    backgroundColor: `var(--n-color-pressed)`,
    color: `var(--n-text-color-pressed)`
  }, [f(`state-border`, {
    border: `var(--n-border-pressed)`
  })])]), y(`loading`, `cursor: wait;`), N(`base-wave`, `
 pointer-events: none;
 top: 0;
 right: 0;
 bottom: 0;
 left: 0;
 animation-iteration-count: 1;
 animation-duration: var(--n-ripple-duration);
 animation-timing-function: var(--n-bezier-ease-out), var(--n-bezier-ease-out);
 `, [y(`active`, {
    zIndex: 1,
    animationName: `button-wave-spread, button-wave-opacity`
  })]), Er && `MozBoxSizing` in document.createElement(`div`).style ? p(`&::moz-focus-inner`, {
    border: 0
  }) : null, f(`border, state-border`, `
 position: absolute;
 left: 0;
 top: 0;
 right: 0;
 bottom: 0;
 border-radius: inherit;
 transition: border-color .3s var(--n-bezier);
 pointer-events: none;
 `), f(`border`, `
 border: var(--n-border);
 `), f(`state-border`, `
 border: var(--n-border);
 border-color: #0000;
 z-index: 1;
 `), f(`icon`, `
 margin: var(--n-icon-margin);
 margin-left: 0;
 height: var(--n-icon-size);
 width: var(--n-icon-size);
 max-width: var(--n-icon-size);
 font-size: var(--n-icon-size);
 position: relative;
 flex-shrink: 0;
 `, [N(`icon-slot`, `
 height: var(--n-icon-size);
 width: var(--n-icon-size);
 position: absolute;
 left: 0;
 top: 50%;
 transform: translateY(-50%);
 display: flex;
 align-items: center;
 justify-content: center;
 `, [Vs({
    top: `50%`,
    originalTransform: `translateY(-50%)`
  })]), gc()]), f(`content`, `
 display: flex;
 align-items: center;
 flex-wrap: nowrap;
 min-width: 0;
 `, [p(`~`, [f(`icon`, {
    margin: `var(--n-icon-margin)`,
    marginRight: 0
  })])]), y(`block`, `
 display: flex;
 width: 100%;
 `), y(`dashed`, [f(`border, state-border`, {
    borderStyle: `dashed !important`
  })]), y(`disabled`, {
    cursor: `not-allowed`,
    opacity: `var(--n-opacity-disabled)`
  })]), p(`@keyframes button-wave-spread`, {
    from: {
      boxShadow: `0 0 0.5px 0 var(--n-ripple-color)`
    },
    to: {
      boxShadow: `0 0 0.5px 4.5px var(--n-ripple-color)`
    }
  }), p(`@keyframes button-wave-opacity`, {
    from: {
      opacity: `var(--n-wave-opacity)`
    },
    to: {
      opacity: 0
    }
  })]),
  Oc = l({
    name: `Button`,
    props: Object.assign(Object.assign({}, Re.props), {
      color: String,
      textColor: String,
      text: Boolean,
      block: Boolean,
      loading: Boolean,
      disabled: Boolean,
      circle: Boolean,
      size: String,
      ghost: Boolean,
      round: Boolean,
      secondary: Boolean,
      tertiary: Boolean,
      quaternary: Boolean,
      strong: Boolean,
      focusable: {
        type: Boolean,
        default: !0
      },
      keyboard: {
        type: Boolean,
        default: !0
      },
      tag: {
        type: String,
        default: `button`
      },
      type: {
        type: String,
        default: `default`
      },
      dashed: Boolean,
      renderIcon: Function,
      iconPlacement: {
        type: String,
        default: `left`
      },
      attrType: {
        type: String,
        default: `button`
      },
      bordered: {
        type: Boolean,
        default: !0
      },
      onClick: [Function, Array],
      nativeFocusBehavior: {
        type: Boolean,
        default: !Sc
      },
      spinProps: Object
    }),
    slots: Object,
    setup(e) {
      let t = U(null),
        r = U(null),
        i = U(!1),
        a = or(() => !e.quaternary && !e.tertiary && !e.secondary && !e.text && (!e.color || e.ghost || e.dashed) && e.bordered),
        o = n(Cc, {}),
        {
          inlineThemeDisabled: s,
          mergedClsPrefixRef: c,
          mergedRtlRef: l,
          mergedComponentPropsRef: d
        } = h(e),
        {
          mergedSizeRef: f
        } = Ma({}, {
          defaultSize: `medium`,
          mergedSize: t => {
            var n, r;
            let {
              size: i
            } = e;
            if (i) return i;
            let {
              size: a
            } = o;
            if (a) return a;
            let {
              mergedSize: s
            } = t || {};
            return s ? s.value : ((r = (n = d == null ? void 0 : d.value) == null ? void 0 : n.Button) == null ? void 0 : r.size) || `medium`
          }
        }),
        p = F(() => e.focusable && !e.disabled),
        m = n => {
          var r;
          p.value || n.preventDefault(), !e.nativeFocusBehavior && (n.preventDefault(), !e.disabled && p.value && ((r = t.value) == null || r.focus({
            preventScroll: !0
          })))
        },
        g = t => {
          var n;
          if (!e.disabled && !e.loading) {
            let {
              onClick: i
            } = e;
            i && ba(i, t), e.text || (n = r.value) == null || n.play()
          }
        },
        v = t => {
          switch (t.key) {
            case `Enter`:
              if (!e.keyboard) return;
              i.value = !1
          }
        },
        y = t => {
          switch (t.key) {
            case `Enter`:
              if (!e.keyboard || e.loading) {
                t.preventDefault();
                return
              }
              i.value = !0
          }
        },
        b = () => {
          i.value = !1
        },
        x = Re(`Button`, `-button`, Dc, Ec, e, c),
        S = Na(`Button`, l, c),
        C = F(() => {
          let {
            common: {
              cubicBezierEaseInOut: t,
              cubicBezierEaseOut: n
            },
            self: r
          } = x.value, {
            rippleDuration: i,
            opacityDisabled: a,
            fontWeight: o,
            fontWeightStrong: s
          } = r, c = f.value, {
            dashed: l,
            type: u,
            ghost: d,
            text: p,
            color: m,
            round: h,
            circle: g,
            textColor: v,
            secondary: y,
            tertiary: b,
            quaternary: S,
            strong: C
          } = e, w = {
            "--n-font-weight": C ? s : o
          }, T = {
            "--n-color": `initial`,
            "--n-color-hover": `initial`,
            "--n-color-pressed": `initial`,
            "--n-color-focus": `initial`,
            "--n-color-disabled": `initial`,
            "--n-ripple-color": `initial`,
            "--n-text-color": `initial`,
            "--n-text-color-hover": `initial`,
            "--n-text-color-pressed": `initial`,
            "--n-text-color-focus": `initial`,
            "--n-text-color-disabled": `initial`
          }, E = u === `tertiary`, D = u === "default", O = E ? `default` : u;
          if (p) {
            let e = v || m;
            T = {
              "--n-color": `#0000`,
              "--n-color-hover": `#0000`,
              "--n-color-pressed": `#0000`,
              "--n-color-focus": `#0000`,
              "--n-color-disabled": `#0000`,
              "--n-ripple-color": `#0000`,
              "--n-text-color": e || r[_(`textColorText`, O)],
              "--n-text-color-hover": e ? yc(e) : r[_(`textColorTextHover`, O)],
              "--n-text-color-pressed": e ? bc(e) : r[_(`textColorTextPressed`, O)],
              "--n-text-color-focus": e ? yc(e) : r[_(`textColorTextHover`, O)],
              "--n-text-color-disabled": e || r[_(`textColorTextDisabled`, O)]
            }
          } else if (d || l) {
            let e = v || m;
            T = {
              "--n-color": `#0000`,
              "--n-color-hover": `#0000`,
              "--n-color-pressed": `#0000`,
              "--n-color-focus": `#0000`,
              "--n-color-disabled": `#0000`,
              "--n-ripple-color": m || r[_(`rippleColor`, O)],
              "--n-text-color": e || r[_(`textColorGhost`, O)],
              "--n-text-color-hover": e ? yc(e) : r[_(`textColorGhostHover`, O)],
              "--n-text-color-pressed": e ? bc(e) : r[_(`textColorGhostPressed`, O)],
              "--n-text-color-focus": e ? yc(e) : r[_(`textColorGhostHover`, O)],
              "--n-text-color-disabled": e || r[_(`textColorGhostDisabled`, O)]
            }
          } else if (y) {
            let e = D ? r.textColor : E ? r.textColorTertiary : r[_(`color`, O)],
              t = m || e,
              n = u !== "default" && u !== `tertiary`;
            T = {
              "--n-color": n ? Ge(t, {
                alpha: Number(r.colorOpacitySecondary)
              }) : r.colorSecondary,
              "--n-color-hover": n ? Ge(t, {
                alpha: Number(r.colorOpacitySecondaryHover)
              }) : r.colorSecondaryHover,
              "--n-color-pressed": n ? Ge(t, {
                alpha: Number(r.colorOpacitySecondaryPressed)
              }) : r.colorSecondaryPressed,
              "--n-color-focus": n ? Ge(t, {
                alpha: Number(r.colorOpacitySecondaryHover)
              }) : r.colorSecondaryHover,
              "--n-color-disabled": r.colorSecondary,
              "--n-ripple-color": `#0000`,
              "--n-text-color": t,
              "--n-text-color-hover": t,
              "--n-text-color-pressed": t,
              "--n-text-color-focus": t,
              "--n-text-color-disabled": t
            }
          } else if (b || S) {
            let e = D ? r.textColor : E ? r.textColorTertiary : r[_(`color`, O)],
              t = m || e;
            b ? (T[`--n-color`] = r.colorTertiary, T[`--n-color-hover`] = r.colorTertiaryHover, T[`--n-color-pressed`] = r.colorTertiaryPressed, T[`--n-color-focus`] = r.colorSecondaryHover, T[`--n-color-disabled`] = r.colorTertiary) : (T[`--n-color`] = r.colorQuaternary, T[`--n-color-hover`] = r.colorQuaternaryHover, T[`--n-color-pressed`] = r.colorQuaternaryPressed, T[`--n-color-focus`] = r.colorQuaternaryHover, T[`--n-color-disabled`] = r.colorQuaternary), T[`--n-ripple-color`] = `#0000`, T[`--n-text-color`] = t, T[`--n-text-color-hover`] = t, T[`--n-text-color-pressed`] = t, T[`--n-text-color-focus`] = t, T[`--n-text-color-disabled`] = t
          } else T = {
            "--n-color": m || r[_(`color`, O)],
            "--n-color-hover": m ? yc(m) : r[_(`colorHover`, O)],
            "--n-color-pressed": m ? bc(m) : r[_(`colorPressed`, O)],
            "--n-color-focus": m ? yc(m) : r[_(`colorFocus`, O)],
            "--n-color-disabled": m || r[_(`colorDisabled`, O)],
            "--n-ripple-color": m || r[_(`rippleColor`, O)],
            "--n-text-color": v || (m ? r.textColorPrimary : E ? r.textColorTertiary : r[_(`textColor`, O)]),
            "--n-text-color-hover": v || (m ? r.textColorHoverPrimary : r[_(`textColorHover`, O)]),
            "--n-text-color-pressed": v || (m ? r.textColorPressedPrimary : r[_(`textColorPressed`, O)]),
            "--n-text-color-focus": v || (m ? r.textColorFocusPrimary : r[_(`textColorFocus`, O)]),
            "--n-text-color-disabled": v || (m ? r.textColorDisabledPrimary : r[_(`textColorDisabled`, O)])
          };
          let k = {
            "--n-border": `initial`,
            "--n-border-hover": `initial`,
            "--n-border-pressed": `initial`,
            "--n-border-focus": `initial`,
            "--n-border-disabled": `initial`
          };
          k = p ? {
            "--n-border": `none`,
            "--n-border-hover": `none`,
            "--n-border-pressed": `none`,
            "--n-border-focus": `none`,
            "--n-border-disabled": `none`
          } : {
            "--n-border": r[_(`border`, O)],
            "--n-border-hover": r[_(`borderHover`, O)],
            "--n-border-pressed": r[_(`borderPressed`, O)],
            "--n-border-focus": r[_(`borderFocus`, O)],
            "--n-border-disabled": r[_(`borderDisabled`, O)]
          };
          let {
            [_(`height`, c)]: A, [_(`fontSize`, c)]: j, [_(`padding`, c)]: ee, [_(`paddingRound`, c)]: te, [_(`iconSize`, c)]: M, [_(`borderRadius`, c)]: ne, [_(`iconMargin`, c)]: re, waveOpacity: N
          } = r, ie = {
            "--n-width": g && !p ? A : `initial`,
            "--n-height": p ? `initial` : A,
            "--n-font-size": j,
            "--n-padding": g || p ? `initial` : h ? te : ee,
            "--n-icon-size": M,
            "--n-icon-margin": re,
            "--n-border-radius": p ? `initial` : g || h ? A : ne
          };
          return Object.assign(Object.assign(Object.assign(Object.assign({
            "--n-bezier": t,
            "--n-bezier-ease-out": n,
            "--n-ripple-duration": i,
            "--n-opacity-disabled": a,
            "--n-wave-opacity": N
          }, w), T), k), ie)
        }),
        w = s ? u(`button`, F(() => {
          let t = ``,
            {
              dashed: n,
              type: r,
              ghost: i,
              text: a,
              color: o,
              round: s,
              circle: c,
              textColor: l,
              secondary: u,
              tertiary: d,
              quaternary: p,
              strong: m
            } = e;
          n && (t += `a`), i && (t += `b`), a && (t += `c`), s && (t += `d`), c && (t += `e`), u && (t += `f`), d && (t += `g`), p && (t += `h`), m && (t += `i`), o && (t += `j${ga(o)}`), l && (t += `k${ga(l)}`);
          let {
            value: h
          } = f;
          return t += `l${h[0]}`, t += `m${r[0]}`, t
        }), C, e) : void 0;
      return {
        selfElRef: t,
        waveElRef: r,
        mergedClsPrefix: c,
        mergedFocusable: p,
        mergedSize: f,
        showBorder: a,
        enterPressed: i,
        rtlEnabled: S,
        handleMousedown: m,
        handleKeydown: y,
        handleBlur: b,
        handleKeyup: v,
        handleClick: g,
        customColorCssVars: F(() => {
          let {
            color: t
          } = e;
          if (!t) return null;
          let n = yc(t);
          return {
            "--n-border-color": t,
            "--n-border-color-hover": n,
            "--n-border-color-pressed": bc(t),
            "--n-border-color-focus": n,
            "--n-border-color-disabled": t
          }
        }),
        cssVars: s ? void 0 : C,
        themeClass: w == null ? void 0 : w.themeClass,
        onRender: w == null ? void 0 : w.onRender
      }
    },
    render() {
      let {
        mergedClsPrefix: e,
        tag: t,
        onRender: n
      } = this;
      n == null || n();
      let r = Oa(this.$slots.default, t => t && z(`span`, {
        class: `${e}-button__content`
      }, t));
      return z(t, {
        ref: `selfElRef`,
        class: [this.themeClass, `${e}-button`, `${e}-button--${this.type}-type`, `${e}-button--${this.mergedSize}-type`, this.rtlEnabled && `${e}-button--rtl`, this.disabled && `${e}-button--disabled`, this.block && `${e}-button--block`, this.enterPressed && `${e}-button--pressed`, !this.text && this.dashed && `${e}-button--dashed`, this.color && `${e}-button--color`, this.secondary && `${e}-button--secondary`, this.loading && `${e}-button--loading`, this.ghost && `${e}-button--ghost`],
        tabindex: this.mergedFocusable ? 0 : -1,
        type: this.attrType,
        style: this.cssVars,
        disabled: this.disabled,
        onClick: this.handleClick,
        onBlur: this.handleBlur,
        onMousedown: this.handleMousedown,
        onKeyup: this.handleKeyup,
        onKeydown: this.handleKeydown
      }, this.iconPlacement === `right` && r, z(Hs, {
        width: !0
      }, {
        default: () => Oa(this.$slots.icon, t => (this.loading || this.renderIcon || t) && z(`span`, {
          class: `${e}-button__icon`,
          style: {
            margin: ka(this.$slots.default) ? `0` : ``
          }
        }, z(zs, null, {
          default: () => this.loading ? z(Ks, Object.assign({
            clsPrefix: e,
            key: `loading`,
            class: `${e}-icon-slot`,
            strokeWidth: 20
          }, this.spinProps)) : z(`div`, {
            key: `icon`,
            class: `${e}-icon-slot`,
            role: `none`
          }, this.renderIcon ? this.renderIcon() : t)
        })))
      }), this.iconPlacement === `left` && r, this.text ? null : z(vc, {
        ref: `waveElRef`,
        clsPrefix: e
      }), this.showBorder ? z(`div`, {
        "aria-hidden": !0,
        class: `${e}-button__border`,
        style: this.customColorCssVars
      }) : null, this.showBorder ? z(`div`, {
        "aria-hidden": !0,
        class: `${e}-button__state-border`,
        style: this.customColorCssVars
      }) : null)
    }
  }),
  kc = Oc,
  Ac = 0,
  jc = me(`alerts`, () => {
    let e = U([]),
      t = U(!1),
      n = F(() => e.value.filter(e => !e.read).length),
      r = F(() => e.value.slice(0, 20));

    function i(t, n = `info`) {
      e.value.unshift({
        id: ++Ac,
        level: n,
        message: t,
        timestamp: Date.now(),
        read: !1
      }), e.value.length > 100 && e.value.splice(100)
    }

    function a() {
      e.value.forEach(e => e.read = !0)
    }

    function o() {
      e.value = []
    }

    function s() {
      t.value = !0, a()
    }

    function c() {
      t.value = !1
    }
    return {
      alerts: e,
      panelOpen: t,
      unreadCount: n,
      recent: r,
      push: i,
      markAllRead: a,
      clear: o,
      openPanel: s,
      closePanel: c
    }
  });

function Mc(e) {
  "@babel/helpers - typeof";
  return Mc = typeof Symbol == `function` && typeof Symbol.iterator == `symbol` ? function(e) {
    return typeof e
  } : function(e) {
    return e && typeof Symbol == `function` && e.constructor === Symbol && e !== Symbol.prototype ? `symbol` : typeof e
  }, Mc(e)
}

function Nc(e, t) {
  if (Mc(e) != `object` || !e) return e;
  var n = e[Symbol.toPrimitive];
  if (n !== void 0) {
    var r = n.call(e, t || `default`);
    if (Mc(r) != `object`) return r;
    throw TypeError(`@@toPrimitive must return a primitive value.`)
  }
  return (t === `string` ? String : Number)(e)
}

function Pc(e) {
  var t = Nc(e, `string`);
  return Mc(t) == `symbol` ? t : t + ``
}

function J(e, t, n) {
  return (t = Pc(t)) in e ? Object.defineProperty(e, t, {
    value: n,
    enumerable: !0,
    configurable: !0,
    writable: !0
  }) : e[t] = n, e
}
var Fc = ft({
    CRC_EXTRA: () => Ic,
    calcCrc: () => Rc,
    calcCrcWithExtra: () => zc
  }),
  Ic = {
    0: 50,
    1: 124,
    20: 214,
    21: 159,
    22: 220,
    23: 168,
    24: 24,
    26: 170,
    27: 144,
    29: 115,
    30: 39,
    33: 104,
    35: 244,
    36: 222,
    41: 28,
    42: 28,
    43: 148,
    44: 221,
    45: 232,
    47: 153,
    51: 196,
    64: 61,
    65: 118,
    66: 148,
    73: 38,
    74: 20,
    76: 152,
    77: 143,
    100: 175,
    105: 39,
    109: 185,
    132: 85,
    147: 154,
    148: 178,
    241: 90,
    253: 83
  };

function Lc(e, t) {
  let n = t ^ e & 255;
  return n ^= n << 4 & 255, (e >> 8 ^ n << 8 ^ n << 3 ^ n >> 4) & 65535
}

function Rc(e, t, n, r, i) {
  var a;
  let o = 65535,
    s = i === 1 ? 6 + n : 10 + n;
  for (let n = t; n < s; n++) {
    var c;
    o = Lc(o, (c = e[n]) == null ? 0 : c)
  }
  let l = (a = Ic[r]) == null ? 0 : a;
  return o = Lc(o, l), o
}

function zc(e, t, n, r, i) {
  let a = 65535,
    o = r === 1 ? 6 + n : 10 + n;
  for (let n = t; n < o; n++) {
    var s;
    a = Lc(a, (s = e[n]) == null ? 0 : s)
  }
  return a = Lc(a, i), a
}
var Bc = 16384,
  Vc = class {
    constructor(e, t) {
      J(this, `buf`, new Uint8Array(Bc)), J(this, `len`, 0), J(this, `onFrame`, void 0), J(this, `onCrcFail`, void 0), J(this, `_learnedExtra`, new Map), this.onFrame = e, this.onCrcFail = t
    }
    push(e) {
      let t = e instanceof Uint8Array ? e : new Uint8Array(e);
      if (this.len + t.length > Bc) {
        let e = Math.min(this.len, Math.floor(Bc / 2));
        e > 0 && this.buf.copyWithin(0, this.len - e), this.len = e
      }
      this.buf.set(t, this.len), this.len += t.length, this.drain()
    }
    drain() {
      for (; this.len > 0;) {
        var e, t;
        let r = -1;
        for (let e = 0; e < this.len; e++) {
          let t = this.buf[e];
          if (t === 254 || t === 253) {
            r = e;
            break
          }
        }
        if (r === -1) {
          this.len = 0;
          return
        }
        if (r > 0 && (this.buf.copyWithin(0, r), this.len -= r), this.len < 2) return;
        let i = (e = this.buf[0]) == null ? 254 : e,
          a = (t = this.buf[1]) == null ? 0 : t,
          o = i === 254 ? 1 : 2,
          s = o === 1 ? 6 + a + 2 : 10 + a + 2;
        if (this.len < s) return;
        let c = this.buf.slice(0, s),
          l = new DataView(c.buffer, c.byteOffset, c.byteLength),
          u, d, f, p, m;
        o === 1 ? (u = l.getUint8(2), d = l.getUint8(3), f = l.getUint8(4), p = l.getUint8(5), m = 6) : (u = l.getUint8(4), d = l.getUint8(5), f = l.getUint8(6), p = l.getUint8(7) | l.getUint8(8) << 8 | l.getUint8(9) << 16, m = 10);
        let h = m + a,
          g = l.getUint8(h) | l.getUint8(h + 1) << 8;
        if (this.verifyCrc(c, a, p, o, g)) {
          let e = c.buffer.slice(c.byteOffset + m, c.byteOffset + m + a);
          this.onFrame({
            magic: i,
            sysId: d,
            compId: f,
            msgId: p,
            seq: u,
            payload: new DataView(e)
          })
        } else {
          var n;
          (n = this.onCrcFail) == null || n.call(this, p, d, f)
        }
        let _ = o === 2 && this.len >= s + 13 && l.getUint8(2) & 1 ? s + 13 : s;
        this.buf.copyWithin(0, _), this.len -= _
      }
    }
    verifyCrc(e, t, n, r, i) {
      let {
        calcCrc: a,
        calcCrcWithExtra: o,
        CRC_EXTRA: s
      } = Fc;
      if (!(n in s)) return !0;
      let c = this._learnedExtra.get(n);
      if (c !== void 0) {
        if (o(e, 1, t, r, c) === i) return !0;
        this._learnedExtra.delete(n)
      }
      if (a(e, 1, t, n, r) === i) return !0;
      for (let a = 0; a <= 255; a++)
        if (o(e, 1, t, r, a) === i) return this._learnedExtra.set(n, a), !0;
      return !1
    }
  },
  Y = {
    HEARTBEAT: 0,
    SYS_STATUS: 1,
    PARAM_REQUEST_READ: 20,
    PARAM_REQUEST_LIST: 21,
    PARAM_VALUE: 22,
    PARAM_SET: 23,
    GPS_RAW_INT: 24,
    SCALED_IMU: 26,
    RAW_IMU: 27,
    SCALED_PRESSURE: 29,
    ATTITUDE: 30,
    GLOBAL_POSITION_INT: 33,
    RC_CHANNELS_RAW: 35,
    SERVO_OUTPUT_RAW: 36,
    MISSION_SET_CURRENT: 41,
    MISSION_CURRENT: 42,
    MISSION_REQUEST_LIST: 43,
    MISSION_COUNT: 44,
    MISSION_CLEAR_ALL: 45,
    MISSION_ACK: 47,
    MISSION_REQUEST_INT: 51,
    RC_CHANNELS: 65,
    REQUEST_DATA_STREAM: 66,
    MISSION_ITEM_INT: 73,
    VFR_HUD: 74,
    COMMAND_LONG: 76,
    COMMAND_ACK: 77,
    OPTICAL_FLOW: 100,
    HIGHRES_IMU: 105,
    RADIO_STATUS: 109,
    DISTANCE_SENSOR: 132,
    AUTOPILOT_VERSION: 148,
    BATTERY_STATUS: 147,
    ATTITUDE_QUATERNION: 64,
    ACTUATOR_CONTROL_TARGET: 140,
    VIBRATION: 241,
    STATUSTEXT: 253
  },
  Hc = 0,
  Uc = () => Hc = Hc + 1 & 255;

function Wc(e, t) {
  let n = t ^ e & 255;
  return n ^= n << 4 & 255, (e >> 8 ^ n << 8 ^ n << 3 ^ n >> 4) & 65535
}

function Gc(e, t) {
  var n;
  let r = 65535;
  for (let t of e) r = Wc(r, t);
  return r = Wc(r, (n = Ic[t]) == null ? 0 : n), r
}

function Kc(e, t) {
  let n = Uc(),
    r = 6 + t.length + 2,
    i = new Uint8Array(r);
  i[0] = 254, i[1] = t.length, i[2] = n, i[3] = 255, i[4] = 0, i[5] = e, i.set(t, 6);
  let a = Gc(i.slice(1, 6 + t.length), e);
  return i[6 + t.length] = a & 255, i[6 + t.length + 1] = a >> 8 & 255, i
}

function qc(e, t) {
  let n = Uc(),
    r = 10 + t.length + 2,
    i = new Uint8Array(r);
  i[0] = 253, i[1] = t.length, i[2] = 0, i[3] = 0, i[4] = n, i[5] = 255, i[6] = 0, i[7] = e & 255, i[8] = e >> 8 & 255, i[9] = e >> 16 & 255, i.set(t, 10);
  let a = Gc(i.slice(1, 10 + t.length), e);
  return i[10 + t.length] = a & 255, i[10 + t.length + 1] = a >> 8 & 255, i
}

function Jc() {
  let e = new Uint8Array(9),
    t = new DataView(e.buffer);
  return t.setUint32(0, 0, !0), t.setUint8(4, 6), t.setUint8(5, 8), t.setUint8(6, 192), t.setUint8(7, 0), t.setUint8(8, 3), Kc(Y.HEARTBEAT, e)
}

function Yc() {
  let e = new Uint8Array(9),
    t = new DataView(e.buffer);
  return t.setUint32(0, 0, !0), t.setUint8(4, 6), t.setUint8(5, 8), t.setUint8(6, 192), t.setUint8(7, 0), t.setUint8(8, 3), qc(Y.HEARTBEAT, e)
}

function Xc(e) {
  var t, n, r, i, a, o, s;
  let c = new Uint8Array(33),
    l = new DataView(c.buffer);
  return l.setFloat32(0, (t = e.param1) == null ? 0 : t, !0), l.setFloat32(4, (n = e.param2) == null ? 0 : n, !0), l.setFloat32(8, (r = e.param3) == null ? 0 : r, !0), l.setFloat32(12, (i = e.param4) == null ? 0 : i, !0), l.setFloat32(16, (a = e.param5) == null ? 0 : a, !0), l.setFloat32(20, (o = e.param6) == null ? 0 : o, !0), l.setFloat32(24, (s = e.param7) == null ? 0 : s, !0), l.setUint16(28, e.command, !0), l.setUint8(30, e.targetSystem), l.setUint8(31, e.targetComponent), l.setUint8(32, e.confirmation), Kc(Y.COMMAND_LONG, c)
}
var X = {
  ARM_DISARM: 400,
  TAKEOFF: 22,
  LAND: 21,
  RETURN_TO_LAUNCH: 20,
  DO_PAUSE_CONTINUE: 193,
  DO_SET_MODE: 176,
  MISSION_START: 300,
  PREFLIGHT_CALIBRATION: 241,
  ACCELCAL_VEHICLE_POS: 2001,
  PREFLIGHT_REBOOT: 246,
  REQUEST_AUTOPILOT_CAPABILITIES: 520,
  REQUEST_MESSAGE: 512
};

function Zc(e = 1, t = 1) {
  let n = new Uint8Array(2);
  return n[0] = e, n[1] = t, Kc(Y.PARAM_REQUEST_LIST, n)
}

function Qc(e, t = 1, n = 1) {
  let r = new Uint8Array(20),
    i = new DataView(r.buffer);
  r[0] = t, r[1] = n;
  for (let t = 0; t < 16 && t < e.length; t++) r[2 + t] = e.charCodeAt(t);
  return i.setInt16(18, -1, !0), Kc(Y.PARAM_REQUEST_READ, r)
}

function $c(e, t, n = 9, r = 1, i = 1) {
  let a = new Uint8Array(23);
  new DataView(a.buffer).setFloat32(0, t, !0), a[4] = r, a[5] = i;
  for (let t = 0; t < 16 && t < e.length; t++) a[6 + t] = e.charCodeAt(t);
  return a[22] = n, Kc(Y.PARAM_SET, a)
}

function el(e, t, n = 1, r = 1, i = 1) {
  let a = new Uint8Array(6);
  return new DataView(a.buffer).setUint16(0, t, !0), a[2] = r, a[3] = i, a[4] = e, a[5] = n, Kc(Y.REQUEST_DATA_STREAM, a)
}
var tl = {
  ALL: 0,
  RAW_SENSORS: 1,
  EXTENDED_STATUS: 2,
  RC_CHANNELS: 4,
  RAW_CONTROLLER: 5,
  POSITION: 6,
  EXTRA1: 10,
  EXTRA2: 11,
  EXTRA3: 12
};

function Z(e, t) {
  return t + 1 <= e.byteLength ? e.getUint8(t) : 0
}

function nl(e, t) {
  return t + 1 <= e.byteLength ? e.getInt8(t) : 0
}

function Q(e, t) {
  return t + 2 <= e.byteLength ? e.getUint16(t, !0) : 0
}

function rl(e, t) {
  return t + 2 <= e.byteLength ? e.getInt16(t, !0) : 0
}

function il(e, t) {
  return t + 4 <= e.byteLength ? e.getUint32(t, !0) : 0
}

function al(e, t) {
  return t + 4 <= e.byteLength ? e.getInt32(t, !0) : 0
}

function $(e, t) {
  return t + 4 <= e.byteLength ? e.getFloat32(t, !0) : 0
}

function ol(e) {
  return {
    customMode: il(e, 0),
    type: Z(e, 4),
    autopilot: Z(e, 5),
    baseMode: Z(e, 6),
    systemStatus: Z(e, 7),
    mavlinkVersion: Z(e, 8)
  }
}

function sl(e) {
  return {
    sensorsPresent: il(e, 0),
    sensorsEnabled: il(e, 4),
    sensorsHealthy: il(e, 8),
    load: Q(e, 12),
    voltageBattery: Q(e, 14),
    currentBattery: rl(e, 16),
    batteryRemaining: nl(e, 30)
  }
}

function cl(e) {
  return {
    flowX: rl(e, 20),
    flowY: rl(e, 22),
    flowRateX: $(e, 8),
    flowRateY: $(e, 12),
    quality: Z(e, 25),
    groundDistance: $(e, 16)
  }
}

function ll(e) {
  return {
    pressAbs: $(e, 4),
    pressDiff: $(e, 8),
    temperature: rl(e, 12)
  }
}

function ul(e) {
  return {
    minDistance: Q(e, 4),
    maxDistance: Q(e, 6),
    currentDistance: Q(e, 8),
    type: Z(e, 10),
    orientation: Z(e, 12)
  }
}

function dl(e) {
  return {
    fixType: Z(e, 28),
    lat: al(e, 8),
    lon: al(e, 12),
    alt: al(e, 16),
    eph: Q(e, 20),
    epv: Q(e, 22),
    vel: Q(e, 24),
    satellitesVisible: Z(e, 29)
  }
}

function fl(e) {
  return {
    roll: $(e, 4),
    pitch: $(e, 8),
    yaw: $(e, 12),
    rollspeed: $(e, 16),
    pitchspeed: $(e, 20),
    yawspeed: $(e, 24)
  }
}

function pl(e) {
  return {
    lat: al(e, 4),
    lon: al(e, 8),
    alt: al(e, 12),
    relativeAlt: al(e, 16),
    vx: rl(e, 20),
    vy: rl(e, 22),
    vz: rl(e, 24),
    hdg: Q(e, 26)
  }
}

function ml(e) {
  return {
    airspeed: $(e, 0),
    groundspeed: $(e, 4),
    alt: $(e, 8),
    climb: $(e, 12),
    heading: rl(e, 16),
    throttle: Q(e, 18)
  }
}

function hl(e) {
  let t = e.getUint8(0),
    n = [];
  for (let t = 1; t < Math.min(e.byteLength, 51); t++) {
    let r = e.getUint8(t);
    if (r === 0) break;
    n.push(r)
  }
  return {
    severity: t,
    text: String.fromCharCode(...n)
  }
}

function gl(e) {
  let t = $(e, 0),
    n = Q(e, 4),
    r = Q(e, 6),
    i = [];
  for (let t = 8; t < Math.min(24, e.byteLength); t++) {
    let n = e.getUint8(t);
    if (n === 0) break;
    i.push(n)
  }
  return {
    paramValue: t,
    paramCount: n,
    paramIndex: r,
    paramId: String.fromCharCode(...i),
    paramType: Z(e, 24)
  }
}

function _l(e) {
  let t = [];
  for (let n = 0; n < 12; n++) t.push(Q(e, 5 + n * 2));
  return {
    servo: t
  }
}

function vl(e) {
  let t = [];
  for (let n = 0; n < 18; n++) t.push(Q(e, 5 + n * 2));
  return {
    chancount: Z(e, 4),
    channels: t,
    rssi: Z(e, 41)
  }
}

function yl(e) {
  return {
    q1: $(e, 4),
    q2: $(e, 8),
    q3: $(e, 12),
    q4: $(e, 16),
    rollspeed: $(e, 20),
    pitchspeed: $(e, 24),
    yawspeed: $(e, 28)
  }
}

function bl(e) {
  let t = [];
  for (let n = 0; n < 8; n++) t.push(Q(e, 5 + n * 2));
  return {
    channels: t,
    rssi: Z(e, 21)
  }
}
var xl = me(`params`, () => {
    let e = U(new Map),
      t = U(!1),
      n = U(0),
      r = F(() => e.value.size),
      i = F(() => n.value > 0 ? r.value / n.value * 100 : 0),
      a = F(() => n.value > 0 && r.value >= n.value);

    function o(r, i, a, o, s) {
      s > 0 && (n.value = s), e.value.set(r, {
        name: r,
        value: i,
        type: a,
        index: o,
        count: s
      }), n.value > 0 && e.value.size >= n.value && (t.value = !1)
    }

    function s() {
      e.value.clear(), n.value = 0, t.value = !0
    }

    function c() {
      e.value.clear(), n.value = 0, t.value = !1
    }

    function l(t) {
      return e.value.get(t)
    }

    function u(t, n) {
      let r = e.value.get(t);
      r && e.value.set(t, {
        ...r,
        value: n
      })
    }
    return {
      params: e,
      loading: t,
      totalCount: n,
      loadedCount: r,
      progress: i,
      isComplete: a,
      onParamValue: o,
      startLoading: s,
      clear: c,
      getParam: l,
      updateLocal: u
    }
  }),
  Sl = 2e3,
  Cl = {
    0: `HEARTBEAT`,
    1: `SYS_STATUS`,
    20: `PARAM_REQUEST_READ`,
    21: `PARAM_REQUEST_LIST`,
    22: `PARAM_VALUE`,
    23: `PARAM_SET`,
    24: `GPS_RAW_INT`,
    26: `SCALED_IMU`,
    29: `SCALED_PRESSURE`,
    30: `ATTITUDE`,
    33: `GLOBAL_POSITION_INT`,
    36: `SERVO_OUTPUT_RAW`,
    41: `MISSION_SET_CURRENT`,
    43: `MISSION_REQUEST_LIST`,
    44: `MISSION_COUNT`,
    45: `MISSION_CLEAR_ALL`,
    47: `MISSION_ACK`,
    51: `MISSION_REQUEST_INT`,
    65: `RC_CHANNELS`,
    66: `REQUEST_DATA_STREAM`,
    73: `MISSION_ITEM_INT`,
    74: `VFR_HUD`,
    76: `COMMAND_LONG`,
    77: `COMMAND_ACK`,
    105: `HIGHRES_IMU`,
    147: `BATTERY_STATUS`,
    148: `AUTOPILOT_VERSION`,
    191: `MAG_CAL_PROGRESS`,
    192: `MAG_CAL_REPORT`,
    241: `VIBRATION`,
    253: `STATUSTEXT`
  },
  wl = me(`mavConsole`, () => {
    let e = U([]),
      t = U(!1),
      n = 0,
      r = new Map;

    function i(i, a, o, s) {
      var c, l;
      if (t.value) return;
      let u = Date.now(),
        d = (c = r.get(i)) == null ? [] : c;
      d.push(u);
      let f = u - 1e3;
      for (; d.length > 0 && d[0] < f;) d.shift();
      r.set(i, d);
      let p = Math.min(s.length, 16),
        m = ``;
      for (let e = 0; e < p; e++) m += s[e].toString(16).padStart(2, `0`) + ` `;
      s.length > 16 && (m += `…`);
      let h = {
        id: ++n,
        ts: u,
        msgId: i,
        msgName: (l = Cl[i]) == null ? `MSG_${i}` : l,
        sysId: a,
        compId: o,
        len: s.length,
        payload: m.trim()
      };
      e.value.push(h), e.value.length > Sl && e.value.shift()
    }

    function a(r, i, a) {
      var o;
      if (t.value) return;
      let s = (o = Cl[r]) == null ? `MSG_${r}` : o,
        c = {
          id: ++n,
          ts: Date.now(),
          msgId: r,
          msgName: `[CRC?] ${s}`,
          sysId: i,
          compId: a,
          len: 0,
          payload: ``
        };
      e.value.push(c), e.value.length > Sl && e.value.shift()
    }

    function o() {
      e.value = [], r.clear()
    }

    function s(e) {
      var t, n;
      return (t = (n = r.get(e)) == null ? void 0 : n.length) == null ? 0 : t
    }
    return {
      entries: e,
      paused: t,
      push: i,
      pushCrcFail: a,
      clear: o,
      getHz: s
    }
  }),
  Tl = 0,
  El = () => Tl = Tl + 1 & 255,
  Dl = {
    41: 28,
    43: 148,
    44: 221,
    45: 232,
    47: 153,
    51: 196,
    73: 38
  };

function Ol(e, t) {
  let n = t ^ e & 255;
  return n ^= n << 4 & 255, (e >> 8 ^ n << 8 ^ n << 3 ^ n >> 4) & 65535
}

function kl(e, t) {
  var n;
  let r = El(),
    i = 6 + t.length + 2,
    a = new Uint8Array(i);
  a[0] = 254, a[1] = t.length, a[2] = r, a[3] = 255, a[4] = 0, a[5] = e, a.set(t, 6);
  let o = 65535;
  for (let e = 1; e < 6 + t.length; e++) o = Ol(o, a[e]);
  return o = Ol(o, (n = Dl[e]) == null ? 0 : n), a[6 + t.length] = o & 255, a[6 + t.length + 1] = o >> 8 & 255, a
}

function Al(e, t = 1) {
  let n = new Uint8Array(5);
  return new DataView(n.buffer).setUint16(0, e, !0), n[2] = t, n[3] = 1, n[4] = 0, kl(Y.MISSION_COUNT, n)
}

function jl(e, t = 1) {
  let n = new Uint8Array(5);
  return new DataView(n.buffer).setUint16(0, e, !0), n[2] = t, n[3] = 1, n[4] = 0, kl(Y.MISSION_REQUEST_INT, n)
}

function Ml(e = 1) {
  let t = new Uint8Array(3);
  return t[0] = e, t[1] = 1, t[2] = 0, kl(Y.MISSION_REQUEST_LIST, t)
}

function Nl(e = 0, t = 1) {
  let n = new Uint8Array(4);
  return n[0] = t, n[1] = 1, n[2] = e, n[3] = 0, kl(Y.MISSION_ACK, n)
}

function Pl(e = 1) {
  let t = new Uint8Array(3);
  return t[0] = e, t[1] = 1, t[2] = 0, kl(Y.MISSION_CLEAR_ALL, t)
}

function Fl(e, t = 1) {
  let n = new Uint8Array(4);
  return new DataView(n.buffer).setUint16(0, e, !0), n[2] = t, n[3] = 1, kl(Y.MISSION_SET_CURRENT, n)
}

function Il(e, t = 1) {
  var n;
  let r = new Uint8Array(37),
    i = new DataView(r.buffer);
  return i.setFloat32(0, (n = e.holdTime) == null ? 0 : n, !0), i.setFloat32(4, 2, !0), i.setFloat32(8, 0, !0), i.setFloat32(12, 0, !0), i.setInt32(16, Math.round(e.lat * 1e7), !0), i.setInt32(20, Math.round(e.lon * 1e7), !0), i.setFloat32(24, e.alt, !0), i.setUint16(28, e.seq, !0), i.setUint16(30, 16, !0), r[32] = t, r[33] = 1, r[34] = 3, r[35] = +(e.seq === 0), r[36] = 1, kl(Y.MISSION_ITEM_INT, r)
}

function Ll(e) {
  return {
    count: e.getUint16(0, !0)
  }
}

function Rl(e) {
  return {
    seq: e.getUint16(0, !0)
  }
}

function zl(e) {
  return {
    type: e.getUint8(2)
  }
}

function Bl(e) {
  return {
    seq: e.getUint16(28, !0),
    lat: e.getInt32(16, !0) / 1e7,
    lon: e.getInt32(20, !0) / 1e7,
    alt: e.getFloat32(24, !0),
    holdTime: e.getFloat32(0, !0)
  }
}
var Vl = 5e3,
  Hl = 3,
  Ul = class {
    constructor(e, t = 1) {
      J(this, `sendFn`, void 0), J(this, `fcSysId`, void 0), J(this, `uploadState`, `idle`), J(this, `uploadWps`, []), J(this, `uploadNext`, 0), J(this, `uploadTimer`, null), J(this, `uploadRetries`, 0), J(this, `uploadResolve`, void 0), J(this, `uploadReject`, void 0), J(this, `downloadState`, `idle`), J(this, `downloadTotal`, 0), J(this, `downloadItems`, []), J(this, `downloadTimer`, null), J(this, `downloadRetries`, 0), J(this, `downloadResolve`, void 0), J(this, `downloadReject`, void 0), J(this, `onProgress`, void 0), this.sendFn = e, this.fcSysId = t
    }
    upload(e) {
      return this._abortAll(), e.length === 0 ? Promise.resolve(!0) : new Promise((t, n) => {
        this.uploadWps = e, this.uploadNext = 0, this.uploadRetries = 0, this.uploadState = `waiting_request`, this.uploadResolve = t, this.uploadReject = n, this.sendFn(Al(e.length, this.fcSysId)), this._startUploadTimeout(), this._emitProgress(`upload`, 0, e.length, !1, null)
      })
    }
    download() {
      return this._abortAll(), new Promise((e, t) => {
        this.downloadState = `waiting_count`, this.downloadTotal = 0, this.downloadItems = [], this.downloadRetries = 0, this.downloadResolve = e, this.downloadReject = t, this.sendFn(Ml(this.fcSysId)), this._startDownloadTimeout(), this._emitProgress(`download`, 0, 0, !1, null)
      })
    }
    handleFrame(e, t) {
      switch (e) {
        case Y.MISSION_REQUEST_INT: {
          let {
            seq: e
          } = Rl(t);
          this._onMissionRequest(e);
          break
        }
        case 40: {
          let e = t.getUint16(0, !0);
          this._onMissionRequest(e);
          break
        }
        case Y.MISSION_ACK: {
          let {
            type: e
          } = zl(t);
          this._onMissionAck(e);
          break
        }
        case Y.MISSION_COUNT: {
          let {
            count: e
          } = Ll(t);
          this._onMissionCount(e);
          break
        }
        case Y.MISSION_ITEM_INT: {
          let e = Bl(t);
          this._onMissionItemInt(e);
          break
        }
      }
    }
    _onMissionRequest(e) {
      if (this.uploadState !== `waiting_request`) return;
      this._clearUploadTimeout(), this.uploadRetries = 0, e !== this.uploadNext && (this.uploadNext = e);
      let t = this.uploadWps[e];
      if (!t) {
        this._failUpload(Error(`无效航点序号 ${e}`));
        return
      }
      this.sendFn(Il(t, this.fcSysId)), this.uploadNext = e + 1, this._emitProgress(`upload`, e + 1, this.uploadWps.length, !1, null), this._startUploadTimeout()
    }
    _onMissionAck(e) {
      if (this.uploadState === `waiting_request`)
        if (this._clearUploadTimeout(), e === 0) {
          var t;
          this.uploadState = `done`, this._emitProgress(`upload`, this.uploadWps.length, this.uploadWps.length, !0, null), (t = this.uploadResolve) == null || t.call(this, !0)
        } else this._failUpload(Error(`MISSION_ACK type=${e}`))
    }
    _startUploadTimeout() {
      this.uploadTimer = setTimeout(() => {
        if (this.uploadRetries++ < Hl) {
          if (this.uploadNext === 0) this.sendFn(Al(this.uploadWps.length, this.fcSysId));
          else {
            let e = this.uploadWps[this.uploadNext - 1];
            e && this.sendFn(Il(e, this.fcSysId))
          }
          this._startUploadTimeout()
        } else this._failUpload(Error(`上传超时`))
      }, Vl)
    }
    _clearUploadTimeout() {
      this.uploadTimer && (clearTimeout(this.uploadTimer), this.uploadTimer = null)
    }
    _failUpload(e) {
      var t;
      this._clearUploadTimeout(), this.uploadState = `error`, this._emitProgress(`upload`, this.uploadNext, this.uploadWps.length, !1, e.message), (t = this.uploadReject) == null || t.call(this, e)
    }
    _onMissionCount(e) {
      if (this.downloadState === `waiting_count`) {
        if (this._clearDownloadTimeout(), e === 0) {
          var t;
          this.downloadState = `done`, this._emitProgress(`download`, 0, 0, !0, null), this.sendFn(Nl(0, this.fcSysId)), (t = this.downloadResolve) == null || t.call(this, []);
          return
        }
        this.downloadTotal = e, this.downloadItems = [], this.downloadState = `waiting_items`, this.downloadRetries = 0, this.sendFn(jl(0, this.fcSysId)), this._startDownloadTimeout(), this._emitProgress(`download`, 0, e, !1, null)
      }
    }
    _onMissionItemInt(e) {
      if (this.downloadState !== `waiting_items`) return;
      this._clearDownloadTimeout(), this.downloadRetries = 0, this.downloadItems[e.seq] = e;
      let t = this.downloadItems.filter(Boolean).length;
      if (this._emitProgress(`download`, t, this.downloadTotal, !1, null), t >= this.downloadTotal) {
        var n;
        this.sendFn(Nl(0, this.fcSysId)), this.downloadState = `done`;
        let e = this.downloadItems.filter(Boolean).sort((e, t) => e.seq - t.seq);
        this._emitProgress(`download`, t, this.downloadTotal, !0, null), (n = this.downloadResolve) == null || n.call(this, e)
      } else this.sendFn(jl(e.seq + 1, this.fcSysId)), this._startDownloadTimeout()
    }
    _startDownloadTimeout() {
      this.downloadTimer = setTimeout(() => {
        if (this.downloadRetries++ < Hl) {
          if (this.downloadState === `waiting_count`) this.sendFn(Ml(this.fcSysId));
          else {
            let e = this.downloadItems.filter(Boolean).length;
            this.sendFn(jl(e, this.fcSysId))
          }
          this._startDownloadTimeout()
        } else this._failDownload(Error(`下载超时`))
      }, Vl)
    }
    _clearDownloadTimeout() {
      this.downloadTimer && (clearTimeout(this.downloadTimer), this.downloadTimer = null)
    }
    _failDownload(e) {
      var t;
      this._clearDownloadTimeout(), this.downloadState = `error`, this._emitProgress(`download`, 0, this.downloadTotal, !1, e.message), (t = this.downloadReject) == null || t.call(this, e)
    }
    _emitProgress(e, t, n, r, i) {
      var a;
      (a = this.onProgress) == null || a.call(this, {
        phase: e,
        current: t,
        total: n,
        done: r,
        error: i
      })
    }
    _abortAll() {
      this._clearUploadTimeout(), this._clearDownloadTimeout(), this.uploadState = `idle`, this.downloadState = `idle`
    }
    destroy() {
      this._abortAll()
    }
  },
  Wl = `cgc_mission_current`,
  Gl = `cgc_missions_saved`;

function Kl() {
  try {
    let e = localStorage.getItem(Wl);
    if (e) return JSON.parse(e)
  } catch {}
  return {
    name: `未命名任务`,
    waypoints: []
  }
}

function ql() {
  try {
    let e = localStorage.getItem(Gl);
    if (e) return JSON.parse(e)
  } catch {}
  return []
}
var Jl = me(`mission`, () => {
    let e = U(Kl()),
      t = U(ql()),
      n = U(!1),
      r = U(!1),
      i = U(-1);

    function a() {
      localStorage.setItem(Wl, JSON.stringify(e.value))
    }

    function o() {
      localStorage.setItem(Gl, JSON.stringify(t.value))
    }

    function s(t, r, i = 30) {
      e.value.waypoints.push({
        seq: e.value.waypoints.length,
        lat: t,
        lon: r,
        alt: i,
        speed: 5,
        holdTime: 0,
        action: `none`
      }), n.value = !0, a()
    }

    function c(t, r) {
      let i = e.value.waypoints.find(e => e.seq === t);
      i && (Object.assign(i, r), n.value = !0, a())
    }

    function l(t) {
      e.value.waypoints = e.value.waypoints.filter(e => e.seq !== t).map((e, t) => ({
        ...e,
        seq: t
      })), n.value = !0, a()
    }

    function u() {
      e.value = {
        name: `未命名任务`,
        waypoints: []
      }, n.value = !1, a()
    }

    function d(t) {
      e.value = {
        ...t
      }, n.value = !1, a()
    }

    function f() {
      let r = {
          ...e.value,
          id: Date.now(),
          updatedAt: new Date().toISOString()
        },
        i = t.value.findIndex(t => t.id === e.value.id);
      i >= 0 ? t.value[i] = r : t.value.unshift(r), e.value = r, n.value = !1, o(), a()
    }

    function p(e) {
      let n = t.value.find(t => t.id === e);
      n && d(n)
    }

    function m(e) {
      t.value = t.value.filter(t => t.id !== e), o()
    }
    return {
      current: e,
      savedMissions: t,
      isDirty: n,
      isUploading: r,
      activeMissionSeq: i,
      addWaypoint: s,
      updateWaypoint: c,
      removeWaypoint: l,
      clearMission: u,
      loadMission: d,
      saveCurrent: f,
      loadSavedMission: p,
      deleteSaved: m
    }
  }),
  Yl = `cgc-v1`,
  Xl = 1,
  Zl = null;

function Ql() {
  return Zl ? Promise.resolve(Zl) : new Promise((e, t) => {
    let n = indexedDB.open(Yl, Xl);
    n.onupgradeneeded = e => {
      let t = e.target.result;
      t.objectStoreNames.contains(`flights`) || t.createObjectStore(`flights`, {
        keyPath: `id`,
        autoIncrement: !0
      }), t.objectStoreNames.contains(`telemetry`) || t.createObjectStore(`telemetry`, {
        keyPath: `id`,
        autoIncrement: !0
      }).createIndex(`by_flight`, `flight_id`, {
        unique: !1
      })
    }, n.onsuccess = () => {
      Zl = n.result, Zl.onclose = () => {
        Zl = null
      }, e(Zl)
    }, n.onerror = () => t(n.error)
  })
}

function $l(e, t) {
  return new Promise(async (n, r) => {
    let i = (await Ql()).transaction(e, `readwrite`).objectStore(e).add(t);
    i.onsuccess = () => n(i.result), i.onerror = () => r(i.error)
  })
}
var eu = !1;

function tu() {
  eu || (eu = !0, jc().push(`飞行日志写入本地存储失败，部分遥测记录可能丢失（浏览器存储空间不足或隐私模式限制）`, `warning`))
}

function nu(e, t) {
  return new Promise(async (n, r) => {
    let i = (await Ql()).transaction(e, `readwrite`).objectStore(e).put(t);
    i.onsuccess = () => n(i.result), i.onerror = () => r(i.error)
  })
}

function ru(e, t) {
  return new Promise(async (n, r) => {
    let i = (await Ql()).transaction(e, `readonly`).objectStore(e).get(t);
    i.onsuccess = () => n(i.result), i.onerror = () => r(i.error)
  })
}

function iu(e) {
  return new Promise(async (t, n) => {
    let r = (await Ql()).transaction(e, `readonly`).objectStore(e).getAll();
    r.onsuccess = () => t(r.result), r.onerror = () => n(r.error)
  })
}

function au(e, t, n) {
  return new Promise(async (r, i) => {
    let a = (await Ql()).transaction(e, `readonly`).objectStore(e).index(t).getAll(n);
    a.onsuccess = () => r(a.result), a.onerror = () => i(a.error)
  })
}
var ou = me(`flightLog`, () => {
  let e = U(null),
    t = 0,
    n = 0,
    r = 0,
    i = 0,
    a = !1;
  async function o(o, s) {
    t = 0, n = 0, r = 0, i = 0, a = !1, e.value = await $l(`flights`, {
      started_at: Date.now(),
      max_alt: 0,
      total_dist: 0,
      vehicle_type: o,
      firmware_ver: s
    })
  }
  async function s() {
    let r = e.value;
    if (r === null) return;
    e.value = null;
    let i = await ru(`flights`, r);
    i && (i.id = r, i.ended_at = Date.now(), i.max_alt = t, i.total_dist = n, await nu(`flights`, i))
  }

  function c(o) {
    let s = e.value;
    s !== null && (o.alt_rel > t && (t = o.alt_rel), o.lat && o.lon && (a ? (n += su(r, i, o.lat, o.lon), r = o.lat, i = o.lon) : (r = o.lat, i = o.lon, a = !0)), $l(`telemetry`, {
      ...o,
      flight_id: s
    }).catch(e => {
      console.error(`[FlightLog] 遥测写入 IndexedDB 失败：`, e), tu()
    }))
  }
  async function l() {
    return (await iu(`flights`)).map(e => ({
      ...e,
      duration_s: e.ended_at ? Math.round((e.ended_at - e.started_at) / 1e3) : void 0
    })).sort((e, t) => t.started_at - e.started_at)
  }
  async function u(e) {
    return (await au(`telemetry`, `by_flight`, e)).sort((e, t) => e.ts - t.ts)
  }
  async function d(e) {
    let t = await Ql();
    await new Promise((n, r) => {
      let i = t.transaction([`flights`, `telemetry`], `readwrite`);
      i.objectStore(`flights`).delete(e);
      let a = i.objectStore(`telemetry`).index(`by_flight`).openCursor(IDBKeyRange.only(e));
      a.onsuccess = e => {
        let t = e.target.result;
        t && (t.delete(), t.continue())
      }, i.oncomplete = () => n(), i.onerror = () => r(i.error)
    })
  }
  async function f() {
    let e = await Ql();
    await new Promise((t, n) => {
      let r = e.transaction([`flights`, `telemetry`], `readwrite`);
      r.objectStore(`flights`).clear(), r.objectStore(`telemetry`).clear(), r.oncomplete = () => t(), r.onerror = () => n(r.error)
    })
  }
  return {
    activeFlightId: e,
    startFlight: o,
    endFlight: s,
    writeTel: c,
    listFlights: l,
    getFlightTelemetry: u,
    deleteFlight: d,
    clearAll: f
  }
});

function su(e, t, n, r) {
  let i = e * Math.PI / 180,
    a = n * Math.PI / 180,
    o = (n - e) * Math.PI / 180,
    s = (r - t) * Math.PI / 180,
    c = Math.sin(o / 2) ** 2 + Math.cos(i) * Math.cos(a) * Math.sin(s / 2) ** 2;
  return 6371e3 * 2 * Math.atan2(Math.sqrt(c), Math.sqrt(1 - c))
}
var cu = 1e3,
  lu = 3e3,
  uu = 6e3,
  du = class {
    constructor() {
      J(this, `ws`, null), J(this, `parser`, void 0), J(this, `heartbeatTimer`, null), J(this, `logSampleTimer`, null), J(this, `reconnectTimer`, null), J(this, `connectTimeoutTimer`, null), J(this, `destroyed`, !1), J(this, `url`, ``), J(this, `fcSysId`, 1), J(this, `mission`, void 0), this.parser = new Vc(this.handleFrame.bind(this), (e, t, n) => wl().pushCrcFail(e, t, n)), this.mission = new Ul(this.send.bind(this), 1)
    }
    connect(e, t) {
      this.url = `ws://${e}:${t}`, this.destroyed = !1, this._connect()
    }
    disconnect() {
      this.destroyed = !0, this._cleanup(), ou().endFlight().catch(() => {}), H().setStatus(`disconnected`), B().stopRefresh(), xl().clear()
    }
    send(e) {
      var t;
      return ((t = this.ws) == null ? void 0 : t.readyState) === WebSocket.OPEN ? (this.ws.send(e.buffer.slice(e.byteOffset, e.byteOffset + e.byteLength)), !0) : !1
    }
    _connect() {
      let e = H();
      e.setStatus(`connecting`);
      try {
        this.ws = new WebSocket(this.url), this.ws.binaryType = `arraybuffer`, this.connectTimeoutTimer = setTimeout(() => {
          var t;
          if (((t = this.ws) == null ? void 0 : t.readyState) !== WebSocket.OPEN) {
            var n;
            (n = this.ws) == null || n.close(), e.setStatus(`error`), this._scheduleReconnect()
          }
        }, uu), this.ws.onopen = this._onOpen.bind(this), this.ws.onmessage = this._onMessage.bind(this), this.ws.onerror = this._onError.bind(this), this.ws.onclose = this._onClose.bind(this)
      } catch {
        e.setStatus(`error`), this._scheduleReconnect()
      }
    }
    _onOpen() {
      this.connectTimeoutTimer && (clearTimeout(this.connectTimeoutTimer), this.connectTimeoutTimer = null), H().setStatus(`connected`), B().startRefresh(), xl().clear(), this._sendHeartbeat(), this.heartbeatTimer = setInterval(this._sendHeartbeat.bind(this), cu);
      let e = H();
      ou().startFlight(e.vehicleType || `CF-Drone`, e.firmwareVersion || ``).catch(() => {}), this.logSampleTimer = setInterval(() => this._sampleTelemetry(), 1e3), setTimeout(() => this._configDataStreams(), 500)
    }
    _onMessage(e) {
      e.data instanceof ArrayBuffer && this.parser.push(e.data)
    }
    _onError() {
      H().setStatus(`error`)
    }
    _onClose() {
      this._cleanup();
      let e = H();
      this.destroyed || (e.setStatus(`error`), B().stopRefresh(), this._scheduleReconnect())
    }
    _sendHeartbeat() {
      this.send(Jc())
    }
    _configDataStreams() {}
    _scheduleReconnect() {
      this.destroyed || (this.reconnectTimer = setTimeout(() => {
        this.destroyed || this._connect()
      }, lu))
    }
    _cleanup() {
      this.heartbeatTimer && (clearInterval(this.heartbeatTimer), this.heartbeatTimer = null), this.logSampleTimer && (clearInterval(this.logSampleTimer), this.logSampleTimer = null), this.reconnectTimer && (clearTimeout(this.reconnectTimer), this.reconnectTimer = null), this.connectTimeoutTimer && (clearTimeout(this.connectTimeoutTimer), this.connectTimeoutTimer = null), this.ws && (this.ws.onopen = this.ws.onmessage = this.ws.onerror = this.ws.onclose = null, this.ws.readyState < WebSocket.CLOSING && this.ws.close(), this.ws = null)
    }
    _sampleTelemetry() {
      let e = B().state,
        t = e.lat ? e.lat / 1e7 : 0,
        n = e.lon ? e.lon / 1e7 : 0;
      ou().writeTel({
        ts: Date.now(),
        roll: e.roll,
        pitch: e.pitch,
        yaw: e.yaw,
        lat: t,
        lon: n,
        alt_rel: e.relativeAlt / 1e3,
        speed: e.groundspeed / 100,
        bat_v: e.voltage / 1e3,
        bat_pct: e.remaining,
        sats: e.satellites,
        hdop: e.hdop,
        rssi: e.rssi,
        mode: e.mode
      })
    }
    handleFrame(e) {
      let t = B(),
        n = H(),
        {
          msgId: r,
          sysId: i,
          compId: a,
          payload: o
        } = e;
      switch (wl().push(r, i, a == null ? 0 : a, new Uint8Array(o.buffer, o.byteOffset, o.byteLength)), r) {
        case Y.HEARTBEAT: {
          let e = ol(o);
          if (i !== fu) {
            this.fcSysId = i;
            let r = hu(e.baseMode, e.customMode),
              a = !!(e.baseMode & 128);
            n.setDeviceInfo(`飞控 (SYS:${i})`, `MAVLink ${e.mavlinkVersion}`, pu(e.type)), t.patch({
              mode: r,
              armed: a
            })
          }
          break
        }
        case Y.SYS_STATUS: {
          let e = sl(o);
          t.patch({
            voltage: e.voltageBattery,
            current: e.currentBattery < 0 ? 0 : e.currentBattery,
            remaining: e.batteryRemaining,
            sensorsPresent: e.sensorsPresent,
            sensorsEnabled: e.sensorsEnabled,
            sensorsHealthy: e.sensorsHealthy
          });
          break
        }
        case Y.GPS_RAW_INT: {
          let e = dl(o);
          t.patch({
            lat: e.lat,
            lon: e.lon,
            alt: e.alt,
            fixType: e.fixType,
            satellites: e.satellitesVisible,
            hdop: e.eph === 65535 ? 9999 : e.eph / 100
          });
          break
        }
        case Y.ATTITUDE: {
          let e = fl(o);
          t.patch({
            roll: e.roll,
            pitch: e.pitch,
            yaw: e.yaw
          });
          break
        }
        case Y.ATTITUDE_QUATERNION: {
          let e = yl(o),
            n = e.q1,
            r = e.q2,
            i = e.q3,
            a = e.q4,
            s = Math.atan2(2 * (n * r + i * a), 1 - 2 * (r * r + i * i)),
            c = Math.asin(Math.max(-1, Math.min(1, 2 * (n * i - a * r)))),
            l = Math.atan2(2 * (n * a + r * i), 1 - 2 * (i * i + a * a));
          t.patch({
            roll: s,
            pitch: c,
            yaw: l
          });
          break
        }
        case Y.GLOBAL_POSITION_INT: {
          let e = pl(o);
          t.patch({
            lat: e.lat,
            lon: e.lon,
            alt: e.alt,
            relativeAlt: e.relativeAlt,
            vz: e.vz,
            heading: e.hdg / 100
          });
          break
        }
        case Y.VFR_HUD: {
          let e = ml(o);
          t.patch({
            airspeed: Math.round(e.airspeed * 100),
            groundspeed: Math.round(e.groundspeed * 100),
            vz: Math.round(-e.climb * 100),
            heading: e.heading,
            relativeAlt: Math.round(e.alt * 1e3)
          });
          break
        }
        case Y.RC_CHANNELS: {
          let e = vl(o);
          t.patch({
            rssi: e.rssi,
            rcChannels: e.channels,
            rcChanCount: e.chancount
          });
          break
        }
        case Y.RC_CHANNELS_RAW: {
          let e = bl(o),
            n = [...e.channels, ...Array(10).fill(0)];
          t.patch({
            rcChannels: n,
            rcChanCount: 8,
            rssi: e.rssi
          });
          break
        }
        case Y.SERVO_OUTPUT_RAW: {
          let e = _l(o);
          t.patch({
            servoOutputs: e.servo
          });
          break
        }
        case Y.ACTUATOR_CONTROL_TARGET: {
          let e = Array(12).fill(0);
          for (let t = 0; t < 4; t++) {
            let n = o.byteLength >= 8 + t * 4 + 4 ? o.getFloat32(4 + t * 4, !0) : 0;
            e[t] = n > .001 ? Math.round(1e3 + n * 1e3) : 0
          }
          t.patch({
            servoOutputs: e
          });
          break
        }
        case Y.OPTICAL_FLOW: {
          let e = cl(o),
            n = isNaN(e.groundDistance) || e.groundDistance < 0 ? -1 : e.groundDistance;
          t.patch({
            optFlowQuality: e.quality,
            optFlowRateX: e.flowRateX,
            optFlowRateY: e.flowRateY,
            optFlowGroundDist: n
          });
          break
        }
        case Y.DISTANCE_SENSOR: {
          let e = ul(o);
          t.patch({
            rangePresent: !0,
            rangeDistance: e.currentDistance >= 65535 ? -1 : e.currentDistance
          });
          break
        }
        case Y.SCALED_PRESSURE: {
          let e = ll(o);
          t.patch({
            baroPresAbs: e.pressAbs,
            baroTemp: e.temperature
          });
          break
        }
        case Y.PARAM_VALUE: {
          let e = gl(o);
          xl().onParamValue(e.paramId, e.paramValue, e.paramType, e.paramIndex, e.paramCount);
          break
        }
        case Y.STATUSTEXT: {
          let e = hl(o),
            t = e.severity <= 2 ? `danger` : e.severity <= 4 ? `warning` : `info`;
          jc().push(e.text, t);
          break
        }
        case Y.MISSION_CURRENT: {
          let e = o.getUint16(0, !0);
          Jl().activeMissionSeq = e;
          break
        }
        case Y.MISSION_COUNT:
        case Y.MISSION_REQUEST_INT:
        case Y.MISSION_ITEM_INT:
        case Y.MISSION_ACK:
        case 40:
          this.mission.handleFrame(r, o);
          break
      }
    }
  },
  fu = 255;

function pu(e) {
  var t;
  return (t = {
    0: `通用`,
    1: `固定翼`,
    2: `四旋翼`,
    3: `直升机`,
    13: `六旋翼`,
    14: `八旋翼`,
    27: `多旋翼 VTOL`
  } [e]) == null ? `MAV_TYPE(${e})` : t
}
var mu = {
  0: `RAW`,
  1: `ACRO`,
  2: `STAB`,
  3: `ALTHOLD`,
  4: `AUTO`,
  5: `POSHOLD`
};

function hu(e, t) {
  let n = mu[t];
  return n === void 0 ? e & 1 ? `CUSTOM(${t})` : e & 4 ? `AUTO` : e & 8 ? `GUIDED` : `MODE(${e})` : n
}
var gu = 1e3,
  _u = class {
    constructor() {
      J(this, `port`, null), J(this, `writer`, null), J(this, `_reader`, null), J(this, `parser`, void 0), J(this, `heartbeatTimer`, null), J(this, `_watchdog`, null), J(this, `_connGen`, 0), J(this, `destroyed`, !1), J(this, `fcSysId`, 1), J(this, `mission`, void 0), this.parser = new Vc(this.handleFrame.bind(this), (e, t, n) => wl().pushCrcFail(e, t, n)), this.mission = new Ul(this.send.bind(this), 1)
    }
    async connect(e) {
      if (!(`serial` in navigator)) return jc().push(`当前浏览器不支持 Web Serial API，请使用 Chrome 或 Edge`, `danger`), !1;
      let t = H();
      t.setStatus(`connecting`), this.destroyed = !1;
      try {
        this.port = await navigator.serial.requestPort()
      } catch (e) {
        return e.name === `NotFoundError` || e.name === `AbortError` ? (t.setStatus(`disconnected`), !1) : (t.setStatus(`error`), jc().push(`串口打开失败：${e.message}`, `danger`), !1)
      }
      try {
        await this.port.open({
          baudRate: e,
          flowControl: `none`
        })
      } catch (e) {
        return t.setStatus(`error`), jc().push(`串口打开失败：${e.message}`, `danger`), this.port = null, !1
      }
      return this._afterPortOpened(t), !0
    }
    disconnect() {
      this.destroyed = !0, this._cleanup(), H().setStatus(`disconnected`), B().stopRefresh(), xl().clear()
    }
    async connectAuto(e) {
      if (!(`serial` in navigator)) return !1;
      let t = await navigator.serial.getPorts();
      if (!t.length) return !1;
      let n = H();
      n.setStatus(`connecting`), this.destroyed = !1, this.port = t[0];
      try {
        await this.port.open({
          baudRate: e,
          flowControl: `none`
        })
      } catch {
        return n.setStatus(`disconnected`), this.port = null, !1
      }
      return this._afterPortOpened(n), !0
    }
    _afterPortOpened(e) {
      e.setStatus(`connected`), B().startRefresh(), xl().clear(), this.port.writable && (this.writer = this.port.writable.getWriter()), this._sendHeartbeat(), [500, 1e3, 1500, 2e3, 3e3].forEach(e => setTimeout(() => {
        this.destroyed || this._sendHeartbeat()
      }, e)), this.heartbeatTimer = setInterval(this._sendHeartbeat.bind(this), gu), setTimeout(() => this._configDataStreams(), 500), this._startActivationWatchdog(), this._startReadLoop(++this._connGen)
    }
    send(e) {
      if (!this.writer) return console.warn(`[CGC Serial] send(): writer 为 null，跳过发送`), !1;
      let t = new Uint8Array(e.buffer.slice(e.byteOffset, e.byteOffset + e.byteLength));
      return this.writer.write(t).catch(e => {
        console.error(`[CGC Serial] write() 失败：`, e), this.destroyed || H().setStatus(`error`)
      }), !0
    }
    async _startReadLoop(e) {
      var t;
      if (!((t = this.port) != null && t.readable)) return;
      let n = this.port;
      this._reader = n.readable.getReader();
      let r = this._reader;
      try {
        for (; !this.destroyed;) {
          let {
            value: e,
            done: t
          } = await r.read();
          if (t) break;
          e && this.parser.push(e.buffer.slice(e.byteOffset, e.byteOffset + e.byteLength))
        }
      } catch {} finally {
        try {
          r.releaseLock()
        } catch {}
        e === this._connGen && (this._reader = null, this.port = null);
        try {
          await n.close()
        } catch {}
      }!this.destroyed && e === this._connGen && (H().setStatus(`error`), B().stopRefresh(), jc().push(`串口连接已断开`, `warning`))
    }
    _sendHeartbeat() {
      this.send(Yc()), this.send(Jc())
    }
    _startActivationWatchdog() {
      let e = 0;
      this._watchdog = setInterval(() => {
        if (this.destroyed || B().hasData) {
          clearInterval(this._watchdog), this._watchdog = null;
          return
        }
        e++, e <= 6 ? (console.warn(`[CGC Serial] 飞控未响应，第 ${e} 次尝试重置写入通道...`), this._reinitWriter()) : (console.warn(`[CGC Serial] 看门狗超时，已达最大重试次数`), clearInterval(this._watchdog), this._watchdog = null)
      }, 5e3)
    }
    _reinitWriter() {
      var e;
      if ((e = this.port) != null && e.writable) {
        try {
          var t;
          (t = this.writer) == null || t.releaseLock(), this.writer = this.port.writable.getWriter(), console.warn(`[CGC Serial] 写入通道已重置，密集发送 HEARTBEAT`)
        } catch (e) {
          console.error(`[CGC Serial] 重置写入通道失败：`, e);
          return
        }
        for (let e = 0; e <= 5; e++) setTimeout(() => {
          this.destroyed || this._sendHeartbeat()
        }, e * 600)
      }
    }
    _configDataStreams() {}
    _cleanup() {
      this.heartbeatTimer && (clearInterval(this.heartbeatTimer), this.heartbeatTimer = null), this._watchdog && (clearInterval(this._watchdog), this._watchdog = null);
      try {
        var e;
        (e = this._reader) == null || e.cancel()
      } catch {}
      try {
        var t;
        (t = this.writer) == null || t.releaseLock()
      } catch {}
      this.writer = null
    }
    handleFrame(e) {
      let t = B(),
        n = H(),
        {
          msgId: r,
          sysId: i,
          compId: a,
          payload: o
        } = e;
      switch (wl().push(r, i, a == null ? 0 : a, new Uint8Array(o.buffer, o.byteOffset, o.byteLength)), r) {
        case Y.HEARTBEAT: {
          let e = ol(o);
          this.fcSysId = i;
          let r = bu(e.baseMode, e.customMode),
            a = !!(e.baseMode & 128);
          n.setDeviceInfo(`飞控 (SYS:${i})`, `MAVLink ${e.mavlinkVersion}`, vu(e.type)), t.patch({
            mode: r,
            armed: a
          });
          break
        }
        case Y.SYS_STATUS: {
          let e = sl(o);
          t.patch({
            voltage: e.voltageBattery,
            current: e.currentBattery < 0 ? 0 : e.currentBattery,
            remaining: e.batteryRemaining,
            sensorsPresent: e.sensorsPresent,
            sensorsEnabled: e.sensorsEnabled,
            sensorsHealthy: e.sensorsHealthy
          });
          break
        }
        case Y.GPS_RAW_INT: {
          let e = dl(o);
          t.patch({
            lat: e.lat,
            lon: e.lon,
            alt: e.alt,
            fixType: e.fixType,
            satellites: e.satellitesVisible,
            hdop: e.eph === 65535 ? 9999 : e.eph / 100
          });
          break
        }
        case Y.ATTITUDE: {
          let e = fl(o);
          t.patch({
            roll: e.roll,
            pitch: e.pitch,
            yaw: e.yaw
          });
          break
        }
        case Y.ATTITUDE_QUATERNION: {
          let e = yl(o),
            n = e.q1,
            r = e.q2,
            i = e.q3,
            a = e.q4,
            s = Math.atan2(2 * (n * r + i * a), 1 - 2 * (r * r + i * i)),
            c = Math.asin(Math.max(-1, Math.min(1, 2 * (n * i - a * r)))),
            l = Math.atan2(2 * (n * a + r * i), 1 - 2 * (i * i + a * a));
          t.patch({
            roll: s,
            pitch: c,
            yaw: l
          });
          break
        }
        case Y.GLOBAL_POSITION_INT: {
          let e = pl(o);
          t.patch({
            lat: e.lat,
            lon: e.lon,
            alt: e.alt,
            relativeAlt: e.relativeAlt,
            vz: e.vz,
            heading: e.hdg / 100
          });
          break
        }
        case Y.VFR_HUD: {
          let e = ml(o);
          t.patch({
            airspeed: Math.round(e.airspeed * 100),
            groundspeed: Math.round(e.groundspeed * 100),
            vz: Math.round(-e.climb * 100),
            heading: e.heading,
            relativeAlt: Math.round(e.alt * 1e3)
          });
          break
        }
        case Y.RC_CHANNELS: {
          let e = vl(o);
          t.patch({
            rssi: e.rssi,
            rcChannels: e.channels,
            rcChanCount: e.chancount
          });
          break
        }
        case Y.RC_CHANNELS_RAW: {
          let e = bl(o),
            n = [...e.channels, ...Array(10).fill(0)];
          t.patch({
            rcChannels: n,
            rcChanCount: 8,
            rssi: e.rssi
          });
          break
        }
        case Y.SERVO_OUTPUT_RAW: {
          let e = _l(o);
          t.patch({
            servoOutputs: e.servo
          });
          break
        }
        case Y.ACTUATOR_CONTROL_TARGET: {
          let e = Array(12).fill(0);
          for (let t = 0; t < 4; t++) {
            let n = o.byteLength >= 8 + t * 4 + 4 ? o.getFloat32(4 + t * 4, !0) : 0;
            e[t] = n > .001 ? Math.round(1e3 + n * 1e3) : 0
          }
          t.patch({
            servoOutputs: e
          });
          break
        }
        case Y.OPTICAL_FLOW: {
          let e = cl(o),
            n = isNaN(e.groundDistance) || e.groundDistance < 0 ? -1 : e.groundDistance;
          t.patch({
            optFlowQuality: e.quality,
            optFlowRateX: e.flowRateX,
            optFlowRateY: e.flowRateY,
            optFlowGroundDist: n
          });
          break
        }
        case Y.DISTANCE_SENSOR: {
          let e = ul(o);
          t.patch({
            rangePresent: !0,
            rangeDistance: e.currentDistance >= 65535 ? -1 : e.currentDistance
          });
          break
        }
        case Y.SCALED_PRESSURE: {
          let e = ll(o);
          t.patch({
            baroPresAbs: e.pressAbs,
            baroTemp: e.temperature
          });
          break
        }
        case Y.PARAM_VALUE: {
          let e = gl(o);
          xl().onParamValue(e.paramId, e.paramValue, e.paramType, e.paramIndex, e.paramCount);
          break
        }
        case Y.STATUSTEXT: {
          let e = hl(o),
            t = e.severity <= 2 ? `danger` : e.severity <= 4 ? `warning` : `info`;
          jc().push(e.text, t);
          break
        }
        case Y.MISSION_CURRENT: {
          let e = o.getUint16(0, !0);
          Jl().activeMissionSeq = e;
          break
        }
        case Y.MISSION_COUNT:
        case Y.MISSION_REQUEST_INT:
        case Y.MISSION_ITEM_INT:
        case Y.MISSION_ACK:
        case 40:
          this.mission.handleFrame(r, o);
          break
      }
    }
  };

function vu(e) {
  var t;
  return (t = {
    0: `通用`,
    1: `固定翼`,
    2: `四旋翼`,
    3: `直升机`,
    13: `六旋翼`,
    14: `八旋翼`,
    27: `多旋翼 VTOL`
  } [e]) == null ? `MAV_TYPE(${e})` : t
}
var yu = {
  0: `RAW`,
  1: `ACRO`,
  2: `STAB`,
  3: `ALTHOLD`,
  4: `AUTO`,
  5: `POSHOLD`
};

function bu(e, t) {
  let n = yu[t];
  return n === void 0 ? e & 1 ? `CUSTOM(${t})` : e & 4 ? `AUTO` : e & 8 ? `GUIDED` : `MODE(${e})` : n
}
var xu = new class {
  constructor() {
    J(this, `wifi`, null), J(this, `serial`, null)
  }
  connectWifi(e, t) {
    this.disconnectAll(), this.wifi = new du, this.wifi.connect(e, t)
  }
  async connectSerial(e) {
    this.disconnectAll(), this.serial = new _u;
    let t = await this.serial.connect(e);
    return t || (this.serial = null), t
  }
  disconnectAll() {
    var e, t;
    (e = this.wifi) == null || e.disconnect(), this.wifi = null, (t = this.serial) == null || t.disconnect(), this.serial = null
  }
  async autoReconnect() {
    let e = localStorage.getItem(`cgc_last_conn_type`);
    if (e) {
      if (e === `wifi`) {
        let e = localStorage.getItem(`cgc_wifi_host`) || `192.168.4.1`,
          t = Number(localStorage.getItem(`cgc_wifi_port`) || `8765`);
        this.connectWifi(e, t)
      } else if (e === `serial`) {
        let e = Number(localStorage.getItem(`cgc_serial_baud`) || `115200`);
        this.disconnectAll(), this.serial = new _u, await this.serial.connectAuto(e) || (this.serial = null)
      }
    }
  }
  get activeConnection() {
    var e;
    return (e = this.wifi) == null ? this.serial : e
  }
  sendRaw(e) {
    var t, n, r, i;
    return (t = (n = (r = this.wifi) == null ? void 0 : r.send(e)) == null ? (i = this.serial) == null ? void 0 : i.send(e) : n) == null ? !1 : t
  }
  send(e) {
    let t = Xc(e);
    return this.sendRaw(t)
  }
  cmd(e, t = 0, n = 0, r = 0, i = 0, a = 0, o = 0, s = 0) {
    return this.send({
      targetSystem: 1,
      targetComponent: 1,
      command: e,
      confirmation: 0,
      param1: t,
      param2: n,
      param3: r,
      param4: i,
      param5: a,
      param6: o,
      param7: s
    })
  }
  setArmed(e) {
    return this.cmd(X.ARM_DISARM, +!!e)
  }
  takeoff(e) {
    return this.cmd(X.TAKEOFF, 0, 0, 0, 0, 0, 0, e)
  }
  land() {
    return this.cmd(X.LAND)
  }
  rtl() {
    return this.cmd(X.RETURN_TO_LAUNCH)
  }
  pauseContinue(e) {
    return this.cmd(X.DO_PAUSE_CONTINUE, +!e)
  }
  startMission() {
    return this.cmd(X.MISSION_START, 0, 0)
  }
  requestAllParams(e = 1) {
    let t = Zc(e, 1),
      n = this.sendRaw(t);
    return n && xl().startLoading(), n
  }
  requestParam(e, t = 1) {
    let n = Qc(e, t, 1);
    return this.sendRaw(n)
  }
  setParam(e, t, n = 9, r = 1) {
    let i = $c(e, t, n, r, 1),
      a = this.sendRaw(i);
    return a && xl().updateLocal(e, t), a
  }
  rebootFc(e = 1) {
    return this.send({
      targetSystem: e,
      targetComponent: 1,
      command: X.PREFLIGHT_REBOOT,
      confirmation: 0,
      param1: 1
    })
  }
  startAccelCal(e = 1) {
    return this.send({
      targetSystem: e,
      targetComponent: 1,
      command: X.PREFLIGHT_CALIBRATION,
      confirmation: 0,
      param1: 0,
      param2: 0,
      param3: 0,
      param4: 0,
      param5: 4
    })
  }
  confirmAccelCalPos(e, t = 1) {
    return this.send({
      targetSystem: t,
      targetComponent: 1,
      command: X.ACCELCAL_VEHICLE_POS,
      confirmation: 0,
      param1: e
    })
  }
  startCompassCal(e = 1) {
    return this.send({
      targetSystem: e,
      targetComponent: 1,
      command: X.PREFLIGHT_CALIBRATION,
      confirmation: 0,
      param1: 0,
      param2: 1
    })
  }
  cancelCal(e = 1) {
    return this.send({
      targetSystem: e,
      targetComponent: 1,
      command: X.PREFLIGHT_CALIBRATION,
      confirmation: 0
    })
  }
  uploadMission(e, t) {
    var n;
    let r = (n = this.wifi) == null ? this.serial : n;
    return r ? (t && (r.mission.onProgress = t), r.mission.upload(e)) : Promise.reject(Error(`未连接`))
  }
  downloadMission(e) {
    var t;
    let n = (t = this.wifi) == null ? this.serial : t;
    return n ? (e && (n.mission.onProgress = e), n.mission.download()) : Promise.reject(Error(`未连接`))
  }
  clearMission() {
    return this.sendRaw(Pl())
  }
  setMissionCurrent(e) {
    return this.sendRaw(Fl(e))
  }
};
export {
  di as $, Vs as A, qn as At, Ta as B, mt as Bt, ec as C, G as Ct, Ks as D, Kn as Dt, Js as E, Zn as Et, Pa as F, Rn as Ft, wa as G, Ea as H, Na as I, Kt as It, ba as J, Ca as K, ja as L, Pn as Lt, ro as M, Wn as Mt, Qa as N, Ot as Nt, Gs as O, Yn as Ot, Ia as P, En as Pt, sa as Q, Ma as R, dt as Rt, $s as S, or as St, Ys as T, Xn as Tt, Da as U, ka as V, Oa as W, ha as X, ga as Y, ca as Z, mc as _, pr as _t, xl as a, Rr as at, rc as b, sr as bt, jc as c, Tr as ct, Ec as d, Sr as dt, Qr as et, Tc as f, xr as ft, gc as g, mr as gt, vc as h, _r as ht, wl as i, Hr as it, zs as j, Gn as jt, Hs as k, Jn as kt, Oc as l, Cr as lt, Sc as m, vr as mt, ou as n, Xr as nt, tl as o, Lr as ot, Cc as p, yr as pt, xa as q, Jl as r, Kr as rt, el as s, Er as st, xu as t, Yr as tt, kc as u, br as ut, pc as v, fr as vt, Zs as w, W as wt, nc as x, cr as xt, cc as y, dr as yt, Aa as z, ft as zt
};