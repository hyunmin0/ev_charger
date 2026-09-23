import React, { useState, useEffect, useCallback } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity, Dimensions,
  ActivityIndicator, Alert, Modal, TextInput,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useLocalSearchParams } from "expo-router";
import api from "@/lib/api";

const { width: SCREEN_WIDTH } = Dimensions.get("window");

type CongestionLevel = "여유" | "보통" | "혼잡" | null;

type ChgerStat = "UNKNOWN" | "COMM_ERROR" | "WAITING" | "CHARGING" | "SUSPENDED" | "INSPECTION" | "RESERVED" | "UNCONFIRMED";

type ChgerType =
  | "DC_DEMO" | "AC_SLOW" | "DC_DEMO_AC3" | "DC_COMBO"
  | "DC_DEMO_DE_COMBO" | "DC_DEMO_AC3_DC_COMBO" | "AC3"
  | "DC_COMBO_SLOW" | "NACS" | "DC_COMBO_NACS" | "DC_COMBO2_BUS";

type ChgerDetail = {
  chgerId: string;
  chgerType: ChgerType;
  output: string;
  chgerStat: ChgerStat;
  isAlert: boolean;
};

type ReviewItem = {
  reviewId: number;
  nickname: string;
  profileImageUrl: string | null;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string;
  isMyReview: boolean;
  isEdited: boolean;
};

type StationDetail = {
  statId: string;
  statNm: string;
  addr: string;
  addrDetail: string | null;
  useTime: string;
  parkingFree: boolean | null;
  note: string | null;
  openToPublic: boolean | null;
  limitDetail: string | null;
  kind: string;
  kindDetail: string | null;
  floorNum: string | null;
  floorType: string | null;
  hasFast: boolean;
  busiNm: string;
  busiCall: string | null;
  averageRating: number | null;
  reviewCount: number;
  isFavorite: boolean | null;
  chargers: ChgerDetail[];
  reviews: ReviewItem[];
  congestions: {
    accuracy: number | null;
    oneHour: CongestionLevel;
    twoHour: CongestionLevel;
    threeHour: CongestionLevel;
  } | null;
};

const CHGER_TYPE_LABEL: Record<ChgerType, string> = {
  DC_DEMO: "DC 차데모",
  AC_SLOW: "AC 완속",
  DC_DEMO_AC3: "DC차데모+AC3상",
  DC_COMBO: "DC 콤보",
  DC_DEMO_DE_COMBO: "DC차데모+DC콤보",
  DC_DEMO_AC3_DC_COMBO: "DC차데모+AC3상+DC콤보",
  AC3: "AC 3상",
  DC_COMBO_SLOW: "DC콤보(완속)",
  NACS: "NACS",
  DC_COMBO_NACS: "DC콤보+NACS",
  DC_COMBO2_BUS: "DC콤보2(버스)",
};

const CHGER_STATUS_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  available: { color: "#4CAF50", bg: "#F0FBF0", label: "충전가능" },
  charging:  { color: "#FF9800", bg: "#FFF8F0", label: "충전중"  },
  reserved:  { color: "#F44336", bg: "#FFF0F0", label: "예약중"  },
  unknown:   { color: "#aaa",    bg: "#f5f5f5", label: "상태불명" },
};

function chgerStatKey(stat: ChgerStat) {
  if (stat === "WAITING") return "available";
  if (stat === "CHARGING") return "charging";
  if (stat === "RESERVED") return "reserved";
  return "unknown";
}

const congColor = (level: CongestionLevel) =>
  level === "여유" ? "#4CAF50" : level === "보통" ? "#FF9800" : level === "혼잡" ? "#F44336" : "#aaa";

function formatDate(iso: string) {
  return iso.slice(0, 10).replace(/-/g, ".");
}

