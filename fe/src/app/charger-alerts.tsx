import React, { useState, useCallback } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  ActivityIndicator, Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useFocusEffect } from "expo-router";
import api from "@/lib/api";

const ACCENT = "#5B9CF6";
const ACCENT_BG = "#EBF3FF";

type ChgerStat = "UNKNOWN" | "COMM_ERROR" | "WAITING" | "CHARGING" | "SUSPENDED" | "INSPECTION" | "RESERVED" | "UNCONFIRMED";

type AlertedCharger = {
  chgerId: string;
  chgerType: string;
  output: string;
  chgerStat: ChgerStat;
};

type AlertedStation = {
  statId: string;
  statNm: string;
  useTime: string;
  parkingFree: boolean | null;
  openToPublic: boolean | null;
  kind: string | null;
  floorType: string | null;
  hasFast: boolean;
  busiNm: string;
  alertedChargers: AlertedCharger[];
};

const STAT_CFG: Record<string, { bg: string; color: string; label: string }> = {
  WAITING:     { bg: "#E8F5E9", color: "#4CAF50", label: "충전대기" },
  CHARGING:    { bg: "#FFF8E1", color: "#F59E0B", label: "충전중"   },
  SUSPENDED:   { bg: "#FFF0F0", color: "#F44336", label: "운영중지"  },
  INSPECTION:  { bg: "#FFF0F0", color: "#F44336", label: "점검중"   },
  RESERVED:    { bg: "#f2f2f2", color: "#999",    label: "예약중"   },
  UNKNOWN:     { bg: "#f2f2f2", color: "#999",    label: "알수없음"  },
  COMM_ERROR:  { bg: "#f2f2f2", color: "#999",    label: "통신이상"  },
  UNCONFIRMED: { bg: "#f2f2f2", color: "#999",    label: "미확인"   },
};

export default function ChargerAlertsScreen() {
  const router = useRouter();
  const [stations, setStations] = useState<AlertedStation[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAlerts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<AlertedStation[]>("/charger-alerts");
      setStations(res.data ?? []);
    } catch {
      Alert.alert("오류", "알림 목록을 불러오지 못했습니다.");
      setStations([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { fetchAlerts(); }, [fetchAlerts]));

  const removeAlert = async (statId: string, chgerId: string) => {
    try {
      await api.delete(`/charger-alerts/${statId}/${chgerId}`);
      setStations(prev =>
        prev
          .map(s => s.statId !== statId ? s : {
            ...s,
            alertedChargers: s.alertedChargers.filter(c => c.chgerId !== chgerId),
          })
          .filter(s => s.alertedChargers.length > 0)
      );
    } catch {
      Alert.alert("오류", "알림 해제에 실패했습니다.");
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={S.container} edges={["top"]}>
        <View style={S.header}>
          <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
            <Ionicons name="chevron-back" size={24} color="#111" />
          </TouchableOpacity>
          <Text style={S.headerTitle}>충전기 알림 관리</Text>
          <View style={S.headerBtn} />
        </View>
        <View style={S.center}>
          <ActivityIndicator size="large" color={ACCENT} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>충전기 알림 관리</Text>
        <View style={S.headerBtn} />
      </View>

      {stations.length === 0 ? (
        <View style={S.empty}>
          <Ionicons name="notifications-outline" size={48} color="#ddd" />
          <Text style={S.emptyTxt}>알림 설정된 충전기가 없어요</Text>
          <Text style={S.emptySubTxt}>충전소 상세 화면에서 알림을 설정할 수 있어요</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={S.scroll}>
          {stations.map(station => {
            const tags = [
              station.hasFast ? "급속" : "완속",
              ...(station.parkingFree ? ["무료주차"] : []),
              station.openToPublic ? "개방" : "비개방",
            ];
            return (
              <View key={station.statId} style={S.card}>
                <View style={S.stationTop}>
                  <Text style={S.stationName} numberOfLines={2}>{station.statNm}</Text>
                  <Text style={S.operator}>{station.busiNm}</Text>
                </View>
                <Text style={S.hours}>{station.useTime}</Text>
                <View style={S.tagRow}>
                  {tags.map(t => (
                    <View key={t} style={S.tag}><Text style={S.tagTxt}>{t}</Text></View>
                  ))}
                  {station.kind && (
                    <View style={S.facilityTag}><Text style={S.facilityTxt}>{station.kind}</Text></View>
                  )}
                </View>
                <View style={S.divider} />
                {station.alertedChargers.map((charger, idx) => {
                  const cfg = STAT_CFG[charger.chgerStat] ?? STAT_CFG.UNKNOWN;
                  return (
                    <View
                      key={charger.chgerId}
                      style={[S.chargerRow, idx < station.alertedChargers.length - 1 && S.chargerBorder]}
                    >
                      <View style={[S.statusBadge, { backgroundColor: cfg.bg }]}>
                        <Text style={[S.statusTxt, { color: cfg.color }]}>{cfg.label}</Text>
                      </View>
                      <View style={S.specGroup}>
                        <Text style={S.specLabel}>타입</Text>
                        <Text style={S.specValue}>{charger.chgerType}</Text>
                      </View>
                      <View style={S.specGroup}>
                        <Text style={S.specLabel}>출력</Text>
                        <Text style={S.specValue}>{charger.output ?? "-"}</Text>
                      </View>
                      <TouchableOpacity
                        onPress={() => removeAlert(station.statId, charger.chgerId)}
                        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                      >
                        <Ionicons name="notifications" size={22} color="#FFB800" />
                      </TouchableOpacity>
                    </View>
                  );
                })}
              </View>
            );
          })}
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f6fa" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
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
  tag: { paddingHorizontal: 9, paddingVertical: 3, backgroundColor: ACCENT_BG, borderRadius: 6 },
  tagTxt: { fontSize: 11, color: ACCENT, fontWeight: "500" },
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
  empty: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
  emptySubTxt: { fontSize: 13, color: "#ccc" },
});
