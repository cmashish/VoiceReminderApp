declare module '@react-native-community/datetimepicker' {
  import * as React from 'react';

  type DateTimePickerProps = {
    value: Date;
    mode?: 'date' | 'time' | 'datetime';
    minimumDate?: Date;
    maximumDate?: Date;
    onChange?: (event: any, date?: Date | undefined) => void;
    display?: 'default' | 'spinner' | 'calendar' | 'clock' | 'inline';
    testID?: string;
  } & Record<string, any>;

  const DateTimePicker: React.ComponentType<DateTimePickerProps>;
  export default DateTimePicker;
}
