import React, {useEffect, useState} from 'react';
import {
  Alert,
  NativeModules,
  PermissionsAndroid,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';

const {AlarmModule} = NativeModules;

type Alarm = {
  id: string;
  title: string;
  message?: string;
  timestamp: number;
  enabled: boolean;
};

function nextTimestamp(time: string) {
  const [hours, minutes] = time.split(':').map(Number);
  const d = new Date();
  d.setSeconds(0, 0);
  d.setHours(hours, minutes, 0, 0);
  if (d.getTime() <= Date.now()) d.setDate(d.getDate() + 1);
  return d.getTime();
}

export default function App() {
  const [time, setTime] = useState(() => { const d = new Date(); return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`; });
  const [now, setNow] = useState(new Date());
  const [title, setTitle] = useState('Reminder');
  const [message, setMessage] = useState('It is time for your reminder.');
  const [alarms, setAlarms] = useState<Alarm[]>([]);
  const [offlineReady, setOfflineReady] = useState(true);

  const load = async () => {
    try {
      const raw = await AlarmModule.getAlarms();
      const parsed = JSON.parse(raw);
      setAlarms(Array.isArray(parsed) ? parsed : []);
      setOfflineReady(true);
    } catch (e) {
      console.warn('Unable to load alarms', e);
      setOfflineReady(true);
    }
  };

    useEffect(() => {
        if (Platform.OS === 'android' && Platform.Version >= 33) {
            PermissionsAndroid.request('android.permission.POST_NOTIFICATIONS' as any).catch(() => { });
        }
    load();
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
    
  }, []);

  const openTimePicker = async () => {
    const [hour, minute] = time.split(':').map(Number);
    try {
      const selected = await AlarmModule.showTimePicker(hour, minute);
      if (selected) setTime(selected);
    } catch (e: any) {
      Alert.alert('Time picker', e?.message || 'Unable to open the system time picker.');
    }
  };

  const addAlarm = async () => {
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
      Alert.alert('Invalid time', 'Use 24-hour format, for example 07:30.');
      return;
    }
    if (!message.trim()) {
      Alert.alert('Message required', 'Enter the text you want the notification and voice to use.');
      return;
    }

    try {
      const exact = await AlarmModule.canScheduleExactAlarms();
      if (!exact) {
        Alert.alert('Allow exact alarms', 'Android requires exact-alarm access for precise reminders.', [
          {text: 'Open Settings', onPress: () => AlarmModule.openExactAlarmSettings()},
          {text: 'Cancel', style: 'cancel'},
        ]);
        return;
      }

      const fullScreenAllowed = await AlarmModule.canUseFullScreenIntent();
      if (!fullScreenAllowed) {
        Alert.alert(
          'Allow full-screen alarms',
          'Android is currently blocking full-screen alarm screens. Enable it so the alarm can appear over the lock screen.',
          [
            {text: 'Open Settings', onPress: () => AlarmModule.openFullScreenIntentSettings()},
            {text: 'Continue', onPress: () => undefined},
          ]
        );
      }

      const id = `${Date.now()}`;
      const timestamp = nextTimestamp(time);
      const alarm = {id, title: title.trim() || 'Reminder', message: message.trim(), timestamp, enabled: true};
      await AlarmModule.scheduleAlarm(id, alarm.title, timestamp, JSON.stringify(alarm));
      await load();
      Alert.alert('Alarm scheduled', `${alarm.title}\n${new Date(timestamp).toLocaleString()}`);
    } catch (e: any) {
      Alert.alert('Schedule failed', e?.message || 'Unable to schedule the alarm.');
    }
  };

  const removeAlarm = async (id: string) => {
    await AlarmModule.cancelAlarm(id);
    await load();
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Reminder App</Text>
        <Text style={styles.subtitle}>Voice reminder</Text>
        <View style={styles.offlineBadge}>
          <View style={styles.offlineDot} />
          <Text style={styles.offlineText}>{offlineReady ? 'Works offline • alarms stored on this device' : 'Preparing offline storage…'}</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.label}>Current time</Text>
          <Text style={styles.currentTime}>{now.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}</Text>

          <Text style={styles.label}>Alarm time</Text>
          <Pressable style={styles.timeButton} onPress={openTimePicker}>
            <Text style={styles.timeValue}>{time}</Text>
            <Text style={styles.timeHint}>Tap to choose time</Text>
          </Pressable>

          <Text style={styles.label}>Notification title</Text>
          <TextInput value={title} onChangeText={setTitle} placeholder="Reminder" style={styles.input} />

          <Text style={styles.label}>Reminder text / Voice TTS</Text>
          <TextInput
            value={message}
            onChangeText={setMessage}
            placeholder="What should the notification and voice say?"
            multiline
            style={[styles.input, styles.messageInput]}
          />

          <Pressable style={styles.primary} onPress={addAlarm}>
            <Text style={styles.primaryText}>Schedule Alarm</Text>
          </Pressable>
        </View>

        <Text style={styles.section}>Scheduled alarms</Text>
        {alarms.length === 0 ? (
          <Text style={styles.empty}>No alarms scheduled.</Text>
        ) : (
          alarms.map(a => (
            <View style={styles.alarm} key={a.id}>
              <View style={styles.alarmInfo}>
                <Text style={styles.alarmTitle}>{a.title}</Text>
                <Text style={styles.alarmTime}>{new Date(a.timestamp).toLocaleString()}</Text>
                <Text style={styles.alarmMessage}>{a.message || 'It is time.'}</Text>
              </View>
              <Pressable style={styles.delete} onPress={() => removeAlarm(a.id)}>
                <Text style={styles.deleteText}>Delete</Text>
              </Pressable>
            </View>
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#F5F7FB'},
  container: {padding: 20, paddingBottom: 40},
  title: {fontSize: 30, fontWeight: '800', color: '#111827'},
  subtitle: {marginTop: 6, color: '#6B7280'},
  offlineBadge: {marginTop: 12, alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', backgroundColor: '#ECFDF5', paddingHorizontal: 10, paddingVertical: 7, borderRadius: 999},
  offlineDot: {width: 8, height: 8, borderRadius: 4, backgroundColor: '#16A34A', marginRight: 7},
  offlineText: {fontSize: 12, fontWeight: '700', color: '#166534'},
  card: {marginTop: 20, padding: 18, borderRadius: 18, backgroundColor: '#FFF'},
  currentTime: {fontSize: 38, fontWeight: '800', color: '#111827', marginBottom: 6},
  timeButton: {borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, backgroundColor: '#FFF'},
  timeValue: {fontSize: 30, fontWeight: '800', color: '#111827'},
  timeHint: {marginTop: 3, color: '#6B7280'},
  label: {fontSize: 14, fontWeight: '700', color: '#374151', marginBottom: 7, marginTop: 10},
  input: {borderWidth: 1, borderColor: '#D1D5DB', borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 16, backgroundColor: '#FFF', color: '#111827'},
  messageInput: {minHeight: 90, textAlignVertical: 'top'},
  primary: {marginTop: 18, borderRadius: 12, paddingVertical: 14, alignItems: 'center', backgroundColor: '#111827'},
  primaryText: {color: '#FFF', fontSize: 16, fontWeight: '800'},
  section: {fontSize: 20, fontWeight: '800', color: '#111827', marginTop: 26, marginBottom: 12},
  empty: {color: '#6B7280'},
  alarm: {backgroundColor: '#FFF', borderRadius: 16, padding: 16, marginBottom: 10, flexDirection: 'row', alignItems: 'center'},
  alarmInfo: {flex: 1},
  alarmTitle: {fontSize: 17, fontWeight: '800', color: '#111827'},
  alarmTime: {marginTop: 4, color: '#4B5563'},
  alarmMessage: {marginTop: 5, color: '#6B7280'},
  delete: {paddingHorizontal: 12, paddingVertical: 9, borderRadius: 10, backgroundColor: '#FEE2E2'},
  deleteText: {color: '#B91C1C', fontWeight: '700'},
});
