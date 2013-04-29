CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/lib/codemirror.js");
CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/lib/util/formatting.js");
CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/mode/xml/xml.js");
CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/mode/javascript/javascript.js");
CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/mode/htmlmixed/htmlmixed.js");
CStudioAuthoring.Utils.addJavascript("/components/cstudio-common/codemirror/mode/css/css.js");
CStudioAuthoring.Utils.addCss("/components/cstudio-common/codemirror/lib/codemirror.css");
//CStudioAuthoring.Utils.addCss("/components/cstudio-common/codemirror/doc/docs.css");
//CStudioAuthoring.Utils.addCss("/components/cstudio-common/codemirror/theme/neat.css");
CStudioAuthoring.Utils.addCss("/themes/cstudioTheme/css/template-editor.css");

CStudioAuthoring.Module.requireModule(
	"cstudio-forms-engine",
	'/components/cstudio-forms/forms-engine.js',
	{  },
	{ moduleLoaded: function() {
		
		
CStudioForms.TemplateEditor = CStudioForms.TemplateEditor ||  function()  {

	return this;
}

CStudioForms.TemplateEditor.prototype = {

	render: function(templatePath, channel, onSaveCb) {
		
		var serviceUri = CStudioAuthoringContext.baseUri
		    + "/proxy/alfresco/cstudio/wcm/content/get-content" 
			+ "?site=" + CStudioAuthoringContext.site 
			+ "&path=" + templatePath 
			+ "&edit=true";
	
		var getContentCb = {
			success: function(response) {
				this.context.renderTemplateEditor(templatePath, response.responseText, onSaveCb);
			},
			failure: function() {
			},
			context: this
		};
		
		YAHOO.util.Connect.asyncRequest('GET', serviceUri, getContentCb);				
	},
	
	renderTemplateEditor: function(templatePath, content, onSaveCb) {

		var permsCallback = {
				success: function(response) {
					var isWrite = CStudioAuthoring.Service.isWrite(response.permissions);

					var modalEl = document.createElement("div");
					modalEl.id = "cstudio-template-editor-container-modal";
					document.body.appendChild(modalEl);
							
					var containerEl = document.createElement("div");
					containerEl.id = "cstudio-template-editor-container";
					YAHOO.util.Dom.addClass(containerEl, 'seethrough');
					modalEl.appendChild(containerEl);
					
					var formHTML = 
						"<div id='editor-container'>"+
						"</div>" + 
						"<div id='template-editor-button-container'>";
						
					if(isWrite == true) {
						formHTML += 
 						    "<div  id='template-editor-update-button' class='cstudio-template-editor-button'>Update</div>" + 
							"<div  id='template-editor-cancel-button' style='right: 120px;' class='cstudio-template-editor-button'>Cancel</div>";
					}
					else {
						formHTML +=
							"<div  id='template-editor-cancel-button' style='right: 120px;' class='cstudio-template-editor-button'>Close</div>";							
					}

					formHTML +=
						"</div>";

					containerEl.innerHTML = formHTML;
					var editorContainerEl = document.getElementById("editor-container");	
					var editorEl = document.createElement("textarea");
					//editorEl.cols= "79";
					//editorEl.rows= "40";
					editorEl.style.backgroundColor= "white";
					editorEl.value= content;
					editorContainerEl.appendChild(editorEl);
					
					var initEditorFn = function() {
						if(typeof CodeMirror === "undefined" ) {
							window.setTimeout(500, initEditorFn);
						}
						else {		
							var mode = "htmlmixed";
							
							if(templatePath.indexOf(".css") != -1) {
								mode = "css";
							}
							else if(templatePath.indexOf(".js") != -1) {
								mode = "javascript";
							}
							
							editorEl.codeMirrorEditor = CodeMirror.fromTextArea(editorEl, {
								mode: mode,
								lineNumbers: true,
								lineWrapping: true,
								smartIndent: false//
								//    onGutterClick: foldFunc,
								//    extraKeys: {"Ctrl-Q": function(cm){foldFunc(cm, cm.getCursor().line);}}
			  				});
			  				
			  				var codeEditorEl = YAHOO.util.Dom.getElementsByClassName("CodeMirror", null, editorContainerEl)[0];
			  				codeEditorEl.style.backgroundColor = "white";
			  				
			  				var codeEditorCanvasEl = YAHOO.util.Dom.getElementsByClassName("CodeMirror-wrap", null, editorContainerEl)[0];
			  				codeEditorCanvasEl.style.height = "100%";

			  				var codeEditorScrollEl = YAHOO.util.Dom.getElementsByClassName("CodeMirror-scroll", null, editorContainerEl)[0];
			  				codeEditorScrollEl.style.height = "100%";
						}
					};
					
					initEditorFn();

					var cancelEl = document.getElementById('template-editor-cancel-button');
					cancelEl.onclick = function() {
			            var cancelEditServiceUrl = "/proxy/alfresco/cstudio/wcm/content/cancel-editing"
			                + "?site=" + CStudioAuthoringContext.site
			                + "&path=" + templatePath;

			            var cancelEditCb = {
			                success: function(response) {
			                    modalEl.parentNode.removeChild(modalEl);
			                },
			                failure: function() {
			                }
			            };
			            YAHOO.util.Connect.asyncRequest('GET', CStudioAuthoring.Service.createServiceUri(cancelEditServiceUrl), cancelEditCb);
					};

					var saveSvcCb = {
						success: function() {
			                modalEl.parentNode.removeChild(modalEl);
			                onSaveCb.success();
						},
						failure: function() {
						}
					};
					
					if(isWrite == true) {
						var saveEl = document.getElementById('template-editor-update-button');
						saveEl.onclick = function() {
							editorEl.codeMirrorEditor.save();
							var value = editorEl.value;
						
							var writeServiceUrl = "/proxy/alfresco/cstudio/wcm/content/write-content-asset"
							                    + "?site=" + CStudioAuthoringContext.site 
							                    + "&path=" + templatePath;
							                    
							YAHOO.util.Connect.setDefaultPostHeader(false);
							YAHOO.util.Connect.initHeader("Content-Type", "text/pain; charset=utf-8");
							YAHOO.util.Connect.asyncRequest('POST', CStudioAuthoring.Service.createServiceUri(writeServiceUrl), saveSvcCb, value);
						};
					}					

				},
				failure: function() {
					
				}
		}
		
		CStudioAuthoring.Service.getUserPermissions(
			CStudioAuthoringContext.site, 
			templatePath, 
			permsCallback);
		
	}	
};

CStudioAuthoring.Module.moduleLoaded("cstudio-forms-template-editor",CStudioForms.TemplateEditor);

}} );