import React, { useState, useCallback } from "react";
import {
  View, Text, TouchableOpacity, StyleSheet, FlatList,
  Modal, TextInput, Alert, ActivityIndicator,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter, useFocusEffect } from "expo-router";
import api from "@/lib/api";

type UserCar = {
  userCarId: string; // UUID
  carName: string;
  batteryCapacity: number;
};

type CarResult = {
  carId: number;
  brand: string;
  model: string;
  trim: string | null;
  modelYear: number;
  batteryCapacity: number;
};

export default function CarManagementScreen() {
  const router = useRouter();
  const [cars, setCars] = useState<UserCar[]>([]);
  const [loading, setLoading] = useState(true);

  const [searchVisible, setSearchVisible] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [searchResults, setSearchResults] = useState<CarResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectedCar, setSelectedCar] = useState<CarResult | null>(null);
  const [batteryInput, setBatteryInput] = useState("");
  const [saving, setSaving] = useState(false);

  const fetchCars = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<UserCar[]>("/user/cars");
      setCars(res.data ?? []);
    } catch {
      Alert.alert("오류", "차량 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { fetchCars(); }, [fetchCars]));

  const searchCars = async () => {
    if (!keyword.trim()) return;
    setSearching(true);
    try {
      const res = await api.get<CarResult[]>("/cars", { params: { keyword: keyword.trim() } });
      setSearchResults(res.data ?? []);
    } catch {
      Alert.alert("오류", "차량 검색에 실패했습니다.");
    } finally {
      setSearching(false);
    }
  };

  const selectCar = (car: CarResult) => {
    setSelectedCar(car);
    setBatteryInput(String(car.batteryCapacity));
    setSearchResults([]);
    setKeyword(`${car.brand} ${car.model}${car.trim ? " " + car.trim : ""}`);
  };

  const handleAdd = async () => {
    if (!selectedCar) {
      Alert.alert("입력 오류", "차량을 검색해서 선택해주세요.");
      return;
    }
    const capacity = parseFloat(batteryInput);
    if (isNaN(capacity) || capacity <= 0) {
      Alert.alert("입력 오류", "배터리 용량을 올바르게 입력해주세요.");
      return;
    }
    setSaving(true);
    try {
      await api.post("/user/cars", null, {
        params: { carId: selectedCar.carId, batteryCapacity: capacity },
      });
      setSearchVisible(false);
      setSelectedCar(null);
      setKeyword("");
      setBatteryInput("");
      await fetchCars();
    } catch (e: any) {
      Alert.alert("오류", e?.response?.data?.message ?? "차량 추가에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = (userCarId: string, carName: string) => {
    Alert.alert("차량 삭제", `${carName}을(를) 삭제할까요?`, [
      { text: "취소", style: "cancel" },
      {
        text: "삭제", style: "destructive",
        onPress: async () => {
          try {
            await api.delete(`/user/cars/${userCarId}`);
            setCars(prev => prev.filter(c => c.userCarId !== userCarId));
          } catch {
            Alert.alert("오류", "차량 삭제에 실패했습니다.");
          }
        },
      },
    ]);
  };

  const openAdd = () => {
    setSelectedCar(null);
    setKeyword("");
    setBatteryInput("");
    setSearchResults([]);
    setSearchVisible(true);
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>내 차량 관리</Text>
        <View style={S.headerBtn} />
      </View>

      {loading ? (
        <View style={S.center}>
          <ActivityIndicator size="large" color="#5B9CF6" />
        </View>
      ) : (
        <FlatList
          data={cars}
          keyExtractor={item => item.userCarId}
          contentContainerStyle={S.scroll}
          renderItem={({ item }) => (
            <View style={S.card}>
              <View style={S.cardLeft}>
                <Ionicons name="car-outline" size={36} color="#555" />
                <Text style={S.carName}>{item.carName}</Text>
              </View>
              <View style={S.cardRight}>
                <View style={S.specRow}>
                  <Text style={S.specLabel}>배터리 용량</Text>
                  <Text style={S.specValue}>{item.batteryCapacity}kWh</Text>
                </View>
              </View>
              <TouchableOpacity
                style={S.deleteBtn}
                onPress={() => handleDelete(item.userCarId, item.carName)}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                <Ionicons name="close-circle" size={18} color="#ddd" />
              </TouchableOpacity>
            </View>
          )}
          ListFooterComponent={
            <TouchableOpacity style={S.addBtn} onPress={openAdd}>
              <Ionicons name="add-circle-outline" size={32} color="#aaa" />
            </TouchableOpacity>
          }
          ListEmptyComponent={
            <View style={S.empty}>
              <Ionicons name="car-outline" size={48} color="#ddd" />
              <Text style={S.emptyTxt}>등록된 차량이 없어요</Text>
            </View>
          }
        />
      )}

      <Modal
        visible={searchVisible}
        animationType="slide"
        transparent
        onRequestClose={() => setSearchVisible(false)}
      >
        <TouchableOpacity style={S.backdrop} activeOpacity={1} onPress={() => setSearchVisible(false)} />
        <View style={S.modalSheet}>
          <View style={S.modalHandle} />
          <Text style={S.modalTitle}>차량 추가</Text>

          <Text style={S.inputLabel}>차량 검색</Text>
          <View style={S.searchRow}>
            <TextInput
              style={[S.input, { flex: 1 }]}
              placeholder="브랜드 또는 모델명 입력"
              placeholderTextColor="#bbb"
              value={keyword}
              onChangeText={v => { setKeyword(v); setSelectedCar(null); }}
              onSubmitEditing={searchCars}
            />
            <TouchableOpacity style={S.searchBtn} onPress={searchCars}>
              {searching
                ? <ActivityIndicator size="small" color="#fff" />
                : <Ionicons name="search" size={18} color="#fff" />}
            </TouchableOpacity>
          </View>

          {searchResults.length > 0 && (
            <View style={S.resultList}>
              {searchResults.map(car => (
                <TouchableOpacity
                  key={car.carId}
                  style={S.resultItem}
                  onPress={() => selectCar(car)}
                >
                  <Text style={S.resultName}>
                    {car.brand} {car.model}{car.trim ? ` ${car.trim}` : ""}
                  </Text>
                  <Text style={S.resultSub}>{car.modelYear}년형 · {car.batteryCapacity}kWh</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          {selectedCar && (
            <>
              <Text style={[S.inputLabel, { marginTop: 16 }]}>배터리 용량 (kWh)</Text>
              <TextInput
                style={S.input}
                placeholder="예: 84"
                placeholderTextColor="#bbb"
                value={batteryInput}
                onChangeText={setBatteryInput}
                keyboardType="decimal-pad"
              />
            </>
          )}

          <TouchableOpacity
            style={[S.saveBtn, saving && { opacity: 0.6 }]}
            onPress={handleAdd}
            disabled={saving}
          >
            {saving
              ? <ActivityIndicator size="small" color="#fff" />
              : <Text style={S.saveTxt}>추가 완료</Text>}
          </TouchableOpacity>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f2f2f2" },
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    backgroundColor: "#fff", paddingHorizontal: 4, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: "#f0f0f0",
  },
  headerBtn: { padding: 10, width: 44 },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111" },
  scroll: { padding: 16, gap: 10 },
  card: {
    backgroundColor: "#fff", borderRadius: 14, padding: 18,
    flexDirection: "row", alignItems: "center",
    shadowColor: "#000", shadowOpacity: 0.04, shadowRadius: 4, elevation: 1,
  },
  cardLeft: { alignItems: "center", width: 90, gap: 6, marginRight: 16 },
  carName: { fontSize: 13, fontWeight: "700", color: "#111", textAlign: "center" },
  cardRight: { flex: 1, gap: 6 },
  specRow: { flexDirection: "row", justifyContent: "space-between" },
  specLabel: { fontSize: 12, color: "#888" },
  specValue: { fontSize: 12, color: "#222", fontWeight: "600" },
  deleteBtn: { position: "absolute", top: 10, right: 10 },
  addBtn: {
    backgroundColor: "#fff", borderRadius: 14, padding: 20,
    alignItems: "center", justifyContent: "center",
    shadowColor: "#000", shadowOpacity: 0.04, shadowRadius: 4, elevation: 1,
  },
  empty: { alignItems: "center", paddingTop: 60, gap: 12 },
  emptyTxt: { fontSize: 15, color: "#bbb" },
  backdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.3)" },
  modalSheet: {
    backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, maxHeight: "85%",
  },
  modalHandle: { width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd", alignSelf: "center", marginBottom: 16 },
  modalTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", marginBottom: 20 },
  inputLabel: { fontSize: 13, color: "#555", fontWeight: "600", marginBottom: 6 },
  searchRow: { flexDirection: "row", gap: 8, marginBottom: 8 },
  input: {
    borderWidth: 1.5, borderColor: "#e8e8e8", borderRadius: 10,
    paddingHorizontal: 14, paddingVertical: 11,
    fontSize: 14, color: "#222", backgroundColor: "#fafafa",
  },
  searchBtn: {
    backgroundColor: "#5B9CF6", borderRadius: 10,
    paddingHorizontal: 14, alignItems: "center", justifyContent: "center",
  },
  resultList: {
    borderWidth: 1, borderColor: "#f0f0f0", borderRadius: 10,
    maxHeight: 180, marginBottom: 8,
  },
  resultItem: { padding: 12, borderBottomWidth: 1, borderBottomColor: "#f5f5f5" },
  resultName: { fontSize: 14, fontWeight: "600", color: "#111" },
  resultSub: { fontSize: 12, color: "#888", marginTop: 2 },
  saveBtn: {
    backgroundColor: "#5B9CF6", borderRadius: 12, height: 50,
    alignItems: "center", justifyContent: "center", marginTop: 20,
  },
  saveTxt: { color: "#fff", fontSize: 16, fontWeight: "600" },
});