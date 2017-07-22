/*
 * Functions for Current Mode
 */
#include <stdint.h>
#include "mode_functions.h"
#include "utils/uartstdio.h"

void change_current(int current){
  if(current == 10){
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 0);
  }
  else if(current == 200){
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
  }
  else{
    UARTprintf("WARNING - UNKNOWN CURRENT LEVEL SETTING\n\r");
  }
  SysCtlDelay(10);
}

int check_current_range(float value, int range_current){

  int new_range = range_current;
  if( value < 0){
		value *= -1;
	}
  //UARTprintf("VALUE IS: %d\n\r", (int)value);
	if( range_current == 200){
		if( value >= 200){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
      change_current(200);
      new_range = 200;
      return 1;
		} else if( value < 9.8){
			UARTprintf("Switching to 10mA resolution\n");
      //Switch Down
      new_range = 10;
      change_current(10);
		}
	} else if ( range_current == 10){
		if( value >= 9.8){
			UARTprintf("Switching to 200mA resolution\n");
			new_range = 200;
			change_current(200);
		} else if( value < 10){
			//No worries
		}
	} else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return new_range;
}

double adjust_current_value(double value, int range){
  //adjust value
  double new_value = value;
  if(range == 200){
    new_value = value - value/10;
    if(value > 6 && value < 8){
      new_value = value + 2;
    }
  } else if(range == 10){
    new_value = value -= 3.8;
    if(value > 6){
      new_value = value + (value - 5.6) * 6;
    }
  }
  return new_value;
}
