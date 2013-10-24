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

import org.craftercms.cstudio.api.log.Logger;
import org.craftercms.cstudio.api.log.LoggerFactory;
import org.craftercms.cstudio.api.repository.ContentRepository;
import org.craftercms.cstudio.api.service.deployment.CopyToEnvironmentItem;
import org.craftercms.cstudio.api.service.deployment.DeploymentException;
import org.craftercms.cstudio.api.service.deployment.DeploymentService;
import org.craftercms.cstudio.api.service.deployment.DeploymentSyncHistoryItem;
import org.craftercms.cstudio.api.service.fsm.TransitionEvent;
import org.craftercms.cstudio.impl.service.deployment.dal.DeploymentDAL;
import org.craftercms.cstudio.impl.service.deployment.dal.DeploymentDALException;
import org.craftercms.cstudio.impl.service.deployment.job.DeployContentToEnvironmentStore;
import org.craftercms.cstudio.impl.service.deployment.job.PublishContentToDeploymentTarget;

import java.util.*;

/**
 */
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    public void deploy(String site, String environment, List<String> paths, Date scheduledDate, String approver, String submissionComment) throws DeploymentException {

        if (scheduledDate != null && scheduledDate.after(new Date())) {
            _contentRepository.stateTransition(site, paths, TransitionEvent.SCHEDULED_DEPLOYMENT);
            _contentRepository.setSystemProcessing(site, paths, false);
        } else {
            _contentRepository.setSystemProcessing(site, paths, true);
        }

        List<String> newPaths = new ArrayList<String>();
        List<String> updatedPaths = new ArrayList<String>();
        List<String> movedPaths = new ArrayList<String>();

        Map<CopyToEnvironmentItem.Action, List<String>> groupedPaths = new HashMap<CopyToEnvironmentItem.Action, List<String>>();

        for (String p : paths) {
            if (_contentRepository.isNew(site, p)) {
                newPaths.add(p);
            } else if (_contentRepository.isRenamed(site, p)) {
                movedPaths.add(p);
            } else {
                updatedPaths.add(p);
            }
        }

        groupedPaths.put(CopyToEnvironmentItem.Action.NEW, newPaths);
        groupedPaths.put(CopyToEnvironmentItem.Action.MOVE, movedPaths);
        groupedPaths.put(CopyToEnvironmentItem.Action.UPDATE, updatedPaths);

        // use dal to setup deploy to environment log
        if (!_contentRepository.environmentRepoExists(site, environment)) {
            _contentRepository.createEnvironmentRepo(site, environment);
        }
        _deploymentDAL.setupItemsToDeploy(site, environment, groupedPaths, scheduledDate, approver, submissionComment);
    }

    @Override
    public void delete(String site, List<String> paths, String approver, Date scheduledDate) throws DeploymentException {
        if (scheduledDate != null && scheduledDate.after(new Date())) {
            _contentRepository.stateTransition(site, paths, TransitionEvent.DELETE);
            _contentRepository.setSystemProcessing(site, paths, false);
        } else {
            _contentRepository.setSystemProcessing(site, paths, true);
        }
        Set<String> environments = _contentRepository.getAllPublishingEnvironments(site);
        for (String environment : environments) {
            _deploymentDAL.setupItemsToDelete(site, environment, paths, approver, scheduledDate);
        }
    }

    @Override
    public void deleteDeploymentDataForSite(final String site) {
        try {
            signalWorkersToStop();
            _deploymentDAL.deleteDeploymentDataForSite(site);
        } catch (DeploymentDALException e) {
            logger.error("Error while deleting deployment data for site {0}", e, site);
        } finally {
            signalWorkersToContinue();
        }
    }

    private void signalWorkersToContinue() {
        DeployContentToEnvironmentStore.signalToStop(false);
        PublishContentToDeploymentTarget.signalToStop(false);
    }

    private void signalWorkersToStop() {
        DeployContentToEnvironmentStore.signalToStop(true);
        PublishContentToDeploymentTarget.signalToStop(true);
        while (DeployContentToEnvironmentStore.isRunning() && PublishContentToDeploymentTarget.isRunning()) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                logger.info("Interrupted while waiting to stop workers", e);
            }
        }
    }

    @Override
    public List<DeploymentSyncHistoryItem> getDeploymentHistory(String site, Date fromDate, Date toDate, String filterType, int numberOfItems) {
        return _deploymentDAL.getDeploymentHistory(site, fromDate, toDate, filterType, numberOfItems);
    }

    @Override
    public List<CopyToEnvironmentItem> getScheduledItems(String site) {
        return _deploymentDAL.getScheduledItems(site);
    }

    @Override
    public void cancelWorkflow(String site, String path) throws DeploymentException {
        _deploymentDAL.cancelWorkflow(site, path);
    }

    public void setDeploymentDAL(DeploymentDAL deploymentDAL) {
        this._deploymentDAL = deploymentDAL;
    }

    public void setContentRepository(ContentRepository contentRepository) {
        this._contentRepository = contentRepository;
    }

    protected DeploymentDAL _deploymentDAL;
    protected ContentRepository _contentRepository;
}
