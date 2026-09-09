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
import { BACKEND_URL } from "@/lib/api";

const CALLBACK_PREFIX = "evcharger://oauth/callback";

type Provider = "kakao" | "google";

const KAKAO_REST_API_KEY = process.env.EXPO_PUBLIC_KAKAO_REST_KEY ?? "";
const KAKAO_REDIRECT_URI = "http://localhost/";
const KAKAO_AUTH_URL =
  `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_REST_API_KEY}` +
  `&redirect_uri=${encodeURIComponent(KAKAO_REDIRECT_URI)}&response_type=code`;

const GOOGLE_WEB_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? "";

export default function LoginScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
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
      const loginRes = await fetch(
        `${BACKEND_URL}/auth/login/kakao/code?code=${encodeURIComponent(code)}`,
        { method: "POST" }
      );
      await handleLoginResponse(loginRes);
    } catch (e) {
      console.error("OAuth error", e);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      await GoogleSignin.hasPlayServices();
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