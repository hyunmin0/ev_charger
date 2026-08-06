import React, { useRef, useState } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  TextInput, Animated, Dimensions, Modal, Switch,
  PanResponder, FlatList,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView } from "react-native-webview";
import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter } from "expo-router";

const KAKAO_API_KEY = "c8ed16f7d0f7208cec6b025168773f5e";
const { height: SCREEN_HEIGHT, width: SCREEN_WIDTH } = Dimensions.get("window");
const SHEET_HEIGHT = SCREEN_HEIGHT * 0.78;
const SHEET_PAD = 20;
const SLIDER_MARGIN = 16;
const TRACK_WIDTH = SCREEN_WIDTH - SHEET_PAD * 2 - SLIDER_MARGIN * 2;
const DOT = 14;
const ACCENT = "#5B9CF6";
const ACCENT_BG = "#EBF3FF";
const LIST_PEEK = 72;
const LIST_MAX = SCREEN_HEIGHT * 0.65;

const FILTER_CHIPS = [
  { id: "radius" }, { id: "available" }, { id: "parking" },
  { id: "open" }, { id: "speed" }, { id: "type" },
  { id: "facility" }, { id: "floor" },
];
const RADIUS_STEPS = ["1km", "3km", "5km", "10km", "10km+"];
const SPEED_STEPS = ["3kW", "7kW", "50kW", "100kW", "200kW", "400kW"];
const CHARGER_TYPES = ["DC 차데모", "DC 콤보", "DC 콤보 (완속)", "DC 콤보2(버스전용)", "AC3 상", "AC 완속", "NACS"];
const FACILITIES = ["공공시설", "주차시설", "휴게시설", "관광시설", "상업시설", "차량정비시설", "기타시설", "공동주택시설", "근린생활시설", "교육문화시설"];
const FLOOR_TYPES = ["지상", "지하"];

const MOCK_STATIONS = [
  { id: "1", name: "한국도로교통공단 광주전남지부", operator: "교육공공시설", operatorType: "급속", distance: "4.5 km", tags: ["표", "일반가", "급속", "무료주차"], rating: 4.5, available: 1, total: 2, prediction: "보통", predictionTime: "1시간 뒤" },
  { id: "2", name: "광주공업단지 북구 산남대학교공과대학", operator: "교육공공시설", operatorType: "급속", distance: "4.5 km", tags: ["표", "일반가", "급속", "무료주차"], rating: 4.5, available: 3, total: 3, prediction: "혼잡", predictionTime: "1시간 뒤" },
  { id: "3", name: "북구동 민원안내주차장", operator: "공공시설", operatorType: "완속", distance: "5.1 km", tags: ["표", "무료주차", "개방"], rating: 4.0, available: 0, total: 3, prediction: "원활", predictionTime: "1시간 뒤" },
  { id: "4", name: "재일풍경채센트럴파크1(입주자대표회의)", operator: "공동주택시설", operatorType: "급속", distance: "5.8 km", tags: ["표", "급속"], rating: 3.8, available: 2, total: 4, prediction: "원활", predictionTime: "1시간 뒤" },
  { id: "5", name: "신논현역 공영주차장", operator: "공공시설", operatorType: "완속", distance: "6.2 km", tags: ["표", "무료주차", "개방"], rating: 4.2, available: 2, total: 3, prediction: "보통", predictionTime: "1시간 뒤" },
];

const predColor = (p: string) => p === "원활" ? "#4CAF50" : p === "보통" ? "#FF9800" : "#F44336";

const mapHTML = `<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>* { margin:0; padding:0; } html,body,#map { width:100%; height:100%; }</style>
</head><body><div id="map"></div>
<script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=c8ed16f7d0f7208cec6b025168773f5e&autoload=false"></script>
<script>kakao.maps.load(function() {
  var map = new kakao.maps.Map(document.getElementById('map'), {
    center: new kakao.maps.LatLng(37.5665, 126.9780), level: 5
  });
  kakao.maps.event.addListener(map, 'center_changed', function() {
    var center = map.getCenter();
    window.ReactNativeWebView.postMessage(JSON.stringify({ lat: center.getLat(), lng: center.getLng() }));
  });
});</script></body></html>`;

