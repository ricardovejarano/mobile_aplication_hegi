package opencv.hegi.countpeopleopencv.ui.login

import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import com.counter.hegi.data.preferences.UserSession
import com.counter.hegi.util.text
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.activity_login.*
import opencv.hegi.countpeopleopencv.R
import opencv.hegi.countpeopleopencv.ui.counter.OpenCvController
import opencv.hegi.countpeopleopencv.ui.main.MainActivity
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

class LoginActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var username = ""
    private var password = ""
    private var mProgressBar: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // mAuth = FirebaseAuth.getInstance();


        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        mProgressBar = ProgressDialog(this)


    }


    override fun onResume() {
        super.onResume()

        loginAcceptBtn.clicks()
                .subscribe {
                    username = loginUsernameEdt.text()
                    password = loginPasswordEdt.text()
                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                        mProgressBar!!.setMessage("Autenticando...")
                        mProgressBar!!.show()

                        mAuth!!.signInWithEmailAndPassword(username!!, password!!)
                                .addOnCompleteListener(this) { task ->
                                    mProgressBar!!.hide()
                                    if (task.isSuccessful) {
                                        // Success Login
                                        UserSession.isLogged = true
                                        startActivity<MainActivity>()
                                    } else {
                                        // Error in Login
                                        toast("Usuario o contraseña incorrectos")
                                    }
                                }
                    } else {
                        toast("Campos requeridos")
                    }
                }
    }

}
