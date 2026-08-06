import React, { useRef, useState } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView,
  TextInput, PanResponder,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";

const ACCENT = "#5B9CF6";

const CARS = ["선택 안함", "현대 아이오닉 5", "기아 EV9"];
const CAR_CAPACITY: Record<string, number> = {
  "선택 안함": 64,
  "현대 아이오닉 5": 72.6,
  "기아 EV9": 99.8,
};
const CHARGERS = [
  { label: "완속 AC (3kW)", kw: 3, type: "완속" },
  { label: "완속 AC (7kW)", kw: 7, type: "완속" },
  { label: "급속 DC (50kW)", kw: 50, type: "급속" },
  { label: "급속 DC (100kW)", kw: 100, type: "급속" },
  { label: "급속 DC (350kW)", kw: 350, type: "급속" },
];

function Slider({ min, max, step, value, onChange }: {
  min: number; max: number; step: number;
  value: number; onChange: (v: number) => void;
}) {
  const viewRef = useRef<View>(null);
  const widthRef = useRef(0);
  const viewLeft = useRef(0);
  const [layoutWidth, setLayoutWidth] = useState(0);

  const move = (pageX: number) => {
    if (widthRef.current === 0) return;
    const x = pageX - viewLeft.current;
    const pct = Math.max(0, Math.min(1, x / widthRef.current));
    const raw = min + pct * (max - min);
    const stepped = Math.round(raw / step) * step;
    onChange(Math.max(min, Math.min(max, stepped)));
  };

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: (e) => move(e.nativeEvent.pageX),
      onPanResponderMove: (e) => move(e.nativeEvent.pageX),
    })
  ).current;

 const thumbPct = (value - min) / (max - min);

  return (
    <View
      ref={viewRef}
      style={sl.container}
      onLayout={(e) => {
        const w = e.nativeEvent.layout.width;
        widthRef.current = w;
        setLayoutWidth(w);
        viewRef.current?.measure((_x, _y, _w, _h, pageX) => {
          viewLeft.current = pageX;
        });
      }}
      {...panResponder.panHandlers}
    >
      <View style={sl.track}>
        <View style={[sl.fill, { width: `${thumbPct * 100}%` as any }]} />
      </View>
      <View style={[sl.thumb, { left: thumbPct * layoutWidth - 11 }]} />
    </View>
  );
}

