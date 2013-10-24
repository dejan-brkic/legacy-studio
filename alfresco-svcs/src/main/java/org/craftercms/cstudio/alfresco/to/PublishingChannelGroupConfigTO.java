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
package org.craftercms.cstudio.alfresco.to;

import javolution.util.FastList;

import java.util.List;

public class PublishingChannelGroupConfigTO {

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private boolean liveEnvironment = false;
    public boolean isLiveEnvironment() {
        return liveEnvironment;
    }
    public void setLiveEnvironment(boolean liveEnvironment) {
        this.liveEnvironment = liveEnvironment;
    }

    private List<PublishingChannelConfigTO> channels = new FastList<PublishingChannelConfigTO>();
    public List<PublishingChannelConfigTO> getChannels() {
        return channels;
    }
    public void setChannels(List<PublishingChannelConfigTO> channels) {
        this.channels = channels;
    }
}
