import axios from "axios";
import AsyncStorage from "@react-native-async-storage/async-storage";

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

let isRefreshing = false;
let refreshQueue: Array<(token: string) => void> = [];

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;

    if (error.response?.status === 401 && !original._retry) {
      const refreshToken = await AsyncStorage.getItem("refresh_token");

      if (!refreshToken) {
        await AsyncStorage.multiRemove(["jwt_token", "refresh_token"]);
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise<string>((resolve) => {
          refreshQueue.push(resolve);
        }).then((newToken) => {
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
        });
      }

      original._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post(
          `${BACKEND_URL}/auth/reissue`,
          null,
          { params: { refreshToken } }
        );
        const { jwtAccessToken, jwtRefreshToken } = res.data;
        await AsyncStorage.multiSet([
          ["jwt_token", jwtAccessToken],
          ["refresh_token", jwtRefreshToken ?? ""],
        ]);
        refreshQueue.forEach((cb) => cb(jwtAccessToken));
        refreshQueue = [];
        original.headers.Authorization = `Bearer ${jwtAccessToken}`;
        return api(original);
      } catch {
        await AsyncStorage.multiRemove(["jwt_token", "refresh_token"]);
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;