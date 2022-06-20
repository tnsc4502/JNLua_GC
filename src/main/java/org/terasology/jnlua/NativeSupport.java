/*
 * Copyright (C) 2008,2012 Andre Naef
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.terasology.jnlua;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the JNLua native library.
 * <p>
 * The class provides and configures a default loader implementation that loads
 * the JNLua native library by means of the <code>System.loadLibrary</code>
 * method. In some situations, you may want to override this behavior. For
 * example, when using JNLua as an OSGi bundle, the native library is loaded by
 * the OSGi runtime. Therefore, the OSGi bundle activator replaces the loader by
 * a no-op implementaion. Note that the loader must be configured before
 * LuaState is accessed.
 */
public final class NativeSupport {
    // -- Static
    private static final NativeSupport INSTANCE = new NativeSupport();

    // -- State
    private Loader loader = new DefaultLoader();

    /**
     * Returns the instance.
     *
     * @return the instance
     */
    public static NativeSupport getInstance() {
        return INSTANCE;
    }

    // -- Construction

    /**
     * Private constructor to prevent external instantiation.
     */
    private NativeSupport() {
    }

    // -- Properties

    /**
     * Return the native library loader.
     *
     * @return the loader
     */
    public Loader getLoader() {
        return loader;
    }

    /**
     * Sets the native library loader.
     *
     * @param loader the loader
     */
    public void setLoader(Loader loader) {
        if (loader == null) {
            throw new NullPointerException("loader must not be null");
        }
        this.loader = loader;
    }

    // -- Member types

    /**
     * Loads the library.
     */
    public interface Loader {
        public void load(Class<?> src);
    }

    private static class DefaultLoader implements Loader {
        @Override
        public void load(Class<?> src) {
            File libFile;
            String platformTypeName;
            String platform;
            String osName = System.getProperty("os.name").toLowerCase();
            switch (platform = System.getProperty("os.arch").toLowerCase()) {
                case "x86_64":
                case "amd64": {
                    platformTypeName = "amd64";
                    break;
                }
                case "aarch64": {
                    platformTypeName = "aarch64";
                    break;
                }
                case "arm": {
                    platformTypeName = "arm";
                    break;
                }
                case "x86": {
                    platformTypeName = "x86";
                    break;
                }
                default: {
                    platformTypeName = "raw" + platform;
                }
            }

            StringBuilder osTypeName = new StringBuilder();
            if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                osTypeName.append("linux").append(".so");
            } else if (osName.contains("win")) {
                osTypeName.append("windows").append(".dll");
            } else if (osName.contains("mac")) {
                osTypeName.append("mac").append(".dylib");
            } else {
                osTypeName.append("raw").append(osName);
            }

            String libFileName = String.format("/%s/%s-%s", "jni", platformTypeName, osTypeName);
            try {
                libFile = File.createTempFile("lib", null);
                libFile.deleteOnExit();
                if (!libFile.exists()) {
                    throw new IOException();
                }
            } catch (IOException iOException) {
                throw new UnsatisfiedLinkError("Failed to create temp file");
            }
            byte[] arrayOfByte = new byte[2048];
            try {
                InputStream inputStream = Loader.class.getResourceAsStream(libFileName);
                if (inputStream == null) {
                    throw new UnsatisfiedLinkError(String.format("Failed to open lib file: %s", libFileName));
                }
                try (FileOutputStream fileOutputStream = new FileOutputStream(libFile);) {
                    int size;
                    while ((size = inputStream.read(arrayOfByte)) != -1) {
                        fileOutputStream.write(arrayOfByte, 0, size);
                    }
				} catch (Throwable throwable) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
            } catch (IOException exception) {
                throw new UnsatisfiedLinkError(String.format("Failed to copy file: %s", exception.getMessage()));
            }
            System.load(libFile.getAbsolutePath());
        }
    }
}
