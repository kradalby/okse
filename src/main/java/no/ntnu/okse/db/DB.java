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
 * Created by Trond Walleraunet on 2/25/15.
 * <p>
 * okse is licenced under the MIT licence.
 */

import java.io.File;
import java.sql.*;

public class DB {

    private static Connection con = null;
    private static String dbName = "okse.db";

    /**
     * Connecting to database okse.db
     */
    public static boolean conDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + dbName);
            System.out.println("conDB: Opened database successfully");
            return true;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Static method to switch to test database
     *
     * @param active True if DB is to be set in testing mode, false for normal database
     */
    public static void setTestMode(boolean active) throws Exception {
        if (active) {
            dbName = "test.db";
            File dbfile = new File(dbName);
            if (!dbfile.exists()) System.out.println("Created test db: " + dbfile.createNewFile());
            else {
                dbfile.delete();
                System.out.println("Created test db: " + dbfile.createNewFile());
            }
        } else {
            File dbfile = new File(dbName);
            if (dbfile.exists()) System.out.println("Deleted test db: " + dbfile.delete());
            dbName = "okse.db";
        }
    }

    /**
     * Closing database connection
     */
    public static boolean closeDB() {
        try {
            con.close();
            System.out.println("closeDB: Closed database successfully");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Initiate database, creates tables and default admin user
     */
    public static boolean initDB() {
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

            sql = "CREATE TABLE persistance" +
                    "(topic VARCHAR(100), " +
                    " message TEXT, " +
                    " protocol VARCHAR(50))";
            stmt.executeUpdate(sql);

            System.out.println("initDB: Tables created successfully");

            sql = "INSERT INTO users (username,password,enabled,description) " +
                    "VALUES ('admin','$2a$08$J8jPGNCgrrhc.YoZ05GJQeXx0SKSZotoOKLNGPbazZ..i3uCk/iX.',1,'Administrator')";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO authorities (username,authority) " +
                    "VALUES ('admin','ROLE_ADMIN')";
            stmt.executeUpdate(sql);

            System.out.println("initDB: User created successfully");

            stmt.close();
            con.close();
            return true;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve all users from the database
     *
     * @return A ResultSet containing all users and their fields
     * @throws SQLException If error during query
     */
    public static ResultSet selectAllUsers() throws SQLException {
        conDB();
        String select = "SELECT * FROM users";
        PreparedStatement stmt = con.prepareStatement(select);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    /**
     * Retrieve a single user based on username
     *
     * @param username The username to query
     * @return A ResultSet containing the user fields
     * @throws SQLException If error during query
     */
    public static ResultSet selectUser(String username) throws SQLException {
        conDB();
        String select = "SELECT * FROM users WHERE username = ?";
        PreparedStatement stmt = con.prepareStatement(select);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    /**
     * Inserts a message into the persistance table
     *
     * @param message        The message content
     * @param topic          The topic message was sent on
     * @param originProtocol The protocol the message originated from
     * @return True if the query was successful, false otherwise
     * @throws SQLException If error during query
     */
    public static boolean insertPersistantMessage(String message, String topic, String originProtocol) throws SQLException {
        conDB();
        String insert = "INSERT INTO persistance (topic, message, protocol) VALUES (?, ?, ?)";
        PreparedStatement stmt = con.prepareStatement(insert);
        stmt.setString(1, topic);
        stmt.setString(2, message);
        stmt.setString(3, originProtocol);
        int rows = 0;
        try {
            rows = stmt.executeUpdate();
        } catch (SQLException e) {
            con.rollback();
        }
        if (rows == 1) return true;
        else return false;
    }

    /**
     * Retrieves all messages in persistance storage on a specific topic
     *
     * @param topic The topic to query
     * @return A ResultSet containing all messages on the topic
     * @throws SQLException If error during query
     */
    public static ResultSet getPersistantMessages(String topic) throws SQLException {
        conDB();
        String select = "SELECT * FROM persistance WHERE topic = ?";
        PreparedStatement stmt = con.prepareStatement(select);
        stmt.setString(1, topic);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    /**
     * SQL query, update the password
     *
     * @param username The username
     * @param password (new password)
     */
    public static boolean changePassword(String username, String password) throws SQLException {
        PreparedStatement changePassword = null;
        String changePasswordString =
                "UPDATE users " +
                        "SET password = ? " +
                        "WHERE username = ?";
        conDB();

        try {
            con.setAutoCommit(false);
            changePassword = con.prepareStatement(changePasswordString);
            changePassword.setString(1, password);
            changePassword.setString(2, username);
            changePassword.executeUpdate();
            con.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (con != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    con.rollback();
                    return true;
                } catch (SQLException excep) {
                    excep.printStackTrace();
                    return false;
                }
            }
        } finally {
            if (changePassword != null) {
                changePassword.close();
            }
            con.setAutoCommit(true);
        }
        return false;
    }
}
