import{G as e,H as t,J as n,W as r,_ as i,l as a,v as o}from"./mavlink-C7-F-u0_.js";import{At as s,Et as c,F as l,G as u,H as d,Ht as f,I as p,K as m,U as h,_t as g,ft as _,kt as v,nt as y,o as b,q as x,rn as S,sn as C}from"./_plugin-vue_export-helper-CQMKH6SZ.js";import{M as w,R as T,U as E,V as D,d as O,s as k,z as A}from"./index-DJhI6atv.js";var j=h(`divider`,`
 position: relative;
 display: flex;
 width: 100%;
 box-sizing: border-box;
 font-size: 16px;
 color: var(--n-text-color);
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
`,[x(`vertical`,`
 margin-top: 24px;
 margin-bottom: 24px;
 `,[x(`no-title`,`
 display: flex;
 align-items: center;
 `)]),u(`title`,`
 display: flex;
 align-items: center;
 margin-left: 12px;
 margin-right: 12px;
 white-space: nowrap;
 font-weight: var(--n-font-weight);
 `),m(`title-position-left`,[u(`line`,[m(`left`,{width:`28px`})])]),m(`title-position-right`,[u(`line`,[m(`right`,{width:`28px`})])]),m(`dashed`,[u(`line`,`
 background-color: #0000;
 height: 0px;
 width: 100%;
 border-style: dashed;
 border-width: 1px 0 0;
 `)]),m(`vertical`,`
 display: inline-block;
 height: 1em;
 margin: 0 8px;
 vertical-align: middle;
 width: 1px;
 `),u(`line`,`
 border: none;
 transition: background-color .3s var(--n-bezier), border-color .3s var(--n-bezier);
 height: 1px;
 width: 100%;
 margin: 0;
 `),x(`dashed`,[u(`line`,{backgroundColor:`var(--n-color)`})]),m(`dashed`,[u(`line`,{borderColor:`var(--n-color)`})]),m(`vertical`,{backgroundColor:`var(--n-color)`})]),M=c({name:`Divider`,props:Object.assign(Object.assign({},b.props),{titlePlacement:{type:String,default:`center`},dashed:Boolean,vertical:Boolean}),setup(e){let{mergedClsPrefixRef:t,inlineThemeDisabled:n}=p(e),r=b(`Divider`,`-divider`,j,O,e,t),i=g(()=>{let{common:{cubicBezierEaseInOut:e},self:{color:t,textColor:n,fontWeight:i}}=r.value;return{"--n-bezier":e,"--n-color":t,"--n-text-color":n,"--n-font-weight":i}}),a=n?l(`divider`,void 0,i,e):void 0;return{mergedClsPrefix:t,cssVars:n?void 0:i,themeClass:a==null?void 0:a.themeClass,onRender:a==null?void 0:a.onRender}},render(){var e;let{$slots:t,titlePlacement:n,vertical:r,dashed:i,cssVars:a,mergedClsPrefix:o}=this;return(e=this.onRender)==null||e.call(this),v(`div`,{role:`separator`,class:[`${o}-divider`,this.themeClass,{[`${o}-divider--vertical`]:r,[`${o}-divider--no-title`]:!t.default,[`${o}-divider--dashed`]:i,[`${o}-divider--title-position-${n}`]:t.default&&n}],style:a},r?null:v(`div`,{class:`${o}-divider__line ${o}-divider__line--left`}),!r&&t.default?v(_,null,v(`div`,{class:`${o}-divider__title`},this.$slots),v(`div`,{class:`${o}-divider__line ${o}-divider__line--right`})):null)}}),N=y(`n-popconfirm`),P={positiveText:String,negativeText:String,showIcon:{type:Boolean,default:!0},onPositiveClick:{type:Function,required:!0},onNegativeClick:{type:Function,required:!0}},F=E(P),I=c({name:`NPopconfirmPanel`,props:P,setup(e){let{localeRef:t}=A(`Popconfirm`),{inlineThemeDisabled:n}=p(),{mergedClsPrefixRef:r,mergedThemeRef:i,props:a}=s(N),o=g(()=>{let{common:{cubicBezierEaseInOut:e},self:{fontSize:t,iconSize:n,iconColor:r}}=i.value;return{"--n-bezier":e,"--n-font-size":t,"--n-icon-size":n,"--n-icon-color":r}}),c=n?l(`popconfirm-panel`,void 0,o,a):void 0;return Object.assign(Object.assign({},A(`Popconfirm`)),{mergedClsPrefix:r,cssVars:n?void 0:o,localizedPositiveText:g(()=>e.positiveText||t.value.positiveText),localizedNegativeText:g(()=>e.negativeText||t.value.negativeText),positiveButtonProps:C(a,`positiveButtonProps`),negativeButtonProps:C(a,`negativeButtonProps`),handlePositiveClick(t){e.onPositiveClick(t)},handleNegativeClick(t){e.onNegativeClick(t)},themeClass:c==null?void 0:c.themeClass,onRender:c==null?void 0:c.onRender})},render(){var e;let{mergedClsPrefix:n,showIcon:i,$slots:o}=this,s=t(o.action,()=>this.negativeText===null&&this.positiveText===null?[]:[this.negativeText!==null&&v(a,Object.assign({size:`small`,onClick:this.handleNegativeClick},this.negativeButtonProps),{default:()=>this.localizedNegativeText}),this.positiveText!==null&&v(a,Object.assign({size:`small`,type:`primary`,onClick:this.handlePositiveClick},this.positiveButtonProps),{default:()=>this.localizedPositiveText})]);return(e=this.onRender)==null||e.call(this),v(`div`,{class:[`${n}-popconfirm__panel`,this.themeClass],style:this.cssVars},r(o.default,e=>i||e?v(`div`,{class:`${n}-popconfirm__body`},i?v(`div`,{class:`${n}-popconfirm__icon`},t(o.icon,()=>[v(T,{clsPrefix:n},{default:()=>v(w,null)})])):null,e):null),s?v(`div`,{class:[`${n}-popconfirm__action`]},s):null)}}),L=h(`popconfirm`,[u(`body`,`
 font-size: var(--n-font-size);
 display: flex;
 align-items: center;
 flex-wrap: nowrap;
 position: relative;
 `,[u(`icon`,`
 display: flex;
 font-size: var(--n-icon-size);
 color: var(--n-icon-color);
 transition: color .3s var(--n-bezier);
 margin: 0 8px 0 0;
 `)]),u(`action`,`
 display: flex;
 justify-content: flex-end;
 `,[d(`&:not(:first-child)`,`margin-top: 8px`),h(`button`,[d(`&:not(:last-child)`,`margin-right: 8px;`)])])]),R=c({name:`Popconfirm`,props:Object.assign(Object.assign(Object.assign({},b.props),o),{positiveText:String,negativeText:String,showIcon:{type:Boolean,default:!0},trigger:{type:String,default:`click`},positiveButtonProps:Object,negativeButtonProps:Object,onPositiveClick:Function,onNegativeClick:Function}),slots:Object,__popover__:!0,setup(e){let{mergedClsPrefixRef:t}=p(),r=b(`Popconfirm`,`-popconfirm`,L,k,e,t),i=S(null);function a(t){var r;if(!((r=i.value)!=null&&r.getMergedShow()))return;let{onPositiveClick:a,"onUpdate:show":o}=e;Promise.resolve(a?a(t):!0).then(e=>{var t;e!==!1&&((t=i.value)==null||t.setShow(!1),o&&n(o,!1))})}function o(t){var r;if(!((r=i.value)!=null&&r.getMergedShow()))return;let{onNegativeClick:a,"onUpdate:show":o}=e;Promise.resolve(a?a(t):!0).then(e=>{var t;e!==!1&&((t=i.value)==null||t.setShow(!1),o&&n(o,!1))})}return f(N,{mergedThemeRef:r,mergedClsPrefixRef:t,props:e}),{setShow(e){var t;(t=i.value)==null||t.setShow(e)},syncPosition(){var e;(e=i.value)==null||e.syncPosition()},mergedTheme:r,popoverInstRef:i,handlePositiveClick:a,handleNegativeClick:o}},render(){let{$slots:t,$props:n,mergedTheme:r}=this;return v(i,Object.assign({},D(n,F),{theme:r.peers.Popover,themeOverrides:r.peerOverrides.Popover,internalExtraClass:[`popconfirm`],ref:`popoverInstRef`}),{trigger:t.trigger,default:()=>{let r=e(n,F);return v(I,Object.assign({},r,{onPositiveClick:this.handlePositiveClick,onNegativeClick:this.handleNegativeClick}),t)}})}});export{M as n,R as t};