import React, { useEffect, useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, TextInput,
  Image, FlatList, ActivityIndicator, Alert, KeyboardAvoidingView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import api from "@/lib/api";

type ProfileImage = { id: number; imageUrl: string; name: string };

// be의 UserService.MAX_NICKNAME_LENGTH와 같음
const MAX_NICKNAME_LENGTH = 20;

export default function ProfileEditScreen() {
  const router = useRouter();

  const [nickname, setNickname] = useState("");
  const [images, setImages] = useState<ProfileImage[]>([]);
  const [selectedImageId, setSelectedImageId] = useState<number | null>(null);
  // 바뀐 항목만 보내기 위해 처음 값을 기억
  const [initialNickname, setInitialNickname] = useState("");
  const [initialImageId, setInitialImageId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [profileRes, imagesRes] = await Promise.all([
          api.get<{ nickname: string; imageUrl: string | null }>("/user/profile"),
          api.get<ProfileImage[]>("/profile-images"),
        ]);
        const name = profileRes.data.nickname ?? "";
        // 프로필 응답에는 이미지 id가 없어서 URL로 현재 사진을 찾음
        const currentId = imagesRes.data.find(img => img.imageUrl === profileRes.data.imageUrl)?.id ?? null;
        setNickname(name);
        setInitialNickname(name);
        setImages(imagesRes.data);
        setSelectedImageId(currentId);
        setInitialImageId(currentId);
      } catch {
        Alert.alert("오류", "프로필 정보를 불러오지 못했어요.", [
          { text: "확인", onPress: () => router.back() },
        ]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const trimmed = nickname.trim();
  const nicknameChanged = trimmed !== initialNickname;
  const imageChanged = selectedImageId !== null && selectedImageId !== initialImageId;

  const handleSave = async () => {
    if (!trimmed) {
      Alert.alert("입력 오류", "닉네임을 입력해주세요.");
      return;
    }
    if (!nicknameChanged && !imageChanged) {
      router.back();
      return;
    }

    setSubmitting(true);
    try {
      if (nicknameChanged) {
        await api.patch("/user/nickname", null, { params: { newName: trimmed } });
      }
      if (imageChanged) {
        await api.patch("/user/profile-image", null, { params: { profileImageId: selectedImageId } });
      }
      router.back();
    } catch (e: any) {
      Alert.alert("오류", e?.response?.data?.message ?? "프로필 수정에 실패했어요. 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={S.container} edges={["top", "bottom"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>프로필 수정</Text>
        <View style={{ width: 52 }} />
      </View>

      {loading ? (
        <ActivityIndicator size="small" color="#5B9CF6" style={{ marginTop: 40 }} />
      ) : (
        <KeyboardAvoidingView style={{ flex: 1 }} behavior="padding">
          <View style={S.section}>
            <Text style={S.label}>닉네임</Text>
            <TextInput
              style={S.input}
              value={nickname}
              onChangeText={setNickname}
              placeholder="닉네임을 입력해주세요"
              placeholderTextColor="#bbb"
              maxLength={MAX_NICKNAME_LENGTH}
            />
          </View>

          <View style={S.section}>
            <Text style={S.label}>프로필 이미지</Text>
            <FlatList
              data={images}
              keyExtractor={(item) => String(item.id)}
              numColumns={4}
              contentContainerStyle={S.imageGrid}
              renderItem={({ item }) => {
                const selected = item.id === selectedImageId;
                return (
                  <TouchableOpacity
                    style={[S.imageWrap, selected && S.imageWrapSelected]}
                    onPress={() => setSelectedImageId(item.id)}
                  >
                    <Image source={{ uri: item.imageUrl }} style={S.image} />
                    {selected && (
                      <View style={S.checkBadge}>
                        <Ionicons name="checkmark" size={14} color="#fff" />
                      </View>
                    )}
                  </TouchableOpacity>
                );
              }}
            />
          </View>

          <View style={S.footer}>
            <TouchableOpacity
              style={[S.submitBtn, submitting && S.submitBtnDisabled]}
              onPress={handleSave}
              disabled={submitting}
            >
              {submitting ? (
                <ActivityIndicator size="small" color="#fff" />
              ) : (
                <Text style={S.submitTxt}>저장</Text>
              )}
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      )}
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 4, paddingVertical: 6,
  },
  headerBtn: { padding: 14 },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },

  section: { paddingHorizontal: 20, marginTop: 24 },
  label: { fontSize: 14, fontWeight: "600", color: "#333", marginBottom: 10 },
  input: {
    height: 50, borderRadius: 12, borderWidth: 1.5, borderColor: "#e0e0e0",
    paddingHorizontal: 16, fontSize: 15, color: "#111",
  },

  imageGrid: { gap: 12 },
  imageWrap: {
    width: 68, height: 68, borderRadius: 34, marginRight: 12, marginBottom: 4,
    borderWidth: 2, borderColor: "transparent", alignItems: "center", justifyContent: "center",
    overflow: "visible",
  },
  imageWrapSelected: { borderColor: "#5B9CF6" },
  image: { width: 60, height: 60, borderRadius: 30 },
  checkBadge: {
    position: "absolute", bottom: -2, right: -2,
    width: 20, height: 20, borderRadius: 10, backgroundColor: "#5B9CF6",
    alignItems: "center", justifyContent: "center",
    borderWidth: 2, borderColor: "#fff",
  },

  footer: { paddingHorizontal: 20, paddingTop: 32, paddingBottom: 16 },
  submitBtn: {
    height: 52, borderRadius: 12, backgroundColor: "#5B9CF6",
    alignItems: "center", justifyContent: "center",
  },
  submitBtnDisabled: { opacity: 0.6 },
  submitTxt: { fontSize: 15, fontWeight: "700", color: "#fff" },
});
