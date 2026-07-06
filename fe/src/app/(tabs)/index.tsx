import React, { useRef, useState } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  TextInput, Animated, Dimensions, Modal, Switch,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView } from "react-native-webview";
import { Ionicons } from "@expo/vector-icons";

const KAKAO_API_KEY = "c8ed16f7d0f7208cec6b025168773f5e";
const { height: SCREEN_HEIGHT, width: SCREEN_WIDTH } = Dimensions.get("window");
const SHEET_HEIGHT = SCREEN_HEIGHT * 0.78;

const FILTER_CHIPS = [
  { id: "radius", label: "반경" },
  { id: "available", label: "충전 가능" },
  { id: "parking", label: "무료 주차장" },
  { id: "open", label: "개방" },
  { id: "speed", label: "충전 속도" },
  { id: "type", label: "타입" },
  { id: "facility", label: "시설" },
  { id: "floor", label: "지상/지하" },
];

const RADIUS_STEPS = ["1km", "3km", "5km", "10km", "10km+"];
const SPEED_STEPS = ["3kW", "7kW", "50kW", "100kW", "200kW", "400kW"];
const CHARGER_TYPES = ["DC 차데모", "DC 콤보", "DC 콤보 (완속)", "DC 콤보2(버스전용)", "AC3 상", "AC 완속", "NACS"];
const FACILITIES = ["공공시설", "주차시설", "휴게시설", "관광시설", "상업시설", "차량정비시설", "기타시설", "공동주택시설", "근린생활시설", "교육문화시설"];
const FLOOR_TYPES = ["지상", "지하"];

const mapHTML = `
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>* { margin:0; padding:0; } html,body,#map { width:100%; height:100%; }</style>
</head><body>
<div id="map"></div>
<script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_API_KEY}&autoload=false"></script>
<script>
kakao.maps.load(function() {
  var map = new kakao.maps.Map(document.getElementById('map'), {
    center: new kakao.maps.LatLng(37.5665, 126.9780), level: 5
  });
});
</script></body></html>`;

