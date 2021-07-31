package bluedev_yu.coecho

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import bluedev_yu.coecho.databinding.ActivityMainBinding
import bluedev_yu.coecho.model.UserDTO
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import java.net.Inet4Address

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemReselectedListener {

    var firestore : FirebaseFirestore? = null
    var mBinding: ActivityMainBinding ?= null
    val binding get() = mBinding!!

    //firestore : 게시글 내용, 사진 위치, timestamp 등
    //firestorage : 문서, 이미지, 파일, 영상 등

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress_bar.visibility = View.VISIBLE
        firestore = FirebaseFirestore.getInstance()

        bottom_navigation.setOnNavigationItemReselectedListener(this)
        bottom_navigation.selectedItemId = R.id.action_home

        //스토리지 접근요청(사진 올려야 하므로)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)



    }

    //data class UserDTO(var name : String? = null, var address: String? = null)
//
//    fun createData(){
//        var userDTO = UserDTO("이름","주소")
//        firestore?.collection("User")?.document("document1")?.set(userDTO)
//    }
//
//    fun readData(){
//        firestore?.collection("User")?.document("document1")?.get()?.addOnCompleteListener{ //pull driven, controller에 따라 화면갱신
//            task ->
//            if(task.isSuccessful)
//            {
//                var userDTO = task.result?.toObject(UserDTO :: class.java)
//                println(userDTO.toString())
//            }
//        }
//    }
//
//    fun addSnapshotDocument(){
//        firestore?.collection("User")?.document("document1")?.addSnapshotListener{ //push driven, db 변경시마다 ui변경
//            documentSnapshot, firebaseFirestoreException ->
//            var document = documentSnapshot?.toObject(UserDTO::class.java)
//            println(document.toString())
//        }
//    }
//
//    fun updateData(){
//        var map = mutableMapOf<String,Any>()
//        map["phone"] = "010-1234-5678"
//        firestore?.collection("User")?.document("document1")?.update(map)
//    }
//
//    fun deleteData(){
//        firestore?.collection("User")?.document("document1")?.delete()
//    }

    override fun onNavigationItemReselected(item: MenuItem)  {
        setToolbarDefault()

        when (item.itemId){
            R.id.action_home ->{ //home 버튼 선택시 detailViewFragement로 이동
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,detailViewFragment).commit()
            }

            R.id.action_search ->{ //검색
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content,gridFragment).commit()
            }

            R.id.action_add_photo->{ //사진 올리기
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) //권한이 있을 경우
                {
                    startActivity(Intent(this,AddPhotoActivity::class.java)) //사진올리기 activity로 이동
                }
                else
                {
                    Toast.makeText(this,"스토리지 읽기 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }
            }

            R.id.action_account->{ //마이페이지
                val userFragment = UserFragment()
                val uid = FirebaseAuth.getInstance().currentUser!!.uid

                val bundle = Bundle() //아무거나 포장할 수 있는 상자같은 것..?
                bundle.putString("destinationUid",uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
            }
        }
    }

    fun setToolbarDefault(){ //fragment 사용시 toolbar 가리기
        toolbar_title_image.visibility = View.VISIBLE
        toolbar_btn_back.visibility = View.GONE
        toolbar_username.visibility = View.GONE
    }
}


