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
package org.craftercms.cstudio.alfresco.dm.util.impl;

import org.apache.commons.lang.StringUtils;
import org.craftercms.cstudio.alfresco.dm.constant.DmConstants;
import org.craftercms.cstudio.alfresco.service.exception.ServiceException;

import java.util.Map;

public class PathMacrosTransaltor {

    public static final String PAGEID = "{pageId}";

    public static final String PAGE_GROUPID = "{pageGroupId}";


    /**
     * Match the URL with the know patterns and translate them to actual value
     *
     * @param path
     */
    public static String resolvePath(String path,Map<String,String> properties) throws ServiceException {

        String pageId = properties.get(DmConstants.KEY_PAGE_ID);

        String groupId = properties.get(DmConstants.KEY_PAGE_GROUP_ID);

        if(StringUtils.isNotEmpty(pageId) && (path.contains(PAGEID))){
            path = path.replace(PAGEID,  pageId);
        }

        if(StringUtils.isNotEmpty(groupId) && (path.contains(PAGE_GROUPID))){
            path = path.replace(PAGE_GROUPID, groupId);
        }

        return path;
    }
}
