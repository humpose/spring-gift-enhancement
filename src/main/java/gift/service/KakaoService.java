package gift.service;

import gift.KakaoProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class KakaoService {

    private final KakaoProperties kakaoProperties;
    private final WebClient webClient;

    public KakaoService(KakaoProperties kakaoProperties, WebClient.Builder webClientBuilder) {
        this.kakaoProperties = kakaoProperties;
        this.webClient = webClientBuilder.baseUrl("https://kauth.kakao.com").build();
    }

    public String getAccessToken(String authorizationCode) {
        try {
            String url = "/oauth/token";
            String response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                    .with("client_id", kakaoProperties.getClientId())
                    .with("redirect_uri", kakaoProperties.getRedirectUri())
                    .with("code", authorizationCode))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return extractAccessToken(response);
        } catch (WebClientResponseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getUserEmail(String accessToken) {
        try {
            String url = kakaoProperties.getUserInfoUrl();
            String response = webClient.get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return extractEmail(response);
        } catch (WebClientResponseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractAccessToken(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractEmail(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getJSONObject("kakao_account").getString("email");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}