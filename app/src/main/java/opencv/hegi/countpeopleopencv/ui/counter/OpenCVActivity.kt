package opencv.hegi.countpeopleopencv.ui.counter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.activity_open_cv.*
import opencv.hegi.countpeopleopencv.R
import opencv.hegi.countpeopleopencv.data.model.Boardings
import opencv.hegi.countpeopleopencv.data.model.PersonCoordinate
import opencv.hegi.countpeopleopencv.data.singleton.DBConection
import opencv.hegi.countpeopleopencv.ui.main.MainActivity
import opencv.hegi.countpeopleopencv.util.format
import opencv.hegi.countpeopleopencv.util.formatHour
import opencv.hegi.countpeopleopencv.util.value
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class OpenCVActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {


    private var mDatabaseReferenceCounter: DatabaseReference? = null
    private var currentDate = ""
    private var currentHour = ""
    private var totalCount = 0
    private var parcialCount = 0

    // Variables para Geolocalización
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // Location Variables
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val TAG = "OCVSample::Activity"
    private val FACE_RECT_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
    val JAVA_DETECTOR = 0
    private var learn_frames = 0

    internal var method = 0

    // matrix for zooming
    private val mZoomWindow: Mat? = null
    private val mZoomWindow2: Mat? = null

    private var mItemFace50: MenuItem? = null
    private var mItemFace40: MenuItem? = null
    private var mItemFace30: MenuItem? = null
    private var mItemFace20: MenuItem? = null
    // private MenuItem               mItemType;

    private var mRgba: Mat? = null
    private var mGray: Mat? = null
    private var mCascadeFile: File? = null
    private val mCascadeFileEye: File? = null
    private var mJavaDetector: CascadeClassifier? = null
    private val mJavaDetectorEye: CascadeClassifier? = null


    private val mDetectorType = JAVA_DETECTOR
    private var mDetectorName = arrayOfNulls<String>(2)

    private var mRelativeFaceSize = 0.2f
    private var mAbsoluteFaceSize = 0

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    internal var counterUp = 0
    internal var counterDown = 0
    internal var counterFrames = 0
    internal var counterRefresh = 0
    internal var xCenter = -1.0
    internal var yCenter = -1.0

    // ======== TRACKING DEFINITION VARIABLES ===================== //
    private lateinit var personCoordinates: ArrayList<PersonCoordinate>   // This array provide coordenades of a real frame
    private lateinit var personTestCoordinates: ArrayList<PersonCoordinate>  // This array help us to see if there are noise in frame

    private var zone1 = 0
    private var zone2 = 0
    private var zone3 = 0
    private var zone4 = 0
    private var zone5 = 0
    private var zone6 = 0
    private var zone7 = 0
    private var zone8 = 0
    private var zone9 = 0

    private var widthRec = 0
    private var widthRecSaved = 0


    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    try {
                        // load cascade file from application resources
                        val `is` = resources.openRawResource(R.raw.lbpcascade_frontalface)
                        val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
                        mCascadeFile = File(cascadeDir, "lbpcascade_frontalface.xml")
                        val os = FileOutputStream(mCascadeFile)

                        val buffer = ByteArray(1024)
                        var bytesRead: Int = 0
                        while (`is`.read(buffer).also { bytesRead = it } >= 0) {

                            os.write(buffer, 0, bytesRead)

                        }
                        `is`.close()
                        os.close()

                        mJavaDetector = CascadeClassifier(mCascadeFile!!.absolutePath)
                        if (mJavaDetector!!.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier")
                            mJavaDetector = null
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile!!.absolutePath)
                        cascadeDir.delete()

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
                    }

                    mOpenCvCameraView!!.enableFpsMeter()
                    mOpenCvCameraView!!.setCameraIndex(0)
                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public fun OpenCVActivity() {
        mDetectorName = arrayOfNulls(2)
        mDetectorName[JAVA_DETECTOR] = "Java"
        personCoordinates = ArrayList()
        personTestCoordinates = ArrayList()
    }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        initialise()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_open_cv)

        // Se inicializa la instancia de FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Este callback se ejecuta cada 5 segundos y reasigna las variables
        // latitude y longitude para insertar en base de datos
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    longitude = location.longitude
                    latitude = location.latitude
                    // toast(location.latitude.toString() + location.longitude.toString())
                }
            }
        }

        mDetectorName = arrayOfNulls(2)
        mDetectorName[JAVA_DETECTOR] = "Java"
        personCoordinates = ArrayList()
        personTestCoordinates = ArrayList()

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.fd_activity_surface_view)
        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    @SuppressLint("CheckResult")
    public override fun onResume() {
        super.onResume()


        currentDate = Date().format()

        val mUser = DBConection.mAuth.currentUser
        val mUserReference = mDatabaseReferenceCounter?.child(mUser!!.uid + "/" + currentDate)
        Log.d("ENTRA",mUserReference.toString())
        mUserReference?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalCount = snapshot.value("totalCount").toString().toInt()
                parcialCount = snapshot.value("parcialCount").toString().toInt()
                Log.d("VARIABLET",totalCount.toString())
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })


        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }


        btnCountStop.clicks()
                .subscribe {
                    startActivity<MainActivity>()
                }

        createLocationRequest()
        startLocationUpdates()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()
    }

    override fun onCameraViewStopped() {
        mGray!!.release()
        mRgba!!.release()
        mZoomWindow!!.release()
        mZoomWindow2!!.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {

        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()
        val x1 = 650
        val y1 = 620
        val x2 = 350
        val y2 = 620



        Imgproc.line(mRgba!!, Point(x1.toDouble(), 0.0), Point(x1.toDouble(), y1.toDouble()), Scalar(255.0, 0.0, 0.0), 3)
        Imgproc.line(mRgba!!, Point(x2.toDouble(), 0.0), Point(x2.toDouble(), y2.toDouble()), Scalar(0.0, 255.0, 0.0), 3)

        Imgproc.putText(mRgba!!, "Contador total: $totalCount",
                Point(700.0, 60.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Contador parcial: $parcialCount",
                Point(700.0, 90.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        // ==============================================================================================================================================

        Imgproc.putText(mRgba!!, "Contador Up: $counterUp",
                Point(20.0, 60.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Contador Down: $counterDown",
                Point(20.0, 90.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Frames: $counterFrames",
                Point(20.0, 120.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        // =====================CONTADORES DE ZONAS ===========================================//
        Imgproc.putText(mRgba!!, "Zona1: $zone1",
                Point(20.0, 150.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona2: $zone2",
                Point(20.0, 180.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona3: $zone3",
                Point(20.0, 210.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona4: $zone4",
                Point(20.0, 240.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona5: $zone5",
                Point(20.0, 270.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona6: $zone6",
                Point(20.0, 300.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona7: $zone7",
                Point(20.0, 330.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "Zona8: $zone8",
                Point(20.0, 360.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "WIDTH: $widthRec",
                Point(20.0, 400.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))

        Imgproc.putText(mRgba!!, "REFRESH: $counterRefresh",
                Point(20.0, 430.0),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                255.0))



        if (mAbsoluteFaceSize == 0) {
            val height = mGray!!.rows()
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize)
            }
        }

        val faces = MatOfRect()

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector!!.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size())
        } else {
            Log.e(TAG, "Detection method is not selected!")
        }

        val facesArray = faces.toArray()
        for (i in facesArray.indices) {
            widthRec = facesArray[i].width
            Imgproc.rectangle(mRgba!!, facesArray[i].tl(), facesArray[i].br(),
                    FACE_RECT_COLOR, 3)
            xCenter = ((facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2).toDouble()
            yCenter = ((facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2).toDouble()
            val center = Point(xCenter, yCenter)

            Imgproc.circle(mRgba!!, center, 10, Scalar(255.0, 0.0, 0.0, 255.0), 3)

            Imgproc.putText(mRgba!!, "[" + center.x + "," + center.y + "]",
                    Point(center.x + 20, center.y + 20),
                    Core.FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255.0, 255.0, 255.0,
                    255.0))

            // La variable myPersonCoordinate guarda las coordenadas X,Y por cada ciclo de detección
            val myPersonCoordinate = PersonCoordinate()


            myPersonCoordinate.horizontal = facesArray[i].x
            myPersonCoordinate.vertical = facesArray[i].y


            // En esta sección se verifica primero si existe algún dato en la
            // variable personCoordenade.  Si no hay componente en ella, se empieza a
            // llenar el array testArray para verificar que no es ruido
            // luego, si se define que esos frames captados no son ruido, el contenido del
            // array temporal se pasa al array de tracking final ==> personCoordinate
            if (personCoordinates.size != 0) {
                // personCoordinates.add(myPersonCoordinate);
            } else {
                if (personTestCoordinates.size == 0) {
                    personTestCoordinates.add(myPersonCoordinate)
                } else {

                    // In this else it is necessary to evaluate if the previous detected frame has in common
                    // similar coordinates with the new capture
                    val sizeArray = 1

                    var lastPosition = 0
                    lastPosition = personTestCoordinates.size - 1
                    val lastVertical = personTestCoordinates[lastPosition].vertical  // last value of the horizontal coordinate
                    val lastHorizontal = personTestCoordinates[lastPosition].horizontal // last value of the vertical coordinate
                    val actualVertical = myPersonCoordinate.vertical
                    val actualHorizontal = myPersonCoordinate.horizontal

                    // This conditional determine if the actual vertical value is near of the pervious value saved
                    if (actualVertical >= lastVertical - 80 && actualVertical <= lastVertical + 80 && actualHorizontal >= lastHorizontal - 80 && actualHorizontal <= lastHorizontal + 80) {

                        if (actualHorizontal > 800) {
                            zone8 = 1
                        }

                        if (actualHorizontal > 700 && actualHorizontal < 800) {
                            zone7 = 1
                        }

                        if (actualHorizontal > 600 && actualHorizontal < 700) {
                            zone6 = 1
                        }

                        if (actualHorizontal > 500 && actualHorizontal < 600) {
                            zone5 = 1
                        }

                        if (actualHorizontal > 400 && actualHorizontal < 500) {
                            zone4 = 1
                        }

                        if (actualHorizontal > 300 && actualHorizontal < 400) {
                            zone3 = 1
                        }

                        if (actualHorizontal > 200 && actualHorizontal < 300) {
                            zone2 = 1
                        }

                        if (actualHorizontal < 100) {
                            zone1 = 1
                        }

                        // Here comes coordinated which belongs to the real object detected
                        counterFrames++
                        personTestCoordinates.add(myPersonCoordinate)
                        if (actualHorizontal < 350) {
                            evaluateUpPassager()
                            // function to evaluate
                        }

                        if (actualHorizontal > 650) {
                            evaluateDownPassager()
                            // function to evaluate
                        }


                        widthRecSaved = widthRec

                        // counterW ++;
                    } else {


                        counterRefresh++

                        if (counterRefresh > 30) {
                            personTestCoordinates.clear()
                            counterRefresh = 0
                            counterFrames = 0
                            zone1 = 0
                            zone2 = 0
                            zone3 = 0
                            zone4 = 0
                            zone5 = 0
                            zone6 = 0
                            zone7 = 0
                            zone8 = 0
                            zone9 = 0
                        }
                        // Un contador que si llega a cierto numero dispara el evento de limpiar el array

                    }
                }
            }


            if (personTestCoordinates.size == 10) {
                // counterW ++;
            }


            val r = facesArray[i]
            // compute the eye area
            val eyearea = Rect(r.x + r.width / 8,
                    (r.y + r.height / 4.5).toInt(), r.width - 2 * r.width / 8,
                    (r.height / 3.0).toInt())
            // split it
            val eyearea_right = Rect(r.x + r.width / 16,
                    (r.y + r.height / 4.5).toInt(),
                    (r.width - 2 * r.width / 16) / 2, (r.height / 3.0).toInt())
            val eyearea_left = Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (r.y + r.height / 4.5).toInt(),
                    (r.width - 2 * r.width / 16) / 2, (r.height / 3.0).toInt())
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba
            Imgproc.rectangle(mRgba!!, eyearea_left.tl(), eyearea_left.br(),
                    Scalar(255.0, 0.0, 0.0, 255.0), 2)
            Imgproc.rectangle(mRgba!!, eyearea_right.tl(), eyearea_right.br(),
                    Scalar(255.0, 0.0, 0.0, 255.0), 2)
        }

        return mRgba
    }

    // In this function it is validated if the person made the trip to get off the bus
    fun evaluateDownPassager() {

        if (parcialCount != 0) {
            val average = zone1 + zone2 + zone3 + zone4 + zone5
            if (average >= 1) {
                counterDown++
                writeDown()
                personTestCoordinates.clear()
                counterFrames = 0
                counterRefresh = 0
                zone1 = 0
                zone2 = 0
                zone3 = 0
                zone4 = 0
                zone5 = 0
                zone6 = 0
                zone7 = 0
                zone8 = 0
                zone9 = 0
            }
        }
    }

    fun writeDown() {
        currentHour = Date().formatHour()
        var downCount = Boardings(currentHour, false, parcialCount - 1, totalCount, latitude, longitude)
        val mUser = DBConection.mAuth.currentUser
        val mUserReference = mDatabaseReferenceCounter!!.child(mUser!!.uid + "/" + currentDate + "/boardings")
        val mCountReference = mDatabaseReferenceCounter!!.child(mUser.uid + "/" + currentDate)
        val tempCount = mCountReference.child("parcialCount")
        tempCount.setValue(parcialCount - 1)
        mUserReference.push().setValue(downCount)
    }

    fun evaluateUpPassager() {

        val average = zone8 + zone7 + zone6 + zone5 + zone4
        if (average >= 1) {
            counterUp++
            writeUp()
            personTestCoordinates.clear()
            counterFrames = 0
            counterRefresh = 0
            zone1 = 0
            zone2 = 0
            zone3 = 0
            zone4 = 0
            zone5 = 0
            zone6 = 0
            zone7 = 0
            zone8 = 0
            zone9 = 0
        }
    }

    fun writeUp() {
        currentHour = Date().formatHour()
        var upCount = Boardings(currentHour, true, parcialCount + 1, totalCount + 1, latitude, longitude)
        val mUser = DBConection.mAuth.currentUser
        val mUserReference = mDatabaseReferenceCounter!!.child(mUser!!.uid + "/" + currentDate + "/boardings")
        val mCountReference = mDatabaseReferenceCounter!!.child(mUser.uid + "/" + currentDate)
        val tempCountParcial = mCountReference.child("parcialCount")
        val tempCountTotal = mCountReference.child("totalCount")
        tempCountParcial.setValue(parcialCount + 1)
        tempCountTotal.setValue(totalCount + 1)
        mUserReference.push().setValue(upCount)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(TAG, "called onCreateOptionsMenu")
        mItemFace50 = menu.add("Face size 50%")
        mItemFace40 = menu.add("Face size 40%")
        mItemFace30 = menu.add("Face size 30%")
        mItemFace20 = menu.add("Face size 20%")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(TAG, "called onOptionsItemSelected; selected item: $item")
        if (item === mItemFace50)
            setMinFaceSize(0.5f)
        else if (item === mItemFace40)
            setMinFaceSize(0.4f)
        else if (item === mItemFace30)
            setMinFaceSize(0.3f)
        else if (item === mItemFace20)
            setMinFaceSize(0.2f)

        return true
    }

    private fun setMinFaceSize(faceSize: Float) {
        mRelativeFaceSize = faceSize
        mAbsoluteFaceSize = 0
    }

    fun onRecreateClick(v: View) {
        learn_frames = 0
    }

    // Detiene el loop de actualización de ubicación
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // La variable locationRequest tiene los parametros de localización de prioridad
    // y de tiempo de repetición de la captura de lat y lng
    fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }!!
    }

    // Loop que gestiona el callback de updateLocation
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */)
    }

    private fun initialise() {
        mDatabaseReferenceCounter = DBConection.db.reference.child("counter")
    }

    override fun onBackPressed() {
          // nada

    }


}
