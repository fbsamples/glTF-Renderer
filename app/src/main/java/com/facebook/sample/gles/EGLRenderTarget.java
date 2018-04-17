/**
 * Copyright 2016-present, Facebook, Inc.
 * All rights reserved.
 *
 * Licensed under the Creative Commons CC BY-NC 4.0 Attribution-NonCommercial
 * License (the "License"). You may obtain a copy of the License at
 * https://creativecommons.org/licenses/by-nc/4.0/.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.sample.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLUtils;

public class EGLRenderTarget {
    private EGLDisplay eglDisplay;
    private EGLConfig eglConfig;
    private EGLContext eglContext;
    private EGLSurface eglSurface;

    public EGLRenderTarget() {
        init();
    }

    private void init() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            abortWithEGLError("eglGetDisplay");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            abortWithEGLError("eglInitialize");
        }

        final int[] attributeList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                attributeList,
                0,
                configs,
                0,
                configs.length,
                numConfigs,
                0)) {
            abortWithEGLError("eglChooseConfig");
        }

        if (numConfigs[0] <= 0) {
            abortWithEGLError("No EGL config found for attribute list");
        }

        eglConfig = configs[0];

        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0);

        if (eglContext == null) {
            abortWithEGLError("eglCreateContext");
        }
    }

    public void createRenderSurface(SurfaceTexture surfaceTexture) {
        if (!hasValidContext()) {
            init();
        }

        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, eglConfig, surfaceTexture, surfaceAttribs, 0);

        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            abortWithEGLError("eglCreateWindowSurface");
        }

        makeCurrent();
    }

    public void swapBuffers() {
        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            abortWithEGLError("eglSwapBuffers");
        }
    }

    private void abortWithEGLError(String msg) {
        int error = EGL14.eglGetError();
        throw new RuntimeException(msg + ": EGL error: 0x" +
                Integer.toHexString(error) + ": " +
                GLUtils.getEGLErrorString(error));
    }

    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            abortWithEGLError("eglMakeCurrent");
        }
    }

    public void release() {
        EGL14.eglDestroySurface(eglDisplay, eglSurface);
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglSurface = EGL14.EGL_NO_SURFACE;
        eglContext = EGL14.EGL_NO_CONTEXT;
    }

    public boolean hasValidContext() {
        return eglContext != EGL14.EGL_NO_CONTEXT;
    }
}
