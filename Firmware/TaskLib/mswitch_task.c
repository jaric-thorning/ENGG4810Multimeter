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

#define MSWITCH_REFRESH_TIME 10


#define CONTROL_A GPIO_PIN_4
#define CONTROL_B GPIO_PIN_6
#define CONTROL_C GPIO_PIN_7
#define CONTROL_D GPIO_PIN_5
#define ALL_CONTROL_PINS CONTROL_A | CONTROL_B | CONTROL_C | CONTROL_D

#define CONTROL_13_V CONTROL_A
#define CONTROL_5_V CONTROL_B
#define CONTROL_1_V CONTROL_C
#define CONTROL_AMP CONTROL_D

#define CONTROL_10_mA CONTROL_1_V | CONTROL_AMP
#define CONTROL_200_mA CONTROL_1_V


int mode = 0; //0 -> Voltage, 1 -> Current
int range = 13;
int range_current = 200;

extern xSemaphoreHandle g_pUARTSemaphore;

void check_range(float value);
void check_current(float value);


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
        //xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);

        //UARTprintf("%c: (+- %d) %d.%d\n\r", mswitch_message.type, mswitch_message.range, mswitch_message.value, mswitch_message.decimal);

        //display(mswitch_message.type, mswitch_message.range, mswitch_message.value, mswitch_message.decimal);
        //xSemaphoreGive(g_pUARTSemaphore);

        value = mswitch_message.ui32Value/4095.0 * 2 * range - range;

        integer = (int)value;
    		decimal = ((int)(value*1000))%1000;
    		if(decimal < 0){
    			decimal *= -1;
    		}

        if(mode == 0){
    			check_range(value);
    			//UARTprintf("ADC: %d.%d\n", (int)(ui32Value/4095.0 * 3.3), ((int)(ui32Value/4095.0 * 3.3 *1000))%1000);
    			//UARTprintf("Voltage : %d.%d Range: %d\n", integer, decimal, range);
          lcd_message.type = 'V';
          lcd_message.range = range;
          lcd_message.value = integer;
          lcd_message.decimal = decimal;
    		}
    		else{
    			/*check_current(value_current);
    			UARTprintf("ADC: %d.%d\n", (int)(ui32Value/4095.0 * 3.3), ((int)(ui32Value/4095.0 * 3.3 *1000))%1000);
    			UARTprintf("Current : %d.%d Range: %d\n", integer_current, decimal_current, range_current);
    			display("current",range_current, integer_current, decimal_current);*/
    		}



        if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
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

    //
	  // Enable the GPIO port that is used for the on-board LED.
	  //
	  SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOC);

    //
	  // Check if the peripheral access is enabled.
	  //
	  while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOC))
	  {
	  }

    GPIOPinTypeGPIOOutput(GPIO_PORTC_BASE, ALL_CONTROL_PINS);

    if(mode == 0){
  		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, CONTROL_A);
  		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_D, 0x0);
  	}
  	else{
  		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);
  		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, 0x0);
  		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_B, 0x0);
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

void check_range(float value){
	if( value < 0){
		value *= -1;
	}

	if( range == 13){
		if( value > 12){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, 0x0);
			//Enable 13V
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, CONTROL_13_V);
		}
		else if( value < 5){
			UARTprintf("Switching to 5V resolution\n");
			range = 5;
			//Switch Down

			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, CONTROL_5_V);
		}
	}
	else if ( range == 5){
		if( value >= 5){
			UARTprintf("Switching to 12V resolution\n");
			range = 13;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, CONTROL_13_V);

		}
		else if( value < 1){
			UARTprintf("Switching to 1V resolution\n");
			range = 1;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, CONTROL_1_V);

		}

	}
	else if (range == 1){
		if( value >= 0.9){
			UARTprintf("Switching to 5V resolution\n");
			range = 5;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, CONTROL_5_V);

		}
		else if( value < 1){
			//No worries
		}
	}
	else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}

void check_current(float value){
	if( value < 0){
		value *= -1;
	}

	if( range_current == 200){
		if( value > 200){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, 0x0);
			//Enable 200mA
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);
		}
		else if( value < 10){
			UARTprintf("Switching to 10mA resolution\n");
			range_current = 10;
			//Switch Down

			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, CONTROL_10_mA);
		}
	}
	else if ( range_current == 10){
		if( value >= 9){
			UARTprintf("Switching to 200mA resolution\n");
			range_current = 200;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);

		}
		else if( value < 10){
			//No worries
		}
	}
	else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}
