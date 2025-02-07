/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = IdentifiableObjectController.RESOURCE_PATH )
public class IdentifiableObjectController
    extends AbstractCrudController<IdentifiableObject>
{
    public static final String RESOURCE_PATH = "/identifiableObjects";

    @Override
    public List<IdentifiableObject> getEntity( String uid, WebOptions options )
    {
        return manager.find( uid ).map( List::of ).orElseGet( List::of );
    }

    @Override
    public WebMessage postXmlObject( HttpServletRequest request )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "POST" );
    }

    @Override
    public WebMessage postJsonObject( HttpServletRequest request )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "POST" );
    }

    @Override
    public WebMessage putJsonObject( @PathVariable( "uid" ) String pvUid, @CurrentUser User currentUser,
        HttpServletRequest request )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "PUT" );
    }

    @Override
    public WebMessage deleteObject( @PathVariable( "uid" ) String pvUid, @CurrentUser User currentUser,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        throw new HttpRequestMethodNotSupportedException( "DELETE" );
    }
}
