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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.common.io.Closeables;

/**
 * APT6-based support for loading/storing generated content.
 * 
 * @author Stuart McCulloch
 */
public abstract class AbstractAPT6
    extends AbstractProcessor
{
    /**
     * @return Javadoc text for the given program element
     */
    protected final String javadoc( final Element element )
    {
        return processingEnv.getElementUtils().getDocComment( element );
    }

    /**
     * Represents content to be loaded from or stored to a specific location.
     */
    public interface Content
    {
        String location();

        void load( InputStream is )
            throws IOException;

        void store( OutputStream os )
            throws IOException;
    }

    /**
     * Loads the given content from a location under the class output directory.
     */
    protected final Content load( final Content content )
    {
        InputStream is = null;
        try
        {
            final String path = content.location();
            final FileObject f = processingEnv.getFiler().getResource( StandardLocation.CLASS_OUTPUT, "", path );
            content.load( is = f.openInputStream() );
        }
        catch ( final FilerException e )
        {
            // someone else is already processing this file!
        }
        catch ( final FileNotFoundException e )
        {
            // nothing to load
        }
        catch ( final Exception e )
        {
            processingEnv.getMessager().printMessage( Kind.ERROR, e.toString() );
        }
        finally
        {
            Closeables.closeQuietly( is );
        }
        return content;
    }

    /**
     * Stores the given content to a location under the class output directory.
     */
    protected final void store( final Content content )
    {
        OutputStream os = null;
        try
        {
            final String path = content.location();
            final FileObject f = processingEnv.getFiler().createResource( StandardLocation.CLASS_OUTPUT, "", path );
            content.store( os = f.openOutputStream() );
        }
        catch ( final FilerException e )
        {
            // someone else is already processing this file!
        }
        catch ( final Exception e )
        {
            processingEnv.getMessager().printMessage( Kind.ERROR, e.toString() );
        }
        finally
        {
            Closeables.closeQuietly( os );
        }
    }
}
