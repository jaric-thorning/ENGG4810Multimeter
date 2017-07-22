#ifndef VOLTAGE_H_   /* Include guard */
#define VOLTAGE_H_

void change_voltage(int voltage);
void change_ac_voltage(int voltage);

int check_voltage_range(float value, int mode, int current_range);


#endif // VOLTAGE_H_
