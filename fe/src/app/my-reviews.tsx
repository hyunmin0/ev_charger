import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Image } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";

const mockReviews = [
  {
    id: 1,
    stationName: "한국도로교통공단 광주전남지부",
    operator: "파워큐브",
    address: "광주광역시 북구 우치로 201",
    rating: 4,
    content: "사용하기에 불편함이 없었어요~ 괜찮네요",
    date: "2026.06.12",
    isEdited: true,
    image: null,
  },
  {
    id: 2,
    stationName: "제일풍경채센트럴파크1단지입주자대표...",
    operator: "파워큐브",
    address: "광주광역시 북구 우치로40번길 25",
    rating: 1,
    content: "아니 다 사용불가면 어쩌라는거야",
    date: "2026.06.12",
    isEdited: true,
    image: null,
  },
  {
    id: 3,
    stationName: "광주광역시 북구 전남대학교공과대학",
    operator: "파워큐브",
    address: "광주광역시 북구 용봉로 77",
    rating: 4,
    content: "충전기는 괜찮은데 주차비용이 좀 비싼듯",
    date: "2026.06.12",
    isEdited: false,
    image: "https://via.placeholder.com/300x200",
  },
];

export default function MyReviewsScreen() {
  return (
    <SafeAreaView style={styles.container} edges={['bottom', 'left', 'right']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#333" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>내 리뷰</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView style={styles.scroll}>
        {mockReviews.map((review) => (
          <View key={review.id} style={styles.card}>
            <View style={styles.cardTop}>
              <Text style={styles.stationName} numberOfLines={1}>{review.stationName}</Text>
              <Text style={styles.operator}>{review.operator}</Text>
            </View>
            <Text style={styles.address}>{review.address}</Text>
            <View style={styles.ratingRow}>
              <Ionicons name="star" size={14} color="#F5C518" />
              <Text style={styles.ratingText}>{review.rating}</Text>
            </View>
            {review.image && (
              <Image source={{ uri: review.image }} style={styles.reviewImage} />
            )}
            <Text style={styles.content}>{review.content}</Text>
            <View style={styles.cardBottom}>
              <Text style={styles.date}>
                {review.date}{review.isEdited ? " (수정됨)" : ""}
              </Text>
              <View style={styles.actions}>
                <TouchableOpacity>
                  <Text style={styles.actionBtn}>수정</Text>
                </TouchableOpacity>
                <Text style={styles.divider}> | </Text>
                <TouchableOpacity>
                  <Text style={styles.actionBtn}>삭제</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f5f5" },
  header: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff" },
  headerTitle: { fontSize: 17, fontWeight: "bold" },
  scroll: { padding: 12 },
  card: { backgroundColor: "#fff", borderRadius: 12, padding: 16, marginBottom: 12 },
  cardTop: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 4 },
  stationName: { fontSize: 15, fontWeight: "bold", flex: 1, marginRight: 8 },
  operator: { fontSize: 13, color: "#888" },
  address: { fontSize: 13, color: "#666", marginBottom: 8 },
  ratingRow: { flexDirection: "row", alignItems: "center", marginBottom: 8 },
  ratingText: { fontSize: 13, marginLeft: 4 },
  reviewImage: { width: "100%", height: 160, borderRadius: 8, marginBottom: 8 },
  content: { fontSize: 14, color: "#333", marginBottom: 12 },
  cardBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  date: { fontSize: 12, color: "#aaa" },
  actions: { flexDirection: "row", alignItems: "center" },
  actionBtn: { fontSize: 13, color: "#666" },
  divider: { fontSize: 13, color: "#ccc" },
});