function StepSlider({ steps, value, onChange }: { steps: string[]; value: number; onChange: (i: number) => void }) {
  const sw = TRACK_WIDTH / (steps.length - 1);
  return (
    <View style={ss.wrap}>
      <View style={ss.trackBg} />
      <View style={[ss.trackFill, { width: value * sw }]} />
      {steps.map((s, i) => (
        <TouchableOpacity key={s} style={[ss.dotWrap, { left: i * sw - 18 }]}
          onPress={() => onChange(i)} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <View style={[ss.dot, i <= value && ss.dotOn]} />
          <Text style={[ss.lbl, i <= value && ss.lblOn]} numberOfLines={1}>{s}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

function RangeSlider({ steps, minIdx, maxIdx, onMin, onMax }: {
  steps: string[]; minIdx: number; maxIdx: number;
  onMin: (i: number) => void; onMax: (i: number) => void;
}) {
  const sw = TRACK_WIDTH / (steps.length - 1);
  return (
    <View style={ss.wrap}>
      <View style={ss.trackBg} />
      <View style={[ss.trackFill, { left: minIdx * sw, width: (maxIdx - minIdx) * sw }]} />
      {steps.map((s, i) => {
        const inRange = i >= minIdx && i <= maxIdx;
        return (
          <TouchableOpacity key={s} style={[ss.dotWrap, { left: i * sw - 18 }]}
            onPress={() => {
              const dMin = Math.abs(i - minIdx), dMax = Math.abs(i - maxIdx);
              if (dMin < dMax) { if (i <= maxIdx) onMin(i); }
              else { if (i >= minIdx) onMax(i); }
            }} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
            <View style={[ss.dot, inRange && ss.dotOn]} />
            <Text style={[ss.lbl, inRange && ss.lblOn]} numberOfLines={1}>{s}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

export default function HomeScreen() {
  const router = useRouter();

  const [sheetVisible, setSheetVisible] = useState(false);
  const [popupChip, setPopupChip] = useState<string | null>(null);
  const slideAnim = useRef(new Animated.Value(SHEET_HEIGHT)).current;
  const [available, setAvailable] = useState(false);
  const [freeParking, setFreeParking] = useState(false);
  const [openOnly, setOpenOnly] = useState(false);
  const [radiusIdx, setRadiusIdx] = useState(1);
  const [speedMin, setSpeedMin] = useState(1);
  const [speedMax, setSpeedMax] = useState(3);
  const [selTypes, setSelTypes] = useState<string[]>([]);
  const [selFacilities, setSelFacilities] = useState<string[]>([]);
  const [selFloor, setSelFloor] = useState<string[]>([]);

  const listSheetY = useRef(new Animated.Value(LIST_MAX - LIST_PEEK)).current;
  const dragStart = useRef(0);
  const listPan = useRef(PanResponder.create({
    onStartShouldSetPanResponder: () => false,
    onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dy) > 6,
    onPanResponderGrant: () => { dragStart.current = (listSheetY as any)._value; },
    onPanResponderMove: (_, g) => {
      const next = Math.max(0, Math.min(LIST_MAX - LIST_PEEK, dragStart.current + g.dy));
      listSheetY.setValue(next);
    },
    onPanResponderRelease: () => {
      const cur = (listSheetY as any)._value;
      const snap = cur > (LIST_MAX - LIST_PEEK) / 2 ? LIST_MAX - LIST_PEEK : 0;
      Animated.spring(listSheetY, { toValue: snap, useNativeDriver: true, bounciness: 0 }).start();
    },
  })).current;

  const openFullSheet = () => {
    setPopupChip(null);
    setSheetVisible(true);
    Animated.spring(slideAnim, { toValue: 0, useNativeDriver: true, bounciness: 0 }).start();
  };

  const closeFullSheet = () => {
    Animated.timing(slideAnim, { toValue: SHEET_HEIGHT, duration: 250, useNativeDriver: true })
      .start(() => setSheetVisible(false));
  };

  const handleChipPress = (id: string) => {
    if (id === "available") { setAvailable(v => !v); return; }
    if (id === "parking") { setFreeParking(v => !v); return; }
    if (id === "open") { setOpenOnly(v => !v); return; }
    const next = popupChip === id ? null : id;
    setPopupChip(next);
    if (next) {
      Animated.spring(listSheetY, { toValue: 0, useNativeDriver: true, bounciness: 0 }).start();
    }
  };

  const toggle = (arr: string[], v: string, set: (a: string[]) => void) =>
    set(arr.includes(v) ? arr.filter(x => x !== v) : [...arr, v]);

  const chipLabel = (id: string) => {
    const open = popupChip === id;
    if (id === "radius") return `반경 ${RADIUS_STEPS[radiusIdx]}`;
    if (id === "speed") return `${SPEED_STEPS[speedMin]}~${SPEED_STEPS[speedMax]}`;
    if (id === "available") return "충전 가능";
    if (id === "parking") return "무료 주차장";
    if (id === "open") return "개방";
    const arrow = open ? " ↓" : " ↑";
    if (id === "type") return (selTypes.length ? `타입 ${selTypes.length}` : "타입") + arrow;
    if (id === "facility") return (selFacilities.length ? `시설 ${selFacilities.length}` : "시설") + arrow;
    if (id === "floor") return "지상/지하" + arrow;
    return id;
  };

  const isActive = (id: string): boolean =>
    ({ available, parking: freeParking, open: openOnly,
       type: selTypes.length > 0, facility: selFacilities.length > 0,
       floor: selFloor.length > 0 }[id] ?? false);

  const renderStation = ({ item }: { item: typeof MOCK_STATIONS[0] }) => (
    <TouchableOpacity style={S.card} onPress={() => router.push(`/station/${item.id}` as any)}>
      <View style={S.cardRow}>
        <Text style={S.cardName} numberOfLines={2}>{item.name}</Text>
        <Text style={S.cardDist}>{item.distance}</Text>
      </View>
      <View style={S.cardRow2}>
        <View style={S.stTagRow}>
          {item.tags.map((t) => (
            <View key={t} style={S.stTag}><Text style={S.stTagTxt}>{t}</Text></View>
          ))}
        </View>
        <View style={S.ratingRow}>
          <Ionicons name="star" size={11} color="#FFB800" />
          <Text style={S.ratingTxt}>{item.rating.toFixed(1)}</Text>
        </View>
      </View>
      <Text style={S.cardOp}>{item.operator} · {item.operatorType}</Text>
      <View style={S.cardBottom}>
        <View style={S.availRow}>
          <Text style={S.availLbl}>충전가능 </Text>
          <Text style={[S.availNum, { color: item.available > 0 ? ACCENT : "#aaa" }]}>{item.available}</Text>
          <Text style={S.availTotal}>/{item.total}</Text>
        </View>
        <View style={[S.predBadge, { backgroundColor: predColor(item.prediction) + "22" }]}>
          <Text style={[S.predTxt, { color: predColor(item.prediction) }]}>
            {item.predictionTime} {item.prediction} 예상
          </Text>
        </View>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={S.container}>
      <WebView
        source={{ html: mapHTML, baseUrl: "http://localhost" }}
        style={StyleSheet.absoluteFill}
        originWhitelist={["*"]}
        javaScriptEnabled
        domStorageEnabled
        onMessage={(e) => {
          const { lat, lng } = JSON.parse(e.nativeEvent.data);
          AsyncStorage.setItem("mapLocation", JSON.stringify({ lat, lng }));
        }}
      />

      <SafeAreaView edges={["top"]} style={S.topOverlay} pointerEvents="box-none">
        <View style={S.searchRow}>
          <View style={S.searchBar}>
            <TextInput placeholder="충전소 검색" style={S.searchInput} placeholderTextColor="#999" />
            <Ionicons name="search-outline" size={20} color="#999" />
          </View>
        </View>
      </SafeAreaView>

      {/* 충전소 목록 시트 — handle + filter chip bar 포함 */}
      <Animated.View style={[S.listSheet, { transform: [{ translateY: listSheetY }] }]}>
        {/* 드래그 가능한 헤더 */}
        <View {...listPan.panHandlers} style={S.listHeader}>
          <View style={S.listHandleBar}>
            <View style={S.listHandle} />
          </View>
          <View style={S.chipBarRow}>
            <TouchableOpacity style={S.filterIcon} onPress={openFullSheet}>
              <Ionicons name="options-outline" size={20} color="#555" />
            </TouchableOpacity>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={S.chips}>
              {FILTER_CHIPS.map(({ id }) => {
                const active = isActive(id) || popupChip === id;
                return (
                  <TouchableOpacity key={id} style={[S.chip, active && S.chipOn]} onPress={() => handleChipPress(id)}>
                    <Text style={[S.chipTxt, active && S.chipTxtOn]}>{chipLabel(id)}</Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
          </View>
        </View>

        {/* 칩 누르면 sheet 안에 인라인 팝업 */}
        {popupChip && (
          <View style={S.inlinePopup}>
            {popupChip === "radius" && (
              <>
                <View style={S.secRow}>
                  <Text style={S.secTitle}>내 위치에서 반경</Text>
                  <Text style={S.secVal}>~{RADIUS_STEPS[radiusIdx]}</Text>
                </View>
                <StepSlider steps={RADIUS_STEPS} value={radiusIdx} onChange={setRadiusIdx} />
              </>
            )}
            {popupChip === "speed" && (
              <>
                <View style={S.secRow}>
                  <Text style={S.secTitle}>충전 속도</Text>
                  <TouchableOpacity onPress={() => { setSpeedMin(1); setSpeedMax(3); }}>
                    <Text style={S.secVal}>기본값</Text>
                  </TouchableOpacity>
                </View>
                <RangeSlider steps={SPEED_STEPS} minIdx={speedMin} maxIdx={speedMax} onMin={setSpeedMin} onMax={setSpeedMax} />
              </>
            )}
            {(popupChip === "type" || popupChip === "facility" || popupChip === "floor") && (
              <View style={S.tagWrap}>
                {(popupChip === "type" ? CHARGER_TYPES : popupChip === "facility" ? FACILITIES : FLOOR_TYPES).map(item => {
                  const sel = popupChip === "type" ? selTypes : popupChip === "facility" ? selFacilities : selFloor;
                  const set = popupChip === "type" ? setSelTypes : popupChip === "facility" ? setSelFacilities : setSelFloor;
                  const on = sel.includes(item);
                  return (
                    <TouchableOpacity key={item} style={[S.tag, on && S.tagOn]} onPress={() => toggle(sel, item, set)}>
                      <Text style={[S.tagTxt, on && S.tagTxtOn]}>{item}</Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}
          </View>
        )}

        <FlatList
          data={MOCK_STATIONS}
          keyExtractor={(item) => item.id}
          renderItem={renderStation}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: 20 }}
        />
      </Animated.View>

      {/* 전체 필터 바텀시트 (≡ 눌렀을 때) */}
      <Modal visible={sheetVisible} transparent animationType="none" onRequestClose={closeFullSheet}>
        <TouchableOpacity style={S.backdrop} activeOpacity={1} onPress={closeFullSheet} />
        <Animated.View style={[S.sheet, { transform: [{ translateY: slideAnim }] }]}>
          <View style={S.handle} />
          <Text style={S.sheetTitle}>필터 설정</Text>
          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 20 }}>
            <View style={S.sec}>
              <View style={S.secRow}>
                <Text style={S.secTitle}>내 위치에서 반경</Text>
                <Text style={S.secVal}>~{RADIUS_STEPS[radiusIdx]}</Text>
              </View>
              <StepSlider steps={RADIUS_STEPS} value={radiusIdx} onChange={setRadiusIdx} />
            </View>
            <View style={S.sec}>
              {([
                ["사용 가능한 충전소", available, setAvailable],
                ["무료 주차장", freeParking, setFreeParking],
                ["개방", openOnly, setOpenOnly],
              ] as [string, boolean, (v: boolean) => void][]).map(([label, val, set]) => (
                <View key={label} style={S.toggleRow}>
                  <Text style={S.toggleLbl}>{label}</Text>
                  <Switch value={val} onValueChange={set} trackColor={{ true: ACCENT }} />
                </View>
              ))}
            </View>
            <View style={S.sec}>
              <View style={S.secRow}>
                <Text style={S.secTitle}>충전 속도</Text>
                <Text style={S.secVal}>{SPEED_STEPS[speedMin]} ~ {SPEED_STEPS[speedMax]}</Text>
              </View>
              <RangeSlider steps={SPEED_STEPS} minIdx={speedMin} maxIdx={speedMax} onMin={setSpeedMin} onMax={setSpeedMax} />
            </View>
            {[
              { title: "타입", items: CHARGER_TYPES, sel: selTypes, set: setSelTypes },
              { title: "시설", items: FACILITIES, sel: selFacilities, set: setSelFacilities },
              { title: "지상 / 지하", items: FLOOR_TYPES, sel: selFloor, set: setSelFloor },
            ].map(({ title, items, sel, set }) => (
              <View key={title} style={S.sec}>
                <Text style={S.secTitle}>{title}</Text>
                <View style={S.tagWrap}>
                  {items.map(item => {
                    const on = sel.includes(item);
                    return (
                      <TouchableOpacity key={item} style={[S.tag, on && S.tagOn]} onPress={() => toggle(sel, item, set)}>
                        <Text style={[S.tagTxt, on && S.tagTxtOn]}>{item}</Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>
              </View>
            ))}
          </ScrollView>
          <TouchableOpacity style={S.applyBtn} onPress={closeFullSheet}>
            <Text style={S.applyTxt}>적용하기</Text>
          </TouchableOpacity>
        </Animated.View>
      </Modal>
    </View>
  );
}

const ss = StyleSheet.create({
  wrap: { height: 64, marginTop: 12, marginHorizontal: SLIDER_MARGIN, position: "relative" },
  trackBg: { position: "absolute", left: 0, right: 0, top: 14, height: 3, backgroundColor: "#e0e0e0", borderRadius: 2 },
  trackFill: { position: "absolute", left: 0, top: 14, height: 3, backgroundColor: ACCENT, borderRadius: 2 },
  dotWrap: { position: "absolute", top: 0, alignItems: "center", width: 36 },
  dot: { width: DOT, height: DOT, borderRadius: DOT / 2, backgroundColor: "#d0d0d0", borderWidth: 2, borderColor: "#fff", marginTop: 7 },
  dotOn: { backgroundColor: ACCENT },
  lbl: { fontSize: 10, color: "#bbb", marginTop: 4, textAlign: "center" },
  lblOn: { color: ACCENT, fontWeight: "600" },
});

const S = StyleSheet.create({
  container: { flex: 1 },
  topOverlay: { position: "absolute", top: 0, left: 0, right: 0, zIndex: 10 },
  searchRow: { paddingHorizontal: 16, paddingTop: 4, paddingBottom: 4 },
  searchBar: { flexDirection: "row", alignItems: "center", backgroundColor: "#fff", borderRadius: 12, paddingHorizontal: 14, height: 44, shadowColor: "#000", shadowOpacity: 0.1, shadowRadius: 6, elevation: 4 },
  searchInput: { flex: 1, fontSize: 15, color: "#222" },

  listSheet: {
    position: "absolute", bottom: 0, left: 0, right: 0, height: LIST_MAX,
    backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20,
    shadowColor: "#000", shadowOpacity: 0.15, shadowRadius: 10, elevation: 12,
  },
  listHeader: { borderBottomWidth: 1, borderBottomColor: "#f0f0f0" },
  listHandleBar: { alignItems: "center", paddingVertical: 10 },
  listHandle: { width: 36, height: 4, borderRadius: 2, backgroundColor: "#ddd" },
  chipBarRow: { flexDirection: "row", alignItems: "center", paddingBottom: 10 },
  filterIcon: { paddingHorizontal: 12 },
  chips: { paddingRight: 16, gap: 8, flexDirection: "row", alignItems: "center" },
  chip: { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 20, backgroundColor: "#f2f2f2", borderWidth: 1.5, borderColor: "#e0e0e0" },
  chipOn: { backgroundColor: ACCENT_BG, borderColor: ACCENT },
  chipTxt: { fontSize: 12, color: "#555", fontWeight: "500" },
  chipTxtOn: { color: ACCENT, fontWeight: "600" },

  inlinePopup: { paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 1, borderBottomColor: "#f0f0f0", backgroundColor: "#fafafa" },

  card: { paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  cardRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 6 },
  cardName: { fontSize: 15, fontWeight: "700", color: "#111", flex: 1, marginRight: 8, lineHeight: 21 },
  cardDist: { fontSize: 12, color: "#888", marginTop: 2 },
  cardRow2: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 4 },
  stTagRow: { flexDirection: "row", gap: 4, flexWrap: "wrap", flex: 1 },
  stTag: { paddingHorizontal: 7, paddingVertical: 2, backgroundColor: ACCENT_BG, borderRadius: 4 },
  stTagTxt: { fontSize: 11, color: ACCENT, fontWeight: "500" },
  ratingRow: { flexDirection: "row", alignItems: "center", gap: 2 },
  ratingTxt: { fontSize: 12, color: "#555", fontWeight: "600" },
  cardOp: { fontSize: 12, color: "#999", marginBottom: 8 },
  cardBottom: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  availRow: { flexDirection: "row", alignItems: "baseline" },
  availLbl: { fontSize: 13, color: "#555" },
  availNum: { fontSize: 14, fontWeight: "700" },
  availTotal: { fontSize: 13, color: "#aaa" },
  predBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 10 },
  predTxt: { fontSize: 12, fontWeight: "600" },

  backdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(0,0,0,0.3)" },
  sheet: { position: "absolute", bottom: 0, left: 0, right: 0, height: SHEET_HEIGHT, backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingHorizontal: SHEET_PAD },
  handle: { width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd", alignSelf: "center", marginTop: 12 },
  sheetTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", paddingVertical: 14 },
  sec: { marginBottom: 24 },
  secRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 4 },
  secTitle: { fontSize: 15, fontWeight: "600", color: "#222" },
  secVal: { fontSize: 13, color: ACCENT, fontWeight: "600" },
  toggleRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: "#f0f0f0" },
  toggleLbl: { fontSize: 15, color: "#333" },
  tagWrap: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginTop: 12 },
  tag: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20, backgroundColor: "#f5f5f5", borderWidth: 1, borderColor: "#e0e0e0" },
  tagOn: { backgroundColor: ACCENT_BG, borderColor: ACCENT },
  tagTxt: { fontSize: 13, color: "#333" },
  tagTxtOn: { color: ACCENT, fontWeight: "600" },
  applyBtn: { backgroundColor: ACCENT, borderRadius: 12, height: 50, alignItems: "center", justifyContent: "center", marginTop: 8, marginBottom: 20 },
  applyTxt: { color: "#fff", fontSize: 16, fontWeight: "600" },
});