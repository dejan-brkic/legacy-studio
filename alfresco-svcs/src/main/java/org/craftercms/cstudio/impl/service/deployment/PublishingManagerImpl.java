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
package org.craftercms.cstudio.impl.service.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.craftercms.cstudio.alfresco.deployment.DeploymentEventItem;
import org.craftercms.cstudio.api.log.Logger;
import org.craftercms.cstudio.api.log.LoggerFactory;
import org.craftercms.cstudio.api.repository.ContentRepository;
import org.craftercms.cstudio.api.service.deployment.CopyToEnvironmentItem;
import org.craftercms.cstudio.api.service.deployment.PublishingSyncItem;
import org.craftercms.cstudio.api.service.deployment.PublishingTargetItem;
import org.craftercms.cstudio.api.service.fsm.TransitionEvent;
import org.craftercms.cstudio.impl.service.deployment.dal.DeploymentDAL;

public class PublishingManagerImpl implements PublishingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishingManagerImpl.class);

    private static final String TARGET_REQUEST_PARAMETER = "target";
    private static final String VERSION_REQUEST_PARAMETER = "version";
    private static final String PASSWORD_REQUEST_PARAMETER = "password";
    private final static String SITE_REQUEST_PARAMETER = "siteId";
    private final static String DELETED_FILES_REQUEST_PARAMETER = "deletedFiles";
    private final static String CONTENT_LOCATION_REQUEST_PARAMETER = "contentLocation";
    private final static String CONTENT_FILE_REQUEST_PARAMETER = "contentFile";
    private final static String METADATA_FILE_REQUEST_PARAMETER = "metadataFile";

    private final static String INDEX_FILE = "index.xml";
    private final static String FILES_SEPARATOR = ",";
    private final static String LIVE_ENVIRONMENT = "live";
    private final static String WORK_AREA_ENVIRONMENT = "work-area";

    @Override
    public Set<String> getAllAvailableSites() {
        return _contentRepository.getAllAvailableSites();
    }

    @Override
    public Set<PublishingTargetItem> getAllTargetsForSite(String site) {
        return _contentRepository.getAllTargetsForSite(site);
    }

    @Override
    public boolean checkConnection(PublishingTargetItem target) {
        boolean connOk = false;
        if (target.getStatusUrl() != null && !target.getStatusUrl().isEmpty()) {
            LOGGER.debug(String.format("Check deployment agent status for target ", target.getName()));
            URL statusUrl = null;
            try {
                statusUrl = new URL(target.getStatusUrl());
            } catch (MalformedURLException e) {
                LOGGER.error(String.format("Invalid endpoint status URL for publishing channel [%s]", target.getName()), e);
            }
            GetMethod getMethod = null;
            HttpClient client = null;
            try {
                getMethod = new GetMethod(target.getStatusUrl());
                client = new HttpClient();
                int status = client.executeMethod(getMethod);
                if (status == HttpStatus.SC_OK) {
                    connOk = true;
                }

            } catch (Exception e) {
                LOGGER.error(String.format("Target (%s) is not available. Status check failed for url %s", target.getName(), target.getStatusUrl()));
            } finally {
                if (client != null) {
                    HttpConnectionManager mgr = client.getHttpConnectionManager();
                    if (mgr instanceof SimpleHttpConnectionManager) {
                        ((SimpleHttpConnectionManager)mgr).shutdown();
                    }
                }
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
                getMethod = null;
                client = null;

            }
        }
        return connOk;
    }

    @Override
	public long getTargetVersion(PublishingTargetItem target, String site) {
        long version = -1;
        if (target.getVersionUrl() != null && !target.getVersionUrl().isEmpty()) {
            LOGGER.debug(String.format("Get deployment agent version for target ", target.getName()));
			URL versionUrl = null;
            try {
                versionUrl = new URL(target.getVersionUrl());
            } catch (MalformedURLException e) {
                LOGGER.error(String.format("Invalid get version URL for target [%s]", target.getName()), e);
            }
            GetMethod getMethod = null;
            HttpClient client = null;
            try {
                getMethod = new GetMethod(target.getVersionUrl());
                getMethod.setQueryString(new NameValuePair[] {new NameValuePair(TARGET_REQUEST_PARAMETER, target.getTarget()),
						new NameValuePair(SITE_REQUEST_PARAMETER, site) });
                client = new HttpClient();
                int status = client.executeMethod(getMethod);
                if (status == HttpStatus.SC_OK) {
                    String responseText = getMethod.getResponseBodyAsString();
                    if (responseText != null && !responseText.isEmpty()) {
                        version = Long.parseLong(responseText.trim());
                    } else {
                        version = 0;
                    }
                }

            } catch (Exception e) {
                //LOGGER.error(String.format("Target (%s) responded with error while checking target version. Get version failed for url %s", target.getName(), target.getVersionUrl()));

            } finally {
                if (client != null) {
                    HttpConnectionManager mgr = client.getHttpConnectionManager();
                    if (mgr instanceof SimpleHttpConnectionManager) {
                        ((SimpleHttpConnectionManager)mgr).shutdown();
                    }
                }
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
                getMethod = null;
                client = null;

            }
        }
        return version;
    }

    @Override
    public List<PublishingSyncItem> getItemsToSync(String site, long targetVersion) {
        return _deploymentDAL.getItemsReadyForTargetSync(site, targetVersion);
    }

    @Override
    public void deployItemsToTarget(String site, List<PublishingSyncItem> filteredItems, PublishingTargetItem target) {
        URL requestUrl = null;
        try {
            requestUrl = new URL(target.getServerUrl());
        } catch (MalformedURLException e) {
            LOGGER.error(String.format("Invalid server URL for target [%s]", target.getName()), e);
        }

        ByteArrayPartSource baps = null;
        PartSource metadataPart = null;
        StringPart stringPart = null;
        FilePart filePart = null;

        int numberOfBuckets = filteredItems.size() / target.getBucketSize() + 1;
        Iterator<PublishingSyncItem> iter = filteredItems.iterator();
        LOGGER.debug(String.format("Divide all deployment items into %d bucket(s) for  target [%s]", numberOfBuckets , target.getName()));
        List<DeploymentEventItem> eventItems = new ArrayList<DeploymentEventItem>();
        for (int bucketIndex = 0; bucketIndex < numberOfBuckets; bucketIndex++) {
            int cntFiles = 0;
            StringBuilder sbDeletedFiles = new StringBuilder();
            List<Part> formParts = new ArrayList<Part>();

            formParts.add(new StringPart(PASSWORD_REQUEST_PARAMETER, target.getPassword()));
            formParts.add(new StringPart(TARGET_REQUEST_PARAMETER, target.getTarget()));
            formParts.add(new StringPart(SITE_REQUEST_PARAMETER, site));

            LOGGER.debug(String.format("Preparing deployment items (bucket %d) for target [%s]", bucketIndex + 1, target.getName()));

            int loopSize = (filteredItems.size() - (bucketIndex * target.getBucketSize()) > target.getBucketSize()) ? target.getBucketSize() : filteredItems.size() - bucketIndex * target.getBucketSize();
            for (int j = 0; j < loopSize; j++) {
                if (iter.hasNext()) {
                    PublishingSyncItem item = iter.next();
                    DeploymentEventItem eventItem = new DeploymentEventItem();
                    eventItem.setSite(item.getSite());
                    eventItem.setPath(item.getPath());
                    eventItem.setUser(item.getUser());
                    eventItem.setDateTime(new Date());

                    if (item.getAction() == PublishingSyncItem.Action.DELETE) {
                        eventItem.setState(DeploymentEventItem.STATE_DELETED);
                        if (sbDeletedFiles.length() > 0) {
                            sbDeletedFiles.append(FILES_SEPARATOR).append(item.getPath());
                        } else {
                            sbDeletedFiles.append(item.getPath());
                        }
                        if (item.getPath().endsWith("/" + INDEX_FILE)) {
                            sbDeletedFiles.append(FILES_SEPARATOR).append(item.getPath().replace("/" + INDEX_FILE, ""));
                        }
                    } else {

                        if (item.getAction() == PublishingSyncItem.Action.NEW) {
                            eventItem.setState(DeploymentEventItem.STATE_NEW);
                        } else if (item.getAction() == PublishingSyncItem.Action.MOVE) {
                            eventItem.setState(DeploymentEventItem.STATE_MOVED);
                        } else {
                            eventItem.setState(DeploymentEventItem.STATE_UPDATED);
                        }

                        InputStream input = _contentRepository.getContent(site, null, LIVE_ENVIRONMENT, item.getPath());
                        try {
                            if (input == null || input.available() > 0) {
                            // Content does not exist - skip deploying file
                            continue;
                            }
                        } catch (IOException err) {
                            LOGGER.error("Error reading input stream for content at path: " + item.getPath() + " site: " + item.getSite());
                            continue;
                        }
                        String fileName = _contentRepository.getFilename(site, item.getPath());

                        byte[] byteArray = null;

                        try {
                            byteArray = IOUtils.toByteArray(input);
                        } catch (IOException e) {
                            LOGGER.error("Error while converting input stream to byte array", e);
                            baps = null;
                            stringPart = null;
                            filePart = null;
                            formParts = null;
                            continue;
                        }
                        finally {
                            IOUtils.closeQuietly(input);
                            input = null;
                        }
                        baps = new ByteArrayPartSource(fileName, byteArray);

                        int idx = item.getPath().lastIndexOf("/");
                        String relativePath = item.getPath().substring(0, idx + 1) + fileName;
                        stringPart = new StringPart(CONTENT_LOCATION_REQUEST_PARAMETER + cntFiles, relativePath);
                        formParts.add(stringPart);
                        filePart = new FilePart(CONTENT_FILE_REQUEST_PARAMETER + cntFiles, baps);
                        formParts.add(filePart);
                        if (item.getAction() == PublishingSyncItem.Action.MOVE) {
                            if (item.getOldPath() != null && !item.getOldPath().equalsIgnoreCase(item.getPath())) {
                                eventItem.setOldPath(item.getOldPath());
                                if (sbDeletedFiles.length() > 0) {
                                    sbDeletedFiles.append(",").append(item.getOldPath());
                                } else {
                                    sbDeletedFiles.append(item.getOldPath());
                                }
                                if (item.getOldPath().endsWith("/" + INDEX_FILE)) {
                                    sbDeletedFiles.append(FILES_SEPARATOR).append(item.getOldPath().replace("/" + INDEX_FILE, ""));
                                }
                            }
                        }

                        if (target.isSendMetadata()) {
                            InputStream metadataStream = null;
                            try {
                                metadataStream = _contentRepository.getMetadataStream(site, item.getPath());
                                metadataPart = new ByteArrayPartSource(fileName + ".meta", IOUtils.toByteArray(metadataStream));
                                formParts.add(new FilePart(METADATA_FILE_REQUEST_PARAMETER + cntFiles, metadataPart));
                            } catch (IOException e) {
                                LOGGER.error("Error while creating input stream with content metadata", e);
                                baps = null;
                                stringPart = null;
                                filePart = null;
                                formParts = null;
                            }
                            finally {
                                IOUtils.closeQuietly(metadataStream);
                                metadataPart = null;
                            }
                        }
                    }
                    cntFiles++;
                    eventItems.add(eventItem);
                }
            }


            if (sbDeletedFiles.length() > 0) {
                formParts.add(new StringPart(DELETED_FILES_REQUEST_PARAMETER, sbDeletedFiles.toString()));
            }
            LOGGER.debug(String.format("Create http request to deploy bucket %d for target [%s]", bucketIndex + 1, target.getName()));

            PostMethod postMethod = null;
            HttpClient client = null;
            try {

                postMethod = new PostMethod(requestUrl.toString());
                postMethod.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
                Part[] parts = new Part[formParts.size()];
                for (int i = 0; i < formParts.size(); i++) parts[i] = formParts.get(i);
                postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams()));
                client = new HttpClient();
                int status = client.executeMethod(postMethod);
                if (status == HttpStatus.SC_OK) {
                    LOGGER.info(String.format("Successfully deployed bucket number %d on target [%s]", bucketIndex + 1, target.getName()));
                } else {
                    LOGGER.error(String.format("Deployment failed for bucket number %d on target [%s]. Deployment agent returned status [%s]", bucketIndex + 1, target.getName(), HttpStatus.getStatusText(status)));
                }
            } catch (HttpException e) {
                String message = String.format("Publish failed for target [%s] due to http protocol exception", target.getName());
                LOGGER.error(message, e);
            } catch (IOException e) {
                String message = String.format("Publish failed for target [%s] due to I/O (transport) exception", target.getName());
                LOGGER.error(message, e);
            } finally {
                if (client != null) {
                    HttpConnectionManager mgr = client.getHttpConnectionManager();
                    if (mgr instanceof SimpleHttpConnectionManager) {
                        ((SimpleHttpConnectionManager)mgr).shutdown();
                    }
                }
                if (postMethod != null) {
                    postMethod.releaseConnection();
                    postMethod = null;
                    client = null;
                }
                baps = null;
                stringPart = null;
                filePart = null;
                formParts = null;
            }
        }
        _contentRepository.publishDeployEvent(target.getName(), eventItems);
        LOGGER.info(String.format("Deployment successful on target [%s]", target.getName()));
    }

    @Override
	public long setTargetVersion(PublishingTargetItem target, long newVersion, String site) {
        long resoponseVersion = -1;
        if (target.getVersionUrl() != null && !target.getVersionUrl().isEmpty()) {
            LOGGER.debug(String.format("Set deployment agent version for target ", target.getName()));
            URL versionUrl = null;
            try {
                versionUrl = new URL(target.getVersionUrl());
            } catch (MalformedURLException e) {
                LOGGER.error(String.format("Invalid set version URL for target [%s]", target.getName()), e);
            }
            PostMethod postMethod = null;
            HttpClient client = null;
            try {
                postMethod = new PostMethod(target.getVersionUrl());
                postMethod.addParameter(TARGET_REQUEST_PARAMETER, target.getTarget());
                postMethod.addParameter(VERSION_REQUEST_PARAMETER, String.valueOf(newVersion));
				postMethod.addParameter(SITE_REQUEST_PARAMETER, site);
                client = new HttpClient();
                int status = client.executeMethod(postMethod);
                if (status == HttpStatus.SC_OK) {
                    String responseText = postMethod.getResponseBodyAsString();
                    if (responseText != null && !responseText.isEmpty()) {
                        resoponseVersion = Long.parseLong(responseText);
                    } else {
                        resoponseVersion = 0;
                    }
                }

            } catch (Exception e) {
                LOGGER.error(String.format("Target (%s) responded with error while setting target version. Set version failed for url %s", target.getName(), target.getVersionUrl()));

            } finally {
                if (client != null) {
                    HttpConnectionManager mgr = client.getHttpConnectionManager();
                    if (mgr instanceof SimpleHttpConnectionManager) {
                        ((SimpleHttpConnectionManager)mgr).shutdown();
                    }
                }
                if (postMethod != null) {
                    postMethod.releaseConnection();
                }
                postMethod = null;
                client = null;

            }
        }
        return resoponseVersion;
    }

    @Override
    public List<CopyToEnvironmentItem> getItemsReadyForDeployment(String site, String environment) {
        return _deploymentDAL.getItemsReadyForDeployment(site, environment);
    }

    @Override
    public void processItem(CopyToEnvironmentItem item) {
        if (item.getAction() == CopyToEnvironmentItem.Action.DELETE) {
            if (item.getOldPath() != null && item.getOldPath().length() > 0) {
                _contentRepository.deleteContent(item.getSite(), item.getEnvironment(), item.getOldPath());
                _contentRepository.clearRenamed(item.getSite(), item.getPath());
            }
            _contentRepository.deleteContent(item.getSite(), item.getEnvironment(), item.getPath());
            _contentRepository.deleteContent(item.getSite(), item.getPath());
        } else {
            _contentRepository.setSystemProcessing(item.getSite(), item.getPath(), true);
            if (LIVE_ENVIRONMENT.equalsIgnoreCase(item.getEnvironment())) {
                _contentRepository.createNewVersion(item.getSite(), item.getPath(), true);
                _contentRepository.stateTransition(item.getSite(), item.getPath(), TransitionEvent.DEPLOYMENT);
            }
            if (item.getAction() == CopyToEnvironmentItem.Action.MOVE) {
                if (item.getOldPath() != null && item.getOldPath().length() > 0) {
                    _contentRepository.deleteContent(item.getSite(), item.getEnvironment(), item.getOldPath());
                    _contentRepository.clearRenamed(item.getSite(), item.getPath());
                }
            }
            _contentRepository.copyToEnvironment(item.getSite(), item.getEnvironment(), item.getPath());
            _contentRepository.setSystemProcessing(item.getSite(), item.getPath(), false);
        }
    }

    @Override
    public void setupItemsForPublishingSync(String site, String environment, List<CopyToEnvironmentItem> itemsToDeploy) {
        _deploymentDAL.setupItemsForPublishingSync(site, environment, itemsToDeploy);
    }

    @Override
    public void insertDeploymentHistory(PublishingTargetItem target, List<PublishingSyncItem> publishedItems, Date publishingDate) {
        _deploymentDAL.insertDeploymentHistory(target, publishedItems, publishingDate);
    }

    public ContentRepository getContentRepository() { return _contentRepository; }
    public void setContentRepository(ContentRepository contentRepository) { this._contentRepository = contentRepository; }

    public DeploymentDAL getDeploymentDal() { return _deploymentDAL; }
    public void setDeploymentDAL(DeploymentDAL deploymentDAL) { this._deploymentDAL = deploymentDAL; }

    protected ContentRepository _contentRepository;
    protected DeploymentDAL _deploymentDAL;
}
