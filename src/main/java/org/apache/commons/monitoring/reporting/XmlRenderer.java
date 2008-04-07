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

import java.util.Collection;
import java.util.List;

import org.apache.commons.monitoring.Monitor;
import org.apache.commons.monitoring.StatValue;
import org.apache.commons.monitoring.Monitor.Key;

public class XmlRenderer
    extends AbstractRenderer
{

    /**
     * {@inheritDoc}
     * @see org.apache.commons.monitoring.reporting.AbstractRenderer#render(java.io.Context, java.util.Collection, org.apache.commons.monitoring.reporting.Renderer.Options)
     */
    @Override
    public void render( Context ctx, Collection<Monitor> monitors, Options options )
    {
        ctx.print( "<monitors>" );
        super.render( ctx, monitors, options );
        ctx.print( "</monitors>" );
    }

    @Override
    public void render( Context ctx, Monitor monitor, Options options, List<String> roles )
    {
        render( ctx, monitor, options );
    }

    @Override
    public void render( Context ctx, Monitor monitor, Options options )
    {
        ctx.print( "<monitor " );
        super.render( ctx, monitor, options );
        ctx.print( "</monitor>" );
    }

    @Override
    public void render( Context ctx, Key key )
    {
        ctx.print( "name=\"" );
        ctx.print( key.getName() );
        if ( key.getCategory() != null )
        {
            ctx.print( "\" category=\"" );
            ctx.print( key.getCategory() );
        }
        if ( key.getSubsystem() != null )
        {
            ctx.print( "\" subsystem=\"" );
            ctx.print( key.getSubsystem() );
        }
        ctx.print( "\">" );
    }


    @Override
    public void render( Context ctx, StatValue value, Options options )
    {
        ctx.print( "<" );
        ctx.print( value.getRole() );
        super.render( ctx, value, options );
        ctx.print( "/>" );
    }

    @Override
    protected void render( Context ctx, StatValue value, String attribute, Number number, Options options, int ratio )
    {
        ctx.print( " " );
        ctx.print( attribute );
        ctx.print( "=\"" );
        super.render( ctx, value, attribute, number, options, ratio );
        ctx.print( "\"" );
    }

}