import React, { useRef, useState, useEffect } from "react";
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView,
  TextInput, PanResponder,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import api from "@/lib/api";

const ACCENT = "#5B9CF6";

// 비로그인 기본 차종 목록
const BASE_CARS = [
  { label: "선택 안함", capacity: 64 },
  { label: "현대 아이오닉 5 (72.6kWh)", capacity: 72.6 },
  { label: "현대 아이오닉 6 (77.4kWh)", capacity: 77.4 },
  { label: "기아 EV6 (77.4kWh)", capacity: 77.4 },
  { label: "기아 EV9 (99.8kWh)", capacity: 99.8 },
  { label: "테슬라 모델 3 (75kWh)", capacity: 75 },
  { label: "테슬라 모델 Y (75kWh)", capacity: 75 },
];

// 기본 충전기 목록 (내 차 데이터 없을 때 사용)
const CHARGERS = [
  { label: "완속 AC (3kW)", kw: 3, type: "완속" },
  { label: "완속 AC (7kW)", kw: 7, type: "완속" },
  { label: "급속 DC (50kW)", kw: 50, type: "급속" },
  { label: "급속 DC (100kW)", kw: 100, type: "급속" },
  { label: "급속 DC (350kW)", kw: 350, type: "급속" },
];

// 충전기 타입별 기준 SOC 구간 (%)
const REF_RANGE: Record<string, { fromSoc: number; toSoc: number }> = {
  "완속":   { fromSoc: 10, toSoc: 100 }, // 기준% = 90
  "급속":   { fromSoc: 10, toSoc: 80  }, // 기준% = 70
  "휴대용": { fromSoc: 10, toSoc: 100 }, // 기준% = 90
};

type CarOption = { label: string; capacity: number; isMine?: boolean; carId?: number };
type RefCharger = { chargerType: string; chargerOutput: number | null; minutes: number };

