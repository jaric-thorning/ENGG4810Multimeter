#include <stdbool.h>
#include <stdint.h>
#include "inc/hw_memmap.h"
#include "inc/hw_types.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "drivers/rgb.h"
#include "drivers/buttons.h"
#include "utils/uartstdio.h"
#include "led_task.h"
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"


#include "mswitch_task.h"
#include "lcd_task.h"

#include "stdlib.h"
//MSWITCH INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"


#define MSWITCHTASKSTACKSIZE        128

#define MSWITCH_ITEM_SIZE           sizeof(struct mswitch_queue_message)
#define MSWITCH_QUEUE_SIZE          5

#define MSWITCH_REFRESH_TIME 100

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

uint8_t shift_reg = 0x00;

int mode = 1; //0 -> Current, 1 -> Voltage, 2 -> Resistance
int range = 13; //V
int range_current = 200; //mA
int range_resistance = 1000; //kOhm

extern xSemaphoreHandle g_pUARTSemaphore;

void check_voltage_range(float value);
void set_shift_pin(int pin, int value);
void set_mode(char new_mode);
void change_voltage(int voltage);
void check_current_range(float value);
void check_resistance_range(float value);

static void
MSWITCHTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32MSWITCHRefreshTime;
    struct mswitch_queue_message mswitch_message;
    struct lcd_queue_message lcd_message;

    ui32MSWITCHRefreshTime = MSWITCH_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    int integer = 0;
    int decimal = 0;
    float value = 0;

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pMSWITCHQueue, &mswitch_message, 0) == pdPASS)
      {
        if(mode == 0){ //current
          value = mswitch_message.ui32Value/4095.0 * 2 * range_current - range_current;
          check_current_range(value);
          lcd_message.type = 'C';
          lcd_message.range = range_current;

        } else if(mode == 1){ //voltage

          value = mswitch_message.ui32Value/4095.0 * 2 * range - range;
          check_voltage_range(value);
          lcd_message.type = 'V';
          lcd_message.range = range;

    		} else if(mode == 2){ //resistance
          value = mswitch_message.ui32Value/4095.0 * 2 * range_resistance - range_resistance;
          check_resistance_range(value);
          lcd_message.type = 'R';
          lcd_message.range = range_resistance;
    		}

        integer = (int)value;
        decimal = ((int)(value*1000))%1000;
        if(decimal < 0){
          decimal *= -1;
        }

        lcd_message.value = integer;
        lcd_message.decimal = decimal;


        if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
           pdPASS){
             UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
           }

      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32MSWITCHRefreshTime / portTICK_RATE_MS);
    }
  }
}

uint32_t
MSWITCHTaskInit(void)
{

    g_pMSWITCHQueue = xQueueCreate(MSWITCH_QUEUE_SIZE, MSWITCH_ITEM_SIZE);


    SysCtlPeripheralEnable(SHIFT_REG_PERIPH_GPIO);

	  while(!SysCtlPeripheralReady(SHIFT_REG_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOOutput(SHIFT_REG_BASE, SHIFT_REG_PINS);


    //Set inital mode
    if(mode == 0){ //current mode
      set_mode('C');
  	} else if(mode == 1){ //votlage mode
      set_mode('V');
    } else if (mode == 2){ //resistance mode
      set_mode('R');
	   }

    if(xTaskCreate(MSWITCHTask, (signed portCHAR *)"MSWITCH", MSWITCHTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_MSWITCH_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("MSWITCH initiated...\n\r");

    //
    // Success.
    //
    return(0);
}

void set_shift_pin(int pin, int value){
  GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
  shift_reg ^= (-value ^ shift_reg) & (1 << pin);
  for(int i = 0; i < 8; i ++){
    if((shift_reg >> (7 - i)) & 1){
      GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, SHIFT_IN_PIN);
    } else{
      GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, 0);
    }

    GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, SHIFT_CLK_PIN);
    SysCtlDelay(5000);
    GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
  }
}

void set_mode(char new_mode){
  if(new_mode == 'V'){
    mode = 1;
    //write S1 to 12V mode (010)
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 1);
    set_shift_pin(S1_A_PIN, 0);

    //write S2 to 200mA (11) and IHN to 1 (off)
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
    set_shift_pin(S2_I_PIN, 1);

    //write S3 to voltage mode (01)
    set_shift_pin(S3_B_PIN, 0);
    set_shift_pin(S3_A_PIN, 1);

    return;

  } else if (new_mode == 'C'){
    mode = 0;
    //write S1 to 000
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 0);

    //write S2 to 200mA (11) and IHN to 0 (on)
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
    set_shift_pin(S2_I_PIN, 0);

    //write S3 to current mode (00)
    set_shift_pin(S3_B_PIN, 0);
    set_shift_pin(S3_A_PIN, 0);

  } else if (new_mode == 'R'){
    mode = 2;
    //write S1 to (000)
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 0);

    //write S2 to 1MOhm (11) and IHN to 0 (on)
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
    set_shift_pin(S2_I_PIN, 0);

    //write S3 to resistance mode (11)
    set_shift_pin(S3_B_PIN, 1);
    set_shift_pin(S3_A_PIN, 1);
  }
}

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
}

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
}

