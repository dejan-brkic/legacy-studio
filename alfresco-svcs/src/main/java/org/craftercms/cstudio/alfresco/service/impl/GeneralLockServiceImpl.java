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
package org.craftercms.cstudio.alfresco.service.impl;

import javolution.util.FastMap;
import org.craftercms.cstudio.alfresco.service.AbstractRegistrableService;
import org.craftercms.cstudio.alfresco.service.api.GeneralLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class GeneralLockServiceImpl extends AbstractRegistrableService implements GeneralLockService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralLockServiceImpl.class);

    protected Map<String, ReentrantLock> nodeLocks = new FastMap<String, ReentrantLock>();

    @Override
    public void register() {
        getServicesManager().registerService(GeneralLockService.class, this);
    }

    @Override
    public void lock(String objectId) {
        ReentrantLock nodeLock;
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Obtaining lock for id " + objectId);
        }
        synchronized (this) {
            if (nodeLocks.containsKey(objectId)) {
                nodeLock = nodeLocks.get(objectId);
            } else {
                nodeLock = new ReentrantLock();
                nodeLocks.put(objectId, nodeLock);
            }
        }
        nodeLock.lock();
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Locked all threads for id " + objectId);
        }
    }

    @Override
    public boolean tryLock(String objectId) {
        ReentrantLock nodeLock;
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Trying to get lock for id " + objectId);
        }
        synchronized (this) {
            if (nodeLocks.containsKey(objectId)) {
                nodeLock = nodeLocks.get(objectId);
            } else {
                nodeLock = new ReentrantLock();
                nodeLocks.put(objectId, nodeLock);
            }
        }
        boolean toRet = nodeLock.tryLock();
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Result for tryLock on id " + objectId + " : " + toRet);
        }
        return toRet;
    }

    @Override
    public void unlock(String objectId) {
        ReentrantLock nodeLock = null;
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Unlocking id " + objectId);
        }
        synchronized (this) {
            nodeLock = nodeLocks.get(objectId);
        }
        if (nodeLock != null) {
            nodeLock.unlock();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[" + Thread.currentThread().getName() + "]" + " Finished unlocking id " + objectId);
        }
    }
}
