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

package com.facebook.sample;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.TextureView;

import com.facebook.sample.gles.EGLRenderTarget;
import com.facebook.sample.gles.GLHelpers;
import com.facebook.sample.rendering.SampleGLTFRenderer;

import java.io.IOException;


public class SampleGLTFView extends TextureView {
    private static final String TAG = SampleGLTFView.class.getSimpleName();
    private static final String RENDER_THREAD_NAME = "GLTFRenderThread";

    private static final float SCALE_FACTOR = 0.5f;
    private float aspectRatio = 1.0f;

    private RenderThread renderThread;
    private final SampleGLTFRenderer gltfObject = new SampleGLTFRenderer();
    private Context context;

    public SampleGLTFView(Context context) {
        this(context, null);
    }

    public SampleGLTFView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SampleGLTFView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void initRenderThread(SurfaceTexture surface, int width, int height) {
        renderThread = new RenderThread(RENDER_THREAD_NAME);
        renderThread.start();

        Message msg = Message.obtain();
        msg.what = RenderThread.MSG_SURFACE_AVAILABLE;
        msg.obj = surface;
        msg.arg1 = width;
        msg.arg2 = height;
        renderThread.handler.sendMessage(msg);
    }

    public void releaseResources() {
        renderThread.handler.sendEmptyMessage(RenderThread.MSG_SURFACE_DESTROYED);
    }

    private class RenderThread extends HandlerThread {
        private static final int MSG_SURFACE_AVAILABLE = 0x1;
        private static final int MSG_VSYNC = 0x2;
        private static final int MSG_SURFACE_DESTROYED = 0x3;

        private static final float FOVY = 70f;
        private static final float Z_NEAR = 1f;
        private static final float Z_FAR = 1000f;

        private Handler handler;
        private Choreographer.FrameCallback frameCallback = new ChoreographerCallback();

        private EGLRenderTarget eglRenderTarget;

        private float[] modelMatrix = new float[16];
        private float[] viewMatrix = new float[16];
        private float[] projectionMatrix = new float[16];

        private class ChoreographerCallback implements Choreographer.FrameCallback {
            @Override
            public void doFrame(long frameTimeNanos) {
                handler.sendEmptyMessage(MSG_VSYNC);
            }
        }

        RenderThread(String name) {
            super(name);
            eglRenderTarget = new EGLRenderTarget();
        }

        @Override
        public synchronized void start() {
                super.start();

                handler = new Handler(getLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case MSG_SURFACE_AVAILABLE:
                                onSurfaceAvailable((SurfaceTexture)msg.obj, msg.arg1, msg.arg2);
                                break;
                            case MSG_VSYNC:
                                onVSync();
                                break;
                           case MSG_SURFACE_DESTROYED:
                               onSurfaceDestroyed();
                               break;
                        }
                    }
                };
        }

        private void onSurfaceAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.d(TAG, "onSurfaceAvailable w: " + width + " h: " + height);

            eglRenderTarget.createRenderSurface(surfaceTexture);

            Choreographer.getInstance().postFrameCallback(frameCallback);

            GLES20.glViewport(0, 0, width, height);
            GLHelpers.checkGlError("glViewport");

            aspectRatio = (float) width / height;
            Matrix.perspectiveM(projectionMatrix, 0, FOVY, aspectRatio, Z_NEAR, Z_FAR);
            Matrix.setIdentityM(viewMatrix, 0);
            Matrix.setIdentityM(modelMatrix, 0);
            GLES20.glClearColor(1.f, 1.f, 1.f, 1.f);

            try {
                gltfObject.createOnGlThread(context, "helloworld.gltf");
            } catch (IOException e) {
               Log.e(TAG, e.getMessage());
            }
        }

        private void onVSync() {
            if (!eglRenderTarget.hasValidContext()) {
                return;
            }

            Choreographer.getInstance().postFrameCallback(frameCallback);

            eglRenderTarget.makeCurrent();
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            updateCamera();
            gltfObject.updateModelMatrix(modelMatrix, SCALE_FACTOR * aspectRatio);

            gltfObject.draw(viewMatrix, projectionMatrix);

            eglRenderTarget.swapBuffers();
        }

        private void updateCamera() {
            Matrix.setLookAtM(
                    viewMatrix, 0,
                    0, 0, -1,
                    0, 0, 0,
                    0, 1, 0
            );
        }

        private void onSurfaceDestroyed() {
            eglRenderTarget.release();
            gltfObject.release();
        }
    }
}
