import React, { useState } from "react";
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView,
  Modal, TextInput, Alert,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";

type Car = {
  id: string;
  brand: string;
  model: string;
  trim: string;
  year: string;
  range: number;
  batteryKwh: number;
};

const MOCK_CARS: Car[] = [
  { id: "1", brand: "현대", model: "아이오닉 5", trim: "Long Range", year: "2025", range: 485, batteryKwh: 84 },
  { id: "2", brand: "기아", model: "ev9", trim: "Standard", year: "2026", range: 374, batteryKwh: 76.1 },
];

const EMPTY_FORM = { brand: "", model: "", trim: "", year: "", range: "", batteryKwh: "" };

export default function CarManagementScreen() {
  const router = useRouter();
  const [cars, setCars] = useState(MOCK_CARS);
  const [modalVisible, setModalVisible] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editId, setEditId] = useState<string | null>(null);

  const openAdd = () => {
    setForm(EMPTY_FORM);
    setEditId(null);
    setModalVisible(true);
  };

  const openEdit = (car: Car) => {
    setForm({
      brand: car.brand, model: car.model, trim: car.trim,
      year: car.year, range: String(car.range), batteryKwh: String(car.batteryKwh),
    });
    setEditId(car.id);
    setModalVisible(true);
  };

  const handleSave = () => {
    if (!form.brand || !form.model) {
      Alert.alert("입력 오류", "브랜드와 모델명을 입력해주세요.");
      return;
    }
    const newCar: Car = {
      id: editId ?? String(Date.now()),
      brand: form.brand, model: form.model, trim: form.trim,
      year: form.year, range: Number(form.range) || 0,
      batteryKwh: Number(form.batteryKwh) || 0,
    };
    if (editId) {
      setCars(prev => prev.map(c => c.id === editId ? newCar : c));
    } else {
      setCars(prev => [...prev, newCar]);
    }
    setModalVisible(false);
  };

  const handleDelete = (id: string) => {
    Alert.alert("차량 삭제", "이 차량을 삭제할까요?", [
      { text: "취소", style: "cancel" },
      { text: "삭제", style: "destructive", onPress: () => setCars(prev => prev.filter(c => c.id !== id)) },
    ]);
  };

  return (
    <SafeAreaView style={S.container} edges={["top"]}>
      {/* 헤더 */}
      <View style={S.header}>
        <TouchableOpacity onPress={() => router.back()} style={S.headerBtn}>
          <Ionicons name="chevron-back" size={24} color="#111" />
        </TouchableOpacity>
        <Text style={S.headerTitle}>내 차량 관리</Text>
        <View style={S.headerBtn} />
      </View>

      <ScrollView contentContainerStyle={S.scroll}>
        {/* 차량 카드 */}
        {cars.map(car => (
          <TouchableOpacity
            key={car.id}
            style={S.card}
            onLongPress={() => openEdit(car)}
            activeOpacity={0.85}
          >
            {/* 왼쪽: 아이콘 + 이름 */}
            <View style={S.cardLeft}>
              <Ionicons name="car-outline" size={36} color="#555" />
              <Text style={S.carName}>{car.brand} {car.model}</Text>
              <Text style={S.carTrim}>{car.trim}</Text>
            </View>

            {/* 오른쪽: 스펙 테이블 */}
            <View style={S.cardRight}>
              {[
                { label: "연식", value: `${car.year}년형` },
                { label: "주행거리", value: `${car.range}km` },
                { label: "배터리 용량", value: `${car.batteryKwh}kWh` },
              ].map(({ label, value }) => (
                <View key={label} style={S.specRow}>
                  <Text style={S.specLabel}>{label}</Text>
                  <Text style={S.specValue}>{value}</Text>
                </View>
              ))}
            </View>

            {/* 삭제 버튼 */}
            <TouchableOpacity
              style={S.deleteBtn}
              onPress={() => handleDelete(car.id)}
              hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            >
              <Ionicons name="close-circle" size={18} color="#ddd" />
            </TouchableOpacity>
          </TouchableOpacity>
        ))}

        {/* 추가 버튼 */}
        <TouchableOpacity style={S.addBtn} onPress={openAdd}>
          <Ionicons name="add-circle-outline" size={32} color="#aaa" />
        </TouchableOpacity>
      </ScrollView>

      {/* 추가/수정 모달 */}
      <Modal visible={modalVisible} animationType="slide" transparent onRequestClose={() => setModalVisible(false)}>
        <TouchableOpacity style={S.backdrop} activeOpacity={1} onPress={() => setModalVisible(false)} />
        <View style={S.modalSheet}>
          <View style={S.modalHandle} />
          <Text style={S.modalTitle}>{editId ? "차량 수정" : "차량 추가"}</Text>
          <ScrollView showsVerticalScrollIndicator={false}>
            {[
              { label: "브랜드", key: "brand", placeholder: "예: 현대" },
              { label: "모델명", key: "model", placeholder: "예: 아이오닉 5" },
              { label: "트림", key: "trim", placeholder: "예: Long Range" },
              { label: "연식", key: "year", placeholder: "예: 2025" },
              { label: "주행거리 (km)", key: "range", placeholder: "예: 485", numeric: true },
              { label: "배터리 용량 (kWh)", key: "batteryKwh", placeholder: "예: 84", numeric: true },
            ].map(({ label, key, placeholder, numeric }) => (
              <View key={key} style={S.inputGroup}>
                <Text style={S.inputLabel}>{label}</Text>
                <TextInput
                  style={S.input}
                  placeholder={placeholder}
                  placeholderTextColor="#bbb"
                  value={form[key as keyof typeof form]}
                  onChangeText={v => setForm(prev => ({ ...prev, [key]: v }))}
                  keyboardType={numeric ? "decimal-pad" : "default"}
                />
              </View>
            ))}
          </ScrollView>
          <TouchableOpacity style={S.saveBtn} onPress={handleSave}>
            <Text style={S.saveTxt}>{editId ? "수정 완료" : "추가 완료"}</Text>
          </TouchableOpacity>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const S = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f2f2f2" },
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
  carTrim: { fontSize: 11, color: "#aaa", textAlign: "center" },

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

  backdrop: { flex: 1, backgroundColor: "rgba(0,0,0,0.3)" },
  modalSheet: {
    backgroundColor: "#fff", borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, maxHeight: "80%",
  },
  modalHandle: { width: 40, height: 4, borderRadius: 2, backgroundColor: "#ddd", alignSelf: "center", marginBottom: 16 },
  modalTitle: { fontSize: 17, fontWeight: "700", color: "#111", textAlign: "center", marginBottom: 20 },
  inputGroup: { marginBottom: 14 },
  inputLabel: { fontSize: 13, color: "#555", fontWeight: "600", marginBottom: 6 },
  input: {
    borderWidth: 1.5, borderColor: "#e8e8e8", borderRadius: 10,
    paddingHorizontal: 14, paddingVertical: 11,
    fontSize: 14, color: "#222", backgroundColor: "#fafafa",
  },
  saveBtn: {
    backgroundColor: "#5B9CF6", borderRadius: 12, height: 50,
    alignItems: "center", justifyContent: "center", marginTop: 12,
  },
  saveTxt: { color: "#fff", fontSize: 16, fontWeight: "600" },
});
