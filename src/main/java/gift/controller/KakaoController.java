package gift.controller;

import gift.KakaoProperties;
import gift.PasswordEncoder;
import gift.model.User;
import gift.service.KakaoService;
import gift.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;


@Controller
public class KakaoController {


    private final KakaoProperties kakaoProperties;
    private final KakaoService kakaoService;
    private final UserService userService;

    public KakaoController(KakaoProperties kakaoProperties, KakaoService kakaoService, UserService userService) {
        this.kakaoProperties = kakaoProperties;
        this.kakaoService = kakaoService;
        this.userService = userService;
    }

    @GetMapping("/kakao/login")
    public RedirectView kakaoLogin() {
        String url = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=" + kakaoProperties.getClientId() + "&redirect_uri=" + kakaoProperties.getRedirectUri() + "&scope=account_email";
        return new RedirectView(url);
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<String> kakaoCallback(@RequestParam("code") String authorizationCode, HttpSession session) {
        String accessToken = kakaoService.getAccessToken(authorizationCode);
        String email = kakaoService.getUserEmail(accessToken);

        session.setAttribute("accessToken", accessToken);
        session.setAttribute("email", email);


        User user = userService.findByEmail(email);
        if (user == null) {
            user = new User(null, email, PasswordEncoder.encode("1234"));
            userService.save(user);
        }

        return ResponseEntity.ok("Login successful: "+accessToken);
    }
}