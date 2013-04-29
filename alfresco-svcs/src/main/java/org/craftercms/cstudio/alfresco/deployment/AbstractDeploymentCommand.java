/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.cstudio.alfresco.deployment;

import org.craftercms.cstudio.alfresco.service.ServicesManager;

public abstract class AbstractDeploymentCommand {

    protected ServicesManager servicesManager;
    public void setServicesManager(ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    protected String batchId;
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    protected DeploymentEndpointConfigTO endpoint;
    public void setEndpoint(DeploymentEndpointConfigTO endpoint) {
        this.endpoint = endpoint;
    }

    public abstract void deploy() throws DeploymentException;
}
