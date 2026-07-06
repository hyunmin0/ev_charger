import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function TabLayout() {
  return (
    <Tabs screenOptions={{ headerShown: false, tabBarActiveTintColor: "#4CAF50" }}>
      <Tabs.Screen
        name="index"
        options={{
          title: "지도",
          tabBarIcon: ({ color }) => <Ionicons name="location-outline" size={24} color={color} />,
        }}
      />
      <Tabs.Screen
  name="calculator"
  options={{
    title: "계산기",
    tabBarIcon: ({ color }) => <Ionicons name="calculator-outline" size={24} color={color} />,
  }}
/>
<Tabs.Screen
  name="notifications"
  options={{
    title: "채팅",
    tabBarIcon: ({ color }) => <Ionicons name="chatbubble-outline" size={24} color={color} />,
  }}
/>
      <Tabs.Screen
        name="mypage"
        options={{
          title: "계정",
          tabBarIcon: ({ color }) => <Ionicons name="person-outline" size={24} color={color} />,
        }}
      />
    </Tabs>
  );
}