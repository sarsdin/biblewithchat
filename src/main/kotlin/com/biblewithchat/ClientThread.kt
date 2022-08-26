package com.biblewithchat

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class ClientThread(socket: Socket?, cliInfo: JsonObject, chatServer: ChatServer) : Thread() {

    val dbCon =  DbControll()

    var cliList = Vector<ClientThread>() //같은 방에 있는 클라이언트 리스트
    lateinit var roomL :HashMap<String, Vector<ClientThread>>
    lateinit var cliInfo :JsonObject //초기화때(처음접속시) 얻어놓은 클라이언트 사용자의 정보 - user_no, user_nick, user_image, cmd, cmd_type
    lateinit var chatServer : ChatServer  //채팅 서버
    var socket: Socket? = null     //원격 클라이언트와 연결된 소켓 객체
    lateinit var msg: String        //이 원격 클라이언트와 같은 방에 연결된 다른 원격 클라이언트(소켓)들에 뿌려줄 문자열 메시지
    lateinit var binaryMsg: String  //이 원격 클라이언트와 같은 방에 연결된 다른 원격 클라이언트(소켓)들에 뿌려줄 byte 메시지(이미지,파일 등)

//    lateinit var rmsg: Array<String>
    private var inMsg: BufferedReader? = null
    var outMsg: PrintWriter? = null

    val gson = GsonBuilder().setPrettyPrinting().setLenient().create() //gson 이쁘게 출력셋팅
    var now: Date = Date(System.currentTimeMillis())
    var simple = SimpleDateFormat("(a hh:mm)") //현재시간에 포맷을 지정


    init {
        //통신소켓을 닫기 위해서 - (스레드 생성할 때) 생성자 매개변수로 소켓을 받아서 멤버변수에 대입
        this.socket = socket
//        this.cliList = cliList
        this.chatServer = chatServer
        this.roomL = chatServer.roomL
        this.cliInfo = cliInfo

        if (socket != null) {
            inMsg = BufferedReader(InputStreamReader(socket.getInputStream()))
            outMsg = PrintWriter(socket.getOutputStream(), true)
        } //예외발생 가능성

    }

    fun 소켓재할당(socket: Socket?){
        this.socket = socket
        if (socket != null) {
            inMsg = BufferedReader(InputStreamReader(socket.getInputStream()))
            outMsg = PrintWriter(socket.getOutputStream(), true)
        }
    }


    override fun run() {
        var status = true
        println("##ClientThread:${name} 생성됨!")
        try {

            while (status) { //수신부
                //여기에 소켓의 null 체크하는 if문을 추가해야함 - 클라 서비스가 재시작되면 소켓이 재생성되기때문
                //접속한 소켓이 방에 담긴 스레드(참가된유저)의 소켓과 일치하지 않게 된다. 정확히 이 스레드의 소켓은 끊킨상황이겠지?
                //그래서 서비스에서 서버로 재접속시 사용한 소켓을 다시 클라이언트 스레드(이곳)를 방번호와 유저번호를 이용해
                //ChatServer에서 찾은 후 접속한 socket을 setter등으로 다시 대입해줘야함(public이면 그냥 넣어주기)
                if (socket == null) {
                    sleep(10)
                    continue
                }
                msg = inMsg!!.readLine()?:""
                if (msg != "") {
                    println("## 쓰레드이름: ${this.name}, 안드로이드클라에서 받은 메시지(cmd): ${msg} ")

                    val jin = JsonParser.parseString(msg).asJsonObject
                    when (jin.get("cmd").asString) {
                        "채팅방접속" -> {  //방에 접속시 실행됨
                            println("## 채팅방접속 실행")
                            채팅방접속시수행(jin)
                        }
                        "채팅전달" ->{
                            println("## 채팅전달 실행")
                            dbCon.전달받은채팅저장(jin)
                            //이 클라가 속한 방의 클라들에게 받은 메시지(json)를 전달해줌
                            //cliList << 이거 지금 방에 속한 스레드에게 보내는 것이지만, 이렇게 말고 db에서 현재 방에 속한 유저들번호
                            //가져와서 그 번호들을 이용해 AllCliMap 에서 클라이언트 스레드들을 가져와 다시 리스트를 만들고 그 리스트에
                            //전달하는게 맞는 것일듯하다. 왜냐? 방에 들어오지 않은 상태라면 방(db)에 소속되있어도 스레드가 추가되어있지
                            //않기 때문이다.. todo 여기 !!!!!!!!!!
                            val receivers: List<String> = dbCon.받을유저리스트작성(jin) //db에서 가져온 비교할 리스트
                            val cliL = chatServer.allCliMap.keys.filter {
                                val rt = receivers.find { it2 ->   //키셋에서 db에서 가져온 리스트와 비교(스레드존재여부확인)
                                    it2 == it
                                }
                                rt != null  //rt값이 존재하면 연결된 스레드가가 있으므로 데이터(알림,채팅)보낼리스트에 추가
                            }.run {
                                val tmpL = Vector<ClientThread>() //임시로 보낼 스레드리스트 작성
                                forEach {
                                    tmpL.add(chatServer.allCliMap.get(it))
                                }
                                tmpL
                            }

                            val sendMsg1 = dbCon.현재채팅목록갱신(jin) // 이건 outMsg 로 나한테만 보내고
//                            outMsg!!.println(sendMsg.toString())
                            val sendMsg2 = dbCon.채팅방목록전달(jin)
                            val sendJo = JsonObject()
                            sendJo.add("chatRoomL", sendMsg1)
                            sendJo.add("chatL", sendMsg2)

                            //통합해서 보내기로 변경: chatL는 GroupChatInnerFm(채팅방안)
                            //chatRoomL은 GroupInChatFm(채팅목록) 에서 각각 필요한 것 선택해서 활용하는 걸로..
                            //어차피 여기로 보내져오는 정보는 group_no, user_no, chat_room_no 이정도면 select 쿼리할 정보도 중복되고..
                            //채팅목록을 핸들러에서 받아 갱신할때는 현재 채팅목록이 보여지는 (소속한)모임의 번호와 채팅이 전달되어온 곳의
                            //group_no랑 일치하는 지만 조건으로 비교하고 일치하면 그 정보를 갱신해주면된다. 그럼 옵져버가 ui 갱신한다.
                            chatServer.broadCast(sendJo.toString(), cliL)

                            println("방의 스레드리스트: ${cliL}")
                            println("방의 0번 스레드 remote socket address: ${cliL[0].socket?.remoteSocketAddress}")
                            println("방의 0번 스레드 정보: id - ${cliL[0].id}, " +
                                    "name - ${cliL[0].name}, isAlive - ${cliL[0].isAlive}, " +
                                    "priority - ${cliL[0].priority}, state - ${cliL[0].state}, " +
                                    "threadGroup - ${cliL[0].threadGroup}")

//                            chatServer.broadCast(msg.toString(), cliL) // 여기로는 내가 쓴 채팅내용만 보내면될듯? 보낸 장소에는 chatL에 add만하고?
//                            chatServer.broadCast(msg, cliList)
//                            println("방의 스레드리스트: ${cliList}")
//                            println("방의 0번 스레드 remote socket address: ${cliList[0].socket?.remoteSocketAddress}")
//                            println("방의 0번 스레드 정보: id - ${cliList[0].id}, " +
//                                    "name - ${cliList[0].name}, isAlive - ${cliList[0].isAlive}, " +
//                                    "priority - ${cliList[0].priority}, state - ${cliList[0].state}, " +
//                                    "threadGroup - ${cliList[0].threadGroup}")


                        }
                        "채팅방목록" ->{
                            println("## 채팅방목록 실행")
                            //group_no 를 이용해 해당하는 모임의 채팅목록 + 마지막 채팅튜플 + 내가 읽지않은 메시지 개수 를 보여줘야함.
//                            chatServer.broadCast(msg, cliList)
                            val sendMsg = dbCon.채팅방목록전달(jin)
                            println("${gson.toJson(sendMsg)}")
                            outMsg!!.println(sendMsg.toString())
                        }
                        "채팅갱신" ->{
                            println("## 채팅갱신 실행")
//                            chatServer.broadCast(msg, cliList)
                            val sendMsg = dbCon.현재채팅목록갱신(jin)
                            println("${gson.toJson(sendMsg)}")
                            outMsg!!.println(sendMsg.toString())
                        }
                        "채팅읽음처리" ->{
                            println("## 채팅읽음처리 실행")
//                            chatServer.broadCast(msg, cliList)
                            /*val sendMsg =*/ dbCon.현재채팅읽음처리(jin)
//                            println("${gson.toJson(sendMsg)}")
//                            outMsg!!.println(sendMsg.toString())
                        }
                        "방나가기" ->{
                            //이 클라가 속한 방에서 탈퇴함 - 방번호 이용해서 db에서 이 참가유저번호 삭제해줌
                            //그리고 나갔다는 메시지 보내줘야함
//                        chatServer.broadCast(msg, cliList)
                        }
                        "방스레드체크" ->{
                            //이 클라가 속한 방에서 탈퇴함 - 방번호 이용해서 db에서 이 참가유저번호 삭제해줌
                            //그리고 나갔다는 메시지 보내줘야함
//                        chatServer.broadCast(msg, cliList)

                        }
                        else -> {
                            println("cmd 가 없다..")
                        }
                    }
                }



            } //while end
            interrupt()
            println("##" + name + "stop!!")

        } catch (e: IOException) {
            try {
                socket?.close()
            } catch (e1: IOException) {
                e.printStackTrace()
            }
            cliList.remove(this)
            e.printStackTrace()
            println("[ClientThread] run() IOException 발생!! or interrupt() 실행됐음")
        }
        println("##" + name + "종료됨~")
    }


    private fun 채팅방접속시수행(cliInfo: JsonObject) {
        val is_joined = dbCon.현재채팅방첫접속여부체크(cliInfo)
        if (is_joined == 0) {
            //방번호에 해당하는 방이 존재하는지부터 확인함
            val isKey = roomL.containsKey(cliInfo.get("chat_room_no").asString)
            if (isKey) {
                //방안에서도 현재 쓰레드가 존재하는지 확인하고 추가해야겠지? 안그러면 중복추가되니까?
                val cliL = roomL.get(cliInfo.get("chat_room_no").asString)
                //방안의 유저리스트에서 유저번호에 해당하는 스레드가 있는지 지금 접속한 유저번호로 비교해서 찾아보고
                //null이라면(유저클라가존재하지 않는다면) 방에 현재 이 클라이언트스레드를 추가해준다.
                val isFind = cliL!!.find {it.cliInfo.get("user_no").asString == cliInfo.get("user_no").asString}
                if (isFind == null) {  //null이 아니면 스레드가 cliL에 존재한다는 거라 추가할 필요없음. 그스레드 재활용하면됨
                    cliL.add(this@ClientThread)
                    this.cliList = cliL //현재 스레드(클라)가 소속된 방의 참조
                }

            //없으면(대게 방처음만들때일듯) 방리스트를 새로 만들고 거기에 현재 클라이언트 스레드를 추가함.
            // 그리고 이 방을 방 해시맵에 새롭게 추가함
            } else {
                val cliL = Vector<ClientThread>().apply { add(this@ClientThread) }
                roomL.put(cliInfo.get("chat_room_no").asString, cliL)
                this.cliList = cliL //현재 스레드(클라)가 소속된 방의 참조
            }

            //db에서 is_joined값을 가져와서 아직 false였다면 true로 바꾸고 조건절를 실행
            //이 조건절은 원래 방에 접속시 소켓을 통해 방을 만들거나 접속하는 로직을 썼다면 이렇게 접속여부 컬럼을 하나 더 두고
            //검사하는 과정이 필요가 없음. 하지만, http 요청으로 방만들기 + 방접속 로직을 짜버려서 어쩔 수 없다...ㅠ

            dbCon.현재채팅방첫접속여부체크변경(cliInfo)
            //채팅 소켓서버와 http 웹서버의 시간이 미묘하게 다를수 있다. 그러면 :s 이부분이 web쪽이 조금더 느릴수도있는데
            //이러면 접속알림을 업데이트하는 소켓채팅서버의 date작업시간이 더 빠르게 기록될수도있다.
            //그러면 접속알림을 보여주는 쿼리가 찰나의 시간차이로 포함되지 않을 수 있음.
            //http웹쪽 서버의 db업데이트 기록시간이 좀더 빨라야 나중에 채팅데이터를 정상적으로 가져올 수 있을듯!
            //그래서 php서버의 user_chat_join_date 값을 insert 시 date()함수의 strtotime() 함수로 -5초를 함
            dbCon.전달받은채팅저장(cliInfo) //접속알림 저장
            chatServer.broadCast(cliInfo.toString(), cliList) //클라에서 접속알림 뷰타입을 띄우는 채팅전달
            
        //위의 경우와는 달리 방에 첫진입이 아니기때문에 방존재확인하고 없으면 만드는 로직은 같되 접속알림은 하지 않음
        } else {
            //방번호에 해당하는 방이 존재하는지부터 확인함
            val isKey = roomL.containsKey(cliInfo.get("chat_room_no").asString)
            if (isKey) {
                //방안에서도 현재 쓰레드가 존재하는지 확인하고 추가해야겠지? 안그러면 중복추가되니까?
                val cliL = roomL.get(cliInfo.get("chat_room_no").asString)
                //방안의 유저리스트에서 유저번호에 해당하는 스레드가 있는지 지금 접속한 유저번호로 비교해서 찾아보고
                //null이라면(유저클라가존재하지 않는다면) 방에 현재 이 클라이언트스레드를 추가해준다.
                val isFind = cliL!!.find {it.cliInfo.get("user_no").asString == cliInfo.get("user_no").asString}
                if (isFind == null) {  //null이 아니면 스레드가 cliL에 존재한다는 거라 추가할 필요없음. 그스레드 재활용하면됨
                    cliL.add(this@ClientThread)
                    this.cliList = cliL //현재 스레드(클라)가 소속된 방의 참조
                }

            //없으면(대게 방처음만들때일듯) 방리스트를 새로 만들고 거기에 현재 클라이언트 스레드를 추가함.
            // 그리고 이 방을 방 해시맵에 새롭게 추가함
            } else {
                val cliL = Vector<ClientThread>().apply { add(this@ClientThread) }
                roomL.put(cliInfo.get("chat_room_no").asString, cliL)
                this.cliList = cliL //현재 스레드(클라)가 소속된 방의 참조
            }
        }
    }



}











