import React, { useState, useEffect } from "react";
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import api from "@/lib/api";

type NoticeItem = {
  id: number;
  title: string;
  createdAt: string;
  isRead: boolean;
};

type NoticeContent = {
  id: number;
  title: string;
  content: string;
  createdAt: string;
};

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}.${m}.${day}`;
}

export default function NoticesScreen() {
  const router = useRouter();
  const [notices, setNotices] = useState<NoticeItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [contentMap, setContentMap] = useState<Record<number, string>>({});
  const [contentLoading, setContentLoading] = useState(false);

  useEffect(() => {
    api.get("/notices", { params: { size: 50, sort: "createdAt,desc" } })
      .then(res => setNotices(res.data.content ?? []))
      .catch(() => setNotices([]))
      .finally(() => setLoading(false));
  }, []);

  const toggle = async (id: number) => {
    if (expandedId === id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(id);
    if (contentMap[id]) return;

    setContentLoading(true);
    try {
      const res = await api.get<NoticeContent>(`/notices/${id}`);
      setContentMap(prev => ({ ...prev, [id]: res.data.content }));
    } catch {
      setContentMap(prev => ({ ...prev, [id]: "내용을 불러올 수 없어요." }));
    } finally {
      setContentLoading(false);
    }
  };

  const renderItem = ({ item }: { item: NoticeItem }) => {
    const isExpanded = expandedId === item.id;
    return (
      <View>
        <TouchableOpacity style={S.item} onPress={() => toggle(item.id)} activeOpacity={0.75}>
          <Text style={S.itemDate}>{formatDate(item.createdAt)}</Text>
          <View style={S.itemMiddle}>
            {!item.isRead && <View style={S.newDot} />}
            <Text style={S.itemTitle} numberOfLines={isExpanded ? undefined : 1}>{item.title}</Text>
          </View>
          <Ionicons name={isExpanded ? "chevron-up" : "chevron-down"} size={16} color="#bbb" />
        </TouchableOpacity>
        {isExpanded && (
          <View style={S.content}>
            {contentLoading && !contentMap[item.id] ? (
              <ActivityIndicator size="small" color="#5B9CF6" />
            ) : (
              <Text style={S.contentTxt}>{contentMap[item.id] ?? ""}</Text>
            )}
          </View>
        )}
        <View style={S.separator} />
      </View>
    );
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>공지사항</Text>
        <View style={S.headerBtn} />
      </View>

      {loading ? (
        <View style={S.center}>
          <ActivityIndicator size="large" color="#5B9CF6" />
        </View>
      ) : (
        <FlatList
          data={notices}
          keyExtractor={item => String(item.id)}
          renderItem={renderItem}
          contentContainerStyle={S.listContent}
          ListEmptyComponent={
            <View style={S.empty}>
              <Text style={S.emptyTxt}>공지사항이 없어요</Text>
            </View>
          }
        />
      )}
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
  headerBtn: { padding: 10, width: 44 },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  listContent: { paddingTop: 4 },
  item: { flexDirection: "row", alignItems: "center", paddingHorizontal: 20, paddingVertical: 16, gap: 10 },
  itemDate: { fontSize: 12, color: "#bbb", width: 68, flexShrink: 0 },
  itemMiddle: { flex: 1, flexDirection: "row", alignItems: "center", gap: 6 },
  newDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: "#5B9CF6", flexShrink: 0 },
  itemTitle: { fontSize: 14, color: "#222", fontWeight: "500", flex: 1 },
  content: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 16, backgroundColor: "#fafafa" },
  contentTxt: { fontSize: 13, color: "#555", lineHeight: 22 },
  separator: { height: 1, backgroundColor: "#f5f5f5", marginHorizontal: 20 },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  empty: { alignItems: "center", paddingTop: 80 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});
