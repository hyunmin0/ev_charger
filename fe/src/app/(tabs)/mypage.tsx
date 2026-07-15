import React, { useState } from "react";
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";


const menuItems = [
  { label: "내 차량 관리", icon: "car-outline" as const, route: "/car-management" },
  { label: "즐겨찾기 충전소", icon: "star-outline" as const, route: "/" },
  { label: "충전기 알림 관리", icon: "notifications-outline" as const, route: "/" },
  { label: "내 리뷰", icon: "chatbubble-outline" as const, route: "/my-reviews" },
  { label: "공지사항", icon: null, route: "/" },
];
export default function MypageScreen() {
   const [isLoggedIn, setIsLoggedIn] = useState(false);
  return (
    <SafeAreaView style={styles.container}edges=
    {["bottom", "left", "right"]}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>마이페이지</Text>
        <View style={styles.headerIcons}>
          <TouchableOpacity style={styles.iconBtn}>
            <Ionicons name="notifications-outline" size={24} color="#333" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconBtn}>
            <Ionicons name="settings-outline" size={24} color="#333" />
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView style={styles.scroll}>
        {/* 프로필 카드 */}
        <TouchableOpacity style={styles.profileCard}>
          <View style={styles.profileImage}>
            <Ionicons name="person-outline" size={36} color="#999" />
          </View>
          <Text style={styles.loginText}>로그인하기</Text>
        </TouchableOpacity>

        {/* 메뉴 카드 */}
        <View style={styles.card}>
          {menuItems.map((item, index) => (
            <TouchableOpacity
  key={item.label}
  style={[styles.menuItem, index < menuItems.length - 1 && styles.menuBorder]}
  onPress={() => router.push(item.route as any)}
>
              <View style={styles.menuLeft}>
                {item.icon
                  ? <Ionicons name={item.icon} size={20} color="#555" style={styles.menuIcon} />
                  : <View style={{ width: null }} />
                }
                <Text style={styles.menuLabel}>{item.label}</Text>
              </View>
              <Ionicons name="chevron-forward" size={18} color="#ccc" />
            </TouchableOpacity>
          ))}
        </View>

        {/* 로그아웃 카드 */}
{isLoggedIn && (
  <View style={styles.card}>
    <TouchableOpacity style={styles.menuItem} onPress={() => setIsLoggedIn(false)}>
      <View style={styles.menuLeft}>
        <Ionicons name="log-out-outline" size={20} color="#e53935" style={styles.menuIcon} />
        <Text style={styles.logoutText}>로그아웃</Text>
      </View>
    </TouchableOpacity>
  </View>
)}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f5f5f5" },
  header: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff" },
  headerTitle: { fontSize: 18, fontWeight: "bold" },
  headerIcons: { flexDirection: "row" },
  iconBtn: { marginLeft: 16 },
  scroll: { padding: 12 },
  profileCard: { flexDirection: "row", alignItems: "center", backgroundColor: "#fff", borderRadius: 12, marginBottom: 12, paddingHorizontal: 16, paddingVertical: 16 },
  profileImage: { width: 52, height: 52, borderRadius: 26, backgroundColor: "#f0f0f0", alignItems: "center", justifyContent: "center" },
  loginText: { fontSize: 16, fontWeight: "500", marginLeft: 14 },
  card: { backgroundColor: "#fff", borderRadius: 12, marginBottom: 12, paddingHorizontal: 16, overflow: "hidden" },
  menuItem: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: 16 },
  menuBorder: { borderBottomWidth: 1, borderBottomColor: "#f0f0f0" },
  menuLeft: { flexDirection: "row", alignItems: "center" },
  menuIcon: { marginRight: 12 },
  menuLabel: { fontSize: 15 },
  logoutText: { fontSize: 15, color: "#e53935" },
});