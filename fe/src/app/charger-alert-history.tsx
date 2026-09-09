import React, { useState, useCallback } from "react";
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity,
  Alert, ActivityIndicator,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useFocusEffect } from "expo-router";
import api from "@/lib/api";

type AlertHistory = {
  id: number;
  statId: string;
  chgerId: string;
  createdAt: string;
  isRead: boolean;
};

function formatTimeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 60) return `${min}분 전`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour}시간 전`;
  const day = Math.floor(hour / 24);
  if (day < 7) return `${day}일 전`;
  return iso.slice(0, 10).replace(/-/g, ".");
}

export default function ChargerAlertHistoryScreen() {
  const router = useRouter();
  const [history, setHistory] = useState<AlertHistory[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<AlertHistory[]>("/notifications");
      setHistory(res.data ?? []);
    } catch {
      setHistory([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { fetchHistory(); }, [fetchHistory]));

  const deleteItem = async (id: number) => {
    try {
      await api.delete(`/notifications/${id}`);
      setHistory(prev => prev.filter(h => h.id !== id));
    } catch {
      Alert.alert("오류", "삭제에 실패했습니다.");
    }
  };

  const clearAll = () => {
    Alert.alert("알림 기록 삭제", "모든 알림 기록을 삭제할까요?", [
      { text: "취소", style: "cancel" },
      {
        text: "삭제", style: "destructive",
        onPress: async () => {
          try {
            await api.delete("/notifications");
            setHistory([]);
          } catch {
            Alert.alert("오류", "전체 삭제에 실패했습니다.");
          }
        },
      },
    ]);
  };

  const renderItem = ({ item }: { item: AlertHistory }) => (
    <View style={[S.item, !item.isRead && S.itemUnread]}>
      <View style={S.itemTop}>
        <Text style={S.stationName} numberOfLines={1}>충전소 {item.statId}</Text>
        <Text style={S.timeAgo}>{formatTimeAgo(item.createdAt)}</Text>
        <TouchableOpacity
          onPress={() => deleteItem(item.id)}
          hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
        >
          <Ionicons name="trash-outline" size={16} color="#ccc" />
        </TouchableOpacity>
      </View>
      <Text style={S.chargerInfo}>충전기 {item.chgerId} · 대기 전환 알림</Text>
      {!item.isRead && <View style={S.unreadDot} />}
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

      {loading ? (
        <View style={S.center}>
          <ActivityIndicator size="large" color="#5B9CF6" />
        </View>
      ) : (
        <FlatList
          data={history}
          keyExtractor={item => String(item.id)}
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
      )}
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  headerBtn: { padding: 10, width: 44, alignItems: "center" },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  listContent: { paddingVertical: 8 },
  item: { paddingHorizontal: 20, paddingVertical: 16, position: "relative" },
  itemUnread: { backgroundColor: "#F8FBFF" },
  itemTop: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 6 },
  stationName: { flex: 1, fontSize: 14, fontWeight: "600", color: "#111" },
  timeAgo: { fontSize: 12, color: "#bbb", flexShrink: 0 },
  chargerInfo: { fontSize: 12, color: "#999" },
  unreadDot: {
    position: "absolute", top: 20, left: 8,
    width: 6, height: 6, borderRadius: 3, backgroundColor: "#5B9CF6",
  },
  separator: { height: 1, backgroundColor: "#f5f5f5", marginHorizontal: 20 },
  empty: { alignItems: "center", paddingTop: 80, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});
