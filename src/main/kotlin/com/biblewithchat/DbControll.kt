package com.biblewithchat

import com.biblewithchat.JDBC.commit
import com.biblewithchat.JDBC.conClose
import com.biblewithchat.JDBC.connection
import com.biblewithchat.JDBC.rollback
import com.biblewithchat.JDBC.stmtClose
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.text.SimpleDateFormat

class DbControll {

    var simple_std = SimpleDateFormat("yyyy-MM-dd H:mm:ss")


    fun 전달받은채팅저장(jin : JsonObject): Int {
        val sql전달받은채팅저장 = "insert into Chat values(0,?,?,?,?,?)"
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        var pkNo = 0
        try {
            pstm = con!!.prepareStatement(sql전달받은채팅저장, Statement.RETURN_GENERATED_KEYS)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            pstm.setInt(2, jin.get("user_no").asInt)
            pstm.setString(3, jin.get("chat_type").asString)
            pstm.setString(4, jin.get("chat_content").asString)
            pstm.setString(5, jin.get("create_date").asString)
            res = pstm.executeUpdate()


            val chatPkNo = pstm.generatedKeys //위에서 insert 한 레코드의 pk번호를 생성하여 사용함
            if (chatPkNo.next()) {
                pkNo = chatPkNo.getInt(1)//채팅을 쓰면 그 채팅방에서 읽지않은 채팅들을 읽음 처리하기 위해 방금 insert한 채팅 pkNo 필요함. 그리고 반환!
                val sql전달받은채팅안읽은사람목록만들기 = "insert into ChatIsRead (chat_no, user_no, chat_room_no) " +
                        " select ?, user_no, chat_room_no from ChatRoom cr where chat_room_no = ?  and user_no != ? "
                pstm = con!!.prepareStatement(sql전달받은채팅안읽은사람목록만들기)
                pstm.setInt(1, chatPkNo.getInt(1)) //리턴되는 pk번호가 1개뿐이라 첫번째 컬럼을 가져오면 된다
                pstm.setInt(2, jin.get("chat_room_no").asInt)
                pstm.setInt(3, jin.get("user_no").asInt) //쓴사람은 제외하고 안읽은 목록에 추가시켜야한다
                pstm.executeUpdate()//이것의 결과를 전송하면 채팅방에 방장 혼자만 있을 경우에는 commit이 안되고 위의 쿼리 결과에 따라 0이되어 rollback이 수행되서 저장이 안되는 문제가 생김.
            }

            // 아래는 insert 방식. delete방식으로 변경
//            val pkNo = pstm.generatedKeys //위에서 insert 한 레코드의 pk번호를 생성하여 사용함
//            if (pkNo.next()) {
//                val sql현재채팅읽음처리 = "insert into ChatIsRead (chat_no, user_no, read_date) values ( ? , ? , ? ) "
//                pstm = con!!.prepareStatement(sql현재채팅읽음처리)
//                pstm.setInt(1, pkNo.getInt(1)) //리턴되는 pk번호가 1개뿐이라 첫번째 컬럼을 가져오면 된다
//                pstm.setInt(2, jin.get("user_no").asInt)
//                pstm.setString(3, simple_std.format(Date(System.currentTimeMillis())))
//                res = pstm.executeUpdate()
//            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (res>0) {
                commit(con!!)
            }else{
                rollback(con!!)
            }
            conClose(con)
        }
        return pkNo
    }

    fun 안읽은사람목록만들기(jin : JsonObject): Int {
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        try {
            val sql전달받은채팅안읽은사람목록만들기 = "insert into ChatIsRead (chat_no, user_no, chat_room_no) " +
                    " select ?, user_no, chat_room_no from ChatRoom cr where chat_room_no = ?  and user_no != ? "
            pstm = con!!.prepareStatement(sql전달받은채팅안읽은사람목록만들기)
            pstm.setInt(1, jin.get("chat").asJsonObject.get("chat_no").asInt) //이건 http 서버에서 저장한 이미지 채팅의 채팅번호를 넣어야함
            pstm.setInt(2, jin.get("chat_room_no").asInt)
            pstm.setInt(3, jin.get("user_no").asInt) //쓴사람은 제외하고 안읽은 목록에 추가시켜야한다
            res = pstm.executeUpdate()

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (res>0) {
                commit(con!!)
            }else{
                rollback(con!!)
            }
            conClose(con)
        }
        return res
    }

    fun 현재채팅방첫접속여부체크(cliInfo: JsonObject): Int {
        val sql현재채팅방첫접속여부체크 = "select is_joined from ChatRoom where chat_room_no = ? and user_no = ? "
        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql현재채팅방첫접속여부체크)
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(2, cliInfo.get("user_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                res = rs.getInt(1)
//                res = rs.getInt("is_joined")
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return res
    }


    fun 현재채팅방첫접속여부체크변경(cliInfo: JsonObject): Int {
        val sql현재채팅방첫접속여부체크변경 = "update ChatRoom set is_joined = 1 where chat_room_no = ? and user_no = ? "
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql현재채팅방첫접속여부체크변경)
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(2, cliInfo.get("user_no").asInt)
            res = pstm.executeUpdate()

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (res>0) {
                commit(con!!)
            }else{
                rollback(con!!)
            }
            conClose(con)
        }
        return res
    }



    fun 현재채팅읽음처리(cliInfo: JsonObject): Int {
        //클라에서 처음 채팅방에 들어가서 채팅 목록이 하나도 없으면 first_chat_no 의 값은 null 이 된다.
        //그러면, 클라에서 먼저 npe가 걸리고 이어 여기서도 npe가 걸릴 것이다.
        // 채팅목록이 없는 경우에 대비해, 없는 경우 오류 방지를 위해 채팅읽음처리를 하지 않는다.
        if (cliInfo.get("first_chat_no") == null) {
            return 0
        }
//        val sql현재채팅읽음처리 = "update ChatIsRead set read_date = ? where chat_no = ? and user_no = ? "
//        val sql현재채팅읽음여부 = "select count(*) from ChatIsRead where chat_no = ? and user_no = ? "
//        val sql현재채팅읽음처리 = "insert into ChatIsRead (chat_no, user_no, read_date, chat_room_no) values ( ? , ? , ?, ? ) "
        val sql현재채팅읽음처리 = "delete from ChatIsRead where user_no = ?  and chat_room_no = ? " +
                " and chat_no BETWEEN ? and ? "
        //between a and b 문의 경우 반드시 a 가 작은수가 와야 정상적으로 범위가 책정된다. 1 and 5 는 가져오지만 5 and 1 의 값이 되는경우 아무런 값도 가져오지 않는다.
        //문법상 a <= x  and  x <= b  와 같기 때문임!
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql현재채팅읽음처리)
            pstm.setInt(1, cliInfo.get("user_no").asInt)
            pstm.setInt(2, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(3, cliInfo.get("first_chat_no").asInt)
            pstm.setInt(4, cliInfo.get("chat_no").asInt)
            res = pstm.executeUpdate()
            
            //insert 방식일 경우 -- 안씀. 위의 delete 방식으로 변경
            //테이블을 체크해서 채팅에 이미 사용자의 번호로된 레코드가 존재하면 insert 하면안되니까 두번째 if문에 참조건으로 res==0일때만이라는 것을 걸어줌
//            pstm = con!!.prepareStatement(sql현재채팅읽음여부)
//            pstm.setInt(1, cliInfo.get("chat_no").asInt)
//            pstm.setInt(2, cliInfo.get("user_no").asInt)
//            val rs = pstm.executeQuery()
//            if (rs.next()) {
//                res = rs.getInt(1)
//            }

            //이 채팅번호에 사용자의 번호로 된 읽기 레코드가 없으면 insert
//            if (res==0 && cliInfo.get("my_read_date").asString == "") {
//                pstm = con!!.prepareStatement(sql현재채팅읽음처리)
//                pstm.setInt(1, cliInfo.get("chat_no").asInt)
//                pstm.setInt(2, cliInfo.get("user_no").asInt)
//                pstm.setString(3, simple_std.format(Date(System.currentTimeMillis())))
//                pstm.setInt(4, cliInfo.get("chat_room_no").asInt)
//                res = pstm.executeUpdate()
//            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (res>0) {
                commit(con!!)
                println("현재채팅읽음처리 commit count: $res")
            }else{
                rollback(con!!)
            }
            conClose(con)
        }
        return res
    }

    fun 채팅방목록전달(cliInfo: JsonObject): JsonObject {
//        val sql채팅목록전달 = "select * " +
//                            "from ChatRoomInfo cri " +
//                            "join `User` u on u.user_no = cri.owner_no  " +
//                            "where group_no = ? "

        val sql채팅목록당읽지않은메시지수 = "select c.chat_room_no , c.user_no , COUNT(*) " +
                "from Chat c " +
                "join ChatRoomInfo cri on cri.chat_room_no = c.chat_room_no " +
                "left outer join ChatIsRead cir on cir.chat_no = c.chat_no " +
                "where cri.group_no = ? and c.user_no = ? and cir.read_date is null " +
                "group by c.chat_room_no , c.user_no  "

        val sql채팅목록당가장최신채팅 = " "

        val sql채팅목록전달 = "select * " +
                "from ( " +
                    "select * , (select count(*) from ChatIsRead cir where cir.user_no = ? and cir.chat_room_no = cri.chat_room_no) my_unread_count  " +
                    " ,(select count(*) from ChatRoom cr where cr.user_no = ? and cr.chat_room_no = cri.chat_room_no) my_is_joined " +
                    "from ChatRoomInfo cri " +
                    "join `User` u on u.user_no = cri.owner_no  " +
                    "where group_no = ? " +
                    ") f " +
                "left join (" +
                    "select c.chat_room_no , MAX(c.create_date) last_msg_date," +
                    "(select chat_no from Chat cc where cc.create_date = MAX(c.create_date) LIMIT 1) last_chat_no ," +
                    "(select user_no from Chat cc where cc.create_date = MAX(c.create_date) LIMIT 1) last_user_no ," +
                    "(select user_nick from Chat cc join `User` uu on cc.user_no = uu.user_no  where cc.create_date = MAX(c.create_date) LIMIT 1) last_user_nick ," +
                    "(select chat_content from Chat cc where cc.create_date = MAX(c.create_date) LIMIT 1) last_chat_content , " +
                    "(select chat_type from Chat cc where cc.create_date = MAX(c.create_date) LIMIT 1) last_chat_type " +
                    "from Chat c " +
                    "join ChatRoomInfo cri on c.chat_room_no = cri.chat_room_no " +
                    "where group_no = ? and chat_type in ('문자열', '이미지', '접속알림', '나가기알림')  " +
                    "group by c.chat_room_no" +
                    ") e " +
                "on f.chat_room_no = e.chat_room_no order by last_msg_date desc "

        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        var res = 0
        val jo1 = JsonObject()
        val jo2 = JsonObject()
        val jo = JsonObject()
        try {
            pstm = con!!.prepareStatement(sql채팅목록전달)
            pstm.setInt(1, cliInfo.get("user_no").asInt)
            pstm.setInt(2, cliInfo.get("user_no").asInt)
            pstm.setInt(3, cliInfo.get("group_no").asInt)
            pstm.setInt(4, cliInfo.get("group_no").asInt)
            rs = pstm.executeQuery()

            val ja = JsonArray()
            while (rs.next()) {
                val tmp = JsonObject()
                tmp.addProperty("chat_room_no", rs.getString("chat_room_no"))
                tmp.addProperty("chat_room_title", rs.getString("chat_room_title"))
                tmp.addProperty("owner_no", rs.getString("owner_no"))
                tmp.addProperty("create_date", rs.getString("create_date"))
                tmp.addProperty("chat_room_image", rs.getString("chat_room_image"))
                tmp.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                tmp.addProperty("group_no", rs.getString("group_no"))
                tmp.addProperty("user_email", rs.getString("user_email"))
                tmp.addProperty("user_nick", rs.getString("user_nick"))
                tmp.addProperty("user_name", rs.getString("user_name"))
                tmp.addProperty("user_image", rs.getString("user_image"))
                tmp.addProperty("my_unread_count", rs.getString("my_unread_count"))
                tmp.addProperty("my_is_joined", rs.getString("my_is_joined"))
                tmp.addProperty("last_msg_date", rs.getString("last_msg_date"))
                tmp.addProperty("last_chat_no", rs.getString("last_chat_no"))
                tmp.addProperty("last_user_no", rs.getString("last_user_no"))
                tmp.addProperty("last_user_nick", rs.getString("last_user_nick"))
                tmp.addProperty("last_chat_content", rs.getString("last_chat_content"))
                tmp.addProperty("last_chat_type", rs.getString("last_chat_type"))
//                tmp.addProperty("last_msg_date", if(rs.getString("last_msg_date")==null) "" else rs.getString("last_msg_date"))
//                tmp.addProperty("last_chat_no",  if(rs.getString("last_chat_no")==null) "" else rs.getString("last_chat_no"))
//                tmp.addProperty("last_user_no", if(rs.getString("last_user_no")==null) "" else rs.getString("last_user_no"))
//                tmp.addProperty("last_user_nick",  if(rs.getString("last_user_nick")==null) "" else rs.getString("last_user_nick"))
//                tmp.addProperty("last_chat_content",  if(rs.getString("last_chat_content")==null) "" else rs.getString("last_chat_content"))
//                res = rs.getInt("is_joined") //last_chat_type 도 고려해보기 - 이미지 알림에 쓸려면...
                ja.add(tmp)
            }

            jo.addProperty("cmd_type", "채팅")
            jo.addProperty("cmd", "채팅방목록")
            jo.addProperty("group_no", cliInfo.get("group_no").asString)
            jo.add("chatRoomInfoL", ja)

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return jo
    }


    fun 현재채팅목록갱신(cliInfo: JsonObject): JsonObject {
        //방정보 및 방장정보
        val sql현재채팅목록갱신1 = "select * from ChatRoomInfo cri " +
                "join User u on u.user_no = cri.owner_no " +
//                "join ChatRoom cr on cri.chat_room_no = cr.chat_room_no " +
                "where cri.chat_room_no = ? and cri.group_no = ?  "

        //방에 참가한 유저 목록
        val sql현재채팅목록갱신2 = "select * from ChatRoomInfo cri " +
                "join ChatRoom cr on cri.chat_room_no = cr.chat_room_no " +
                "join User u on u.user_no = cr.user_no " +
                "where cri.chat_room_no = ? and cri.group_no = ? "

        //현재 채팅목록 및 안읽은 사람 수
        //c.create_date 에 해당하는 채팅이 '접속알림' 채팅이었으면 미세한 시간에 따라 접속알림 채팅이 목록에 포함되지 않을 수 도 있다.
        //왜냐하면 user_chat_join_date 의 값이 접속알림이 생성된 시간보다 이후에 생성됐다면(채팅&php 양쪽 서버시간이 서로달라서 php쪽이 살짝 느릴경우)
        //'접속알림' 채팅이 포함되지 않기 때문..
        val sql현재채팅목록갱신3 = "select *, " +
                "(select COUNT(*) from  ChatIsRead cir where cir.chat_no = c.chat_no ) unread_count , " +
                "(select count(*) from ChatIsRead cir where cir.chat_no = c.chat_no  and cir.user_no = ? and cir.chat_room_no = ?) is_unread  " +
                "from Chat c " +
                "join `User` u on u.user_no = c.user_no " +
                "where chat_room_no = ? and " +
                "    c.create_date BETWEEN  " +
                "    (select user_chat_join_date from ChatRoom cr where chat_room_no = ? and user_no = ? )  " +
                "    and CURRENT_TIMESTAMP() " +
                "order by create_date   "

        //페이징 - chat_no를 조건으로 between 기존의 처음번호 and 기존번호의 마지막번호 를 조건으로 가져옴.
        // 기존의 마지막번호+1은 채팅방프래그먼트에 변수로 저장했다가 스크롤이 최상단에 가서 갱신할때 채팅데이터를 가져오는 시작점(다시 기존의 처음번호)으로 활용!


        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet      //ResultSet 은 재사용 및 st or pstm 객체가 닫히면 자동으로 같이 닫힘
        var res = 0
        val jo1 = JsonObject() //채팅방 정보
        val jo2 = JsonArray() //채팅방 유저리스트 정보
        val jo3 = JsonArray() //유저들이 쓴 채팅리스트
        val jo = JsonObject()
        try {
            //첫번째
            pstm = con!!.prepareStatement(sql현재채팅목록갱신1)
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(2, cliInfo.get("group_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                jo1.addProperty("chat_room_no", rs.getString("chat_room_no"))
                jo1.addProperty("chat_room_title", rs.getString("chat_room_title"))
                jo1.addProperty("owner_no", rs.getString("owner_no"))
                jo1.addProperty("create_date", rs.getString("create_date"))
                jo1.addProperty("chat_room_image", rs.getString("chat_room_image"))
                jo1.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                jo1.addProperty("group_no", rs.getString("group_no"))
                jo1.addProperty("user_email", rs.getString("user_email"))
                jo1.addProperty("user_nick", rs.getString("user_nick"))
                jo1.addProperty("user_name", rs.getString("user_name"))
                jo1.addProperty("user_image", rs.getString("user_image"))
            }

            //두번째
            pstm = con!!.prepareStatement(sql현재채팅목록갱신2)
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(2, cliInfo.get("group_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                val tmp = JsonObject()
                tmp.addProperty("chat_room_pk", rs.getString("chat_room_pk"))
                tmp.addProperty("chat_room_no", rs.getString("chat_room_no"))
                tmp.addProperty("chat_room_title", rs.getString("chat_room_title"))
                tmp.addProperty("user_no", rs.getString("user_no"))
                tmp.addProperty("create_date", rs.getString("create_date"))
                tmp.addProperty("chat_room_image", rs.getString("chat_room_image"))
                tmp.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                tmp.addProperty("user_chat_join_date", rs.getString("user_chat_join_date"))
                tmp.addProperty("group_no", rs.getString("group_no"))
                tmp.addProperty("user_email", rs.getString("user_email"))
                tmp.addProperty("user_nick", rs.getString("user_nick"))
                tmp.addProperty("user_name", rs.getString("user_name"))
                tmp.addProperty("user_image", rs.getString("user_image"))
                jo2.add(tmp)
            }

            //마지막
            pstm = con!!.prepareStatement(sql현재채팅목록갱신3)
            pstm.setInt(1, cliInfo.get("user_no").asInt)
            pstm.setInt(2, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(3, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(4, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(5, cliInfo.get("user_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                val tmp = JsonObject()
                tmp.addProperty("chat_no", rs.getString("chat_no"))
                tmp.addProperty("chat_content", rs.getString("chat_content"))
                tmp.addProperty("create_date", rs.getString("create_date"))
                tmp.addProperty("chat_room_no", rs.getString("chat_room_no"))
                tmp.addProperty("user_no", rs.getString("user_no"))  //채팅쓴사람
                tmp.addProperty("user_image", rs.getString("user_image"))
                tmp.addProperty("user_email", rs.getString("user_email"))
                tmp.addProperty("user_nick", rs.getString("user_nick"))
                tmp.addProperty("user_name", rs.getString("user_name"))
                tmp.addProperty("user_create_date", rs.getString("user_create_date"))
                tmp.addProperty("user_image", rs.getString("user_image"))
//                tmp.addProperty("my_read_date", if(rs.getString("my_read_date")==null) "" else rs.getString("my_read_date"))
                tmp.addProperty("unread_count", rs.getString("unread_count")) //각 채팅당 읽지 않은 사람 수
                tmp.addProperty("is_unread", rs.getString("is_unread"))    // 각 채팅당 내가 읽었는지 유무. 0 == 읽음. 1 == 안읽음
//                tmp.addProperty("stored_file_name", rs.getString("stored_file_name"))
//                tmp.addProperty("chat_image_no", rs.getString("chat_image_no"))
                tmp.addProperty("chat_type", rs.getString("chat_type")) //문자열,이미지, 접속알림, 방나가기
                jo3.add(tmp)
            }
            val sql현재채팅목록갱신4 = "select * from ChatImage where chat_no = ? "
            //각 채팅에 대해 chat_type == '이미지' 라면 db로부터 이미지 배열목록을 불러와 채팅 객체의 하위 속성(chat_image)으로 추가함
            jo3.forEach {
                if (it.asJsonObject.get("chat_type").asString == "이미지") {
                    pstm = con.prepareStatement(sql현재채팅목록갱신4)
                    pstm!!.setInt(1, it.asJsonObject.get("chat_no").asInt)
                    rs = pstm!!.executeQuery()
                    val tmpArray = JsonArray()
                    while (rs!!.next()) {
                        val tmp = JsonObject()
                        tmp.addProperty("chat_image_no", rs.getString("chat_image_no"))
                        tmp.addProperty("chat_no", rs.getString("chat_no"))
                        tmp.addProperty("origin_file_name", rs.getString("origin_file_name"))
                        tmp.addProperty("stored_file_name", rs.getString("stored_file_name"))
                        tmp.addProperty("file_size", rs.getString("file_size"))
                        tmp.addProperty("create_date", rs.getString("create_date"))
                        tmpArray.add(tmp)
                    }
                    it.asJsonObject.add("chat_image", tmpArray)

                } else {
                    it.asJsonObject.add("chat_image", JsonArray())
                }
            }

            jo.addProperty("cmd_type", "채팅")
            jo.addProperty("cmd", "채팅갱신")
            jo.addProperty("chat_room_no", cliInfo.get("chat_room_no").asString)
            jo.add("chatRoomInfo", jo1)
            jo.add("chatRoomUserL", jo2)
            jo.add("chatL", jo3)

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return jo
    }


    fun 받을유저리스트작성(cliInfo: JsonObject): MutableList<String> {
        val sql받을유저리스트작성 = "select * from ChatRoom where chat_room_no = ?  "
        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        var res = 0
        val receivers = mutableListOf<String>()
        try {
            pstm = con!!.prepareStatement(sql받을유저리스트작성)
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
//                receivers.addProperty("user_no", rs.getInt("user_no"))
                receivers.add(rs.getString("user_no"))
//                res = rs.getInt(1)
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return receivers
    }

    //현재 방나가기 명령을 보내온 채팅방 번호를 이용해서 해당 유저를 탈퇴(삭제) 시킨다.
    //그 채팅방에서 유저가 읽지 않은 채팅이 있을 수 있기 때문에 읽지않은 채팅목록에서도 모두 삭제 한다.
    fun 방나가기(jin: JsonObject) :Int {
        val sql방나가기 = "delete from ChatRoom where chat_room_no = ? and user_no = ? "
        val sql현재채팅방모두읽음처리 = "delete from ChatIsRead where chat_room_no = ? and user_no = ? "
        val con = connection
        var pstm: PreparedStatement? = null
//        var rs : ResultSet? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql방나가기)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            pstm.setInt(2, jin.get("user_no").asInt)
            res = pstm.executeUpdate()

            pstm = con!!.prepareStatement(sql현재채팅방모두읽음처리)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            pstm.setInt(2, jin.get("user_no").asInt)
            pstm.executeUpdate()


        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (res>0) {
                commit(con!!)
                println("방나가기 commit")
            }else{
                rollback(con!!)
            }
            conClose(con)
        }
        return res
    }

    fun 채팅방참가원목록(jin: JsonObject): JsonArray {
        val sql채팅방참가원목록 = "select * from ChatRoomInfo cri join ChatRoom cr on cri.chat_room_no = cr.chat_room_no " +
                " join `User` u on u.user_no = cr.user_no " +
                " where cri.chat_room_no = ?  and cri.group_no = ? order by cr.user_chat_join_date "
        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        val memberL = JsonArray()
        try {
            pstm = con!!.prepareStatement(sql채팅방참가원목록)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            pstm.setInt(2, jin.get("group_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                val jo = JsonObject()
                jo.addProperty("chat_room_no", rs.getString("chat_room_no"))
                jo.addProperty("owner_no", rs.getString("owner_no"))
                jo.addProperty("chat_room_title", rs.getString("chat_room_title"))
                jo.addProperty("create_date", rs.getString("create_date"))
                jo.addProperty("chat_room_image", rs.getString("chat_room_image"))
                jo.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                jo.addProperty("group_no", rs.getString("group_no"))
                jo.addProperty("user_no", rs.getString("user_no"))
                jo.addProperty("user_chat_join_date", rs.getString("user_chat_join_date"))
                jo.addProperty("chat_room_pk", rs.getString("chat_room_pk"))
                jo.addProperty("is_joined", rs.getString("is_joined"))
                jo.addProperty("user_email", rs.getString("user_email"))
                jo.addProperty("user_nick", rs.getString("user_nick"))
                jo.addProperty("user_create_date", rs.getString("user_create_date"))
                jo.addProperty("user_name", rs.getString("user_name"))
                jo.addProperty("user_image", rs.getString("user_image"))
                memberL.add(jo)
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return memberL
    }

    fun 채팅방이미지목록(jin: JsonObject): JsonArray {
        val sql채팅방참가원목록 = "select * from ChatRoomInfo cri " +
                " join Chat c on c.chat_room_no = cri.chat_room_no " +
                " join ChatImage ci on c.chat_no = ci.chat_no " +
                " where cri.chat_room_no = ? order by chat_image_no desc "
        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        val imageL = JsonArray()
        try {
            pstm = con!!.prepareStatement(sql채팅방참가원목록)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                val jo = JsonObject()
                jo.addProperty("chat_room_no", rs.getString("chat_room_no"))
                jo.addProperty("owner_no", rs.getString("owner_no"))
                jo.addProperty("chat_room_title", rs.getString("chat_room_title"))
//                jo.addProperty("create_date", rs.getString("create_date"))
                jo.addProperty("chat_room_image", rs.getString("chat_room_image"))
                jo.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                jo.addProperty("group_no", rs.getString("group_no"))
                jo.addProperty("user_no", rs.getString("user_no"))
                jo.addProperty("chat_image_no", rs.getString("chat_image_no"))
                jo.addProperty("chat_type", rs.getString("chat_type"))
                jo.addProperty("chat_no", rs.getString("chat_no"))
                jo.addProperty("stored_file_name", rs.getString("stored_file_name"))
                imageL.add(jo)
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return imageL
    }


}