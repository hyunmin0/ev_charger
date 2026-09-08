import React, { useEffect, useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, Modal, ActivityIndicator,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView, WebViewNavigation } from "react-native-webview";
import { GoogleSignin } from "@react-native-google-signin/google-signin";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";

// USB로 연결해서 adb reverse tcp:8080 tcp:8080으로 포트를 넘겨받는 구성이라 localhost로 접근함
// (와이파이로 테스트할 땐 대신 컴퓨터의 LAN IP를 넣어야 함)
const BACKEND_URL = "http://localhost:8080";

const KAKAO_REST_API_KEY = "da7c455f848e4647403a7998bdb5ff6d";
const KAKAO_REDIRECT_URI = "http://localhost/";
const KAKAO_AUTH_URL =
  `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_REST_API_KEY}` +
  `&redirect_uri=${encodeURIComponent(KAKAO_REDIRECT_URI)}&response_type=code`;

// 네이티브 구글 로그인 SDK가 서버에서 검증 가능한 토큰을 받으려면
// "웹 애플리케이션" 타입 클라이언트 ID가 필요함 (Android 클라이언트 ID와는 다름)
const GOOGLE_WEB_CLIENT_ID = "386508397583-v6tuoduhkk5o7abv9shbgeg4nakoc8ll.apps.googleusercontent.com";

type Provider = "kakao" | "google";

export default function LoginScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();   // ← 핵심 fix
  const [oauthUrl, setOauthUrl] = useState<string | null>(null);
  const [webLoading, setWebLoading] = useState(false);

  useEffect(() => {
    GoogleSignin.configure({ webClientId: GOOGLE_WEB_CLIENT_ID });
  }, []);

  const handleNavChange = async (nav: WebViewNavigation) => {
    const url = nav.url;
    if (!url.startsWith("http://localhost")) return;

    const code = new URLSearchParams(url.split("?")[1] ?? "").get("code");
    if (!code) return;

    setOauthUrl(null);

    try {
      // 카카오: code만 보내면 백엔드가 토큰 교환까지 처리 (client_secret 이슈 회피)
      const loginRes = await fetch(
        `${BACKEND_URL}/auth/login/kakao/code?code=${encodeURIComponent(code)}`,
        { method: "POST" }
      );
      await handleLoginResponse(loginRes);
    } catch (e) {
      console.error("OAuth error", e);
    }
  };

  // 구글은 WebView/브라우저 리다이렉트 로그인을 막아놔서 네이티브 SDK를 써야 함
  const handleGoogleLogin = async () => {
    try {
      await GoogleSignin.hasPlayServices();
      // 이전에 승인한 계정이 있으면 선택 창 없이 조용히 로그인되므로, 매번 계정 선택 창이 뜨도록 먼저 로그아웃
      await GoogleSignin.signOut();
      await GoogleSignin.signIn();
      const { accessToken } = await GoogleSignin.getTokens();

      const loginRes = await fetch(
        `${BACKEND_URL}/auth/login?accessToken=${accessToken}&provider=GOOGLE`,
        { method: "POST" }
      );
      await handleLoginResponse(loginRes);
    } catch (e) {
      console.error("OAuth error", e);
    }
  };

  const handleLoginResponse = async (res: Response) => {
    const data = await res.json();
    if (data.status === "SUCCESS") {
      await AsyncStorage.multiSet([
        ["jwt_token", data.accessToken],
        ["refresh_token", data.refreshToken ?? ""],
      ]);
      router.replace("/(tabs)/mypage" as any);
    } else if (data.status === "NEED_PROFILE_SELECT") {
      router.replace({ pathname: "/register", params: { tempToken: data.tempToken } } as any);
    }
  };

  return (
    <SafeAreaView style={S.container} edges={["bottom"]}>
      <TouchableOpacity style={S.backBtn} onPress={() => router.back()}>
        <Ionicons name="chevron-back" size={24} color="#111" />
      </TouchableOpacity>

      <View style={S.logoSection}>
        <View style={S.logoCircle}>
          <Ionicons name="flash" size={44} color="#5B9CF6" />
        </View>
        <Text style={S.appName}>EV 충전</Text>
        <Text style={S.tagline}>EV_CHARGER</Text>
      </View>

      <View style={S.btnSection}>
        <TouchableOpacity style={[S.oauthBtn, S.kakaoBtn]} onPress={() => setOauthUrl(KAKAO_AUTH_URL)}>
          <Text style={[S.oauthTxt, S.kakaoTxt]}>카카오로 로그인</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[S.oauthBtn, S.googleBtn]} onPress={handleGoogleLogin}>
          <Text style={[S.oauthTxt, S.googleTxt]}>구글로 로그인</Text>
        </TouchableOpacity>
      </View>

      <Text style={S.notice}>
        로그인 시 서비스 이용약관 및{"\n"}개인정보 처리방침에 동의하게 됩니다.
      </Text>

      <Modal
        visible={!!oauthUrl}
        animationType="slide"
        onRequestClose={() => setOauthUrl(null)}
      >
        {/* SafeAreaView 대신 View + paddingTop: insets.top — Modal 안에서 확실히 동작 */}
         <View style={[S.modalContainer, { paddingTop: Math.max(insets.top - 20, 0) }]}>
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
              incognito={true}
              javaScriptEnabled
              domStorageEnabled
              style={{ flex: 1 }}
            />
          )}
        </View>
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
  kakaoTxt: { color: "#3C1E1E" },
  googleBtn: { backgroundColor: "#fff", borderWidth: 1.5, borderColor: "#e0e0e0" },
  googleTxt: { color: "#333" },

  notice: { fontSize: 11, color: "#bbb", textAlign: "center", lineHeight: 17, marginBottom: 32 },

  modalContainer: { flex: 1, backgroundColor: "#fff" },
  wvHeader: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
    backgroundColor: "#fff",
  },
  wvClose: { padding: 10, width: 44 },
  wvTitle: { fontSize: 16, fontWeight: "700", color: "#111" },
  wvLoader: {
    ...StyleSheet.absoluteFillObject,
    alignItems: "center", justifyContent: "center", backgroundColor: "#fff", zIndex: 1,
  },
});