// 이산 스텝 슬라이더 컴포넌트
function StepSlider({ steps, value, onChange }: { steps: string[]; value: number; onChange: (i: number) => void }) {
  const trackWidth = SCREEN_WIDTH - 80;
  const stepWidth = trackWidth / (steps.length - 1);
  return (
    <View style={ss.sliderWrap}>
      <View style={ss.track} />
      <View style={[ss.trackFill, { width: value * stepWidth }]} />
      {steps.map((s, i) => (
        <TouchableOpacity key={s} style={[ss.dotWrap, { left: i * stepWidth - 10 }]} onPress={() => onChange(i)}>
          <View style={[ss.dot, i === value && ss.dotActive]} />
          <Text style={[ss.stepLabel, i === value && ss.stepLabelActive]}>{s}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

// 범위 스텝 슬라이더 (두 핸들)
function RangeSlider({ steps, minIdx, maxIdx, onMinChange, onMaxChange }: {
  steps: string[]; minIdx: number; maxIdx: number;
  onMinChange: (i: number) => void; onMaxChange: (i: number) => void;
}) {
  const trackWidth = SCREEN_WIDTH - 80;
  const stepWidth = trackWidth / (steps.length - 1);
  return (
    <View style={ss.sliderWrap}>
      <View style={ss.track} />
      <View style={[ss.trackFill, { left: minIdx * stepWidth, width: (maxIdx - minIdx) * stepWidth }]} />
      {steps.map((s, i) => (
        <TouchableOpacity key={s}
          style={[ss.dotWrap, { left: i * stepWidth - 10 }]}
          onPress={() => {
            const distMin = Math.abs(i - minIdx);
            const distMax = Math.abs(i - maxIdx);
            if (distMin <= distMax) { if (i <= maxIdx) onMinChange(i); }
            else { if (i >= minIdx) onMaxChange(i); }
          }}>
          <View style={[ss.dot, (i === minIdx || i === maxIdx) && ss.dotActive]} />
          <Text style={[ss.stepLabel, (i === minIdx || i === maxIdx) && ss.stepLabelActive]}>{s}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

export default function HomeScreen() {
  const [sheetVisible, setSheetVisible] = useState(false);
  const [activeChip, setActiveChip] = useState<string | null>(null);
  const slideAnim = useRef(new Animated.Value(SHEET_HEIGHT)).current;

  const [available, setAvailable] = useState(false);
  const [freeParking, setFreeParking] = useState(false);
  const [openOnly, setOpenOnly] = useState(false);
  const [radiusIdx, setRadiusIdx] = useState(1); // 기본 3km
  const [speedMinIdx, setSpeedMinIdx] = useState(1); // 기본 7kW
  const [speedMaxIdx, setSpeedMaxIdx] = useState(3); // 기본 100kW
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [selectedFacilities, setSelectedFacilities] = useState<string[]>([]);
  const [selectedFloor, setSelectedFloor] = useState<string[]>([]);

  const openSheet = (chipId: string) => {
    setActiveChip(chipId);
    setSheetVisible(true);
    Animated.spring(slideAnim, { toValue: 0, useNativeDriver: true, bounciness: 0 }).start();
  };

  const closeSheet = () => {
    Animated.timing(slideAnim, { toValue: SHEET_HEIGHT, duration: 250, useNativeDriver: true })
      .start(() => { setSheetVisible(false); setActiveChip(null); });
  };

  const toggleArr = (arr: string[], val: string, setArr: (v: string[]) => void) => {
    setArr(arr.includes(val) ? arr.filter(x => x !== val) : [...arr, val]);
  };

  // 칩 라벨에 현재 값 표시
  const chipLabel = (id: string) => {
    if (id === "radius") return `반경 ${RADIUS_STEPS[radiusIdx]}`;
    if (id === "speed") return `${SPEED_STEPS[speedMinIdx]}~${SPEED_STEPS[speedMaxIdx]}`;
    if (id === "available") return available ? "충전 가능 ✓" : "충전 가능";
    if (id === "parking") return freeParking ? "무료 주차장 ✓" : "무료 주차장";
    if (id === "open") return openOnly ? "개방 ✓" : "개방";
    if (id === "type") return selectedTypes.length ? `타입 ${selectedTypes.length}` : "타입";
    if (id === "facility") return selectedFacilities.length ? `시설 ${selectedFacilities.length}` : "시설";
    if (id === "floor") return selectedFloor.length ? `지상/지하 ✓` : "지상/지하";
    return id;
  };

  return (
    <View style={styles.container}>
      <WebView source={{ html: mapHTML, baseUrl: "http://localhost" }}
        style={StyleSheet.absoluteFill} originWhitelist={["*"]} javaScriptEnabled domStorageEnabled />

      {/* 상단 검색바 */}
      <SafeAreaView edges={["top"]} style={styles.topOverlay} pointerEvents="box-none">
        <View style={styles.searchRow}>
          <View style={styles.searchBar}>
            <TextInput placeholder="충전소 검색" style={styles.searchInput} placeholderTextColor="#999" />
            <Ionicons name="search-outline" size={20} color="#999" />
          </View>
        </View>
      </SafeAreaView>

      {/* 하단 필터 칩바 */}
      <View style={styles.bottomBar}>
        <TouchableOpacity style={styles.filterIcon} onPress={() => openSheet("all")}>
          <Ionicons name="options-outline" size={20} color="#444" />
        </TouchableOpacity>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chipsContainer}>
          {FILTER_CHIPS.map((chip) => (
            <TouchableOpacity key={chip.id}
              style={[styles.chip, activeChip === chip.id && styles.chipActive]}
              onPress={() => openSheet(chip.id)}>
              <Text style={[styles.chipText, activeChip === chip.id && styles.chipTextActive]}>
                {chipLabel(chip.id)}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 바텀시트 */}
      <Modal visible={sheetVisible} transparent animationType="none" onRequestClose={closeSheet}>
        <TouchableOpacity style={styles.backdrop} activeOpacity={1} onPress={closeSheet} />
        <Animated.View style={[styles.sheet, { transform: [{ translateY: slideAnim }] }]}>
          <View style={styles.handle} />
          <Text style={styles.sheetTitle}>필터 설정</Text>

          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 20 }}>
            {/* 반경 슬라이더 */}
            <View style={styles.section}>
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>내 위치에서 반경</Text>
                <Text style={styles.sectionValue}>~{RADIUS_STEPS[radiusIdx]}</Text>
              </View>
              <StepSlider steps={RADIUS_STEPS} value={radiusIdx} onChange={setRadiusIdx} />
            </View>

            {/* 토글 */}
            <View style={styles.section}>
              {[
                { label: "사용 가능한 충전소", val: available, set: setAvailable },
                { label: "무료 주차장", val: freeParking, set: setFreeParking },
                { label: "개방", val: openOnly, set: setOpenOnly },
              ].map(({ label, val, set }) => (
                <View key={label} style={styles.toggleRow}>
                  <Text style={styles.toggleLabel}>{label}</Text>
                  <Switch value={val} onValueChange={set} trackColor={{ true: "#4CAF50" }} />
                </View>
              ))}
            </View>

            {/* 충전 속도 범위 슬라이더 */}
            <View style={styles.section}>
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>충전 속도</Text>
                <Text style={styles.sectionValue}>{SPEED_STEPS[speedMinIdx]} ~ {SPEED_STEPS[speedMaxIdx]}</Text>
              </View>
              <RangeSlider steps={SPEED_STEPS} minIdx={speedMinIdx} maxIdx={speedMaxIdx}
                onMinChange={setSpeedMinIdx} onMaxChange={setSpeedMaxIdx} />
            </View>

            {/* 타입 */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>타입</Text>
              <View style={styles.tagWrap}>
                {CHARGER_TYPES.map((t) => (
                  <TouchableOpacity key={t}
                    style={[styles.tag, selectedTypes.includes(t) && styles.tagActive]}
                    onPress={() => toggleArr(selectedTypes, t, setSelectedTypes)}>
                    <Text style={[styles.tagText, selectedTypes.includes(t) && styles.tagTextActive]}>{t}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            {/* 시설 */}
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>시설</Text>
              <View style={styles.tagWrap}>
                {FACILITIES.map((f) => (
                  <TouchableOpacity key={f}
                    style={[styles.tag, selectedFacilities.includes(f) && styles.tagActive]}
                    onPress={() => toggleArr(selectedFacilities, f, setSelectedFacilities)}>
                    <Text style={[styles.tagText, selectedFacilities.includes(f) && styles.tagTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>

            {/* 지상/지하 */}
            <View style={[styles.section, { marginBottom: 8 }]}>
              <Text style={styles.sectionTitle}>지상 / 지하</Text>
              <View style={styles.tagWrap}>
                {FLOOR_TYPES.map((f) => (
                  <TouchableOpacity key={f}
                    style={[styles.tag, selectedFloor.includes(f) && styles.tagActive]}
                    onPress={() => toggleArr(selectedFloor, f, setSelectedFloor)}>
                    <Text style={[styles.tagText, selectedFloor.includes(f) && styles.tagTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          </ScrollView>

          <TouchableOpacity style={styles.applyBtn} onPress={closeSheet}>
            <Text style={styles.applyText}>적용하기</Text>
          </TouchableOpacity>
        </Animated.View>
      </Modal>
    </View>
  );
}

// 슬라이더 전용 스타일
const ss = StyleSheet.create({
  sliderWrap: { height: 56, marginTop: 8, marginHorizontal: 10, position: "relative", justifyContent: "center" },
  track: { position: "absolute", left: 0, right: 0, height: 3, backgroundColor: "#e0e0e0", borderRadius: 2 },
  trackFill: { position: "absolute", left: 0, height: 3, backgroundColor: "#4CAF50", borderRadius: 2 },
  dotWrap: { position: "absolute", alignItems: "center", width: 20, top: 8 },
  dot: { width: 14, height: 14, borderRadius: 7, backgroundColor: "#e0e0e0", borderWidth: 2, borderColor: "#fff" },
  dotActive: { backgroundColor: "#4CAF50" },
  stepLabel: { fontSize: 10, color: "#aaa", marginTop: 4 },
  stepLabelActive: { color: "#4CAF50", fontWeight: "600" },
});

const styles = StyleSheet.create({
  container: { flex: 1 },
  topOverlay: { position: "absolute", top: 0, left: 0, right: 0, zIndex: 10 },
  searchRow: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 8 },
  searchBar: {
    flexDirection: "row", alignItems: "center", backgroundColor: "#fff",
    borderRadius: 12, paddingHorizontal: 14, height: 46,
    shadowColor: "#000", shadowOpacity: 0.1, shadowRadius: 6, elevation: 4,
  },
  searchInput: { flex: 1, fontSize: 15, color: "#222" },
  bottomBar: {
    position: "absolute", bottom: 0, left: 0, right: 0,
    flexDirection: "row", alignItems: "center",
    backgroundColor: "#fff", paddingVertical: 12, paddingBottom: 28,
    shadowColor: "#000", shadowOpacity: 0.12, shadowRadius: 6, elevation: 8,
  },
  filterIcon: { paddingHorizontal: 12 },
  chipsContainer: { paddingRight: 16, gap: 8, flexDirection: "row", alignItems: "center" },
  chip: {
    paddingHorizontal: 12, paddingVertical: 7, borderRadius: 20,
    backgroundColor: "#f2f2f2", borderWidth: 1, borderColor: "#e0e0e0",
  },
  chipActive: { backgroundColor: "#4CAF50", borderColor: "#4CAF50" },
  chipText: { fontSize: 12, color: "#444", fontWeight: "500" },
  chipTextActive: { color: "#fff" },
  backdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(0,0,0,0.3)" },
  sheet: {
    position: "absolute", bottom: 0, left: 0, right: 0, height: SHEET_HEIGHT,
    backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20,
    paddingHorizontal: 20,
  },
  handle: {
    width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd",
    alignSelf: "center", marginTop: 12,
  },
  sheetTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", paddingVertical: 14 },
  section: { marginBottom: 24 },
  sectionHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 4 },
  sectionTitle: { fontSize: 15, fontWeight: "600", color: "#222" },
  sectionValue: { fontSize: 13, color: "#4CAF50", fontWeight: "600" },
  toggleRow: {
    flexDirection: "row", justifyContent: "space-between", alignItems: "center",
    paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  toggleLabel: { fontSize: 15, color: "#333" },
  tagWrap: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginTop: 12 },
  tag: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
    backgroundColor: "#f2f2f2", borderWidth: 1, borderColor: "#e0e0e0",
  },
  tagActive: { backgroundColor: "#4CAF50", borderColor: "#4CAF50" },
  tagText: { fontSize: 13, color: "#444" },
  tagTextActive: { color: "#fff" },
  applyBtn: {
    backgroundColor: "#4CAF50", borderRadius: 12, height: 50,
    alignItems: "center", justifyContent: "center", marginTop: 8, marginBottom: 20,
  },
  applyText: { color: "#fff", fontSize: 16, fontWeight: "600" },
});