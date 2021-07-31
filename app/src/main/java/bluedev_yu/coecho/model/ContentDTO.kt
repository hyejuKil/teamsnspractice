package bluedev_yu.coecho.model

import java.sql.Timestamp

data class ContentDTO(var explain : String?= null,
                      var imageurl : String?= null,
                      var uid : String?= null,
                      var userId : String?= null, //유저 아이디
                      var timestamp: Long?= null,
                      var favoriteCount: Int =0,
                      var favorites : MutableMap<String, Boolean> = HashMap()) {
    data class Comment(var uid : String ?= null,
                       var userId: String?= null,
                       var comment: String?= null,
                       var timestamp: Long? = null)
}