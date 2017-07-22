#ifndef CURRENT_H_   /* Include guard */
#define CURRENT_H_


void change_current(int current);
int check_current_range(float value, int range_current);
double adjust_current_value(double value, int range);

#endif // CURRENT_H_
