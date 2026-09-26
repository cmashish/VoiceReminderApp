import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {Alarm} from '../types/Alarm';
import {formatAlarmDate, formatAlarmTime} from '../utils/dateUtils';

type Props = {
  alarm: Alarm;
  onDelete: (id: number) => void;
};

export default function ReminderCard({alarm, onDelete}: Props) {
  return (
    <View style={styles.card}>
      <View style={styles.content}>
        <Text style={styles.title}>{alarm.title}</Text>
        <Text style={styles.time}>{formatAlarmTime(alarm.triggerAt)}</Text>
        <Text style={styles.date}>{formatAlarmDate(alarm.triggerAt)}</Text>
      </View>
      <TouchableOpacity
        accessibilityRole="button"
        onPress={() => onDelete(alarm.id)}
        style={styles.deleteButton}>
        <Text style={styles.deleteText}>Delete</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 18,
    padding: 18,
    marginBottom: 12,
    elevation: 3,
  },
  content: {flex: 1},
  title: {fontSize: 18, fontWeight: '700', color: '#111'},
  time: {fontSize: 28, fontWeight: '700', marginTop: 8, color: '#111'},
  date: {color: '#777', marginTop: 4},
  deleteButton: {justifyContent: 'center', paddingHorizontal: 10},
  deleteText: {color: '#D32F2F', fontWeight: '600'},
});