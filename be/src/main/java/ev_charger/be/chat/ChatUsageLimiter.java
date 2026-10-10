package ev_charger.be.chat;

import ev_charger.be.common.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

// 유저별 하루(한국 날짜) 챗봇 질문 수를 Redis로 셈 (OpenAI 비용 상한)
@Component
public class ChatUsageLimiter {

    static final int DAILY_LIMIT = 10;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String KEY_PREFIX = "chat:usage:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Clock clock;

    @Autowired
    public ChatUsageLimiter(RedisTemplate<String, String> redisTemplate) {
        this(redisTemplate, Clock.system(KST));
    }

    ChatUsageLimiter(RedisTemplate<String, String> redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    // 차감한 키를 같이 들고 있어야 자정을 넘겨 응답이 와도 같은 날짜의 차감을 되돌릴 수 있음
    public record Reservation(String key, int remaining) {}

    /**
     * 질문 1회를 차감
     * @return 차감 후 오늘 남은 횟수
     * @throws TooManyRequestsException 오늘 상한을 이미 다 쓴 경우 (차감하지 않음)
     */
    public Reservation acquire(UUID userId) {
        String key = key(userId);
        Long incremented = redisTemplate.opsForValue().increment(key);
        long used = incremented == null ? 1 : incremented;
        if (used == 1) {
            redisTemplate.expire(key, untilTomorrow());
        }
        if (used > DAILY_LIMIT) {
            redisTemplate.opsForValue().decrement(key);
            throw new TooManyRequestsException(
                    "오늘 챗봇 질문 " + DAILY_LIMIT + "회를 모두 사용했어요. 내일 다시 이용해 주세요.");
        }
        return new Reservation(key, DAILY_LIMIT - (int) used);
    }

    // 답변을 받지 못했을 때 차감을 되돌림
    public void release(Reservation reservation) {
        redisTemplate.opsForValue().decrement(reservation.key());
    }

    public int used(UUID userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        int used = value == null ? 0 : Integer.parseInt(value);
        return Math.max(0, Math.min(used, DAILY_LIMIT));
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.ofInstant(clock.instant(), KST);
    }

    // 날짜가 키에 들어 있어서 TTL은 정리용. 자정 직후 요청이 지난 키를 다시 만들지 않도록 1시간 여유
    private Duration untilTomorrow() {
        ZonedDateTime now = ZonedDateTime.ofInstant(clock.instant(), KST);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
        return Duration.between(now, nextMidnight).plusHours(1);
    }
}
