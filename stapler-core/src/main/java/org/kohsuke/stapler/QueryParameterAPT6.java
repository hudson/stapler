/*******************************************************************************
 *
 * Copyright (c) 2012 Sonatype, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Stuart McCulloch
 *
 *******************************************************************************/

package org.kohsuke.stapler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.AbstractAPT6.Content;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * Generates stapler files for {@link QueryParameter} methods.
 * 
 * @author Stuart McCulloch
 */
@SupportedAnnotationTypes( "org.kohsuke.stapler.QueryParameter" )
@SupportedSourceVersion( SourceVersion.RELEASE_6 )
@MetaInfServices( Processor.class )
public final class QueryParameterAPT6
    extends AbstractAPT6
{
    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment round )
    {
        final Set<Element> visited = new HashSet<Element>();
        for ( final Element parameter : round.getElementsAnnotatedWith( QueryParameter.class ) )
        {
            final ExecutableElement method = (ExecutableElement) parameter.getEnclosingElement();
            if ( visited.add( method ) )
            {
                store( new QueryParameterContent( method ) );
            }
        }
        return false;
    }
}

/**
 * Generates {@link QueryParameter} stapler content.
 */
final class QueryParameterContent
    implements Content
{
    private final ExecutableElement method;

    QueryParameterContent( final ExecutableElement method )
    {
        this.method = method;
    }

    public String location()
    {
        final TypeElement clazz = (TypeElement) method.getEnclosingElement();
        final String path = clazz.getQualifiedName().toString().replace( '.', '/' );
        return path + '/' + method.getSimpleName() + ".stapler";
    }

    public void load( final InputStream is )
    {
        // nothing to do
    }

    public void store( final OutputStream os )
        throws IOException
    {
        final StringBuilder buf = new StringBuilder();
        for ( final VariableElement var : method.getParameters() )
        {
            if ( buf.length() > 0 )
            {
                buf.append( ',' );
            }
            buf.append( var.getSimpleName() );
        }

        final byte[] bytes = buf.toString().getBytes( Charsets.UTF_8 );
        ByteStreams.copy( ByteStreams.newInputStreamSupplier( bytes ), os );
    }
}
