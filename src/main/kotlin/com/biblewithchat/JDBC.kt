package com.biblewithchat

import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.sql.*
import java.util.*

object JDBC {

    val connection: Connection?
        get() {
            var driver = ""
            var url = ""
            var id = ""
            var pw = ""
            try {
                val prop = Properties()
                prop.load(FileReader("driver.properties"))
                driver = prop.getProperty("driver")
                url = prop.getProperty("url")
                id = prop.getProperty("user")
                pw = prop.getProperty("password")

            } catch (e1: FileNotFoundException) {
                e1.printStackTrace()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }

            try {
                Class.forName(driver)
            } catch (e: ClassNotFoundException) {
                println("[Error] Driver enrollment fail!")
                e.printStackTrace()
            }
            var con: Connection? = null
            try {
                con = DriverManager.getConnection(url, id, pw)
                con.autoCommit = false
            } catch (e: SQLException) {
                println("[Error] Oracle Connection fail!")
                e.printStackTrace()
            }
            return con
        }

    //2.????
    fun conClose(con: Connection) {
        try {
            con.close()
        } catch (e: SQLException) {
            println("[Error] Connection close fail")
            e.printStackTrace()
        }
    }

    fun stmtClose(stmt: Statement) {
        try {
            stmt.close() //pstmt?? stmt?? ?????? ?????????? ?θ??????
            //?? close()??? ??????? ??????.
        } catch (e: SQLException) {
            println("[Error] Statement close fail")
            e.printStackTrace()
        }
    }

    fun rsClose(rs: ResultSet) {
        try {
            rs.close()
        } catch (e: SQLException) {
            println("[Error] ResultSet close fail")
            e.printStackTrace()
        }
    }

    //3.????
    fun commit(con: Connection) {
        try {
            con.commit()
            println("Commit complete!")
        } catch (e: SQLException) {
            println("[Error] Commit fail")
            e.printStackTrace()
        }
    }

    //4.???
    fun rollback(con: Connection) {
        try {
            con.rollback()
            println("Rollback complete!")
        } catch (e: SQLException) {
            println("[Error] Rollback fail")
            e.printStackTrace()
        }
    }
}