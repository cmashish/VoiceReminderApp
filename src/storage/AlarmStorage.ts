import AsyncStorage from '@react-native-async-storage/async-storage';
import {Alarm} from '../types/Alarm';

const STORAGE_KEY = '@reminder_app_alarms';

export async function getAlarms(): Promise<Alarm[]> {
  try {
    const value = await AsyncStorage.getItem(STORAGE_KEY);
    return value ? JSON.parse(value) : [];
  } catch {
    return [];
  }
}

export async function saveAlarms(alarms: Alarm[]) {
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(alarms));
}

export async function addAlarm(alarm: Alarm) {
  const alarms = await getAlarms();
  alarms.push(alarm);
  await saveAlarms(alarms);
}

export async function deleteAlarm(id: number) {
  const alarms = await getAlarms();
  await saveAlarms(alarms.filter(alarm => alarm.id !== id));
}