package ev_charger.be.auth.client;

import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.user.enums.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KakaoApiClient implements OAuthApiClient{

    @Value("${kakao.api-url}")  //yml에서 kakao.api-url 값을 읽어옴
    private String apiUrl;

    @Override
    public UserInfo getUserInfo(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders(); // HTTP 요청 헤더 설정
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String,Object>> response = restTemplate.exchange( //카카오 서버에 get 요청 보내기 map 형태로 받음
                apiUrl, HttpMethod.GET, request,
                new ParameterizedTypeReference<Map<String,Object>>() {});

        Map<String,Object> body = response.getBody();
        String id = String.valueOf(body.get("id"));

        Map<String,Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        return new UserInfo(id, email);
    }

    @Override
    public Provider getProvider() { return Provider.KAKAO; }

}