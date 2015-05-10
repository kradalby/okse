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
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package no.ntnu.okse.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import no.ntnu.okse.Application;
import no.ntnu.okse.core.CoreService;
import no.ntnu.okse.core.Utilities;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import no.ntnu.okse.web.model.Log;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.RequestBuilder;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.*;


@SpringApplicationConfiguration(classes = { Server.class })
@WebIntegrationTest("server.port:0")
@DirtiesContext
@Test(singleThreaded = true, threadPoolSize = 0, sequential = true)
public class SpringApplicationTest extends AbstractTestNGSpringContextTests {

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "password";

    @Value("${local.server.port}")
    private int port;


    @Test
    public void testIndex() throws Exception {
        ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
                "http://localhost:" + this.port, String.class);
        assertEquals(entity.getStatusCode(), HttpStatus.OK);

        //assertEquals("Hello World", entity.getBody());
    }

    @Test
    public void testVerifyCSRFExist() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
        ResponseEntity<String> entity = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/", HttpMethod.GET,
                new HttpEntity<Void>(headers), String.class);
        assertEquals(entity.getStatusCode(), HttpStatus.OK);
        assertTrue(entity.getBody().contains("_csrf"));
    }

    @Test
    public void testLogin() throws Exception {
        HttpHeaders headers = getHeaders();
        headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.set("username", DEFAULT_USER);
        form.set("password", DEFAULT_PASSWORD);
        ResponseEntity<String> entity = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/auth/login", HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(form, headers),
                String.class);
        assertEquals(entity.getStatusCode(), HttpStatus.FOUND);
        assertTrue(entity.getHeaders().getLocation().toString().endsWith(port + "/"));
        //assertNotNull(entity.getHeaders().get("Set-Cookie"));
    }

    @Test
    public void testTopicGetAll() throws Exception {

        class Obj {
            private int subscribers;
            private String topic;

            public int getSubscribers() {
                return subscribers;
            }

            public void setSubscribers(int subscribers) {
                this.subscribers = subscribers;
            }

            public String getTopic() {
                return topic;
            }

            public void setTopic(String topic) {
                this.topic = topic;
            }
        }

        TopicService ts = TopicService.getInstance();
        ts.boot();

        int topics = 5;

        for (int i = 1; i < topics+1; i++) {
            ts.addTopic("Topic" + i);
        }

        Thread.sleep(100);

        HttpHeaders headers = getHeaders();

        String cookie = getLoginHeader().getFirst("Set-Cookie");
        headers.set("Cookie", cookie);

        ResponseEntity<String> log = doGet("/api/topic/get/all/", headers);

        assertEquals(HttpStatus.OK, log.getStatusCode());
        assertTrue(Utilities.isJSONValid(log.getBody()));


        Gson gson = new GsonBuilder().create();

        Type listOfTestObject = new TypeToken<List<Obj>>(){}.getType();
        List<Obj> list = gson.fromJson(log.getBody(), listOfTestObject);

        assertEquals(ts.getAllTopics().size(), list.size());
        assertEquals(ts.getAllTopics().size(), topics);
        assertEquals(list.size(), topics);

        ts.stop();
        ts = null;
    }


    @Test
    public void testTopicDeleteAllTopic() throws Exception {

        TopicService ts = TopicService.getInstance();
        ts.boot();

        int topics = 5;

        for (int i = 1; i < topics+1; i++) {
            ts.addTopic("Topic" + i);
        }

        Thread.sleep(200);


        String url = "http://localhost:" + this.port + "/api/topic/delete/all";
        HttpHeaders headers = getHeaders();
        HttpHeaders loginHeaders = getLoginHeader();

        String cookie = loginHeaders.getFirst("Set-Cookie");
        headers.set("Cookie", cookie);

        String csrf = getCSRF(headers);
        headers.set("X-CSRF-TOKEN", csrf);

        String urlWithCSRF = url + "?_csrf=" + csrf;

        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
//            @Override
//            protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
//                if (HttpMethod.DELETE == httpMethod) {
//                    return new HttpEntityEnclosingDeleteRequest(uri);
//                }
//                return super.createHttpUriRequest(httpMethod, uri);
//            }
//        });

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                urlWithCSRF,
                HttpMethod.DELETE,
                entity,
                String.class
        );


        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(Utilities.isJSONValid(response.getBody()));

        assertNotEquals(ts.getAllTopics().size(), topics);
        assertEquals(ts.getAllTopics().size(), 0);

        ts.stop();
        ts = null;
    }
    @Test
    public void testTopicDeleteSingleTopic() throws Exception {

        TopicService ts = TopicService.getInstance();
        ts.boot();

        int topics = 5;

        for (int i = 1; i < topics+1; i++) {
            ts.addTopic("Topic" + i);
        }

        Thread.sleep(200);

        Topic t = ts.getTopic("Topic2");

        Thread.sleep(100);

        String url = "http://localhost:" + this.port + "/api/topic/delete/single";
        HttpHeaders headers = getHeaders();
        HttpHeaders loginHeaders = getLoginHeader();

        String cookie = loginHeaders.getFirst("Set-Cookie");
        //String csrf = loginHeaders.getFirst("X-CSRF-TOKEN");
        headers.set("Cookie", cookie);

        String csrf = getCSRF(headers);
        headers.set("X-CSRF-TOKEN", csrf);

//        ResponseEntity<String> derp = doGet("/", headers);
//        System.out.println(derp.getHeaders().keySet());


        String urlWithID = url + "?topicID=" + t.getTopicID();
        String urlWithIDAndCSRF = urlWithID + "&_csrf=" + csrf;

        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
//            @Override
//            protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
//                if (HttpMethod.DELETE == httpMethod) {
//                    return new HttpEntityEnclosingDeleteRequest(uri);
//                }
//                return super.createHttpUriRequest(httpMethod, uri);
//            }
//        });

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                urlWithIDAndCSRF,
                HttpMethod.DELETE,
                entity,
                String.class
        );

        System.out.println(entity.getBody());

        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(Utilities.isJSONValid(response.getBody()));

        assertEquals(ts.getAllTopics().size(), topics-1);

        ts.stop();
        ts = null;
    }


    @Test
    public void testLogAPIJSON() throws Exception {
        HttpHeaders headers = getHeaders();

        String cookie = getLoginHeader().getFirst("Set-Cookie");
        headers.set("Cookie", cookie);

        ResponseEntity<String> log = doGet("/api/log/", headers);

        assertEquals(log.getStatusCode(), HttpStatus.OK);
        assertTrue(Utilities.isJSONValid(log.getBody()));
    }

    @Test
    public void testLogFilesAPIJSON() throws Exception {
        HttpHeaders headers = getHeaders();

        String cookie = getLoginHeader().getFirst("Set-Cookie");
        headers.set("Cookie", cookie);

        ResponseEntity<String> log = doGet("/api/log/", headers);

        assertEquals(log.getStatusCode(), HttpStatus.OK);
        assertTrue(Utilities.isJSONValid(log.getBody()));
    }

    private String getCSRF(HttpHeaders headers) {
        ResponseEntity<String> page = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/auth/password/",
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                String.class
        );
        assertEquals(HttpStatus.OK, page.getStatusCode());
        String cookie = page.getHeaders().getFirst("Set-Cookie");
        Matcher matcher = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*")
                .matcher(page.getBody());
        assertTrue(matcher.matches());
        return matcher.group(1);
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> page = new TestRestTemplate().getForEntity(
                "http://localhost:" + this.port + "/",
                String.class);
        assertEquals(HttpStatus.OK, page.getStatusCode());
        String cookie = page.getHeaders().getFirst("Set-Cookie");
        headers.set("Cookie", cookie);
        Matcher matcher = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*")
                .matcher(page.getBody());
        assertTrue(matcher.matches());
        headers.set("X-CSRF-TOKEN", matcher.group(1));
        return headers;
    }

    private HttpHeaders getLoginHeader() {
        HttpHeaders headers = getHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.set("username", DEFAULT_USER);
        form.set("password", DEFAULT_PASSWORD);
        ResponseEntity<String> login = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, String>>(form, headers),
                String.class);

        HttpHeaders h = new HttpHeaders();
        login.getHeaders().keySet().forEach(key -> {
            h.add(key, login.getHeaders().getFirst(key));
        });
        h.add("X-CSRF-TOKEN", headers.getFirst("X-CSRF-TOKEN"));

        return h;
    }


    private ResponseEntity<String> doGet(String endPoint, HttpHeaders headers) {
        ResponseEntity<String> response = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + endPoint,
                HttpMethod.GET,
                new HttpEntity<String>(headers),
                String.class
        );
        return response;

    }

//    public static class HttpEntityEnclosingDeleteRequest extends HttpEntityEnclosingRequestBase {
//
//        public HttpEntityEnclosingDeleteRequest(final URI uri) {
//            super();
//            setURI(uri);
//        }
//
//        @Override
//        public String getMethod() {
//            return "DELETE";
//        }
//    }
}