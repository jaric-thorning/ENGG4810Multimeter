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


#include "ADC_task.h"

#include "stdlib.h"
//ADC INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
#include "driverlib/adc.h"

#include "display.h"

#include "LCD_task.h"

#define ADCTASKSTACKSIZE        128

#define ADC_ITEM_SIZE           sizeof(struct ADC_queue_message)
#define ADC_QUEUE_SIZE          5

#define ADC_REFRESH_TIME 100

uint32_t ui32Value;

extern xSemaphoreHandle g_pUARTSemaphore;

static void
ADCTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32ADCRefreshTime;

    ui32ADCRefreshTime = ADC_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    struct lcd_queue_message lcd_message;

    int integer = 0;
	  int decimal = 0;
	  float value = 0;
    int range = 5;

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      /*if(xQueueReceive(g_pADCQueue, &adc_message2, 0) == pdPASS)
      {
        xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);

        UARTprintf("%c: (+- %d) %d.%d\n\r", adc_message2.type, adc_message2.range, adc_message2.value, adc_message2.decimal);

        display(adc_message2.type, adc_message2.range, adc_message2.value, adc_message2.decimal);
        xSemaphoreGive(g_pUARTSemaphore);
      }*/

      ADCProcessorTrigger(ADC0_BASE, 0);
  		while(!ADCIntStatus(ADC0_BASE, 0, false))
  		{
  		}
  		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

  		value = ui32Value/4095.0 * 2 * range - range;

  		integer = (int)value;
  		decimal = ((int)(value*1000))%1000;
  		if(decimal < 0){
  			decimal *= -1;
  		}

      lcd_message.type = 'V';
      lcd_message.range = range;
      lcd_message.value = integer;
      lcd_message.decimal = decimal;

      if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
         pdPASS){
           UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
         }

      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32ADCRefreshTime / portTICK_RATE_MS);
    }
}

uint32_t
ADCTaskInit(void)
{
    //g_pADCQueue = xQueueCreate(ADC_QUEUE_SIZE, ADC_ITEM_SIZE);

  	//
  	// Enable the ADC0 module.
  	//
  	SysCtlPeripheralEnable(SYSCTL_PERIPH_ADC0);
  	//
  	// Wait for the ADC0 module to be ready.
  	//
  	while(!SysCtlPeripheralReady(SYSCTL_PERIPH_ADC0))
  	{
  	}
  	//
  	// Enable the first sample sequencer to capture the value of channel 0 when
  	// the processor trigger occurs.
  	//
  	ADCSequenceConfigure(ADC0_BASE, 0, ADC_TRIGGER_PROCESSOR, 0);
  	ADCSequenceStepConfigure(ADC0_BASE, 0, 0,
  							 ADC_CTL_IE | ADC_CTL_END | ADC_CTL_CH0);
  	ADCSequenceEnable(ADC0_BASE, 0);
  	//
  	// Trigger the sample sequence.
  	//
  	ADCProcessorTrigger(ADC0_BASE, 0);
  	//
  	// Wait until the sample sequence has completed.
  	//
  	while(!ADCIntStatus(ADC0_BASE, 0, false))
  	{
  	}
  	//
  	// Read the value from the ADC.
  	//
  	ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

    if(xTaskCreate(ADCTask, (signed portCHAR *)"ADC", ADCTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_ADC_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("ADC initiated...\n\r");

    //
    // Success.
    //
    return(0);
}
