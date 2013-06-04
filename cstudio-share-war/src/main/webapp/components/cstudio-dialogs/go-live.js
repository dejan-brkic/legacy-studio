
CStudioAuthoring.Dialogs = CStudioAuthoring.Dialogs || {};

/**
 * GoLive Constructor
 */
CStudioAuthoring.Dialogs.DialogGoLive = CStudioAuthoring.Dialogs.DialogGoLive || function() {
    CStudioAuthoring.Dialogs.DialogGoLive.superclass.constructor.call(this);
    this.moduleName = "goLive";
};

CStudioAuthoring.Module.requireModule("publish-dialog", "/components/cstudio-dialogs/publish-dialog.js", {}, {
    moduleLoaded: function(moduleName, parentClass) {

        var Y = YAHOO,
            YConnect = window.YConnect,
            CSA = CStudioAuthoring,
            CSAContext = CStudioAuthoringContext;

        // Make GoLive constructor inherit from its parent (i.e. PublishDialog)
        Y.lang.extend(CSA.Dialogs.DialogGoLive, parentClass);

        var DialogGoLive = CSA.Dialogs.DialogGoLive,
            DialogPrototype = DialogGoLive.prototype;

        // Extend GoLive's prototype with its own class functions
        DialogPrototype.createPanel = function (panelName, modalState, zIdx) {
            return new YAHOO.widget.Panel(panelName, {
                fixedcenter : true,
                visible : false,
                close : false,
                draggable : false,
                underlay : "none",
                modal : modalState,
                zIndex : zIdx,
                constraintoviewport : true,
                autofillheight: null
            });
        };

        DialogPrototype.setUpGoLiveListeners = function () {
            YEvent.addListener("globalSetToNow", "click", this.changeToNow, this, true);
        };

        DialogPrototype.invokeGoLiveService = function() {
            // remove unchecked items and dependencies from dependencyJsonObj
            this.selectedJsonObj = this.clone_obj(this.dependencyJsonObj);
            if (this.removeUncheckedItemsFromJson() == -1) { // no items selected
                return;
            }

            if (this.selectedJsonObj.items.length != 0 &&
                !this.checkParentChildSchedules(this.selectedJsonObj.items)) {
                return;
            }

            if (this.selectedJsonObj.items.length != 0) {

                this.appendPublishingChannelsData(this.selectedJsonObj);

                // add isNow and scheduledDate fields
                this.selectedJsonObj.now = "true";
                this.selectedJsonObj.scheduledDate = "";
                this.selectedJsonObj.submissionComment=document.getElementById("acn-submission-comment").value;

                var jsonSubmitString = YAHOO.lang.JSON.stringify(this.selectedJsonObj),
                    self = this,
                    serviceCallback = {
                        success:function(oResponse) {
                            var siteConfigCb = {
                                success: function(config) {
                                    if(config.onGoLive) {
                                        if(config.onGoLive && !config.onGoLive.assetGroups.length) {
                                            config.onGoLive.assetGroups = [config.onGoLive.assetGroups.assetGroup];
                                        }

                                        var createWorkflowRequest = {
                                            site: CSAContext.site,
                                            jobs: []

                                        };

                                        for(var l=0; l<config.onGoLive.assetGroups.length; l++) {
                                            var wfAssetGroupConfig = config.onGoLive.assetGroups[l];
                                            var itemsForGroup = [];
                                            for(var j=0; j<this.self.itemArray.length; j++) {
                                                var path = this.self.itemArray[j];
                                                if(true) {//compare path with wfAssetGroupConfig.include pattern
                                                    itemsForGroup[itemsForGroup.length] = path;
                                                }

                                                if(wfAssetGroupConfig.submitAsGroup == "true") {
                                                    createWorkflowRequest.jobs[createWorkflowRequest.jobs.length] = {
                                                        processName: wfAssetGroupConfig.process,
                                                        paths:itemsForGroup,
                                                        properties: [
                                                            {	"name":"submitter", "value": CSAContext.user},
                                                            {	"name":"publishingChannel", "value": self.selectedJsonObj.publishChannel},
                                                            {	"name":"scheduledDate", "value": self.selectedJsonObj.scheduledDate},
                                                            {	"name":"submissionComment", "value": self.selectedJsonObj.submissionComment}
                                                        ]
                                                    };
                                                }
                                                else {
                                                    // submit each item as it's own job
                                                    for(var k=0; k<itemsForGroup.length; k++) {
                                                        createWorkflowRequest.jobs[createWorkflowRequest.jobs.length] = {
                                                            processName: wfAssetGroupConfig.process,
                                                            paths:[itemsForGroup[k]],
                                                            properties: [
                                                                {	"name":"submitter", "value": CSAContext.user},
                                                                {	"name":"publishingChannel", "value": self.selectedJsonObj.publishChannel},
                                                                {	"name":"scheduledDate", "value": self.selectedJsonObj.scheduledDate},
                                                                {	"name":"submissionComment", "value": self.selectedJsonObj.submissionComment}
                                                            ]
                                                        };																																								}
                                                }

                                                var createJobCb = {
                                                    success: function(status) {
                                                        self.hideLoadingImage("approve");
                                                        // redirect to dashboard
                                                        self.dialog.setBody(oResponse.responseText);
                                                        self.setFocusOnDefaultButton();
                                                    },
                                                    failure: function() {
                                                    },
                                                    self: self
                                                };

                                                CSA.Service.createWorkflowJobs(createWorkflowRequest,createJobCb);

                                            }//for
                                        }
                                    }
                                    else {
                                        //hide loading image when submit is clicked.
                                        self.hideLoadingImage("approve");
                                        // redirect to dashboard
                                        self.dialog.setBody(oResponse.responseText);
                                        self.setFocusOnDefaultButton();
                                    }
                                },
                                failure: function() {
                                },
                                self: self

                            };

                            // check for on-go-live workflows (eventually moves server side)
                            CSA.Service.lookupConfigurtion(CSAContext.site,
                                "/workflow-config.xml", siteConfigCb);
                        },
                        failure: function (oResponse) {
                            self.pageRedirect(oResponse);
                            //hide loading image when submit is clicked.
                            self.hideLoadingImage("approve");
                            //re enable if service failed to submit againg
                            YDom.get("golivesubmitButton").disabled = false;
                            YDom.get("golivecancelButton").disabled = false;
                            if (oResponse.status == -1) {
                                alert('Go live is taking longer. The icon status will be updated once the content goes live.');
                                self.dialog.hide();
                                CSA.Operations.pageReload();
                            } else {
                                alert('go live call failed ' + oResponse.statusText);
                            }
                        },
                        timeout: CSA.Request.Timeout.GoLiveTimeout
                    };

                //show loading image when submit is clicked.
                this.showLoadingImage("approve");
                //disable submit button to protect multipale submit at the same time.
                YDom.get("golivesubmitButton").disabled = true;
                YDom.get("golivecancelButton").disabled = true;
                // submit to service
                if (YConnect._isFormSubmit) {
                    YConnect.resetFormState();
                }
                YConnect.setDefaultPostHeader(false);
                YConnect.initHeader("Content-Type", "application/json; charset=utf-8");
                // call go-live
                YConnect.asyncRequest(
                    'POST',
                    CSAContext.baseUri +
                        "/service/ui/workflow-actions/go-live?site=" +
                        CSAContext.site,
                    serviceCallback,
                    jsonSubmitString);
            } else {
                alert('no items selected');
            }
        };

        DialogPrototype.handleDependencies = function (matchedInputElement, isChecked) {
            this.updateUncheckedItemList(matchedInputElement, isChecked);

            var selectedElementURI = matchedInputElement.id,
                item = this.flatMap[selectedElementURI];

            if (isChecked) {
                if (item.submittedForDeletion) {
                    //check all child elements
                    this.checkAllChildren(matchedInputElement);
                } else {
                    //check all parents
                    var parentURI = item.mandatoryParent;
                    if (parentURI) {
                        var parentItem = this.flatMap[parentURI];
                        if (parentItem && parentItem.pages && parentItem.pages.length >= 1) {
                            var isReferencePage = this.checkReferencePages(parentItem.pages, item.browserUri);

                            if (isReferencePage) {
                                //no need to check the parent item
                                return;
                            }
                        }
                        var parentInputElement = YDom.get(parentURI);
                        parentInputElement.checked = true;
                        this.handleDependencies(parentInputElement, true);
                    }

                    //check all page references along with parent page.
                    if (item.pages && item.pages.length >= 1) {
                        for (var pagesIdx = 0; pagesIdx < item.pages.length; pagesIdx++) {
                            var pageInputElement = YDom.get(item.pages[pagesIdx].uri);
                            pageInputElement.checked = true;
                            this.updateUncheckedItemList(pageInputElement, true);
                        }
                    }
                }
            } else {
                if (item.submittedForDeletion) {
                    var parentURI = "/site/website" + item.parentPath + "/index.xml";
                    if (parentURI) {
                        var parentItem = this.flatMap[parentURI];
                        if (parentItem && parentItem.submittedForDeletion) {
                            var parentInputElement = YDom.get(parentURI);
                            if (item.submittedForDeletion && parentInputElement.checked) {
                                matchedInputElement.checked = true;
                            }
                        }
                    }
                } else {
                    var isParentSelectedForDelete = false;
                    //check if parent element is submitted for delete?
                    if (item.parentPath && item.parentPath != "") {
                        var parentURI = "/site/website" + item.parentPath + "/index.xml";
                        var parentItem = this.flatMap[parentURI];
                        if (parentItem && parentItem.submittedForDeletion) {
                            isParentSelectedForDelete = true;
                            var parentInputElement = YDom.get(parentURI);
                            if (parentInputElement.checked) {
                                matchedInputElement.checked = true;
                            }
                        }
                    }

                    if (!isParentSelectedForDelete) {
                        //deselect all children
                        //Check for page references in mandatoryParent.

                        var parentURI = item.mandatoryParent;
                        if (parentURI) {
                            var parentItem = this.flatMap[parentURI];
                            if (parentItem && parentItem.pages && parentItem.pages.length >= 1) {
                                var isReferencePage = this.checkReferencePages(parentItem.pages, item.browserUri);
                                var parentInputElement = YDom.get(parentURI);
                                if (isReferencePage && parentInputElement.checked) {
                                    matchedInputElement.checked = true;
                                    return;
                                }
                            }
                        }

                        var children = this.getChildren(item);
                        if (children.length) {
                            for (var i = 0; i < children.length; i++) {
                                var child = children[i];
                                var childInputElement = YDom.get(child.uri);
                                childInputElement.checked = false;
                                this.handleDependencies(childInputElement, false);
                            }
                        }
                    }
                }
            }
        };




        DialogPrototype.render = function (contentItems) {

            var me = this;

            if (me.itemArray.length) {

                // TODO xmlString might not be needed for the basic html getting request
                // TODO change dependencyUrl to be the just-html-getting request URL

                var xmlString = CSA.Utils.createContentItemsXml(contentItems),
                    dependencyUrl = CSA.StringUtils.format(
                        '{0}/service/ui/workflow-actions/go-live-dialog?site={1}&baseUrl={2}',
                        CSAContext.baseUri,
                        CSAContext.site,
                        CSAContext.baseUri);

                if (YConnect._isFormSubmit) {
                    YConnect.resetFormState();
                }

                YConnect.setDefaultPostHeader(false);
                YConnect.initHeader("Content-Type", "application/xml; charset=utf-8");
                YConnect.asyncRequest('POST', dependencyUrl, {
                    success: function(o) {

                        var respText = o.responseText,
                        // Will be used to test the existence of Publishing Channels
                            testRoot = document.createElement("div");

                        testRoot.innerHTML = respText;

                        if (testRoot.querySelector("#go-pub-channel").children.length) {

                            // There are publishing channels set
                            var dialog = me.createPanel('submitPanel', true, 10);

                            dialog.setBody(respText);
                            dialog.render(document.body);

                            //set z-index for panel so that it will appear over context nav bar also.
                            var oContainerPanel = YDom.get('submitPanel_c');
                            if (oContainerPanel && oContainerPanel.style.zIndex != "") {
                                var zIdx = oContainerPanel.style.zIndex;
                                if (!isNaN(zIdx) && parseInt(zIdx, 10) <= 100) {
                                    oContainerPanel.style.zIndex = "101";
                                }
                            }

                            // put up curtain on top of nav bar
                            YDom.get('curtain').style.display = 'block';
                            me.setUpGoLiveListeners();

                            //set height of items div
                            var oScrollBox = YDom.get('acnScrollBoxDiv');
                            oScrollBox.style.height = "280px";

                            //check for in-valid inline styles
                            if (dialog && dialog.body && dialog.body.style.height != "") {
                                dialog.body.style.height = "";
                            }

                            dialog.show();
                            me.dialog = dialog;

                            YDom.get('acnScrollBoxDiv').innerHTML = CSA.StringUtils.format(
                                '<div class="spinner"><img src="{0}/themes/cstudioTheme/images/wait.gif"> <span class="warn">{1}</span></div>',
                                CSAContext.authoringAppBaseUri, 'Loading items &amp; dependecies, please wait.');

                            YEvent.addListener("golivecancelButton", "click", me.closeDialog, me, true);

                            me.getDependenciesForGoLiveItemList(contentItems);

                        } else {

                            if (!parentClass.messagePanel) {
                                parentClass.messagePanel = new parentClass.createMessagePanel("messageOverlay", true, 1000);
                                parentClass.messagePanel.setBody(parentClass.getNoPublishingChannelsBody());

                                parentClass.messagePanel.hideEvent.subscribe( function() {
                                    // remove curtain on top of nav bar
                                    YDom.get('curtain').style.display = 'none';

                                    //clear the overlay mask if it remains after closing the dialog.
                                    var tempMask = YDom.getElementsByClassName('mask');
                                    for (var i = 0; i < tempMask.length; ++i) {
                                        tempMask[i].parentNode.removeChild(tempMask[i]);
                                    }
                                });
                                parentClass.messagePanel.render(document.body);
                            }

                            //set z-index for panel so that it will appear over context nav bar also.
                            var oContainerPanel = YDom.get('submitPanel_c');
                            if (oContainerPanel && oContainerPanel.style.zIndex != "") {
                                var zIdx = oContainerPanel.style.zIndex;
                                if (!isNaN(zIdx) && parseInt(zIdx, 10) <= 100) {
                                    oContainerPanel.style.zIndex = "101";
                                }
                            }
                            // put up curtain on top of nav bar
                            YDom.get('curtain').style.display = 'block';

                            parentClass.messagePanel.show();
                        }

                    },
                    failure: function (o) {
                        me.pageRedirect(o);
                        alert(o.statusText);
                    }
                }, xmlString);

            } else {
                alert('No items selected');
            }
        }

        DialogPrototype.getDependenciesForGoLiveItemList = function(contentItems) {

            var self = this,
                me = this,
                dialog = this.dialog;

            // TODO call here the table & json service
            var xmlString = CSA.Utils.createContentItemsXml(contentItems),
                dependencyUrl = CSA.StringUtils.format(
                    '{0}/service/ui/workflow-actions/go-live-dependencies?site={1}&baseUrl={2}',
                    CSAContext.baseUri,
                    CSAContext.site,
                    CSAContext.baseUri);

            if (YConnect._isFormSubmit) {
                YConnect.resetFormState();
            }

            YConnect.setDefaultPostHeader(false);
            YConnect.initHeader("Content-Type", "application/xml; charset=utf-8");
            YConnect.asyncRequest('POST', dependencyUrl, {
                success: function(o) {

                    var respText = o.responseText,
                        timeZoneText = o.getResponseHeader.Timezone,
                        scriptString = self.getJsonObject(respText),
                    // replace everything in between and including <script> tags;
                        ftlWithoutScriptTag = self.removeScriptContent(respText);


                    var container = YDom.get('acnScrollBoxDiv');
                    container.innerHTML = ftlWithoutScriptTag;

                    var submissionCommentsValue = YDom.get("acn-submission-comment-hidden-div").innerHTML;
                    YDom.get("acn-submission-comment").value = submissionCommentsValue;

                    YDom.get("golivesubmitButton").disabled = false;
                    YDom.get("golivecancelButton").disabled = false;

                    // There are publishing channels set
                    self.dependencyJsonObj = eval('(' + scriptString + ')');
                    self.flatMap = self.createItemMap();
                    self.uncheckedItemsArray = [];

                    var onCheckBoxSubmittedItemClick = function (event, matchedEl) {
                        // skipping email checkbox
                        if (matchedEl.id == "email") return;

                        me.handleDependencies(matchedEl, matchedEl.checked);
                        me.anyoneSelected = false;

                        for (var key in me.flatMap) {
                            if (me.flatMap.hasOwnProperty(key)) {
                                var inputElement = YDom.get(key);
                                me.anyoneSelected = me.anyoneSelected || inputElement.checked;
                            }
                        }

                        var submittButton = YDom.get("golivesubmitButton");
                        submittButton.disabled = !me.anyoneSelected;
                    };

                    // handle checkbox clicks
                    YEvent.delegate("acnVersionWrapper", "click", onCheckBoxSubmittedItemClick, ".acnLiveTableCheckbox > input", me, true);
                    YEvent.delegate("acnSubmitWrapper", "click", onCheckBoxSubmittedItemClick, ".acnLiveTableCheckbox > input", me, true);

                    me.publishingChannelsInit();

                    YEvent.addListener("golivesubmitButton", "click", me.invokeGoLiveService, me, true);

                    YEvent.removeListener("now", "click", me.toggleTimeSelection);
                    YEvent.addListener("now", "click", me.toggleTimeSelection);
                    YEvent.removeListener("settime", "click", me.toggleTimeSelection);
                    YEvent.addListener("settime", "click", me.toggleTimeSelection);

                    var submittButton = YDom.get("golivesubmitButton");
                    if (submittButton) {
                        //set tab focus items.
                        var oCancelButton = YDom.get("golivecancelButton");
                        var oGlobalSetToNow = YDom.get("globalSetToNow");

                        dialog.firstElement = oGlobalSetToNow;
                        dialog.lastElement = oCancelButton;

                        CSA.Utils.setDefaultFocusOn(submittButton);
                    }

                    // init yui datepicker
                    var afterRenderFn = function(sourceElementId){
                        if (afterRenderFn.firecount == 0) {
                            afterRenderFn.firecount++;
                            var today = new Date();
                            today.setDate(today.getDate() + 1);
                            YDom.get(sourceElementId).value = [
                                (today.getMonth()+1),
                                today.getDate(),
                                today.getFullYear()
                            ].join("/");
                        }
                    };

                    afterRenderFn.firecount = 0;

                    var initCalendar = CSA.Utils.yuiCalendar('datepicker', 'focus', 'datepicker', afterRenderFn),
                        status = CSA.Utils.initCursorPosition('timepicker', ['click', 'keydown', 'keyup', 'keypress', 'mouseup', 'mousedown']),
                        initTimeFormat = CSA.Utils.textFieldTimeHelper('timepicker', 'blur', 'timepicker'),
                        initTimeIncrementButton = CSA.Utils.textFieldTimeIncrementHelper('timeIncrementButton', 'timepicker', 'click'),
                        initTimeDecrementButton = CSA.Utils.textFieldTimeDecrementHelper('timeDecrementButton', 'timepicker', 'click');

                    // Updating time zone name dynamically
                    if (timeZoneText) {
                        timeZoneText = timeZoneText.replace(/^\s+|\s+$/, '');

                        var oTimeZoneSpan = YDom.get("timeZone");
                        if (oTimeZoneSpan) {
                            oTimeZoneSpan.innerHTML = timeZoneText;
                        }
                    }

                    if (YUISelector.query("#acnScrollBoxDiv .acnLiveCellIndented").length) {
                        // Only add notice if there are dependencies present
                        var el = YUISelector.query('#acnVersionWrapper .acnBoxFloatLeft')[0],
                            msg = document.createElement('span');

                        msg.className = "notice";
                        msg.textContent = "*Dependencies must be checked before you can Schedule to Go Live.";

                        el.appendChild(msg);
                    }

                    // hide dependency line if only 1 item
                    if (me.dependencyJsonObj.items.length == 1) { // only 1 item in the json obj
                        if (me.dependencyJsonObj.items[0].numOfChildren == 0) { // and no children
                            var dependencyText = YDom.get('depText');
                            if (dependencyText) {
                                YDom.setStyle(dependencyText, "display", "none");
                            }
                        }
                    }

                },
                failure: function (o) {
                    self.pageRedirect(o);
                    alert(o.statusText);
                }
            }, xmlString);

        };




        DialogPrototype.closeDialog = function () {
            // remove curtain on top of nav bar
            YDom.get('curtain').style.display = 'none';

            this.dialog.destroy();

            //clear the overlay mask if it remains after closing the dialog.
            var tempMask = YDom.getElementsByClassName('mask');
            for (var i = 0; i < tempMask.length; ++i) {
                tempMask[i].parentNode.removeChild(tempMask[i]);
            }
        };

        DialogPrototype.showDialog = function(site, contentItems) {

            var selectedContent = CSA.SelectedContent.getSelectedContent();
            this.init();

            for(var i=0; i < selectedContent.length; i++) {
                this.itemArray.push(selectedContent[i].uri);
            }

            this.render(contentItems);

        };

        DialogPrototype.openSchedulePopup = function (element, e, browserUri) {
            YDom.get('goLivePopWrapper').style.display = 'block';
            this.elementThatClickedMiniScheduler = e;
            this.browserUriOfItemClickedInMiniScheduler = browserUri;
            YDom.get('golivesubmitButton').disabled = true;
            var scheduledDate = this.getTimeInJsonObject(this.dependencyJsonObj.items, browserUri);
            this.setTimeConfiguration(scheduledDate);
        };

        DialogPrototype.onDone = function () {
            YDom.get('golivesubmitButton').disabled = !this.anyoneSelected;

            var now = false;
            if (YDom.get('settime').checked == true) {

                now = false;
                var dateValue = YDom.get('datepicker').value,
                    timeValue = YDom.get('timepicker').value,
                    scheduledDate = this.getScheduledDateTimeForJson(dateValue, timeValue);

            } else {  // it is Now
                now = true;
            }

            var dateValue = YDom.get('datepicker').value;
            var timeValue = YDom.get('timepicker').value;
            if (((dateValue == 'Date...') || (timeValue == 'Time...') || (timeValue == '')) && YDom.get('settime').checked == true) {
                alert('Please provide a date and/or time');
                return;
            }

            YDom.get('goLivePopWrapper').style.display = 'none';

            // update main pop-up
            if (now == false) {
                this.elementThatClickedMiniScheduler.innerHTML = CSA.Utils.getScheduledDateTimeUI(dateValue, timeValue);
                var jsonScheduledTime = this.getScheduledDateTimeForJson(dateValue, timeValue);
                this.setTimeInJsonObject(this.dependencyJsonObj.items, jsonScheduledTime, this.browserUriOfItemClickedInMiniScheduler);
            } else {
                this.elementThatClickedMiniScheduler.innerHTML = 'Now';
                this.setTimeInJsonObject(this.dependencyJsonObj.items, 'now', this.browserUriOfItemClickedInMiniScheduler);
            }
        };

        // Create GoLive dialog instance
        var goLive = new DialogGoLive();

        // Create a global pointer to the current dialog instance
        DialogGoLive.instance = goLive;

        // dialog instance will be reused with every call to 'dialog-approve'
        CSA.Module.moduleLoaded("dialog-approve", goLive);

    }
});