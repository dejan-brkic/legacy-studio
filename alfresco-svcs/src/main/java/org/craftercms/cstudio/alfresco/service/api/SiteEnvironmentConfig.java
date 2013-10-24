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
package org.craftercms.cstudio.alfresco.service.api;

import java.util.List;
import java.util.Map;

import org.craftercms.cstudio.alfresco.to.EnvironmentConfigTO;
import org.craftercms.cstudio.alfresco.to.PublishingChannelConfigTO;
import org.craftercms.cstudio.alfresco.to.PublishingChannelGroupConfigTO;

public interface SiteEnvironmentConfig {

	/**
	 * get the common preview server URL for all content in the given site
	 * 
	 * @param site
	 * @return preview server url
	 */
	public String getPreviewServerUrl(String site);
	
	/**
	 * get the live site url 
	 * 
	 * @param site
	 * @return
	 */
	public String getLiveServerUrl(String site);
	
	/**
	 * get admin email address
	 * 
	 * @param site
	 * @return
	 */
	public String getAdminEmailAddress(String site);
	
	/**
	 * get the authoring server URL 
	 * 
	 * @param site
	 * @return authoring server URL
	 */
	public String getAuthoringServerUrl(String site);
	
	/**
	 * get the form server URL 
	 * 
	 * @param site
	 * @return form server URL
	 */
	public String getFormServerUrl(String site);

    /**
     * get the cookie domain of the given site
     * 
     * @param site
     * @return
     */
	public String getCookieDomain(String site);

	/**
	 * get the environment config for the given site
	 * 
	 * @param site
	 * @return
	 */
	public EnvironmentConfigTO getEnvironmentConfig(String site);
	
	/**
	 * check if the site environment configuration is updated
	 * 
	 * @param site
	 * @return
	 */
	public boolean isUpdated(String site);

    /**
     * get Publishing Channels configuration
     *
     * @param site
     * @return
     */
    public Map<String, PublishingChannelGroupConfigTO> getPublishingChannelGroupConfigs(String site);
    
    public boolean exists(String site);

    PublishingChannelGroupConfigTO getLiveEnvironmentPublishingGroup(String site);
}
