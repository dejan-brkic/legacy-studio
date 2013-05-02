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
package org.craftercms.cstudio.alfresco.dm.service.impl;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.alfresco.model.ContentModel;
import org.alfresco.model.WCMWorkflowModel;
import org.alfresco.repo.publishing.PublishingModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.publishing.PublishingDetails;
import org.alfresco.service.cmr.publishing.PublishingEvent;
import org.alfresco.service.cmr.publishing.Status;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.wcm.util.WCMUtil;
import org.apache.commons.collections.FastArrayList;
import org.apache.commons.lang.StringUtils;
import org.craftercms.cstudio.alfresco.constant.CStudioConstants;
import org.craftercms.cstudio.alfresco.constant.CStudioContentModel;
import org.craftercms.cstudio.alfresco.deployment.DeploymentItemDAO;
import org.craftercms.cstudio.alfresco.dm.constant.DmConstants;
import org.craftercms.cstudio.alfresco.dm.service.api.*;
import org.craftercms.cstudio.alfresco.dm.to.*;
import org.craftercms.cstudio.alfresco.dm.util.DmContentItemComparator;
import org.craftercms.cstudio.alfresco.dm.util.DmUtils;
import org.craftercms.cstudio.alfresco.dm.util.ScheduleItem;
import org.craftercms.cstudio.alfresco.dm.util.WorkflowProgress;
import org.craftercms.cstudio.alfresco.dm.util.impl.ScheduleDeleteHandler;
import org.craftercms.cstudio.alfresco.dm.workflow.GoLiveContext;
import org.craftercms.cstudio.alfresco.dm.workflow.MultiChannelPublishingContext;
import org.craftercms.cstudio.alfresco.dm.workflow.RequestContext;
import org.craftercms.cstudio.alfresco.dm.workflow.WorkflowProcessor;
import org.craftercms.cstudio.alfresco.dm.workflow.operation.SubmitLifeCycleOperation;
import org.craftercms.cstudio.alfresco.dm.workflow.operation.presubmit.PreGoLiveOperation;
import org.craftercms.cstudio.alfresco.dm.workflow.operation.presubmit.PreScheduleDeleteOperation;
import org.craftercms.cstudio.alfresco.dm.workflow.operation.presubmit.PreScheduleOperation;
import org.craftercms.cstudio.alfresco.dm.workflow.operation.presubmit.PreSubmitDeleteOperation;
import org.craftercms.cstudio.alfresco.service.api.*;
import org.craftercms.cstudio.alfresco.service.exception.ContentNotFoundException;
import org.craftercms.cstudio.alfresco.service.exception.ServiceException;
import org.craftercms.cstudio.api.service.deployment.CopyToEnvironmentItem;
import org.craftercms.cstudio.api.service.deployment.DeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DmSimpleWorkflowServiceImpl extends DmWorkflowServiceImpl {

    private Logger logger = LoggerFactory.getLogger(DmSimpleWorkflowServiceImpl.class);

    public static final String ACTION_APPROVAL = "approval";

    protected WorkflowProcessor _workflowProcessor;

    public WorkflowProcessor getWorkflowProcessor() {
        return _workflowProcessor;
    }

    public void setWorkflowProcessor(WorkflowProcessor workflowProcessor) {
        this._workflowProcessor = workflowProcessor;
    }

    protected boolean isScheduleDeleteHandlerThrStarted;
    protected ScheduleDeleteHandler _scheduleDeleteHandler = null;

    public ScheduleDeleteHandler getScheduleDeleteHandler() {
        return _scheduleDeleteHandler;
    }

    public void setScheduleDeleteHandler(ScheduleDeleteHandler scheduleDeleteHandler) {
        this._scheduleDeleteHandler = scheduleDeleteHandler;
    }

    protected boolean customContentTypeNotification;

    public void setCustomContentTypeNotification(boolean customContentTypeNotification) {
        this.customContentTypeNotification = customContentTypeNotification;
    }

    protected String customContentTypeNotificationPattern;

    public void setCustomContentTypeNotificationPattern(String customContentTypeNotificationPattern) {
        this.customContentTypeNotificationPattern = customContentTypeNotificationPattern;
    }

    protected DeploymentService _deploymentService;
    public DeploymentService getDeploymentService() {
        return _deploymentService;
    }
    public void setDeploymentService(DeploymentService deploymentService) {
        this._deploymentService = deploymentService;
    }

    @Override
    public boolean isInFlight(String assetPath) {
        if (assetPath.endsWith(DmConstants.XML_PATTERN)) {
            if (assetPath.endsWith(DmConstants.INDEX_FILE)) {
                String parentDir = DmUtils.getParentUrl(assetPath);
                return _workflowProcessor.isInFlight(assetPath) || _workflowProcessor.isInFlight(parentDir);
            }
            return _workflowProcessor.isInFlight(assetPath);
        } else {
            return _workflowProcessor.isInFlight(assetPath) || _workflowProcessor.isInFlight(assetPath + "/" + DmConstants.INDEX_FILE);
        }
    }

    @Override
    public List<DmError> submitToGoLive(List<DmDependencyTO> submittedItems, Date scheduledDate, boolean sendEmail, boolean submitForDeletion, RequestContext requestContext, String submissionComment) throws ServiceException {
        List<DmError> errors = new ArrayList<DmError>();
        String site = requestContext.getSite();
        String submittedBy = requestContext.getUser();
        DmContentService dmContentService = getService(DmContentService.class);
        for (DmDependencyTO submittedItem : submittedItems) {
            try {
                DependencyRules rule = new DependencyRules(site, getServicesManager());
                submitThisAndReferredComponents(submittedItem, site, scheduledDate, sendEmail, submitForDeletion, submittedBy, rule, submissionComment);
                List<DmDependencyTO> children = submittedItem.getChildren();
                if (children != null && !submitForDeletion) {
                    for (DmDependencyTO child : children) {
                        if (!child.isReference()) {
                            submitThisAndReferredComponents(child, site, scheduledDate, sendEmail, submitForDeletion, submittedBy, rule, submissionComment);
                        }
                    }
                }
            } catch (ContentNotFoundException e) {
                errors.add(new DmError(site, submittedItem.getUri(), e));
            }
        }
        return errors;
    }

    protected void submitThisAndReferredComponents(DmDependencyTO submittedItem, String site, Date scheduledDate, boolean sendEmail, boolean submitForDeletion, String submittedBy, DependencyRules rule, String submissionComment) throws ServiceException {
        doSubmit(site, submittedItem, scheduledDate, sendEmail, submitForDeletion, submittedBy, true, submissionComment);
        Set<DmDependencyTO> stringSet;
        if (submitForDeletion) {
            stringSet = rule.applyDeleteDependencyRule(submittedItem);
        } else {
            stringSet = rule.applySubmitRule(submittedItem);
        }
        DmContentService dmContentService = getService(DmContentService.class);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        for (DmDependencyTO s : stringSet) {
            String fullPath = servicesConfig.getRepositoryRootPath(site) + s.getUri();
            DmContentItemTO contentItem = persistenceManagerService.getContentItem(fullPath);
            boolean lsendEmail = true;
            boolean lnotifyAdmin = true;
            lsendEmail = sendEmail && ((!contentItem.isDocument() && !contentItem.isComponent() && !contentItem.isAsset()) || customContentTypeNotification);
            lnotifyAdmin = (!contentItem.isDocument() && !contentItem.isComponent() && !contentItem.isAsset());
            // notify admin will always be true, unless for dependent document/banner/other-files
            doSubmit(site, s, scheduledDate, lsendEmail, submitForDeletion, submittedBy, lnotifyAdmin, submissionComment);
        }
    }

    /**
     * Note: notify admin will always be true, unless for dependent document/banner/other-files
     *
     * @param site
     * @param dependencyTO
     * @param scheduledDate
     * @param sendEmail
     * @param submitForDeletion
     * @param user
     * @param notifyAdmin
     */
    protected void doSubmit(final String site, final DmDependencyTO dependencyTO, final Date scheduledDate, final boolean sendEmail, final boolean submitForDeletion, final String user, final boolean notifyAdmin, final String submissionComment) {
        //first remove from workflow
        removeFromWorkflow(site, null, dependencyTO.getUri(), true);
        final DmContentService dmContentService = getService(DmContentService.class);
        final PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        String fullPath = dmContentService.getContentFullPath(site, dependencyTO.getUri());
        DmPathTO path = new DmPathTO(fullPath);
        final NodeRef node = persistenceManagerService.getNodeRef(fullPath);
        final DmStateManager action = getService(DmStateManager.class);

        DmTransactionService dmTransactionService = getService(DmTransactionService.class);
        RetryingTransactionHelper transactionHelper = dmTransactionService.getRetryingTransactionHelper();
        final NotificationService notificationService = getService(NotificationService.class);
        transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback() {
            @Override
            public Object execute() throws Throwable {
                /**
                 * added to handle issue with submitting locked content (Dejan 2012/04/12)
                 */

                if (!persistenceManagerService.getLockStatus(node).equals(LockStatus.NO_LOCK)) {
//                	persistenceManagerService.unlock(node);
                }
                /****** end ******/
                //action.markSubmit(node, user, sendEmail, scheduledDate, submitForDeletion);
                Map<QName, Serializable> nodeProperties = persistenceManagerService.getProperties(node);
                nodeProperties.put(CStudioContentModel.PROP_WEB_WF_SUBMITTED_BY, user);
                nodeProperties.put(CStudioContentModel.PROP_WEB_WF_SEND_EMAIL, sendEmail);
                nodeProperties.put(CStudioContentModel.PROP_WEB_WF_SUBMITTEDFORDELETION, submitForDeletion);

                nodeProperties.put(Version2Model.PROP_QNAME_VERSION_DESCRIPTION, submissionComment);

                if (null == scheduledDate) {
                    nodeProperties.remove(WCMWorkflowModel.PROP_LAUNCH_DATE);
                } else {
                    nodeProperties.put(WCMWorkflowModel.PROP_LAUNCH_DATE, scheduledDate);
                }
                persistenceManagerService.setProperties(node, nodeProperties);
                if (scheduledDate != null) {
                    persistenceManagerService.transition(node, ObjectStateService.TransitionEvent.SUBMIT_WITH_WORKFLOW_SCHEDULED);
                } else {
                    persistenceManagerService.transition(node, ObjectStateService.TransitionEvent.SUBMIT_WITH_WORKFLOW_UNSCHEDULED);
                }
                _listener.postSubmitToGolive(site, dependencyTO);
                if (notifyAdmin) {
                    ServicesConfig servicesConfig = getService(ServicesConfig.class);
                    PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
                    String fullPath = servicesConfig.getRepositoryRootPath(site) + dependencyTO.getUri();
                    DmContentItemTO contentItem = persistenceManagerService.getContentItem(fullPath);
                    boolean isPreviewable = contentItem.isPreviewable();
                    notificationService.sendContentSubmissionNotification(site, AuthenticationUtil.getAdminUserName(), dependencyTO.getUri(), user, scheduledDate, isPreviewable, submitForDeletion);
                }
                return null;
            }
        });

    }

    public void fillQueue(String site, String storeName, GoLiveQueue goLiveQueue, GoLiveQueue inProcessQueue) throws ServiceException {
        //_cacheManager.put(Scope.DM_SUBMITTED_ITEMS, CStudioConstants.DM_GO_LIVE_CACHE_KEY ,site, goLiveQueue);
        //SearchService searchService = getService(SearchService.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        //List<NodeRef> changeSet = searchService.findNodes(CStudioConstants.STORE_REF, getSubmittedItemsQuery(site));
        List<NodeRef> changeSet = persistenceManagerService.getSubmittedItems(site);
        // TODO: implement list changed all

        // the category item to add all other items that do not belong to
        // regular categories specified in the configuration
        if (changeSet != null) {
            // add all content items from each task if task is the review task
            for (NodeRef nodeRef : changeSet) {
                try {
                    addToQueue(site, goLiveQueue, inProcessQueue, nodeRef);
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Could not warm cache for [" + persistenceManagerService.getNodePath(nodeRef) + "] " + e.getMessage());
                    }
                }
            }
        }
    }

    protected void addToQueue(String site, GoLiveQueue queue, GoLiveQueue inProcessQueue, NodeRef node) throws ServiceException {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        FileInfo nodeInfo = persistenceManagerService.getFileInfo(node);
        if (nodeInfo != null && !nodeInfo.isFolder()) {
            ObjectStateService objectStateService = getService(ObjectStateService.class);
            ObjectStateService.State nodeState = objectStateService.getObjectState(node);
            //add only submitted items to go live Q.
            if (ObjectStateService.State.isSubmitted(nodeState)) {
                DmContentItemTO to = persistenceManagerService.getContentItem(persistenceManagerService.getNodePath(node), false);
                queue.add(to);
            }
        } else {
            List<FileInfo> children = persistenceManagerService.list(node);
            for (FileInfo child : children) {
                addToQueue(site, queue, inProcessQueue, child.getNodeRef());
            }
        }
        if (inProcessQueue != null)
            addToInProcess(site, inProcessQueue, node);
    }


    protected void addToInProcess(String site, GoLiveQueue inProcessQueue, NodeRef node) throws ServiceException {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        ObjectStateService.State state = persistenceManagerService.getObjectState(node);
        if (!ObjectStateService.State.isLive(state)) {
            DmContentItemTO to = persistenceManagerService.getContentItem(persistenceManagerService.getNodePath(node), false);
            if (to != null) {
                inProcessQueue.add(to);
                inProcessQueue.add(to.getPath(), to);
            }
        }
    }

    @Override
    public void doDelete(String site, String sub, List<DmDependencyTO> submittedItems, String approver) throws ServiceException {
        long start = System.currentTimeMillis();
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        String user = persistenceManagerService.getCurrentUserName();
        // get web project information
        String assignee = getAssignee(site, sub);
        // Don't make go live an item if it is new and to be deleted
        final Date now = new Date();
        List<String> itemsToDelete = new FastList<String>();
        List<DmDependencyTO> deleteItems = new FastList<DmDependencyTO>();
        List<DmDependencyTO> scheItems = new FastList<DmDependencyTO>();
        DmContentService dmContentService = getService(DmContentService.class);
        for (DmDependencyTO submittedItem : submittedItems) {
            String uri = submittedItem.getUri();
            Date schDate = submittedItem.getScheduledDate();
            boolean isItemForSchedule = false;
            if (schDate == null || schDate.before(now)) {
                // Sending Notification
                if (StringUtils.isNotEmpty(approver)) {
                    // immediate delete
                    if (submittedItem.isSendEmail()) {
                        sendDeleteApprovalNotification(site, submittedItem, approver);//TODO move it after delete actually happens
                    }
                }
                if (submittedItem.getUri().endsWith(DmConstants.INDEX_FILE)) {
                    submittedItem.setUri(submittedItem.getUri().replace("/" + DmConstants.INDEX_FILE, ""));
                }
                itemsToDelete.add(uri);
            } else {
                scheItems.add(submittedItem);
                isItemForSchedule = true;
            }
            submittedItem.setDeleted(true);
            // replace with the folder name
            boolean isNew = dmContentService.isNew(site, uri);
            if (!isNew || isItemForSchedule) {
                deleteItems.add(submittedItem);
            }
            NodeRef itemToDelete = persistenceManagerService.getNodeRef(dmContentService.getContentFullPath(site,uri));
            if(persistenceManagerService.hasAspect(itemToDelete, CStudioContentModel.ASPECT_RENAMED)){
            	String oldPath = (String) persistenceManagerService.getProperty(itemToDelete, CStudioContentModel.PROP_RENAMED_OLD_URL);
            	if(oldPath!=null){
            		itemsToDelete.add(oldPath);//Make sure old path is going to be deleted
            	}
            }
        }
        //List<String> deletedItems = deleteInTransaction(site, itemsToDelete);
        GoLiveContext context = new GoLiveContext(approver, site);
        //final String pathPrefix = getSiteRoot(site, null);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        final String pathPrefix = servicesConfig.getRepositoryRootPath(site);
        Map<Date, List<DmDependencyTO>> groupedPackages = groupByDate(deleteItems, now);
        if (groupedPackages.isEmpty()) {
            groupedPackages.put(now, Collections.<DmDependencyTO>emptyList());
        }
        for (Date scheduledDate : groupedPackages.keySet()) {
            List<DmDependencyTO> deletePackage = groupedPackages.get(scheduledDate);
            SubmitPackage submitpackage = new SubmitPackage(pathPrefix);
            Set<String> rescheduledUris = new HashSet<String>();
            if (deletePackage != null) {
                Date launchDate = scheduledDate.equals(now) ? null : scheduledDate;
                for (DmDependencyTO dmDependencyTO : deletePackage) {
                    if (launchDate != null) {
                        handleReferences(site, submitpackage, dmDependencyTO, true, null, "", rescheduledUris);
                    } else {
                        applyDeleteDependencyRule(site, submitpackage, dmDependencyTO);
                    }
                }
                String label = submitpackage.getLabel();
                String workFlowName = _submitDirectWorkflowName;

                SubmitLifeCycleOperation deleteOperation = null;
                Set<String> liveDependencyItems = new HashSet<String>();
                Set<String> allItems = new HashSet<String>();
                for (String uri : itemsToDelete) {//$ToDO $ remove this case and keep the item in go live queue
                    GoLiveDeleteCandidates deleteCandidate = dmContentService.getDeleteCandidates(context.getSite(), uri);
                    allItems.addAll(deleteCandidate.getAllItems());
                    //get all dependencies that has to be removed as well
                    liveDependencyItems.addAll(deleteCandidate.getLiveDependencyItems());
                }

                List<String> submitPackPaths = submitpackage.getPaths();
                if (launchDate != null) {
                    deleteOperation = new PreScheduleDeleteOperation(this, submitpackage.getUris(), launchDate, context, rescheduledUris);
                    label = DmConstants.DM_SCHEDULE_SUBMISSION_FLOW + ":" + label;
                    workFlowName = _reviewWorkflowName;
                    for (String submitPackPath : submitpackage.getUris()) {
                        String fullpath = dmContentService.getContentFullPath(site, submitPackPath);
                        _cacheManager.invalidateAndRemoveFromQueue(fullpath, site);
                    }
                } else {
                    //add dependencies to submitPackage
                    for (String liveDependency : liveDependencyItems) {
                        DmPathTO pathTO = new DmPathTO(liveDependency);
                        submitpackage.addToPackage(pathTO.getRelativePath());
                    }
                    submitPackPaths = submitpackage.getPaths();
                    
                    deleteOperation = new PreSubmitDeleteOperation(this, new HashSet<String>(itemsToDelete), context, rescheduledUris);
                    removeChildFromSubmitPackForDelete(submitPackPaths);
                    for (String deleteCandidate : allItems) {
                        //_cacheManager.invalidateAndRemoveFromQueue(deleteCandidate, site);
                    }
                }
                Map<String, String> submittedBy = new FastMap<String, String>();

                SearchService searchService = getService(SearchService.class);
                for (String longPath : submitPackPaths) {
                    String uri = longPath.substring(pathPrefix.length());
                    DmUtils.addToSubmittedByMapping(persistenceManagerService, dmContentService, searchService, site, uri, submittedBy, approver);
                }

                _workflowProcessor.addToWorkflow(site, new ArrayList<String>(), launchDate, workFlowName, label, deleteOperation, approver, null);
            }
        }
        if (logger.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            logger.debug("Submitted deleted items to queue time = " + (end - start));
        }
    }

    protected void sendDeleteApprovalNotification(String site, DmDependencyTO submittedItem, String approver) {
        try {

            if (submittedItem.isSendEmail()) {
                PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
                String uri = submittedItem.getUri();

                DmContentService dmContentService = getService(DmContentService.class);
                String path = dmContentService.getContentFullPath(site, uri);
                NodeRef node = persistenceManagerService.getNodeRef(path);
                if (node != null) {
                    //Prepare to send notification

                    Serializable submittedByValue = persistenceManagerService.getProperty(node, CStudioContentModel.PROP_WEB_WF_SUBMITTED_BY);
                    String submittedBy = "";
                    if (submittedByValue != null) {
                        submittedBy = (String) submittedByValue;
                        NotificationService notificationService = getService(NotificationService.class);
                        notificationService.sendDeleteApprovalNotification(site, submittedBy, uri, approver);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not send delete approval notification for newly created item", e);
            }
        }
    }

    protected void removeChildFromSubmitPackForDelete(List<String> paths) {
        Iterator<String> itr = paths.iterator();
        while (itr.hasNext()) {
            String path = itr.next();
            if (checkParentExistsInSubmitPackForDelete(paths, path)) {
                itr.remove();
            }
        }
    }

    protected boolean checkParentExistsInSubmitPackForDelete(List<String> paths, String path) {
        String split[] = path.split("/");
        for (int i = split.length - 1; i >= 0; i--) {
            int lastIndex = path.lastIndexOf(split[i]) - 1;
            if (lastIndex > 0) {
                path = path.substring(0, lastIndex);
                if (paths.contains(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void handleReferences(String site, SubmitPackage submitpackage, DmDependencyTO dmDependencyTO, boolean isNotScheduled, SubmitPackage dependencyPackage, String approver, Set<String> rescheduledUris) {//,boolean isReferencePage) {
        DmContentService dmContentService = getService(DmContentService.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        String path = dmContentService.getContentFullPath(site, dmDependencyTO.getUri());
        NodeRef node = persistenceManagerService.getNodeRef(path);
        Serializable scheduledDateValue = persistenceManagerService.getProperty(node, WCMWorkflowModel.PROP_LAUNCH_DATE);
        Date scheduledDate = DefaultTypeConverter.INSTANCE.convert(Date.class, scheduledDateValue);
        if (!dmDependencyTO.isSubmitted() && scheduledDate != null && scheduledDate.equals(dmDependencyTO.getScheduledDate())) {
            return;
        }
        if (isNotScheduled || !dmDependencyTO.isReference()) {
            submitpackage.addToPackage(dmDependencyTO);
        }

        DependencyRules rule = new DependencyRules(site, getServicesManager());
        Set<DmDependencyTO> dependencyTOSet;
        if (dmDependencyTO.isSubmittedForDeletion() || dmDependencyTO.isDeleted()) {
            dependencyTOSet = rule.applyDeleteDependencyRule(dmDependencyTO);
        } else {
            long start = System.currentTimeMillis();
            dependencyTOSet = rule.applySubmitRule(dmDependencyTO);
            if (logger.isDebugEnabled()) {
                long end = System.currentTimeMillis();
                logger.debug("Time to get dependencies rule = " + (end - start));
            }
        }
        for (DmDependencyTO dependencyTO : dependencyTOSet) {
            if (isNotScheduled) {
                submitpackage.addToPackage(dependencyTO);
            } else {
                dependencyPackage.addToPackage(dependencyTO);
            }
        }

        if (isRescheduleRequest(dmDependencyTO, site)) {
            rescheduledUris.add(dmDependencyTO.getUri());
            if (dmDependencyTO.isNow()) {
                //dmStateManager.markNow(node);
            }
        }
    }

    protected void applyDeleteDependencyRule(String site, SubmitPackage pack, DmDependencyTO dmDependencyTO) {
        pack.addToPackage(dmDependencyTO);
        DmContentService dmContentService = getService(DmContentService.class);
        DependencyRules rule = new DependencyRules(site, getServicesManager());
        Set<DmDependencyTO> dependencyTOSet = rule.applyDeleteDependencyRule(dmDependencyTO);
        for (DmDependencyTO dependencyTO : dependencyTOSet) {
            pack.addToPackage(dependencyTO);
        }
    }

    @Override
    public void preScheduleDelete(Set<String> urisToDelete, final Date scheduleDate, final GoLiveContext context, Set rescheduledUris)
            throws ServiceException {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        DmContentService dmContentService = getService(DmContentService.class);
        DmTransactionService dmTransactionService = getService(DmTransactionService.class);
        final DmPublishService dmPublishService = getService(DmPublishService.class);
        final String site = context.getSite();
        final List<String> itemsToDelete = new ArrayList<String>(urisToDelete);
        RetryingTransactionHelper txnHelper = dmTransactionService.getRetryingTransactionHelper();
        RetryingTransactionHelper.RetryingTransactionCallback<Void> cancelWorkflowCallBack = new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
            public Void execute() throws Throwable {
                dmPublishService.unpublish(site, itemsToDelete, context.getApprover(), scheduleDate);
                return null;
            }
        };
        txnHelper.doInTransaction(cancelWorkflowCallBack, false, true);
    }

    @Override
    public List<String> preDelete(Set<String> urisToDelete, GoLiveContext context, Set<String> rescheduledUris) throws ServiceException {
        cleanUrisFromWorkflow(null, urisToDelete, context.getSite());
        cleanUrisFromWorkflow(null, rescheduledUris, context.getSite());
        List<String> deletedItems = deleteInTransaction(context.getSite(), new ArrayList<String>(urisToDelete), true, context.getApprover());
        return deletedItems;
    }

    protected List<String> deleteInTransaction(final String site, final List<String> itemsToDelete, final boolean generateActivity, final String approver) throws ServiceException {
        DmTransactionService dmTransactionService = getService(DmTransactionService.class);
        final DmPublishService dmPublishService = getService(DmPublishService.class);
        final DmContentService dmContentService = getService(DmContentService.class);
        RetryingTransactionHelper txnHelper = dmTransactionService.getRetryingTransactionHelper();
        RetryingTransactionHelper.RetryingTransactionCallback<List<String>> cancelWorkflowCallBack = new RetryingTransactionHelper.RetryingTransactionCallback<List<String>>() {
            public List<String> execute() throws Throwable {
                dmPublishService.unpublish(site, itemsToDelete, approver);
                if (!enableNewDeploymentEngine) {
                    return dmContentService.deleteContents(site, itemsToDelete, generateActivity, approver);
                } else {
                    return null;
                }
            }
        };
        return txnHelper.doInTransaction(cancelWorkflowCallBack, false, false);
    }

    protected void cleanUrisFromWorkflow(final String sandBox, final Set<String> uris, final String site) {
        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                cleanWorkflow(sandBox, uri, site, Collections.<DmDependencyTO>emptySet());
            }
        }
    }

    public boolean cleanWorkflow(final String sandBox, final String url, final String site, final Set<DmDependencyTO> dependents) {
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        String rootPath = servicesConfig.getRepositoryRootPath(site);
        String fullPath = rootPath + url;
        NodeRef nodeRef = persistenceManagerService.getNodeRef(fullPath);
       
        String currentUser = persistenceManagerService.getCurrentUserName();
        List<PublishingEvent> events = persistenceManagerService.getPublishEventsForNode(nodeRef);
        if (events != null && events.size() > 0) {
            for (PublishingEvent event : events) {
                if (event.getStatus().equals(Status.SCHEDULED)) {
                    Set<NodeRef> eventNodes = event.getPackage().getNodesToPublish();
                    List<NodeRef> nodesToPublish = new FastList<NodeRef>();
                    for (NodeRef eventNode : eventNodes) {
                        if (!eventNode.equals(nodeRef)) nodesToPublish.add(eventNode);
                    }
                    Calendar scheduledDate = event.getScheduledTime();
                    String channelId = event.getChannelId();
                    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
                    persistenceManagerService.cancelPublishingEvent(event.getId());
                    if (nodesToPublish.size() > 0) {
                        PublishingDetails publishingDetails = persistenceManagerService.createPublishingDetails();
                        publishingDetails.addNodesToPublish(nodesToPublish);
                        publishingDetails.setSchedule(scheduledDate);
                        publishingDetails.setPublishChannelId(channelId);
                        persistenceManagerService.scheduleNewEvent(publishingDetails);
                    }
                    AuthenticationUtil.setFullyAuthenticatedUser(currentUser);
                }
            }
        }
        return true;
    }

    protected void revert(String site, String path, String fullPath, String previewFullPath) {

    }

    @Override
    public Map<Date, List<DmDependencyTO>> groupByDate(List<DmDependencyTO> submittedItems, Date now) {
        Map<Date, List<DmDependencyTO>> groupedPackages = new FastMap<Date, List<DmDependencyTO>>();
        for (DmDependencyTO submittedItem : submittedItems) {

            Date scheduledDate = (submittedItem.isNow()) ? null : submittedItem.getScheduledDate();
            if (scheduledDate == null || scheduledDate.before(now)) {
                scheduledDate = now;
            }
            List<DmDependencyTO> goLivePackage = groupedPackages.get(scheduledDate);
            if (goLivePackage == null)
                goLivePackage = new FastList<DmDependencyTO>();
            goLivePackage.add(submittedItem);
            groupedPackages.put(scheduledDate, goLivePackage);
        }

        return groupedPackages;
    }

    @Override
    public List<DmContentItemTO> getGoLiveItems(final String site, final String sub,
                                                final DmContentItemComparator comparator) throws ServiceException {
        String storeName = DmUtils.createStoreName(site);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        List<String> displayPatterns = servicesConfig.getDisplayInWidgetPathPatterns(site);
        List<DmContentItemTO> categoryItems = getCategoryItems(site, sub, storeName);
        //GoLiveQueue queue = (GoLiveQueue) _cacheManager.get(Scope.DM_SUBMITTED_ITEMS, CStudioConstants.DM_GO_LIVE_CACHE_KEY,site);
        //if (queue == null) {
        GoLiveQueue queue = new GoLiveQueue();
        fillQueue(site, storeName, queue, null);

        //}

        Set<DmContentItemTO> queueItems = queue.getQueue();
        DmContentItemTO.ChildFilter childFilter = new GoLiveQueueChildFilter(queue);
        DmContentService dmContentService = getService(DmContentService.class);
        GoLiveQueueOrganizer goLiveQueueOrganizer = new GoLiveQueueOrganizer(dmContentService, childFilter);
        for (DmContentItemTO queueItem : queueItems) {
            if (queueItem.getLastEditDate() != null) {
                queueItem.setEventDate(queueItem.getLastEditDate());
            }
            goLiveQueueOrganizer.addToGoLiveItems(site, queueItem, categoryItems, comparator, false, displayPatterns);
        }
        return categoryItems;
    }

    @Override
    public void reject(String site, String sub, List<DmDependencyTO> submittedItems, String reason, String approver) {
        if (submittedItems != null) {
            // for each top level items submitted
            // add its children and dependencies that must go with the top level
            // item to the submitted aspect
            // and only submit the top level items to workflow
            DmContentService dmContentService = getService(DmContentService.class);
            for (DmDependencyTO dmDependencyTO : submittedItems) {
                DependencyRules rule = new DependencyRules(site, getServicesManager());
                rejectThisAndReferences(site, dmDependencyTO, rule, approver, reason);
                List<DmDependencyTO> children = dmDependencyTO.getChildren();
                if (children != null) {
                    for (DmDependencyTO child : children) {
                        rejectThisAndReferences(site, child, rule, approver, reason);
                    }
                }


            }
        }

        // TODO: send the reason to the user
    }

    protected void rejectThisAndReferences(String site, DmDependencyTO dmDependencyTO, DependencyRules rule, String approver, String reason) {
        _reject(site, dmDependencyTO, approver, true, reason);
        Set<DmDependencyTO> dependencyTOSet = rule.applyRejectRule(dmDependencyTO);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        for (DmDependencyTO dependencyTO : dependencyTOSet) {
            boolean lsendEmail = true;
            try {
                String fullPath = servicesConfig.getRepositoryRootPath(site) + dependencyTO.getUri();
                DmContentItemTO contentItem = persistenceManagerService.getContentItem(fullPath);
                lsendEmail = !contentItem.isDocument() && !contentItem.isComponent() && !contentItem.isAsset();
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("during rejection, content retrieve failed");
                }
                lsendEmail = false;
            }
            _reject(site, dependencyTO, approver, lsendEmail, reason);
        }
    }

    protected void _reject(String site, DmDependencyTO dmDependencyTO, String approver, boolean sendEmail, String reason) {
        DmContentService dmContentService = getService(DmContentService.class);
        String path = dmContentService.getContentFullPath(site, dmDependencyTO.getUri());
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        NodeRef node = persistenceManagerService.getNodeRef(path);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        if (node != null) {
            Serializable submittedByValue = persistenceManagerService.getProperty(node, CStudioContentModel.PROP_WEB_WF_SUBMITTED_BY);
            String submittedBy = (submittedByValue == null ? null : (String) submittedByValue);
            if (sendEmail && StringUtils.isNotEmpty(submittedBy) && StringUtils.isNotEmpty(approver)) {
                boolean isPreviewable = true;
                try {
                    String fullPath = servicesConfig.getRepositoryRootPath(site) + dmDependencyTO.getUri();
                    DmContentItemTO contentItem = persistenceManagerService.getContentItem(fullPath);
                    isPreviewable = contentItem.isPreviewable();
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Item cannot be retrieved during rejection notification" + path);
                    }
                }
                NotificationService notificationService = getService(NotificationService.class);
                notificationService.sendRejectionNotification(site, submittedBy, dmDependencyTO.getUri(), reason, approver, isPreviewable);
            }
            Map<QName, Serializable> nodeProps = persistenceManagerService.getProperties(node);
            for (QName propName : DmConstants.SUBMITTED_PROPERTIES) {
                nodeProps.remove(propName);
            }
            persistenceManagerService.setProperties(node, nodeProps);
        }
        _listener.postReject(site, dmDependencyTO);
    }

    @Override
    public void goLive(final String site, String sub, List<DmDependencyTO> submittedItems, final String approver) throws ServiceException {
        goLive(site, sub, submittedItems, approver, null);
    }

    @Override
    public void goLive(final String site, String sub, List<DmDependencyTO> submittedItems, final String approver, final MultiChannelPublishingContext mcpContext) throws ServiceException {
        long start = System.currentTimeMillis();
        // get web project information
        final String assignee = getAssignee(site, sub);
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        final String pathPrefix = servicesConfig.getRepositoryRootPath(site);
        final Date now = new Date();
        if (submittedItems != null) {
            // group submitted items into packages by their scheduled date
            Map<Date, List<DmDependencyTO>> groupedPackages = groupByDate(submittedItems, now);

            for (Date scheduledDate : groupedPackages.keySet()) {
                List<DmDependencyTO> goLivePackage = groupedPackages.get(scheduledDate);
                if (goLivePackage != null) {
                    Date launchDate = scheduledDate.equals(now) ? null : scheduledDate;

                    final boolean isNotScheduled = (launchDate == null);
                    // for submit direct, package them together and submit them
                    // together as direct submit
                    final SubmitPackage submitpackage = new SubmitPackage(pathPrefix);
                    /*
                        dependencyPackage holds references of page.
                     */
                    final Set<String> rescheduledUris = new HashSet<String>();
                    final SubmitPackage dependencyPackage = new SubmitPackage("");
                    DmTransactionService dmTransactionService = getService(DmTransactionService.class);
                    for (final DmDependencyTO dmDependencyTO : goLivePackage) {
                        RetryingTransactionHelper helper = dmTransactionService.getRetryingTransactionHelper();
                        helper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback() {
                            @Override
                            public Object execute() throws Throwable {
                                goLivepackage(site, submitpackage, dmDependencyTO, isNotScheduled, dependencyPackage, approver, rescheduledUris);
                                return null;
                            }
                        }, false, true);
                    }

                    List<String> stringList = submitpackage.getPaths();
                    String label = submitpackage.getLabel();
                    SubmitLifeCycleOperation operation = null;
                    GoLiveContext context = new GoLiveContext(approver, site);
                    if (!isNotScheduled) {
                        Set<String> uris = new HashSet<String>();
                        uris.addAll(dependencyPackage.getUris());
                        uris.addAll(submitpackage.getUris());
                        label = getScheduleLabel(submitpackage, dependencyPackage);
                        operation = new PreScheduleOperation(this, uris, launchDate, context, rescheduledUris);
                    } else {
                        operation = new PreGoLiveOperation(this, submitpackage.getUris(), context, rescheduledUris);
                    }
                    if (!stringList.isEmpty()) {
                        // get the workflow initiator mapping
                        Map<String, String> submittedBy = new FastMap<String, String>();

                        DmContentService dmContentService = getService(DmContentService.class);
                        DmPublishService dmPublishService = getService(DmPublishService.class);
                        for (String longPath : stringList) {
                            String uri = longPath.substring(pathPrefix.length());
                            DmUtils.addToSubmittedByMapping(getService(PersistenceManagerService.class), dmContentService, site, uri, submittedBy, approver);
                            dmPublishService.cancelScheduledItem(site, uri);
                        }
                        _workflowProcessor.addToWorkflow(site, stringList, launchDate, _submitDirectWorkflowName, label, operation, approver, mcpContext);
                    }
                    Set<DmDependencyTO> dependencyTOSet = submitpackage.getItems();
                    for (DmDependencyTO dmDependencyTO : dependencyTOSet) {
                        _listener.postGolive(site, dmDependencyTO);
                    }
                    dependencyTOSet = dependencyPackage.getItems();
                    for (DmDependencyTO dmDependencyTO : dependencyTOSet) {
                        _listener.postGolive(site, dmDependencyTO);
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            logger.debug("Total go live time = " + (end - start));
        }
    }

    protected void goLivepackage(String site, SubmitPackage submitpackage, DmDependencyTO dmDependencyTO, boolean isNotScheduled, SubmitPackage dependencyPackage, String approver, Set<String> rescheduledUris) {
        handleReferences(site, submitpackage, dmDependencyTO, isNotScheduled, dependencyPackage, approver, rescheduledUris);
        List<DmDependencyTO> children = dmDependencyTO.getChildren();
        if (children != null) {
            for (DmDependencyTO child : children) {
                handleReferences(site, submitpackage, child, isNotScheduled, dependencyPackage, approver, rescheduledUris);
                goLivepackage(site, submitpackage, child, isNotScheduled, dependencyPackage, approver, rescheduledUris);
            }
        } /*
        List<DmDependencyTO> deps = dmDependencyTO.getDirectDependencies();
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        String siteRoot = servicesConfig.getRepositoryRootPath(site);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        if (deps != null) {
            for (DmDependencyTO dep : deps) {
                String fullPath = siteRoot + dep.getUri();
                if (persistenceManagerService.isNew(fullPath)) {
                    handleReferences(site, submitpackage, dep, isNotScheduled, dependencyPackage, approver, rescheduledUris);
                }
                goLivepackage(site, submitpackage, dep, isNotScheduled, dependencyPackage, approver, rescheduledUris);
            }
        }   */
    }

    protected String getScheduleLabel(SubmitPackage submitPackage, SubmitPackage dependencyPack) {
        StringBuilder builder = new StringBuilder("schedule_workflow:");
        builder.append(submitPackage.getLabel()).
                append(",").
                append(dependencyPack.getLabel());
        String label = builder.toString();
        if (label.length() > 255) {
            label = label.substring(0, 252) + "..";
        }
        return label;

    }

    /*
    * (non-Javadoc)
    * @see org.craftercms.cstudio.alfresco.dm.service.impl.DmWorkflowServiceImpl#prepareSubmission(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String)
    */
    public void prepareSubmission(NodeRef packageRef, String workflowId, String desc) {
        boolean sendNotice = false;
        this.prepareStagingSubmission(packageRef, workflowId, desc, sendNotice);
    }

    /*
     * (non-Javadoc)
     * @see org.craftercms.cstudio.alfresco.wcm.service.api.WcmWorkflowService#prepareStagingSubmission(org.alfresco.service.cmr.repository.NodeRef, java.lang.String, java.lang.String, boolean)
     */
    public void prepareStagingSubmission(NodeRef packageRef, String workflowId, String desc, boolean sendNotice) {
        if (logger.isDebugEnabled()) {
            logger.debug("Staring prepareStagingSubmission");
        }
        if (DmUtils.isBootStrapWorkflow(desc)) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            String site = null;

            if (logger.isDebugEnabled()) {
                logger.debug("preparing staging submission for  site: "
                        + site);
            }

            // TODO: send email to individual owner
            SearchService searchService = getService(SearchService.class);
            if (DmUtils.isScheduleSubmission(desc)) {
                List<NodeRef> changeSet = searchService.findNodes(CStudioConstants.STORE_REF, getListChangedQuery(site));
                if (changeSet != null && changeSet.size() > 0) {
                    for (NodeRef workFlowNode : changeSet) {
                        if (workFlowNode != null) {
                            updateWorkflowPathsWithDependenices(site, packageRef, workFlowNode);
                        }
                    }
                }
            }

            //update workflow for rename after updating dependencies for submitted items
            DmRenameService dmRenameService = getService(DmRenameService.class);
            if (DmUtils.isRenameWorkflow(desc)) {
                dmRenameService.updateWorkflow(site, desc);
            }

            //note- delete case returns changeSet empty
            ServicesConfig servicesConfig = getService(ServicesConfig.class);
            PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
            List<NodeRef> changeSet = persistenceManagerService.getChangeSet(site);
            ArrayList<DmContentItemTO> contentList = new ArrayList();
            if (sendNotice) {
                buildContentList(site, changeSet, contentList);
            }
            DmStateManager action = getService(DmStateManager.class);
            if (changeSet != null && changeSet.size() > 0) {
                for (NodeRef workFlowNode : changeSet) {
                    updateWorkflowWithRemovedDependenices(site, workFlowNode, packageRef);
                    updateStatusToLive(action, workFlowNode, site, contentList, sendNotice);
                }
            }


        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not submit to staging", e);
            }
        }
        if (logger.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            logger.debug("prepareStagingSubmission = " + (end - start));
        }
    }

    protected void updateWorkflowPathsWithDependenices(String site, NodeRef packageRef, NodeRef node) {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        DmPathTO path = new DmPathTO(persistenceManagerService.getNodePath(node));
        FileInfo fileInfo = persistenceManagerService.getFileInfo(node);
        if (fileInfo.isFolder()) {
            List<FileInfo> children = persistenceManagerService.list(node);
            for (FileInfo child : children) {
                updateWorkflowPathsWithDependenices(site, packageRef, child.getNodeRef());
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Updating dependencies for " + path.getRelativePath());
            }
            addDependenciesToWorkFlow(site, path.getRelativePath(), packageRef);
            updateMandatoryParentDependency(site, packageRef, path);
        }
    }

    protected void addDependenciesToWorkFlow(String site, String relativePath, NodeRef packageRef) {
        DmDependencyService dmDependencyService = getService(DmDependencyService.class);
        DmDependencyTO dmDependencyTo = dmDependencyService.getDependencies(site, null, relativePath, false, true);

        List<DmDependencyTO> pages = dmDependencyTo.getPages();
        updateWorkFlowWithDependencies(pages, site, packageRef, false);

        List<DmDependencyTO> components = dmDependencyTo.getComponents();
        updateWorkFlowWithDependencies(components, site, packageRef, true);

        List<DmDependencyTO> documents = dmDependencyTo.getDocuments();
        updateWorkFlowWithDependencies(documents, site, packageRef, true);

        List<DmDependencyTO> assets = dmDependencyTo.getAssets();
        updateWorkFlowWithDependencies(assets, site, packageRef, true);

        List<DmDependencyTO> templates = dmDependencyTo.getRenderingTemplates();
        updateWorkFlowWithDependencies(templates, site, packageRef, true);

        List<DmDependencyTO> levelDescriptors = dmDependencyTo.getLevelDescriptors();
        updateWorkFlowWithDependencies(levelDescriptors, site, packageRef, true);
    }

    protected void updateWorkFlowWithDependencies(List<DmDependencyTO> dependencies, String site, NodeRef packageRef, boolean isUpdateOrNew) {
        if (dependencies != null) {
            DmContentService dmContentService = getService(DmContentService.class);
            for (DmDependencyTO dependencyTo : dependencies) {
                boolean updateWorkFlow;
                if (isUpdateOrNew) {
                    updateWorkFlow = dmContentService.isUpdatedOrNew(site, dependencyTo.getUri());
                } else {
                    updateWorkFlow = dmContentService.isNew(site, dependencyTo.getUri());
                }
                if (updateWorkFlow) {
                    String uri = dependencyTo.getUri();
                    String fullPath = dmContentService.getContentFullPath(site, uri);
                    //updateWorkFlow(site, fullPath, packageRef);
                    if (dependencyTo.getUri().endsWith(DmConstants.XML_PATTERN)) {
                        addDependenciesToWorkFlow(site, dependencyTo.getUri(), packageRef);
                    }
                }
            }
        }
    }

    protected void updateWorkFlow(String site, String fullPath, NodeRef eventNodeRef) {
        PublishingEvent event = _publishingEventHelper.getPublishingEvent(eventNodeRef);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        NodeRef node = persistenceManagerService.getNodeRef(fullPath);
        if (node != null) {
            event.getPackage().getNodesToPublish().add(node);
        }
    }

    /**
     * @param site
     * @param packageRef
     * @param path
     */
    protected void updateMandatoryParentDependency(String site, NodeRef packageRef, DmPathTO path) {
        DmContentService dmContentService = getService(DmContentService.class);
        if (dmContentService.isNew(site, path.getRelativePath())) {
            DmDependencyService dmDependencyService = getService(DmDependencyService.class);
            List<String> submittedItems = new ArrayList<String>();
            submittedItems.add(path.getRelativePath());
            try {
                DmContentItemComparator comparator = new DmContentItemComparator(DmContentItemComparator.SORT_PATH, true, true, true);
                List<DmContentItemTO> contentItemTOs = dmDependencyService.getDependencies(site, submittedItems, comparator, false);
                List<DmDependencyTO> dummyDependencyTO = new FastList<DmDependencyTO>();
                addMandatoryParentToList(site, path.getRelativePath(), dummyDependencyTO, contentItemTOs);
                if (!dummyDependencyTO.isEmpty()) {
                    updateWorkFlowWithDependencies(dummyDependencyTO, site, packageRef, true);
                }
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param path
     * @param dummyDependencyTO
     * @param contentItemTOs
     */
    protected void addMandatoryParentToList(String site, String path, List<DmDependencyTO> dummyDependencyTO, List<DmContentItemTO> contentItemTOs) {
        DmContentService dmContentService = getService(DmContentService.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        for (DmContentItemTO contentItem : contentItemTOs) {
            if (!contentItem.getUri().equals(path)) {

                String fullPath = dmContentService.getContentFullPath(site, contentItem.getUri());
                NodeRef workFlowNode = persistenceManagerService.getNodeRef(fullPath);
                if (workFlowNode == null) {
                    DmDependencyTO dependencyTO = new DmDependencyTO();
                    dependencyTO.setUri(contentItem.getUri());
                    dummyDependencyTO.add(dependencyTO);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Mandatory parent added to workflow: " + contentItem.getUri());
                    }
                }
            }
            List<DmContentItemTO> childContentItemTOs = contentItem.getChildren();
            if (childContentItemTOs != null && !childContentItemTOs.isEmpty()) {
                addMandatoryParentToList(site, path, dummyDependencyTO, childContentItemTOs);
            }
        }
    }

    protected void buildContentList(String site, List<NodeRef> changeSet, List<DmContentItemTO> contentList) {
        if (changeSet != null && changeSet.size() > 0) {
            DmContentService dmContentService = getService(DmContentService.class);
            PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
            for (NodeRef node : changeSet) {
                try {
                    DmPathTO path = new DmPathTO(persistenceManagerService.getNodePath(node));
                    String fullPath = dmContentService.getContentFullPath(site, path.getRelativePath());
                    DmContentItemTO item = persistenceManagerService.getContentItem(fullPath);
                    if (item != null)
                        contentList.add(item);
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("error while building content list. Ignoring it...");
                    }
                }
            }
        }
    }

    /**
     * @param site
     * @param node
     * @param packageRef
     */
    protected void updateWorkflowWithRemovedDependenices(String site, NodeRef node, NodeRef packageRef) {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        String nodePath = persistenceManagerService.getNodePath(node);
        String workPath = nodePath;
        Matcher m = DmConstants.DM_REPO_TYPE_PATH_PATTERN.matcher(nodePath);
        if (m.matches() && m.group(3).equalsIgnoreCase(DmConstants.DM_LIVE_REPO_FOLDER)) {
            StringBuilder sb = new StringBuilder(m.group(1));
            sb.append(m.group(2));
            sb.append(DmConstants.DM_WORK_AREA_REPO_FOLDER);
            sb.append(m.group(4));
            workPath = sb.toString();
        }
        DmPathTO path = new DmPathTO(workPath);
        String relativePath = path.getRelativePath();
        FileInfo nodeInfo = persistenceManagerService.getFileInfo(node);

        DmContentService dmContentService = getService(DmContentService.class);
        DmRenameService dmRenameService = getService(DmRenameService.class);
        if (nodeInfo.isFolder()) {
            List<FileInfo> children = persistenceManagerService.list(node);
            for (FileInfo child : children) {
                updateWorkflowWithRemovedDependenices(site, child.getNodeRef(), packageRef);
            }
        } else if (relativePath.endsWith(DmConstants.XML_PATTERN) && (!dmContentService.isNew(site, relativePath) || dmRenameService.isItemRenamed(site, relativePath))) {
            String stagingPath = relativePath;
            boolean isRenamed = dmRenameService.isItemRenamed(site, relativePath);
            if (isRenamed) {
                stagingPath = (String) persistenceManagerService.getProperty(node, CStudioContentModel.PROP_RENAMED_OLD_URL);
            }
            DmDependencyDiffService.DiffRequest diffRequest = new DmDependencyDiffService.DiffRequest(site, relativePath, stagingPath, null, site, true);
            List<String> deleted;
            DmDependencyService dmDependencyService = getService(DmDependencyService.class);
            try {
                deleted = dmDependencyService.getRemovedDependenices(diffRequest, true);
                for (String dependency : deleted) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Removed dependenices submitted to staging " + dependency);
                    }
                    String depFullPath = dmContentService.getContentFullPath(site, dependency);
                    updateWorkFlow(site, depFullPath, packageRef);
                }
            } catch (ServiceException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Unable to retrieve removed dependencies to be added to workflow");
                }
            }
        }
    }

    /**
     * update status to live and send approval notice
     *
     * @param action
     * @param node
     * @param site
     * @param contentList
     * @param sendNotice
     */
    protected void updateStatusToLive(DmStateManager action, NodeRef node, String site, List<DmContentItemTO> contentList, boolean sendNotice) {
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        DmPathTO path = new DmPathTO(persistenceManagerService.getNodePath(node));
        DmContentService dmContentService = getService(DmContentService.class);
        String fullPath = dmContentService.getContentFullPath(site, path.getRelativePath());
        FileInfo nodeInfo = persistenceManagerService.getFileInfo(node);
        if (nodeInfo.isFolder()) {
            List<FileInfo> children = persistenceManagerService.list(node);
            for (FileInfo child : children) {
                updateStatusToLive(action, child.getNodeRef(), site, contentList, sendNotice);
            }
        } else {
            //action.markLive(node, site, fullPath);
            if (sendNotice) {
                try {
                    DmContentItemTO to = persistenceManagerService.getContentItem(fullPath);
                    sendApprovalNotification(site, node, contentList, to);
                } catch (ServiceException e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while sending approval notification on " + fullPath, e);
                    }
                }
            }
        }
    }

    protected void sendApprovalNotification(String site, NodeRef node, List<DmContentItemTO> contentList, DmContentItemTO to) {
        try {
            PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
            NotificationService notificationService = getService(NotificationService.class);
            String nodePath = persistenceManagerService.getNodePath(node);
            if (customContentTypeNotification) {
                Pattern pattern = Pattern.compile(customContentTypeNotificationPattern);
                DmPathTO dmPathTO = new DmPathTO(nodePath);
                Matcher matcher = pattern.matcher(dmPathTO.getRelativePath());
                if (!matcher.matches()) {
                    return;
                }
            } else {
                // Never send approval email notification for assets.
                if (to == null || to.isAsset())
                    return;

                // Logic to check if we need to send approval email notification for component or document
                if (to.isComponent() || to.isDocument()) {
                    String url = to.getUri();
                    for (DmContentItemTO item : contentList) {
                        if (!item.isAsset() && !item.isLevelDescriptor()) {
                            List<DmContentItemTO> referredList = null;

                            if (to.isComponent())
                                referredList = item.getComponents();

                            if (to.isDocument())
                                referredList = item.getDocuments();

                            if (referredList != null) {
                                for (DmContentItemTO referredItem : referredList) {
                                    if (referredItem.getUri().equals(url))
                                        return;
                                }
                            }
                        }
                    }

                }
            }

            // If control reached here, then we should send email if sendmail flag is true

            if (node != null) {

                Serializable sendEmailValue = persistenceManagerService.getProperty(node, CStudioContentModel.PROP_WEB_WF_SEND_EMAIL);
                boolean sendEmail = (sendEmailValue != null) && (Boolean) sendEmailValue;
                if (sendEmail) {
                    Serializable submittedByValue = persistenceManagerService.getProperty(node, CStudioContentModel.PROP_WEB_WF_SUBMITTED_BY);
                    String submittedBy = "";
                    if (submittedByValue != null) {
                        submittedBy = (String) submittedByValue;
                    } else {
                        if (logger.isErrorEnabled()) {
                            logger.error("did not send approval notification as submitted by property is null");
                        }
                        return;
                    }
                    DmPathTO path = new DmPathTO(nodePath);
                    String approver = (String) persistenceManagerService.getProperty(node, CStudioContentModel.PROP_WEB_APPROVED_BY);
                    notificationService.sendApprovalNotification(site, submittedBy, path.getRelativePath(), approver);
                    /*
                    * Remove this sendmail property as we are done sending email
                    */
                    persistenceManagerService.removeProperty(node, CStudioContentModel.PROP_WEB_WF_SEND_EMAIL);

                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("did not send approval notification as sendemail flag is false");
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not send approval notification as node is null");
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not send approval notification", e);
            }
        }
    }

    /**
     * Hook to JPM - called after Post staging submission
     *
     * @param packageRef
     * @param workflowId
     * @param desc
     */
    @Override
    public void postSubmission(NodeRef packageRef, String workflowId, String desc) {


        if (logger.isDebugEnabled()) {
            logger.debug("Starting postStagingSubmission");
        }
        try {
            //StoreRef storeRef = packageRef.getStoreRef();
            //String store = storeRef.getIdentifier();
            //String[] tokens = store.split("--");
            //String site = tokens[0];
            String site = "";
            //String sandbox = _servicesConfig.getSandbox(site);
            //if the workflow is Rename Workflow do special processing
            DmRenameService dmRenameService = getService(DmRenameService.class);
            try {
                if (DmUtils.isRenameWorkflow(desc)) {
                    dmRenameService.postSubmission(site, desc);
                }
                WorkflowProgress.endWorkFlow(desc);
            } finally {
                //_workflowProcessor.unlockWorkflowBatch(site);
            }

        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Runtime exception caught during postStagingSubmission  ", e);
            }
        }
    }

    /*
    * (non-Javadoc)
    * @see org.craftercms.cstudio.alfresco.dm.service.api.DmWorkflowService#prePublish(org.alfresco.service.cmr.repository.NodeRef)
    */
    public void prePublish(NodeRef eventNodeRef) {
        if (logger.isDebugEnabled()) {
            logger.debug("Staring pre-publish");
        }
        long start = System.currentTimeMillis();
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        Collection<String> nodesToPublish = (Collection<String>) persistenceManagerService.getProperty(eventNodeRef, PublishingModel.PROP_PUBLISHING_EVENT_NODES_TO_PUBLISH);
        List<NodeRef> nodeRefs = new FastList<NodeRef>();
        Serializable scheduledTimeVal = persistenceManagerService.getProperty(eventNodeRef, PublishingModel.PROP_PUBLISHING_EVENT_TIME);
        Date scheduledTime = (scheduledTimeVal == null) ? null : (Date) scheduledTimeVal;
        try {

            String site = null;

            if (logger.isDebugEnabled()) {
                logger.debug("preparing publishing");
            }

            // TODO: send email to individual owner
            if (scheduledTime != null) {
                if (nodesToPublish != null && nodesToPublish.size() > 0) {
                    for (String nodeRef : nodesToPublish) {
                        NodeRef workFlowNode = new NodeRef(nodeRef);
                        nodeRefs.add(workFlowNode);
                        String fullPath = persistenceManagerService.getNodePath(workFlowNode);
                        Matcher m = DmConstants.DM_REPO_TYPE_PATH_PATTERN.matcher(fullPath);
                        if (m.matches()) {
                            site = m.group(2);
                            if (site.endsWith("/")) site = site.substring(0, site.length() - 1);
                            fullPath = m.group(1) + m.group(2) + DmConstants.DM_WORK_AREA_REPO_FOLDER + m.group(4);
                            workFlowNode = persistenceManagerService.getNodeRef(fullPath);
                        } else {
                            site = DmUtils.getSiteFromFullPath(fullPath);
                        }
                        if (workFlowNode != null) {
                            updateWorkflowPathsWithDependenices(site, eventNodeRef, workFlowNode);
                        }
                    }
                }
            }

            //note- delete case returns changeSet empty
            ArrayList<DmContentItemTO> contentList = new ArrayList();
            /*
            if (sendNotice) {
                buildContentList(site, changeSet, contentList);
            }
            */
            //List<NodeRef> nodes = (List<NodeRef>) persistenceManagerService.getProperty(eventNodeRef, PublishingModel.PROP_PUBLISHING_EVENT_NODES_TO_PUBLISH);
            buildContentList(site, nodeRefs, contentList);
            if (nodesToPublish != null && nodesToPublish.size() > 0) {
                for (NodeRef nodeRef : nodeRefs) {
                    //NodeRef workFlowNode = new NodeRef(nodeRef);
                    String fullPath = persistenceManagerService.getNodePath(nodeRef);
                    Matcher m = DmConstants.DM_REPO_TYPE_PATH_PATTERN.matcher(fullPath);
                    if (m.matches()) {
                        site = m.group(2);
                        if (site.endsWith("/")) site = site.substring(0, site.length() - 1);
                        fullPath = m.group(1) + m.group(2) + DmConstants.DM_WORK_AREA_REPO_FOLDER + m.group(4);
                        //node = persistenceManagerService.getNodeRef(fullPath);
                    } else {
                        site = DmUtils.getSiteFromFullPath(fullPath);
                    }
                    DmPathTO dmPathTO = new DmPathTO(fullPath);
                    //site = DmUtils.getSiteFromFullPath(fullPath);
                    updateWorkflowWithRemovedDependenices(site, nodeRef, eventNodeRef);
                    try {
                        DmContentItemTO item = persistenceManagerService.getContentItem(fullPath);
                        sendApprovalNotification(site, nodeRef, contentList, item);
                    } catch (ServiceException e) {
                        if (logger.isErrorEnabled()) {
                            logger.error("Error while sending notification", e);
                        }
                    }
                    Map<QName, Serializable> nodeProps = persistenceManagerService.getProperties(nodeRef);
                    for (QName propName : DmConstants.SUBMITTED_PROPERTIES) {
                        nodeProps.remove(propName);
                    }
                    persistenceManagerService.setProperties(nodeRef, nodeProps);
                    //updateStatusToLive(action, workFlowNode, site, contentList, sendNotice);
                    //updateStatusToLive(action, workFlowNode, site, contentList, false);
                    //_dmVersionService.createNextMajorVersion(site, dmPathTO.getRelativePath());
                }
            }
        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not submit to staging", e);
            }
        }
        if (logger.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            logger.debug("prepareStagingSubmission = " + (end - start));
        }
    }

    @Override
    public void postPublish(NodeRef eventNodeRef) {
        if (logger.isDebugEnabled()) {
            logger.debug("Starting post publish");
        }
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        try {
            //StoreRef storeRef = packageRef.getStoreRef();
            //String store = storeRef.getIdentifier();
            //String[] tokens = store.split("--");
            //String site = tokens[0];
            PublishingEvent event = _publishingEventHelper.getPublishingEvent(eventNodeRef);
            Set<NodeRef> changeSet = event.getPackage().getNodesToPublish();
            String site = "";
            if (changeSet != null && changeSet.size() > 0) {
                NodeRef workFlowNode = changeSet.iterator().next();
                String fullPath = persistenceManagerService.getNodePath(workFlowNode);
                DmPathTO dmPathTO = new DmPathTO(fullPath);
                site = DmUtils.getSiteFromFullPath(fullPath);
            }
            //String sandbox = _servicesConfig.getSandbox(site);
            //if the workflow is Rename Workflow do special processing
            //try {
            //    if (DmUtils.isRenameWorkflow(desc)) {
            //        _dmRenameService.postSubmission(site, desc);
            //    }
            //WorkflowProgress.endWorkFlow(desc);
            //} finally {
            //_workflowProcessor.unlockWorkflowBatch(site);
            //}

        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Runtime exception caught during postStagingSubmission  ", e);
            }
        }
    }

    public void scheduleDeleteSubmission(NodeRef packageRef, String workflowId, String description) {              //$Review$ mark submit delete

        if (logger.isDebugEnabled()) {
            logger.debug("DmSimpleWorkflowServiceImpl.scheduleDeleteSubmission");
        }
        long start = System.currentTimeMillis();
        SearchService searchService = getService(SearchService.class);
        try {
            StoreRef storeRef = packageRef.getStoreRef();
            String store = storeRef.getIdentifier();
            String[] tokens = store.split("--");
            String site = tokens[0];
            String workflowSandbox = WCMUtil.getWorkflowId(storeRef.getIdentifier());
            if (logger.isDebugEnabled()) {
                logger.debug("preparing staging submission for " + workflowSandbox + " site: " + site + ", store: " + store);
            }
            ScheduleItem scheduleItem = new ScheduleItem();

            // TODO: send email to individual owner
            List<NodeRef> changeSet = searchService.findNodes(CStudioConstants.STORE_REF, getListChangedQuery(site));
            List<String> itemsToDel = new FastList<String>();
            PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
            if (changeSet != null && changeSet.size() > 0) {
                for (NodeRef asset : changeSet) {
                    String path = persistenceManagerService.getNodePath(asset);
                    if (path.endsWith("/" + DmConstants.INDEX_FILE)) {
                        path = DmUtils.getParentUrl(path);
                    }
                    itemsToDel.add(path);
                }
            }
            if (!itemsToDel.isEmpty()) {
                scheduleItem.setPaths(itemsToDel);
                scheduleItem.setSite(site);
                long now = System.currentTimeMillis();
                //give some time to finish work flow
                long pickUpTime = now + 40 * 1000;
                scheduleItem.setPickUpTime(pickUpTime);
                addScheduleItem(scheduleItem);
            }
        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not submit to staging", e);
            }
        }
        if (logger.isDebugEnabled()) {
            long end = System.currentTimeMillis();
            logger.debug("scheduleDeleteSubmission = " + (end - start));
        }
    }

    protected synchronized void addScheduleItem(ScheduleItem item) {
        if (!isScheduleDeleteHandlerThrStarted) {
            isScheduleDeleteHandlerThrStarted = true;
            Thread thread = new Thread(_scheduleDeleteHandler);
            thread.start();
        }
        _scheduleDeleteHandler.addToQueue(item);
    }

    @Override
    public void preSchedule(Set<String> uris, final Date date, final GoLiveContext context, Set<String> rescheduledUris) {
        preGoLive(uris, context, rescheduledUris);
        DmContentService dmContentService = getService(DmContentService.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        for (String path : uris) {
            String fullPath = dmContentService.getContentFullPath(context.getSite(), path);
            NodeRef node = persistenceManagerService.getNodeRef(fullPath);
            if (node != null) {
                //dmStateManager.markScheduled(node, date, context.getSite());
                Map<QName, Serializable> nodeProperties = persistenceManagerService.getProperties(node);
                nodeProperties.put(WCMWorkflowModel.PROP_LAUNCH_DATE, date);
                persistenceManagerService.setProperties(node, nodeProperties);
            }
        }
    }

    @Override
    public void preGoLive(Set<String> uris, GoLiveContext context, Set<String> rescheduledUris) {
        String approver = context.getApprover();
        String site = context.getSite();
        DmContentService dmContentService = getService(DmContentService.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);

        for (String uri : uris) {
            if (dmContentService.matchesDisplayPattern(site, uri) || customContentTypeNotification) {
                String path = dmContentService.getContentFullPath(site, uri);
                final NodeRef node = persistenceManagerService.getNodeRef(path);
                if (node != null && StringUtils.isNotEmpty(approver)) {
                    persistenceManagerService.disableBehaviour(node, ContentModel.ASPECT_LOCKABLE);
                    persistenceManagerService.setProperty(node, CStudioContentModel.PROP_WEB_APPROVED_BY, approver);
                    persistenceManagerService.enableBehaviour(node, ContentModel.ASPECT_LOCKABLE);
                }
            }
        }
    }

    @Override
    public void updateItemStatus(NodeRef packageRef, String status, Date date) {
        try {
            super.updateItemStatus(packageRef, status, date);

        } finally {
            StoreRef storeRef = packageRef.getStoreRef();
            String store = storeRef.getIdentifier();
            String[] tokens = store.split("--");
            String site = tokens[0];
        }

    }

    public static class SubmitPackage {
        protected String pathPrefix;
        protected Set<String> paths = new HashSet<String>();
        protected Set<DmDependencyTO> items = new HashSet<DmDependencyTO>();
        protected Set<String> uris = new HashSet<String>();

        protected StringBuilder builder = new StringBuilder();

        public SubmitPackage(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public void addToPackage(String relativePath) {
            paths.add(pathPrefix + relativePath);
            builder.append(relativePath).append(", ");
            uris.add(relativePath);
        }

        public void addToPackage(DmDependencyTO item) {
            paths.add(pathPrefix + item.getUri());
            builder.append(item).append(", ");
            items.add(item);
            uris.add(item.getUri());
        }

        public Set<String> getUris() {
            return uris;
        }

        public List<String> getPaths() {
            return new ArrayList<String>(paths);
        }

        public Set<DmDependencyTO> getItems() {
            return items;
        }

        public String getLabel() {
            String label = builder.toString();
            if (label.length() > 255) {
                label = label.substring(0, 252) + "..";
            }
            return label;
        }


    }

    @Override
    public List<DmContentItemTO> getScheduledItemsDeploymentEngine(String site, String sub, DmContentItemComparator comparator, DmContentItemComparator subComparator, String filterType) {
        ServicesConfig servicesConfig = getService(ServicesConfig.class);
        PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
        List<DmContentItemTO> results = new FastArrayList();
        List<String> displayPatterns = servicesConfig.getDisplayInWidgetPathPatterns(site);
        List<CopyToEnvironmentItem> deploying = findScheduledItems(site);
        SimpleDateFormat format = new SimpleDateFormat(CStudioConstants.DATE_FORMAT_SCHEDULED);
        List<DmContentItemTO> scheduledItems = new ArrayList<DmContentItemTO>();
        for (CopyToEnvironmentItem deploymentItem : deploying) {
            String fullPath = servicesConfig.getRepositoryRootPath(deploymentItem.getSite()) + deploymentItem.getPath();
            NodeRef noderef = persistenceManagerService.getNodeRef(fullPath);
            addScheduledItem(site, deploymentItem.getScheduledDate(), format, noderef, results, comparator, subComparator, null, displayPatterns, filterType, null);
        }
        return results;
    }

    protected DmContentItemTO getDmContentItemTO(DeploymentItemDAO deploymentItemDAO) {
        try {
            ServicesConfig servicesConfig = getService(ServicesConfig.class);
            PersistenceManagerService persistenceManagerService = getService(PersistenceManagerService.class);
            String fullPath = servicesConfig.getRepositoryRootPath(deploymentItemDAO.getSite()) + deploymentItemDAO.getPath();
            return persistenceManagerService.getContentItem(fullPath);
        } catch (ContentNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error in retrieving : " + deploymentItemDAO.getPath() + " in " + deploymentItemDAO.getSite() + ".", e);
            }
            return null;
        }
    }

    private List<CopyToEnvironmentItem> findScheduledItems(String site) {
        return this._deploymentService.getScheduledItems(site);
    }
}
