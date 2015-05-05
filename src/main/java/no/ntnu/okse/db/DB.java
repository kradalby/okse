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
 * <p>
 * okse is licenced under the MIT licence.
 */

import java.sql.*;

public class DB {
    private static Connection con = null;

    /**
     * Connecting to database okse.db
     */
    public static void conDB(){
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:okse.db");
            System.out.println("conDB: Opened database successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Closeing database connection
     */
    public static void closeDB(){
        try {
            con.close();
            System.out.println("closeDB: Closed database successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiate database, creates tables and default admin user
     */
    public static void initDB() {
        Statement stmt = null;
        String sql = null;
        conDB();
        try {
            stmt = con.createStatement();

            sql = "CREATE TABLE users" +
                    "(username VARCHAR(50) PRIMARY KEY NOT NULL," +
                    " password VARCHAR(50) NOT NULL ," +
                    " enabled INT NOT NULL ," +
                    " description VARCHAR(50))";
            stmt.executeUpdate(sql);

            sql = "CREATE TABLE authorities" +
                    "(username VARCHAR(50) NOT NULL," +
                    " authority VARCHAR(50) NOT NULL ," +
                    " constraint fk_authorities_users foreign key(username) references users(username))";
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

            sql = "INSERT INTO users (username,password,enabled,description) " +
                    "VALUES ('admin','password',1,'Administrator')";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO authorities (username,authority) " +
                    "VALUES ('admin','ROLE_ADMIN')";
            stmt.executeUpdate(sql);

            System.out.println("initDB: User created successfully");

            stmt.close();
            con.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Run SQL queries
     * @param query An SQL query string.
     * @return A resultset with the returned results
     * @throws SQLException An SQLException instance with returned error from SQLite driver
     */
    public static ResultSet sqlQuery(String query) throws SQLException {
        conDB();
        Statement sta = con.createStatement();
        System.out.println(query);
        return sta.executeQuery(query);
    }

    /**
     * INSERT INTO table (fields) VALUES (values)
     * @param table A string representing the table
     * @param fields A string representing the fields
     * @param values A string representing the values
     */
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

    /**
     * SELECT * FROM table
     * @param table A string representing the table
     * @return A resultSet of all returned results
     */
    public static ResultSet selectAll(String table) {
        Statement stmt = null;
        String query = "SELECT * FROM " + table;
        conDB();
        System.out.println(query);
        try {
            stmt = con.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * SELECT * FROM tavle WHERE colum = value
     * @param table A string representing the table
     * @param colum A string representing the column
     * @param value A string representing the value
     * @return A ResultSet of the returned results
     */
    public static ResultSet select(String table, String colum, String value) {
        Statement stmt = null;
        String query = "SELECT * FROM " + table + " where " + colum + "='" + value + "'";
        conDB();
        System.out.println(query);
        try {
            stmt = con.createStatement();
            return stmt.executeQuery(query);
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
        System.out.println("select: Operation done successfully");
        return null;
    }

    /**
     * SQL querie, update the password
     * @param user (username)
     * @param password (new password)
     */
    public static void changePassword(String user, String password) {
        Statement stmt = null;
        String sql = null;
        conDB();
        try {
            stmt = con.createStatement();
            sql = "UPDATE users SET password='" + password +"' WHERE username='" + user + "'";
            stmt.executeUpdate(sql);
            stmt.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
