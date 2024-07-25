package gift.api;

import gift.KakaoProperties;
import gift.model.User;
import gift.service.KakaoService;
import gift.service.OptionService;
import gift.service.ProductService;
import gift.service.UserService;
import gift.service.WishListService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class OrderController {

    private final OptionService optionService;
    private final ProductService productService;
    private final KakaoProperties kakaoProperties;
    private final WebClient webClient;
    private final UserService userService;
    private final WishListService wishListService;
    private final KakaoService kakaoService;

    public OrderController(OptionService optionService, ProductService productService, KakaoProperties kakaoProperties, WebClient webClient, UserService userService, WishListService wishListService, KakaoService kakaoService) {
        this.optionService = optionService;
        this.productService = productService;
        this.kakaoProperties = kakaoProperties;
        this.webClient = webClient;
        this.userService = userService;
        this.wishListService = wishListService;
        this.kakaoService = kakaoService;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<String> createOrder(@RequestHeader("Authorization") String authorization, @RequestBody OrderRequest orderRequest) {
        // Bearer token 추출
        String token = authorization.replace("Bearer ", "");

        // Option 수량 차감
        boolean updated = optionService.decreaseOptionQuantity(orderRequest.getOptionId(), orderRequest.getQuantity());

        if (!updated) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient product quantity.");
        }

        // 카카오톡 메시지 전송
        boolean messageSent = sendKakaoMessage(token, orderRequest);

        if (!messageSent) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order created but failed to send message.");
        }

        // access token에서 이메일 추출
        String email = kakaoService.getUserEmail(token);

        // 이메일로 사용자 조회
        User user = userService.findByEmail(email);
        if (user != null && wishListService.isProductInWishList(email, orderRequest.getProductId())) {
            // 위시리스트에서 주문한 제품 ID 삭제
            wishListService.removeProductFromWishList(email, orderRequest.getProductId());
        }

        return ResponseEntity.ok("Order created and message sent.");
    }

    private boolean sendKakaoMessage(String accessToken, OrderRequest orderRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        String productName = productService.getProductNameById(orderRequest.getProductId());
        String optionName = optionService.getOptionNameById(orderRequest.getOptionId());
        // 현재 시각을 포맷팅
        LocalDateTime orderDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = orderDateTime.format(formatter);

        String messageContent = String.format(
            "Order Details:\nProduct: %s\nOption: %s\nQuantity: %d\nMessage: %s\nOrder DateTime: %s",
            productName, optionName, orderRequest.getQuantity(), orderRequest.getMessage(), formattedDateTime
        );

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("template_object", createMessagePayload(messageContent));

        return webClient.post()
            .uri(kakaoProperties.getSendMessageUrl())
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .body(BodyInserters.fromFormData(parameters))
            .retrieve()
            .onStatus(status -> status.isError(), response -> {
                // 로그 추가 또는 처리
                return Mono.error(new RuntimeException("Error while sending Kakao message"));
            })
            .bodyToMono(String.class)
            .map(response -> true)
            .onErrorReturn(false)
            .block();
    }

    private String createMessagePayload(String messageContent) {
        JSONObject payload = new JSONObject();
        payload.put("object_type", "text");
        payload.put("text", messageContent);
        JSONObject link = new JSONObject();
        link.put("web_url", "");
        link.put("mobile_web_url", "");
        payload.put("link", link);
        payload.put("button_title", "버튼");
        return payload.toString();
    }
}