import React, { useState, useEffect, useCallback } from "react";
import { View, Text, TouchableOpacity, StyleSheet, FlatList, Image, Alert, ActivityIndicator } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { router, useFocusEffect } from "expo-router";
import api from "@/lib/api";

type Review = {
  reviewId: number;
  statNm: string;
  busiNm: string;
  addr: string;
  rating: number;
  content: string;
  imageUrls: string[];
  createdAt: string;
  isEdited: boolean;
};

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}.${m}.${day}`;
}

export default function MyReviewsScreen() {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchReviews = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get("/reviews/my");
      setReviews(res.data ?? []);
    } catch {
      setReviews([]);
    } finally {
      setLoading(false);
    }
  }, []);

    useFocusEffect(
    useCallback(() => {
      fetchReviews();
    }, [fetchReviews])
  );


  const handleDelete = (reviewId: number) => {
    Alert.alert("리뷰 삭제", "이 리뷰를 삭제할까요?", [
      { text: "취소", style: "cancel" },
      {
        text: "삭제",
        style: "destructive",
        onPress: async () => {
          try {
            await api.delete(`/reviews/${reviewId}`);
            setReviews(prev => prev.filter(r => r.reviewId !== reviewId));
          } catch {
            Alert.alert("오류", "삭제에 실패했어요. 다시 시도해주세요.");
          }
        },
      },
    ]);
  };

  const renderItem = ({ item }: { item: Review }) => (
    <View style={styles.card}>
      <View style={styles.cardTop}>
        <Text style={styles.stationName} numberOfLines={1}>{item.statNm}</Text>
        <Text style={styles.operator}>{item.busiNm}</Text>
      </View>
      <Text style={styles.address}>{item.addr}</Text>
      <View style={styles.ratingRow}>
        {[1, 2, 3, 4, 5].map(i => (
          <Ionicons key={i} name="star" size={14} color={i <= item.rating ? "#F5C518" : "#e0e0e0"} />
        ))}
      </View>
      {item.imageUrls.length > 0 && (
        <Image source={{ uri: item.imageUrls[0] }} style={styles.reviewImage} />
      )}
      <Text style={styles.content}>{item.content}</Text>
      <View style={styles.cardBottom}>
        <Text style={styles.date}>
          {formatDate(item.createdAt)}{item.isEdited ? " (수정됨)" : ""}
        </Text>
        <View style={styles.actions}>
          <TouchableOpacity onPress={() => Alert.alert("준비 중", "수정 기능은 곧 제공될 예정이에요.")}>
            <Text style={styles.actionBtn}>수정</Text>
          </TouchableOpacity>
          <Text style={styles.divider}> | </Text>
          <TouchableOpacity onPress={() => handleDelete(item.reviewId)}>
            <Text style={[styles.actionBtn, { color: "#e53935" }]}>삭제</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#333" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>내 리뷰</Text>
        <View style={{ width: 24 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#5B9CF6" />
        </View>
      ) : (
        <FlatList
          data={reviews}
          keyExtractor={item => String(item.reviewId)}
          renderItem={renderItem}
          contentContainerStyle={styles.scroll}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Ionicons name="chatbubble-outline" size={48} color="#ddd" />
              <Text style={styles.emptyTxt}>작성한 리뷰가 없어요</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f5f5" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff",
  },
  headerTitle: { fontSize: 17, fontWeight: "bold" },
  scroll: { padding: 12 },
  card: { backgroundColor: "#fff", borderRadius: 12, padding: 16, marginBottom: 12 },
  cardTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 4 },
  stationName: { fontSize: 15, fontWeight: "bold", flex: 1, marginRight: 8 },
  operator: { fontSize: 13, color: "#888" },
  address: { fontSize: 13, color: "#666", marginBottom: 8 },
  ratingRow: { flexDirection: "row", gap: 2, marginBottom: 8 },
  reviewImage: { width: "100%", height: 160, borderRadius: 8, marginBottom: 8 },
  content: { fontSize: 14, color: "#333", marginBottom: 12 },
  cardBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  date: { fontSize: 12, color: "#aaa" },
  actions: { flexDirection: "row", alignItems: "center" },
  actionBtn: { fontSize: 13, color: "#666" },
  divider: { fontSize: 13, color: "#ccc" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  empty: { alignItems: "center", paddingTop: 80, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
});
