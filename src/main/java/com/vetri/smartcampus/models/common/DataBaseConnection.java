package com.vetri.smartcampus.models.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DataBaseConnection {

    public static Connection getConnection(){
        String url = "jdbc:mysql://localhost:3306/smartcampus";
        String dbUser = "root";
        String dbPass = "root";

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, dbUser, dbPass);
            return con;

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static PreparedStatement getPreparedStatement(Connection con, String sql){
        try{
            return con.prepareStatement(sql);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
