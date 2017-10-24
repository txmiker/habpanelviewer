package vier_bier.de.habpanelviewer.motion;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vier_bier.de.habpanelviewer.CameraException;

/**
 * Motion detection using the old Camera API.
 */
@SuppressWarnings("deprecation")
public class MotionDetector extends AbstractMotionDetector<ImageData> {
    private static final String TAG = "MotionDetector";

    private boolean mRunning;
    private Camera mCamera;

    public MotionDetector(MotionListener l) {
        super(l);
    }

    @Override
    protected int getSensorOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Integer.parseInt(mCameraId), info);

        return info.orientation;
    }

    @Override
    protected LumaData getPreviewLumaData() {
        ImageData p = mPreview.getAndSet(null);
        if (p != null) {
            return p.extractLumaData(mBoxes);
        }

        return null;
    }

    protected String createCamera(int deviceDegrees) throws CameraException {
        if (mCamera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);

                    int result = (info.orientation + deviceDegrees) % 360;
                    result = (360 - result) % 360;
                    mCamera.setDisplayOrientation(result);

                    return String.valueOf(i);
                }
            }

            throw new CameraException("Could not find front facing camera!");
        }

        return null;
    }

    protected synchronized void startPreview() {
        if (mEnabled && !mRunning && mCamera != null && mSurface != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(mSurface);

                Camera.Parameters parameters = mCamera.getParameters();
                chooseOptimalSize(toPointArray(parameters.getSupportedPictureSizes()), 640, 480, new Point(640, 480));

                parameters.setPreviewSize(mPreviewSize.x, mPreviewSize.y);
                mCamera.setParameters(parameters);

                Log.d(TAG, "preview size: " + mCamera.getParameters().getPreviewSize().width + "x" + mCamera.getParameters().getPreviewSize().height);

                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        if (mCamera == camera) {
                            setPreview(new ImageData(bytes, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height));
                        }
                    }
                });

                mCamera.startPreview();
                mRunning = true;
            } catch (IOException e) {
                Log.e(TAG, "Error setting preview texture", e);
            }
        }
    }

    private Point[] toPointArray(List<Camera.Size> supportedPictureSizes) {
        ArrayList<Point> result = new ArrayList<>();
        for (Camera.Size s : supportedPictureSizes) {
            result.add(new Point(s.width, s.height));
        }
        return result.toArray(new Point[result.size()]);
    }

    @Override
    protected synchronized void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();

            Camera c = mCamera;
            mCamera = null;
            c.release();

            mRunning = false;
        }
    }

    @Override
    public String getCameraInfo(Activity act) {
        String camStr = "Camera API (Pre-Lollipop)\n";
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            camStr += "Camera " + i + ": ";

            boolean hasFlash = act.getApplicationContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

            camStr += (hasFlash ? "has" : "no") + " flash, ";
            camStr += (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front") + "-facing\n";
        }

        return camStr;
    }
}