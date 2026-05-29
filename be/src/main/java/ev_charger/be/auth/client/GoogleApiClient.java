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
public class GoogleApiClient implements OAuthApiClient{

    // https://www.googleapis.com/oauth2/v3/userinfo
    @Value("${google.api-url}")
    private String apiUrl;

    @Override
    public UserInfo getUserInfo(String accessToken) {

        // http 요청을 보낼 수 있는 객체 생성(외부 api 호출 시 사용)
        RestTemplate restTemplate = new RestTemplate();

        // "Authorization: Bearer 토큰값"로 요청
        HttpHeaders headers = new HttpHeaders(); // HTTP 요청 헤더 설정
        headers.set("Authorization", "Bearer " + accessToken);

        // 실제 요청 객체 생성
        // get 요청이므로 body 없이 header만
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // map을 쓰는 이유: json 응답을 키-값으로 쉽게 접근하기 위해
        ResponseEntity<Map<String,Object>> response = restTemplate.exchange( // 서버에 get 요청 보내기 map 형태로 받음
                apiUrl, HttpMethod.GET, request,
                // ParameterizedTypeReference를 사용하여 Map의 타입을 명시
                new ParameterizedTypeReference<Map<String,Object>>() {});

        Map<String, Object> body = response.getBody();
        String id = body.get("sub").toString();

        String email = body.get("email").toString();

        return new UserInfo(id, email);

    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }
}
