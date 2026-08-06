import React, { useState } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity, Dimensions,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useLocalSearchParams } from "expo-router";

const { width: SCREEN_WIDTH } = Dimensions.get("window");

const MOCK_DETAIL = {
  id: "1",
  name: "한국도로교통공단 광주전남지부",
  operator: "교육공공시설",
  tags: ["표", "일반가", "급속", "무료주차", "개방"],
  address: "광주 북구 대천동 100번지 25 제1층",
  hours: "09:00~18:00 (토·일·공휴일 제외)",
  phone: "1522-2573",
  facility: "교육공공시설",
  environment: "실외",
  floor: "지상",
  parking: "무료",
  congestionPrediction: [
    { time: "지금", level: "보통" },
    { time: "1시간 뒤", level: "혼잡" },
    { time: "2시간 뒤", level: "보통" },
    { time: "3시간 뒤", level: "원활" },
    { time: "4시간 뒤", level: "원활" },
  ],
  chargers: [
    { id: "C01", status: "available", type: "DC 콤보", speed: "100kW" },
    { id: "C02", status: "charging", type: "DC 콤보", speed: "100kW" },
    { id: "C03", status: "paying", type: "DC 콤보", speed: "100kW" },
    { id: "C04", status: "available", type: "DC 콤보", speed: "100kW" },
    { id: "C05", status: "charging", type: "DC 콤보", speed: "100kW" },
    { id: "C06", status: "available", type: "DC 콤보", speed: "100kW" },
  ],
  reviews: [
    { id: "r1", author: "홍*동", rating: 4, content: "접근하기 편하고 충전 속도 만족!", date: "2025.06.12" },
    { id: "r2", author: "이*신", rating: 3, content: "주차공간이 좁지만 쓸만함", date: "2025.06.08" },
  ],
  reviewCount: 3,
  rating: 4.5,
};

const STATUS_CONFIG = {
  available: { color: "#4CAF50", bg: "#F0FBF0", label: "충전가능" },
  charging: { color: "#FF9800", bg: "#FFF8F0", label: "충전중" },
  paying: { color: "#F44336", bg: "#FFF0F0", label: "결제중" },
};

const congColor = (level: string) =>
  level === "원활" ? "#4CAF50" : level === "보통" ? "#FF9800" : "#F44336";

export default function StationDetailScreen() {
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();
  const [bookmarked, setBookmarked] = useState(false);

  const station = MOCK_DETAIL;
  const availableCount = station.chargers.filter((c) => c.status === "available").length;

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle} numberOfLines={1}>{station.name}</Text>
          <Text style={styles.headerSub}>{station.operator}</Text>
        </View>
        <TouchableOpacity onPress={() => setBookmarked(!bookmarked)} style={styles.headerBtn}>
          <Ionicons
            name={bookmarked ? "star" : "star-outline"}
            size={22}
            color={bookmarked ? "#FFB800" : "#999"}
          />
        </TouchableOpacity>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        <View style={styles.tagRow}>
          {station.tags.map((t) => (
            <View key={t} style={styles.tag}>
              <Text style={styles.tagText}>{t}</Text>
            </View>
          ))}
        </View>

        <View style={styles.infoCard}>
          <View style={styles.infoRow}>
            <Ionicons name="location-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.address}</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="time-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.hours}</Text>
          </View>
          <View style={[styles.infoRow, { borderBottomWidth: 0 }]}>
            <Ionicons name="call-outline" size={17} color="#5B9CF6" style={styles.infoIcon} />
            <Text style={styles.infoText}>{station.phone}</Text>
            <Text style={styles.telLink}>전화</Text>
          </View>
        </View>

        <View style={styles.extraCard}>
          {[
            { label: "시설명", value: station.facility },
            { label: "이용환경", value: `${station.environment} ${station.floor}` },
            { label: "주차요금", value: station.parking },
          ].map(({ label, value }, i, arr) => (
            <View key={label} style={[styles.extraRow, i === arr.length - 1 && { borderBottomWidth: 0 }]}>
              <Text style={styles.extraLabel}>{label}</Text>
              <Text style={styles.extraValue}>{value}</Text>
            </View>
          ))}
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>혼잡도 예측</Text>
          </View>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={{ flexDirection: "row", gap: 10, paddingVertical: 4 }}>
              {station.congestionPrediction.map((item) => (
                <View key={item.time} style={styles.congItem}>
                  <View style={[styles.congBadge, { backgroundColor: congColor(item.level) }]}>
                    <Text style={styles.congLabel}>{item.level}</Text>
                  </View>
                  <Text style={styles.congTime}>{item.time}</Text>
                </View>
              ))}
            </View>
          </ScrollView>
        </View>

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
              const cfg = STATUS_CONFIG[c.status as keyof typeof STATUS_CONFIG];
              return (
                <View key={c.id} style={[styles.chargerCard, { backgroundColor: cfg.bg }]}>
                  <View style={[styles.statusDot, { backgroundColor: cfg.color }]} />
                  <Text style={[styles.chargerStatus, { color: cfg.color }]}>{cfg.label}</Text>
                  <Text style={styles.chargerType}>{c.type}</Text>
                  <Text style={styles.chargerSpeed}>충전 속도 {c.speed}</Text>
                </View>
              );
            })}
          </View>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={styles.ratingRow}>
              <Ionicons name="star" size={14} color="#FFB800" />
              <Text style={styles.ratingText}>{station.rating}</Text>
              <Text style={styles.reviewCount}>리뷰 {station.reviewCount}개</Text>
            </View>
            <TouchableOpacity style={styles.writeBtn}>
              <Text style={styles.writeBtnText}>리뷰 작성</Text>
            </TouchableOpacity>
          </View>
          {station.reviews.map((r) => (
            <View key={r.id} style={styles.reviewCard}>
              <View style={styles.reviewTop}>
                <Text style={styles.reviewAuthor}>{r.author}</Text>
                <View style={{ flexDirection: "row", gap: 1 }}>
                  {Array.from({ length: 5 }, (_, i) => (
                    <Ionicons key={i} name={i < r.rating ? "star" : "star-outline"} size={12} color="#FFB800" />
                  ))}
                </View>
                <Text style={styles.reviewDate}>{r.date}</Text>
              </View>
              <Text style={styles.reviewContent}>{r.content}</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f8f9fb"},
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
  reviewCount: { fontSize: 13, color: "#888" },
  writeBtn: { paddingHorizontal: 14, paddingVertical: 6, backgroundColor: "#EBF3FF", borderRadius: 8 },
  writeBtnText: { fontSize: 13, color: "#5B9CF6", fontWeight: "600" },
  reviewCard: { backgroundColor: "#fff", borderRadius: 12, padding: 14, marginBottom: 10, shadowColor: "#000", shadowOpacity: 0.04, shadowRadius: 4, elevation: 1 },
  reviewTop: { flexDirection: "row", alignItems: "center", gap: 6, marginBottom: 6 },
  reviewAuthor: { fontSize: 13, fontWeight: "600", color: "#333" },
  reviewDate: { fontSize: 11, color: "#aaa", marginLeft: "auto" as any },
  reviewContent: { fontSize: 13, color: "#555", lineHeight: 20 },
});