import React, { useState, useCallback } from "react";
import { View, Text, TouchableOpacity, StyleSheet, FlatList, Image, Alert, ActivityIndicator, Modal, TextInput } from "react-native";
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

  // 리뷰 수정 모달 상태
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingReview, setEditingReview] = useState<Review | null>(null);
  const [editRating, setEditRating] = useState(5);
  const [editContent, setEditContent] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

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

  const openEditModal = (review: Review) => {
    setEditingReview(review);
    setEditRating(review.rating);
    setEditContent(review.content);
    setEditModalVisible(true);
  };

  const handleEditSubmit = async () => {
    if (!editingReview) return;
    if (!editContent.trim()) {
      Alert.alert("입력 오류", "리뷰 내용을 입력해주세요.");
      return;
    }
    setEditSubmitting(true);
    try {
      await api.patch(`/reviews/${editingReview.reviewId}`, {
        rating: editRating,
        content: editContent.trim(),
      });
      setReviews(prev =>
        prev.map(r =>
          r.reviewId === editingReview.reviewId
            ? { ...r, rating: editRating, content: editContent.trim(), isEdited: true }
            : r
        )
      );
      setEditModalVisible(false);
      setEditingReview(null);
    } catch (e: any) {
      Alert.alert("오류", e?.response?.data?.message ?? "수정에 실패했어요. 다시 시도해주세요.");
    } finally {
      setEditSubmitting(false);
    }
  };

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
          <TouchableOpacity onPress={() => openEditModal(item)}>
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

      {/* 리뷰 수정 모달 */}
      <Modal
        visible={editModalVisible}
        animationType="slide"
        transparent
        onRequestClose={() => setEditModalVisible(false)}
      >
        <TouchableOpacity
          style={styles.modalBackdrop}
          activeOpacity={1}
          onPress={() => setEditModalVisible(false)}
        />
        <View style={styles.modalSheet}>
          <View style={styles.modalHandle} />
          <Text style={styles.modalTitle}>리뷰 수정</Text>

          <Text style={styles.modalLabel}>별점</Text>
          <View style={styles.starRow}>
            {[1, 2, 3, 4, 5].map((star) => (
              <TouchableOpacity key={star} onPress={() => setEditRating(star)}>
                <Ionicons
                  name={star <= editRating ? "star" : "star-outline"}
                  size={32}
                  color="#FFB800"
                />
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.modalLabel}>내용</Text>
          <TextInput
            style={styles.modalInput}
            value={editContent}
            onChangeText={setEditContent}
            placeholder="리뷰 내용을 입력해주세요"
            placeholderTextColor="#bbb"
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />

          <TouchableOpacity
            style={[styles.modalSubmitBtn, editSubmitting && { opacity: 0.6 }]}
            onPress={handleEditSubmit}
            disabled={editSubmitting}
          >
            {editSubmitting
              ? <ActivityIndicator size="small" color="#fff" />
              : <Text style={styles.modalSubmitTxt}>수정 완료</Text>}
          </TouchableOpacity>
        </View>
      </Modal>
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
  // 모달
  modalBackdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.3)" },
  modalSheet: { backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 },
  modalHandle: { width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd", alignSelf: "center", marginBottom: 16 },
  modalTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", marginBottom: 20 },
  modalLabel: { fontSize: 13, color: "#555", fontWeight: "600", marginBottom: 8 },
  starRow: { flexDirection: "row", gap: 8, marginBottom: 20 },
  modalInput: { borderWidth: 1.5, borderColor: "#e8e8e8", borderRadius: 10, paddingHorizontal: 14, paddingVertical: 11, fontSize: 14, color: "#222", backgroundColor: "#fafafa", height: 100, marginBottom: 20 },
  modalSubmitBtn: { backgroundColor: "#5B9CF6", borderRadius: 12, height: 50, alignItems: "center", justifyContent: "center" },
  modalSubmitTxt: { color: "#fff", fontSize: 16, fontWeight: "600" },
});