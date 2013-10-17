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
package org.apache.commons.monitoring.cube;

import org.apache.commons.monitoring.Role;
import org.apache.commons.monitoring.counters.Counter;
import org.apache.commons.monitoring.gauges.Gauge;
import org.apache.commons.monitoring.repositories.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CubeDataStoreTest {
    private CubeServer server;
    private Gauge.LoaderHelper gauges;

    @Before
    public void startCube() throws IOException {
        server = new CubeServer("localhost", 1234).start();
        Repository.INSTANCE.clear();
        gauges = new Gauge.LoaderHelper(false);
    }

    @After
    public void stopCube() {
        gauges.destroy();
        Repository.INSTANCE.clear();
        server.stop();
    }

    @Test
    public void store() throws InterruptedException, UnknownHostException {
        { // force some counter data
            final Counter counter = Repository.INSTANCE.getCounter(new Counter.Key(Role.PERFORMANCES, "test"));
            counter.add(1.4);
            counter.add(1.6);
            Thread.sleep(150);
            counter.add(2.3);
            counter.add(2.9);
            Thread.sleep(150);
        }

        final Collection<String> messages = server.getMessages();
        final Collection<String> gauges = new ArrayList<String>(4);
        int counters = 0;
        String aCounterMessage = null;
        for (final String m : messages) {
            if (m.contains("\"type\": \"gauge\"")) {
                gauges.add(m.replaceAll("\"time\": \"[^\"]*\"", "\"time\": \"-\"")); // remove date to be able to test it easily
            } else {
                counters++;
                aCounterMessage = m;
            }
        }

        final String host = InetAddress.getLocalHost().getHostName();
        assertTrue(gauges.contains("[{\"type\": \"gauge\",\"time\": \"-\",\"data\": {\"unit\":\"u\",\"marker\":\"" + host + "\",\"value\":0.0,\"role\":\"mock\"}}]"));
        assertTrue(gauges.contains("[{\"type\": \"gauge\",\"time\": \"-\",\"data\": {\"unit\":\"u\",\"marker\":\"" + host + "\",\"value\":1.0,\"role\":\"mock\"}}]"));
        assertTrue(gauges.contains("[{\"type\": \"gauge\",\"time\": \"-\",\"data\": {\"unit\":\"u\",\"marker\":\"" + host + "\",\"value\":2.0,\"role\":\"mock\"}}]"));
        assertTrue(gauges.contains("[{\"type\": \"gauge\",\"time\": \"-\",\"data\": {\"unit\":\"u\",\"marker\":\"" + host + "\",\"value\":3.0,\"role\":\"mock\"}}]"));

        assertTrue(counters >= 3);
        assertNotNull(aCounterMessage);
        assertThat(aCounterMessage, containsString("Variance"));
        assertThat(aCounterMessage, containsString("Value"));
        assertThat(aCounterMessage, containsString("Hits"));
        assertThat(aCounterMessage, containsString("Sum"));
        assertThat(aCounterMessage, containsString("marker"));
    }
}