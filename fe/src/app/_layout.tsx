import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";

export default function RootLayout() {
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
      </Stack>
    </>
  );
}