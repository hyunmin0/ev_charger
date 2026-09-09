import React, { useState, useCallback } from "react";
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useFocusEffect } from "expo-router";
import * as Location from "expo-location";
import api from "@/lib/api";

const ACCENT = "#5B9CF6";
const ACCENT_BG = "#EBF3FF";

type Station = {
  statId: string;
  statNm: string;
  addr: string;
  useTime: string;
  parkingFree: boolean;
  openToPublic: boolean;
  kind: string;
  busiNm: string;
  totalCount: number;
  availableCount: number;
  allUnavailable: boolean;
  allUnknown: boolean;
  averageRating: number | null;
  reviewCount: number;
};

function getStatus(s: Station) {
  if (s.availableCount > 0) return { label: "이용 가능", color: "#4CAF50", bg: "#F0FBF0" };
  if (s.allUnavailable) return { label: "사용불가", color: "#F44336", bg: "#FFF0F0" };
  if (s.allUnknown) return { label: "상태불명", color: "#aaa", bg: "#f5f5f5" };
  return { label: "충전 중", color: "#FF9800", bg: "#FFF8F0" };
}

function getTags(s: Station) {
  const tags: string[] = [];
  if (s.parkingFree) tags.push("무료 주차");
  tags.push(s.openToPublic ? "개방" : "비개방");
  return tags;
}

export default function FavoritesScreen() {
  const router = useRouter();
  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchFavorites = useCallback(async () => {
    setLoading(true);
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      let lat = 35.1595, lng = 126.8526; // 광주 기본값
      if (status === "granted") {
        const loc = await Location.getCurrentPositionAsync({});
        lat = loc.coords.latitude;
        lng = loc.coords.longitude;
      }
      const res = await api.get("/favorites", { params: { lat, lng } });
      setStations(res.data ?? []);
    } catch {
      setStations([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      fetchFavorites();
    }, [fetchFavorites])
  );

  const removeFavorite = async (statId: string) => {
    Alert.alert("즐겨찾기 해제", "즐겨찾기에서 제거할까요?", [
      { text: "취소", style: "cancel" },
      {
        text: "해제",
        style: "destructive",
        onPress: async () => {
          try {
            await api.delete(`/favorites/${statId}`);
            setStations(prev => prev.filter(s => s.statId !== statId));
          } catch {
            Alert.alert("오류", "다시 시도해주세요.");
          }
        },
      },
    ]);
  };

  const renderItem = ({ item }: { item: Station }) => {
    const st = getStatus(item);
    const tags = getTags(item);
    return (
      <TouchableOpacity
        style={S.card}
        onPress={() => router.push(`/station/${item.statId}` as any)}
        activeOpacity={0.85}
      >
        <View style={S.cardTop}>
          <Text style={S.cardName} numberOfLines={2}>{item.statNm}</Text>
          <Text style={S.operator}>{item.busiNm}</Text>
        </View>
        <Text style={S.hours}>{item.useTime}</Text>
        {item.averageRating != null && (
          <View style={S.ratingRow}>
            <Ionicons name="star" size={13} color="#FFB800" />
            <Text style={S.ratingNum}>{item.averageRating.toFixed(1)}</Text>
            <Text style={S.reviewCount}>({item.reviewCount})</Text>
          </View>
        )}
        <View style={S.tagRow}>
          {tags.map(t => (
            <View key={t} style={S.tag}><Text style={S.tagTxt}>{t}</Text></View>
          ))}
          <View style={S.facilityTag}><Text style={S.facilityTxt}>{item.kind}</Text></View>
        </View>
        <View style={S.cardBottom}>
          <Text style={S.chargerCount}>
            충전기{" "}
            <Text style={{ color: item.availableCount > 0 ? ACCENT : "#aaa", fontWeight: "700" }}>
              {item.availableCount}
            </Text>{" "}
            / {item.totalCount}
          </Text>
          <View style={S.rightRow}>
            <View style={[S.statusBadge, { backgroundColor: st.bg }]}>
              <Text style={[S.statusTxt, { color: st.color }]}>{st.label}</Text>
            </View>
            <TouchableOpacity
              onPress={() => removeFavorite(item.statId)}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Ionicons name="star" size={18} color="#FFB800" />
            </TouchableOpacity>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>즐겨찾기 충전소</Text>
        <View style={S.headerBtn} />
      </View>

      {loading ? (
        <View style={S.center}>
          <ActivityIndicator size="large" color={ACCENT} />
        </View>
      ) : (
        <FlatList
          data={stations}
          keyExtractor={item => item.statId}
          renderItem={renderItem}
          contentContainerStyle={S.listContent}
          ItemSeparatorComponent={() => <View style={S.separator} />}
          ListEmptyComponent={
            <View style={S.empty}>
              <Ionicons name="star-outline" size={48} color="#ddd" />
              <Text style={S.emptyTxt}>즐겨찾기한 충전소가 없어요</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f6fa" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  headerBtn: { padding: 10, width: 44 },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  listContent: { padding: 16 },
  separator: { height: 10 },
  card: {
    backgroundColor: "#fff", borderRadius: 14, padding: 16,
    shadowColor: "#000", shadowOpacity: 0.06, shadowRadius: 6, elevation: 2,
  },
  cardTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 4 },
  cardName: { fontSize: 14, fontWeight: "700", color: "#111", flex: 1, marginRight: 8, lineHeight: 20 },
  operator: { fontSize: 12, color: "#888", marginTop: 2, flexShrink: 0 },
  hours: { fontSize: 12, color: "#999", marginBottom: 6 },
  ratingRow: { flexDirection: "row", alignItems: "center", gap: 4, marginBottom: 8 },
  ratingNum: { fontSize: 12, fontWeight: "700", color: "#333" },
  reviewCount: { fontSize: 12, color: "#aaa" },
  tagRow: { flexDirection: "row", flexWrap: "wrap", gap: 6, marginBottom: 12 },
  tag: { paddingHorizontal: 9, paddingVertical: 3, backgroundColor: ACCENT_BG, borderRadius: 6 },
  tagTxt: { fontSize: 11, color: ACCENT, fontWeight: "500" },
  facilityTag: { paddingHorizontal: 9, paddingVertical: 3, backgroundColor: "#f2f2f2", borderRadius: 6 },
  facilityTxt: { fontSize: 11, color: "#777" },
  cardBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  chargerCount: { fontSize: 13, color: "#555" },
  rightRow: { flexDirection: "row", alignItems: "center", gap: 10 },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  statusTxt: { fontSize: 12, fontWeight: "600" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  empty: { alignItems: "center", paddingTop: 80, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});
