#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "inc/hw_memmap.h"
#include "inc/hw_types.h"

#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "driverlib/pin_map.h"

#include "drivers/rgb.h"
#include "drivers/buttons.h"

#include "utils/uartstdio.h"

//BGet Include
#include "bget.h"

//FreeRTOS Includes
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"

// Task Includes
#include "ADC_task.h"
#include "sd_task.h"
#include "led_task.h"
#include "mswitch_task.h"
#include "lcd_task.h"
#include "switch_task.h"
#include "buzzer_task.h"

//LIB Includes
#include "LIB/mswitch_helper.h"
#include "LIB/sd_card.h"
#include "LIB/mode_functions.h"
#include "LIB/voltage.h"
#include "LIB/current.h"
#include "LIB/resistance.h"
#include "display.h"
#include "general_functions.h"


#define MSWITCHTASKSTACKSIZE        512

#define MSWITCH_ITEM_SIZE           sizeof(struct mswitch_queue_message)
#define MSWITCH_QUEUE_SIZE          5

#define MSWITCH_REFRESH_TIME 1

#define DC_VOLTAGE 0
#define AC_VOLTAGE 1
#define DC_CURRENT 2
#define AC_CURRENT 3
#define RESISTANCE 4
#define LOGIC      5
#define CONTINUITY 6

#define NUM_MODES 7

#define ADJUSTMENT 0



int mode = DC_VOLTAGE; //0 -> Current, 1 -> Voltage, 2 -> Resistance
int range = 13; //V
int range_current = 200; //mA
int range_resistance = 1000; //kOhm

int zero_count = 0;
int reset_resister = 0;

extern xSemaphoreHandle g_pUARTSemaphore;


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
    double prev_tested_value = 0.5;
    int contiuous = 0;
    int prev_tested_resistance = range_resistance;

    int logging = 0;
    int ledison = 1;
    int flash_reset = 1;

    int filename_count = 0;

    int last_res_range_change = xTaskGetTickCount();

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pMSWITCHQueue, &mswitch_message, 0) == pdPASS)
      {
        //UARTprintf("MSWITCH RECIEVED TYPE: %c\n\r", mswitch_message.type);
        if(mswitch_message.type == 'M'){

          mode = next_mode(mode, mswitch_message.mode);

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

          logging = trigger_sd_logging(logging, &sd_message, filename_count);

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

            value = mswitch_message.value * range; //convert to value
            range = check_voltage_range(value, mode, range); // update range
            value = mswitch_message.value * range; //re-evalutate value

            if(value > 12){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }

            integer = (int)(mswitch_message.value * range);
            decimal = ((int)((mswitch_message.value * range)*1000000))%1000000;

            lcd_message.type = 'V';
            lcd_message.range = range;

          } else if (mode == AC_VOLTAGE){
            value = mswitch_message.value;
            lcd_message.type = 'W';
            lcd_message.range = 12;
            if(value < 0){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }
            //UARTprintf("VALUE: %d.%d\n\r", (int) value, (int)((int)value * 1000)%1000);
          } else if(mode == DC_CURRENT){ //current

            value = mswitch_message.value * range_current; //convert to value
            range_current = check_current_range(value, range_current); // update range
            value = mswitch_message.value * range_current; //re-evalutate value
            value = adjust_current_value(value, range_current); //adjust value to correct

            if(value > 200){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }

            integer = (int)(mswitch_message.value * range);
            decimal = ((int)((mswitch_message.value * range)*1000000))%1000000;

            lcd_message.type = 'I';
            lcd_message.range = range_current;

          } else if (mode == AC_CURRENT){
            //TODO CHANGE THIS
            value = mswitch_message.value/12.0 * 200;
            lcd_message.range = 200;
            lcd_message.type = 'J';
            if(value < 0){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }
            //lcd_message.type = 'J';

      		} else if(mode == RESISTANCE){ //resistance



            if(mswitch_message.value > 1){
              if(prev_tested_resistance == range_resistance){
                if(prev_tested_value == 500){
                  prev_tested_value = 0.5;
                } else{
                  prev_tested_value *= 10;
                }
              }
              prev_tested_resistance = range_resistance;
              range_resistance = check_resistance_range(prev_tested_value, range_resistance);
            } else{
              value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/5664672.0 * range_resistance;  //convert to value
              //value = adjust_resistance_value(value, range_resistance);
               // update range

              if(last_res_range_change + 5000 < xTaskGetTickCount()){
                int current_range = range_resistance;
                range_resistance = check_resistance_range(value, range_resistance);
                if(current_range != range_resistance){
                  last_res_range_change = xTaskGetTickCount();
                }
              }

              value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/5664672.0 * range_resistance;  //convert to value
              value = adjust_resistance_value(value, range_resistance);

            }

            if(value > 1000 || mswitch_message.value > 1){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }

            int integer2 = (int)mswitch_message.value;
            int decimal2 = ((int)(((int)(mswitch_message.value*1000))%1000));

            UARTprintf("Value: %d.%d\n\r", integer2, decimal2);


            lcd_message.type = 'R';
            lcd_message.range = range_resistance;

      		} else if (mode == CONTINUITY){

            /*if(mswitch_message.value > 1){
              if(prev_tested_value == 500){
                prev_tested_value = 0.5;
              } else{
                prev_tested_value *= 10;
              }
              range_resistance = check_resistance_range(prev_tested_value, range_resistance);
              contiuous = 1;
            } else{*/
              value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/5664672.0 * range_resistance;  //convert to value
              //value = adjust_resistance_value(value, range_resistance);
               // update range

              if(last_res_range_change + 500 < xTaskGetTickCount()){
                int current_range = range_resistance;
                range_resistance = check_resistance_range(value, range_resistance);
                if(current_range != range_resistance){
                  last_res_range_change = xTaskGetTickCount();
                }
              }

              value = ((mswitch_message.value * 5664672.0 + 5664672.0)/2.0)/5664672.0 * range_resistance;  //convert to value
              value = adjust_resistance_value(value, range_resistance);
              contiuous = 0;
            //}

              lcd_message.overlimit = 0;
              lcd_message.type = 'C';
              lcd_message.range = range_resistance;

          } else if (mode == LOGIC){
            value = mswitch_message.value * range; //convert to value
            range = check_voltage_range(value, mode, range); // update range
            value = mswitch_message.value * range; //re-evalutate value

            if(value > 12){
              lcd_message.overlimit = 1;
            } else{
              lcd_message.overlimit = 0;
            }

            integer = (int)(mswitch_message.value * range);
            decimal = ((int)((mswitch_message.value * range)*1000000))%1000000;

            lcd_message.type = 'L';
            lcd_message.range = range;

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
            buzzer_message.frequency = 0;
            buzzer_message.sound = 0;
          }

        } else if (mode == CONTINUITY){
          if(value > 1000){
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

        if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
          pdPASS){
            UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
         }

        if( xSemaphoreTake(g_pUARTSemaphore,portMAX_DELAY) == pdTRUE )
        {
            record_to_sd(logging, integer, decimal, lcd_message.type, &sd_message);
        }
        xSemaphoreGive(g_pUARTSemaphore);
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
