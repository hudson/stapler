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

package org.kohsuke.stapler.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.AbstractAPT6;
import org.kohsuke.stapler.AbstractAPT6.Content;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

/**
 * Generates stapler files for {@link ExportedBean} classes.
 * 
 * @author Stuart McCulloch
 */
@SupportedAnnotationTypes( "org.kohsuke.stapler.export.Exported" )
@SupportedSourceVersion( SourceVersion.RELEASE_6 )
@MetaInfServices( Processor.class )
public final class ExportedBeanAPT6
    extends AbstractAPT6
{
    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment round )
    {
        final Map<TypeElement, Properties> exported = new HashMap<TypeElement, Properties>();
        for ( final Element fieldOrMethod : round.getElementsAnnotatedWith( Exported.class ) )
        {
            final Element element = fieldOrMethod.getEnclosingElement();
            if ( element instanceof TypeElement )
            {
                Properties props = exported.get( element );
                if ( props == null )
                {
                    exported.put( (TypeElement) element, props = new Properties() );
                }
                final String javadoc = javadoc( fieldOrMethod );
                if ( javadoc != null )
                {
                    props.put( fieldOrMethod.getSimpleName().toString(), javadoc );
                }
            }
        }
        if ( !exported.isEmpty() )
        {
            store( load( new ExportedBeanContent( exported.keySet() ) ) );
        }
        for ( final Entry<TypeElement, Properties> e : exported.entrySet() )
        {
            store( new ExportedJavadocContent( e.getKey(), e.getValue() ) );
        }
        return false;
    }
}

/**
 * Generates {@literal "META-INF/exposed.stapler-beans"} content.
 */
final class ExportedBeanContent
    implements Content
{
    private final Set<String> beans = new TreeSet<String>();

    ExportedBeanContent( final Iterable<TypeElement> beans )
    {
        for ( final TypeElement b : beans )
        {
            this.beans.add( b.getQualifiedName().toString() );
        }
    }

    public String location()
    {
        return "META-INF/exposed.stapler-beans";
    }

    public void load( final InputStream is )
        throws IOException
    {
        beans.addAll( CharStreams.readLines( new InputStreamReader( is, Charsets.UTF_8 ) ) );
    }

    public void store( final OutputStream os )
        throws IOException
    {
        final byte[] bytes = Joiner.on( '\n' ).join( beans ).getBytes( Charsets.UTF_8 );
        ByteStreams.copy( ByteStreams.newInputStreamSupplier( bytes ), os );
    }
}

/**
 * Generates {@link Exported} javadoc content.
 */
final class ExportedJavadocContent
    implements Content
{
    private final TypeElement clazz;

    private final Properties javadoc;

    ExportedJavadocContent( final TypeElement clazz, final Properties javadoc )
    {
        this.clazz = clazz;
        this.javadoc = javadoc;
    }

    public String location()
    {
        return clazz.getQualifiedName().toString().replace( '.', '/' ) + ".javadoc";
    }

    public void load( final InputStream is )
    {
        // nothing to do
    }

    public void store( final OutputStream os )
        throws IOException
    {
        javadoc.store( os, null );
    }
}
