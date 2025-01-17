/*
 * Copyright (C) 2016 CUBRID Corporation.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */
package com.cubrid.common.ui.swt;

import com.cubrid.cubridmigration.core.common.log.LogUtil;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Utility class for managing OS resources associated with SWT/JFace controls such as colors, fonts,
 * images, etc.
 *
 * <p>!!! IMPORTANT !!! Application code must explicitly invoke the <code>dispose()</code> method to
 * release the operating system resources managed by cached objects when those objects and OS
 * resources are no longer needed (e.g. on application shutdown)
 *
 * <p>This class may be freely distributed as part of any application or plugin.
 *
 * <p>Copyright (c) 2003 - 2005, Instantiations, Inc. <br>
 * All Rights Reserved
 *
 * @author scheglov_ke
 * @author Dan Rubel
 */
public final class ResourceManager extends SWTResourceManager {
    private static final Logger LOG = LogUtil.getLogger(ResourceManager.class);

    private ResourceManager() {}

    /**
     * Dispose of cached objects and their underlying OS resources. This should only be called when
     * the cached objects are no longer needed (e.g. on application shutdown)
     */
    public static void dispose() {
        disposeColors();
        disposeFonts();
        disposeImages();
        disposeCursors();
    }

    /** Do nothing method. */
    public void doNothing() {
        // do nothing.;
    }

    //////////////////////////////
    // Image support
    //////////////////////////////

    /** Maps image descriptors to images */
    private static Map<ImageDescriptor, Image> m_DescriptorImageMap =
            new HashMap<ImageDescriptor, Image>();

    /** Maps images to image decorators */
    private static Map<Image, HashMap<Image, Image>> m_ImageToDecoratorMap =
            new HashMap<Image, HashMap<Image, Image>>();

    /**
     * Returns an image descriptor stored in the file at the specified path relative to the
     * specified class
     *
     * @param clazz Class The class relative to which to find the image descriptor
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the specified path
     */
    public static ImageDescriptor getImageDescriptor(Class<?> clazz, String path) {
        return ImageDescriptor.createFromFile(clazz, path);
    }

    /**
     * Returns an image descriptor stored in the file at the specified path
     *
     * @param path String The path to the image file
     * @return ImageDescriptor The image descriptor stored in the file at the specified path
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        try {
            return ImageDescriptor.createFromURL((new File(path)).toURI().toURL());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns an image based on the specified image descriptor
     *
     * @param descriptor ImageDescriptor The image descriptor for the image
     * @return Image The image based on the specified image descriptor
     */
    public static Image getImage(ImageDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        Image image = m_DescriptorImageMap.get(descriptor);
        if (image == null) {
            image = descriptor.createImage();
            m_DescriptorImageMap.put(descriptor, image);
        }
        return image;
    }

    /**
     * Returns an image composed of a base image decorated by another image
     *
     * @param baseImage Image The base image that should be decorated
     * @param decorator Image The image to decorate the base image
     * @param corner The corner to place decorator image
     * @return Image The resulting decorated image
     */
    public static Image decorateImage(
            final Image baseImage, final Image decorator, final int corner) {
        HashMap<Image, Image> decoratedMap = m_ImageToDecoratorMap.get(baseImage);
        if (decoratedMap == null) {
            decoratedMap = new HashMap<Image, Image>();
            m_ImageToDecoratorMap.put(baseImage, decoratedMap);
        }
        Image result = decoratedMap.get(decorator);
        if (result == null) {
            final Rectangle bid = baseImage.getBounds();
            final Rectangle did = decorator.getBounds();
            final Point baseImageSize = new Point(bid.width, bid.height);
            CompositeImageDescriptor compositImageDesc =
                    new CompositeImageDescriptor() {
                        protected void drawCompositeImage(int width, int height) {
                            drawImage(baseImage.getImageData(), 0, 0);
                            if (corner == TOP_LEFT) {
                                drawImage(decorator.getImageData(), 0, 0);
                            } else if (corner == TOP_RIGHT) {
                                drawImage(decorator.getImageData(), bid.width - did.width - 1, 0);
                            } else if (corner == BOTTOM_LEFT) {
                                drawImage(decorator.getImageData(), 0, bid.height - did.height - 1);
                            } else if (corner == BOTTOM_RIGHT) {
                                drawImage(
                                        decorator.getImageData(),
                                        bid.width - did.width - 1,
                                        bid.height - did.height - 1);
                            }
                        }

                        protected Point getSize() {
                            return baseImageSize;
                        }
                    };
            result = compositImageDesc.createImage();
            decoratedMap.put(decorator, result);
        }
        return result;
    }

