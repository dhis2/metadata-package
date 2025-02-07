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
package org.hisp.dhis.cacheinvalidation.redis;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.cacheinvalidation.BaseCacheEvictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.lettuce.core.pubsub.RedisPubSubListener;

/**
 * Listens for messages on a Redis pub/sub channel, and when it receives a
 * message, it invalidates the cache for the entity that was changed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
@Profile( { "!test", "!test-h2" } )
@Conditional( value = RedisCacheInvalidationEnabledCondition.class )
public class CacheInvalidationListener extends BaseCacheEvictionService implements RedisPubSubListener<String, String>
{

    @Autowired
    @Qualifier( "cacheInvalidationServerId" )
    protected String serverInstanceId;

    @Override
    public void message( String channel, String message )
    {
        log.debug( "Got {} on channel {}", message, channel );

        try
        {
            handleMessage( message );
        }
        catch ( Exception e )
        {
            log.error( "Error handling message: " + message, e );
        }
    }

    private void handleMessage( String message )
        throws Exception
    {
        log.debug( "Handling Redis cache invalidation message: " + message );

        String[] parts = message.split( ":" );

        String uid = parts[0];
        // If the UID is the same, it means the event is coming from this
        // server.
        if ( serverInstanceId.equals( uid ) )
        {
            log.debug( "Message came from this server, ignoring." );
            return;
        }

        log.debug( "Incoming invalidating cache message from other server with UID: " + uid );

        CacheEventOperation operationType = CacheEventOperation.valueOf( parts[1].toUpperCase() );

        if ( CacheEventOperation.COLLECTION == operationType )
        {
            String role = parts[3];
            Long ownerEntityId = Long.parseLong( parts[4] );
            sessionFactory.getCache().evictCollectionData( role, ownerEntityId );

            log.debug( "Invalidated cache for collection: " + role + " with entity id: " + ownerEntityId );
            return;
        }

        Long entityId = Long.parseLong( parts[3] );
        Class<?> entityClass = Class.forName( parts[2] );
        Objects.requireNonNull( entityClass, "Entity class can't be null" );

        if ( CacheEventOperation.INSERT == operationType )
        {
            // Make sure queries will re-fetch to capture the new object.
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), entityClass );
            paginationCacheManager.evictCache( entityClass.getName() );
            // Try to fetch the new entity, so it might get cached.
            tryFetchNewEntity( entityId, entityClass );

            log.debug( "Invalidated cache for create: " + entityClass.getName() + " with entity id: " + entityId );
        }
        else if ( CacheEventOperation.UPDATE == operationType )
        {
            sessionFactory.getCache().evict( entityClass, entityId );

            log.debug( "Invalidated cache for update: " + entityClass.getName() + " with entity id: " + entityId );
        }
        else if ( CacheEventOperation.DELETE == operationType )
        {
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), entityClass );
            paginationCacheManager.evictCache( entityClass.getName() );
            sessionFactory.getCache().evict( entityClass, entityId );

            log.debug( "Invalidated cache for delete: " + entityClass.getName() + " with entity id: " + entityId );
        }
    }

    @Override
    public void message( String pattern, String channel, String message )
    {
        log.debug( "Got {} on channel {}", message, channel );
    }

    @Override
    public void subscribed( String channel, long count )
    {
        log.debug( "Subscribed to {}", channel );
    }

    @Override
    public void psubscribed( String pattern, long count )
    {
        log.debug( "Subscribed to pattern {}", pattern );
    }

    @Override
    public void unsubscribed( String channel, long count )
    {
        log.debug( "Unsubscribed from {}", channel );
    }

    @Override
    public void punsubscribed( String pattern, long count )
    {
        log.debug( "Unsubscribed from pattern {}", pattern );
    }
}
