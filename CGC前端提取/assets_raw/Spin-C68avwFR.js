import{At as e,D as t,F as n,I as r,Nt as i,O as a,gt as o,p as s,w as c}from"./mavlink-C7-F-u0_.js";import{Et as l,F as u,G as d,H as f,Ht as p,I as m,J as h,K as g,U as _,Zt as v,_t as y,kt as b,o as x,q as S,rn as C}from"./_plugin-vue_export-helper-CQMKH6SZ.js";import{a as w}from"./index-DJhI6atv.js";var T=`0!important`,E=`-1px!important`;function D(e){return g(`${e}-type`,[f(`& +`,[_(`button`,{},[g(`${e}-type`,[d(`border`,{borderLeftWidth:T}),d(`state-border`,{left:E})])])])])}function O(e){return g(`${e}-type`,[f(`& +`,[_(`button`,[g(`${e}-type`,[d(`border`,{borderTopWidth:T}),d(`state-border`,{top:E})])])])])}var k=_(`button-group`,`
 flex-wrap: nowrap;
 display: inline-flex;
 position: relative;
`,[S(`vertical`,{flexDirection:`row`},[S(`rtl`,[_(`button`,[f(`&:first-child:not(:last-child)`,`
 margin-right: ${T};
 border-top-right-radius: ${T};
 border-bottom-right-radius: ${T};
 `),f(`&:last-child:not(:first-child)`,`
 margin-left: ${T};
 border-top-left-radius: ${T};
 border-bottom-left-radius: ${T};
 `),f(`&:not(:first-child):not(:last-child)`,`
 margin-left: ${T};
 margin-right: ${T};
 border-radius: ${T};
 `),D(`default`),g(`ghost`,[D(`primary`),D(`info`),D(`success`),D(`warning`),D(`error`)])])])]),g(`vertical`,{flexDirection:`column`},[_(`button`,[f(`&:first-child:not(:last-child)`,`
 margin-bottom: ${T};
 margin-left: ${T};
 margin-right: ${T};
 border-bottom-left-radius: ${T};
 border-bottom-right-radius: ${T};
 `),f(`&:last-child:not(:first-child)`,`
 margin-top: ${T};
 margin-left: ${T};
 margin-right: ${T};
 border-top-left-radius: ${T};
 border-top-right-radius: ${T};
 `),f(`&:not(:first-child):not(:last-child)`,`
 margin: ${T};
 border-radius: ${T};
 `),O(`default`),g(`ghost`,[O(`primary`),O(`info`),O(`success`),O(`warning`),O(`error`)])])])]),A=l({name:`ButtonGroup`,props:{size:String,vertical:Boolean},setup(e){let{mergedClsPrefixRef:t,mergedRtlRef:i}=m(e);return n(`-button-group`,k,t),p(s,e),{rtlEnabled:r(`ButtonGroup`,i,t),mergedClsPrefix:t}},render(){let{mergedClsPrefix:e}=this;return b(`div`,{class:[`${e}-button-group`,this.rtlEnabled&&`${e}-button-group--rtl`,this.vertical&&`${e}-button-group--vertical`],role:`group`},this.$slots)}}),j=f([f(`@keyframes spin-rotate`,`
 from {
 transform: rotate(0);
 }
 to {
 transform: rotate(360deg);
 }
 `),_(`spin-container`,`
 position: relative;
 `,[_(`spin-body`,`
 position: absolute;
 top: 50%;
 left: 50%;
 transform: translateX(-50%) translateY(-50%);
 `,[c()])]),_(`spin-body`,`
 display: inline-flex;
 align-items: center;
 justify-content: center;
 flex-direction: column;
 `),_(`spin`,`
 display: inline-flex;
 height: var(--n-size);
 width: var(--n-size);
 font-size: var(--n-size);
 color: var(--n-color);
 `,[g(`rotate`,`
 animation: spin-rotate 2s linear infinite;
 `)]),_(`spin-description`,`
 display: inline-block;
 font-size: var(--n-font-size);
 color: var(--n-text-color);
 transition: color .3s var(--n-bezier);
 margin-top: 8px;
 `),_(`spin-content`,`
 opacity: 1;
 transition: opacity .3s var(--n-bezier);
 pointer-events: all;
 `,[g(`spinning`,`
 user-select: none;
 -webkit-user-select: none;
 pointer-events: none;
 opacity: var(--n-opacity-spinning);
 `)])]),M={small:20,medium:18,large:16},N=l({name:`Spin`,props:Object.assign(Object.assign(Object.assign({},x.props),{contentClass:String,contentStyle:[Object,String],description:String,size:{type:[String,Number],default:`medium`},show:{type:Boolean,default:!0},rotate:{type:Boolean,default:!0},spinning:{type:Boolean,validator:()=>!0,default:void 0},delay:Number}),a),slots:Object,setup(t){let{mergedClsPrefixRef:n,inlineThemeDisabled:r}=m(t),i=x(`Spin`,`-spin`,j,w,t,n),a=y(()=>{let{size:n}=t,{common:{cubicBezierEaseInOut:r},self:a}=i.value,{opacitySpinning:o,color:s,textColor:c}=a;return{"--n-bezier":r,"--n-opacity-spinning":o,"--n-size":typeof n==`number`?e(n):a[h(`size`,n)],"--n-color":s,"--n-text-color":c}}),s=r?u(`spin`,y(()=>{let{size:e}=t;return typeof e==`number`?String(e):e[0]}),a,t):void 0,c=o(t,[`spinning`,`show`]),l=C(!1);return v(e=>{let n;if(c.value){let{delay:r}=t;if(r){n=window.setTimeout(()=>{l.value=!0},r),e(()=>{clearTimeout(n)});return}}l.value=c.value}),{mergedClsPrefix:n,active:l,mergedStrokeWidth:y(()=>{let{strokeWidth:e}=t;if(e!==void 0)return e;let{size:n}=t;return M[typeof n==`number`?`medium`:n]}),cssVars:r?void 0:a,themeClass:s==null?void 0:s.themeClass,onRender:s==null?void 0:s.onRender}},render(){var e,n;let{$slots:r,mergedClsPrefix:a,description:o}=this,s=r.icon&&this.rotate,c=(o||r.description)&&b(`div`,{class:`${a}-spin-description`},o||((e=r.description)==null?void 0:e.call(r))),l=r.icon?b(`div`,{class:[`${a}-spin-body`,this.themeClass]},b(`div`,{class:[`${a}-spin`,s&&`${a}-spin--rotate`],style:r.default?``:this.cssVars},r.icon()),c):b(`div`,{class:[`${a}-spin-body`,this.themeClass]},b(t,{clsPrefix:a,style:r.default?``:this.cssVars,stroke:this.stroke,"stroke-width":this.mergedStrokeWidth,radius:this.radius,scale:this.scale,class:`${a}-spin`}),c);return(n=this.onRender)==null||n.call(this),r.default?b(`div`,{class:[`${a}-spin-container`,this.themeClass],style:this.cssVars},b(`div`,{class:[`${a}-spin-content`,this.active&&`${a}-spin-content--spinning`,this.contentClass],style:this.contentStyle},r),b(i,{name:`fade-in-transition`},{default:()=>this.active?l:null})):l}});export{A as n,N as t};