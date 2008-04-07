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

package org.apache.commons.monitoring.reporting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.monitoring.Counter;
import org.apache.commons.monitoring.Monitor;
import org.apache.commons.monitoring.StatValue;
import org.apache.commons.monitoring.Unit;
import org.apache.commons.monitoring.impl.monitors.CreateValuesOnDemandMonitor;
import org.apache.commons.monitoring.impl.values.ThreadSafeCounter;
import org.apache.commons.monitoring.reporting.Renderer.Options;

public class RendererTest
    extends TestCase
{
    Collection<Monitor> monitors;

    @Override
    protected void setUp()
        throws Exception
    {
        monitors = new LinkedList<Monitor>();
        Monitor m1 = new CreateValuesOnDemandMonitor( new Monitor.Key( "JsonRendererTest.setUp", "test", "reporting" ) );
        m1.getCounter( Monitor.PERFORMANCES ).add( 10, Unit.NANOS );
        m1.getGauge( Monitor.CONCURRENCY );
        monitors.add( m1 );

        Monitor m2 = new CreateValuesOnDemandMonitor( new Monitor.Key( "TestCase", "test", "junit" ) );
        m2.getCounter( Monitor.PERFORMANCES );
        m2.getGauge( Monitor.CONCURRENCY ).increment(Unit.NONE);
        monitors.add( m2 );
    }

    public void testRenderToJson()
        throws Exception
    {
        assertExpectedRendering( new JsonRenderer(), "js" );
    }

    public void testRenderToXml()
        throws Exception
    {
        assertExpectedRendering( new XmlRenderer(), "xml" );
    }

    public void testRenderToTxt()
        throws Exception
    {
        assertExpectedRendering( new TxtRenderer(), "txt" );
    }

    public void testRenderToHtml()
        throws Exception
    {
        assertExpectedRendering( new HtmlRenderer(), "html" );
    }

    public void testOptions()
        throws Exception
    {
        Options options = new OptionsSupport()
        {
            public Unit unitFor( StatValue value )
            {
                return Unit.MICROS;
            }
        };

        AbstractRenderer renderer = new XmlRenderer();
        Counter counter = new ThreadSafeCounter( "test" );
        counter.add( 1, Unit.MILLIS );
        counter.add( 1, Unit.MICROS );
        StringWriter writer = new StringWriter();
        Context ctx = new Context( new PrintWriter( writer ) );
        renderer.render( ctx, counter, "mean", counter.getMean(), options );
        assertEquals( " mean=\"500.50\"", writer.toString() );
    }

    protected void assertExpectedRendering( Renderer renderer, String format )
        throws Exception
    {
        StringWriter out = new StringWriter();
        Context ctx = new Context( new PrintWriter( out ) );
        renderer.render( ctx, monitors );
        String actual = out.toString();
        actual = StringUtils.remove( StringUtils.remove( actual, "\n" ), "\r" );

        String expected = IOUtils.toString( getClass().getResourceAsStream( "RendererTest." + format ) );
        expected = StringUtils.remove( StringUtils.remove( expected, '\n' ), '\r' );

        assertEquals( expected, actual );
    }
}