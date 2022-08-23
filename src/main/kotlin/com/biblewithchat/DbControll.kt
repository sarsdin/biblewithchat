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
import java.text.SimpleDateFormat
import java.util.*

class DbControll {

    var simple_std = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val sql전달받은채팅저장 = "insert into Chat values(0,?,?,?,?,?)"


    fun 전달받은채팅저장(jin : JsonObject): Int {
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql전달받은채팅저장)
            pstm.setInt(1, jin.get("chat_room_no").asInt)
            pstm.setInt(2, jin.get("user_no").asInt)
            pstm.setString(3, jin.get("chat_type").asString)
            pstm.setString(4, jin.get("chat_content").asString)
            pstm.setString(5, jin.get("create_date").asString)
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

    fun 현재채팅방읽음여부변경(cliInfo: JsonObject): Int {
        val sql현재채팅방읽음여부변경 = "update ChatIsRead set read_date = ? where chat_no = ? "
        val con = connection
        var pstm: PreparedStatement? = null
        var res = 0
        try {
            pstm = con!!.prepareStatement(sql현재채팅방읽음여부변경)
            pstm.setString(1, simple_std.format( Date(System.currentTimeMillis())))
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

    fun 채팅방목록전달(cliInfo: JsonObject): String {
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
                "from (" +
                "select * " +
                "from ChatRoomInfo cri " +
                "join `User` u on u.user_no = cri.owner_no  " +
                "where group_no = ? " +
                ") f " +
                "join (" +
                "select c.chat_room_no , c.user_no , COUNT(*) unread_count " +
                "from Chat c " +
                "join ChatRoomInfo cri on cri.chat_room_no = c.chat_room_no " +
                "left outer join ChatIsRead cir on cir.chat_no = c.chat_no" +
                "where cri.group_no = ? and c.user_no = ? and cir.read_date is null " +
                "group by c.chat_room_no , c.user_no " +
                ") s " +
                "on f.chat_room_no = s.chat_room_no " +
                "join (" +
                "select c.chat_room_no , MAX(c.create_date) last_msg_date," +
                "(select chat_no from Chat where create_date = MAX(c.create_date) ) last_chat_no ," +
                "(select user_no from Chat where create_date = MAX(c.create_date) ) last_user_no ," +
                "(select user_nick from Chat cc join `User` uu on cc.user_no = uu.user_no  where create_date = MAX(c.create_date) ) last_user_nick ," +
                "(select chat_content from Chat where create_date = MAX(c.create_date) ) last_chat_content " +
                "from Chat c " +
                "join ChatRoomInfo cri on c.chat_room_no = cri.chat_room_no " +
                "where group_no = ? and chat_type in ('문자열', '이미지') " +
                "group by c.chat_room_no" +
                ") e " +
                "on f.chat_room_no = e.chat_room_no  "

        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
        var res = 0
        val jo1 = JsonObject()
        val jo2 = JsonObject()
        val jo = JsonObject()
        try {
            //첫번째 작업
//            pstm = con!!.prepareStatement(sql채팅목록당읽지않은메시지수)
//            pstm.setInt(1, cliInfo.get("group_no").asInt)
//            pstm.setInt(2, cliInfo.get("user_no").asInt)
//            rs = pstm.executeQuery()
//
//            while (rs.next()) {
//                jo1.addProperty("chat_room_no", rs.getString("chat_room_no"))
//                jo1.addProperty("chat_room_title", rs.getString("chat_room_title"))
//                jo1.addProperty("owner_no", rs.getString("owner_no"))
//                jo1.addProperty("create_date", rs.getString("create_date"))
//                jo1.addProperty("chat_room_image", rs.getString("chat_room_image"))
//                jo1.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
//                jo1.addProperty("group_no", rs.getString("group_no"))
//                jo1.addProperty("user_email", rs.getString("user_email"))
//                jo1.addProperty("user_nick", rs.getString("user_nick"))
//                jo1.addProperty("user_name", rs.getString("user_name"))
//                jo1.addProperty("user_image", rs.getString("user_image"))
////                res = rs.getInt("is_joined")
//            }

            //두번째 작업
//            pstm = con!!.prepareStatement(sql채팅목록당가장최신채팅)
//            pstm.setInt(1, cliInfo.get("group_no").asInt)
//            pstm.setInt(2, cliInfo.get("user_no").asInt)
//            rs = pstm.executeQuery()
//
//            while (rs.next()) {
//                jo2.addProperty("chat_room_no", rs.getString("chat_room_no"))
//                jo2.addProperty("chat_room_title", rs.getString("chat_room_title"))
//                jo2.addProperty("owner_no", rs.getString("owner_no"))
//                jo2.addProperty("create_date", rs.getString("create_date"))
//                jo2.addProperty("chat_room_image", rs.getString("chat_room_image"))
//                jo2.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
//                jo2.addProperty("group_no", rs.getString("group_no"))
//                jo2.addProperty("user_email", rs.getString("user_email"))
//                jo2.addProperty("user_nick", rs.getString("user_nick"))
//                jo2.addProperty("user_name", rs.getString("user_name"))
//                jo2.addProperty("user_image", rs.getString("user_image"))
////                res = rs.getInt("is_joined")
//            }

            //마지막 작업
            pstm = con!!.prepareStatement(sql채팅목록전달)
            pstm.setInt(1, cliInfo.get("group_no").asInt)
            pstm.setInt(2, cliInfo.get("group_no").asInt)
            pstm.setInt(3, cliInfo.get("user_no").asInt)
            pstm.setInt(4, cliInfo.get("group_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                jo.addProperty("chat_room_no", rs.getString("chat_room_no"))
                jo.addProperty("chat_room_title", rs.getString("chat_room_title"))
                jo.addProperty("owner_no", rs.getString("owner_no"))
                jo.addProperty("create_date", rs.getString("create_date"))
                jo.addProperty("chat_room_image", rs.getString("chat_room_image"))
                jo.addProperty("chat_room_desc", rs.getString("chat_room_desc"))
                jo.addProperty("group_no", rs.getString("group_no"))
                jo.addProperty("user_email", rs.getString("user_email"))
                jo.addProperty("user_nick", rs.getString("user_nick"))
                jo.addProperty("user_name", rs.getString("user_name"))
                jo.addProperty("user_image", rs.getString("user_image"))
                jo.addProperty("unread_count", rs.getString("unread_count"))
                jo.addProperty("last_msg_date", rs.getString("last_msg_date"))
                jo.addProperty("last_chat_no", rs.getString("last_chat_no"))
                jo.addProperty("last_user_no", rs.getString("last_user_no"))
                jo.addProperty("last_user_nick", rs.getString("last_user_nick"))
                jo.addProperty("last_chat_content", rs.getString("last_chat_content"))
//                res = rs.getInt("is_joined") //last_chat_type 도 고려해보기 - 이미지 알림에 쓸려면...
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            stmtClose(pstm!!)
            if (con != null) {
                conClose(con)
            }
        }
        return jo.toString()
    }


    fun 현재채팅목록갱신(cliInfo: JsonObject): JsonObject {
        val sql현재채팅목록갱신1 = "select * from ChatRoomInfo cri " +
                "join User u on u.user_no = cri.owner_no " +
//                "join ChatRoom cr on cri.chat_room_no = cr.chat_room_no " +
                "where cri.chat_room_no = ? and cri.group_no = ?  "

        val sql현재채팅목록갱신2 = "select * from ChatRoomInfo cri " +
                "join ChatRoom cr on cri.chat_room_no = cr.chat_room_no " +
                "join User u on u.user_no = cr.user_no " +
                "where cri.chat_room_no = ? and cri.group_no = ? "

        val sql현재채팅목록갱신3 = "select c.*, u.*, ci.stored_file_name, ci.chat_image_no " +
                "(select count(*) from ChatRoom cr where cr.chat_room_no = ?) - " +
                "(select count(*) from ChatIsRead cir where cir.chat_no = c.chat_no) unread_count " +
                "                    from Chat c " +
                "                    join `User` u on u.user_no = c.user_no " +
                "                    Left join ChatImage ci on ci.chat_no = c.chat_no " +
                "                    where chat_room_no = ? " +
                "                    order by create_date   "

        val con = connection
        var pstm: PreparedStatement? = null
        var rs : ResultSet? = null
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
            pstm.setInt(1, cliInfo.get("chat_room_no").asInt)
            pstm.setInt(2, cliInfo.get("chat_room_no").asInt)
            rs = pstm.executeQuery()

            while (rs.next()) {
                val tmp = JsonObject()
                tmp.addProperty("chat_no", rs.getString("chat_no"))
                tmp.addProperty("chat_content", rs.getString("chat_room_desc"))
                tmp.addProperty("create_date", rs.getString("create_date"))
                tmp.addProperty("chat_room_no", rs.getString("chat_room_no"))
                tmp.addProperty("user_no", rs.getString("user_no"))  //채팅쓴사람
                tmp.addProperty("user_image", rs.getString("user_image"))
                tmp.addProperty("user_email", rs.getString("user_email"))
                tmp.addProperty("user_nick", rs.getString("user_nick"))
                tmp.addProperty("user_name", rs.getString("user_name"))
                tmp.addProperty("user_create_date", rs.getString("user_create_date"))
                tmp.addProperty("user_image", rs.getString("user_image"))
                tmp.addProperty("unread_count", rs.getString("unread_count"))
                tmp.addProperty("stored_file_name", rs.getString("stored_file_name"))
                tmp.addProperty("chat_image_no", rs.getString("chat_image_no"))
                tmp.addProperty("chat_type", rs.getString("chat_type")) //문자열,이미지, 접속알림, 방나가기
                jo3.add(tmp)
            }

            jo.add("chat_room_info", jo1)
            jo.add("chat_room_userL", jo2)
            jo.add("chat_list", jo3)
            jo.addProperty("cmd", "채팅전달")
            jo.addProperty("cmd_type", "채팅")

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


}