import { useEffect } from "react";
import { Stack, router } from "expo-router";
import { StatusBar } from "expo-status-bar";
import * as Notifications from "expo-notifications";
import { registerPushToken } from "@/lib/push";

export default function RootLayout() {
  const lastResponse = Notifications.useLastNotificationResponse();

  // 앱 실행 시 알림 권한 요청 + FCM 토큰 등록 (로그인 상태일 때만. 로그인 직후에는 login/register 화면이 등록)
  useEffect(() => {
    registerPushToken({ ask: true });
    // 토큰은 드물게 바뀜 -> 바뀐 토큰으로 다시 등록
    const sub = Notifications.addPushTokenListener(({ data }) => registerPushToken({ ask: false, token: data }));
    return () => sub.remove();
  }, []);

  // 알림을 누르면 알림 기록 화면으로 (앱이 꺼져 있다가 알림으로 켜진 경우 포함)
  useEffect(() => {
    if (lastResponse?.actionIdentifier === Notifications.DEFAULT_ACTION_IDENTIFIER) {
      router.push("/charger-alert-history" as any);
      Notifications.clearLastNotificationResponse();
    }
  }, [lastResponse]);

  return (
    <>
      {/* edge-to-edge라 상태바 배경은 투명 -> 뒤에 깔린 화면 배경(#fff)이 보임 */}
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: "#fff" } }}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="login" />
        <Stack.Screen name="register" />
        <Stack.Screen name="station/[id]" />
        <Stack.Screen name="car-management" />
        <Stack.Screen name="charger-alert-history" />
        <Stack.Screen name="favorites" />
        <Stack.Screen name="charger-alerts" />
        <Stack.Screen name="my-reviews" />
        <Stack.Screen name="notices" />
        <Stack.Screen name="profile-edit" />
        <Stack.Screen name="settings" />
      </Stack>
    </>
  );
}
