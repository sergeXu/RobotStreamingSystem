package cn.nodemedia;

/**
 * Created by Serge on 2016/2/19.
 */

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import java.io.IOException;
import java.util.List;

public class VideoRecorder implements Callback, PreviewCallback {
    private final String TAG = "rtmpMedia.VideoRecorder";
    private Camera mCamera = null;
    private SurfaceHolder mSurfaceHolder = null;
    private int mCameraNum = 0;
    private int mCameraId = 0;
    private int mUIOrientation = 0;
    private byte[] mPreviewBuffer;
    public int preWidth = 1280;
    public int preHeight = 720;
    private boolean[] cameraAutoFocus;
    private boolean[] cameraFlashModes;
    boolean isCameraInfoDetected = false;
    boolean isPause = false;

    public VideoRecorder() {
    }

    public int startVideoRecorder(SurfaceView preview, int uiOrientation, int camId) {
        if (preview == null) {
            return -1;
        } else {
            switch (uiOrientation) {
                case 0:
                    this.mUIOrientation = 90;
                    break;
                case 1:
                    this.mUIOrientation = 0;
                    break;
                case 2:
                    this.mUIOrientation = 270;
                    break;
                case 3:
                    this.mUIOrientation = 180;
            }

            if (!this.isCameraInfoDetected) {
                this.mCameraNum = Camera.getNumberOfCameras();
                if (this.mCameraNum == 0) {
                    Log.e(TAG, "Get number of cameras error mCameraNum:" + this.mCameraNum);
                    return -1;
                }

                this.cameraAutoFocus = new boolean[this.mCameraNum];
                this.cameraFlashModes = new boolean[this.mCameraNum];

                for (int e = 0; e < this.mCameraNum; ++e) {
                    this.cameraAutoFocus[e] = true;
                    this.cameraFlashModes[e] = false;

                    try {
                        this.mCamera = Camera.open(e);
                    } catch (Exception var11) {
                        Log.e(TAG, "Camera id:" + e + " open error.");
                        continue;
                    }

                    Parameters e1;
                    try {
                        e1 = this.mCamera.getParameters();
                        e1.setPreviewSize(1280, 720);
                        this.mCamera.setParameters(e1);
                        Log.i(TAG, "Camera id:" + e + " supported 720p preview");
                    } catch (Exception var10) {
                        this.preWidth = 640;
                        this.preHeight = 480;
                        Log.w(TAG, "Camera id:" + e + " unsupported 720p preview,all using VGA");
                    }

                    try {
                        if (VERSION.SDK_INT < 14) {
                            throw new Exception("Unsupported API version for FOCUS_MODE_CONTINUOUS_VIDEO");
                        }

                        e1 = this.mCamera.getParameters();
                        e1.setFocusMode("continuous-video");
                        this.mCamera.setParameters(e1);
                        Log.i(TAG, "Camera id:" + e + " supported FOCUS_MODE_CONTINUOUS_VIDEO");
                    } catch (Exception var9) {
                        this.cameraAutoFocus[e] = false;
                        Log.w(TAG, "Camera id:" + e + " unsupported FOCUS_MODE_CONTINUOUS_VIDEO");
                    }

                    try {
                        e1 = this.mCamera.getParameters();
                        List flashModes = e1.getSupportedFlashModes();
                        if (flashModes.contains("torch") && flashModes.contains("off")) {
                            this.cameraFlashModes[e] = true;
                        }

                        Log.i(TAG, "Camera id:" + e + " supported set flash mode.");
                    } catch (Exception var8) {
                        Log.w(TAG, "Camera id:" + e + " unsupported set flash mode.");
                    }

                    this.mCamera.release();
                    this.mCamera = null;
                    this.isCameraInfoDetected = true;
                }

                if (!this.isCameraInfoDetected) {
                    Log.e(TAG, "VideoRecorder 启动错误，可能是权限未开启");
                    return -1;
                }
            }

            this.releaseCamera();
            if (this.openCamera(camId) != 0) {
                return -1;
            } else {
                try {
                    preview.getHolder().addCallback(this);
                    preview.getHolder().setKeepScreenOn(true);
                } catch (Exception var7) {
                    return -1;
                }

                this.isPause = false;
                return 0;
            }
        }
    }

