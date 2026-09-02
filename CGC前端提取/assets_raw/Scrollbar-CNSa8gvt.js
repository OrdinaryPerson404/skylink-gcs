import{S as e}from"./mavlink-C7-F-u0_.js";import{$ as t,Et as n,F as r,H as i,I as a,J as o,K as s,U as c,Z as l,_t as u,kt as d,o as f,rn as p}from"./_plugin-vue_export-helper-CQMKH6SZ.js";import{F as m,M as h,N as g,P as _,R as v,o as y}from"./index-DJhI6atv.js";var b={success:d(g,null),error:d(m,null),warning:d(h,null),info:d(_,null)},x=n({name:`ProgressCircle`,props:{clsPrefix:{type:String,required:!0},status:{type:String,required:!0},strokeWidth:{type:Number,required:!0},fillColor:[String,Object],railColor:String,railStyle:[String,Object],percentage:{type:Number,default:0},offsetDegree:{type:Number,default:0},showIndicator:{type:Boolean,required:!0},indicatorTextColor:String,unit:String,viewBoxWidth:{type:Number,required:!0},gapDegree:{type:Number,required:!0},gapOffsetDegree:{type:Number,default:0}},setup(e,{slots:n}){let r=u(()=>{let n=`gradient`,{fillColor:r}=e;return typeof r==`object`?`${n}-${t(JSON.stringify(r))}`:n});function i(t,n,i,a){let{gapDegree:o,viewBoxWidth:s,strokeWidth:c}=e,l=50+c/2,u=`M ${l},${l} m 0,50
      a 50,50 0 1 1 0,-100
      a 50,50 0 1 1 0,100`,d=Math.PI*2*50;return{pathString:u,pathStyle:{stroke:a===`rail`?i:typeof e.fillColor==`object`?`url(#${r.value})`:i,strokeDasharray:`${Math.min(t,100)/100*(d-o)}px ${s*8}px`,strokeDashoffset:`-${o/2}px`,transformOrigin:n?`center`:void 0,transform:n?`rotate(${n}deg)`:void 0}}}let a=()=>{let t=typeof e.fillColor==`object`,n=t?e.fillColor.stops[0]:``,i=t?e.fillColor.stops[1]:``;return t&&d(`defs`,null,d(`linearGradient`,{id:r.value,x1:`0%`,y1:`100%`,x2:`100%`,y2:`0%`},d(`stop`,{offset:`0%`,"stop-color":n}),d(`stop`,{offset:`100%`,"stop-color":i})))};return()=>{let{fillColor:t,railColor:r,strokeWidth:o,offsetDegree:s,status:c,percentage:l,showIndicator:u,indicatorTextColor:f,unit:p,gapOffsetDegree:m,clsPrefix:h}=e,{pathString:g,pathStyle:_}=i(100,0,r,`rail`),{pathString:y,pathStyle:x}=i(l,s,t,`fill`),S=100+o;return d(`div`,{class:`${h}-progress-content`,role:`none`},d(`div`,{class:`${h}-progress-graph`,"aria-hidden":!0},d(`div`,{class:`${h}-progress-graph-circle`,style:{transform:m?`rotate(${m}deg)`:void 0}},d(`svg`,{viewBox:`0 0 ${S} ${S}`},a(),d(`g`,null,d(`path`,{class:`${h}-progress-graph-circle-rail`,d:g,"stroke-width":o,"stroke-linecap":`round`,fill:`none`,style:_})),d(`g`,null,d(`path`,{class:[`${h}-progress-graph-circle-fill`,l===0&&`${h}-progress-graph-circle-fill--empty`],d:y,"stroke-width":o,"stroke-linecap":`round`,fill:`none`,style:x}))))),u?d(`div`,null,n.default?d(`div`,{class:`${h}-progress-custom-content`,role:`none`},n.default()):c==="default"?d(`div`,{class:`${h}-progress-text`,style:{color:f},role:`none`},d(`span`,{class:`${h}-progress-text__percentage`},l),d(`span`,{class:`${h}-progress-text__unit`},p)):d(`div`,{class:`${h}-progress-icon`,"aria-hidden":!0},d(v,{clsPrefix:h},{default:()=>b[c]}))):null)}}}),S={success:d(g,null),error:d(m,null),warning:d(h,null),info:d(_,null)},C=n({name:`ProgressLine`,props:{clsPrefix:{type:String,required:!0},percentage:{type:Number,default:0},railColor:String,railStyle:[String,Object],fillColor:[String,Object],status:{type:String,required:!0},indicatorPlacement:{type:String,required:!0},indicatorTextColor:String,unit:{type:String,default:`%`},processing:{type:Boolean,required:!0},showIndicator:{type:Boolean,required:!0},height:[String,Number],railBorderRadius:[String,Number],fillBorderRadius:[String,Number]},setup(e,{slots:t}){let n=u(()=>l(e.height)),r=u(()=>{var t,n;return typeof e.fillColor==`object`?`linear-gradient(to right, ${(t=e.fillColor)==null?void 0:t.stops[0]} , ${(n=e.fillColor)==null?void 0:n.stops[1]})`:e.fillColor}),i=u(()=>e.railBorderRadius===void 0?e.height===void 0?``:l(e.height,{c:.5}):l(e.railBorderRadius)),a=u(()=>e.fillBorderRadius===void 0?e.railBorderRadius===void 0?e.height===void 0?``:l(e.height,{c:.5}):l(e.railBorderRadius):l(e.fillBorderRadius));return()=>{let{indicatorPlacement:o,railColor:s,railStyle:c,percentage:l,unit:u,indicatorTextColor:f,status:p,showIndicator:m,processing:h,clsPrefix:g}=e;return d(`div`,{class:`${g}-progress-content`,role:`none`},d(`div`,{class:`${g}-progress-graph`,"aria-hidden":!0},d(`div`,{class:[`${g}-progress-graph-line`,{[`${g}-progress-graph-line--indicator-${o}`]:!0}]},d(`div`,{class:`${g}-progress-graph-line-rail`,style:[{backgroundColor:s,height:n.value,borderRadius:i.value},c]},d(`div`,{class:[`${g}-progress-graph-line-fill`,h&&`${g}-progress-graph-line-fill--processing`],style:{maxWidth:`${e.percentage}%`,background:r.value,height:n.value,lineHeight:n.value,borderRadius:a.value}},o===`inside`?d(`div`,{class:`${g}-progress-graph-line-indicator`,style:{color:f}},t.default?t.default():`${l}${u}`):null)))),m&&o===`outside`?d(`div`,null,t.default?d(`div`,{class:`${g}-progress-custom-content`,style:{color:f},role:`none`},t.default()):p==="default"?d(`div`,{role:`none`,class:`${g}-progress-icon ${g}-progress-icon--as-text`,style:{color:f}},l,u):d(`div`,{class:`${g}-progress-icon`,"aria-hidden":!0},d(v,{clsPrefix:g},{default:()=>S[p]}))):null)}}});function w(e,t,n=100){return`m ${n/2} ${n/2-e} a ${e} ${e} 0 1 1 0 ${2*e} a ${e} ${e} 0 1 1 0 -${2*e}`}var T=n({name:`ProgressMultipleCircle`,props:{clsPrefix:{type:String,required:!0},viewBoxWidth:{type:Number,required:!0},percentage:{type:Array,default:[0]},strokeWidth:{type:Number,required:!0},circleGap:{type:Number,required:!0},showIndicator:{type:Boolean,required:!0},fillColor:{type:Array,default:()=>[]},railColor:{type:Array,default:()=>[]},railStyle:{type:Array,default:()=>[]}},setup(e,{slots:t}){let n=u(()=>e.percentage.map((t,n)=>`${Math.PI*t/100*(e.viewBoxWidth/2-e.strokeWidth/2*(1+2*n)-e.circleGap*n)*2}, ${e.viewBoxWidth*8}`)),r=(t,n)=>{let r=e.fillColor[n],i=typeof r==`object`?r.stops[0]:``,a=typeof r==`object`?r.stops[1]:``;return typeof e.fillColor[n]==`object`&&d(`linearGradient`,{id:`gradient-${n}`,x1:`100%`,y1:`0%`,x2:`0%`,y2:`100%`},d(`stop`,{offset:`0%`,"stop-color":i}),d(`stop`,{offset:`100%`,"stop-color":a}))};return()=>{let{viewBoxWidth:i,strokeWidth:a,circleGap:o,showIndicator:s,fillColor:c,railColor:l,railStyle:u,percentage:f,clsPrefix:p}=e;return d(`div`,{class:`${p}-progress-content`,role:`none`},d(`div`,{class:`${p}-progress-graph`,"aria-hidden":!0},d(`div`,{class:`${p}-progress-graph-circle`},d(`svg`,{viewBox:`0 0 ${i} ${i}`},d(`defs`,null,f.map((e,t)=>r(e,t))),f.map((e,t)=>d(`g`,{key:t},d(`path`,{class:`${p}-progress-graph-circle-rail`,d:w(i/2-a/2*(1+2*t)-o*t,a,i),"stroke-width":a,"stroke-linecap":`round`,fill:`none`,style:[{strokeDashoffset:0,stroke:l[t]},u[t]]}),d(`path`,{class:[`${p}-progress-graph-circle-fill`,e===0&&`${p}-progress-graph-circle-fill--empty`],d:w(i/2-a/2*(1+2*t)-o*t,a,i),"stroke-width":a,"stroke-linecap":`round`,fill:`none`,style:{strokeDasharray:n.value[t],strokeDashoffset:0,stroke:typeof c[t]==`object`?`url(#gradient-${t})`:c[t]}})))))),s&&t.default?d(`div`,null,d(`div`,{class:`${p}-progress-text`},t.default())):null)}}}),E=i([c(`progress`,{display:`inline-block`},[c(`progress-icon`,`
 color: var(--n-icon-color);
 transition: color .3s var(--n-bezier);
 `),s(`line`,`
 width: 100%;
 display: block;
 `,[c(`progress-content`,`
 display: flex;
 align-items: center;
 `,[c(`progress-graph`,{flex:1})]),c(`progress-custom-content`,{marginLeft:`14px`}),c(`progress-icon`,`
 width: 30px;
 padding-left: 14px;
 height: var(--n-icon-size-line);
 line-height: var(--n-icon-size-line);
 font-size: var(--n-icon-size-line);
 `,[s(`as-text`,`
 color: var(--n-text-color-line-outer);
 text-align: center;
 width: 40px;
 font-size: var(--n-font-size);
 padding-left: 4px;
 transition: color .3s var(--n-bezier);
 `)])]),s(`circle, dashboard`,{width:`120px`},[c(`progress-custom-content`,`
 position: absolute;
 left: 50%;
 top: 50%;
 transform: translateX(-50%) translateY(-50%);
 display: flex;
 align-items: center;
 justify-content: center;
 `),c(`progress-text`,`
 position: absolute;
 left: 50%;
 top: 50%;
 transform: translateX(-50%) translateY(-50%);
 display: flex;
 align-items: center;
 color: inherit;
 font-size: var(--n-font-size-circle);
 color: var(--n-text-color-circle);
 font-weight: var(--n-font-weight-circle);
 transition: color .3s var(--n-bezier);
 white-space: nowrap;
 `),c(`progress-icon`,`
 position: absolute;
 left: 50%;
 top: 50%;
 transform: translateX(-50%) translateY(-50%);
 display: flex;
 align-items: center;
 color: var(--n-icon-color);
 font-size: var(--n-icon-size-circle);
 `)]),s(`multiple-circle`,`
 width: 200px;
 color: inherit;
 `,[c(`progress-text`,`
 font-weight: var(--n-font-weight-circle);
 color: var(--n-text-color-circle);
 position: absolute;
 left: 50%;
 top: 50%;
 transform: translateX(-50%) translateY(-50%);
 display: flex;
 align-items: center;
 justify-content: center;
 transition: color .3s var(--n-bezier);
 `)]),c(`progress-content`,{position:`relative`}),c(`progress-graph`,{position:`relative`},[c(`progress-graph-circle`,[i(`svg`,{verticalAlign:`bottom`}),c(`progress-graph-circle-fill`,`
 stroke: var(--n-fill-color);
 transition:
 opacity .3s var(--n-bezier),
 stroke .3s var(--n-bezier),
 stroke-dasharray .3s var(--n-bezier);
 `,[s(`empty`,{opacity:0})]),c(`progress-graph-circle-rail`,`
 transition: stroke .3s var(--n-bezier);
 overflow: hidden;
 stroke: var(--n-rail-color);
 `)]),c(`progress-graph-line`,[s(`indicator-inside`,[c(`progress-graph-line-rail`,`
 height: 16px;
 line-height: 16px;
 border-radius: 10px;
 `,[c(`progress-graph-line-fill`,`
 height: inherit;
 border-radius: 10px;
 `),c(`progress-graph-line-indicator`,`
 background: #0000;
 white-space: nowrap;
 text-align: right;
 margin-left: 14px;
 margin-right: 14px;
 height: inherit;
 font-size: 12px;
 color: var(--n-text-color-line-inner);
 transition: color .3s var(--n-bezier);
 `)])]),s(`indicator-inside-label`,`
 height: 16px;
 display: flex;
 align-items: center;
 `,[c(`progress-graph-line-rail`,`
 flex: 1;
 transition: background-color .3s var(--n-bezier);
 `),c(`progress-graph-line-indicator`,`
 background: var(--n-fill-color);
 font-size: 12px;
 transform: translateZ(0);
 display: flex;
 vertical-align: middle;
 height: 16px;
 line-height: 16px;
 padding: 0 10px;
 border-radius: 10px;
 position: absolute;
 white-space: nowrap;
 color: var(--n-text-color-line-inner);
 transition:
 right .2s var(--n-bezier),
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 `)]),c(`progress-graph-line-rail`,`
 position: relative;
 overflow: hidden;
 height: var(--n-rail-height);
 border-radius: 5px;
 background-color: var(--n-rail-color);
 transition: background-color .3s var(--n-bezier);
 `,[c(`progress-graph-line-fill`,`
 background: var(--n-fill-color);
 position: relative;
 border-radius: 5px;
 height: inherit;
 width: 100%;
 max-width: 0%;
 transition:
 background-color .3s var(--n-bezier),
 max-width .2s var(--n-bezier);
 `,[s(`processing`,[i(`&::after`,`
 content: "";
 background-image: var(--n-line-bg-processing);
 animation: progress-processing-animation 2s var(--n-bezier) infinite;
 `)])])])])])]),i(`@keyframes progress-processing-animation`,`
 0% {
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 right: 100%;
 opacity: 1;
 }
 66% {
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 right: 0;
 opacity: 0;
 }
 100% {
 position: absolute;
 left: 0;
 top: 0;
 bottom: 0;
 right: 0;
 opacity: 0;
 }
 `)]),D=n({name:`Progress`,props:Object.assign(Object.assign({},f.props),{processing:Boolean,type:{type:String,default:`line`},gapDegree:Number,gapOffsetDegree:Number,status:{type:String,default:`default`},railColor:[String,Array],railStyle:[String,Array],color:[String,Array,Object],viewBoxWidth:{type:Number,default:100},strokeWidth:{type:Number,default:7},percentage:[Number,Array],unit:{type:String,default:`%`},showIndicator:{type:Boolean,default:!0},indicatorPosition:{type:String,default:`outside`},indicatorPlacement:{type:String,default:`outside`},indicatorTextColor:String,circleGap:{type:Number,default:1},height:Number,borderRadius:[String,Number],fillBorderRadius:[String,Number],offsetDegree:Number}),setup(e){let t=u(()=>e.indicatorPlacement||e.indicatorPosition),n=u(()=>{if(e.gapDegree||e.gapDegree===0)return e.gapDegree;if(e.type===`dashboard`)return 75}),{mergedClsPrefixRef:i,inlineThemeDisabled:s}=a(e),c=f(`Progress`,`-progress`,E,y,e,i),l=u(()=>{let{status:t}=e,{common:{cubicBezierEaseInOut:n},self:{fontSize:r,fontSizeCircle:i,railColor:a,railHeight:s,iconSizeCircle:l,iconSizeLine:u,textColorCircle:d,textColorLineInner:f,textColorLineOuter:p,lineBgProcessing:m,fontWeightCircle:h,[o(`iconColor`,t)]:g,[o(`fillColor`,t)]:_}}=c.value;return{"--n-bezier":n,"--n-fill-color":_,"--n-font-size":r,"--n-font-size-circle":i,"--n-font-weight-circle":h,"--n-icon-color":g,"--n-icon-size-circle":l,"--n-icon-size-line":u,"--n-line-bg-processing":m,"--n-rail-color":a,"--n-rail-height":s,"--n-text-color-circle":d,"--n-text-color-line-inner":f,"--n-text-color-line-outer":p}}),d=s?r(`progress`,u(()=>e.status[0]),l,e):void 0;return{mergedClsPrefix:i,mergedIndicatorPlacement:t,gapDeg:n,cssVars:s?void 0:l,themeClass:d==null?void 0:d.themeClass,onRender:d==null?void 0:d.onRender}},render(){let{type:e,cssVars:t,indicatorTextColor:n,showIndicator:r,status:i,railColor:a,railStyle:o,color:s,percentage:c,viewBoxWidth:l,strokeWidth:u,mergedIndicatorPlacement:f,unit:p,borderRadius:m,fillBorderRadius:h,height:g,processing:_,circleGap:v,mergedClsPrefix:y,gapDeg:b,gapOffsetDegree:S,themeClass:w,$slots:E,onRender:D}=this;return D==null||D(),d(`div`,{class:[w,`${y}-progress`,`${y}-progress--${e}`,`${y}-progress--${i}`],style:t,"aria-valuemax":100,"aria-valuemin":0,"aria-valuenow":c,role:e===`circle`||e===`line`||e===`dashboard`?`progressbar`:`none`},e===`circle`||e===`dashboard`?d(x,{clsPrefix:y,status:i,showIndicator:r,indicatorTextColor:n,railColor:a,fillColor:s,railStyle:o,offsetDegree:this.offsetDegree,percentage:c,viewBoxWidth:l,strokeWidth:u,gapDegree:b===void 0?e===`dashboard`?75:0:b,gapOffsetDegree:S,unit:p},E):e===`line`?d(C,{clsPrefix:y,status:i,showIndicator:r,indicatorTextColor:n,railColor:a,fillColor:s,railStyle:o,percentage:c,processing:_,indicatorPlacement:f,unit:p,fillBorderRadius:h,railBorderRadius:m,height:g},E):e===`multiple-circle`?d(T,{clsPrefix:y,strokeWidth:u,railColor:a,fillColor:s,railStyle:o,viewBoxWidth:l,percentage:c,showIndicator:r,circleGap:v},E):null)}}),O=n({name:`Scrollbar`,props:Object.assign(Object.assign({},f.props),{trigger:String,xScrollable:Boolean,onScroll:Function,contentClass:String,contentStyle:[Object,String],size:Number,yPlacement:{type:String,default:`right`},xPlacement:{type:String,default:`bottom`}}),setup(){let e=p(null);return Object.assign(Object.assign({},{scrollTo:(...t)=>{var n;(n=e.value)==null||n.scrollTo(t[0],t[1])},scrollBy:(...t)=>{var n;(n=e.value)==null||n.scrollBy(t[0],t[1])}}),{scrollbarInstRef:e})},render(){return d(e,Object.assign({ref:`scrollbarInstRef`},this.$props),this.$slots)}});export{D as n,O as t};