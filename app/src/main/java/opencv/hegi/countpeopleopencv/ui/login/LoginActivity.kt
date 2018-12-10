package opencv.hegi.countpeopleopencv.ui.login

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.activity_login.*
import opencv.hegi.countpeopleopencv.R
import opencv.hegi.countpeopleopencv.ui.counter.OpenCvController
import org.jetbrains.anko.startActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onResume() {
        super.onResume()

        button.clicks()
                .subscribe{
                    startActivity<OpenCvController>()
                }
    }
}
