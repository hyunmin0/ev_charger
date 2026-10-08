import * as Notifications from "expo-notifications";
import AsyncStorage from "@react-native-async-storage/async-storage";
import api from "@/lib/api";

// app.json의 expo-notifications 플러그인 defaultChannel과 같아야 FCM 알림이 이 채널로 옴
export const PUSH_CHANNEL_ID = "default";

// 설정 화면 토글 값 (이 기기 기준). 없으면 켜짐으로 봄
const PUSH_ENABLED_KEY = "push_enabled";

// 앱을 보고 있을 때 온 알림도 상단 배너로 보여줌
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

// 상단 팝업(헤드업)은 중요도 HIGH 이상 채널에서만 뜸
export async function ensurePushChannel() {
  await Notifications.setNotificationChannelAsync(PUSH_CHANNEL_ID, {
    name: "충전기 알림",
    importance: Notifications.AndroidImportance.HIGH,
  });
}

export async function isPushEnabled() {
  return (await AsyncStorage.getItem(PUSH_ENABLED_KEY)) !== "0";
}

export async function setPushEnabled(enabled: boolean) {
  await AsyncStorage.setItem(PUSH_ENABLED_KEY, enabled ? "1" : "0");
}

// 이번 실행에서 be에 등록한 토큰. 같은 토큰을 반복해서 보내지 않기 위함
let registeredToken: string | null = null;

/**
 * 알림 권한을 확인(ask면 요청)하고 이 기기의 FCM 토큰을 be에 등록
 * 로그인 안 했거나, 토글을 껐거나, 권한이 없으면 등록하지 않음
 * @param token 토큰 변경 이벤트로 받은 토큰. 있으면 다시 조회하지 않음
 * @returns 등록됐으면(이미 등록된 경우 포함) true
 */
export async function registerPushToken({ ask, token }: { ask: boolean; token?: string }) {
  try {
    if (!(await AsyncStorage.getItem("jwt_token"))) return false;
    if (!(await isPushEnabled())) return false;

    await ensurePushChannel();
    let perm = await Notifications.getPermissionsAsync();
    if (!perm.granted && ask && perm.canAskAgain) {
      perm = await Notifications.requestPermissionsAsync();
    }
    if (!perm.granted) return false;

    // Android의 getDevicePushTokenAsync는 토큰 변경 이벤트도 같이 보냄 -> 이벤트에서 부를 땐 받은 토큰을 씀
    const fcmToken = token ?? (await Notifications.getDevicePushTokenAsync()).data;
    if (fcmToken === registeredToken) return true;

    await api.post("/user/fcm-token", null, { params: { fcmToken } });
    registeredToken = fcmToken;
    return true;
  } catch (e) {
    console.warn("FCM 토큰 등록 실패", e);
    return false;
  }
}

// 로그아웃·토글 끄기: 이 기기로 알림이 오지 않게 be에서 토큰 삭제 (로그인 상태에서 호출)
export async function unregisterPushToken() {
  try {
    const fcmToken = registeredToken ?? (await Notifications.getDevicePushTokenAsync()).data;
    await api.delete("/user/fcm-token", { params: { fcmToken } });
    registeredToken = null;
  } catch (e) {
    console.warn("FCM 토큰 삭제 실패", e);
  }
}