    public void pause() {
        this.isPause = true;
    }

    public void resume() {
        this.isPause = false;
    }

    public int switchCamera() {
        if (this.mCameraNum == 1) {
            return 0;
        } else {
            this.releaseCamera();
            if (this.openCamera(this.mCameraId == 0 ? 1 : 0) != 0) {
                return -1;
            } else {
                try {
                    this.mCamera.setPreviewCallbackWithBuffer(this);
                    this.mCamera.setPreviewDisplay(this.mSurfaceHolder);
                    this.mCamera.startPreview();
                } catch (IOException var2) {
                    var2.printStackTrace();
                    return -1;
                }

                return this.mCameraId;
            }
        }
    }

    public int setFlashEnable(boolean flashEnable) {
        byte ret = -1;
        if (this.cameraFlashModes[this.mCameraId]) {
            Parameters parameters = this.mCamera.getParameters();
            if (flashEnable) {
                parameters.setFlashMode("torch");
                ret = 1;
            } else {
                parameters.setFlashMode("off");
                ret = 0;
            }

            this.mCamera.setParameters(parameters);
        }

        return ret;
    }

    public int stopVideoRecorder() {
        this.releaseCamera();
        return 0;
    }

    public int setCameraOrientation(int interfaceOrientation) {
        switch (interfaceOrientation) {
            case 0:
                this.mUIOrientation = 90;
                break;
            case 1:
                this.mUIOrientation = 0;
                break;
            case 2:
                this.mUIOrientation = 270;
                break;
            case 3:
                this.mUIOrientation = 180;
        }

        this.mCamera.setDisplayOrientation(this.mUIOrientation);
        return 0;
    }

    private int openCamera(int id) {
        try {
            this.mCamera = Camera.open(id);
        } catch (Exception var4) {
            Log.e(TAG, "Camera id:" + id + " open error:" + var4.getMessage());
            return -1;
        }

        this.mCameraId = id;
        Parameters parameters = this.mCamera.getParameters();
        parameters.setPreviewFormat(17);
        int previewBufferSize = this.preWidth * this.preHeight * 3 / 2;
        this.mPreviewBuffer = new byte[previewBufferSize];
        Log.i(TAG, "Camera Preview set to width:" + this.preWidth + " height:" + this.preHeight + " PreviewBufferSize:" + previewBufferSize);
        parameters.setPreviewSize(this.preWidth, this.preHeight);
        if (this.cameraAutoFocus[id]) {
            parameters.setFocusMode("continuous-video");
        }

        this.mCamera.setParameters(parameters);
        this.mCamera.addCallbackBuffer(this.mPreviewBuffer);
        this.mCamera.setDisplayOrientation(this.mUIOrientation);
        return 0;
    }

    private void releaseCamera() {
        if (this.mCamera != null) {
            this.mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }

    }

    public byte[] currentBuffer() {
        return this.mPreviewBuffer;
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    }

    public void surfaceCreated(SurfaceHolder arg0) {
        if (this.mCamera != null) {
            try {
                this.mCamera.stopPreview();
                this.mCamera.setPreviewCallbackWithBuffer(this);
                this.mCamera.setPreviewDisplay(arg0);
                this.mCamera.startPreview();
                this.mSurfaceHolder = arg0;
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        if (this.mCamera != null) {
            try {
                this.mCamera.stopPreview();
                this.mCamera.setPreviewDisplay((SurfaceHolder) null);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!this.isPause) {
            LivePublisher.putVideoData(data, data.length);
        }

        camera.addCallbackBuffer(data);
    }
}
