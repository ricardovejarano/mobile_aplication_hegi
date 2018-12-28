package opencv.hegi.countpeopleopencv.ui.main

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import opencv.hegi.countpeopleopencv.util.format
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_main.*
import opencv.hegi.countpeopleopencv.BR.urlImage
import opencv.hegi.countpeopleopencv.R
import opencv.hegi.countpeopleopencv.data.model.CountPassegers
import opencv.hegi.countpeopleopencv.data.model.Daily
import opencv.hegi.countpeopleopencv.data.preferences.UserSession
import opencv.hegi.countpeopleopencv.data.singleton.DBConection
import opencv.hegi.countpeopleopencv.databinding.ActivityMainBinding
import opencv.hegi.countpeopleopencv.ui.counter.OpenCVActivity
import opencv.hegi.countpeopleopencv.ui.counter.OpenCvController
import opencv.hegi.countpeopleopencv.ui.login.LoginActivity
import opencv.hegi.countpeopleopencv.util.BlurImage
import org.jetbrains.anko.startActivity
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mDatabaseReference: DatabaseReference? = null
    private var mDatabaseReferenceCounter: DatabaseReference? = null
    private var mDatabaseReferenceCountePassegers: DatabaseReference? = null
    private var currentDate = ""
    private var routeDriver = ""
    private var busAssigned = ""
    private var existDateInDatabase = false
    private var existCountInDatabase = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(this, mainDrawer,
                R.string.open_menu, R.string.close_menu) {}

        toggle.isDrawerIndicatorEnabled = true
        mainDrawer.addDrawerListener(toggle)
        toggle.syncState()
        getDate()
        initialise()
        checkExitPath()
        checkExistCount()
        loadBluri()
        binding.urlImage = "http://gsiep.labc.usb.ve/wp-content/uploads/Headshots/u_abueno.jpg"
    }


    fun loadBluri() {
        val target = object : Target {
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                profileRibbon.setImageResource(R.drawable.user_unknown)
            }

            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                profileRibbon.setImageBitmap(BlurImage.fastblur(bitmap, 1f, 35))
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable) {

            }
        }

        profileRibbon.tag = target
        Picasso.get()
                .load(urlImage)
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .into(target)
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()

        mainNavigationMenu.setNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.menu_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    UserSession.isLogged = false
                    startActivity<LoginActivity>()
                }
            }
            mainDrawer.closeDrawer(GravityCompat.START)
            true
        }


        MainBtnStartCount.clicks()
                .subscribe{
                    if (existDateInDatabase && existCountInDatabase) {
                        startActivity<OpenCVActivity>()
                    } else {
                        if(!existDateInDatabase) {
                            val daily = Daily(routeDriver, busAssigned, 0, 0)
                            val mUser = DBConection.mAuth.currentUser
                            val mUserReference = mDatabaseReferenceCounter!!.child(mUser!!.uid+ "/" +currentDate)
                            mUserReference.setValue(daily)
                        }

                        if(!existCountInDatabase) {
                            val countP = CountPassegers(0)
                            val mUserReference2 = mDatabaseReferenceCountePassegers!!.child(currentDate)
                            mUserReference2.setValue(countP)
                        }

                        startActivity<OpenCVActivity>()

                    }
                }
    }

    override fun onSupportNavigateUp(): Boolean {
        if(mainDrawer.isDrawerOpen(GravityCompat.START)) mainDrawer.closeDrawer(GravityCompat.START)
        else mainDrawer.openDrawer(GravityCompat.START)

        return super.onSupportNavigateUp()

    }

    private fun initialise() {
        mDatabaseReference = DBConection.db.reference.child("driver")
        mDatabaseReferenceCounter = DBConection.db.reference.child("counter")
        mDatabaseReferenceCountePassegers = DBConection.db.reference.child("countPassegers")
    }

    override fun onStart() {
        super.onStart()
        val mUser = DBConection.mAuth!!.currentUser
        val mUserReference = mDatabaseReference!!.child(mUser!!.uid)
        mUserReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mainTxtViewUserName.text = snapshot.child("name").value as String
                driverRoute.text = snapshot.child("route").value as String
                routeDriver = snapshot.child("route").value as String
                busAssigned = snapshot.child("busAssigned").value as String
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getDate() {
        currentDate = Date().format()
        MainEdtTxtDate.text = currentDate
    }

    private fun checkExitPath() {
        // This function checks if uid/date exist
        val mUser = DBConection.mAuth?.currentUser
        val mUserReference2 = mDatabaseReferenceCounter?.child(mUser?.uid + "/" + currentDate)
        mUserReference2?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ghj = snapshot.value
                if(snapshot.value != null) {
                    existDateInDatabase = true
                    Log.i("counter", "Existe")
                } else {
                    existDateInDatabase = false
                    Log.i("counter", "No Existe")
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun checkExistCount() {
        val mUser = DBConection.mAuth?.currentUser
        val mUserReference3 = mDatabaseReferenceCountePassegers?.child(currentDate)
        mUserReference3?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ghj = snapshot.value
                if(snapshot.value != null) {
                    existCountInDatabase = true
                    Log.i("counting", "Existe")
                } else {
                    existCountInDatabase = false
                    Log.i("counting", "No Existe")
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}
