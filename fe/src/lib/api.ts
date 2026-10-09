import axios from "axios";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Alert } from "react-native";
import { router } from "expo-router";

// ── 백엔드 주소 ────────────────────────────────────────────────
// iOS 시뮬레이터: "http://127.0.0.1:8080"
// 실기기(같은 와이파이): "http://192.168.x.x:8080" ← 맥북 IP로 바꿔
// 배포 후: "https://api.example.com"
export const BACKEND_URL = "http://127.0.0.1:8080";

const api = axios.create({
  baseURL: BACKEND_URL,
  timeout: 10000,
});

api.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem("jwt_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── 로그인 만료 처리 ────────────────────────────────────────────
// 화면(마이페이지 등)이 로그아웃을 바로 반영하도록 알림을 받을 수 있게 함
const logoutListeners = new Set<() => void>();
export function onSessionExpired(listener: () => void) {
  logoutListeners.add(listener);
  return () => { logoutListeners.delete(listener); };
}

let expiredAlertShown = false;
// 리프레시 토큰까지 만료 -> 로그아웃 (저장된 로그인 정보 삭제 + 안내 팝업 한 번)
async function expireSession() {
  const hadToken = await AsyncStorage.getItem("jwt_token");
  await AsyncStorage.multiRemove(["jwt_token", "refresh_token", "user_name", "user_email"]);
  logoutListeners.forEach((l) => l());
  // 여러 요청이 동시에 실패해도 팝업은 한 번만, 원래 비로그인이었으면 띄우지 않음
  if (!hadToken || expiredAlertShown) return;
  expiredAlertShown = true;
  Alert.alert("로그인이 만료됐어요", "다시 로그인해 주세요.", [
    { text: "닫기", style: "cancel", onPress: () => { expiredAlertShown = false; } },
    { text: "로그인", onPress: () => { expiredAlertShown = false; router.push("/login" as any); } },
  ]);
}

let isRefreshing = false;
// 재발급을 기다리는 요청들: 성공하면 새 토큰으로 다시 보내고, 실패하면 같이 실패 처리 (안 그러면 영원히 대기)
let refreshQueue: Array<{ resolve: (token: string) => void; reject: (e: unknown) => void }> = [];

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;

    if (error.response?.status === 401 && !original._retry) {
      const refreshToken = await AsyncStorage.getItem("refresh_token");

      if (!refreshToken) {
        await expireSession();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          refreshQueue.push({ resolve, reject });
        }).then((newToken) => {
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
        });
      }

      original._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post(`${BACKEND_URL}/auth/reissue`, { refreshToken });
        const { newAccessToken, newRefreshToken } = res.data; // ReissueResponse 필드명
        await AsyncStorage.multiSet([
          ["jwt_token", newAccessToken],
          ["refresh_token", newRefreshToken ?? ""],
        ]);
        refreshQueue.forEach(({ resolve }) => resolve(newAccessToken));
        refreshQueue = [];
        original.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(original);
      } catch (refreshError: any) {
        refreshQueue.forEach(({ reject }) => reject(error));
        refreshQueue = [];
        // 서버가 재발급을 거절했을 때만 로그아웃 (네트워크 오류면 토큰은 살아 있을 수 있어서 그대로 둠)
        if (refreshError?.response) await expireSession();
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    console.error(
      `[API Error] ${error.config?.method?.toUpperCase()} ${error.config?.url}`,
      `status: ${error.response?.status}`,
      error.response?.data ?? error.message
    );
    return Promise.reject(error);
  }
);

export default api;