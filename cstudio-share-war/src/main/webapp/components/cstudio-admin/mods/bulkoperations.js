CStudioAuthoring.Utils.addCss("/components/cstudio-admin/mods/bulkoperations.css");
CStudioAdminConsole.Tool.BulkOperations = CStudioAdminConsole.Tool.BulkOperations ||  function(config, el)  {
    this.containerEl = el;
    this.config = config;
    this.types = [];
    return this;
}

/**
 * Overarching class that drives the content type tools
 */
YAHOO.extend(CStudioAdminConsole.Tool.BulkOperations, CStudioAdminConsole.Tool, {
    renderWorkarea: function() {
        var workareaEl = document.getElementById("cstudio-admin-console-workarea");

        workareaEl.innerHTML =
            "<div id='bulk-ops'>" +
            "</div>";

        var actions = [];

        CStudioAuthoring.ContextualNav.AdminConsoleNav.initActions(actions);

        this.renderJobsList();
    },

    renderRename: function() {
       CStudioAdminConsole.Tool.BulkOperations.rename = function() {
            var srcPath = document.getElementById("bulk-rename-src-path").value;
            var targetPath = document.getElementById("bulk-rename-target-path").value;
            if (srcPath && targetPath) {
                var serviceUri = "/proxy/alfresco/cstudio/util/bulk-rename?site="+CStudioAuthoringContext.site+"&srcPath="+srcPath+"&targetPath="+targetPath;
                var renameOpMessage = document.getElementById("bulk-rename-message");

                var cb = {
                    success:function() {
                        renameOpMessage.innerHTML = "Bulk rename successful";
                    },
                    failure: function() {
                        renameOpMessage.innerHTML = "Bulk rename failed";
                    }
                }
                YConnect.asyncRequest("POST", CStudioAuthoring.Service.createServiceUri(serviceUri), cb);
                renameOpMessage.innerHTML = "Executing bulk rename ...";
            }
        };

        var mainEl = document.getElementById("bulk-ops");

        mainEl.innerHTML =
            "<div id='bulk-rename' class='bulk-op-area'>" +
                "<p><h2>Bulk Rename</h2></p><p>" +
                    "<div class='bulk-table'>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>Source path:" +
                            "</div>" +
                            "<div class='bulk-table-cell'><input type='text' size=70 id='bulk-rename-src-path' />" +
                            "</div>" +
                        "</div>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>" +
                            "</div>" +
                            "<div class='bulk-table-cell'>(e.g. /site/website/about-us/index.xml)" +
                            "</div>" +
                        "</div>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>Target path:" +
                            "</div>" +
                            "<div class='bulk-table-cell'><input type='text' size=70 id='bulk-rename-target-path' />" +
                            "</div>" +
                        "</div>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>" +
                            "</div>" +
                            "<div class='bulk-table-cell'>(e.g. /site/website/about/index.xml)" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                " <br/>" +
                "<input type='button' class='action-button' value='Rename' onclick='CStudioAdminConsole.Tool.BulkOperations.rename()' /></p>" +
                "<p id='bulk-rename-message'></p>" +
            "</div>";

    },

    renderGoLive: function() {
         CStudioAdminConsole.Tool.BulkOperations.golive = function() {
         	 var envSelectEl = document.getElementById("go-pub-channel");
             var environment = envSelectEl[envSelectEl.selectedIndex].value;
             var path = document.getElementById("bulk-golive-path").value;
             if (path) {
                 var serviceUri = "/proxy/alfresco/cstudio/util/bulk-golive?site=" + CStudioAuthoringContext.site
                 	 + "&path=" + path + "&environment=" + environment;
                 var goLiveOpMessage = document.getElementById("bulk-golive-message");
                 var cb = {
                     success:function() {
                         goLiveOpMessage.innerHTML = "Bulk Go Live successful";
                     },
                     failure: function() {
                         goLiveOpMessage.innerHTML = "Bulk Go Live failed";
                     }
                 }

                 YConnect.asyncRequest("POST", CStudioAuthoring.Service.createServiceUri(serviceUri), cb);
                 goLiveOpMessage.innerHTML = "Executing bulk Go Live ...";
             }
         };

        var mainEl = document.getElementById("bulk-ops");

        mainEl.innerHTML =
                "<div id='bulk-golive' class='bulk-op-area'>" +
                    "<p><h2>Bulk Go Live</h2></p><p>" +
                    "<div class='bulk-table'>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>Path to Publish:" +
                            "</div>" +
                            "<div class='bulk-table-cell'><input type='text' size=70 id='bulk-golive-path'/>" +
                            "</div>" +
                        "</div>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>" +
                            "</div>" +
                            "<div class='bulk-table-cell'>(e.g. /site/website/about/index.xml)" +
                            "</div>" +
                        "</div>" +
                        "<div class='bulk-table-row'>" +
                            "<div class='bulk-table-cell'>Publishing Environment:" +
                            "</div>" +
                            "<div class='bulk-table-cell'><select id='go-pub-channel'></select>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                    "</br>" +
                    "<input type='button' class='action-button' value='Go Live' onclick='CStudioAdminConsole.Tool.BulkOperations.golive()' /></p>" +
                    "<p id='bulk-golive-message'></p>" +
                "</div>";


        var channelsSelect = document.getElementById("go-pub-channel");
        var publishingOptionsCB = {
            success:function(o) {
                var resultChannels = o.responseText;
                var channels = eval('(' + resultChannels + ')');
                var publishingOptions = "";
                var channel_index = 0;
                for (idx in channels.availablePublishChannels) {
                    publishingOptions += "<option value='" + channels.availablePublishChannels[idx].name +"'>" + channels.availablePublishChannels[idx].name + "</option>"
                }
                channelsSelect.innerHTML = publishingOptions;
            },
            failure: function() {
            }
        }

        var channelsServiceUrl = "/proxy/alfresco/cstudio/publish/get-available-publishing-channels?site=" + CStudioAuthoringContext.site;
        YConnect.asyncRequest("POST", CStudioAuthoring.Service.createServiceUri(channelsServiceUrl), publishingOptionsCB);
    },

    renderJobsList: function() {

		var actions = [
				{ name: "Rename", context: this, method: this.renderRename },
				{ name: "Go Live", context: this, method: this.renderGoLive }
		];
		CStudioAuthoring.ContextualNav.AdminConsoleNav.initActions(actions);

        this.renderRename();
    }
});

CStudioAuthoring.Module.moduleLoaded("cstudio-console-tools-bulkoperations",CStudioAdminConsole.Tool.BulkOperations);