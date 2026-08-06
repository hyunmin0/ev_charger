import React, { useState } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

type Charger = {
  id: string; speed: "급속" | "완속"; type: string; kw: string; alertOn: boolean;
};
type Station = {
  id: string; name: string; operator: string; hours: string; facility: string; chargers: Charger[];
};

const INITIAL_STATIONS: Station[] = [
  {
    id: "1",
    name: "한국도로교통공단 광주전남지부",
    operator: "파워큐브",
    hours: "24시간 이용가능",
    facility: "공공주차시설",
    chargers: [
      { id: "C01", speed: "급속", type: "DC 콤보", kw: "100kW", alertOn: true },
      { id: "C02", speed: "급속", type: "DC 콤보", kw: "100kW", alertOn: false },
      { id: "C03", speed: "급속", type: "DC 콤보", kw: "100kW", alertOn: true },
    ],
  },
  {
    id: "2",
    name: "광주광역시 북구 전남대학교공과대학",
    operator: "GS차지비",
    hours: "24시간 이용가능",
    facility: "교육문화시설",
    chargers: [
      { id: "C01", speed: "급속", type: "DC 콤보", kw: "100kW", alertOn: true },
      { id: "C02", speed: "완속", type: "AC 완속", kw: "7kW", alertOn: false },
    ],
  },
  {
    id: "3",
    name: "재일풍경채센트럴파크1단지입주자대표회의(SP)",
    operator: "파워큐브",
    hours: "24시간 이용가능",
    facility: "공동주택시설",
    chargers: [
      { id: "C01", speed: "급속", type: "DC 콤보", kw: "50kW", alertOn: false },
      { id: "C02", speed: "급속", type: "DC 콤보", kw: "50kW", alertOn: false },
      { id: "C03", speed: "급속", type: "DC 콤보", kw: "50kW", alertOn: true },
    ],
  },
];

export default function ChargerAlertsScreen() {
  const router = useRouter();
  const [stations, setStations] = useState(INITIAL_STATIONS);

  const toggleAlert = (stationId: string, chargerId: string) => {
    setStations(prev =>
      prev.map(s =>
        s.id !== stationId ? s : {
          ...s,
          chargers: s.chargers.map(c =>
            c.id !== chargerId ? c : { ...c, alertOn: !c.alertOn }
          ),
        }
      )
    );
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
            {/* 충전소 정보 */}
            <View style={S.stationTop}>
              <Text style={S.stationName} numberOfLines={2}>{station.name}</Text>
              <Text style={S.operator}>{station.operator}</Text>
            </View>
            <Text style={S.hours}>{station.hours}</Text>
            <Text style={S.facility}>시설: {station.facility}</Text>

            <View style={S.divider} />

            {/* 충전기 목록 */}
            {station.chargers.map((charger, idx) => (
              <View
                key={charger.id}
                style={[S.chargerRow, idx < station.chargers.length - 1 && S.chargerBorder]}
              >
                <View style={S.chargerLeft}>
                  <View style={[S.speedBadge, charger.speed === "급속" ? S.badgeFast : S.badgeSlow]}>
                    <Text style={[S.speedTxt, charger.speed === "급속" ? S.fastTxt : S.slowTxt]}>
                      {charger.speed}
                    </Text>
                  </View>
                  <View style={S.chargerInfo}>
                    <Text style={S.chargerType}>{charger.type}</Text>
                    <Text style={S.chargerKw}>{charger.kw}</Text>
                  </View>
                </View>
                <TouchableOpacity
                  onPress={() => toggleAlert(station.id, charger.id)}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  <Ionicons
                    name={charger.alertOn ? "notifications" : "notifications-outline"}
                    size={22}
                    color={charger.alertOn ? "#FFB800" : "#ccc"}
                  />
                </TouchableOpacity>
              </View>
            ))}
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

  card: {
    backgroundColor: "#fff", borderRadius: 14, padding: 16,
    shadowColor: "#000", shadowOpacity: 0.06, shadowRadius: 6, elevation: 2,
  },
  stationTop: {
    flexDirection: "row", justifyContent: "space-between",
    alignItems: "flex-start", marginBottom: 4,
  },
  stationName: { fontSize: 14, fontWeight: "700", color: "#111", flex: 1, marginRight: 8, lineHeight: 20 },
  operator: { fontSize: 12, color: "#888", flexShrink: 0 },
  hours: { fontSize: 12, color: "#999", marginBottom: 2 },
  facility: { fontSize: 12, color: "#aaa", marginBottom: 12 },
  divider: { height: 1, backgroundColor: "#f0f0f0", marginBottom: 12 },

  chargerRow: {
    flexDirection: "row", alignItems: "center",
    justifyContent: "space-between", paddingVertical: 10,
  },
  chargerBorder: { borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  chargerLeft: { flexDirection: "row", alignItems: "center", gap: 12 },
  speedBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  badgeFast: { backgroundColor: "#FFF8E1" },
  badgeSlow: { backgroundColor: "#E8F5E9" },
  speedTxt: { fontSize: 12, fontWeight: "600" },
  fastTxt: { color: "#F59E0B" },
  slowTxt: { color: "#4CAF50" },
  chargerInfo: {},
  chargerType: { fontSize: 13, color: "#333", fontWeight: "500" },
  chargerKw: { fontSize: 12, color: "#999", marginTop: 1 },
});
