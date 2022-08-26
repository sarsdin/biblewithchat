/*
package com.biblewithchat;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class JDBCTemplate {

    public static Connection getConnection() {
        String driver = "";
        String url = "";
        String id = "";
        String pw = "";

        try {
            Properties prop = new Properties();
            prop.load(new FileReader("driver.properties"));

            driver = prop.getProperty("driver");
            url = prop.getProperty("url");
            id = prop.getProperty("user");
            pw = prop.getProperty("password");


        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        /////////////////////////////
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.out.println("[Error] Driver enrollment fail!");
            e.printStackTrace();
        }

        Connection con = null;
        try {
            con = DriverManager.getConnection(url, id, pw);
            con.setAutoCommit(false); //????????? ??????? commit????? ????:true

        } catch (SQLException e) {
            System.out.println("[Error] Oracle Connection fail!");
            e.printStackTrace();
        }

        return con;  //connection ????? ????????
    }
    //2.????
    public static void conClose(Connection con) {
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("[Error] Connection close fail");
            e.printStackTrace();
        }
    }
    public static void stmtClose(Statement stmt) {
        try {
            stmt.close(); //pstmt?? stmt?? ?????? ?????????? ?θ??????
            //?? close()??? ??????? ??????.
        } catch (SQLException e) {
            System.out.println("[Error] Statement close fail");
            e.printStackTrace();
        }
    }
    public static void rsClose(ResultSet rs) {
        try {
            rs.close();
        } catch (SQLException e) {
            System.out.println("[Error] ResultSet close fail");
            e.printStackTrace();
        }
    }

    //3.????
    public static void commit(Connection con) {
        try {
            con.commit();
            System.out.println("Commit complete!");
        } catch (SQLException e) {
            System.out.println("[Error] Commit fail");
            e.printStackTrace();
        }
    }
    //4.???
    public static void rollback(Connection con) {
        try {
            con.rollback();
            System.out.println("Rollback complete!");
        } catch (SQLException e) {
            System.out.println("[Error] Rollback fail");
            e.printStackTrace();
        }
    }
}
*/
