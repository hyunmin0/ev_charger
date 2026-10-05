import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  FlatList,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView, useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
const ACCENT = "#5B9CF6";
const CARS = ["선택 안함", "현대 아이오닉 5", "기아 EV9"];
type Msg = { id: string; role: "user" | "bot"; text: string };

const INITIAL_MSGS: Msg[] = [
  {
    id: "0",
    role: "bot",
    text: "안녕하세요! 저는 전기차 AI 챗봇이에요.\n충전소 추천부터 전기차 관련 정보까지 도와드립니다.\n\n• 근처 충전소 찾기\n• 충전 요금 비교\n• 전기차 주행 가능 거리 계산\n• 충전기 타입별 안내\n\n이런 질문을 해보세요!",
  },
  {
    id: "1",
    role: "user",
    text: "성심당 근처 지하 충전소 추천해줘",
  },
  {
    id: "2",
    role: "bot",
    text: "아직 LLM 프롬프트 수정중이라서\n답변을 생각하지 못했어요. 😅\n조금만 기다려주세요!",
  },
];

export default function ChatScreen() {
  const [car, setCar] = useState("선택 안함");
  const [dropVisible, setDropVisible] = useState(false);
  const [msgs, setMsgs] = useState<Msg[]>([...INITIAL_MSGS]);
  const [input, setInput] = useState("");
  const send = async () => {
  const text = input.trim();
  if (!text) return;

  const stored = await AsyncStorage.getItem("mapLocation");
  const location = stored ? JSON.parse(stored) : null;

  // 대화 히스토리 변환 (현재 메시지 제외한 이전 것들)
  const history = msgs.map((m) => ({
    role: m.role === "user" ? "user" : "assistant",
    content: m.text,
  }));

  const payload = {
    user_id: "3fa85f64-5717-4562-b3fc-2c963f66afa6", // 나중에 실제 로그인 유저 ID로 교체
    car_id: null, // 나중에 선택된 차량 ID로 교체
    message: text,
    history: history,
    lat: location?.lat ?? null,
    lng: location?.lng ?? null,
  };

  console.log("전송:", JSON.stringify(payload, null, 2));
  // 백엔드 연결 시: const res = await axios.post("/api/chat", payload);

  const userMsg: Msg = { id: Date.now().toString(), role: "user", text };
  const botMsg: Msg = {
    id: (Date.now() + 1).toString(),
    role: "bot",
    text: "아직 LLM 프롬프트 수정중이라서\n답변을 생각하지 못했어요. 😅",
  };
  setMsgs((prev) => [...prev, userMsg, botMsg]);
  setInput("");
};

  return (
    <SafeAreaView style={s.safe} edges={["top"]}>
      {/* 헤더 */}
      <View style={[s.header, {paddingTop: 14
      }]}>
        <Ionicons name="car-outline" size={20} color="#444" />
        <TouchableOpacity style={s.carBtn} onPress={() => setDropVisible((v) => !v)}>
          <Text style={s.carText}>{car}</Text>
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
            {CARS.map((c) => (
              <TouchableOpacity
                key={c}
                style={[s.dropItem, c === car && s.dropItemActive]}
                onPress={() => { setCar(c); setDropVisible(false); }}
              >
                <Text style={[s.dropItemText, c === car && s.dropItemTextActive]}>{c}</Text>
                {c === car && <Ionicons name="checkmark" size={16} color={ACCENT} />}
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
          data={msgs}
          keyExtractor={(m) => m.id}
          contentContainerStyle={s.list}
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
                <View style={s.bubbleBot}>
                  <Text style={s.bubbleBotText}>{item.text}</Text>
                </View>
              </View>
            )
          }
          ItemSeparatorComponent={() => <View style={{ height: 12 }} />}
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
            style={[s.sendBtn, !input.trim() && s.sendBtnOff]}
            onPress={send}
            disabled={!input.trim()}
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
    maxWidth: "72%",
    shadowColor: "#000",
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  bubbleBotText: { fontSize: 14, color: "#222", lineHeight: 21 },
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