function refChargerLabel(c: RefCharger): string {
  if (c.chargerType === "급속" && c.chargerOutput != null) return `급속 DC (${c.chargerOutput}kW)`;
  if (c.chargerType === "완속") return "완속 충전";
  if (c.chargerType === "휴대용") return "휴대용 충전";
  return c.chargerType;
}

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
  const [carOptions, setCarOptions] = useState<CarOption[]>(BASE_CARS);
  const [car, setCar] = useState("선택 안함");
  const [dropCar, setDropCar] = useState(false);
  const [dropCharger, setDropCharger] = useState(false);

  // 내 차 기준 충전 데이터
  const [refChargers, setRefChargers] = useState<RefCharger[]>([]);
  // 선택된 기준 충전기 (내 차 모드)
  const [selectedRef, setSelectedRef] = useState<RefCharger | null>(null);
  // 일반 충전기 (kW 모드)
  const [charger, setCharger] = useState(CHARGERS[0]);

  const [soc, setSoc] = useState(34);
  const [mode, setMode] = useState<"target" | "time">("target");
  const [targetSoc, setTargetSoc] = useState(60);
  const [availableMin, setAvailableMin] = useState("");
  const [result, setResult] = useState<string | null>(null);

  // 로그인된 경우 내 차 목록 상단에 추가
  useEffect(() => {
    api.get<{ userCarId: string; carId: number; carName: string; batteryCapacity: number }[]>("/user/cars")
      .then(res => {
        if (res.data.length > 0) {
          const myCars: CarOption[] = res.data.map(c => ({
            label: `⭐ ${c.carName}`,
            capacity: c.batteryCapacity,
            isMine: true,
            carId: c.carId,
          }));
          setCarOptions([BASE_CARS[0], ...myCars, ...BASE_CARS.slice(1)]);
        }
      })
      .catch(() => {}); // 비로그인이면 무시
  }, []);

  // 내 차 선택 시 기준 충전 데이터 가져오기
  useEffect(() => {
    const selected = carOptions.find(c => c.label === car);
    if (selected?.isMine && selected.carId) {
      api.get<RefCharger[]>(`/cars/${selected.carId}/charges`)
        .then(res => {
          setRefChargers(res.data);
          setSelectedRef(res.data.length > 0 ? res.data[0] : null);
        })
        .catch(() => {
          setRefChargers([]);
          setSelectedRef(null);
        });
    } else {
      setRefChargers([]);
      setSelectedRef(null);
    }
    setResult(null);
  }, [car]);

  // 현재 내 차 기준 데이터 모드 여부
  const isRefMode = refChargers.length > 0;

  // targetMax: 급속은 80%, 완속/휴대용은 100%
  const currentChargerType = isRefMode ? (selectedRef?.chargerType ?? "완속") : charger.type;
  const isFast = currentChargerType === "급속";
  const targetMax = isFast ? 80 : 100;

  const calculate = () => {
    const capacity = carOptions.find(c => c.label === car)?.capacity ?? 64;

    if (isRefMode && selectedRef) {
      // 기준 시간 공식: 소요시간 = (목표% - 현재%) / 기준% × 기준시간
      const ref = REF_RANGE[selectedRef.chargerType] ?? { fromSoc: 10, toSoc: 100 };
      const refRange = ref.toSoc - ref.fromSoc;

      if (mode === "target") {
        if (targetSoc <= soc) { setResult("목표 배터리가 현재 잔량보다 낮아요."); return; }
        const totalMin = Math.round((targetSoc - soc) / refRange * selectedRef.minutes);
        if (totalMin >= 60) {
          const h = Math.floor(totalMin / 60), m = totalMin % 60;
          setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n약 ${h}시간 ${m > 0 ? m + "분" : ""} 소요됩니다.`);
        } else {
          setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n약 ${totalMin}분 소요됩니다.`);
        }
      } else {
        const mins = parseInt(availableMin);
        if (!mins || mins <= 0) { setResult("충전 가능 시간을 입력해주세요."); return; }
        // 기준 충전률(%/분) = 기준% / 기준시간
        const ratePerMin = refRange / selectedRef.minutes;
        const reachable = Math.min(Math.round(soc + ratePerMin * mins), targetMax);
        setResult(`실제 충전 시간은 차량 기종, 배터리 상태, 충전소에 따라 달라질 수 있습니다.\n\n${mins}분 충전 시 약 ${reachable}%까지 충전 가능합니다.`);
      }
    } else {
      // 기존 kW 공식 (BASE_CARS)
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
    }
  };

  return (
    <SafeAreaView style={s.safe} edges={["top"]}>
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
            {carOptions.map((c) => (
              <TouchableOpacity key={c.label} style={[s.dropItem, c.label === car && s.dropActive]}
                onPress={() => { setCar(c.label); setDropCar(false); setResult(null); }}>
                <Text style={[s.dropText, c.label === car && s.dropTextOn, c.isMine && s.dropTextMine]}>
                  {c.label}
                </Text>
                {c.label === car && <Ionicons name="checkmark" size={16} color={ACCENT} />}
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
            <Text style={s.selectorText}>
              {isRefMode
                ? (selectedRef ? refChargerLabel(selectedRef) : "선택")
                : charger.label}
            </Text>
            <Ionicons name={dropCharger ? "chevron-up-outline" : "chevron-down-outline"} size={16} color="#888" />
          </TouchableOpacity>
          {dropCharger && (
            <View style={s.selectorDrop}>
              {isRefMode
                ? refChargers.map((c) => {
                    const lbl = refChargerLabel(c);
                    const isSelected = selectedRef?.chargerType === c.chargerType && selectedRef?.chargerOutput === c.chargerOutput;
                    return (
                      <TouchableOpacity key={lbl} style={[s.selectorItem, isSelected && s.dropActive]}
                        onPress={() => { setSelectedRef(c); setDropCharger(false); setTargetSoc(60); setResult(null); }}>
                        <Text style={[s.dropText, isSelected && s.dropTextOn]}>{lbl}</Text>
                        {isSelected && <Ionicons name="checkmark" size={16} color={ACCENT} />}
                      </TouchableOpacity>
                    );
                  })
                : CHARGERS.map((c) => (
                    <TouchableOpacity key={c.label} style={[s.selectorItem, c.label === charger.label && s.dropActive]}
                      onPress={() => { setCharger(c); setDropCharger(false); setTargetSoc(60); setResult(null); }}>
                      <Text style={[s.dropText, c.label === charger.label && s.dropTextOn]}>{c.label}</Text>
                      {c.label === charger.label && <Ionicons name="checkmark" size={16} color={ACCENT} />}
                    </TouchableOpacity>
                  ))
              }
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
              <Slider min={10} max={targetMax} step={1} value={Math.min(targetSoc, targetMax)}
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
  dropTextMine: { color: "#5B9CF6" },
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