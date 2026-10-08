import React, { useRef, useState, useEffect, useCallback } from "react";
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  TextInput, Animated, Dimensions, Modal, Switch,
  PanResponder, FlatList, ActivityIndicator, Alert, Linking,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView } from "react-native-webview";
import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useRouter, useFocusEffect } from "expo-router";
import * as Location from "expo-location";
import api from "@/lib/api";

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
// 마지막 "전체"는 반경 대신 지도 화면 기준으로 불러옴
const RADIUS_STEPS = ["1km", "3km", "5km", "10km", "전체"];
const ALL_RADIUS_IDX = RADIUS_STEPS.length - 1;
const SPEED_STEPS = ["3kW", "7kW", "50kW", "100kW", "200kW", "400kW"];
const CHARGER_TYPES = ["DC 차데모", "DC 콤보", "DC 콤보 (완속)", "DC 콤보2(버스전용)", "AC3 상", "AC 완속", "NACS"];
const FACILITIES = ["공공시설", "주차시설", "휴게시설", "관광시설", "상업시설", "차량정비시설", "기타시설", "공동주택시설", "근린생활시설", "교육문화시설"];
const FLOOR_TYPES = ["지상", "지하"];

type Region = { code: string; name: string; stationCount: number; availableStationCount: number; lat: number; lng: number };
type MapView = { level: number; minLat: number; maxLat: number; minLng: number; maxLng: number };

type Station = {
  statId: string;
  statNm: string;
  busiNm: string;
  lat: number;
  lng: number;
  hasFast: boolean;
  parkingFree: boolean;
  openToPublic: boolean;
  averageRating: number | null;
  availableCount: number;
  totalCount: number;
  distance: number;
  nextHourCongestionLevel: string | null;
  hasCharging: boolean;
  allUnknown: boolean;
  allUnavailable: boolean;
};

const RADIUS_METERS = [1000, 3000, 5000, 10000];
// 카카오 지도 레벨(1 확대 ~ 14 전국)에 따라 요약 단위를 바꿈
// SIDO_LEVEL 이상: 시·도 / CITY_LEVEL 이상: 도 안의 시·군(광역시는 하나) / 그 아래: 개별 충전소
const SIDO_LEVEL = 11;
const CITY_LEVEL = 8;
const REFRESH_INTERVAL_MS = 5 * 60 * 1000;

// 내 위치 버튼: 이 시간 안에 잡힌 위치면 바로 사용, 정확한 위치가 이만큼 다르면 다시 옮김
const LAST_LOCATION_MAX_AGE_MS = 5 * 60 * 1000;
const RELOCATE_MIN_METERS = 50;
// 이동 중 위치 추적 간격 (이만큼 움직이거나 시간이 지나면 파란 점 갱신)
const WATCH_DISTANCE_METERS = 10;
const WATCH_INTERVAL_MS = 5000;

function distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number) {
  const r = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return 2 * r * Math.asin(Math.sqrt(a));
}

function formatDistance(m: number) {
  return m >= 1000 ? `${(m / 1000).toFixed(1)}km` : `${Math.round(m)}m`;
}

function stationTags(s: Station) {
  const tags: string[] = [];
  if (s.hasFast) tags.push("급속");
  if (s.parkingFree) tags.push("무료주차");
  if (s.openToPublic) tags.push("개방");
  return tags;
}

const predColor = (p: string | null) =>
  p === "여유" ? "#C8E4CB" : p === "보통" ? "#F1E8B1" : p === "혼잡" ? "#F6D2D4" : "#eee";

