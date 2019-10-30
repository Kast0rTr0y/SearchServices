/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.solr.tracker;

import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.IndexTrackingShutdownException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that provides common {@link Tracker} behaviour.
 * 
 * @author Matt Ward
 */
public abstract class AbstractTracker implements Tracker
{
    static final long TIME_STEP_32_DAYS_IN_MS = 1000 * 60 * 60 * 24 * 32L;
    static final long TIME_STEP_1_HR_IN_MS = 60 * 60 * 1000L;
    static final String SHARD_METHOD_DBID = "DB_ID";

    protected final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    protected Properties props;    
    protected SOLRAPIClient client;
    InformationServer infoSrv;
    protected String coreName;
    protected StoreRef storeRef;
    protected long batchCount;
    protected String alfrescoVersion;
    protected TrackerStats trackerStats;
    protected boolean runPostModelLoadInit = true;
    private int maxLiveSearchers;
    private volatile boolean shutdown = false;

    private Semaphore runLock = new Semaphore(1, true);
    private Semaphore writeLock = new Semaphore(1, true);

    protected volatile TrackerState state;
    protected int shardCount;
    protected int shardInstance;
    protected String shardMethod;
    protected boolean transformContent;
    protected String shardTemplate;
    protected volatile boolean rollback;
    protected final Type type;

    /*
     * A thread handler can be used by subclasses, but they have to intentionally instantiate it.
     */
    ThreadHandler threadHandler;

    /**
     * Default constructor, strictly for testing.
     */
    protected AbstractTracker(Type type)
    {
        this.type = type;
    }
    
    protected AbstractTracker(Properties p, SOLRAPIClient client, String coreName, InformationServer informationServer,Type type)
    {
        this.props = p;
        this.client = client;
        this.coreName = coreName;
        this.infoSrv = informationServer;

        storeRef = new StoreRef(p.getProperty("alfresco.stores", "workspace://SpacesStore"));
        batchCount = Integer.parseInt(p.getProperty("alfresco.batch.count", "5000"));
        maxLiveSearchers =  Integer.parseInt(p.getProperty("alfresco.maxLiveSearchers", "2"));
        
        shardCount =  Integer.parseInt(p.getProperty("shard.count", "1"));
        shardInstance =  Integer.parseInt(p.getProperty("shard.instance", "0"));
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);

        shardTemplate =  p.getProperty("alfresco.template", "");
        
        transformContent = Boolean.parseBoolean(p.getProperty("alfresco.index.transformContent", "true"));

        this.trackerStats = this.infoSrv.getTrackerStats();

        alfrescoVersion = p.getProperty("alfresco.version", "5.0.0");
        
        this.type = type;
        
