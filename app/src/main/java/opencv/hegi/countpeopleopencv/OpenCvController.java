package opencv.hegi.countpeopleopencv;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class OpenCvController extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2  {

    private static final int W = 400;


    private CameraBridgeViewBase cameraView = null;
    private static boolean initOpenCV = false;

    static { initOpenCV = OpenCVLoader.initDebug(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv);

        cameraView = (CameraBridgeViewBase) findViewById(R.id.cameraview);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (initOpenCV) { cameraView.enableView(); }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Release the camera.
        if (cameraView != null) {
            cameraView.disableView();
            cameraView = null;
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat src = inputFrame.gray(); // convertir a escala de grises
        Mat line = new Mat();  // objeto para almacenar el resultado
        Point pt11 = new Point(W,0 );
        Point pt21 = new Point( W,2*W );

        Point pt12 = new Point( 4*W/3,0);
        Point pt22 = new Point( 4*W/3,2*W);



        // aplicar el algoritmo canny para detectar los bordes
        Imgproc.line(src, pt11, pt21, new Scalar(0,255,0), 3);

        Imgproc.line(src, pt12, pt22, new Scalar(0,255,0), 3);

        Imgproc.Canny(src, src, 10, 100);

        // devolver el objeto Mat procesado
        return src;
    }
}
