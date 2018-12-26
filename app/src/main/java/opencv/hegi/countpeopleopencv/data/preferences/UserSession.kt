package opencv.hegi.countpeopleopencv.data.preferences

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

object UserSession {

    private const val USER_LOGGED = "user_logged"

    private lateinit var preferences: SharedPreferences

    var isLogged: Boolean
        get() = preferences.getBoolean(USER_LOGGED, false)
        set(value) { preferences.edit().putBoolean(USER_LOGGED, value).apply()}

    fun init(context: Context){
        preferences = context.getSharedPreferences("UserPreferences", Activity.MODE_PRIVATE)
    }

}