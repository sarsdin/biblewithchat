package com.biblewithchat

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class ChatServer {

    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null

//    var allCliList = Vector<ClientThread>() //서버에 존재하는 모든 클라이언트 스레드 리스트
    var allCliMap = HashMap<String, ClientThread>()

    //방번호를 KEY로 해서 '클라이언트가 속한 방의 스레드 리스트' 를 VALUE로 갖고 있는 HASHMAP
    var roomL = HashMap<String, Vector<ClientThread>>()

    var now: Date = Date(System.currentTimeMillis())
    var simple = SimpleDateFormat("(a hh:mm)")
    var simple_std = SimpleDateFormat("yyyy-MM-dd H:mm:ss")
    //채팅내용 옆에 시간을 같이 출력하기 위해서 현재시간에 포맷을 지정

    fun start() {
        try {
            serverSocket = ServerSocket(8088)
            println("채팅서버 시작됨")
            while (true) {
                Thread.sleep(100) //10000명이 동시접속시 모두접속시간은 sleep(1)기준 10초 지연됨. 100이면 1000초(=약17분)
                socket = serverSocket!!.accept()
                println("클라이언트 접속 ip:${socket!!.inetAddress} port:${socket!!.port} localAdress:${socket!!.localAddress}")
                //접속했을때 일단 검사함- 뭐를? allCliMap에 접속한 사용자번호의 키가 존재하는지
                val inMsg = BufferedReader(InputStreamReader(socket!!.getInputStream()))
//                val outMsg = PrintWriter(socket!!.getOutputStream(), true)
                val reader = JsonReader(/*StringReader()*/inMsg).apply { isLenient = true }
                val jin = JsonParser.parseReader(reader)

//                val jin = JsonParser.parseString(inMsg.readLine())
                val cliInfo = jin.asJsonObject
                println("## 처음 소켓으로 접속시 받은 메시지: ${cliInfo} ")
                //클라이언트에서 보내온 유저번호에 해당하는 스레드가 있는지 확인하고 있으면 재활용(재연결), 없으면 만드는 메소드
                클라이언트스레드등록분기(cliInfo)

            }
        } catch (e: IOException) {
            e.printStackTrace()
            println("통신소켓 생성불가")
            if (!serverSocket?.isClosed!!) {
                stopServer()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("서버소켓 생성불가")
            if (!serverSocket?.isClosed!!) {
                stopServer()
            }
        }
    }

    private fun 클라이언트스레드등록분기(cliInfo: JsonObject) {
        //접속에 사용한 새로운 클라이언트의 소켓을 넣어줄 클라이언트 스레드 객체(방에참가한유저)가 존재하는지 방번호와 유저번호로 찾음
        //존재한다면 접속에 사용한 소켓만 재할당해줌
        if (allCliMap.containsKey(cliInfo.get("user_no").asString)) {
            allCliMap.get(cliInfo.get("user_no").asString)!!.소켓재할당(socket)
            println("## Map에 해당클라이언트 스레드객체가 존재함! 이 스레드에 접속소켓을 재할당!: ${cliInfo} ")
    //                    allCliMap[cliInfo.get("user_no").asString] = this //전체클라이언트를 가진 맵에 현재 사용자 번호를 키로 클라이언트스레드 객체를 저장

        //유저번호에 해당하는 클라이언트스레드가 존재하지 않다면 만들어줌    
        } else {
            val cli = ClientThread(socket!!, cliInfo, /*cliList,*/ this)
            //모든 클라이언트 맵에 접속한 클라이언트 스레드를 추가해줌. 방에서 나가기시에도 이맵의 스레드를 제거해주는 것 잊지말기!!
            allCliMap.put(cliInfo.get("user_no").asString, cli) //allCliMap은 유저스레드가 존재하는지 검사하는 용도
            cli.start()

        }
    }

    fun stopServer() {
        try {
//            val iterator = cliList.iterator()
//            //cliList에 있는 스레드 전체를 가져오기 위해 iterator 객체 생성
//            while (iterator.hasNext()) { //다음 객체가 있는 동안
//                val cli = iterator.next() // 다음 객체를 스레드에 대입
//                cli.socket.close() //해당 스레드 통신소켓제거
//                iterator.remove() //스레드 제거
//            }
            if (serverSocket != null && !serverSocket!!.isClosed) {
                serverSocket!!.close() //서버소켓 닫기
            }
            println("채팅서버 종료됨")
        } catch (e: Exception) {
        }
    }

    companion object { //채팅서버 시작 전용
        @JvmStatic
        fun main(args: Array<String>) {
            println("main fun 실행")
            val server = ChatServer()
            server.start()
        }
    }


    fun broadCast(msg: String, cliList: Vector<ClientThread>) { //채팅방 인원 전체출력
        println("broadCast 실행: ${msg}")
        for (cli in cliList) {
            cli.outMsg!!.println(msg /*+ simple.format(now)*/)
            //매개변수로 받은 채팅내용을 시간과 함께 출력
        }
    }

    fun wisper(from: ClientThread, to: ClientThread, msg: String) { //송신그레드,수신스레드,대화내용 매개변수)
        from.outMsg!!.println(msg + simple.format(now)) //송신스레드 채팅창에 출력
        to.outMsg!!.println(msg + simple.format(now)) // 수신스레드 채팅창에 출력
    }

//    fun updatinglist() { //현재 참가자 명단(id)을 클라이언트들에게 뿌려줌
//        val list: Set<String> = roomL.keys // hashmap에서 아이디(key)만 set으로 가져옴
//        for (cli in cliList) {
//            cli.outMsg!!.println("$CPLIST/$list") //CPLIST명령어로 전체에게 출력
//        }
//    }

//    fun disconnect(thread: ClientThread, id: String) {
//        try {
//            thread.socket?.close()
//        } catch (e: IOException) {
//            // TODO Auto-generated catch block
//            e.printStackTrace()
//        }
//        roomL.remove(id) //hashmap에서 제거
//        cliList.remove(thread) //cliList에서 제거
//    }






}