import {NativeModules} from 'react-native';

type NativeAlarmModule = {
  scheduleAlarm: (id: number, title: string, triggerAt: number) => Promise<void>;
  cancelAlarm: (id: number) => Promise<void>;
  checkExactAlarmPermission: () => Promise<boolean>;
  requestExactAlarmPermission: () => Promise<void>;
};

export const AlarmModule =
  NativeModules.AlarmModule as NativeAlarmModule;

export const scheduleAlarm = (
  id: number,
  title: string,
  triggerAt: number,
) => AlarmModule.scheduleAlarm(id, title, triggerAt);

export const cancelAlarm = (id: number) =>
  AlarmModule.cancelAlarm(id);

export const hasExactAlarmPermission = () =>
  AlarmModule.checkExactAlarmPermission();

export const requestExactAlarmPermission = () =>
  AlarmModule.requestExactAlarmPermission();