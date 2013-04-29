/**
 * Logout Plugin
 */
CStudioAuthoring.ContextualNav.WcmLogoutMod = CStudioAuthoring.ContextualNav.WcmLogoutMod || {

	initialized: false,
	
	/**
	 * initialize module
	 */
	initialize: function(config) {
		YDom.get("acn-logout-link").href = CStudioAuthoringContext.authoringAppBaseUri + "/page/dologout";   
	}
}

CStudioAuthoring.Module.moduleLoaded("logout", CStudioAuthoring.ContextualNav.WcmLogoutMod);
