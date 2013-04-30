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

import java.util.List;

public class DeploymentEndpointConfigTO {

    protected String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    protected String type;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    protected String serverUrl;
    public String getServerUrl() {
        return serverUrl;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    protected String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    protected String target;
    public String getTarget() {
        return target;
    }
    public void setTarget(String target) {
        this.target = target;
    }

    protected boolean sendMetadata;
    public boolean isSendMetadata() {
        return sendMetadata;
    }
    public void setSendMetadata(boolean sendMetadata) {
        this.sendMetadata = sendMetadata;
    }

    protected List<String> excludePattern;
    public List<String> getExcludePattern() {
        return excludePattern;
    }
    public void setExcludePattern(List<String> excludePattern) {
        this.excludePattern = excludePattern;
    }

    protected List<String> includePattern;
    public List<String> getIncludePattern() {
        return includePattern;
    }
    public void setIncludePattern(List<String> includePattern) {
        this.includePattern = includePattern;
    }

    protected int bucketSize = 10;
    public int getBucketSize() {
        return bucketSize;
    }
    public void setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    protected String statusUrl;
    public String getStatusUrl() {
        return statusUrl;
    }
    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    protected String versionUrl;
    public String getVersionUrl() {
        return versionUrl;
    }
    public void setVersionUrl(String versionUrl) {
        this.versionUrl = versionUrl;
    }
}