void check_voltage_range(float value){
	if( value < 0){
		value *= -1;
	}
	if( range == 13){
		if( value > 12){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all to 12V range
      change_voltage(12);
		} else if( value < 5){
			UARTprintf("Switching to 5V resolution\n");
			range = 5;
			//Switch Down
      change_voltage(5);
		}
	} else if ( range == 5){
		if( value >= 5){
			UARTprintf("Switching to 12V resolution\n");
			range = 13;
			change_voltage(12);

		} else if( value < 1){
			UARTprintf("Switching to 1V resolution\n");
			range = 1;
			change_voltage(1);
		}
	} else if (range == 1){
  		if( value >= 0.9){
  			UARTprintf("Switching to 5V resolution\n");
  			range = 5;
  			change_voltage(5);
		} else if( value < 1){
			//No worries
		}
	} else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}

void check_current_range(float value){
	if( value < 0){
		value *= -1;
	}
	if( range_current == 200){
		if( value > 200){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
      change_current(200);
		} else if( value < 10){
			UARTprintf("Switching to 10mA resolution\n");
      //Switch Down
      range_current = 10;
      change_current(10);
		}
	} else if ( range_current == 10){
		if( value >= 9){
			UARTprintf("Switching to 200mA resolution\n");
			range_current = 200;
			change_current(200);
		} else if( value < 10){
			//No worries
		}
	} else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}

void check_resistance_range(float value){
	if( value < 0){
		value *= -1;
	}
	if( range_resistance == 1000){
		if( value > 1000){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
      change_resistance(1000);
		} else if( value < 100){
			UARTprintf("Switching to 100k resolution\n");
      //Switch Down
      range_resistance = 100;
      change_resistance(100);
		}
	} else if ( range_resistance == 100){
    if( value > 100){
      UARTprintf("Switching to 1000k resolution\n");
			range_resistance = 1000;
      change_resistance(1000);
		} else if( value < 10){
			UARTprintf("Switching to 10k resolution\n");
      //Switch Down
      range_resistance = 10;
      change_resistance(10);
		}
	} else if ( range_resistance == 10){
    if( value > 10){
      UARTprintf("Switching to 100k resolution\n");
			range_resistance = 100;
      change_resistance(100);
		} else if( value < 1){
			UARTprintf("Switching to 1k resolution\n");
      //Switch Down
      range_resistance = 1;
      change_resistance(1);
		}
	} else if ( range_resistance == 1){
    if( value > 1){
      UARTprintf("Switching to 10k resolution\n");
			range_resistance = 10;
      change_resistance(10);
		} else if( value < 1){
			//No worries
		}
	}
  else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}
