import React, { useState } from "react";
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity, Modal, ScrollView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

type Notice = {
  id: string;
  title: string;
  date: string;
  content: string;
  isNew: boolean;
};

const MOCK_NOTICES: Notice[] = [
  {
    id: "1",
    title: "[안내] 서비스 점검 안내 (6월 20일 새벽 2시~4시)",
    date: "2025.06.18",
    content: "안녕하세요. EV 충전 서비스입니다.\n\n더 나은 서비스 제공을 위한 정기 점검을 아래와 같이 실시합니다.\n\n■ 점검 일시: 2025년 6월 20일 (금) 새벽 2:00 ~ 4:00\n■ 점검 내용: 서버 업그레이드 및 데이터베이스 최적화\n■ 영향 범위: 앱 전체 서비스 일시 중단\n\n점검 시간 동안에는 서비스 이용이 불가하오니 양해 부탁드립니다.\n\n감사합니다.",
    isNew: true,
  },
  {
    id: "2",
    title: "[업데이트] 충전기 알림 기능 추가 안내",
    date: "2025.06.10",
    content: "안녕하세요. EV 충전 서비스입니다.\n\n이번 업데이트를 통해 충전기 알림 기능이 추가되었습니다.\n\n■ 주요 기능\n- 원하는 충전기에 알림 설정 가능\n- 충전기가 사용 가능해지면 푸시 알림 발송\n- 마이페이지 > 충전기 알림 관리에서 확인 가능\n\n앞으로도 더 좋은 서비스로 찾아뵙겠습니다.\n\n감사합니다.",
    isNew: false,
  },
  {
    id: "3",
    title: "[이벤트] 즐겨찾기 충전소 등록 시 포인트 적립",
    date: "2025.06.01",
    content: "안녕하세요. EV 충전 서비스입니다.\n\n6월 한 달간 즐겨찾기 충전소 등록 이벤트를 진행합니다.\n\n■ 이벤트 기간: 2025.06.01 ~ 2025.06.30\n■ 이벤트 내용: 즐겨찾기 충전소 최초 등록 시 500포인트 적립\n■ 지급 방법: 등록 후 3 영업일 이내 자동 지급\n\n많은 참여 부탁드립니다!\n\n감사합니다.",
    isNew: false,
  },
  {
    id: "4",
    title: "[안내] 개인정보 처리방침 개정 안내",
    date: "2025.05.20",
    content: "안녕하세요. EV 충전 서비스입니다.\n\n개인정보 처리방침이 아래와 같이 개정됩니다.\n\n■ 시행일: 2025년 6월 1일\n■ 주요 변경 내용: 수집 항목 명확화, 제3자 제공 항목 업데이트\n\n자세한 내용은 앱 내 개인정보 처리방침을 확인해주세요.\n\n감사합니다.",
    isNew: false,
  },
  {
    id: "5",
    title: "[안내] 회원가입 혜택 안내",
    date: "2025.05.10",
    content: "안녕하세요. EV 충전 서비스입니다.\n\n신규 회원가입 시 아래 혜택을 드립니다.\n\n■ 혜택: 가입 즉시 1,000포인트 지급\n■ 사용처: 충전 요금 결제 시 사용 가능\n\n지금 바로 가입하고 혜택을 받아보세요!\n\n감사합니다.",
    isNew: false,
  },
];

export default function NoticesScreen() {
  const router = useRouter();
  const [selected, setSelected] = useState<Notice | null>(null);

  const renderItem = ({ item }: { item: Notice }) => (
    <TouchableOpacity
      style={S.item}
      onPress={() => setSelected(item)}
      activeOpacity={0.75}
    >
      <View style={S.itemLeft}>
        {item.isNew && <View style={S.newDot} />}
        <View style={{ flex: 1 }}>
          <Text style={S.itemTitle} numberOfLines={2}>{item.title}</Text>
          <Text style={S.itemDate}>{item.date}</Text>
        </View>
      </View>
      <Ionicons name="chevron-forward" size={16} color="#ccc" />
    </TouchableOpacity>
  );

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
        ItemSeparatorComponent={() => <View style={S.separator} />}
        contentContainerStyle={S.listContent}
        ListEmptyComponent={
          <View style={S.empty}>
            <Text style={S.emptyTxt}>공지사항이 없어요</Text>
          </View>
        }
      />

      {/* 상세 모달 */}
      <Modal
        visible={!!selected}
        animationType="slide"
        onRequestClose={() => setSelected(null)}
      >
        <SafeAreaView style={{ flex: 1, backgroundColor: "#fff" }} edges={["top"]}>
          <View style={S.header}>
            <TouchableOpacity onPress={() => setSelected(null)} style={S.headerBtn}>
              <Ionicons name="chevron-back" size={24} color="#111" />
            </TouchableOpacity>
            <Text style={S.headerTitle}>공지사항</Text>
            <View style={S.headerBtn} />
          </View>

          {selected && (
            <ScrollView contentContainerStyle={S.detailContent}>
              <Text style={S.detailTitle}>{selected.title}</Text>
              <Text style={S.detailDate}>{selected.date}</Text>
              <View style={S.detailDivider} />
              <Text style={S.detailBody}>{selected.content}</Text>
            </ScrollView>
          )}
        </SafeAreaView>
      </Modal>
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
  listContent: { paddingVertical: 8 },

  item: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 20, paddingVertical: 16,
  },
  itemLeft: { flexDirection: "row", alignItems: "flex-start", flex: 1, marginRight: 8, gap: 8 },
  newDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: "#5B9CF6", marginTop: 5, flexShrink: 0 },
  itemTitle: { fontSize: 14, color: "#222", lineHeight: 20, fontWeight: "500" },
  itemDate: { fontSize: 12, color: "#bbb", marginTop: 4 },
  separator: { height: 1, backgroundColor: "#f5f5f5", marginHorizontal: 20 },

  empty: { alignItems: "center", paddingTop: 80 },
  emptyTxt: { fontSize: 15, color: "#bbb" },

  detailContent: { padding: 24 },
  detailTitle: { fontSize: 16, fontWeight: "700", color: "#111", lineHeight: 24, marginBottom: 8 },
  detailDate: { fontSize: 13, color: "#aaa", marginBottom: 16 },
  detailDivider: { height: 1, backgroundColor: "#f0f0f0", marginBottom: 20 },
  detailBody: { fontSize: 14, color: "#444", lineHeight: 24 },
});
