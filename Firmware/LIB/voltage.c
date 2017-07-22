/*
 * Functions for Voltage Mode
 */
#include <stdint.h>
#include "mode_functions.h"
#include "utils/uartstdio.h"


void change_voltage(int voltage){
  if(voltage == 12){
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 1);
    set_shift_pin(S1_A_PIN, 0);
  }
  else if(voltage == 5){
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 1);
  }
  else if(voltage == 1){
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 0);
  }
  else{
    UARTprintf("WARNING - UNKNOWN VOLTAGE LEVEL SETTING\n\r");
  }
}

void change_ac_voltage(int voltage){
  if(voltage == 12){
    set_shift_pin(S1_C_PIN, 1);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 1);
  }
  else if(voltage == 5){
    set_shift_pin(S1_C_PIN, 1);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 0);
  }
  else if(voltage == 1){
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 1);
    set_shift_pin(S1_A_PIN, 1);
  }
  else{
    UARTprintf("WARNING - UNKNOWN VOLTAGE LEVEL SETTING\n\r");
  }
  SysCtlDelay(10);
}

int check_voltage_range(float value, int mode, int current_range){
  int new_range = current_range;
	if( value < 0){
		value *= -1;
	}
	if( current_range == 13){
		if( value > 12){
			//UARTprintf("Warning: Value out of range!\n");
			//Reset all to 12V range
      if(mode == AC_VOLTAGE){
        change_ac_voltage(12);
      } else{
        change_voltage(12);
      }
      new_range = 13;
		} else if( value < 4.9){
			//UARTprintf("Switching to 5V resolution\n");
			new_range = 5;
			//Switch Down
      if(mode == AC_VOLTAGE){
        change_ac_voltage(5);
      } else{
        change_voltage(5);
      }
		}
	} else if ( current_range == 5){
		if( value >= 4.98){
			//UARTprintf("Switching to 12V resolution\n");
			new_range = 13;
      if(mode == AC_VOLTAGE){
        change_ac_voltage(12);
      } else{
			  change_voltage(12);
      }

		} else if( value < 0.95){
			//UARTprintf("Switching to 1V resolution\n");
			new_range = 1;
      if(mode == AC_VOLTAGE){
        change_ac_voltage(1);
      } else{
			  change_voltage(1);
      }
		}
	} else if (current_range == 1){
  		if( value >= 0.99){

  			//UARTprintf("Switching to 5V resolution\n");
  			new_range = 5;
        if(mode == AC_VOLTAGE){
          change_ac_voltage(5);
        } else{
  			  change_voltage(5);
        }
		} else if( value < 1){
			//No worries
		}
	} else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return new_range;
}
