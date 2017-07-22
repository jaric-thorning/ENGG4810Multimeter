#ifndef MODE_FUNCTIONS_H   /* Include guard */
#define MODE_FUNCTIONS_H

#define DC_VOLTAGE 0
#define AC_VOLTAGE 1
#define DC_CURRENT 2
#define AC_CURRENT 3
#define RESISTANCE 4
#define LOGIC      5
#define CONTINUITY 6

#define NUM_MODES 7


#define S1_A_PIN 0
#define S1_B_PIN 1
#define S1_C_PIN 2
#define S2_A_PIN 3
#define S2_B_PIN 4
#define S2_I_PIN 5
#define S3_A_PIN 6
#define S3_B_PIN 7

#define SHIFT_IN_PIN_BASE GPIO_PORTC_BASE
#define SHIFT_IN_PIN GPIO_PIN_4

#define SHIFT_CLK_PIN_BASE GPIO_PORTC_BASE
#define SHIFT_CLK_PIN GPIO_PIN_5

#define SHIFT_REG_PERIPH_GPIO SYSCTL_PERIPH_GPIOC
#define SHIFT_REG_PINS SHIFT_IN_PIN | SHIFT_CLK_PIN
#define SHIFT_REG_BASE GPIO_PORTC_BASE

void set_shift_pin(int pin, int value);
void set_mode(int new_mode);


#endif // MODE_FUNCTIONS_H