/*
//            rmsg = msg.split("/").toTypedArray()
              when (val commend = rmsg[0].toInt()) {
                  LOGIN -> {
                      println(commend)
                      if (chatServer.hash.containsKey(rmsg[1])) { //id로 hashmap에서 중복검사
                          outMsg!!.println(ERR_DUP.toString() + "/" + "[SERVER]" + "/" + "로그인불가>ID 중복")
                          //로그인 한 상대방 채팅창에 로그인 불가 안내메시지 출력
                          socket.close() //소켓 제거
                          cliList.remove(this) //스레드리스트에서 제거
                          status = false // 상태변경으로 while문 탈출
                          break
                      } else {
                          chatServer.hash.put(rmsg[1], this) //중복이 아니면 해당 아이디를 key/ 스레드를 value로 추가
                          chatServer.broadCast(NOMAL.toString() + "/" + "[SERVER]" + "/" + rmsg[1] + "님이 로그인했습니다.")
                          //채팅창에 로그인 메세지 출력
                          chatServer.updatinglist() //변경된 참가자 리스트를 송신
                          break
                      }
                  }
                  LOGOUT -> {
                      chatServer.disconnect(this, rmsg[1]) //해당 스레드와 연결을 해제하는 메서드
                      chatServer.broadCast(NOMAL.toString() + "/" + "[SERVER]" + "/" + rmsg[1] + "님이 종료했습니다.")
                      //나감을 알림
                      chatServer.updatinglist() // 변경된 채팅 참가자 리스트 송신
                      status = false //while문 반복탈출
                  }
                  EXIT -> {
                      chatServer.disconnect(this, rmsg[1])
                      chatServer.broadCast(NOMAL.toString() + "/" + "[SERVER]" + "/" + rmsg[1] + "님과 연결이 끊어졌습니다.")
                      chatServer.updatinglist()
                      status = false
                  }
                  NOMAL -> {
                      chatServer.broadCast(msg)
                  }
                  WISPER -> {
                      val from: ClientThread = chatServer.hash.get(rmsg[1])!! // rmsg[1] 송신 id를 key값으로 value스레드 찾기
                      val to: ClientThread = chatServer.hash.get(rmsg[2])!! // rmsg[2] 수신 id를 key값으로 value스레드 찾기
                      chatServer.wisper(from, to, msg) //찾은 송신 스레드 , 수신 스레드, 내용을 매개변수로 wisper메소드 호출
                  }
                  VAN -> {
                      if (cliList.indexOf(this) != 0) { //0번 스레드에 방장권한을 줌 (방장이 아니라면)
                          outMsg!!.println(
                              NOMAL.toString() + "/" + "[SERVER]" + "/" + "강퇴권한이 없습니다." + simple.format(now)
                          )
                          //해당 스레드에만 권한이 없음을 송신
                          break

                      } else {
                          chatServer.broadCast(NOMAL.toString() + "/" + "[SERVER]" + "/" + rmsg[2] + "님이 강제퇴장하셨습니다.")
                          // 해당 스레드가 방장이라면 강퇴당한 사실을 전체에게 출력
                          val thread: ClientThread = chatServer.hash.get(rmsg[2])
                          //hashmap에서 강퇴할 id로 해당 스레드를 검색해서 thread에 대입
                          thread.outMsg?.println(VAN.toString() + "/")
                          chatServer.disconnect(thread, rmsg[2]) //강퇴당한 스레드 연결해제
                          chatServer.updatinglist()
                          break
                      }
                  }
              }*/