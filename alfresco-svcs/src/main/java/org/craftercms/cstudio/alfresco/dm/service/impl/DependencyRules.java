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

import org.craftercms.cstudio.alfresco.dm.to.DmDependencyTO;
import org.craftercms.cstudio.alfresco.service.ServicesManager;
import org.craftercms.cstudio.alfresco.service.api.PersistenceManagerService;
import org.craftercms.cstudio.alfresco.service.api.ServicesConfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DependencyRules {

    protected String _site;

    protected ServicesManager servicesManager;

    public DependencyRules(String site, ServicesManager servicesManager) {
        this._site = site;
        this.servicesManager = servicesManager;
    }

    public Set<DmDependencyTO> applySubmitRule(DmDependencyTO submittedItem){
        ServicesConfig servicesConfig = servicesManager.getService(ServicesConfig.class);
        String siteRoot = servicesConfig.getRepositoryRootPath(_site);
        PersistenceManagerService persistenceManagerService = servicesManager.getService(PersistenceManagerService.class);
        Set<DmDependencyTO> dependencies = new HashSet<DmDependencyTO>();
        if (submittedItem.getDocuments() != null) {
            for (DmDependencyTO document : submittedItem.getDocuments()) {
                String fullPath = siteRoot + document.getUri();
                if (persistenceManagerService.isUpdatedOrNew(fullPath)) {
                    document.setNow(submittedItem.isNow());
                    document.setScheduledDate(submittedItem.getScheduledDate());
                    document.setSubmitted(true);
                    dependencies.add(document);
                }
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(document);
                dependencies.addAll(dependencyTOSet);
            }
        }
        // get components
        if (submittedItem.getComponents() != null) {
            for (DmDependencyTO component : submittedItem.getComponents()) {
                String fullPath = siteRoot + component.getUri();
                if (persistenceManagerService.isUpdatedOrNew(fullPath)) {
                    component.setNow(submittedItem.isNow());
                    component.setScheduledDate(submittedItem.getScheduledDate());
                    component.setSubmitted(true);
                    dependencies.add(component);

                }
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(component);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get assets
        if (submittedItem.getAssets() != null) {
            for (DmDependencyTO asset : submittedItem.getAssets()) {
                String fullPath = siteRoot + asset.getUri();
                if (persistenceManagerService.isUpdatedOrNew(fullPath)) {
                    dependencies.add(asset);
                    asset.setNow(submittedItem.isNow());
                    asset.setScheduledDate(submittedItem.getScheduledDate());
                    asset.setSubmitted(true);

                }
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(asset);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get templates
        if (submittedItem.getRenderingTemplates() != null) {
            for (DmDependencyTO template : submittedItem.getRenderingTemplates()) {
                String fullPath = siteRoot + template.getUri();
                if (persistenceManagerService.isUpdatedOrNew(fullPath)) {
                    dependencies.add(template);
                    template.setNow(submittedItem.isNow());
                    template.setScheduledDate(submittedItem.getScheduledDate());
                    template.setSubmitted(true);

                }
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(template);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get level descriptors
        if (submittedItem.getLevelDescriptors() != null) {
            for (DmDependencyTO ld : submittedItem.getLevelDescriptors()) {
                String fullPath = siteRoot + ld.getUri();
                if (persistenceManagerService.isUpdatedOrNew(fullPath)) {
                    dependencies.add(ld);
                    ld.setNow(submittedItem.isNow());
                    ld.setScheduledDate(submittedItem.getScheduledDate());
                    ld.setSubmitted(true);
                }
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(ld);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get pages
        if (submittedItem.getPages() != null) {
            for (DmDependencyTO page : submittedItem.getPages()) {
                String fullPath = siteRoot + page.getUri();
                if (persistenceManagerService.isNew(fullPath)) {
                    page.setNow(submittedItem.isNow());
                    page.setScheduledDate(submittedItem.getScheduledDate());
                    page.setSubmitted(true);
                    dependencies.add(page);

                }
                Set<DmDependencyTO> childPages = applySubmitRule(page);
                dependencies.addAll(childPages);
            }
        }
        return dependencies;
    }

    public Set<DmDependencyTO> applyDeleteDependencyRule(DmDependencyTO deletedItem) {
        return Collections.emptySet();//$Review$ pick up the cascades from configuration
    }

    public Set<DmDependencyTO> applyRejectRule(DmDependencyTO submittedItem){
        Set<DmDependencyTO> dependencies = new HashSet<DmDependencyTO>();
        if(submittedItem.isSubmittedForDeletion()) {
            return applyDeleteDependencyRule(submittedItem);
        }

        if (submittedItem.getDocuments() != null) {
            for (DmDependencyTO document : submittedItem.getDocuments()) {
                dependencies.add(document);
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(document);
                dependencies.addAll(dependencyTOSet);
            }
        }
        // get components
        if (submittedItem.getComponents() != null) {
            for (DmDependencyTO component : submittedItem.getComponents()) {
                dependencies.add(component);
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(component);
                dependencies.addAll(dependencyTOSet);
            }
        }
        // get components
        if (submittedItem.getAssets() != null) {
            for (DmDependencyTO asset : submittedItem.getAssets()) {
                dependencies.add(asset);
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(asset);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get templates
        if (submittedItem.getRenderingTemplates() != null) {
            for (DmDependencyTO template : submittedItem.getRenderingTemplates()) {
                dependencies.add(template);
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(template);
                dependencies.addAll(dependencyTOSet);
            }
        }

        // get level descriptors
        if (submittedItem.getLevelDescriptors() != null) {
            for (DmDependencyTO ld : submittedItem.getLevelDescriptors()) {
                dependencies.add(ld);
                Set<DmDependencyTO> dependencyTOSet = applySubmitRule(ld);
                dependencies.addAll(dependencyTOSet);
            }
        }

        return dependencies;
    }
}
