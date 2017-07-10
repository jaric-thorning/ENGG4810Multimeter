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

#include <string.h>

#include "mswitch_task.h"
#include "lcd_task.h"

//#include "xxx.h"

#include "general_functions.h"
//MSWITCH INCLUDES

#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"
#include "sd_task.h"

#include "ADC_task.h"

#include "bget.h"

#include "switch_task.h"
#include "buzzer_task.h"
#include "sd_card.h"


#define MSWITCHTASKSTACKSIZE        512

#define MSWITCH_ITEM_SIZE           sizeof(struct mswitch_queue_message)
#define MSWITCH_QUEUE_SIZE          5

#define MSWITCH_REFRESH_TIME 10

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

#define DC_VOLTAGE 0
#define AC_VOLTAGE 1
#define DC_CURRENT 2
#define AC_CURRENT 3
#define RESISTANCE 4
#define LOGIC      5
#define CONTINUITY 6

#define NUM_MODES 7

#define ADJUSTMENT 0

uint8_t shift_reg = 0x00;

int mode = DC_VOLTAGE; //0 -> Current, 1 -> Voltage, 2 -> Resistance
int range = 13; //V
int range_current = 200; //mA
int range_resistance = 1000; //kOhm

int zero_count = 0;
int reset_resister = 0;

extern xSemaphoreHandle g_pUARTSemaphore;

int check_voltage_range(float value, int mode);
void set_shift_pin(int pin, int value);
void set_mode(int new_mode);
void change_voltage(int voltage);
int check_current_range(float value);
int check_resistance_range(float value);