    /** Dispose all of the cached images */
    public static void disposeImages() {
        SWTResourceManager.disposeImages();
        // dispose ImageDescriptor images
        {
            for (Iterator<Image> it = m_DescriptorImageMap.values().iterator(); it.hasNext(); ) {
                it.next().dispose();
            }

            m_DescriptorImageMap.clear();
        }
        // dispose plugin images
        {
            for (Iterator<Image> it = m_URLImageMap.values().iterator(); it.hasNext(); ) {
                it.next().dispose();
            }

            m_URLImageMap.clear();
        }
        // dispose decorated images
        for (Iterator<HashMap<Image, Image>> it = m_ImageToDecoratorMap.values().iterator();
                it.hasNext(); ) {
            HashMap<Image, Image> decoratedMap = it.next();
            for (Iterator<Image> it1 = decoratedMap.values().iterator(); it1.hasNext(); ) {
                Image decoratedImage = it1.next();
                decoratedImage.dispose();
            }
        }
    }

    //////////////////////////////
    // Plugin images support
    //////////////////////////////

    /** Maps URL to images */
    private static Map<URI, Image> m_URLImageMap = new Hashtable<URI, Image>();

    /**
     * Retuns an image based on a plugin and file path
     *
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return Image The image stored in the file at the specified path
     */
    public static Image getPluginImage(Object plugin, String name) {
        try {
            URL url = getPluginImageURL(plugin, name);
            URI uri = url.toURI();
            if (m_URLImageMap.containsKey(uri)) {
                return m_URLImageMap.get(uri);
            }

            InputStream is = url.openStream();
            Image image;
            try {
                image = getImage(is);
                m_URLImageMap.put(uri, image);
            } finally {
                is.close();
            }
            return image;
        } catch (Throwable ex) {
            LOG.error(LogUtil.getExceptionString(ex));
        }
        return null;
    }

    /**
     * Retuns an image descriptor based on a plugin and file path
     *
     * @param plugin Object The plugin containing the image
     * @param name String The path to th eimage within the plugin
     * @return ImageDescriptor The image descriptor stored in the file at the specified path
     */
    public static ImageDescriptor getPluginImageDescriptor(Object plugin, String name) {
        try {
            try {
                URL url = getPluginImageURL(plugin, name);
                return ImageDescriptor.createFromURL(url);
            } catch (Throwable ex) {
                LOG.error(LogUtil.getExceptionString(ex));
            }
        } catch (Throwable ex) {
            LOG.error(LogUtil.getExceptionString(ex));
        }
        return null;
    }

    /**
     * Retuns an URL based on a plugin and file path
     *
     * @param plugin Object The plugin containing the file path
     * @param name String The file path
     * @return URL The URL representing the file at the specified path
     * @throws Exception e
     */
    private static URL getPluginImageURL(Object plugin, String name) throws Exception {
        // try to work with 'plugin' as with OSGI BundleContext
        try {
            Class<?> bundleClass = Class.forName("org.osgi.framework.Bundle"); // $NON-NLS-1$
            Class<?> bundleContextClass =
                    Class.forName("org.osgi.framework.BundleContext"); // $NON-NLS-1$
            if (bundleContextClass.isAssignableFrom(plugin.getClass())) {
                Method getBundleMethod =
                        bundleContextClass.getMethod("getBundle", new Class[0]); // $NON-NLS-1$
                Object bundle = getBundleMethod.invoke(plugin, new Object[0]);
                //
                Class<?> ipathClass =
                        Class.forName("org.eclipse.core.runtime.IPath"); // $NON-NLS-1$
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path"); // $NON-NLS-1$
                Constructor<?> pathConstructor =
                        pathClass.getConstructor(new Class[] {String.class});
                Object path = pathConstructor.newInstance(new Object[] {name});
                //
                Class<?> platformClass =
                        Class.forName("org.eclipse.core.runtime.Platform"); // $NON-NLS-1$
                Method findMethod =
                        platformClass.getMethod(
                                "find", new Class[] {bundleClass, ipathClass}); // $NON-NLS-1$
                return (URL) findMethod.invoke(null, new Object[] {bundle, path});
            }
        } catch (Throwable ex) {
            LOG.error(LogUtil.getExceptionString(ex));
        }
        // else work with 'plugin' as with usual Eclipse plugin
        {
            Class<?> pluginClass = Class.forName("org.eclipse.core.runtime.Plugin"); // $NON-NLS-1$
            if (pluginClass.isAssignableFrom(plugin.getClass())) {
                //
                Class<?> ipathClass =
                        Class.forName("org.eclipse.core.runtime.IPath"); // $NON-NLS-1$
                Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path"); // $NON-NLS-1$
                Constructor<?> pathConstructor =
                        pathClass.getConstructor(new Class[] {String.class});
                Object path = pathConstructor.newInstance(new Object[] {name});
                //
                Method findMethod =
                        pluginClass.getMethod("find", new Class[] {ipathClass}); // $NON-NLS-1$
                return (URL) findMethod.invoke(plugin, new Object[] {path});
            }
        }
        return null;
    }
}
