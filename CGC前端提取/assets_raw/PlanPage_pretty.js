import {
  Lt as e,
  l as t,
  r as n,
  t as r
} from "./mavlink-C7-F-u0_.js";
import {
  Et as i,
  Qt as a,
  Sn as o,
  Tt as s,
  Ut as c,
  Vt as l,
  _t as u,
  bt as d,
  cn as f,
  ft as p,
  n as ee,
  r as te,
  rn as m,
  t as h,
  vt as g,
  wn as _,
  wt as v,
  xt as y,
  yt as ne,
  zt as re
} from "./_plugin-vue_export-helper-CQMKH6SZ.js";
import {
  n as ie,
  t as ae
} from "./Popconfirm-Dcx9649Y.js";
import {
  n as oe,
  t as se
} from "./Scrollbar-CNSa8gvt.js";
import {
  A as ce,
  C as le,
  b as ue,
  c as de,
  h as b,
  t as fe,
  u as x
} from "./index-DJhI6atv.js";
import {
  t as S
} from "./CgcMap-B99j7AVl.js";
var C = {
    class: `page-container plan-page`
  },
  w = {
    class: `plan-body`
  },
  T = {
    class: `map-area`
  },
  E = {
    class: `map-toolbar float-panel`
  },
  D = {
    key: 0,
    class: `map-hud`
  },
  O = {
    class: `hud-item`
  },
  k = {
    class: `hud-item`
  },
  A = {
    class: `hud-val mono`
  },
  j = {
    class: `hud-item`
  },
  M = {
    class: `hud-val mono`
  },
  N = {
    class: `hud-item`
  },
  P = {
    class: `hud-val mono`
  },
  F = {
    class: `hud-item`
  },
  I = {
    class: `hud-val mono`
  },
  L = {
    class: `hud-item`
  },
  R = {
    class: `hud-val mono`
  },
  z = {
    class: `hud-item`
  },
  B = {
    class: `hud-val mono`
  },
  V = {
    key: 1,
    class: `map-hint`
  },
  pe = {
    class: `panel-header`
  },
  me = {
    class: `panel-title`
  },
  he = {
    key: 0,
    class: `dirty-dot`,
    title: `有未保存修改`
  },
  ge = {
    class: `panel-name-row`
  },
  _e = {
    key: 0
  },
  ve = [`onClick`],
  ye = {
    class: `wp-head`
  },
  be = {
    class: `wp-seq`
  },
  xe = {
    class: `wp-coords mono`
  },
  Se = {
    class: `wp-editor-row`
  },
  Ce = {
    class: `wp-editor-row`
  },
  we = {
    class: `wp-editor-row`
  },
  H = {
    class: `wp-editor-row`
  },
  Te = {
    key: 0,
    class: `panel-stats`
  },
  Ee = {
    class: `stat-row`
  },
  De = {
    class: `mono`
  },
  Oe = {
    class: `stat-row`
  },
  ke = {
    class: `mono`
  },
  Ae = {
    class: `panel-actions`
  },
  je = {
    key: 0,
    class: `transfer-progress`
  },
  Me = {
    class: `tp-label`
  },
  Ne = {
    key: 0,
    style: {
      color: `#EF4444`
    }
  },
  Pe = {
    key: 1,
    style: {
      color: `#22C55E`
    }
  },
  Fe = {
    class: `btn-row`
  },
  Ie = {
    key: 1,
    class: `fly-hint`
  },
  U = h(i({
    __name: `PlanPage`,
    setup(i) {
      let h = te(),
        U = n(),
        W = ee(),
        G = de(),
        Le = fe(),
        K = m(!1),
        q = null,
        J = m(),
        Y = m(!0),
        X = m(null),
        Z = m(`dark`),
        Re = [{
          label: `悬停经过`,
          value: `WAYPOINT`
        }, {
          label: `起飞`,
          value: `TAKEOFF`
        }, {
          label: `降落`,
          value: `LAND`
        }, {
          label: `返航`,
          value: `RTL`
        }, {
          label: `定时盘旋`,
          value: `LOITER_TIME`
        }],
        ze = u(() => {
          let e = U.current.waypoints;
          if (e.length < 2) return 0;
          let t = 0;
          for (let n = 1; n < e.length; n++) {
            let r = e[n - 1],
              i = e[n],
              a = (i.lat - r.lat) * Math.PI / 180,
              o = (i.lon - r.lon) * Math.PI / 180,
              s = Math.sin(a / 2) ** 2 + Math.cos(r.lat * Math.PI / 180) * Math.cos(i.lat * Math.PI / 180) * Math.sin(o / 2) ** 2;
            t += 2 * 6371e3 * Math.asin(Math.sqrt(s))
          }
          return (t / 1e3).toFixed(2)
        });

      function Be() {
        var e;
        (e = J.value) == null || e.fitWaypoints()
      }

      function Ve(e) {
        X.value === e && (X.value = null), U.removeWaypoint(e)
      }

      function He() {
        U.clearMission(), X.value = null
      }
      let Q = m(null),
        $ = m(!1);
      async function Ue() {
        if (!h.isConnected) {
          G.warning(`请先连接飞控`);
          return
        }
        let e = U.current.waypoints;
        if (e.length === 0) {
          G.warning(`航点列表为空`);
          return
        }
        $.value = !0, Q.value = null;
        try {
          await r.uploadMission(e, e => {
            Q.value = e
          }), G.success(`已上传 ${e.length} 个航点`), U.isDirty = !1, K.value = !0, q && clearTimeout(q), q = setTimeout(() => {
            K.value = !1
          }, 8e3)
        } catch (e) {
          G.error(`上传失败: ${e instanceof Error?e.message:String(e)}`)
        } finally {
          $.value = !1, setTimeout(() => {
            Q.value = null
          }, 3e3)
        }
      }
      re(() => {
        q && (clearTimeout(q), q = null)
      });
      async function We() {
        if (!h.isConnected) {
          G.warning(`请先连接飞控`);
          return
        }
        $.value = !0, Q.value = null;
        try {
          let e = await r.downloadMission(e => {
            Q.value = e
          });
          if (e.length === 0) {
            G.info(`飞控任务为空`);
            return
          }
          U.loadMission({
            name: `飞控任务`,
            waypoints: e
          }), G.success(`已读取 ${e.length} 个航点`)
        } catch (e) {
          G.error(`读取失败: ${e instanceof Error?e.message:String(e)}`)
        } finally {
          $.value = !1, setTimeout(() => {
            Q.value = null
          }, 3e3)
        }
      }

      function Ge() {
        let e = JSON.stringify(U.current, null, 2),
          t = new Blob([e], {
            type: `application/json`
          }),
          n = document.createElement(`a`);
        n.href = URL.createObjectURL(t), n.download = (U.current.name || `mission`) + `.json`, n.click(), URL.revokeObjectURL(n.href), G.success(`已保存为 JSON 文件`)
      }

      function Ke() {
        let e = document.createElement(`input`);
        e.type = `file`, e.accept = `.json`, e.onchange = e => {
          var t;
          let n = (t = e.target.files) == null ? void 0 : t[0];
          n && n.text().then(e => {
            try {
              let n = JSON.parse(e);
              if (Array.isArray(n.waypoints)) {
                var t;
                U.loadMission(n), (t = J.value) == null || t.fitWaypoints(), G.success(`任务已加载`)
              } else G.error(`无效的任务文件格式`)
            } catch {
              G.error(`文件解析失败`)
            }
          })
        }, e.click()
      }
      return (n, r) => {
        var i, u;
        return l(), y(`div`, C, [g(`div`, w, [g(`div`, T, [s(S, {
          ref_key: `mapRef`,
          ref: J,
          mode: `plan`,
          "map-style": Z.value
        }, null, 8, [`map-style`]), g(`div`, E, [s(f(b), {
          placement: `bottom`
        }, {
          trigger: a(() => [s(f(t), {
            quaternary: ``,
            size: `small`,
            onClick: Be,
            disabled: !f(U).current.waypoints.length
          }, {
            default: a(() => [...r[5] || (r[5] = [v(` ⊞ 适配视图 `, -1)])]),
            _: 1
          }, 8, [`disabled`])]),
          default: a(() => [r[6] || (r[6] = v(`缩放到全部航点`, -1))]),
          _: 1
        }), r[10] || (r[10] = g(`div`, {
          class: `toolbar-divider`
        }, null, -1)), s(f(b), {
          placement: `bottom`
        }, {
          trigger: a(() => [s(f(t), {
            quaternary: ``,
            size: `small`,
            type: Z.value === `satellite` ? `primary` : `default`,
            onClick: r[0] || (r[0] = e => Z.value = Z.value === `satellite` ? `dark` : `satellite`)
          }, {
            default: a(() => [v(_(Z.value === `satellite` ? `🗺️ 矢量` : `🛰️ 卫星`), 1)]),
            _: 1
          }, 8, [`type`])]),
          default: a(() => [r[7] || (r[7] = v(`切换卫星/矢量底图`, -1))]),
          _: 1
        }), r[11] || (r[11] = g(`div`, {
          class: `toolbar-divider`
        }, null, -1)), s(f(ae), {
          onPositiveClick: He
        }, {
          trigger: a(() => [s(f(t), {
            quaternary: ``,
            size: `small`,
            type: `error`,
            disabled: !f(U).current.waypoints.length
          }, {
            default: a(() => [...r[8] || (r[8] = [v(` 🗑 清空 `, -1)])]),
            _: 1
          }, 8, [`disabled`])]),
          default: a(() => [r[9] || (r[9] = v(` 清空全部航点？ `, -1))]),
          _: 1
        })]), f(h).isConnected ? (l(), y(`div`, D, [g(`div`, O, [s(f(b), {
          placement: `top`
        }, {
          trigger: a(() => [...r[12] || (r[12] = [g(`span`, {
            class: `hud-label`
          }, `D`, -1)])]),
          default: a(() => [r[13] || (r[13] = v(` 离家距离（暂未支持，需飞控上报 HOME_POSITION） `, -1))]),
          _: 1
        }), r[14] || (r[14] = g(`span`, {
          class: `hud-val mono`
        }, `—`, -1))]), g(`div`, k, [r[16] || (r[16] = g(`span`, {
          class: `hud-label`
        }, `H`, -1)), g(`span`, A, [v(_(f(W).relAltM.toFixed(1)), 1), r[15] || (r[15] = g(`span`, {
          class: `hud-unit`
        }, `m`, -1))])]), g(`div`, j, [r[18] || (r[18] = g(`span`, {
          class: `hud-label`
        }, `GS`, -1)), g(`span`, M, [v(_(f(W).speedMs.toFixed(1)), 1), r[17] || (r[17] = g(`span`, {
          class: `hud-unit`
        }, `m/s`, -1))])]), g(`div`, N, [r[20] || (r[20] = g(`span`, {
          class: `hud-label`
        }, `VS`, -1)), g(`span`, P, [v(_((f(W).climbRate >= 0 ? `+` : ``) + f(W).climbRate.toFixed(1)), 1), r[19] || (r[19] = g(`span`, {
          class: `hud-unit`
        }, `m/s`, -1))])]), r[24] || (r[24] = g(`div`, {
          class: `hud-sep`
        }, null, -1)), g(`div`, F, [r[21] || (r[21] = g(`span`, {
          class: `hud-label`
        }, `LAT`, -1)), g(`span`, I, _(f(W).latDeg.toFixed(6)) + `°`, 1)]), g(`div`, L, [r[22] || (r[22] = g(`span`, {
          class: `hud-label`
        }, `LON`, -1)), g(`span`, R, _(f(W).lonDeg.toFixed(6)) + `°`, 1)]), g(`div`, z, [r[23] || (r[23] = g(`span`, {
          class: `hud-label`
        }, `HDG`, -1)), g(`span`, B, _(f(W).state.heading.toFixed(0)) + `°`, 1)])])) : (l(), y(`div`, V, `点击地图添加航点 · 拖动调整位置 · 右侧面板编辑参数`))]), g(`div`, {
          class: o([`mission-panel`, {
            collapsed: !Y.value
          }])
        }, [g(`div`, pe, [g(`div`, me, [r[25] || (r[25] = g(`span`, null, `任务规划`, -1)), f(U).isDirty ? (l(), y(`span`, he)) : d(``, !0)]), s(f(t), {
          text: ``,
          size: `tiny`,
          onClick: r[1] || (r[1] = e => Y.value = !Y.value)
        }, {
          default: a(() => [v(_(Y.value ? `◀` : `▶`), 1)]),
          _: 1
        })]), Y.value ? (l(), y(p, {
          key: 0
        }, [g(`div`, ge, [s(f(le), {
          value: f(U).current.name,
          "onUpdate:value": r[2] || (r[2] = e => f(U).current.name = e),
          size: `small`,
          placeholder: `任务名称`
        }, null, 8, [`value`])]), s(f(se), {
          class: `panel-body`
        }, {
          default: a(() => [f(U).current.waypoints.length ? (l(), y(`div`, _e, [(l(!0), y(p, null, c(f(U).current.waypoints, n => {
            var i, c, u;
            return l(), y(`div`, {
              key: n.seq,
              class: o([`waypoint-item`, {
                active: X.value === n.seq
              }]),
              onClick: e => X.value = X.value === n.seq ? null : n.seq
            }, [g(`div`, ye, [g(`div`, be, _(n.seq + 1), 1), g(`div`, xe, _(n.lat.toFixed(5)) + `, ` + _(n.lon.toFixed(5)), 1), s(f(t), {
              text: ``,
              size: `tiny`,
              type: `error`,
              onClick: e(e => Ve(n.seq), [`stop`])
            }, {
              default: a(() => [...r[26] || (r[26] = [v(`✕`, -1)])]),
              _: 1
            }, 8, [`onClick`])]), X.value === n.seq ? (l(), y(`div`, {
              key: 0,
              class: `wp-editor`,
              onClick: r[3] || (r[3] = e(() => {}, [`stop`]))
            }, [g(`div`, Se, [r[27] || (r[27] = g(`label`, null, `高度 (m)`, -1)), s(f(x), {
              value: n.alt,
              "onUpdate:value": e => f(U).updateWaypoint(n.seq, {
                alt: e == null ? 30 : e
              }),
              size: `tiny`,
              min: 1,
              max: 500,
              style: {
                width: `90px`
              }
            }, null, 8, [`value`, `onUpdate:value`])]), g(`div`, Ce, [r[28] || (r[28] = g(`label`, null, `速度 (m/s)`, -1)), s(f(x), {
              value: (i = n.speed) == null ? 5 : i,
              "onUpdate:value": e => f(U).updateWaypoint(n.seq, {
                speed: e == null ? 5 : e
              }),
              size: `tiny`,
              min: 1,
              max: 20,
              style: {
                width: `90px`
              }
            }, null, 8, [`value`, `onUpdate:value`])]), g(`div`, we, [r[29] || (r[29] = g(`label`, null, `悬停 (s)`, -1)), s(f(x), {
              value: (c = n.holdTime) == null ? 0 : c,
              "onUpdate:value": e => f(U).updateWaypoint(n.seq, {
                holdTime: e == null ? 0 : e
              }),
              size: `tiny`,
              min: 0,
              max: 600,
              style: {
                width: `90px`
              }
            }, null, 8, [`value`, `onUpdate:value`])]), g(`div`, H, [r[30] || (r[30] = g(`label`, null, `动作`, -1)), s(f(ue), {
              value: (u = n.action) == null ? `WAYPOINT` : u,
              "onUpdate:value": e => f(U).updateWaypoint(n.seq, {
                action: e
              }),
              options: Re,
              size: `tiny`,
              style: {
                width: `120px`
              }
            }, null, 8, [`value`, `onUpdate:value`])])])) : d(``, !0)], 10, ve)
          }), 128))])) : (l(), ne(f(ce), {
            key: 1,
            description: `点击地图添加航点`,
            "show-icon": !1,
            class: `panel-empty`
          }))]),
          _: 1
        }), f(U).current.waypoints.length ? (l(), y(`div`, Te, [s(f(ie), {
          style: {
            margin: `8px 0`
          }
        }), g(`div`, Ee, [r[31] || (r[31] = g(`span`, null, `航点数`, -1)), g(`span`, De, _(f(U).current.waypoints.length), 1)]), g(`div`, Oe, [r[32] || (r[32] = g(`span`, null, `预计距离`, -1)), g(`span`, ke, _(ze.value) + ` km`, 1)])])) : d(``, !0), g(`div`, Ae, [Q.value ? (l(), y(`div`, je, [g(`div`, Me, [v(_(Q.value.phase === `upload` ? `上传` : `下载`) + ` ` + _(Q.value.current) + `/` + _(Q.value.total) + ` `, 1), Q.value.error ? (l(), y(`span`, Ne, ` ✗`)) : Q.value.done ? (l(), y(`span`, Pe, ` ✓`)) : d(``, !0)]), s(f(oe), {
          type: `line`,
          percentage: Q.value.total > 0 ? Q.value.current / Q.value.total * 100 : 0,
          status: Q.value.error ? `error` : Q.value.done ? `success` : `default`,
          "show-indicator": !1
        }, null, 8, [`percentage`, `status`])])) : d(``, !0), s(f(t), {
          type: `primary`,
          size: `small`,
          block: ``,
          disabled: !f(h).isConnected || !f(U).current.waypoints.length || $.value,
          loading: $.value && ((i = Q.value) == null ? void 0 : i.phase) === `upload`,
          onClick: Ue
        }, {
          default: a(() => [...r[33] || (r[33] = [v(` ⬆ 上传到飞控 `, -1)])]),
          _: 1
        }, 8, [`disabled`, `loading`]), s(f(t), {
          size: `small`,
          block: ``,
          disabled: !f(h).isConnected || $.value,
          loading: $.value && ((u = Q.value) == null ? void 0 : u.phase) === `download`,
          onClick: We
        }, {
          default: a(() => [...r[34] || (r[34] = [v(` ⬇ 从飞控读取 `, -1)])]),
          _: 1
        }, 8, [`disabled`, `loading`]), g(`div`, Fe, [s(f(t), {
          size: `small`,
          style: {
            flex: `1`
          },
          onClick: Ge
        }, {
          default: a(() => [...r[35] || (r[35] = [v(`💾 保存`, -1)])]),
          _: 1
        }), s(f(t), {
          size: `small`,
          style: {
            flex: `1`
          },
          onClick: Ke
        }, {
          default: a(() => [...r[36] || (r[36] = [v(`📂 加载`, -1)])]),
          _: 1
        })]), K.value ? (l(), y(`div`, Ie, [r[37] || (r[37] = v(` ✓ 任务已上传 · `, -1)), g(`button`, {
          class: `fly-hint-link`,
          onClick: r[4] || (r[4] = e => f(Le).push(`/fly`))
        }, `切换到飞行页执行 →`)])) : d(``, !0)])], 64)) : d(``, !0)], 2)])])
      }
    }
  }), [
    [`__scopeId`, `data-v-4c9517ce`]
  ]);
export {
  U as
  default
};