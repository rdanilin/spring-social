/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.oauth1;

import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.web.client.test.RequestMatchers.*;
import static org.springframework.web.client.test.ResponseCreators.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.test.MockRestServiceServer;

public class OAuth1TemplateTest {

	private static final String ACCESS_TOKEN_URL = "http://www.someprovider.com/oauth/accessToken";
	
	private static final String REQUEST_TOKEN_URL = "https://www.someprovider.com/oauth/requestToken";
	
	private OAuth1Template oauth1;

	@Before
	public void setup() {
		oauth1 = new OAuth1Template("consumer_key", "consumer_secret", REQUEST_TOKEN_URL,
				"https://www.someprovider.com/oauth/authorize?oauth_token={request_token}", ACCESS_TOKEN_URL);
	}

	@Test
	public void buildAuthorizeUrl() {
		assertEquals("https://www.someprovider.com/oauth/authorize?oauth_token=request_token", oauth1.buildAuthorizeUrl("request_token"));
	}

	@Test
	public void fetchNewRequestToken() {
		MockRestServiceServer mockServer = MockRestServiceServer.createServer(oauth1.getRestTemplate());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		mockServer.expect(requestTo("https://www.someprovider.com/oauth/requestToken"))
				.andExpect(method(POST))
				.andExpect(headerContains("Authorization", "oauth_callback=\"http%3A%2F%2Fwww.someclient.com%2Foauth%2Fcallback\""))
				.andExpect(headerContains("Authorization", "oauth_version=\"1.0\""))
				.andExpect(headerContains("Authorization", "oauth_signature_method=\"HMAC-SHA1\""))
				.andExpect(headerContains("Authorization", "oauth_consumer_key=\"consumer_key\""))
				.andExpect(headerContains("Authorization", "oauth_nonce=\""))
				.andExpect(headerContains("Authorization", "oauth_signature=\""))
				.andExpect(headerContains("Authorization", "oauth_timestamp=\""))
				.andRespond(withResponse(new ClassPathResource("requestToken.formencoded", getClass()), responseHeaders));

		OAuthToken requestToken = oauth1.fetchNewRequestToken("http://www.someclient.com/oauth/callback");
		assertEquals("1234567890", requestToken.getValue());
		assertEquals("abcdefghijklmnop", requestToken.getSecret());
	}

	@Test
	public void exchangeForAccessToken() {
		MockRestServiceServer mockServer = MockRestServiceServer.createServer(oauth1.getRestTemplate());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		mockServer
				.expect(requestTo("http://www.someprovider.com/oauth/accessToken"))
				.andExpect(method(POST))
				.andExpect(headerContains("Authorization", "oauth_version=\"1.0\""))
				.andExpect(headerContains("Authorization", "oauth_signature_method=\"HMAC-SHA1\""))
				.andExpect(headerContains("Authorization", "oauth_consumer_key=\"consumer_key\""))
				.andExpect(headerContains("Authorization", "oauth_token=\"1234567890\""))
				.andExpect(headerContains("Authorization", "oauth_verifier=\"verifier\""))
				.andExpect(headerContains("Authorization", "oauth_nonce=\""))
				.andExpect(headerContains("Authorization", "oauth_signature=\""))
				.andExpect(headerContains("Authorization", "oauth_timestamp=\""))
				.andRespond(withResponse(new ClassPathResource("accessToken.formencoded", getClass()), responseHeaders));

		OAuthToken requestToken = new OAuthToken("1234567890", "abcdefghijklmnop");
		OAuthToken accessToken = oauth1.exchangeForAccessToken(new AuthorizedRequestToken(requestToken, "verifier"));
		assertEquals("9876543210", accessToken.getValue());
		assertEquals("ponmlkjihgfedcba", accessToken.getSecret());
	}
}
