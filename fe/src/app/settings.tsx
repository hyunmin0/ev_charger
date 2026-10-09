import React, { useCallback, useEffect, useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, Switch, Alert, Linking, AppState,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useRouter, useFocusEffect } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as Notifications from "expo-notifications";
import {
  ensurePushPermission, isPushEnabled, setPushEnabled, registerPushToken, unregisterPushToken,
} from "@/lib/push";

const ACCENT = "#5B9CF6";

export default function SettingsScreen() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);
  // 토글 표시값 = 이 기기에서 켰음 && 휴대폰 알림 권한 허용
  const [pushOn, setPushOn] = useState(false);
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    const token = await AsyncStorage.getItem("jwt_token");
    const enabled = await isPushEnabled();
    const perm = await Notifications.getPermissionsAsync();
    setLoggedIn(!!token);
    setPushOn(!!token && enabled && perm.granted);
    // 휴대폰 설정에서 권한을 허용하고 돌아온 경우 토큰 등록
    if (token && enabled && perm.granted) registerPushToken({ ask: false });
  }, []);

  useFocusEffect(useCallback(() => { refresh(); }, [refresh]));

  // 휴대폰 설정 화면에 갔다가 돌아오면 권한이 바뀌었을 수 있음
  useEffect(() => {
    const sub = AppState.addEventListener("change", (state) => {
      if (state === "active") refresh();
    });
    return () => sub.remove();
  }, [refresh]);

  const turnOn = async () => {
    await setPushEnabled(true);
    if (!(await ensurePushPermission())) {
      // 두 번 거절하면 앱에서 다시 물을 수 없어서 휴대폰 설정으로 보냄
      Alert.alert("알림 권한이 꺼져 있어요", "휴대폰 설정에서 이 앱의 알림을 허용해 주세요.", [
        { text: "취소", style: "cancel" },
        { text: "설정 열기", onPress: () => Linking.openSettings() },
      ]);
      return;
    }
    if (!(await registerPushToken({ ask: false }))) {
      await setPushEnabled(false);
      Alert.alert("오류", "알림을 켜지 못했어요. 잠시 후 다시 시도해주세요.");
    }
  };

  const turnOff = async () => {
    await setPushEnabled(false);
    await unregisterPushToken();
  };

  const onToggle = async (value: boolean) => {
    setBusy(true);
    try {
      if (value) await turnOn();
      else await turnOff();
    } finally {
      await refresh();
      setBusy(false);
    }
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>설정</Text>
        <View style={S.headerBtn} />
      </View>

      <View style={S.card}>
        <View style={S.row}>
          <View style={S.rowText}>
            <Text style={S.rowTitle}>충전기 알림 받기</Text>
            <Text style={S.rowSub}>
              {loggedIn
                ? "알림 설정한 충전기가 사용 가능해지면 이 기기로 알려드려요"
                : "로그인 후 이용할 수 있어요"}
            </Text>
          </View>
          <Switch
            value={pushOn}
            onValueChange={onToggle}
            disabled={!loggedIn || busy}
            trackColor={{ true: ACCENT, false: "#ddd" }}
            thumbColor="#fff"
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f6fa" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  headerBtn: { padding: 10, width: 44 },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  card: { backgroundColor: "#fff", borderRadius: 14, margin: 16, paddingHorizontal: 16 },
  row: { flexDirection: "row", alignItems: "center", paddingVertical: 16, gap: 12 },
  rowText: { flex: 1 },
  rowTitle: { fontSize: 15, fontWeight: "600", color: "#111", marginBottom: 4 },
  rowSub: { fontSize: 12, color: "#999", lineHeight: 17 },
});