static void
MSWITCHTask(void *pvParameters)
{
    portTickType ui32WakeTime, lastledflash, value_acquired;
    uint32_t ui32MSWITCHRefreshTime;
    struct mswitch_queue_message mswitch_message;
    struct lcd_queue_message lcd_message;
    struct sd_queue_message sd_message;
    struct adc_queue_message adc_message;
    struct buzzer_queue_message buzzer_message;
    struct switch_queue_message switch_message;

    sd_message.filename = (char*)bgetz(64 * 2 * sizeof(char));
    sd_message.text = (char*)bgetz(64 * 2 * sizeof(char));


    //sd_message.filename  = "ltest.txt";


    ui32MSWITCHRefreshTime = MSWITCH_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();
    value_acquired = 0;
    lastledflash = 0;
    double integer = 0;
    double decimal = 0;
    double value = 0;
    double convert = 0;

    int logging = 0;
    int ledison = 1;
    int led2ison = 0;
    int flash_reset = 1;

    int filename_count = 0;

    char sd_write_line[64];

    char integer_buf[10];
    char decimal_buf[10];

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pMSWITCHQueue, &mswitch_message, 0) == pdPASS)
      {
        //UARTprintf("MSWITCH RECIEVED TYPE: %c\n\r", mswitch_message.type);
        if(mswitch_message.type == 'M'){

          if(mswitch_message.mode == 'V'){

            mode = DC_VOLTAGE;
          } else if(mswitch_message.mode == 'W'){
            mode = AC_VOLTAGE;
          } else if(mswitch_message.mode == 'I'){
            mode = DC_CURRENT;
          } else if(mswitch_message.mode == 'J'){
            mode = AC_CURRENT;
          } else if(mswitch_message.mode == 'R'){
            mode = RESISTANCE;
          } else if(mswitch_message.mode == 'C'){
            mode = CONTINUITY;
          } else if(mswitch_message.mode == 'L'){
            mode = LOGIC;
          } else if(mswitch_message.mode == 'U'){
            mode = (mode + 1) % NUM_MODES;
          }
          else if(mswitch_message.mode == 'D'){
            mode = (mode - 1) % NUM_MODES;
          } else{
            UARTprintf("WARNING - ATTEMPTED TO SWITCH TO UNKNOWN MODE\n\r");
          }

          if(mode == AC_VOLTAGE || mode == AC_CURRENT){
            adc_message.mode = 'R';
          } else{
            adc_message.mode = 'N';
          }

          if(xQueueSend(g_pADCQueue, &adc_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
             }

          set_mode(mode);
          UARTprintf("Switching to mode: %d\n\r", mode);


        } else if(mswitch_message.type == 'R'){
          if(logging == 0){
            UARTprintf("Recording to SD....\n\r");
            logging = 1;

            char temp_filename[64];
            char temp_type[2];
            char counter_buf[10];

            do {
              strcpy(temp_filename,"logfile\0");
              int2str(filename_count++, counter_buf, 10);
              strcat(temp_filename, counter_buf);
              strcat(temp_filename, ".csv\0");

            } while(check_filename(temp_filename));

            sd_message.filename = temp_filename;
            static char header_text[64];
            strcpy(header_text, "Time,Value,Units,IsoTime\n\0");
            sd_message.text = header_text;
            UARTprintf("Found valid filname: %s\n\r", sd_message.filename);
            if(xQueueSend(g_pSDQueue, &sd_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
               }

          } else{
            UARTprintf("Recording done.\n\r");
            logging = 0;
          }

        } else if(mswitch_message.type == 'V'){
          //UARTprintf("ADC 1 : %d\n\r", mswitch_message.ui32Value);
          if(flash_reset){
            value_acquired = xTaskGetTickCount();
            flash_reset = 0;
          }
          switch_message.led2 = 1;
          switch_message.setting = 'X';
          switch_message.setled2 = 1;
          switch_message.setled1 = 0;

          if(xQueueSend(g_pSWITCHQueue, &switch_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
             }

          if(mode == DC_VOLTAGE){ //voltage

            value = mswitch_message.value * range;

            int integer = (int)mswitch_message.value;
            int decimal = ((int)(mswitch_message.value*1000000))%1000000;
            //UARTprintf("AD2: %d.%d\n\n\r", integer, decimal);

            integer = (int)value;
            decimal = ((int)(value*1000000))%1000000;
            //UARTprintf("AD3: %d.%d\n\n\r", integer, decimal);

            integer = (int)(mswitch_message.value * range);
            decimal = ((int)((mswitch_message.value * range)*1000000))%1000000;
            //UARTprintf("AD4: %d.%d\n\n\r", integer, decimal);

            //UARTprintf("Value: %d.%d\n\r", (int)value, ((int)value * 1000)%1000);
            lcd_message.type = 'V';
            lcd_message.range = range;
            //lcd_message.overlimit = check_voltage_range(0.8, mode);
            lcd_message.overlimit = check_voltage_range(value, mode);
          } else if (mode == AC_VOLTAGE){
            //TODO CHANGE THIS
            lcd_message.type = 'W';
            value = mswitch_message.value * range * 2 - range;
            float max_range = mswitch_message.max_value/3.3 * range * 2 - range;

            lcd_message.overlimit = check_voltage_range(max_range, mode);

          } else if(mode == DC_CURRENT){ //current

            value = mswitch_message.value * 2 * range_current - range_current;

            if(range_current == 200){
              value -= 5;
              if(value > 6 && value < 8){
                value += 2;
              }
            } else if(range_current == 10){
              value -= 3.8;
              if(value > 6){
                value += (value - 5.6) * 6;
              }
            }
            //UARTprintf("Recieved uValue = %d", mswitch_message.ui32Value);
            //UARTprintf("    Current range = %d\n\r", range_current);
            lcd_message.type = 'I';
            lcd_message.range = range_current;
            lcd_message.overlimit = check_current_range(value);
          } else if (mode == AC_CURRENT){
            //TODO CHANGE THIS
            lcd_message.type = 'J';

      		} else if(mode == RESISTANCE){ //resistance
            if(range_resistance == 1000){
              value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/2715133.0 * range_resistance;

              if(value < 200){
                value -= 10;
              }
            } else if (range_resistance == 100){
              value = (((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/2715133.0)/2 * range_resistance;
            } else if (range_resistance == 10){
              value = (((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/2715133.0)/2 * range_resistance;
            }

            if(value > 2 * range_resistance){
              value = 0;
              lcd_message.overlimit = check_resistance_range(50);
            } else {
              lcd_message.overlimit = check_resistance_range(value);
            }

            lcd_message.range = range_resistance;
            lcd_message.type = 'R';

      		} else if (mode == CONTINUITY){
            value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/2715133.0 * range_resistance;
            lcd_message.type = 'C';
            lcd_message.overlimit = check_resistance_range(value);

          } else if (mode == LOGIC){
            value = mswitch_message.value * range * 2 - range;
            lcd_message.type = 'L';
            lcd_message.overlimit = check_voltage_range(value, mode);
          }

        //Adjust displayed range
        if(lcd_message.range == 13){
          lcd_message.range = 12;
        }

        integer = (int)value;
        decimal = ((int)(value*100000)%100000);
        if(decimal < 0){
          decimal *= -1;
          lcd_message.negative_value = 1;
        } else{
          lcd_message.negative_value = 0;
        }
        if(mode != LOGIC && mode != CONTINUITY){
          lcd_message.value = integer;
          lcd_message.decimal = decimal;
          buzzer_message.sound = 0;
        } else if (mode == LOGIC){
          if(value > 0.6){
            lcd_message.value = 1;
            lcd_message.decimal = 0;
            buzzer_message.frequency = 1;
            buzzer_message.sound = 1;
          } else if (value > -1){
            lcd_message.value = 0;
            lcd_message.decimal = 0;
            buzzer_message.frequency = 5;
            buzzer_message.sound = 1;
          } else{
            lcd_message.value = 0;
            lcd_message.decimal = 0;
            lcd_message.overlimit = 1;
            buzzer_message.sound = 0;
          }

        } else if (mode == CONTINUITY){
          if(value < 5){
            lcd_message.value = 1;
            lcd_message.decimal = 0;
            buzzer_message.sound = 1;
          } else{
            lcd_message.value = 0;
            lcd_message.decimal = 0;
            buzzer_message.sound = 0;
          }
          buzzer_message.frequency = 1;

         }

         if(xQueueSend(g_pBuzzerQueue, &buzzer_message, portMAX_DELAY) !=
            pdPASS){
              UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
            }

        memset(decimal_buf, 0, 10);
        memset(integer_buf, 0, 10);
        memset(sd_write_line, 0, 64);
        //memset(sd_message.text, 0, sizeof(sd_message.text));
        //sd_message.text = "TEST";
        int ticks_seconds = xTaskGetTickCount();
        int2str(ticks_seconds / 1000, integer_buf, 10);
        int2str(ticks_seconds % 1000, decimal_buf, 10);

        strncpy(sd_write_line, integer_buf, 10);
        strcat(sd_write_line, ".");
        strcat(sd_write_line, decimal_buf);
        strcat(sd_write_line, ",");

        if(integer < 0){
          integer *= -1;
          strcat(sd_write_line, "-");
        }
        int2str(integer, integer_buf, 10);
        int2str(decimal, decimal_buf, 10);

        strcat(sd_write_line, integer_buf);
        strcat(sd_write_line, ".");
        strcat(sd_write_line, decimal_buf);
        strcat(sd_write_line, ",");

        if(lcd_message.type == 'V'){
          strcat(sd_write_line, "V,");
        } else if (lcd_message.type == 'C'){
          strcat(sd_write_line, "C,");
        } else if (lcd_message.type == 'R'){
          strcat(sd_write_line, "R,");
        } else{
          strcat(sd_write_line, "U,");
        }
        strcat(sd_write_line, "N\n");

        if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
           pdPASS){
             UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
           }

        if(logging){
          //UARTprintf("Logging...\n\r");
          if(xTaskGetTickCount() > lastledflash + 250){
            if(ledison){
              switch_message.led1 = 0;
              ledison = 0;
            }else{
              switch_message.led1 = 1;
              ledison = 1;
            }
            switch_message.setting = 'X';
            switch_message.setled2 = 0;
            switch_message.setled1 = 1;

            if(xQueueSend(g_pSWITCHQueue, &switch_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
               }
            lastledflash = xTaskGetTickCount();
          }

          /*UARTprintf("BUILT: ");
          for(int i = 0; i < 64; i++){
            UARTprintf("%c", sd_write_line[i]);
          }
          UARTprintf("\n\r");*/

          sd_message.text = sd_write_line;
          if(xQueueSend(g_pSDQueue, &sd_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
             }
        }
      }
    }
    if(xTaskGetTickCount() > (value_acquired + 10)){
      flash_reset = 1;
      switch_message.led2 = 0;
      switch_message.setting = 'X';
      switch_message.setled2 = 1;
      switch_message.setled1 = 0;
      if(xQueueSend(g_pSWITCHQueue, &switch_message, portMAX_DELAY) !=
         pdPASS){
           UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
         }
    }
      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32MSWITCHRefreshTime / portTICK_RATE_MS);
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
    set_mode(mode);


    if(xTaskCreate(MSWITCHTask, (signed portCHAR *)"MSWITCH",
          MSWITCHTASKSTACKSIZE, NULL, tskIDLE_PRIORITY + PRIORITY_MSWITCH_TASK,
          NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("    MSwitch initiated.\n\r");

    //
    // Success.
    //
    return(0);
}

void set_shift_pin(int pin, int value){
  GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
  shift_reg ^= (-value ^ shift_reg) & (1 << pin);
  //UARTprintf("Shift Register: ");
  for(int i = 0; i < 8; i ++){
    if((shift_reg >> (7 - i)) & 1){

      GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, SHIFT_IN_PIN);
    } else{
      GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, 0);
    }
    //UARTprintf("%d",(shift_reg >> i) & 1);
    GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, SHIFT_CLK_PIN);
    SysCtlDelay(5000);
    GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
  }
  //UARTprintf("\n\r");
}

void set_mode(int new_mode){
  //mode = new_mode;
  if(new_mode == DC_VOLTAGE){
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

  } else if (new_mode == AC_VOLTAGE){
    set_shift_pin(S1_C_PIN, 1);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 1);

    //write S2 to 200mA (11) and IHN to 1 (off)
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
    set_shift_pin(S2_I_PIN, 1);

    //write S3 to voltage mode (01)
    set_shift_pin(S3_B_PIN, 0);
    set_shift_pin(S3_A_PIN, 1);
  } else if ((new_mode == DC_CURRENT)|| (new_mode == AC_CURRENT)){
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

  } else if (new_mode == RESISTANCE){
    //write S1 to (000)
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 0);
    set_shift_pin(S1_A_PIN, 0);

    //write S2 to 1MOhm (11) and IHN to 0 (on)
    set_shift_pin(S2_B_PIN, 0);
    set_shift_pin(S2_A_PIN, 0);
    set_shift_pin(S2_I_PIN, 0);

    //write S3 to resistance mode (10)
    set_shift_pin(S3_B_PIN, 1);
    set_shift_pin(S3_A_PIN, 0);
  } else if (new_mode == LOGIC){

    //write S1 to 12V mode (010)
    set_shift_pin(S1_C_PIN, 0);
    set_shift_pin(S1_B_PIN, 1);
    set_shift_pin(S1_A_PIN, 0);

    //write S2 to 200mA (11) and IHN to 1 (off)
    set_shift_pin(S2_B_PIN, 1);
    set_shift_pin(S2_A_PIN, 1);
    set_shift_pin(S2_I_PIN, 1);

    //write S3 to voltage mode (01)
    set_shift_pin(S3_B_PIN, 1);
    set_shift_pin(S3_A_PIN, 1);
  }
  return;
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
  xQueueReset(g_pMSWITCHQueue);
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
  xQueueReset(g_pMSWITCHQueue);
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
  SysCtlDelay(10);
  xQueueReset(g_pMSWITCHQueue);
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
  SysCtlDelay(10);
  xQueueReset(g_pMSWITCHQueue);
}

int check_voltage_range(float value, int mode){
	if( value < 0){
		value *= -1;
	}
	if( range == 13){
		if( value > 12){
			//UARTprintf("Warning: Value out of range!\n");
			//Reset all to 12V range
      if(mode == AC_VOLTAGE){
        change_ac_voltage(12);
      } else{
        change_voltage(12);
      }
      return 1;
		} else if( value < 4.9){
			//UARTprintf("Switching to 5V resolution\n");
			range = 5;
			//Switch Down
      if(mode == AC_VOLTAGE){
        change_ac_voltage(5);
      } else{
        change_voltage(5);
      }
		}
	} else if ( range == 5){
		if( value >= 4.98){
			//UARTprintf("Switching to 12V resolution\n");
			range = 13;
      if(mode == AC_VOLTAGE){
        change_ac_voltage(12);
      } else{
			  change_voltage(12);
      }

		} else if( value < 0.95){
			//UARTprintf("Switching to 1V resolution\n");
			range = 1;
      if(mode == AC_VOLTAGE){
        change_ac_voltage(1);
      } else{
			  change_voltage(1);
      }
		}
	} else if (range == 1){
  		if( value >= 0.99){

  			//UARTprintf("Switching to 5V resolution\n");
  			range = 5;
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
	return 0;
}

int check_current_range(float value){
	if( value < 0){
		value *= -1;
	}
  //UARTprintf("VALUE IS: %d\n\r", (int)value);
	if( range_current == 200){
		if( value >= 200){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
      change_current(200);
      range_current = 200;
      return 1;
		} else if( value < 9.8){
			UARTprintf("Switching to 10mA resolution\n");
      //Switch Down
      range_current = 10;
      change_current(10);
		}
	} else if ( range_current == 10){
		if( value >= 9.8){
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
	return 0;
}

int check_resistance_range(float value){
	if( value < 0){
		value *= -1;
	}
	if( range_resistance == 1000){
		if( value > 1000){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
      change_resistance(1000);
      return 1;
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
    if( value > 10){
      UARTprintf("Switching to 100k resolution\n");
			range_resistance = 100;
      change_resistance(100);
    } else if( value > 1){
      UARTprintf("Switching to 10k resolution\n");
			range_resistance = 10;
      change_resistance(10);
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
	return 0;
}
