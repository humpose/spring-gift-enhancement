package gift;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("kakao.oauth")
public class KakaoProperties {

    private final String clientId;
    private final String redirectUri;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String sendMessageUrl;

    // Constructor
    @ConstructorBinding
    public KakaoProperties(String clientId, String redirectUri, String tokenUrl, String userInfoUrl, String sendMessageUrl) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.sendMessageUrl = sendMessageUrl;
    }

    // Getters
    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getUserInfoUrl() {
        return userInfoUrl;
    }

    public String getSendMessageUrl() {
        return sendMessageUrl;
    }
}