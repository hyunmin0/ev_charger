package ev_charger.be.chat;

import ev_charger.be.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatUsageLimiterTest {

    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    // 2026-10-10 23:30 (한국 시간) = 14:30 UTC
    private static final Instant NOW = Instant.parse("2026-10-10T14:30:00Z");
    private static final String TODAY_KEY = "chat:usage:" + USER_ID + ":2026-10-10";

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ChatUsageLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new ChatUsageLimiter(redisTemplate, Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    @Test
    void 오늘_첫_질문이면_키에_자정_이후까지_TTL을_건다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(TODAY_KEY)).willReturn(1L);

        ChatUsageLimiter.Reservation reservation = limiter.acquire(USER_ID);

        assertThat(reservation.key()).isEqualTo(TODAY_KEY);
        assertThat(reservation.remaining()).isEqualTo(ChatUsageLimiter.DAILY_LIMIT - 1);
        // 23:30 → 자정까지 30분 + 여유 1시간
        verify(redisTemplate).expire(TODAY_KEY, Duration.ofMinutes(90));
    }

    @Test
    void 날짜는_서버_시간대가_아니라_한국_날짜_기준() {
        // 2026-10-10 15:30 UTC = 2026-10-11 00:30 (한국 시간)
        limiter = new ChatUsageLimiter(redisTemplate, Clock.fixed(Instant.parse("2026-10-10T15:30:00Z"), ZoneId.of("UTC")));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(1L);

        assertThat(limiter.acquire(USER_ID).key()).isEqualTo("chat:usage:" + USER_ID + ":2026-10-11");
    }

    @Test
    void 두_번째_질문부터는_TTL을_다시_걸지_않는다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(TODAY_KEY)).willReturn(4L);

        assertThat(limiter.acquire(USER_ID).remaining()).isEqualTo(ChatUsageLimiter.DAILY_LIMIT - 4);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void 상한째_질문은_허용하고_남은_횟수는_0() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(TODAY_KEY)).willReturn((long) ChatUsageLimiter.DAILY_LIMIT);

        assertThat(limiter.acquire(USER_ID).remaining()).isZero();
        verify(valueOperations, never()).decrement(anyString());
    }

    @Test
    void 상한을_넘으면_차감을_되돌리고_429용_예외() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(TODAY_KEY)).willReturn((long) ChatUsageLimiter.DAILY_LIMIT + 1);

        assertThatThrownBy(() -> limiter.acquire(USER_ID))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("오늘 챗봇 질문 10회를 모두 사용했어요. 내일 다시 이용해 주세요.");
        verify(valueOperations).decrement(TODAY_KEY);
    }

    @Test
    void 되돌릴_때는_차감한_키를_그대로_쓴다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        limiter.release(new ChatUsageLimiter.Reservation("chat:usage:" + USER_ID + ":2026-10-09", 3));

        verify(valueOperations).decrement("chat:usage:" + USER_ID + ":2026-10-09");
    }

    @Test
    void 오늘_기록이_없으면_사용량_0() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(TODAY_KEY)).willReturn(null);

        assertThat(limiter.used(USER_ID)).isZero();
    }

    @Test
    void 사용량은_0과_상한_사이로_맞춘다() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        given(valueOperations.get(TODAY_KEY)).willReturn("3");
        assertThat(limiter.used(USER_ID)).isEqualTo(3);

        given(valueOperations.get(TODAY_KEY)).willReturn("15");
        assertThat(limiter.used(USER_ID)).isEqualTo(ChatUsageLimiter.DAILY_LIMIT);

        given(valueOperations.get(TODAY_KEY)).willReturn("-1");
        assertThat(limiter.used(USER_ID)).isZero();
    }
}
