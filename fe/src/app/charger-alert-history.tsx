import React, { useState } from "react";
import { View, Text, StyleSheet, FlatList, TouchableOpacity, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

type AlertHistory = { id: string; stationName: string; timeAgo: string; method: "급속" | "완속"; type: string; speed: string };

const MOCK_HISTORY: AlertHistory[] = [
  { id: "1", stationName: "한국도로교통공단 광주전남지부", timeAgo: "3분 전", method: "급속", type: "DC 콤보", speed: "100kW" },
  { id: "2", stationName: "한국도로교통공단 광주전남지부", timeAgo: "3시간 전", method: "급속", type: "DC 콤보", speed: "100kW" },
  { id: "3", stationName: "한국도로교통공단 광주전남지부", timeAgo: "7월 4일", method: "급속", type: "DC 콤보", speed: "100kW" },
];

export default function ChargerAlertHistoryScreen() {
  const router = useRouter();
  const [history, setHistory] = useState(MOCK_HISTORY);

  const deleteItem = (id: string) => setHistory(prev => prev.filter(h => h.id !== id));

  const clearAll = () => {
    Alert.alert("알림 기록 삭제", "모든 알림 기록을 삭제할까요?", [
      { text: "취소", style: "cancel" },
      { text: "삭제", style: "destructive", onPress: () => setHistory([]) },
    ]);
  };

  const renderItem = ({ item }: { item: AlertHistory }) => (
    <View style={S.item}>
      <View style={S.itemTop}>
        <Text style={S.stationName} numberOfLines={1}>{item.stationName}</Text>
        <Text style={S.timeAgo}>{item.timeAgo}</Text>
        <TouchableOpacity onPress={() => deleteItem(item.id)} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
          <Ionicons name="trash-outline" size={16} color="#ccc" />
        </TouchableOpacity>
      </View>
      <Text style={S.chargerInfo}>방식 {item.method}  |  타입 {item.type}  |  충전 속도 {item.speed}</Text>
    </View>
  );

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>알림 기록</Text>
        <TouchableOpacity style={S.headerBtn} onPress={clearAll}>
          <Ionicons name="trash-outline" size={20} color="#aaa" />
        </TouchableOpacity>
      </View>
      <FlatList
        data={history}
        keyExtractor={item => item.id}
        renderItem={renderItem}
        ItemSeparatorComponent={() => <View style={S.separator} />}
        contentContainerStyle={S.listContent}
        ListEmptyComponent={
          <View style={S.empty}>
            <Ionicons name="notifications-outline" size={48} color="#ddd" />
            <Text style={S.emptyTxt}>알림 기록이 없어요</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  headerBtn: { padding: 10, width: 44, alignItems: "center" },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  listContent: { paddingVertical: 8 },
  item: { paddingHorizontal: 20, paddingVertical: 16 },
  itemTop: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 6 },
  stationName: { flex: 1, fontSize: 14, fontWeight: "600", color: "#111" },
  timeAgo: { fontSize: 12, color: "#bbb", flexShrink: 0 },
  chargerInfo: { fontSize: 12, color: "#999" },
  separator: { height: 1, backgroundColor: "#f5f5f5", marginHorizontal: 20 },
  empty: { alignItems: "center", paddingTop: 80, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});