/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.sirona.tracking;


import org.apache.sirona.configuration.Configuration;
import org.apache.sirona.configuration.ioc.Destroying;
import org.apache.sirona.configuration.ioc.IoCs;
import org.apache.sirona.store.DataStoreFactory;
import org.apache.sirona.store.tracking.PathTrackingDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains logic to track class#method invocation path
 */
public class PathTracker
{
    private static final String NODE =
        Configuration.getProperty( Configuration.CONFIG_PROPERTY_PREFIX + "javaagent.path.tracking.marker", //
                                   Configuration.getProperty( "org.apache.sirona.cube.CubeBuilder.marker", "node" ) );

    private static final PathTrackingDataStore PATH_TRACKING_DATA_STORE =
        IoCs.findOrCreateInstance( DataStoreFactory.class ).getPathTrackingDataStore();

    private static final ThreadLocal<Context> THREAD_LOCAL = new ThreadLocal<Context>()
    {
        @Override
        protected Context initialValue()
        {
            return new Context();
        }
    };

    private final PathTrackingInformation pathTrackingInformation;

    private static final boolean USE_EXECUTORS = Boolean.parseBoolean(
        Configuration.getProperty( Configuration.CONFIG_PROPERTY_PREFIX + "pathtracking.useexecutors", "false" ) );

    private static boolean USE_SINGLE_STORE = Boolean.parseBoolean(
        Configuration.getProperty( Configuration.CONFIG_PROPERTY_PREFIX + "pathtracking.singlestore", "false" ) );

    protected static ExecutorService EXECUTORSERVICE;

    static
    {

        if ( USE_EXECUTORS )
        {
            int threadsNumber =
                Configuration.getInteger( Configuration.CONFIG_PROPERTY_PREFIX + "pathtracking.executors", 5 );
            EXECUTORSERVICE = Executors.newFixedThreadPool( threadsNumber );
        }
    }

    private PathTracker( final PathTrackingInformation pathTrackingInformation )
    {
        this.pathTrackingInformation = pathTrackingInformation;
    }


    private static void cleanUp()
    {
        THREAD_LOCAL.remove();
    }


    // An other solution could be using Thread.currentThread().getStackTrace() <- very slow

    public static PathTracker start( PathTrackingInformation pathTrackingInformation )
    {

        final Context context = THREAD_LOCAL.get();

        final int level;
        final PathTrackingInformation current = context.getTrackingInformation();
        if ( current == null )
        {
            level = context.getLevel().incrementAndGet();
            pathTrackingInformation.setLevel( level );
        }
        else
        {
            // same class so no inc
            if ( current != pathTrackingInformation )
            {
                level = context.getLevel().incrementAndGet();
                pathTrackingInformation.setLevel( level );
                pathTrackingInformation.setParent( current );
            }


        }
        pathTrackingInformation.setStart( System.nanoTime() );

        context.setTrackingInformation( pathTrackingInformation );

        return new PathTracker( pathTrackingInformation );
    }


    public void stop()
    {
        final long end = System.nanoTime();
        final long start = pathTrackingInformation.getStart();
        final Context context = THREAD_LOCAL.get();

        final String uuid = context.getUuid();

        final PathTrackingInformation current = context.getTrackingInformation();
        // same invocation so no inc, class can do recursion so don't use classname/methodname
        if ( pathTrackingInformation != current )
        {
            context.getLevel().decrementAndGet();
            context.setTrackingInformation( pathTrackingInformation.getParent() );
        }

        final PathTrackingEntry pathTrackingEntry =
            new PathTrackingEntry( uuid, NODE, pathTrackingInformation.getClassName(),
                                   pathTrackingInformation.getMethodName(), start, ( end - start ),
                                   pathTrackingInformation.getLevel() );
        if ( USE_SINGLE_STORE )
        {
            PATH_TRACKING_DATA_STORE.store( pathTrackingEntry );
        }
        else
        {
            context.getEntries().add( pathTrackingEntry );
        }

        if ( pathTrackingInformation.getLevel() == 1 && pathTrackingInformation.getParent() == null )
        { // 0 is never reached so 1 is first
            if (!USE_SINGLE_STORE)
            {
                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        PATH_TRACKING_DATA_STORE.store( context.getEntries() );
                        PathTracker.cleanUp();
                    }
                };
                if ( USE_EXECUTORS )
                {
                    EXECUTORSERVICE.submit( runnable );
                }
                else
                {
                    runnable.run();
                }
            }
        }
    }

    @Destroying
    public void destroy()
    {
        PathTracker.shutdown();
    }

    public static void shutdown()
    {
        EXECUTORSERVICE.shutdownNow();
    }


}