export default function StationDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [station, setStation] = useState<StationDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [bookmarked, setBookmarked] = useState(false);
  const [bookmarkLoading, setBookmarkLoading] = useState(false);

  // 리뷰 작성 모달 상태
  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewContent, setReviewContent] = useState("");
  const [reviewSubmitting, setReviewSubmitting] = useState(false);

  useEffect(() => {
    if (!id) return;
    (async () => {
      try {
        const res = await api.get<StationDetail>(`/stations/${id}`);
        setStation(res.data);
        setBookmarked(res.data.isFavorite ?? false);
      } catch {
        Alert.alert("오류", "충전소 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const toggleBookmark = useCallback(async () => {
    if (!station || bookmarkLoading) return;
    setBookmarkLoading(true);
    try {
      if (bookmarked) {
        await api.delete(`/favorites/${station.statId}`);
        setBookmarked(false);
      } else {
        await api.post(`/favorites/${station.statId}`);
        setBookmarked(true);
      }
    } catch {
      Alert.alert("오류", "즐겨찾기 변경에 실패했습니다.");
    } finally {
      setBookmarkLoading(false);
    }
  }, [station, bookmarked, bookmarkLoading]);

  const handleSubmitReview = async () => {
    if (!reviewContent.trim()) {
      Alert.alert("입력 오류", "리뷰 내용을 입력해주세요.");
      return;
    }
    if (!station) return;
    setReviewSubmitting(true);
    try {
      await api.post(`/reviews/${station.statId}`, {
        rating: reviewRating,
        content: reviewContent.trim(),
      });
      // 리뷰 작성 후 데이터 새로고침
      const res = await api.get<StationDetail>(`/stations/${id}`);
      setStation(res.data);
      setReviewModalVisible(false);
      setReviewRating(5);
      setReviewContent("");
      Alert.alert("완료", "리뷰가 등록되었습니다.");
    } catch (e: any) {
      Alert.alert("오류", e?.response?.data?.message ?? "리뷰 등록에 실패했습니다.");
    } finally {
      setReviewSubmitting(false);
    }
  };

  const stationTags: string[] = station ? [
    station.hasFast ? "급속" : "완속",
    ...(station.parkingFree ? ["무료주차"] : []),
    station.openToPublic ? "개방" : "비개방",
    station.kind,
  ] : [];

  const availableCount = station?.chargers.filter(c => chgerStatKey(c.chgerStat) === "available").length ?? 0;

  const congestionItems: { time: string; level: CongestionLevel }[] = station?.congestions ? [
    { time: "1시간 뒤", level: station.congestions.oneHour },
    { time: "2시간 뒤", level: station.congestions.twoHour },
    { time: "3시간 뒤", level: station.congestions.threeHour },
  ] : [];

  if (loading) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.headerBtn}>
            <Ionicons name="chevron-back" size={24} color="#111" />
          </TouchableOpacity>
          <View style={styles.headerCenter} />
          <View style={styles.headerBtn} />
        </View>
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#5B9CF6" />
        </View>
      </SafeAreaView>
    );
  }

  if (!station) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.headerBtn}>
            <Ionicons name="chevron-back" size={24} color="#111" />
          </TouchableOpacity>
          <View style={styles.headerCenter}>
            <Text style={styles.headerTitle}>충전소 정보 없음</Text>
          </View>
          <View style={styles.headerBtn} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle} numberOfLines={1}>{station.statNm}</Text>
          <Text style={styles.headerSub}>{station.busiNm}</Text>
        </View>
        <TouchableOpacity onPress={toggleBookmark} style={styles.headerBtn} disabled={bookmarkLoading}>
          <Ionicons
            name={bookmarked ? "star" : "star-outline"}
            size={22}
            color={bookmarked ? "#FFB800" : "#999"}
          />
        </TouchableOpacity>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        <View style={styles.tagRow}>
          {stationTags.map((t) => (
            <View key={t} style={styles.tag}>
              <Text style={styles.tagText}>{t}</Text>
            </View>
          ))}
        </View>

        <View style={styles.infoCard}>
          <View style={styles.infoRow}>
            <Ionicons name="location-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.addr}{station.addrDetail ? ` ${station.addrDetail}` : ""}</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="time-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.useTime}</Text>
          </View>
          <View style={[styles.infoRow, { borderBottomWidth: 0 }]}>
            <Ionicons name="call-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.busiCall ?? "-"}</Text>
            {station.busiCall ? <Text style={styles.telLink}>전화</Text> : null}
          </View>
        </View>

        <View style={styles.extraCard}>
          {[
            { label: "시설명", value: station.kind },
            {
              label: "이용환경",
              value: [station.floorType, station.floorNum].filter(Boolean).join(" ") || "-",
            },
            { label: "주차요금", value: station.parkingFree ? "무료" : "유료" },
          ].map(({ label, value }, i, arr) => (
            <View key={label} style={[styles.extraRow, i === arr.length - 1 && { borderBottomWidth: 0 }]}>
              <Text style={styles.extraLabel}>{label}</Text>
              <Text style={styles.extraValue}>{value}</Text>
            </View>
          ))}
        </View>

        {congestionItems.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>혼잡도 예측</Text>
            </View>
            <ScrollView horizontal showsHorizontalScrollIndicator={false}>
              <View style={{ flexDirection: "row", gap: 10, paddingVertical: 4 }}>
                {congestionItems.map((item) => (
                  <View key={item.time} style={styles.congItem}>
                    <View style={[styles.congBadge, { backgroundColor: congColor(item.level) }]}>
                      <Text style={styles.congLabel}>{item.level ?? "정보없음"}</Text>
                    </View>
                    <Text style={styles.congTime}>{item.time}</Text>
                  </View>
                ))}
              </View>
            </ScrollView>
          </View>
        )}

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>충전기</Text>
            <Text style={styles.sectionSub}>
              <Text style={{ color: "#5B9CF6", fontWeight: "700" }}>{availableCount}</Text>
              /{station.chargers.length} 이용 가능
            </Text>
          </View>
          <View style={styles.chargerGrid}>
            {station.chargers.map((c) => {
              const key = chgerStatKey(c.chgerStat);
              const cfg = CHGER_STATUS_CONFIG[key];
              return (
                <View key={c.chgerId} style={[styles.chargerCard, { backgroundColor: cfg.bg }]}>
                  <View style={[styles.statusDot, { backgroundColor: cfg.color }]} />
                  <Text style={[styles.chargerStatus, { color: cfg.color }]}>{cfg.label}</Text>
                  <Text style={styles.chargerType}>{CHGER_TYPE_LABEL[c.chgerType] ?? c.chgerType}</Text>
                  <Text style={styles.chargerSpeed}>충전 출력 {c.output ?? "-"}</Text>
                </View>
              );
            })}
          </View>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={styles.ratingRow}>
              <Ionicons name="star" size={14} color="#FFB800" />
              <Text style={styles.ratingText}>
                {station.averageRating != null ? station.averageRating.toFixed(1) : "-"}
              </Text>
              <Text style={styles.reviewCountText}>리뷰 {station.reviewCount}개</Text>
            </View>
            <TouchableOpacity
              style={styles.writeBtn}
              onPress={() => setReviewModalVisible(true)}
            >
              <Text style={styles.writeBtnText}>리뷰 작성</Text>
            </TouchableOpacity>
          </View>
          {station.reviews.map((r) => (
            <View key={r.reviewId} style={styles.reviewCard}>
              <View style={styles.reviewTop}>
                <Text style={styles.reviewAuthor}>{r.nickname}</Text>
                <View style={{ flexDirection: "row", gap: 1 }}>
                  {Array.from({ length: 5 }, (_, i) => (
                    <Ionicons key={i} name={i < r.rating ? "star" : "star-outline"} size={12} color="#FFB800" />
                  ))}
                </View>
                <Text style={styles.reviewDate}>{formatDate(r.createdAt)}</Text>
              </View>
              <Text style={styles.reviewContent}>{r.content}</Text>
            </View>
          ))}
        </View>
      </ScrollView>

      {/* 리뷰 작성 모달 */}
      <Modal
        visible={reviewModalVisible}
        animationType="slide"
        transparent
        onRequestClose={() => setReviewModalVisible(false)}
      >
        <TouchableOpacity
          style={styles.modalBackdrop}
          activeOpacity={1}
          onPress={() => setReviewModalVisible(false)}
        />
        <View style={styles.modalSheet}>
          <View style={styles.modalHandle} />
          <Text style={styles.modalTitle}>리뷰 작성</Text>

          <Text style={styles.modalLabel}>별점</Text>
          <View style={styles.starRow}>
            {[1, 2, 3, 4, 5].map((star) => (
              <TouchableOpacity key={star} onPress={() => setReviewRating(star)}>
                <Ionicons
                  name={star <= reviewRating ? "star" : "star-outline"}
                  size={32}
                  color="#FFB800"
                />
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.modalLabel}>내용</Text>
          <TextInput
            style={styles.modalInput}
            value={reviewContent}
            onChangeText={setReviewContent}
            placeholder="충전소 이용 경험을 남겨주세요"
            placeholderTextColor="#bbb"
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />

          <TouchableOpacity
            style={[styles.modalSubmitBtn, reviewSubmitting && { opacity: 0.6 }]}
            onPress={handleSubmitReview}
            disabled={reviewSubmitting}
          >
            {reviewSubmitting
              ? <ActivityIndicator size="small" color="#fff" />
              : <Text style={styles.modalSubmitTxt}>등록</Text>}
          </TouchableOpacity>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f8f9fb" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  header: { flexDirection: "row", alignItems: "center", backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: "#f0f0f0" },
  headerBtn: { padding: 10 },
  headerCenter: { flex: 1, marginHorizontal: 2 },
  headerTitle: { fontSize: 16, fontWeight: "700", color: "#111" },
  headerSub: { fontSize: 12, color: "#888", marginTop: 1 },
  tagRow: { flexDirection: "row", flexWrap: "wrap", gap: 6, paddingHorizontal: 16, paddingVertical: 12 },
  tag: { paddingHorizontal: 10, paddingVertical: 4, backgroundColor: "#EBF3FF", borderRadius: 6 },
  tagText: { fontSize: 12, color: "#5B9CF6", fontWeight: "500" },
  infoCard: { backgroundColor: "#fff", marginHorizontal: 16, borderRadius: 14, paddingHorizontal: 16, marginBottom: 10, shadowColor: "#000", shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  infoRow: { flexDirection: "row", alignItems: "center", paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  infoIcon: { marginRight: 10 },
  infoText: { flex: 1, fontSize: 13, color: "#333" },
  telLink: { fontSize: 12, color: "#5B9CF6", fontWeight: "600" },
  extraCard: { backgroundColor: "#fff", marginHorizontal: 16, borderRadius: 14, paddingHorizontal: 16, marginBottom: 20, shadowColor: "#000", shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  extraRow: { flexDirection: "row", justifyContent: "space-between", paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  extraLabel: { fontSize: 13, color: "#888" },
  extraValue: { fontSize: 13, color: "#222", fontWeight: "500" },
  section: { marginHorizontal: 16, marginBottom: 24 },
  sectionHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 12 },
  sectionTitle: { fontSize: 16, fontWeight: "700", color: "#111" },
  sectionSub: { fontSize: 13, color: "#555" },
  congItem: { alignItems: "center", gap: 4 },
  congBadge: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12 },
  congLabel: { fontSize: 12, color: "#fff", fontWeight: "600" },
  congTime: { fontSize: 11, color: "#888" },
  chargerGrid: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  chargerCard: { width: (SCREEN_WIDTH - 52) / 2, borderRadius: 12, padding: 14 },
  statusDot: { width: 8, height: 8, borderRadius: 4, marginBottom: 6 },
  chargerStatus: { fontSize: 13, fontWeight: "700", marginBottom: 4 },
  chargerType: { fontSize: 12, color: "#555", marginBottom: 2 },
  chargerSpeed: { fontSize: 11, color: "#888" },
  ratingRow: { flexDirection: "row", alignItems: "center", gap: 4 },
  ratingText: { fontSize: 15, fontWeight: "700", color: "#111" },
  reviewCountText: { fontSize: 13, color: "#888" },
  writeBtn: { paddingHorizontal: 14, paddingVertical: 6, backgroundColor: "#EBF3FF", borderRadius: 8 },
  writeBtnText: { fontSize: 13, color: "#5B9CF6", fontWeight: "600" },
  reviewCard: { backgroundColor: "#fff", borderRadius: 12, padding: 14, marginBottom: 10, shadowColor: "#000", shadowOpacity: 0.04, shadowRadius: 4, elevation: 1 },
  reviewTop: { flexDirection: "row", alignItems: "center", gap: 6, marginBottom: 6 },
  reviewAuthor: { fontSize: 13, fontWeight: "600", color: "#333" },
  reviewDate: { fontSize: 11, color: "#aaa", marginLeft: "auto" as any },
  reviewContent: { fontSize: 13, color: "#555", lineHeight: 20 },
  // 모달
  modalBackdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.3)" },
  modalSheet: { backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 },
  modalHandle: { width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd", alignSelf: "center", marginBottom: 16 },
  modalTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", marginBottom: 20 },
  modalLabel: { fontSize: 13, color: "#555", fontWeight: "600", marginBottom: 8 },
  starRow: { flexDirection: "row", gap: 8, marginBottom: 20 },
  modalInput: { borderWidth: 1.5, borderColor: "#e8e8e8", borderRadius: 10, paddingHorizontal: 14, paddingVertical: 11, fontSize: 14, color: "#222", backgroundColor: "#fafafa", height: 100, marginBottom: 20 },
  modalSubmitBtn: { backgroundColor: "#5B9CF6", borderRadius: 12, height: 50, alignItems: "center", justifyContent: "center" },
  modalSubmitTxt: { color: "#fff", fontSize: 16, fontWeight: "600" },
});