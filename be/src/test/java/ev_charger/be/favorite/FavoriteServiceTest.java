package ev_charger.be.favorite;

import ev_charger.be.station.Station;
import ev_charger.be.station.StationRepository;
import ev_charger.be.station.congestion.CongestionLevel;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private StationRepository stationRepository;

    private FavoriteService favoriteService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        favoriteService = new FavoriteService(favoriteRepository, stationRepository);

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void 즐겨찾기_추가_성공() {
        // given
        String statId = "ST1";
        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .build();

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(favoriteRepository.existsByUserAndStation(user, station)).willReturn(false);

        // when
        favoriteService.addFavorite(user, statId);

        // then
        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(captor.capture());

        Favorite saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getStation()).isEqualTo(station);
    }

    @Test
    void 유효하지_않은_충전소면_추가시_예외_발생() {
        // given
        String statId = "ST1";
        given(stationRepository.findById(statId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteService.addFavorite(user, statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 충전소입니다.");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void 이미_등록된_충전소면_추가시_예외_발생() {
        // given
        String statId = "ST1";
        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .build();

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(favoriteRepository.existsByUserAndStation(user, station)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> favoriteService.addFavorite(user, statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 충전소입니다.");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void 즐겨찾기_삭제_성공() {
        // given
        String statId = "ST1";
        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .build();

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));

        // when
        favoriteService.deleteFavorite(user, statId);

        // then
        verify(favoriteRepository).deleteByUserAndStation(user, station);
    }

    @Test
    void 존재하지_않는_충전소면_삭제시_예외_발생() {
        // given
        String statId = "ST1";
        given(stationRepository.findById(statId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteService.deleteFavorite(user, statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("즐겨찾기에 존재하지 않는 충전소입니다.");

        verify(favoriteRepository, never()).deleteByUserAndStation(any(), any());
    }

    @Test
    void 즐겨찾기_목록_조회_성공() {
        // given
        UUID userId = UUID.randomUUID();
        User mockUser = mock(User.class);
        given(mockUser.getUserId()).willReturn(userId);

        double lat = 37.5;
        double lng = 127.0;

        StationResponse response = new StationResponse(
                "ST1", "테스트충전소", "서울시 강남구", 37.5, 127.0, "24시간",
                true, true, "공공시설", "지상",
                true, "테스트운영사", 4, 2,
                true, false, false,
                4.5, 3, 120.0, CongestionLevel.NORMAL
        );

        given(stationRepository.findFavoriteStations(userId, lat, lng)).willReturn(List.of(response));

        // when
        List<StationResponse> responses = favoriteService.getFavoriteList(mockUser, lat, lng);

        // then
        assertThat(responses).containsExactly(response);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
