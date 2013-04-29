<#import "/cstudio/wcm/import/cstudio-xforms-template.ftl" as template />
<#import "/org/alfresco/import/alfresco-layout.ftl" as layout />

<@template.header "transitional">
   <#-- allow theme to be specified in url args - helps debugging themes -->
   <#assign theme = (page.url.args.theme)!theme /> 
   <script type="text/javascript" src="${url.context}/yui/treeview/treeview-min.js"></script> 
   <script type="text/javascript" src="${url.context}/yui/animation/animation-min.js"></script>
   <script type="text/javascript" src="${url.context}/themes/cstudioTheme/js/global.js"></script>
   <script type="text/javascript" src="${url.context}/components/cstudio-form/swfobject.js"></script>
   <script type="text/javascript" src="${url.context}/components/cstudio-browse/browse.js"></script>

	<!-- filter templates -->
   <script type="text/javascript" src="${url.context}/components/cstudio-browse/filters/common.js"></script>
   <script type="text/javascript" src="${url.context}/components/cstudio-browse/filters/default.js"></script>

	<!-- result templates -->
   <script type="text/javascript" src="${url.context}/components/cstudio-search/results/default.js"></script>
	<#list searchResultTemplates as resultTemplate>
       <script type="text/javascript" src="${resultTemplate}"></script> 	
	</#list>
   
   <link href="${url.context}/themes/cstudioTheme/css/icons.css" type="text/css" rel="stylesheet">
   <link href="${url.context}/yui/container/assets/container.css" type="text/css" rel="stylesheet">
</@>

<@template.body>
  <@region id="cstudio-browse" scope="global" protected=true />
  <div id="cstudio-command-controls"></div>
</@>