        LOGGER.info("Solr built for Alfresco version: " + alfrescoVersion);
    }

    
    /**
     * Subclasses must implement behaviour that completes the following steps, in order:
     * <ol>
     *     <li>Purge</li>
     *     <li>Reindex</li>
     *     <li>Index</li>
     *     <li>Track repository</li>
     * </ol>
     * @throws Throwable
     */
    protected abstract void doTrack() throws Throwable;


    private boolean assertTrackerStateRemainsNull() {

        /*
        * This assertion is added to accommodate DistributedAlfrescoSolrTrackerRaceTest.
        * The sleep is needed to allow the test case to add a txn into the queue before
        * the tracker makes its call to pull transactions from the test repo client.
        */

        try
        {
            Thread.sleep(5000);
        }
        catch(Exception e)
        {

        }


        /*
        *  This ensures that getTrackerState does not have the side effect of setting the
        *  state instance variable. This allows classes outside of the tracker framework
        *  to safely call getTrackerState without interfering with the trackers design.
        */

        getTrackerState();


        if(state == null) {
            return true;
        } else {
            return false;
        }

    }
    /**
     * Template method - subclasses must implement the {@link Tracker}-specific indexing
     * by implementing the abstract method {@link #doTrack()}.
     */
    @Override
    public void track()
    {
        if(runLock.availablePermits() == 0) {
            LOGGER.info("... " + this.getClass().getSimpleName() + " for core [" + coreName + "] is already in use "+ this.getClass());
            return;
        }

        try
        {
            /*
            * The runLock ensures that for each tracker type (metadata, content, commit, cascade) only one tracker will
            * be running at a time.
            */

            runLock.acquire();

            if(state==null && Boolean.parseBoolean(System.getProperty("alfresco.test", "false")))
            {
                assert(assertTrackerStateRemainsNull());
            }

            LOGGER.info("... Running " + this.getClass().getSimpleName() + " for core [" + coreName + "].");
            
            if(this.state == null)
            {
                /*
                * Set the global state for the tracker here.
                */
                this.state = getTrackerState();
                LOGGER.debug("##### Setting tracker global state.");
                LOGGER.debug("State set: " + this.state.toString());
                this.state.setRunning(true);
            }
            else
            {
                continueState();
                this.state.setRunning(true);
            }

            infoSrv.registerTrackerThread();

            try
            {
                doTrack();
            }
            catch(IndexTrackingShutdownException t)
            {
                setRollback(true);
                LOGGER.info("Stopping index tracking for " + getClass().getSimpleName() + " - " + coreName);
            }
            catch(Throwable t)
            {
                setRollback(true);
                if (t instanceof SocketTimeoutException || t instanceof ConnectException)
                {
                    if (LOGGER.isDebugEnabled())
                    {
                        // DEBUG, so give the whole stack trace
                        LOGGER.warn("Tracking communication timed out for " + getClass().getSimpleName() + " - " + coreName, t);
                    }
                    else
                    {
                        // We don't need the stack trace.  It timed out.
                        LOGGER.warn("Tracking communication timed out for " + getClass().getSimpleName() + " - " + coreName);
                    }
                }
                else
                {
                    LOGGER.error("Tracking failed for " + getClass().getSimpleName() + " - " + coreName, t);
                }
            }
        }
        catch (InterruptedException e)
        {
            LOGGER.error("Semaphore interrupted for " + getClass().getSimpleName() + " - " + coreName, e);
        }
        finally
        {
            infoSrv.unregisterTrackerThread();
            if(state != null) {
                //During a rollback state is set to null.
                state.setRunning(false);
                state.setCheck(false);
            }
            runLock.release();
        }
    }

    public boolean getRollback() {
        return this.rollback;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    private void continueState() {
        infoSrv.continueState(state);
        state.incrementTrackerCycles();
    }

    public synchronized void invalidateState()
    {
        state = null;
    }
    
    @Override
    public synchronized TrackerState getTrackerState()
    {
        if(this.state != null)
        {
           return this.state;
        }
        else
        {
            return this.infoSrv.getTrackerInitialState();
        }
    }
    
    

    /**
     * Allows time for the scheduled asynchronous tasks to complete
     */
    protected synchronized void waitForAsynchronous()
    {
        AbstractWorkerRunnable currentRunnable = this.threadHandler.peekHeadReindexWorker();
        while (currentRunnable != null)
        {
            checkShutdown();
            synchronized (this)
            {
                try
                {
                    wait(100);
                }
                catch (InterruptedException e)
                {
                }
            }
            currentRunnable = this.threadHandler.peekHeadReindexWorker();
        }
    }

    public int getMaxLiveSearchers()
    {
        return maxLiveSearchers;
    }

    protected void checkShutdown()
    {
        if(shutdown)
        {
            throw new IndexTrackingShutdownException();
        }
    }

    @Override
    public boolean isAlreadyInShutDownMode()
    {
        return shutdown;
    }

    @Override
    public void setShutdown(boolean shutdown)
    {
        this.shutdown = shutdown;
    }

    @Override
    public void shutdown()
    {
        setShutdown(true);
        if(this.threadHandler != null)
        {
            threadHandler.shutDownThreadPool();
        }
    }

    public Semaphore getWriteLock() {
        return this.writeLock;
    }

    public Semaphore getRunLock() {
        return this.runLock;
    }


    /**
     * @return Alfresco version Solr was built for
     */
    @Override
    public String getAlfrescoVersion()
    {
        return alfrescoVersion;
    }

    public Properties getProps()
    {
        return props;
    }

    public Type getType()
    {
        return type;
    }
    
    
}
