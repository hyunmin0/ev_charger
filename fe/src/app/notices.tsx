import React, { useState } from "react";
import { View, Text, StyleSheet, FlatList, TouchableOpacity } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

type Notice = { id: string; title: string; date: string; content: string; isNew: boolean };

const MOCK_NOTICES: Notice[] = [
  {
    id: "1", title: "서비스 점검 안내", date: "2026.07.07", isNew: true,
    content: "안녕하세요. ev-charger앱 점검이 있습니다.\n7월 10일 03시~05시에 앱 점검이 예정입니다.\n이용에 불편을 드려 죄송합니다.",
  },
  {
    id: "2", title: "운영정책 개정 안내", date: "2026.07.07", isNew: false,
    content: "안녕하세요. EV 충전 서비스입니다.\n\n운영정책이 아래와 같이 개정됩니다.\n\n■ 시행일: 2026년 8월 1일\n■ 주요 변경 내용: 수집 항목 명확화\n\n감사합니다.",
  },
  {
    id: "3", title: "[이벤트] 즐겨찾기 충전소 등록 시 포인트 적립", date: "2026.06.01", isNew: false,
    content: "안녕하세요. EV 충전 서비스입니다.\n\n6월 한 달간 즐겨찾기 충전소 등록 이벤트를 진행합니다.\n\n■ 이벤트 기간: 2026.06.01 ~ 2026.06.30\n■ 내용: 최초 등록 시 500포인트 적립\n\n감사합니다.",
  },
  {
    id: "4", title: "[안내] 개인정보 처리방침 개정 안내", date: "2026.05.20", isNew: false,
    content: "안녕하세요. EV 충전 서비스입니다.\n\n개인정보 처리방침이 아래와 같이 개정됩니다.\n\n■ 시행일: 2026년 6월 1일\n■ 주요 변경 내용: 수집 항목 명확화\n\n감사합니다.",
  },
  {
    id: "5", title: "[안내] 회원가입 혜택 안내", date: "2026.05.10", isNew: false,
    content: "안녕하세요. EV 충전 서비스입니다.\n\n신규 회원가입 시 가입 즉시 1,000포인트를 드립니다.\n\n지금 바로 가입하고 혜택을 받아보세요!\n\n감사합니다.",
  },
];

export default function NoticesScreen() {
  const router = useRouter();
  const [expandedId, setExpandedId] = useState<string | null>("1");

  const toggle = (id: string) => setExpandedId(prev => prev === id ? null : id);

  const renderItem = ({ item }: { item: Notice }) => {
    const isExpanded = expandedId === item.id;
    return (
      <View>
        <TouchableOpacity style={S.item} onPress={() => toggle(item.id)} activeOpacity={0.75}>
          <Text style={S.itemDate}>{item.date}</Text>
          <View style={S.itemMiddle}>
            {item.isNew && <View style={S.newDot} />}
            <Text style={S.itemTitle} numberOfLines={isExpanded ? undefined : 1}>{item.title}</Text>
          </View>
          <Ionicons name={isExpanded ? "chevron-up" : "chevron-down"} size={16} color="#bbb" />
        </TouchableOpacity>
        {isExpanded && (
          <View style={S.content}>
            <Text style={S.contentTxt}>{item.content}</Text>
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
      <FlatList
        data={MOCK_NOTICES}
        keyExtractor={item => item.id}
        renderItem={renderItem}
        contentContainerStyle={S.listContent}
        ListEmptyComponent={<View style={S.empty}><Text style={S.emptyTxt}>공지사항이 없어요</Text></View>}
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
  empty: { alignItems: "center", paddingTop: 80 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});