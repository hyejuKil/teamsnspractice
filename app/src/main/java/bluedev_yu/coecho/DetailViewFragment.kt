package bluedev_yu.coecho

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bluedev_yu.coecho.model.ContentDTO
import bluedev_yu.coecho.model.FollowDTO
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class DetailViewFragment : Fragment() {

    var user : FirebaseUser?= null
    var firestore: FirebaseFirestore?= null
    var imageSnapshot : ListenerRegistration?= null
    var mainView : View?= null


    override fun onCreate(savedInstanceState: Bundle?) { //fragment 생성시 호출
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView( //fragment 생성 후 화면 생성시 호출
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        user = FirebaseAuth.getInstance().currentUser
        firestore = FirebaseFirestore.getInstance()

        //RecyclerView와 어댑터 연결
        mainView = inflater.inflate(R.layout.fragment_detail, container, false)
        return mainView
    }

    override fun onResume() { //해당 frament(activity)가 foreground로 나와 유저와 인터렉션 할 때 호출, background로 나올 시 onPause
        super.onResume()
        mainView?.detailviewfragment_recyclerview?.layoutManager = LinearLayoutManager(activity)
        mainView?.detailviewfragment_recyclerview?.adapter = DetailRecyclerViewAdapter()
        var mainActivity = activity as MainActivity
        mainActivity.progress_bar.visibility = View.INVISIBLE //페이지 로딩 완료시 progressbar 제거
    }

    override fun onStop() { //아예 멈추고 onstart부터 다시해야되는 멈춤?!
        super.onStop()
        imageSnapshot?.remove() //이미지 스냅샷 삭제
    }


    //recyclerview : 많은 수의 데이터 집합을 개별 아이템 단위로 구성하여 화면에 출력하는 view그룹, 한 화면에 표시되기
    //많은 수의 데이터를 스크롤 가능한 리스트로 표시해주는 위젯

    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>()
    {
        var contentDTOs: ArrayList<ContentDTO>?= null
        var contentUidList: ArrayList<String>?= null

        init{
            contentDTOs = ArrayList()
            contentUidList = ArrayList()
            var uid = FirebaseAuth.getInstance().currentUser?.uid

            //파이어베이스에서 해당하는 uid의 데이터 가져오기(read)
            firestore?.collection("user")?.document(uid!!)?.get()?.addOnCompleteListener(){
                task ->
                if(task.isSuccessful)
                {
                    var userDTO = task.result?.toObject(FollowDTO:: class.java)
                    if(userDTO?.followings != null)
                    {
                        getContents(userDTO?.followings)
                    }
                }
            }
        }

        fun getContents(followers: MutableMap<String,Boolean>?){ //팔로우 하는 사람들 게시글 받아오기
            imageSnapshot = firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{ //push driven으로 데이터 가져오기
                querySnapshot, firebaseFirestoreException ->
                contentDTOs?.clear()
                contentUidList?.clear()

                if(querySnapshot == null) return@addSnapshotListener //가져온 값이 null일 경우 이벤트 종료

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)!!
                    println(item.uid)
                    if(followers?.keys?.contains(item.uid)!!)
                    {
                        contentDTOs?.add(item) //document 전체 내용 담기
                        contentUidList?.add(snapshot.id) //document id 저장, document 내용 수정시(좋아요 갯수 등) 참조하는 리스트
                    }
                }
                notifyDataSetChanged() //recyclerview 다시 그리기
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false) //item_detail.xml 바인딩
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { //서버에서 가져온 데이터들 바인딩
            val viewHolder = (holder as CustomViewHolder).itemView

            //프로필 이미지 가져오기
            firestore?.collection("profileImages")?.document(contentDTOs?.get(position)?.uid!!)?.get()?.addOnCompleteListener{
                task ->
                if (task.isSuccessful)
                {
                    val url = task.result?.get("image")

                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(viewHolder.detailviewitem_profile_image)
                }
            }

            viewHolder.detailviewitem_profile_image.setOnClickListener() //프로필 사진 클릭하면 userFragment로
            {
                val fragment = UserFragment()
                val bundle = Bundle()

                bundle.putString("destinationUid", contentDTOs?.get(position)?.uid)
                bundle.putString("userId", contentDTOs?.get(position)?.userId)

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
            }

            //유저 아이디
            viewHolder.detailviewitem_profile_textview.text = contentDTOs?.get(position)?.userId

            //가운데 이미지
            Glide.with(holder.itemView.context).load(contentDTOs?.get(position)?.imageurl).into(viewHolder.detailviewitem_imageview_content)

            //설명 텍스트
            viewHolder.detailviewitem_explain_textview.text = contentDTOs?.get(position)?.explain

            //좋아요 이벤트
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener(){
                favoriteEvent(position)
            }

            //좋아요 버튼 설정
            if(contentDTOs?.get(position)?.favorites?.containsKey(FirebaseAuth.getInstance().currentUser!!.uid) == true) //좋아요 눌렀을 시
            {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite) //색칠된 하트
            }
            else
            {
                viewHolder.detailviewitem_favorite_imageview.setColorFilter(R.drawable.ic_favorite_border) //빈하트
            }

            //좋아요 카운터
            viewHolder.detailviewitem_favoritecounter_textview.text = contentDTOs?.get(position)?.favoriteCount.toString()

        }

        override fun getItemCount(): Int { //서버에서 불러온 데이터들 카운트
            if(contentDTOs != null) return contentDTOs!!.size else return 0
        }

        fun favoriteEvent(position: Int){ //좋아요 이벤트 발생시키는 fun
            var tsDoc = firestore?.collection("images")?.document(contentUidList?.get(position).toString())
            firestore?.runTransaction()
            {
                transaction ->
                var uid = FirebaseAuth.getInstance().currentUser!!.uid
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java) //내가 선택한 컨텐츠

                if(contentDTO!!.favorites.containsKey(uid)) //이미 좋아요 한 경우 -> 좋아요 빼기
                {
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! -1
                    contentDTO?.favorites.remove(uid)
                }
                else{ //좋아요 안함 -> 좋아요 더하기
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! +1
                    contentDTO?.favorites[uid] = true
                }
                transaction.set(tsDoc,contentDTO)
            }

        }

    }

    inner class CustomViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) //RecyclerView.ViewHoler, Recycler 메모리 누수 방지
}

