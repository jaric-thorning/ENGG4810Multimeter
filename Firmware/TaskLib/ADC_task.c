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

#include "spi_adc.h"

#include "ADC_task.h"

#include "stdlib.h"
//ADC INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
#include "driverlib/adc.h"

#include "display.h"

#include "mswitch_task.h"

#define ADCTASKSTACKSIZE        128

#define ADC_ITEM_SIZE           sizeof(struct adc_queue_message)
#define ADC_QUEUE_SIZE          5

#define ADC_REFRESH_TIME 1000

uint32_t ui32Value;

extern xSemaphoreHandle g_pUARTSemaphore;

static void
ADCTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32ADCRefreshTime;

    ui32ADCRefreshTime = ADC_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    struct mswitch_queue_message mswitch_message;
    struct adc_queue_message adc_message;

    uint32_t data;
    uint8_t status;
    uint8_t control;



    uint8_t command = 0b10000001;
    send_command(command); //Self Calibrate

    //write_byte(0b11000010, 0b00000000);

    //command = 0b10000111;
    //send_command(command); //Self Calibrate

    while(1)
    {

      send_command(command); //Self Calibrate
      SysCtlDelay(10);
      //send_command(command);
      //send_command(command); //Self Calibrate
      status = read_byte(0b11000001);

      UARTprintf("   Status: ", status);
      for(int i = 7; i >= 0; i--){
        UARTprintf("%d", (status >> i) & 1);
      }
      UARTprintf("\n\r");



      control = read_byte(0b11000011);

      UARTprintf("Control 1: ", control);
      for(int i = 7; i >= 0; i--){
        UARTprintf("%d", (control >> i) & 1);
      }
      UARTprintf("\n\r");

      control = read_byte(0b11000101);

      UARTprintf("Control 2: ", control);
      for(int i = 7; i >= 0; i--){
        UARTprintf("%d", (control >> i) & 1);
      }
      UARTprintf("\n\r");

      control = read_byte(0b11000111);

      UARTprintf("Control 3: ", control);
      for(int i = 7; i >= 0; i--){
        UARTprintf("%d", (control >> i) & 1);
      }
      UARTprintf("\n\r");

      //write_byte(0b11000010, 0b1100100);

      if(1){ //Conversion ready
        data = read_data();
        float converted = data/5756991.0 * 3.3;

        int integer = (int)converted;
        int decimal = ((int)(converted*100000))%100000;
        if(decimal < 0){
          decimal *= -1;
        }
        UARTprintf("External ADC read: %d.%d\n\r", integer, decimal);

        UARTprintf("External ADC read: ");
        for(int i = 23; i >= 0; i--){
          UARTprintf("%d", (data >> i) & 1);
        }
        UARTprintf("\n\r");
      } else { //conversion not ready
        UARTprintf("Data not ready.\n\r");
      }



      ADCProcessorTrigger(ADC0_BASE, 0);
  		while(!ADCIntStatus(ADC0_BASE, 0, false))
  		{
  		}
  		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

      //UARTprintf("ADC : %dw")
      mswitch_message.ui32Value = ui32Value;
      mswitch_message.type = 'V'; //sending V for value

      if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
         pdPASS){
           UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
         }

      if(xQueueReceive(g_pADCQueue, &adc_message, 0) == pdPASS)
      {
          if( adc_message.frequency > 0){
            ui32ADCRefreshTime = adc_message.frequency * 100;
          }
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
    //SysCtlDelay(10000000);
    g_pADCQueue = xQueueCreate(ADC_QUEUE_SIZE, ADC_ITEM_SIZE);
    uint8_t status;
    uint8_t control1;

    spi_adc_init();



    /*UARTprintf(" ------------ On Load SPI Characteristics ----\n\r");

    status = read_byte(0b11000001);

    UARTprintf("Status: ", status);
    for(int i = 7; i >= 0; i--){
      UARTprintf("%d", (status >> i) & 1);
    }
    UARTprintf("\n\r");

    control1 = read_byte(0b11000011);

    UARTprintf("Control 1: ", control1);
    for(int i = 7; i >= 0; i--){
      UARTprintf("%d", (control1 >> i) & 1);
    }
    UARTprintf("\n\r");

    UARTprintf(" ------------ On Load ADC Characteristics ----\n\r");


    UARTprintf("Calibrating ADC....\n\r");

    uint8_t command = 0b10010000;
    send_command(command); //Self Calibrate


    command = 0b10000111;
    send_command(command);

    write_byte(0b11000010, 0b0000000);


    UARTprintf(" ------------ Post Calibration ADC Characteristics ----n\r");

    status = read_byte(0b11000001);

    UARTprintf("Status: ", status);
    for(int i = 7; i >= 0; i--){
      UARTprintf("%d", (status >> i) & 1);
    }
    UARTprintf("\n\r");

    control1 = read_byte(0b11000011);

    UARTprintf("Control 1: ", control1);
    for(int i = 7; i >= 0; i--){
      UARTprintf("%d", (control1 >> i) & 1);
    }
    UARTprintf("\n\r");

    UARTprintf(" ------------ Post Calibration ADC Characteristics ----\n\r");
    */

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

    UARTprintf("    ADC initiated.\n\r");

    //
    // Success.
    //
    return(0);
}