export default function CalculatorScreen() {
  const [car, setCar] = useState("선택 안함");
  const [dropCar, setDropCar] = useState(false);
  const [dropCharger, setDropCharger] = useState(false);
  const [charger, setCharger] = useState(CHARGERS[0]);
  const [soc, setSoc] = useState(34);
  const [mode, setMode] = useState<"target" | "time">("target");
  const [targetSoc, setTargetSoc] = useState(60);
  const [availableMin, setAvailableMin] = useState("");
  const [result, setResult] = useState<string | null>(null);

  const isFast = charger.type === "급속";
  const targetMax = isFast ? 80 : 100;

  const calculate = () => {
    const capacity = CAR_CAPACITY[car];
    if (mode === "target") {
      if (targetSoc <= soc) { setResult("목표 배터리가 현재 잔량보다 낮아요."); return; }
      const hours = ((targetSoc - soc) / 100) * capacity / charger.kw;
      const totalMin = Math.round(hours * 60);
      if (totalMin >= 60) {
        const h = Math.floor(totalMin / 60), m = totalMin % 60;
        setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n약 ${h}시간 ${m > 0 ? m + "분" : ""} 소요됩니다.`);
      } else {
        setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n약 ${totalMin}분 소요됩니다.`);
      }
    } else {
      const mins = parseInt(availableMin);
      if (!mins || mins <= 0) { setResult("충전 가능 시간을 입력해주세요."); return; }
      const addedPct = (mins / 60) * charger.kw / capacity * 100;
      const reachable = Math.min(Math.round(soc + addedPct), targetMax);
      setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n${mins}분 충전 시 약 ${reachable}%까지 충전 가능합니다.`);
    }
  };

  return (
    <SafeAreaView style={s.safe} edges={[]}>
      <View style={s.header}>
        <Ionicons name="car-outline" size={20} color="#444" />
        <TouchableOpacity style={s.carBtn} onPress={() => { setDropCar(v => !v); setDropCharger(false); }}>
          <Text style={s.carText}>{car}</Text>
          <Ionicons name={dropCar ? "chevron-up-outline" : "chevron-down-outline"} size={16} color="#444" style={{ marginLeft: 2 }} />
        </TouchableOpacity>
      </View>

      {dropCar && (
        <>
          <TouchableOpacity style={StyleSheet.absoluteFill} activeOpacity={1} onPress={() => setDropCar(false)} />
          <View style={s.dropdown}>
            {CARS.map((c) => (
              <TouchableOpacity key={c} style={[s.dropItem, c === car && s.dropActive]}
                onPress={() => { setCar(c); setDropCar(false); }}>
                <Text style={[s.dropText, c === car && s.dropTextOn]}>{c}</Text>
                {c === car && <Ionicons name="checkmark" size={16} color={ACCENT} />}
              </TouchableOpacity>
            ))}
          </View>
        </>
      )}

      <ScrollView contentContainerStyle={s.content} keyboardShouldPersistTaps="handled">
        <View style={s.card}>
          <View style={s.row}>
            <Text style={s.cardLabel}>현재 배터리 잔량 (SOC)</Text>
            <Text style={s.rangeHint}>10% ~ 100%</Text>
          </View>
          <Text style={s.bigVal}>{soc}%</Text>
          <Slider min={10} max={100} step={1} value={soc} onChange={(v) => { setSoc(v); setResult(null); }} />
        </View>

        <View style={s.card}>
          <Text style={s.cardLabel}>충전기 종류</Text>
          <TouchableOpacity style={s.selector} onPress={() => { setDropCharger(v => !v); setDropCar(false); }}>
            <Text style={s.selectorText}>{charger.label}</Text>
            <Ionicons name={dropCharger ? "chevron-up-outline" : "chevron-down-outline"} size={16} color="#888" />
          </TouchableOpacity>
          {dropCharger && (
            <View style={s.selectorDrop}>
              {CHARGERS.map((c) => (
                <TouchableOpacity key={c.label} style={[s.selectorItem, c.label === charger.label && s.dropActive]}
                  onPress={() => { setCharger(c); setDropCharger(false); setTargetSoc(60); setResult(null); }}>
                  <Text style={[s.dropText, c.label === charger.label && s.dropTextOn]}>{c.label}</Text>
                  {c.label === charger.label && <Ionicons name="checkmark" size={16} color={ACCENT} />}
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>

        <View style={s.card}>
          <View style={s.radioRow}>
            {(["target", "time"] as const).map((m) => (
              <TouchableOpacity key={m} style={s.radioItem} onPress={() => { setMode(m); setResult(null); }}>
                <View style={[s.radio, mode === m && s.radioOn]}>
                  {mode === m && <View style={s.radioDot} />}
                </View>
                <Text style={s.radioLabel}>
                  {m === "target" ? "목표 충전량으로 계산" : "충전 가능 시간으로 계산"}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {mode === "target" && (
            <View style={{ marginTop: 16 }}>
              <View style={s.row}>
                <Text style={s.cardLabel}>목표 배터리 잔량</Text>
                <Text style={s.rangeHint}>10% ~ {targetMax}%</Text>
              </View>
              <Text style={s.bigVal}>{targetSoc}%</Text>
              <Slider min={10} max={targetMax} step={1} value={targetSoc}
                onChange={(v) => { setTargetSoc(v); setResult(null); }} />
              {isFast && <Text style={s.hint}>급속 충전은 배터리 보호를 위해 80%까지 권장해요.</Text>}
            </View>
          )}

          {mode === "time" && (
            <View style={{ marginTop: 16 }}>
              <Text style={s.cardLabel}>충전 가능 시간 (분)</Text>
              <TextInput
                style={s.timeInput}
                value={availableMin}
                onChangeText={(v) => { setAvailableMin(v); setResult(null); }}
                keyboardType="number-pad"
                placeholder="예: 30"
                placeholderTextColor="#bbb"
              />
            </View>
          )}
        </View>

        <TouchableOpacity style={s.calcBtn} onPress={calculate}>
          <Text style={s.calcBtnText}>계산하기</Text>
        </TouchableOpacity>

        {result && (
          <View style={s.resultCard}>
            <Text style={s.resultText}>{result}</Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const sl = StyleSheet.create({
  container: { height: 40, justifyContent: "center", marginTop: 8 },
  track: { height: 4, backgroundColor: "#e0e0e0", borderRadius: 2, overflow: "hidden" },
  fill: { height: 4, backgroundColor: ACCENT },
  thumb: {
    position: "absolute", width: 22, height: 22, borderRadius: 11,
    backgroundColor: "#fff", borderWidth: 2.5, borderColor: ACCENT,
    shadowColor: "#000", shadowOpacity: 0.15, shadowRadius: 4, elevation: 4,
  },
});

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: "#fff" },
  header: { flexDirection: "row", alignItems: "center", paddingHorizontal: 16, paddingTop: 14, paddingBottom: 14, borderBottomWidth: 1, borderBottomColor: "#eee", gap: 8 },
  carBtn: { flexDirection: "row", alignItems: "center" },
  carText: { fontSize: 16, fontWeight: "600", color: "#222" },
  dropdown: { position: "absolute", top: 70, left: 16, right: 16, zIndex: 100, backgroundColor: "#fff", borderRadius: 14, shadowColor: "#000", shadowOpacity: 0.12, shadowRadius: 10, elevation: 10, overflow: "hidden" },
  dropItem: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: 18, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: "#f3f3f3" },
  dropActive: { backgroundColor: "#EBF3FF" },
  dropText: { fontSize: 15, color: "#333" },
  dropTextOn: { color: ACCENT, fontWeight: "600" },
  content: { padding: 16, gap: 12, paddingBottom: 40 },
  card: { backgroundColor: "#f8f9ff", borderRadius: 14, padding: 16 },
  row: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  cardLabel: { fontSize: 14, fontWeight: "600", color: "#333" },
  rangeHint: { fontSize: 12, color: "#aaa" },
  bigVal: { fontSize: 22, fontWeight: "700", color: ACCENT, marginTop: 4 },
  selector: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", borderWidth: 1, borderColor: "#e0e0e0", borderRadius: 10, paddingHorizontal: 14, paddingVertical: 13, marginTop: 10, backgroundColor: "#fff" },
  selectorText: { fontSize: 14, color: "#333" },
  selectorDrop: { marginTop: 8, borderWidth: 1, borderColor: "#e8e8e8", borderRadius: 10, overflow: "hidden", backgroundColor: "#fff" },
  selectorItem: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingHorizontal: 14, paddingVertical: 13, borderBottomWidth: 1, borderBottomColor: "#f3f3f3" },
  radioRow: { flexDirection: "row", gap: 16 },
  radioItem: { flexDirection: "row", alignItems: "center", gap: 8, flex: 1 },
  radio: { width: 20, height: 20, borderRadius: 10, borderWidth: 2, borderColor: "#ccc", alignItems: "center", justifyContent: "center" },
  radioOn: { borderColor: ACCENT },
  radioDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: ACCENT },
  radioLabel: { fontSize: 13, color: "#333", flex: 1 },
  hint: { fontSize: 12, color: "#f57c00", marginTop: 8 },
  timeInput: { marginTop: 10, borderWidth: 1, borderColor: "#e0e0e0", borderRadius: 10, paddingHorizontal: 14, paddingVertical: 13, fontSize: 16, backgroundColor: "#fff", color: "#222" },
  calcBtn: { backgroundColor: ACCENT, borderRadius: 14, height: 52, alignItems: "center", justifyContent: "center" },
  calcBtnText: { color: "#fff", fontSize: 16, fontWeight: "700" },
  resultCard: { backgroundColor: "#FFF8E7", borderRadius: 14, padding: 16 },
  resultText: { fontSize: 14, color: "#555", lineHeight: 22 },
});