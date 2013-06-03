/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.cstudio.impl.service.translation.workflow.handler;

import java.util.Map;

import org.craftercms.cstudio.api.service.workflow.*;
import org.craftercms.cstudio.api.service.translation.*;
import org.craftercms.cstudio.impl.service.workflow.*;

/**
 * Job is started. Send content to translation service  and change state to in progress.
 * @author rdanner
 */
public class UpdateItemTranslationStatusHandler implements JobStateHandler {

	@Override
	public String handleState(WorkflowJob job, WorkflowService workflowService) {
		String retState = "WAITING-FOR-TRANSLATION";
		String path = job.getItems().get(0).getPath();
		Map<String, String> prop = job.getProperties();
		String sourceSite = prop.get("sourceSite");
		String targetLanguage = prop.get("targetLanguage");
		try {
			int percentComplete = _translationService.getTranslationStatusForItem(sourceSite, targetLanguage, path);
	
			if (percentComplete == 100)
				retState = "TRANSLATION-COMPLETE";
		}
		catch (ProviderException ex) {
			if (ex.isFatal())
				retState =  WorkflowService.STATE_ENDED;
		}
		return retState;
	}

	/** getter translationService */
	public TranslationService getTranslationService() { return _translationService; }
	/** setter for translation service */
	public void setTranslationService(TranslationService service) { _translationService = service; }
	
	protected TranslationService _translationService;

}
