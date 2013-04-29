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
package org.craftercms.cstudio.alfresco.util;


import java.util.Date;
import java.util.TimeZone;

/**
 */
public class TimeZoneUtils {


    public static String getTimeZoneDisplay(String timeZone){
        TimeZone timeZone1 = TimeZone.getTimeZone(timeZone);
        boolean isDayLightRunning = timeZone1.inDaylightTime(new Date());
        return timeZone1.getDisplayName(isDayLightRunning, TimeZone.SHORT);
    }
}
