/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.db;

/**
 * Created by Aleksander Skraastad (myth) on 2/25/15.
 * <p/>
 * okse is licenced under the MIT licence.
 */

import java.sql.*;

public class DB {
    private static Connection con = null;

    public static void main(String args[]){
        //conDB();
        //initDB();
        //select("users", "username", "admin");
    }

    //Eksprementering
    public static ResultSet sqlQuerry(String sql) throws SQLException {
        Statement sta = con.createStatement();
        sql = "SELECT * FROM users where username='admin'";
        return sta.executeQuery(sql);
    }

    //Connectiong to database okse.db
    public static void conDB(){
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:okse.db");
            System.out.println("conDB: Opened database successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    //Init database, creates tables and default admin user
    public static void initDB() {
        //Connection con = null;
        Statement stmt = null;
        String sql = null;
        conDB();
        try {
            stmt = con.createStatement();
            sql = "CREATE TABLE users" +
                    "(id INT PRIMARY KEY NOT NULL," +
                    " username TEXT NOT NULL, " +
                    " password TEXT NOT NULL ," +
                    " description CHAR(50))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE presistance" +
                    "(topic CHAR(100), " +
                    " message CHAR(200) ," +
                    " protocol CHAR(50))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE presets" +
                    "(topic CHAR(100), " +
                    " dialect CHAR(50))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE stats" +
                    "(stat CHAR(100), " +
                    " counter INT)";
            stmt.executeUpdate(sql);

            System.out.println("initDB: Tables created successfully");

            sql = "INSERT INTO users (id,username,password,description) " +
                    "VALUES (1,'admin','admin','Administrator')";
            stmt.executeUpdate(sql);

            System.out.println("initDB: User created successfully");

            stmt.close();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void insert(String table, String fields, String values) {
        Statement stmt = null;
        String query = "INSERT INTO " + table + " (" + fields + ") " +
                "VALUES (" + values + ")";
        conDB();
        System.out.println(query);
        try {
            stmt = con.createStatement();
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static ResultSet selectAll(String table) {
        Statement stmt = null;
        String query = "SELECT * FROM " + table;
        conDB();
        try {
            stmt = con.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ResultSet select(String table, String colum, String value) {
        conDB();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery( "SELECT * FROM " + table + " where " + colum + "='" + value + "'");
            //For testing
            int id = rs.getInt("id");
            String usr = rs.getString("username");
            String pwd = rs.getString("password");
            String desc = rs.getString("description");
            System.out.println("selectDB: ");
            System.out.println(id + " - " + usr + " - " + pwd + " - " + desc);
            //Slutt testing
            return stmt.executeQuery( "SELECT * FROM " + table + " where " + colum + "='" + value + "'");

            //ResultSet rs = stmt.executeQuery( "SELECT * FROM users;" );
//            while ( rs.next() ) {
//                int id = rs.getInt("id");
//                String  name = rs.getString("name");
//                System.out.println( "ID = " + id );
//                System.out.println( "NAME = " + name );
//            }
            //rs.close();
            //stmt.close();
            //con.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
        System.out.println("select: Operation done successfully");
        return null;
    }
}
