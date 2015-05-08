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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.ResultSet;

import static org.testng.Assert.*;

@Test(singleThreaded = true)
public class DBTest {

    @BeforeMethod
    public void setUp() throws Exception {
        DB.setTestMode(true);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        DB.setTestMode(false);
    }

    @Test
    public void testConDB() throws Exception {
        assertTrue(DB.conDB());
    }

    @Test
    public void testCloseDB() throws Exception {
        assertTrue(DB.closeDB());
    }

    @Test
    public void testInitDB() throws Exception {
        assertTrue(DB.initDB());
    }

    @Test
    public void testChangePassword() throws Exception {
        assertTrue(DB.initDB());
        assertTrue(DB.changePassword("admin", "password"));
    }

    @Test
    public void testSelectAllUsers() throws Exception {
        assertTrue(DB.initDB());
        ResultSet rs = DB.selectAllUsers();
        assertTrue(rs.next());
        assertFalse(rs.next());
    }

    @Test
    public void testSelectUser() throws Exception {
        assertTrue(DB.initDB());
        ResultSet rs = DB.selectUser("admin");
        assertTrue(rs.next());
        assertFalse(rs.next());
    }

    @Test
    public void testInsertPersistantMessage() throws Exception {
        assertTrue(DB.initDB());
        assertTrue(DB.insertPersistantMessage("test", "test", "test"));
    }

    @Test
    public void testGetPersistantMessages() throws Exception {
        assertTrue(DB.initDB());
        assertTrue(DB.insertPersistantMessage("test", "test", "test"));
        ResultSet rs = DB.getPersistantMessages("test");
        assertTrue(rs.next());
        assertFalse(rs.next());
    }
}