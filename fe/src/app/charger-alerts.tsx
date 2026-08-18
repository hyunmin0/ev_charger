import React, { useState } from "react";
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

type Charger = { id: string; status: "충전중" | "대기중" | "고장" | "사용불가"; type: string; kw: string; alertOn: boolean };
type Station = { id: string; name: string; operator: string; hours: string; facility: string; tags: string[]; chargers: Charger[] };

const STATUS_CFG = {
  충전중: { bg: "#FFF8E1", color: "#F59E0B" },
  대기중: { bg: "#E8F5E9", color: "#4CAF50" },
  고장: { bg: "#FFF0F0", color: "#F44336" },
  사용불가: { bg: "#f2f2f2", color: "#999" },
};

const INITIAL_STATIONS: Station[] = [
  {
    id: "1", name: "한국도로교통공단 광주전남지부", operator: "파워큐브",
    hours: "24시간 이용가능", facility: "공공주차시설", tags: ["비개방"],
    chargers: [
      { id: "C01", status: "충전중", type: "DC 콤보", kw: "100kW", alertOn: false },
      { id: "C02", status: "충전중", type: "DC 콤보", kw: "100kW", alertOn: false },
      { id: "C03", status: "충전중", type: "DC 콤보", kw: "100kW", alertOn: true },
    ],
  },
  {
    id: "2", name: "광주광역시 북구 전남대학교공과대학", operator: "GS차지비",
    hours: "24시간 이용가능", facility: "교육문화시설", tags: ["무료 주차", "개방"],
    chargers: [
      { id: "C01", status: "대기중", type: "DC 콤보", kw: "100kW", alertOn: true },
      { id: "C02", status: "충전중", type: "AC 완속", kw: "7kW", alertOn: false },
    ],
  },
  {
    id: "3", name: "재일풍경채센트럴파크1단지입주자대표회의(SP)", operator: "파워큐브",
    hours: "24시간 이용가능", facility: "공동주택시설", tags: ["비개방"],
    chargers: [
      { id: "C01", status: "사용불가", type: "DC 콤보", kw: "50kW", alertOn: false },
      { id: "C02", status: "사용불가", type: "DC 콤보", kw: "50kW", alertOn: false },
      { id: "C03", status: "사용불가", type: "DC 콤보", kw: "50kW", alertOn: true },
    ],
  },
];

export default function ChargerAlertsScreen() {
  const router = useRouter();
  const [stations, setStations] = useState(INITIAL_STATIONS);

  const toggleAlert = (stationId: string, chargerId: string) => {
    setStations(prev => prev.map(s =>
      s.id !== stationId ? s : {
        ...s,
        chargers: s.chargers.map(c => c.id !== chargerId ? c : { ...c, alertOn: !c.alertOn }),
      }
    ));
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>충전기 알림 관리</Text>
        <View style={S.headerBtn} />
      </View>

      <ScrollView contentContainerStyle={S.scroll}>
        {stations.map(station => (
          <View key={station.id} style={S.card}>
            <View style={S.stationTop}>
              <Text style={S.stationName} numberOfLines={2}>{station.name}</Text>
              <Text style={S.operator}>{station.operator}</Text>
            </View>
            <Text style={S.hours}>{station.hours}</Text>
            <View style={S.tagRow}>
              {station.tags.map(t => (
                <View key={t} style={S.tag}><Text style={S.tagTxt}>{t}</Text></View>
              ))}
              <View style={S.facilityTag}><Text style={S.facilityTxt}>{station.facility}</Text></View>
            </View>
            <View style={S.divider} />
            {station.chargers.map((charger, idx) => {
              const cfg = STATUS_CFG[charger.status];
              return (
                <View key={charger.id} style={[S.chargerRow, idx < station.chargers.length - 1 && S.chargerBorder]}>
                  <View style={[S.statusBadge, { backgroundColor: cfg.bg }]}>
                    <Text style={[S.statusTxt, { color: cfg.color }]}>{charger.status}</Text>
                  </View>
                  <View style={S.specGroup}>
                    <Text style={S.specLabel}>타입</Text>
                    <Text style={S.specValue}>{charger.type}</Text>
                  </View>
                  <View style={S.specGroup}>
                    <Text style={S.specLabel}>충전 속도</Text>
                    <Text style={S.specValue}>{charger.kw}</Text>
                  </View>
                  <TouchableOpacity onPress={() => toggleAlert(station.id, charger.id)} hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}>
                    <Ionicons name={charger.alertOn ? "notifications" : "notifications-outline"} size={22} color={charger.alertOn ? "#FFB800" : "#ccc"} />
                  </TouchableOpacity>
                </View>
              );
            })}
          </View>
        ))}
      </ScrollView>
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
  scroll: { padding: 16, gap: 12 },
  card: { backgroundColor: "#fff", borderRadius: 14, padding: 16, shadowColor: "#000", shadowOpacity: 0.06, shadowRadius: 6, elevation: 2 },
  stationTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 4 },
  stationName: { fontSize: 14, fontWeight: "700", color: "#111", flex: 1, marginRight: 8, lineHeight: 20 },
  operator: { fontSize: 12, color: "#888", flexShrink: 0 },
  hours: { fontSize: 12, color: "#999", marginBottom: 8 },
  tagRow: { flexDirection: "row", flexWrap: "wrap", gap: 6, marginBottom: 12 },
  tag: { paddingHorizontal: 9, paddingVertical: 3, backgroundColor: "#EBF3FF", borderRadius: 6 },
  tagTxt: { fontSize: 11, color: "#5B9CF6", fontWeight: "500" },
  facilityTag: { paddingHorizontal: 9, paddingVertical: 3, backgroundColor: "#f2f2f2", borderRadius: 6 },
  facilityTxt: { fontSize: 11, color: "#777" },
  divider: { height: 1, backgroundColor: "#f0f0f0", marginBottom: 4 },
  chargerRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingVertical: 10, gap: 8 },
  chargerBorder: { borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 8, minWidth: 58, alignItems: "center" },
  statusTxt: { fontSize: 11, fontWeight: "600" },
  specGroup: { flex: 1 },
  specLabel: { fontSize: 10, color: "#aaa", marginBottom: 2 },
  specValue: { fontSize: 12, fontWeight: "600", color: "#333" },
});