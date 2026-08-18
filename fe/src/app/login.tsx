import React, { useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, Modal, ActivityIndicator,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView, WebViewNavigation } from "react-native-webview";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

// ── 백엔드 주소 (실제 서버 IP로 교체) ────────────────────────
// 안드로이드 에뮬레이터: "http://10.0.2.2:8080"
// 실기기: "http://192.168.x.x:8080" 또는 배포 URL
const BACKEND_URL = "http://10.0.2.2:8080";

// 백엔드가 OAuth 완료 후 리다이렉트하는 URL
// Spring Boot에서 이 URL로 ?token=JWT 붙여서 보내줘야 함
const CALLBACK_PREFIX = "evcharger://oauth/callback";

type Provider = "kakao" | "naver" | "google";

export default function LoginScreen() {
  const router = useRouter();
  const [oauthUrl, setOauthUrl] = useState<string | null>(null);
  const [webLoading, setWebLoading] = useState(false);

  const handleOAuth = (provider: Provider) => {
    // Spring Security OAuth2 기본 엔드포인트
    setOauthUrl(`${BACKEND_URL}/oauth2/authorization/${provider}`);
  };

  const handleNavChange = async (nav: WebViewNavigation) => {
    if (!nav.url.startsWith(CALLBACK_PREFIX)) return;

    // evcharger://oauth/callback?token=JWT&name=홍길동&email=...
    const query = nav.url.split("?")[1] ?? "";
    const params = Object.fromEntries(
      query.split("&").map(p => p.split("=").map(decodeURIComponent))
    );

    if (params.token) {
      await AsyncStorage.multiSet([
        ["jwt_token", params.token],
        ["user_name", params.name ?? ""],
        ["user_email", params.email ?? ""],
      ]);
      setOauthUrl(null);
      router.replace("/(tabs)/mypage" as any);
    }
  };

  return (
    <SafeAreaView style={S.container} edges={["bottom"]}>
      <TouchableOpacity style={S.backBtn} onPress={() => router.back()}>
        <Ionicons name="chevron-back" size={24} color="#111" />
      </TouchableOpacity>

      {/* 로고 */}
      <View style={S.logoSection}>
        <View style={S.logoCircle}>
          <Ionicons name="flash" size={44} color="#5B9CF6" />
        </View>
        <Text style={S.appName}>EV 충전</Text>
        <Text style={S.tagline}>EV_CHARGER</Text>
      </View>

      {/* 소셜 로그인 버튼 */}
      <View style={S.btnSection}>
  <TouchableOpacity style={[S.oauthBtn, S.kakaoBtn]} onPress={() => handleOAuth("kakao")}>
    <Text style={[S.oauthTxt, S.kakaoTxt]}>카카오로 로그인</Text>
  </TouchableOpacity>

  <TouchableOpacity style={[S.oauthBtn, S.googleBtn]} onPress={() => handleOAuth("google")}>
    <Text style={[S.oauthTxt, S.googleTxt]}>구글로 로그인</Text>
  </TouchableOpacity>
</View>

      <Text style={S.notice}>
        로그인 시 서비스 이용약관 및{"\n"}개인정보 처리방침에 동의하게 됩니다.
      </Text>

      {/* 인앱 OAuth WebView */}
      <Modal
        visible={!!oauthUrl}
        animationType="slide"
        onRequestClose={() => setOauthUrl(null)}
      >
        <SafeAreaView style={{ flex: 1, backgroundColor: "#fff" }} edges={["top"]}>
          <View style={S.wvHeader}>
            <TouchableOpacity onPress={() => setOauthUrl(null)} style={S.wvClose}>
              <Ionicons name="close" size={24} color="#111" />
            </TouchableOpacity>
            <Text style={S.wvTitle}>로그인</Text>
            <View style={{ width: 44 }} />
          </View>

          {webLoading && (
            <View style={S.wvLoader}>
              <ActivityIndicator size="large" color="#5B9CF6" />
            </View>
          )}

          {oauthUrl && (
            <WebView
              source={{ uri: oauthUrl }}
              onLoadStart={() => setWebLoading(true)}
              onLoadEnd={() => setWebLoading(false)}
              onNavigationStateChange={handleNavChange}
              javaScriptEnabled
              domStorageEnabled
              style={{ flex: 1 }}
            />
          )}
        </SafeAreaView>
      </Modal>
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  backBtn: { padding: 14 },

  logoSection: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  logoCircle: {
    width: 88, height: 88, borderRadius: 44,
    backgroundColor: "#EBF3FF", alignItems: "center", justifyContent: "center",
    marginBottom: 4,
  },
  appName: { fontSize: 26, fontWeight: "800", color: "#111" },
  tagline: { fontSize: 14, color: "#888" },

  btnSection: { paddingHorizontal: 24, gap: 12, marginBottom: 24 },
  oauthBtn: {
    flexDirection: "row", alignItems: "center", justifyContent: "center",
    height: 52, borderRadius: 12, gap: 10,
  },
  oauthTxt: { fontSize: 15, fontWeight: "600" },

  kakaoBtn: { backgroundColor: "#FEE500" },
  kakaoIcon: { fontSize: 18 },
  kakaoTxt: { color: "#3C1E1E" },

  naverBtn: { backgroundColor: "#03C75A" },
  naverIconWrap: {
    width: 22, height: 22, borderRadius: 4,
    backgroundColor: "#fff", alignItems: "center", justifyContent: "center",
  },
  naverIconTxt: { fontSize: 13, fontWeight: "900", color: "#03C75A" },
  naverTxt: { color: "#fff" },

  googleBtn: { backgroundColor: "#fff", borderWidth: 1.5, borderColor: "#e0e0e0" },
  googleIconWrap: {
    width: 22, height: 22, borderRadius: 11,
    backgroundColor: "#fff", alignItems: "center", justifyContent: "center",
    borderWidth: 1, borderColor: "#ddd",
  },
  googleIconTxt: { fontSize: 13, fontWeight: "900", color: "#4285F4" },
  googleTxt: { color: "#333" },

  notice: { fontSize: 11, color: "#bbb", textAlign: "center", lineHeight: 17, marginBottom: 32 },

  wvHeader: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  wvClose: { padding: 10, width: 44 },
  wvTitle: { fontSize: 16, fontWeight: "700", color: "#111" },
  wvLoader: {
    ...StyleSheet.absoluteFillObject,
    alignItems: "center", justifyContent: "center", backgroundColor: "#fff", zIndex: 1,
  },
});
