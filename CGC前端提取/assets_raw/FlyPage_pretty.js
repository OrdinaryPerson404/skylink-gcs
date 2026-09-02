import {
  c as e,
  l as t,
  r as n,
  t as r
} from "./mavlink-C7-F-u0_.js";
import {
  At as ee,
  Cn as i,
  Et as a,
  Qt as o,
  Sn as s,
  Tt as c,
  Ut as te,
  Vt as l,
  _t as u,
  bt as d,
  cn as f,
  ft as ne,
  n as re,
  r as ie,
  rn as p,
  t as m,
  vt as h,
  wn as g,
  wt as _,
  xt as v,
  z as y
} from "./_plugin-vue_export-helper-CQMKH6SZ.js";
import {
  c as ae,
  f as b,
  h as x,
  t as oe
} from "./index-DJhI6atv.js";
import {
  t as se
} from "./CgcMap-B99j7AVl.js";
import {
  t as ce
} from "./AttitudeIndicator-IQe-Q6GU.js";

function le() {
  let e = ee(b, null);
  return e === null && y(`use-dialog`, `No outer <n-dialog-provider /> founded.`), e
}
var S = {
    class: `page-container fly-page`
  },
  C = {
    class: `fly-body`
  },
  w = {
    key: 0,
    class: `map-banner info-banner`
  },
  T = {
    key: 1,
    class: `map-banner warn-banner`
  },
  E = {
    key: 2,
    class: `adi-overlay float-panel`
  },
  D = {
    class: `hud-right float-panel`
  },
  O = {
    class: `hud-row`
  },
  k = {
    class: `hr-val mono`
  },
  A = {
    class: `hud-row`
  },
  j = {
    class: `hr-val mono`
  },
  M = {
    class: `hud-row`
  },
  N = {
    class: `hr-val mono`
  },
  P = {
    class: `hud-row`
  },
  F = {
    class: `hud-row`
  },
  I = {
    class: `hr-val mono`
  },
  L = {
    class: `hud-row`
  },
  R = {
    class: `hr-val mono`
  },
  z = {
    key: 0,
    class: `hud-row`
  },
  B = {
    class: `hr-val mono`
  },
  V = {
    class: `alerts-chevron`
  },
  H = {
    key: 0,
    class: `alerts-body`
  },
  U = {
    key: 0,
    class: `alert-empty`
  },
  W = {
    class: `alert-time`
  },
  ue = {
    class: `bottom-hud`
  },
  de = {
    class: `bh-item`
  },
  fe = {
    class: `bh-item`
  },
  pe = {
    class: `bh-val`
  },
  me = {
    class: `bh-item`
  },
  he = {
    class: `bh-item`
  },
  ge = {
    class: `bh-val`
  },
  _e = {
    class: `bh-item`
  },
  ve = {
    class: `bh-val`
  },
  ye = {
    class: `bh-item`
  },
  be = {
    class: `bh-val`
  },
  xe = {
    class: `command-bar`
  },
  G = m(a({
    __name: `FlyPage`,
    setup(ee) {
      let a = re(),
        m = ie(),
        y = e(),
        b = n(),
        G = le(),
        K = ae(),
        q = oe(),
        Se = p(),
        J = p(!0),
        Y = p(!1),
        X = u(() => a.state.armed),
        Z = u(() => m.isConnected),
        Q = u(() => b.current.waypoints.length > 0),
        Ce = u(() => {
          let e = a.state.remaining;
          return e < 0 ? `#484F58` : e < 15 ? `#EF4444` : e < 30 ? `#F59E0B` : `#22C55E`
        });

      function $() {
        Z.value && (X.value ? G.warning({
          title: `确认加锁`,
          content: `将关闭电机，确认操作？`,
          positiveText: `加锁`,
          onPositiveClick: () => {
            r.setArmed(!1)
          }
        }) : G.warning({
          title: `确认解锁`,
          content: `解锁后电机将启动，请确保周围安全！`,
          positiveText: `解锁`,
          negativeText: `取消`,
          onPositiveClick: () => {
            r.setArmed(!0)
          }
        }))
      }

      function we() {
        if (!X.value) {
          K.warning(`请先解锁飞控`);
          return
        }
        G.info({
          title: `自动起飞`,
          content: `起飞至默认高度 2 米，确认？`,
          positiveText: `起飞`,
          negativeText: `取消`,
          onPositiveClick: () => {
            r.takeoff(2)
          }
        })
      }

      function Te() {
        if (!X.value) {
          K.warning(`请先解锁飞控`);
          return
        }
        if (!Q.value) {
          K.warning(`请先在规划页创建并上传任务`);
          return
        }
        if (b.isDirty) {
          K.warning(`任务有修改未上传，请先前往规划页上传`);
          return
        }
        G.info({
          title: `开始任务`,
          content: `将按任务列表依次飞行，确认？`,
          positiveText: `开始`,
          negativeText: `取消`,
          onPositiveClick: () => {
            r.startMission()
          }
        })
      }

      function Ee() {
        r.land()
      }

      function De() {
        r.rtl()
      }

      function Oe() {
        r.pauseContinue(!0)
      }
      return (e, n) => (l(), v(`div`, S, [h(`div`, C, [c(se, {
        ref_key: `mapRef`,
        ref: Se,
        mode: `fly`
      }, null, 512), Z.value && !Q.value ? (l(), v(`div`, w, [n[5] || (n[5] = _(` 当前无任务 · `, -1)), h(`button`, {
        class: `banner-link`,
        onClick: n[0] || (n[0] = e => f(q).push(`/plan`))
      }, `前往规划页创建任务 →`)])) : d(``, !0), f(b).isDirty && Q.value ? (l(), v(`div`, T, [n[6] || (n[6] = _(` ⚠ 任务已修改但未上传 · `, -1)), h(`button`, {
        class: `banner-link`,
        onClick: n[1] || (n[1] = e => f(q).push(`/plan`))
      }, `前往规划页上传 →`)])) : d(``, !0), J.value ? (l(), v(`div`, E, [c(ce, {
        roll: f(a).rollDeg,
        pitch: f(a).pitchDeg,
        heading: f(a).state.heading
      }, null, 8, [`roll`, `pitch`, `heading`]), h(`button`, {
        class: `adi-close`,
        onClick: n[2] || (n[2] = e => J.value = !1)
      }, `✕`)])) : (l(), v(`button`, {
        key: 3,
        class: `adi-show`,
        onClick: n[3] || (n[3] = e => J.value = !0)
      }, `◎ ADI`)), h(`div`, D, [h(`div`, O, [n[7] || (n[7] = h(`span`, {
        class: `hr-label`
      }, `HDG`, -1)), h(`span`, k, g(f(a).state.heading.toFixed(0)) + `°`, 1)]), h(`div`, A, [n[9] || (n[9] = h(`span`, {
        class: `hr-label`
      }, `ALT`, -1)), h(`span`, j, [_(g(f(a).relAltM.toFixed(1)), 1), n[8] || (n[8] = h(`span`, {
        class: `hr-unit`
      }, `m`, -1))])]), h(`div`, M, [n[11] || (n[11] = h(`span`, {
        class: `hr-label`
      }, `GS`, -1)), h(`span`, N, [_(g(f(a).speedMs.toFixed(1)), 1), n[10] || (n[10] = h(`span`, {
        class: `hr-unit`
      }, `m/s`, -1))])]), h(`div`, P, [n[13] || (n[13] = h(`span`, {
        class: `hr-label`
      }, `VS`, -1)), h(`span`, {
        class: `hr-val mono`,
        style: i({
          color: f(a).climbRate > .2 ? `#22C55E` : f(a).climbRate < -.2 ? `#EF4444` : `#8B949E`
        })
      }, [_(g((f(a).climbRate >= 0 ? `+` : ``) + f(a).climbRate.toFixed(1)), 1), n[12] || (n[12] = h(`span`, {
        class: `hr-unit`
      }, `m/s`, -1))], 4)]), h(`div`, F, [n[14] || (n[14] = h(`span`, {
        class: `hr-label`
      }, `ROL`, -1)), h(`span`, I, g(f(a).rollDeg.toFixed(1)) + `°`, 1)]), h(`div`, L, [n[15] || (n[15] = h(`span`, {
        class: `hr-label`
      }, `PIT`, -1)), h(`span`, R, g(f(a).pitchDeg.toFixed(1)) + `°`, 1)]), n[17] || (n[17] = h(`div`, {
        class: `hud-sep`
      }, null, -1)), h(`div`, {
        class: s([`hud-mode`, {
          armed: X.value
        }])
      }, g(f(a).state.mode), 3), Q.value ? (l(), v(`div`, z, [n[16] || (n[16] = h(`span`, {
        class: `hr-label`
      }, `WP`, -1)), h(`span`, B, g(f(b).activeMissionSeq + 1) + `/` + g(f(b).current.waypoints.length), 1)])) : d(``, !0)]), h(`div`, {
        class: s([`alerts-panel`, {
          open: Y.value
        }])
      }, [h(`div`, {
        class: `alerts-header`,
        onClick: n[4] || (n[4] = e => Y.value = !Y.value)
      }, [n[18] || (n[18] = h(`span`, null, `系统消息`, -1)), h(`span`, {
        class: s([`alert-count`, {
          warn: f(y).alerts.some(e => e.level !== `info`)
        }])
      }, g(f(y).alerts.length), 3), h(`span`, V, g(Y.value ? `▾` : `▸`), 1)]), Y.value ? (l(), v(`div`, H, [f(y).alerts.length ? d(``, !0) : (l(), v(`div`, U, `暂无消息`)), (l(!0), v(ne, null, te(f(y).alerts.slice(0, 50), e => (l(), v(`div`, {
        key: e.id,
        class: s([`alert-item`, e.level])
      }, [h(`span`, W, g(new Date(e.timestamp).toLocaleTimeString(`zh`, {
        hour12: !1
      })), 1), h(`span`, null, g(e.message), 1)], 2))), 128))])) : d(``, !0)], 2)]), h(`div`, ue, [h(`div`, de, [n[19] || (n[19] = h(`span`, {
        class: `bh-label`
      }, `电池`, -1)), h(`span`, {
        class: `bh-val`,
        style: i({
          color: Ce.value
        })
      }, g(f(a).state.remaining >= 0 ? f(a).state.remaining + `%` : `--`), 5)]), h(`div`, fe, [n[20] || (n[20] = h(`span`, {
        class: `bh-label`
      }, `电压`, -1)), h(`span`, pe, g(f(a).voltageV > 0 ? f(a).voltageV.toFixed(1) + `V` : `--`), 1)]), h(`div`, me, [n[21] || (n[21] = h(`span`, {
        class: `bh-label`
      }, `GPS`, -1)), h(`span`, {
        class: `bh-val`,
        style: i({
          color: f(a).isGpsOk ? `#22C55E` : `#F59E0B`
        })
      }, g(f(a).state.satellites) + ` 颗 `, 5)]), h(`div`, he, [n[22] || (n[22] = h(`span`, {
        class: `bh-label`
      }, `RC`, -1)), h(`span`, ge, g(Z.value ? f(a).signalPct + `%` : `--`), 1)]), n[25] || (n[25] = h(`div`, {
        class: `bh-sep`
      }, null, -1)), h(`div`, _e, [n[23] || (n[23] = h(`span`, {
        class: `bh-label`
      }, `LAT`, -1)), h(`span`, ve, g(f(a).latDeg.toFixed(5)) + `°`, 1)]), h(`div`, ye, [n[24] || (n[24] = h(`span`, {
        class: `bh-label`
      }, `LON`, -1)), h(`span`, be, g(f(a).lonDeg.toFixed(5)) + `°`, 1)])]), h(`div`, xe, [c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: X.value ? `warning` : `default`,
          class: `cmd-btn`,
          onClick: $
        }, {
          default: o(() => [_(g(X.value ? `加锁` : `解锁`), 1)]),
          _: 1
        }, 8, [`type`])]),
        default: o(() => [_(` ` + g(X.value ? `关闭电机` : `解锁飞控`), 1)]),
        _: 1
      }), c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: `primary`,
          class: `cmd-btn`,
          onClick: we
        }, {
          default: o(() => [...n[26] || (n[26] = [_(`起飞`, -1)])]),
          _: 1
        })]),
        default: o(() => [n[27] || (n[27] = _(` 自动起飞至 2m `, -1))]),
        _: 1
      }), c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: `success`,
          class: `cmd-btn`,
          disabled: !Q.value || f(b).isDirty,
          onClick: Te
        }, {
          default: o(() => [...n[28] || (n[28] = [_(`任务`, -1)])]),
          _: 1
        }, 8, [`disabled`])]),
        default: o(() => [n[29] || (n[29] = _(` 开始执行已上传任务 `, -1))]),
        _: 1
      }), c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: `default`,
          class: `cmd-btn`,
          onClick: Ee
        }, {
          default: o(() => [...n[30] || (n[30] = [_(`降落`, -1)])]),
          _: 1
        })]),
        default: o(() => [n[31] || (n[31] = _(` 原地降落 `, -1))]),
        _: 1
      }), c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: `default`,
          class: `cmd-btn cmd-rtl`,
          onClick: De
        }, {
          default: o(() => [...n[32] || (n[32] = [_(`返航`, -1)])]),
          _: 1
        })]),
        default: o(() => [n[33] || (n[33] = _(` 返回起飞点（RTL） `, -1))]),
        _: 1
      }), c(f(x), null, {
        trigger: o(() => [c(f(t), {
          size: `large`,
          type: `default`,
          class: `cmd-btn`,
          onClick: Oe
        }, {
          default: o(() => [...n[34] || (n[34] = [_(`悬停`, -1)])]),
          _: 1
        })]),
        default: o(() => [n[35] || (n[35] = _(` 悬停在当前位置 `, -1))]),
        _: 1
      })])]))
    }
  }), [
    [`__scopeId`, `data-v-c67b5f8b`]
  ]);
export {
  G as
  default
};