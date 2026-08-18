import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";

export default function RootLayout() {
  return (
    <>
      <StatusBar style="dark" backgroundColor="#fff" />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: "#fff" } }}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="login" />
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