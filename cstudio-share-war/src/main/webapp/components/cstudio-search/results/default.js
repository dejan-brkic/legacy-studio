CStudioSearch.ResultRenderer.Default = {
	render: function(contentTO) {
		var liveUrl = CStudioAuthoringContext.liveAppBaseUri + contentTO.item.browserUri;

		if (!CStudioAuthoring.Utils.isEmpty(contentTO.item.contentType)) {
			var ctype = contentTO.item.contentType;
		}
		
		var onPreviewCode = "CStudioAuthoring.Service.lookupContentItem('" + 
		                    CStudioAuthoringContext.site + 
		                    "', '" +  
		                    contentTO.item.uri +"', " +
		                    "{ success:function(to) { CStudioAuthoring.Operations.openPreview(to.item, '" + 
		                    window.id +
		                    "', false, false); }, failure: function() {} }, false); return false;";

		var result = 
			"<a href='#' " +
				"onclick=\"" + onPreviewCode + "\" " +
				"class='" + ((contentTO.item && contentTO.item.component)?"cstudio-search-no-preview":"cstudio-search-download-link") +
			       	"'>"+contentTO.item.internalName+(contentTO.item.newFile?"*":"")+"</a>" +
				"<span class='cstudio-search-download-link-additional'> | " + 
					CStudioSearch.getContentTypeName(contentTO.item.contentType) + 
				"</span>" +
				"<br />"+
				"<div class='cstudio-search-result-detail'>" +
					"<span class='cstudio-search-description'>" +
						contentTO.item.metaDescription + 
					"</span>";
					
					if(contentTO.item.previewable && contentTO.item.previewable == true) {
						result +=
							"<div>"+liveUrl+"</div>"
					}
					
			result +=
				"</div>"+
				"<span class='cstudio-search-download-additional'>Edited "+ 
					CStudioAuthoring.Utils.formatDateFromString(contentTO.item.eventDate, "simpleformat") + 
					" by " + CStudioAuthoring.Utils.getAuthorFullNameFromContentTOItem(contentTO.item) +
				"</span>";
				
		return CStudioSearch.renderCommonResultWrapper(contentTO, result);
	}
};

// register renderer
CStudioSearch.resultRenderers["default"] = CStudioSearch.ResultRenderer.Default;
