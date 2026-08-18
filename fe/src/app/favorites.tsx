import React, { useState } from "react";
import { View, Text, StyleSheet, FlatList, TouchableOpacity } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

const ACCENT = "#5B9CF6";
const ACCENT_BG = "#EBF3FF";

const MOCK_FAVORITES = [
  {
    id: "1",
    name: "제일풍경채센트럴파크1단지입주자대표회의(SP)",
    operator: "파워큐브",
    hours: "24시간 이용가능",
    facility: "공동주택시설",
    tags: ["비개방"],
    available: 0,
    total: 3,
    rating: 2.2,
    reviewCount: 7,
    status: "unavailable",
  },
  {
    id: "2",
    name: "한국도로교통공단 광주전남지부",
    operator: "채비",
    hours: "평일 09:00~18:00 / 주말·공휴일 미개방",
    facility: "공공시설",
    tags: ["무료 주차", "개방"],
    available: 1,
    total: 2,
    rating: 4.1,
    reviewCount: 12,
    status: "available",
  },
  {
    id: "3",
    name: "광주광역시 북구 전남대학교공과대학",
    operator: "GS차지비",
    hours: "24시간 이용가능",
    facility: "교육문화시설",
    tags: ["무료 주차", "개방"],
    available: 3,
    total: 5,
    rating: 3.8,
    reviewCount: 23,
    status: "available",
  },
];

const statusConfig = {
  available: { label: "이용 가능", color: "#4CAF50", bg: "#F0FBF0" },
  unavailable: { label: "사용불가", color: "#F44336", bg: "#FFF0F0" },
};

export default function FavoritesScreen() {
  const router = useRouter();
  const [favorites, setFavorites] = useState(MOCK_FAVORITES.map(s => s.id));

  const toggleFavorite = (id: string) => {
    setFavorites(prev => prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]);
  };

  const renderItem = ({ item }: { item: typeof MOCK_FAVORITES[0] }) => {
    const st = statusConfig[item.status as keyof typeof statusConfig];
    const isFav = favorites.includes(item.id);
    return (
      <TouchableOpacity style={S.card} onPress={() => router.push(`/station/${item.id}` as any)} activeOpacity={0.85}>
        <View style={S.cardTop}>
          <Text style={S.cardName} numberOfLines={2}>{item.name}</Text>
          <Text style={S.operator}>{item.operator}</Text>
        </View>
        <Text style={S.hours}>{item.hours}</Text>
        <View style={S.ratingRow}>
          <Ionicons name="star" size={13} color="#FFB800" />
          <Text style={S.ratingNum}>{item.rating}</Text>
          <Text style={S.reviewCount}>({item.reviewCount})</Text>
        </View>
        <View style={S.tagRow}>
          {item.tags.map(t => (
            <View key={t} style={S.tag}><Text style={S.tagTxt}>{t}</Text></View>
          ))}
          <View style={S.facilityTag}><Text style={S.facilityTxt}>{item.facility}</Text></View>
        </View>
        <View style={S.cardBottom}>
          <Text style={S.chargerCount}>
            충전기 <Text style={{ color: item.available > 0 ? ACCENT : "#aaa", fontWeight: "700" }}>{item.available}</Text> / {item.total}
          </Text>
          <View style={S.rightRow}>
            <View style={[S.statusBadge, { backgroundColor: st.bg }]}>
              <Text style={[S.statusTxt, { color: st.color }]}>{st.label}</Text>
            </View>
            <TouchableOpacity onPress={() => toggleFavorite(item.id)} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
              <Ionicons name={isFav ? "star" : "star-outline"} size={18} color={isFav ? "#FFB800" : "#ccc"} />
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
      <FlatList
        data={MOCK_FAVORITES}
        keyExtractor={item => item.id}
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
  empty: { alignItems: "center", paddingTop: 80, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});