package opencv.hegi.countpeopleopencv.data.singleton

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object DBConection {

    lateinit var db: FirebaseDatabase
    lateinit var mAuth: FirebaseAuth

    fun init(){
        db = FirebaseDatabase.getInstance()
        mAuth= FirebaseAuth.getInstance()
    }
}