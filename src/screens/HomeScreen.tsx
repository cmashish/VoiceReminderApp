import React, {useEffect, useState, useCallback} from 'react';
import {
  Alert,
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';

import ReminderCard from '../components/ReminderCard';
import {Alarm} from '../types/Alarm';
import {
  addAlarm,
  deleteAlarm,
  getAlarms,
} from '../storage/AlarmStorage';
import {
  cancelAlarm,
  hasExactAlarmPermission,
  requestExactAlarmPermission,
  scheduleAlarm,
} from '../services/AlarmService';
import {requestNotificationPermission} from '../services/PermissionService';

export default function HomeScreen() {
  const [title, setTitle] = useState('');
  const [selectedDate, setSelectedDate] = useState(
    new Date(Date.now() + 5 * 60 * 1000),
  );
  const [showPicker, setShowPicker] = useState(false);
  const [alarms, setAlarms] = useState<Alarm[]>([]);

  const initialize = useCallback(async () => {
    await requestNotificationPermission();
    await loadAlarms();

    const exact = await hasExactAlarmPermission();
    if (!exact) {
      Alert.alert(
        'Enable exact alarms',
        'Android needs the Alarms & reminders permission so your reminder can fire at the exact time.',
        [
          {text: 'Not now', style: 'cancel'},
          {text: 'Open Settings', onPress: requestExactAlarmPermission},
        ],
      );
    }
  }, []);

  useEffect(() => {
    initialize();
  }, [initialize]);

  async function loadAlarms() {
    setAlarms(await getAlarms());
  }

  async function createAlarm() {
    if (!title.trim()) {
      Alert.alert('Reminder', 'Please enter reminder text.');
      return;
    }

    if (selectedDate.getTime() <= Date.now()) {
      Alert.alert('Invalid time', 'Please select a future time.');
      return;
    }

    const exact = await hasExactAlarmPermission();
    if (!exact) {
      await requestExactAlarmPermission();
      return;
    }

    const id = Date.now();
    const alarm: Alarm = {
      id,
      title: title.trim(),
      triggerAt: selectedDate.getTime(),
      enabled: true,
    };

    try {
      await scheduleAlarm(id, alarm.title, alarm.triggerAt);
      await addAlarm(alarm);
      setAlarms(prev => [...prev, alarm]);
      setTitle('');
      Alert.alert(
        'Alarm scheduled',
        `${alarm.title}\n${selectedDate.toLocaleString()}`,
      );
    } catch (error) {
      console.error(error);
      Alert.alert(
        'Unable to schedule',
        'Check notification and exact-alarm permissions and try again.',
      );
    }
  }

  async function removeAlarm(id: number) {
    await cancelAlarm(id);
    await deleteAlarm(id);
    setAlarms(prev => prev.filter(alarm => alarm.id !== id));
  }

  return (
    <SafeAreaView style={styles.container}>
      <FlatList
        data={alarms}
        keyExtractor={item => item.id.toString()}
        renderItem={({item}) => (
          <ReminderCard alarm={item} onDelete={removeAlarm} />
        )}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <>
            <Text style={styles.heading}>Remind Me</Text>
            <Text style={styles.subtitle}>
              Native-style reminders that work even when the app is closed.
            </Text>

            <TextInput
              value={title}
              onChangeText={setTitle}
              placeholder="What do you want to remember?"
              placeholderTextColor="#999"
              style={styles.input}
              returnKeyType="done"
            />

            <TouchableOpacity
              style={styles.timeButton}
              onPress={() => setShowPicker(true)}>
              <Text style={styles.timeButtonText}>
                {selectedDate.toLocaleString([], {
                  dateStyle: 'medium',
                  timeStyle: 'short',
                })}
              </Text>
            </TouchableOpacity>

            {showPicker && (
              <DateTimePicker
                value={selectedDate}
                mode="datetime"
                minimumDate={new Date()}
                onChange={(event: any, date?: Date | undefined) => {
                  setShowPicker(false);
                  if (date) {
                    setSelectedDate(date);
                  }
                }}
              />
            )}

            <TouchableOpacity
              style={styles.scheduleButton}
              onPress={createAlarm}>
              <Text style={styles.scheduleText}>Schedule Alarm</Text>
            </TouchableOpacity>

            <Text style={styles.sectionTitle}>Scheduled reminders</Text>
          </>
        }
        ListEmptyComponent={
          <Text style={styles.empty}>
            No reminders yet. Create your first one above.
          </Text>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: '#F5F7FA'},
  list: {padding: 20},
  heading: {fontSize: 36, fontWeight: '800', marginTop: 20, color: '#111'},
  subtitle: {color: '#666', marginTop: 6, marginBottom: 28},
  input: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 18,
    fontSize: 16,
    marginBottom: 14,
    color: '#111',
  },
  timeButton: {
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 18,
    marginBottom: 14,
  },
  timeButtonText: {fontSize: 17, fontWeight: '600', color: '#111'},
  scheduleButton: {
    backgroundColor: '#111',
    padding: 18,
    borderRadius: 16,
    alignItems: 'center',
  },
  scheduleText: {color: '#FFF', fontSize: 17, fontWeight: '700'},
  sectionTitle: {
    fontSize: 21,
    fontWeight: '700',
    marginVertical: 24,
    color: '#111',
  },
  empty: {color: '#777', textAlign: 'center', marginTop: 20},
});