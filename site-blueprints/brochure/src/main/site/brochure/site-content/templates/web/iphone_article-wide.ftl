<!DOCTYPE html>
<!-- Conditional comment for mobile ie7 http://blogs.msdn.com/b/iemobile/ -->
<!--[if IEMobile 7 ]> <html class="no-js iem7" lang="en"> <![endif]-->
<!--[if (gt IEMobile 7)|!(IEMobile)]><!--> <html class="no-js" lang="en"> <!--<![endif]-->

	<head>
		<#include "/templates/web/components/mobile_common-head.ftl">
		<link rel="stylesheet" type="text/css" href="${urlTransformationService.transform('toWebAppRelativeUrl', '/static-assets/mobile/css/iphone.css')}" media="screen" />
	</head>

	<body>
		<#include "/templates/web/components/iphone_common-header.ftl" />
		
		<div data-role="page" data-title="Acme - Making our customers proud">
			
			<div data-role="content" data-theme="d">
				<article class="main-col">
					<div id="main-content">
						${model.body_html}
					</div>
				</article>
			</div>
			<#include "/templates/web/components/mobile_page-footer.ftl" />
		</div> <!-- page -->
		<#include "/templates/web/components/mobile_common-footer.ftl" />
	</body>
</html>