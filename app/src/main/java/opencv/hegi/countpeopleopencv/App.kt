package opencv.hegi.countpeopleopencv

import android.support.multidex.MultiDexApplication
import com.counter.hegi.data.preferences.UserSession
import com.counter.hegi.data.singleton.DBConection


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        UserSession.init(this)
        DBConection.init()
    }

}