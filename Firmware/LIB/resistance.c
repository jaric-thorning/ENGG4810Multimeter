/*
 * Functions for Voltage Mode
 */
#include <stdint.h>
#include "mode_functions.h"
#include "utils/uartstdio.h"

void change_resistance(int resistance){
   if(resistance == 1){
     set_shift_pin(S2_B_PIN, 0);
     set_shift_pin(S2_A_PIN, 0);
   } else if(resistance == 10){
     set_shift_pin(S2_B_PIN, 0);
     set_shift_pin(S2_A_PIN, 1);
   } else if(resistance == 100){
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 0);
   } else if(resistance == 1000){
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 1);
   } else{
     UARTprintf("WARNING - UNKNOWN RESISTANCE LEVEL SETTING\n\r");
   }
   SysCtlDelay(10);
}


int check_resistance_range(float value, int range_resistance){
  UARTprintf("Recieved resistance value: %d.%d\n\r", (int)value, (int)(((int)(value * 1000))%1000));
  int new_range = range_resistance;
	if( value < 0){
		value *= -1;
	}
	if( range_resistance == 1000){
		if( value > 1000){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
		} else if( value < 100){
			UARTprintf("Switching 1000k to 100k resolution\n");
      //Switch Down
      new_range = 100;
		}
	} else if ( range_resistance == 100){
    if( value > 100){
      UARTprintf("Switching 100k to 1000k resolution\n");
			new_range = 1000;
		} else if( value < 10){
			UARTprintf("Switching 100k to 10k resolution\n");
      //Switch Down
      new_range = 10;
		}
	} else if ( range_resistance == 10){
    if( value > 10){
      UARTprintf("Switching 10k to 100k resolution\n");
			new_range = 100;
		} else if( value < 1){
			UARTprintf("Switching 10k to 1k resolution\n");
      //Switch Down
      new_range = 1;
		}
	} else if ( range_resistance == 1){
    if( value > 1){
      UARTprintf("Switching 1k to 10k resolution\n");
			new_range = 10;
		} else if( value < 1){
			/*zero_count++;
      if(zero_count > 10){
        UARTprintf("Switching to 1000k resolution\n");
        change_resistance(1000);
        range_resistance = 1000;
        zero_count = 0;
      }*/
		}
	}
  else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
  change_resistance(new_range);
	return new_range;
}

double adjust_resistance_value(double value, int range_resistance){
  double new_value = value;
  if(range_resistance == 1){
    new_value = value * 47/36.0;
  } else if (range_resistance == 10){
  } else if (range_resistance == 100){
    new_value = value/10;
  } else if(range_resistance == 1000){
    new_value = value * 2;
  }
  return new_value;
}
