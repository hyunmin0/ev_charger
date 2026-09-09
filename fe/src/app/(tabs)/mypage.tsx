import React, { useState, useCallback } from "react";
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert, Image,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { router, useFocusEffect } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";
import api from "@/lib/api";

const MENU_ITEMS = [
  { label: "내 차량 관리", icon: "car-outline" as const, route: "/car-management" },
  { label: "즐겨찾기 충전소", icon: "star-outline" as const, route: "/favorites" },
  { label: "충전기 알림 관리", icon: "notifications-outline" as const, route: "/charger-alerts" },
  { label: "내 리뷰", icon: "chatbubble-outline" as const, route: "/my-reviews" },
  { label: "공지사항", icon: "megaphone-outline" as const, route: "/notices" },
];

type Profile = {
  nickname: string;
  email: string | null;
  imageUrl: string | null;
};

export default function MypageScreen() {
  const [token, setToken] = useState<string | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        const t = await AsyncStorage.getItem("jwt_token");
        setToken(t);
        if (t) {
          try {
            const res = await api.get("/user/profile");
            setProfile(res.data);
          } catch {
            setProfile(null);
          }
        } else {
          setProfile(null);
        }
      })();
    }, [])
  );

  const handleLogout = () => {
    Alert.alert("로그아웃", "로그아웃 하시겠어요?", [
      { text: "취소", style: "cancel" },
      {
        text: "로그아웃",
        style: "destructive",
        onPress: async () => {
          try {
            const accessToken = await AsyncStorage.getItem("jwt_token");
            const refreshToken = await AsyncStorage.getItem("refresh_token");
            if (accessToken && refreshToken) {
              await api.post("/auth/logout", null, {
                params: { accessToken, refreshToken },
              });
            }
          } catch {
            // 서버 오류여도 로컬 로그아웃 진행
          } finally {
            await AsyncStorage.multiRemove(["jwt_token", "refresh_token", "user_name", "user_email"]);
            setToken(null);
            setProfile(null);
          }
        },
      },
    ]);
  };

  return (
    <SafeAreaView style={S.container} edges={["top", "bottom"]}>
      <View style={S.header}>
        <Text style={S.headerTitle}>마이페이지</Text>
        <View style={S.headerIcons}>
          <TouchableOpacity style={S.iconBtn} onPress={() => router.push("/charger-alert-history" as any)}>
            <Ionicons name="notifications-outline" size={24} color="#333" />
          </TouchableOpacity>
          <TouchableOpacity style={S.iconBtn}>
            <Ionicons name="settings-outline" size={24} color="#333" />
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView style={S.scroll}>
        {/* 프로필 카드 */}
        {token && profile ? (
          <View style={S.profileCard}>
            {profile.imageUrl ? (
              <Image source={{ uri: profile.imageUrl }} style={S.profileImage} />
            ) : (
              <View style={[S.profileImage, S.profileImageFallback]}>
                <Ionicons name="person" size={34} color="#5B9CF6" />
              </View>
            )}
            <View style={S.profileInfo}>
              <Text style={S.profileName}>{profile.nickname || "사용자"}</Text>
              <Text style={S.profileEmail}>{profile.email || ""}</Text>
            </View>
            <TouchableOpacity onPress={() => router.push("/login" as any)}>
              <Ionicons name="create-outline" size={20} color="#aaa" />
            </TouchableOpacity>
          </View>
        ) : (
          <TouchableOpacity style={S.profileCard} onPress={() => router.push("/login" as any)}>
            <View style={[S.profileImage, S.profileImageFallback]}>
              <Ionicons name="person-outline" size={34} color="#999" />
            </View>
            <View style={S.profileInfo}>
              <Text style={S.loginText}>로그인하기</Text>
              <Text style={S.loginSub}>탭하여 로그인하세요</Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color="#ccc" />
          </TouchableOpacity>
        )}

        {/* 메뉴 카드 */}
        <View style={S.card}>
          {MENU_ITEMS.map((item, index) => (
            <TouchableOpacity
              key={item.label}
              style={[S.menuItem, index < MENU_ITEMS.length - 1 && S.menuBorder]}
              onPress={() => router.push(item.route as any)}
            >
              <View style={S.menuLeft}>
                <Ionicons name={item.icon} size={20} color="#555" style={S.menuIcon} />
                <Text style={S.menuLabel}>{item.label}</Text>
              </View>
              <Ionicons name="chevron-forward" size={18} color="#ccc" />
            </TouchableOpacity>
          ))}
        </View>

        {token && (
          <View style={S.card}>
            <TouchableOpacity style={S.menuItem} onPress={handleLogout}>
              <View style={S.menuLeft}>
                <Ionicons name="log-out-outline" size={20} color="#e53935" style={S.menuIcon} />
                <Text style={S.logoutText}>로그아웃</Text>
              </View>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f5f5" },
  header: {
    flexDirection: "row", justifyContent: "space-between", alignItems: "center",
    paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff",
  },
  headerTitle: { fontSize: 18, fontWeight: "bold" },
  headerIcons: { flexDirection: "row" },
  iconBtn: { marginLeft: 16 },
  scroll: { padding: 12 },

  profileCard: {
    flexDirection: "row", alignItems: "center", backgroundColor: "#fff",
    borderRadius: 12, marginBottom: 12, paddingHorizontal: 16, paddingVertical: 16,
  },
  profileImage: { width: 52, height: 52, borderRadius: 26 },
  profileImageFallback: { backgroundColor: "#EBF3FF", alignItems: "center", justifyContent: "center" },
  profileInfo: { flex: 1, marginLeft: 14 },
  profileName: { fontSize: 16, fontWeight: "700", color: "#111" },
  profileEmail: { fontSize: 13, color: "#888", marginTop: 2 },
  loginText: { fontSize: 16, fontWeight: "600", color: "#111" },
  loginSub: { fontSize: 12, color: "#aaa", marginTop: 2 },

  card: { backgroundColor: "#fff", borderRadius: 12, marginBottom: 12, paddingHorizontal: 16, overflow: "hidden" },
  menuItem: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 16 },
  menuBorder: { borderBottomWidth: 1, borderBottomColor: "#f0f0f0" },
  menuLeft: { flexDirection: "row", alignItems: "center" },
  menuIcon: { marginRight: 12 },
  menuLabel: { fontSize: 15, color: "#222" },
  logoutText: { fontSize: 15, color: "#e53935" },
});
