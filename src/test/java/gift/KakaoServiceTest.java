package gift;

import gift.service.KakaoService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;
import java.net.URI;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@ContextConfiguration(classes = KakaoServiceTest.TestConfig.class)
public class KakaoServiceTest {

    @MockBean
    private KakaoProperties kakaoProperties;

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private MockWebServer mockWebServer;

    @Configuration
    static class TestConfig {
        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }

        @Bean
        public KakaoService kakaoService(KakaoProperties kakaoProperties, WebClient.Builder webClientBuilder) {
            return new KakaoService(kakaoProperties, webClientBuilder);
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // Mock KakaoProperties values
        when(kakaoProperties.getClientId()).thenReturn("your_client_id");
        when(kakaoProperties.getRedirectUri()).thenReturn("your_redirect_uri");
        kakaoService = new KakaoService(kakaoProperties, webClientBuilder.baseUrl(mockWebServer.url("/").toString()));
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testGetAccessToken_failure() {
        String authorizationCode = "invalid_code";

        // Mock the response from MockWebServer
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.BAD_REQUEST.value())
            .setBody("{\"error\":\"invalid_grant\"}")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        String accessToken = kakaoService.getAccessToken(authorizationCode);
        assertThat(accessToken).isNull();
    }

    @Test
    public void testGetUserEmail_failure() {
        String accessToken = "invalid_token";

        // Mock the response from MockWebServer
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(HttpStatus.UNAUTHORIZED.value())
            .setBody("{\"error\":\"unauthorized\"}")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        String email = kakaoService.getUserEmail(accessToken);
        assertThat(email).isNull();
    }
}