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
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementScanner6;

import org.kohsuke.MetaInfServices;
import org.kohsuke.stapler.AbstractAPT6.Content;

/**
 * Generates stapler files for {@link DataBoundConstructor} classes.
 * 
 * @author Stuart McCulloch
 */
@SupportedAnnotationTypes( "*" )
@SupportedSourceVersion( SourceVersion.RELEASE_6 )
@MetaInfServices( Processor.class )
public final class DataBoundConstructorAPT6
    extends AbstractAPT6
{
    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment round )
    {
        for ( final Element element : round.getRootElements() )
        {
            SCANNER.visit( element, this );
        }
        return false;
    }

    private static final ElementVisitor<Void, DataBoundConstructorAPT6> SCANNER =
        new ElementScanner6<Void, DataBoundConstructorAPT6>()
        {
            @Override
            public Void visitExecutable( final ExecutableElement method, final DataBoundConstructorAPT6 self )
            {
                if ( method.getAnnotation( DataBoundConstructor.class ) != null )
                {
                    self.store( new DataBoundConstructorContent( method ) );
                }
                else if ( method.getKind() == ElementKind.CONSTRUCTOR )
                {
                    final String javadoc = self.javadoc( method );
                    if ( javadoc != null && javadoc.contains( "@stapler-constructor" ) )
                    {
                        self.store( new DataBoundConstructorContent( method ) );
                    }
                }
                return super.visitExecutable( method, self );
            }
        };
}

/**
 * Generates {@link DataBoundConstructor} stapler content.
 */
final class DataBoundConstructorContent
    implements Content
{
    private final ExecutableElement ctor;

    DataBoundConstructorContent( final ExecutableElement ctor )
    {
        this.ctor = ctor;
    }

    public String location()
    {
        final TypeElement clazz = (TypeElement) ctor.getEnclosingElement();
        final String path = clazz.getQualifiedName().toString().replace( '.', '/' );
        return path + ".stapler";
    }

    public void load( final InputStream is )
    {
        // nothing to do
    }

    public void store( final OutputStream os )
        throws IOException
    {
        final StringBuilder buf = new StringBuilder();
        for ( final VariableElement var : ctor.getParameters() )
        {
            if ( buf.length() > 0 )
            {
                buf.append( ',' );
            }
            buf.append( var.getSimpleName() );
        }

        final Properties props = new Properties();
        props.put( "constructor", buf.toString() );
        props.store( os, null );
    }
}
