import React, { useState, useEffect, useRef } from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import axios from "axios";
import api from "@/lib/api";

const ACCENT = "#5B9CF6";
const AI_URL = process.env.EXPO_PUBLIC_AI_URL ?? "";
const INTERNAL_KEY = process.env.EXPO_PUBLIC_INTERNAL_KEY ?? "";

type Station = { statId: string; statNm: string; addr: string; parkingFree: string; distance_km: number };
type Msg = { id: string; role: "user" | "bot"; text: string; stations?: Station[] };
type CarOption = { label: string; carId: number | null };

const INITIAL_MSGS: Msg[] = [
  {
    id: "0",
    role: "bot",
    text: "안녕하세요! 저는 전기차 AI 챗봇이에요.\n충전소 추천부터 전기차 관련 정보까지 도와드립니다.\n\n• 근처 충전소 찾기\n• 충전 요금 비교\n• 전기차 주행 가능 거리 계산\n• 충전기 타입별 안내\n\n이런 질문을 해보세요!",
  },
];

function decodeJwt(token: string): any {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export default function ChatScreen() {
  const [carList, setCarList] = useState<CarOption[]>([{ label: "선택 안함", carId: null }]);
  const [selectedCar, setSelectedCar] = useState<CarOption>({ label: "선택 안함", carId: null });
  const [dropVisible, setDropVisible] = useState(false);
  const [msgs, setMsgs] = useState<Msg[]>([...INITIAL_MSGS]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [userId, setUserId] = useState<string | null>(null);
  const flatListRef = useRef<FlatList>(null);

  useEffect(() => {
    (async () => {
      // JWT에서 user_id 가져오기
      const token = await AsyncStorage.getItem("jwt_token");
      if (token) {
        const payload = decodeJwt(token);
        setUserId(payload?.sub ?? null);
      }

      // 내 차량 목록 가져오기
      try {
        const res = await api.get("/user/userCar");
        const cars: CarOption[] = [
          { label: "선택 안함", carId: null },
          ...(res.data ?? []).map((c: any) => ({ label: c.carName, carId: c.carId })),
        ];
        setCarList(cars);
      } catch {
        // 차량 목록 못 불러와도 계속 진행
      }
    })();
  }, []);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;

    const stored = await AsyncStorage.getItem("mapLocation");
    const location = stored ? JSON.parse(stored) : null;

    // 대화 히스토리 변환 (초기 안내 메시지 제외)
    const history = msgs
      .filter(m => m.id !== "0")
      .map(m => ({
        role: m.role === "user" ? "user" : "assistant",
        content: m.text,
      }));

    const userMsg: Msg = { id: Date.now().toString(), role: "user", text };
    setMsgs(prev => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      const res = await axios.post(
        `${AI_URL}/chat`,
        {
          user_id: userId ?? "00000000-0000-0000-0000-000000000000",
          car_id: selectedCar.carId,
          message: text,
          history,
          lat: location?.lat ?? null,
          lng: location?.lng ?? null,
        },
        {
          headers: { "X-Internal-Key": INTERNAL_KEY },
          timeout: 30000,
        }
      );

      const botMsg: Msg = {
        id: (Date.now() + 1).toString(),
        role: "bot",
        text: res.data.reply,
        stations: res.data.stations?.length > 0 ? res.data.stations : undefined,
      };
      setMsgs(prev => [...prev, botMsg]);
    } catch {
      setMsgs(prev => [...prev, {
        id: (Date.now() + 1).toString(),
        role: "bot",
        text: "죄송해요, 응답을 받지 못했어요. 다시 시도해주세요.",
      }]);
    } finally {
      setLoading(false);
    }
  };

  const renderStation = (station: Station) => (
    <View key={station.statId} style={s.stationCard}>
      <Text style={s.stationName} numberOfLines={1}>{station.statNm}</Text>
      <Text style={s.stationAddr} numberOfLines={1}>{station.addr}</Text>
      <View style={s.stationRow}>
        <Text style={s.stationDist}>{station.distance_km.toFixed(1)}km</Text>
        {station.parkingFree === "Y" && (
          <View style={s.parkingBadge}><Text style={s.parkingTxt}>무료주차</Text></View>
        )}
      </View>
    </View>
  );

  return (
    <SafeAreaView style={s.safe} edges={["top"]}>
      {/* 헤더 */}
      <View style={[s.header, { paddingTop: 14 }]}>
        <Ionicons name="car-outline" size={20} color="#444" />
        <TouchableOpacity style={s.carBtn} onPress={() => setDropVisible(v => !v)}>
          <Text style={s.carText}>{selectedCar.label}</Text>
          <Ionicons
            name={dropVisible ? "chevron-up-outline" : "chevron-down-outline"}
            size={16}
            color="#444"
            style={{ marginLeft: 2 }}
          />
        </TouchableOpacity>
      </View>

      {/* 드롭다운 - 헤더 바로 아래 인라인 */}
      {dropVisible && (
        <>
          <TouchableOpacity
            style={StyleSheet.absoluteFill}
            activeOpacity={1}
            onPress={() => setDropVisible(false)}
          />
          <View style={s.dropdown}>
            {carList.map((c) => (
              <TouchableOpacity
                key={c.label}
                style={[s.dropItem, c.label === selectedCar.label && s.dropItemActive]}
                onPress={() => { setSelectedCar(c); setDropVisible(false); }}
              >
                <Text style={[s.dropItemText, c.label === selectedCar.label && s.dropItemTextActive]}>{c.label}</Text>
                {c.label === selectedCar.label && <Ionicons name="checkmark" size={16} color={ACCENT} />}
              </TouchableOpacity>
            ))}
          </View>
        </>
      )}

      {/* 메시지 + 입력바 */}
      <KeyboardAvoidingView
        style={s.flex}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        keyboardVerticalOffset={60}
      >
        <FlatList
          ref={flatListRef}
          data={msgs}
          keyExtractor={m => m.id}
          contentContainerStyle={s.list}
          onContentSizeChange={() => flatListRef.current?.scrollToEnd({ animated: true })}
          renderItem={({ item }) =>
            item.role === "user" ? (
              <View style={s.rowUser}>
                <View style={s.bubbleUser}>
                  <Text style={s.bubbleUserText}>{item.text}</Text>
                </View>
              </View>
            ) : (
              <View style={s.rowBot}>
                <View style={s.botAvatar}>
                  <Ionicons name="flash" size={14} color="#fff" />
                </View>
                <View style={{ flex: 1 }}>
                  <View style={s.bubbleBot}>
                    <Text style={s.bubbleBotText}>{item.text}</Text>
                  </View>
                  {item.stations && item.stations.length > 0 && (
                    <View style={s.stationList}>
                      {item.stations.map(renderStation)}
                    </View>
                  )}
                </View>
              </View>
            )
          }
          ItemSeparatorComponent={() => <View style={{ height: 12 }} />}
          ListFooterComponent={
            loading ? (
              <View style={[s.rowBot, { marginTop: 12 }]}>
                <View style={s.botAvatar}>
                  <Ionicons name="flash" size={14} color="#fff" />
                </View>
                <View style={[s.bubbleBot, { paddingHorizontal: 16 }]}>
                  <ActivityIndicator size="small" color={ACCENT} />
                </View>
              </View>
            ) : null
          }
        />

        {/* 입력바 */}
        <View style={s.inputRow}>
          <TextInput
            style={s.input}
            value={input}
            onChangeText={setInput}
            placeholder="채팅을 입력하세요."
            placeholderTextColor="#bbb"
            multiline
            returnKeyType="default"
          />
          <TouchableOpacity
            style={[s.sendBtn, (!input.trim() || loading) && s.sendBtnOff]}
            onPress={send}
            disabled={!input.trim() || loading}
          >
            <Ionicons name="arrow-up" size={20} color="#fff" />
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: "#f4f6fa" },
  flex: { flex: 1 },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingBottom: 14,
    backgroundColor: "#fff",
    borderBottomWidth: 1,
    borderBottomColor: "#ececec",
    gap: 8,
    zIndex: 10,
  },
  carBtn: { flexDirection: "row", alignItems: "center" },
  carText: { fontSize: 16, fontWeight: "600", color: "#222" },
  dropdown: {
    position: "absolute",
    top: 57,
    left: 16,
    right: 16,
    zIndex: 100,
    backgroundColor: "#fff",
    borderRadius: 14,
    shadowColor: "#000",
    shadowOpacity: 0.12,
    shadowRadius: 10,
    elevation: 10,
    overflow: "hidden",
  },
  dropItem: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 18,
    paddingVertical: 15,
    borderBottomWidth: 1,
    borderBottomColor: "#f3f3f3",
  },
  dropItemActive: { backgroundColor: "#EBF3FF" },
  dropItemText: { fontSize: 15, color: "#333" },
  dropItemTextActive: { color: ACCENT, fontWeight: "600" },
  list: { paddingHorizontal: 16, paddingVertical: 16 },
  rowUser: { flexDirection: "row", justifyContent: "flex-end" },
  rowBot: { flexDirection: "row", alignItems: "flex-start", gap: 10 },
  botAvatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: ACCENT,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 2,
    flexShrink: 0,
  },
  bubbleUser: {
    backgroundColor: "#e2e5ea",
    borderRadius: 18,
    borderBottomRightRadius: 4,
    paddingHorizontal: 14,
    paddingVertical: 10,
    maxWidth: "72%",
  },
  bubbleUserText: { fontSize: 14, color: "#222", lineHeight: 21 },
  bubbleBot: {
    backgroundColor: "#fff",
    borderRadius: 18,
    borderBottomLeftRadius: 4,
    paddingHorizontal: 14,
    paddingVertical: 10,
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  bubbleBotText: { fontSize: 14, color: "#222", lineHeight: 21 },
  stationList: { marginTop: 8, gap: 8 },
  stationCard: {
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 12,
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  stationName: { fontSize: 14, fontWeight: "700", color: "#111", marginBottom: 2 },
  stationAddr: { fontSize: 12, color: "#888", marginBottom: 6 },
  stationRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  stationDist: { fontSize: 12, color: ACCENT, fontWeight: "600" },
  parkingBadge: { backgroundColor: "#EBF3FF", borderRadius: 4, paddingHorizontal: 6, paddingVertical: 2 },
  parkingTxt: { fontSize: 11, color: ACCENT },
  inputRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 16,
    backgroundColor: "#fff",
    borderTopWidth: 1,
    borderTopColor: "#ececec",
    gap: 10,
  },
  input: {
    flex: 1,
    backgroundColor: "#f2f3f5",
    borderRadius: 22,
    paddingHorizontal: 16,
    paddingTop: 11,
    paddingBottom: 11,
    fontSize: 14,
    color: "#222",
    maxHeight: 100,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: ACCENT,
    alignItems: "center",
    justifyContent: "center",
  },
  sendBtnOff: { backgroundColor: "#c8d8f8" },
});