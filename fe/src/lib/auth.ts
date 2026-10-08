import { Alert } from "react-native";
import { router } from "expo-router";
import AsyncStorage from "@react-native-async-storage/async-storage";

// 로그인 안 돼 있으면 로그인 유도 팝업을 띄우고 false 반환
export async function requireLogin(): Promise<boolean> {
  const token = await AsyncStorage.getItem("jwt_token");
  if (token) return true;

  Alert.alert("로그인 필요", "로그인이 필요한 기능이에요.\n로그인하시겠어요?", [
    { text: "취소", style: "cancel" },
    { text: "로그인", onPress: () => router.push("/login" as any) },
  ]);
  return false;
}