function makeMapHTML(initLat: number, initLng: number) {
  return `<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
* { margin:0; padding:0; }
html,body,#map { width:100%; height:100%; }
.loc-wrap { position:relative; width:50px; height:50px; display:flex; align-items:center; justify-content:center; }
.loc-pulse { position:absolute; width:60px; height:60px; border-radius:50%; background:rgba(74,144,226,0.2); }
.loc-dot { width:14px; height:14px; border-radius:50%; background:#4A90E2; border:2.5px solid #fff; box-shadow:0 1px 4px rgba(0,0,0,0.25); position:relative; z-index:1; }
.st-pin-wrap { cursor:pointer; display:block; }
.rg { background:#fff; border:2px solid #5B9CF6; border-radius:14px; padding:5px 10px; text-align:center; box-shadow:0 1px 4px rgba(0,0,0,0.2); cursor:pointer; white-space:nowrap; font-family:sans-serif; }
.rg b { display:block; font-size:13px; color:#222; }
.rg span { font-size:11px; color:#555; }
.rg i { font-style:normal; font-size:11px; color:#09AD12; margin-left:4px; }
</style>
</head><body><div id="map"></div>
<script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_API_KEY}&autoload=false"></script>
<script>
function pinSVG(color) {
  // 표시 크기 20x26 (path 좌표는 viewBox 26x34 기준)
  return '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="26" viewBox="0 0 26 34">'
    + '<path d="M13 0C5.82 0 0 5.82 0 13c0 9.75 13 21 13 21s13-11.25 13-21C26 5.82 20.18 0 13 0z" fill="' + color + '" stroke="rgba(255,255,255,0.85)" stroke-width="1.5"/>'
    + '<circle cx="13" cy="12.5" r="5" fill="white"/>'
    + '</svg>';
}
function stationColor(s) {
  if (s.availableCount > 0) return '#09AD12';
  if (s.hasCharging) return '#EDA144';
  if (s.allUnavailable) return '#EB0000';
  if (s.allUnknown) return '#545050';
  return '#545050';
}
document.addEventListener('click', function(e) {
  var el = e.target;
  while (el && !el.getAttribute('data-sid')) el = el.parentElement;
  if (el) {
    window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'st', id: el.getAttribute('data-sid') }));
  }
});
kakao.maps.load(function() {
  window.map = new kakao.maps.Map(document.getElementById('map'), {
    center: new kakao.maps.LatLng(${initLat}, ${initLng}), level: 5
  });
  kakao.maps.event.addListener(window.map, 'center_changed', function() {
    var c = window.map.getCenter();
    window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'center', lat: c.getLat(), lng: c.getLng() }));
  });
  // zoomIn: 내 위치 버튼 -> 많이 줌아웃해 있으면 동네가 보이는 정도로 확대
  window.moveToMyLocation = function(lat, lng, zoomIn) {
    var pos = new kakao.maps.LatLng(lat, lng);
    if (zoomIn && window.map.getLevel() > 5) {
      window.map.setLevel(5);
      window.map.setCenter(pos);
    } else {
      window.map.panTo(pos);
    }
    window.setMyLocationDot(lat, lng);
  };
  // 파란 점만 옮김 (이동 중 위치 추적: 지도 화면은 따라가지 않음)
  window.setMyLocationDot = function(lat, lng) {
    var pos = new kakao.maps.LatLng(lat, lng);
    if (window.myLocationDot) {
      window.myLocationDot.setPosition(pos);
    } else {
      window.myLocationDot = new kakao.maps.CustomOverlay({
        position: pos,
        content: '<div class="loc-wrap"><div class="loc-pulse"></div><div class="loc-dot"></div></div>',
        xAnchor: 0.5, yAnchor: 0.5, zIndex: 10
      });
      window.myLocationDot.setMap(window.map);
    }
  };
  // 지도를 움직이거나 줌한 뒤 멈추면 화면 범위와 줌 레벨을 앱으로 보냄 ("전체" 모드에서 사용)
  window.postView = function() {
    var b = window.map.getBounds(), sw = b.getSouthWest(), ne = b.getNorthEast();
    window.ReactNativeWebView.postMessage(JSON.stringify({
      type: 'view', level: window.map.getLevel(),
      minLat: sw.getLat(), maxLat: ne.getLat(), minLng: sw.getLng(), maxLng: ne.getLng()
    }));
  };
  kakao.maps.event.addListener(window.map, 'idle', window.postView);
  window.regionOverlays = [];
  // zoomTo: 말풍선을 눌렀을 때 확대할 레벨
  window.updateRegions = function(list, zoomTo) {
    window.regionOverlays.forEach(function(o) { o.setMap(null); });
    window.regionOverlays = [];
    list.forEach(function(r) {
      var ov = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(r.lat, r.lng),
        content: '<div class="rg" onclick="window.zoomRegion(' + r.lat + ',' + r.lng + ',' + zoomTo + ')"><b>' + r.name + '</b>'
          + '<span>' + r.stationCount.toLocaleString() + '곳</span><i>가능 ' + r.availableStationCount.toLocaleString() + '</i></div>',
        xAnchor: 0.5, yAnchor: 0.5, zIndex: 6
      });
      ov.setMap(window.map);
      window.regionOverlays.push(ov);
    });
  };
  // 말풍선을 누르면 그 지역으로 한 단계 확대 (시·도 -> 시·군, 시·군 -> 개별 충전소)
  window.zoomRegion = function(lat, lng, level) {
    window.map.setLevel(level, { anchor: new kakao.maps.LatLng(lat, lng) });
    window.map.setCenter(new kakao.maps.LatLng(lat, lng));
  };
  window.stationMarkers = [];
  window.updateStationMarkers = function(list) {
    window.stationMarkers.forEach(function(m) { m.setMap(null); });
    window.stationMarkers = [];
    list.forEach(function(s) {
      var color = stationColor(s);
      var ov = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(s.lat, s.lng),
        content: '<div class="st-pin-wrap" data-sid="' + s.statId + '">' + pinSVG(color) + '</div>',
        xAnchor: 0.5, yAnchor: 1.0, zIndex: 5
      });
      ov.setMap(window.map);
      window.stationMarkers.push(ov);
    });
  };
});
</script></body></html>`;
}

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

  const [stations, setStations] = useState<Station[]>([]);
  const [stationsLoading, setStationsLoading] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [fetchError, setFetchError] = useState(false);
  const userLat = useRef(37.5665);
  const userLng = useRef(126.9780);
  const webviewRef = useRef<any>(null);
  const mapLoaded = useRef(false);
  const [mapReady, setMapReady] = useState(false);
  const [mapCenter, setMapCenter] = useState<{ lat: number; lng: number } | null>(null);
  const pendingLocation = useRef<{ lat: number; lng: number } | null>(null);
  const isInitialMount = useRef(true);

  const moveToUserLocation = useCallback((lat: number, lng: number, zoomIn = false) => {
    webviewRef.current?.injectJavaScript(`window.moveToMyLocation(${lat}, ${lng}, ${zoomIn}); true;`);
  }, []);

  const [sheetVisible, setSheetVisible] = useState(false);
  const [popupChip, setPopupChip] = useState<string | null>(null);
  const slideAnim = useRef(new Animated.Value(SHEET_HEIGHT)).current;
  const [available, setAvailable] = useState(false);
  const [freeParking, setFreeParking] = useState(false);
  const [openOnly, setOpenOnly] = useState(false);
  const [radiusIdx, setRadiusIdx] = useState(1);
  const isAllMode = radiusIdx === ALL_RADIUS_IDX;
  // "전체" 모드: 지도가 마지막으로 멈춘 화면 범위, 시·도 요약을 보여주는 중인지
  const lastView = useRef<MapView | null>(null);
  const [regionMode, setRegionMode] = useState(false);
  const viewTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const viewSeq = useRef(0);

  // background: 주기 새로고침. 실패해도 기존 목록을 그대로 둠
  const fetchStations = useCallback(async (cursor?: string | null, background = false) => {
    setStationsLoading(true);
    if (!cursor && !background) setFetchError(false);
    try {
      const res = await api.get("/stations/nearby", {
        params: {
          lat: userLat.current,
          lng: userLng.current,
          range: RADIUS_METERS[radiusIdx],
          "filter.availableOnly": available,
          ...(cursor ? { cursor } : {}),
        },
      });
      const data = res.data;
      if (cursor) {
        setStations(prev => [...prev, ...(data.stations ?? [])]);
      } else {
        setStations(data.stations ?? []);
      }
      // be는 반경 안 충전소를 한 번에 다 주고도 nextCursor를 항상 채워 보냄
      // -> 빈 페이지가 오면 더 없는 것으로 보고 멈춤 (안 그러면 목록 끝에서 빈 요청이 끝없이 반복됨)
      setNextCursor(data.stations?.length ? data.nextCursor ?? null : null);
    } catch {
      if (!cursor && !background) {
        setFetchError(true);
        setStations([]);
      }
    } finally {
      setStationsLoading(false);
    }
  }, [radiusIdx, available]);

  const showRegions = useCallback((list: Region[], zoomTo = 0) => {
    webviewRef.current?.injectJavaScript(`window.updateRegions && window.updateRegions(${JSON.stringify(list)}, ${zoomTo}); true;`);
  }, []);

  // "전체" 모드: 줌아웃 정도에 따라 시·도 / 시·군 요약, 확대하면 화면 범위 안 충전소 (be가 화면 중심에서 가까운 500곳까지)
  const loadView = useCallback(async (view: MapView, background = false) => {
    const seq = ++viewSeq.current; // 지도를 빨리 움직이면 늦게 온 이전 응답은 버림
    setStationsLoading(true);
    if (!background) setFetchError(false);
    try {
      if (view.level >= CITY_LEVEL) {
        const isSido = view.level >= SIDO_LEVEL;
        const res = await api.get<Region[]>("/stations/regions", { params: { level: isSido ? "sido" : "city" } });
        if (seq !== viewSeq.current) return;
        setRegionMode(true);
        setStations([]);
        showRegions(res.data ?? [], isSido ? SIDO_LEVEL - 2 : CITY_LEVEL - 2);
      } else {
        const res = await api.get<Station[]>("/stations/bounds", {
          params: {
            minLat: view.minLat, maxLat: view.maxLat, minLng: view.minLng, maxLng: view.maxLng,
            userLat: userLat.current, userLng: userLng.current,
            "filter.availableOnly": available,
          },
        });
        if (seq !== viewSeq.current) return;
        setRegionMode(false);
        showRegions([]);
        setStations(res.data ?? []);
      }
      setNextCursor(null);
    } catch {
      if (seq === viewSeq.current && !background) {
        setFetchError(true);
        setStations([]);
      }
    } finally {
      if (seq === viewSeq.current) setStationsLoading(false);
    }
  }, [available, showRegions]);

  useEffect(() => {
    (async () => {
      try {
        const cached = await AsyncStorage.getItem("mapLocation");
        if (cached) {
          const c = JSON.parse(cached);
          userLat.current = c.lat;
          userLng.current = c.lng;
          setMapCenter({ lat: c.lat, lng: c.lng });
        } else {
          setMapCenter({ lat: 37.5665, lng: 126.9780 });
        }
      } catch {
        setMapCenter({ lat: 37.5665, lng: 126.9780 });
      }

      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === "granted") {
        const loc = await Location.getCurrentPositionAsync({});
        userLat.current = loc.coords.latitude;
        userLng.current = loc.coords.longitude;
        if (mapLoaded.current) {
          moveToUserLocation(loc.coords.latitude, loc.coords.longitude);
        } else {
          pendingLocation.current = { lat: loc.coords.latitude, lng: loc.coords.longitude };
        }
      }
      fetchStations();
    })();
  }, []);

  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }
    if (isAllMode) {
      // 지도에 현재 화면 범위를 다시 보내 달라고 함 -> onMessage의 view에서 불러옴
      webviewRef.current?.injectJavaScript(`window.postView && window.postView(); true;`);
    } else {
      viewSeq.current++; // 진행 중인 "전체" 모드 응답 무시
      setRegionMode(false);
      showRegions([]);
      fetchStations();
    }
  }, [radiusIdx, available]);

  // 충전기 상태는 EC2가 5분마다 갱신함 -> 지도 탭을 보고 있는 동안 5분마다 다시 불러옴
  const isAllModeRef = useRef(isAllMode);
  isAllModeRef.current = isAllMode;
  const loadViewRef = useRef(loadView);
  loadViewRef.current = loadView;
  // 내 위치 버튼: 현재 GPS로 지도를 옮기고 내 위치 기준으로 다시 불러옴
  const [locating, setLocating] = useState(false);
  const goToMyLocation = async () => {
    setLocating(true);
    try {
      let perm = await Location.getForegroundPermissionsAsync();
      if (!perm.granted && perm.canAskAgain) perm = await Location.requestForegroundPermissionsAsync();
      if (!perm.granted) {
        Alert.alert("위치 권한이 필요해요", "휴대폰 설정에서 이 앱의 위치 권한을 허용해 주세요.", [
          { text: "취소", style: "cancel" },
          { text: "설정 열기", onPress: () => Linking.openSettings() },
        ]);
        return;
      }
      // 새 GPS 위치를 잡는 데 몇 초 걸려서, 기기가 이미 알고 있는 최근 위치로 먼저 바로 옮김
      const last = await Location.getLastKnownPositionAsync({ maxAge: LAST_LOCATION_MAX_AGE_MS });
      if (last) applyMyLocation(last.coords.latitude, last.coords.longitude);
      else moveToUserLocation(userLat.current, userLng.current, true);
      setLocating(false);

      // 정확한 현재 위치는 뒤에서 받아서, 많이 달라졌을 때만 다시 옮김
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      const { latitude, longitude } = loc.coords;
      if (!last || distanceMeters(last.coords.latitude, last.coords.longitude, latitude, longitude) > RELOCATE_MIN_METERS) {
        applyMyLocation(latitude, longitude);
      }
    } catch {
      Alert.alert("오류", "현재 위치를 가져오지 못했어요.");
    } finally {
      setLocating(false);
    }
  };

  const applyMyLocation = (lat: number, lng: number) => {
    userLat.current = lat;
    userLng.current = lng;
    moveToUserLocation(lat, lng, true);
    // "전체" 모드는 지도가 멈추면(idle) 그 화면으로 다시 불러옴
    if (!isAllMode) fetchStations();
  };

  // 지도 탭을 보고 있는 동안 위치를 추적해서 파란 점을 옮김 (충전소는 5분 새로고침·필터 변경 때 이 위치로 불러옴)
  useFocusEffect(
    useCallback(() => {
      let sub: Location.LocationSubscription | null = null;
      let cancelled = false;
      (async () => {
        // 권한은 묻지 않음: 처음 화면과 내 위치 버튼에서 이미 물어봄
        const perm = await Location.getForegroundPermissionsAsync();
        if (!perm.granted || cancelled) return;
        const s = await Location.watchPositionAsync(
          { accuracy: Location.Accuracy.Balanced, distanceInterval: WATCH_DISTANCE_METERS, timeInterval: WATCH_INTERVAL_MS },
          (loc) => {
            userLat.current = loc.coords.latitude;
            userLng.current = loc.coords.longitude;
            if (mapLoaded.current) {
              webviewRef.current?.injectJavaScript(
                `window.setMyLocationDot && window.setMyLocationDot(${loc.coords.latitude}, ${loc.coords.longitude}); true;`
              );
            }
          }
        );
        if (cancelled) s.remove();
        else sub = s;
      })().catch(() => {});
      return () => {
        cancelled = true;
        sub?.remove();
      };
    }, [])
  );

  const refreshRef = useRef(() => {});
  refreshRef.current = () => {
    if (!isAllMode) fetchStations(null, true);
    else if (lastView.current) loadView(lastView.current, true);
  };
  useFocusEffect(
    useCallback(() => {
      const timer = setInterval(() => refreshRef.current(), REFRESH_INTERVAL_MS);
      return () => clearInterval(timer);
    }, [])
  );

  useEffect(() => {
    // 빈 목록도 보내야 이전 마커가 지워짐 (모드 전환, 시·도 요약)
    if (!mapReady) return;
    const payload = stations.map(s => ({
      statId: s.statId,
      lat: s.lat,
      lng: s.lng,
      availableCount: s.availableCount,
      hasCharging: s.hasCharging,
      allUnavailable: s.allUnavailable,
      allUnknown: s.allUnknown,
    }));
    webviewRef.current?.injectJavaScript(
      `window.updateStationMarkers(${JSON.stringify(payload)}); true;`
    );
  }, [stations, mapReady]);

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
    if (id === "radius") return isAllMode ? "전체 지역" : `반경 ${RADIUS_STEPS[radiusIdx]}`;
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

  const isActive = (id: string): boolean => {
    const map: Record<string, boolean> = {
      available,
      parking: freeParking,
      open: openOnly,
      type: selTypes.length > 0,
      facility: selFacilities.length > 0,
      floor: selFloor.length > 0,
    };
    return map[id] ?? false;
  };

  const renderStation = ({ item }: { item: Station }) => {
    const tags = stationTags(item);
    const pred = item.nextHourCongestionLevel;
    return (
      <TouchableOpacity style={S.card} onPress={() => router.push(`/station/${item.statId}` as any)}>
        <View style={S.cardRow}>
          <Text style={S.cardName} numberOfLines={2}>{item.statNm}</Text>
          <Text style={S.cardDist}>{formatDistance(item.distance)}</Text>
        </View>
        <View style={S.cardRow2}>
          <View style={S.stTagRow}>
            {tags.map((t) => (
              <View key={t} style={S.stTag}><Text style={S.stTagTxt}>{t}</Text></View>
            ))}
          </View>
          {item.averageRating != null && (
            <View style={S.ratingRow}>
              <Ionicons name="star" size={11} color="#FFB800" />
              <Text style={S.ratingTxt}>{item.averageRating.toFixed(1)}</Text>
            </View>
          )}
        </View>
        <Text style={S.cardOp}>{item.busiNm} · {item.hasFast ? "급속" : "완속"}</Text>
        <View style={S.cardBottom}>
          <View style={S.availRow}>
            <Text style={S.availLbl}>충전가능 </Text>
            <Text style={[S.availNum, { color: item.availableCount > 0 ? ACCENT : "#aaa" }]}>{item.availableCount}</Text>
            <Text style={S.availTotal}>/{item.totalCount}</Text>
          </View>
          {/* 지금 쓸 수 없는 충전소는 예측 대신 상태를 보여줌 (색은 지도 마커와 같은 기준) */}
          {item.allUnavailable ? (
            <Text style={[S.stateTxt, { color: "#F44336" }]}>사용불가</Text>
          ) : item.allUnknown ? (
            <Text style={[S.stateTxt, { color: "#999" }]}>알수없음</Text>
          ) : pred ? (
            <View style={[S.predBadge, { backgroundColor: predColor(pred) }]}>
              <Text style={S.predTxt}>
                1시간 뒤 {pred} 예상
              </Text>
            </View>
          ) : null}
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <View style={S.container}>
      {mapCenter && (
        <WebView
          ref={webviewRef}
          source={{ html: makeMapHTML(mapCenter.lat, mapCenter.lng), baseUrl: "http://localhost" }}
          style={StyleSheet.absoluteFill}
          originWhitelist={["*"]}
          javaScriptEnabled
          domStorageEnabled
          onLoadEnd={() => {
            mapLoaded.current = true;
            setTimeout(() => {
              setMapReady(true);
              if (pendingLocation.current) {
                moveToUserLocation(pendingLocation.current.lat, pendingLocation.current.lng);
                pendingLocation.current = null;
              }
            }, 1500);
          }}
          onMessage={(e) => {
            try {
              const data = JSON.parse(e.nativeEvent.data);
              if (data.type === "st") {
                router.push(`/station/${data.id}` as any);
              } else if (data.type === "center") {
                AsyncStorage.setItem("mapLocation", JSON.stringify({ lat: data.lat, lng: data.lng }));
              } else if (data.type === "view") {
                lastView.current = data;
                if (!isAllModeRef.current) return;
                // 지도를 연달아 움직이면 마지막으로 멈춘 화면만 불러옴
                if (viewTimer.current) clearTimeout(viewTimer.current);
                viewTimer.current = setTimeout(() => loadViewRef.current(data), 500);
              }
            } catch {}
          }}
        />
      )}

      <SafeAreaView edges={["top"]} style={S.topOverlay} pointerEvents="box-none">
        <View style={S.searchRow}>
          <View style={S.searchBar}>
            <TextInput placeholder="충전소 검색" style={S.searchInput} placeholderTextColor="#999" />
            <Ionicons name="search-outline" size={20} color="#999" />
          </View>
        </View>
        <TouchableOpacity style={S.myLocBtn} onPress={goToMyLocation} disabled={locating}>
          {locating
            ? <ActivityIndicator size="small" color={ACCENT} />
            : <Ionicons name="locate" size={22} color="#333" />}
        </TouchableOpacity>
      </SafeAreaView>

      <Animated.View style={[S.listSheet, { transform: [{ translateY: listSheetY }] }]}>
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

        {popupChip && (
          <View style={S.inlinePopup}>
            {popupChip === "radius" && (
              <>
                <View style={S.secRow}>
                  <Text style={S.secTitle}>내 위치에서 반경</Text>
                  <Text style={S.secVal}>{isAllMode ? "지도 화면 전체" : `~${RADIUS_STEPS[radiusIdx]}`}</Text>
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
          data={stations}
          keyExtractor={(item) => item.statId}
          renderItem={renderStation}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: 20 }}
          onEndReached={() => { if (nextCursor && !stationsLoading) fetchStations(nextCursor); }}
          onEndReachedThreshold={0.3}
          ListFooterComponent={stationsLoading ? <ActivityIndicator style={{ padding: 16 }} color={ACCENT} /> : null}
          ListEmptyComponent={
            !stationsLoading ? (
              <View style={{ alignItems: "center", paddingTop: 40, gap: 8 }}>
                <Text style={{ color: "#aaa", fontSize: 14 }}>
                  {fetchError ? "불러오지 못했어요" : regionMode ? "지도를 확대하면 충전소 목록이 보여요" : "주변 충전소가 없어요"}
                </Text>
              </View>
            ) : null
          }
        />
      </Animated.View>

      <Modal visible={sheetVisible} transparent animationType="none" onRequestClose={closeFullSheet}>
        <TouchableOpacity style={S.backdrop} activeOpacity={1} onPress={closeFullSheet} />
        <Animated.View style={[S.sheet, { transform: [{ translateY: slideAnim }] }]}>
          <View style={S.handle} />
          <Text style={S.sheetTitle}>필터 설정</Text>
          <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 20 }}>
            <View style={S.sec}>
              <View style={S.secRow}>
                <Text style={S.secTitle}>내 위치에서 반경</Text>
                <Text style={S.secVal}>{isAllMode ? "지도 화면 전체" : `~${RADIUS_STEPS[radiusIdx]}`}</Text>
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
  myLocBtn: {
    alignSelf: "flex-end", marginRight: 16, marginTop: 8, width: 44, height: 44, borderRadius: 22,
    backgroundColor: "#fff", alignItems: "center", justifyContent: "center",
    shadowColor: "#000", shadowOpacity: 0.1, shadowRadius: 6, elevation: 4,
  },
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
  predTxt: { fontSize: 12, fontWeight: "600", color: "#333" },
  stateTxt: { fontSize: 13, fontWeight: "700" },
  backdrop: { ...StyleSheet.absoluteFill, backgroundColor: "rgba(0,0,0,0.3)" },
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