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
package org.craftercms.cstudio.impl.service.site.dal;

import org.dom4j.Document;

import org.craftercms.cstudio.api.repository.*;
import org.craftercms.cstudio.api.service.site.SiteConfigNotFoundException;
import org.craftercms.cstudio.impl.service.site.AbstractSiteServiceDAL;

/**
 * use content repository as data access layer for site configuration
 * @author russdanner
 */
public class RepositorySiteServiceDAL extends AbstractSiteServiceDAL {
	
	/**
	 * given a site ID return the configuration as a document
	 * This method allows extensions to add additional properties to the configuration that
	 * are not made available through the site configuration object
	 * @param site the name of the site
	 * @return a Document containing the entire site configuration
	 */
	public Document getSiteConfiguration(String site)
	throws SiteConfigNotFoundException {
		Document retConfigDoc = null;
		
		try {
			retConfigDoc = _contentRepository.getContentAsDocument("/cstudio/config/sites/"+site+"/site-config.xml");
		}
		catch(Exception err) {
			throw new SiteConfigNotFoundException();
		}
		
		return retConfigDoc;
	}

	/** getter for content repository */
	public ContentRepository getContentRepository() { return _contentRepository; }
	/** setter for content repository */
	public void setContentRepository(ContentRepository repo) { _contentRepository = repo; }
	
	protected ContentRepository _contentRepository;
}
