import React, { useEffect, useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, TextInput,
  Image, FlatList, ActivityIndicator, Alert, KeyboardAvoidingView, Platform,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useLocalSearchParams } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";

// USB로 연결해서 adb reverse tcp:8080 tcp:8080으로 포트를 넘겨받는 구성이라 localhost로 접근함
const BACKEND_URL = "http://localhost:8080";

type ProfileImage = { id: number; imageUrl: string; name: string };

export default function RegisterScreen() {
  const router = useRouter();
  const { tempToken } = useLocalSearchParams<{ tempToken: string }>();

  const [nickname, setNickname] = useState("");
  const [images, setImages] = useState<ProfileImage[]>([]);
  const [selectedImageId, setSelectedImageId] = useState<number | null>(null);
  const [loadingImages, setLoadingImages] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(`${BACKEND_URL}/profile-images`);
        const data: ProfileImage[] = await res.json();
        setImages(data);
        if (data.length > 0) setSelectedImageId(data[0].id);
      } catch (e) {
        console.error("프로필 이미지 로드 실패", e);
        Alert.alert("오류", "프로필 이미지를 불러오지 못했습니다.");
      } finally {
        setLoadingImages(false);
      }
    })();
  }, []);

  const handleSubmit = async () => {
    const trimmed = nickname.trim();
    if (!trimmed) {
      Alert.alert("입력 오류", "닉네임을 입력해주세요.");
      return;
    }
    if (!selectedImageId) {
      Alert.alert("입력 오류", "프로필 이미지를 선택해주세요.");
      return;
    }
    if (!tempToken) {
      Alert.alert("오류", "로그인 정보가 만료되었습니다. 다시 로그인해주세요.");
      router.replace("/login" as any);
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${BACKEND_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tempToken,
          nickname: trimmed,
          profileImageId: selectedImageId,
        }),
      });

      if (!res.ok) {
        Alert.alert("가입 실패", "잠시 후 다시 시도해주세요.");
        return;
      }

      const data = await res.json();
      if (data.status === "SUCCESS") {
        await AsyncStorage.multiSet([
          ["jwt_token", data.accessToken],
          ["refresh_token", data.refreshToken ?? ""],
        ]);
        router.replace("/(tabs)/mypage" as any);
      } else {
        Alert.alert("가입 실패", "잠시 후 다시 시도해주세요.");
      }
    } catch (e) {
      console.error("회원가입 오류", e);
      Alert.alert("오류", "네트워크 연결을 확인해주세요.");
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
        <Text style={S.headerTitle}>회원가입</Text>
        <View style={{ width: 44 }} />
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <View style={S.section}>
          <Text style={S.label}>닉네임</Text>
          <TextInput
            style={S.input}
            value={nickname}
            onChangeText={setNickname}
            placeholder="사용하실 닉네임을 입력해주세요"
            placeholderTextColor="#bbb"
            maxLength={20}
          />
        </View>

        <View style={S.section}>
          <Text style={S.label}>프로필 이미지</Text>
          {loadingImages ? (
            <ActivityIndicator size="small" color="#5B9CF6" style={{ marginTop: 12 }} />
          ) : (
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
          )}
        </View>

        <View style={S.footer}>
          <TouchableOpacity
            style={[S.submitBtn, submitting && S.submitBtnDisabled]}
            onPress={handleSubmit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : (
              <Text style={S.submitTxt}>가입 완료</Text>
            )}
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
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
