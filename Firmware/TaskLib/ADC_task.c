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
#include "math.h"

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

    struct mswitch_queue_message mswitch_message;
    struct adc_queue_message adc_message;

    uint32_t data;
    uint8_t status;
    uint8_t control;

    char adc_mode = 'N';

    uint8_t command = 0b10000111;
    send_command(command); //Self Calibrate

    //write_byte(0b11000010, 0b00000000);

    //command = 0b10000111;
    //send_command(command); //Self Calibrate

    int getting_min = 0;
    int getting_max = 1;
    int getting_period = 0;
    int calculating_rms = 0;

    float max_value = 0;
    float min_value = 3.3;
    int last_was_below_half = 1;
    int started_timer = 0;
    float half_way = 0;

    int crossed = 0;
    int cross_count = 0;

    int started_calculation = 0;

    float average_rms = 0;
    float sum_square_rms = 0;
    int rms_count = 0;
    float rms = 0;

    int integer;
    int decimal;

    float converted;

    portTickType period, start_time, start_calc_time;
    while(1)
    {

      send_command(command); //Self Calibrate
      SysCtlDelay(10);
      //send_command(command);
      //send_command(command); //Self Calibrate
      /*status = read_byte(0b11000001);

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
      UARTprintf("\n\r");*/

      //write_byte(0b11000010, 0b1100100);


      if(1){ //Conversion ready
        data = read_data();
        converted = data/5756991.0 * 3.3;

        //converted -= 1.65;
        integer = (int)converted;
        decimal = ((int)(converted*100000))%100000;
        if(decimal < 0){
          decimal *= -1;
        }

        /*if( xSemaphoreTake(g_pUARTSemaphore,portMAX_DELAY) == pdTRUE )
        {
        UARTprintf("External ADC: %d.%d\n\r", integer, decimal);
        }
        xSemaphoreGive(g_pUARTSemaphore);*/
        /*UARTprintf("External ADC read: ");
        for(int i = 23; i >= 0; i--){
          UARTprintf("%d", (data >> i) & 1);
        }
        UARTprintf("\n\r");*/
      } else { //conversion not ready
        UARTprintf("Data not ready.\n\r");
      }

      if(0){//adc_mode == 'R'){
        if(getting_max){

          if(converted > max_value){
            max_value = converted;
          }
          else{
            getting_max = 0;
            getting_min = 1;
            //UARTprintf("Got max\n\r");
          }
        } else if (getting_min){

          if(converted < min_value){
            min_value = converted;
          }
          else{
            getting_min = 0;
            half_way = (max_value + min_value)/2.0;
            getting_period = 1;
            //UARTprintf("Got min\n\r");
          }
        } else if (getting_period){
          if(last_was_below_half && (converted > half_way)){
            //crosssed
            crossed = 1;
          } else if (!last_was_below_half && (converted < half_way)){
            //crossed
            crossed = 1;
          } else if (converted > half_way){
            last_was_below_half = 0;
          } else if ( converted < half_way){
            last_was_below_half = 1;
          }

          if(crossed){
            cross_count++;
            if(started_timer){
              if(cross_count >= 3){
                period = xTaskGetTickCount() - start_time;
                getting_period = 0;
                calculating_rms = 1;
                //UARTprintf("Got period\n\r");
              }
            } else{
              started_timer = 1;
              start_time = xTaskGetTickCount();
            }
          }

        } else if (calculating_rms){
          //UARTprintf("Calculating\n\r");
          if(started_calculation){
            if(xTaskGetTickCount() > (start_calc_time + period)){
              //done
              rms = sqrt(sum_square_rms/rms_count);

              integer = (int)rms;
              decimal = ((int)(rms*100000))%100000;
              if(decimal < 0){
                decimal *= -1;
              }
              UARTprintf("RMS: %d.%d\n\r", integer, decimal);
              vTaskDelayUntil(&ui32WakeTime, 1000 / portTICK_RATE_MS);
              //reset all values:
              getting_max = 1;
              calculating_rms = 0;

              getting_min = 0;
              getting_period = 0;
              calculating_rms = 0;

              max_value = 0;
              min_value = 3.3;
              last_was_below_half = 1;
              started_timer = 0;
              half_way = 0;

              crossed = 0;
              cross_count = 0;

              started_calculation = 0;

              average_rms = 0;
              sum_square_rms = 0;
              rms_count = 0;
              rms = 0;

            } else{
              sum_square_rms += (converted * converted);
              rms_count++;
            }
          } else{
            start_calc_time = xTaskGetTickCount();
            started_calculation = 1;
          }

        }
      } else{

        ADCProcessorTrigger(ADC0_BASE, 0);
    		while(!ADCIntStatus(ADC0_BASE, 0, false))
    		{
    		}
    		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

        //UARTprintf("ADC : %dw")
        //
        mswitch_message.ui32Value = ui32Value;
        //mswitch_message.ui32Value = data;
        mswitch_message.type = 'V'; //sending V for value

        if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
           pdPASS){
             UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
           }
      }
      if(xQueueReceive(g_pADCQueue, &adc_message, 0) == pdPASS)
      {
          if(adc_message.mode == 'R'){
            adc_mode = 'R';
            ui32ADCRefreshTime = 50;
          } else if ( adc_message.mode == 'N'){
            adc_mode = 'N';
            ui32ADCRefreshTime = 1000;
          } else if(adc_message.mode == 'F'){
            if( adc_message.frequency > 0){
              ui32ADCRefreshTime = adc_message.frequency/2;
            }
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
