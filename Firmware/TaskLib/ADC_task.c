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
//#include "math.h"

#include "spi_adc.h"

#include "ADC_task.h"

//#include "xxx.h"
//ADC INCLUDES

#include "driverlib/rom.h"
#include "driverlib/adc.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
//#include "driverlib/adc.h"

#include "display.h"

#include "mswitch_task.h"

#define ADCTASKSTACKSIZE        128

#define ADC_ITEM_SIZE           sizeof(struct adc_queue_message)
#define ADC_QUEUE_SIZE          5

#define ADC_REFRESH_TIME 500
#define RMS_TIMEOUT 1000

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
    uint8_t slow_command = 0b10000111;
    uint8_t fast_command = 0b10000111;

    send_command(fast_command); //Self Calibrate

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

    int getting_rms = 1;

    double converted;

    portTickType period, start_time, start_calc_time, rms_start_time, read_timeout;


    while(1)
    {
      if(adc_mode == 'R'){
        getting_rms = 1;
        rms_start_time = xTaskGetTickCount();
        while(getting_rms && (xTaskGetTickCount() < rms_start_time + RMS_TIMEOUT)){

          send_command(fast_command); //Self Calibrate
          SysCtlDelay(1);

          read_timeout = xTaskGetTickCount();
          do{
            status = read_byte(0b11000001);

            /*UARTprintf("   Status: ", status);
            for(int i = 7; i >= 0; i--){
              UARTprintf("%d", (status >> i) & 1);
            }
            UARTprintf("\n\r");*/
          } while(!(status  & 1) && (xTaskGetTickCount() < read_timeout + 10));

          if(!(status  & 1)){
            UARTprintf("Warning - ADC timeout.\n\r");
          }
            data = read_data();
            converted = data/5691091.0;

            //UARTprintf("ADC: %d.%d\n\r", (int)converted, ((int)(converted * 1000))%1000);
          if(getting_max){

            if(converted > max_value){
              max_value = converted;
            }
            else{
              rms_start_time = xTaskGetTickCount();
              getting_max = 0;
              getting_min = 1;
              UARTprintf("Got max %d.%d\n\r", (int)max_value, ((int)(max_value * 1000))%1000);
            }
          } else if (getting_min){

            if(converted < min_value){
              min_value = converted;
            }
            else{
              rms_start_time = xTaskGetTickCount();
              getting_min = 0;
              half_way = (max_value + min_value)/2.0;
              getting_period = 1;
              UARTprintf("Got min %d.%d\n\r", (int)min_value, ((int)(min_value * 1000))%1000);
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
                  rms_start_time = xTaskGetTickCount();
                  period = xTaskGetTickCount() - start_time;
                  getting_period = 0;
                  calculating_rms = 1;
                  //UARTprintf("Got period %d\n\r", period);
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
                //rms = sqrt(sum_square_rms/rms_count)  - 1.65;

                integer = (int)rms;
                decimal = ((int)(rms*100000))%100000;
                if(decimal < 0){
                  decimal *= -1;
                }
                UARTprintf("RMS: %d.%d\n\r", integer, decimal);


                mswitch_message.max_value = max_value;
                mswitch_message.value = rms;
                //mswitch_message.ui32Value = data;
                mswitch_message.type = 'V'; //sending V for value

                if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
                   pdPASS){
                     UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
                   }

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
                getting_rms = 0;

              } else{
                sum_square_rms += (converted * converted);
                rms_count++;
              }
            } else{
              start_calc_time = xTaskGetTickCount();
              started_calculation = 1;
            }
          }
          //vTaskDelayUntil(&ui32WakeTime, 10 / portTICK_RATE_MS);
        }
      } else{

        send_command(slow_command); //Self Calibrate

        //send_command(command);
        //send_command(command); //Self Calibrate

        read_timeout = xTaskGetTickCount();
        do{
          status = read_byte(0b11000001);

          /*UARTprintf("   Status: ", status);
          for(int i = 7; i >= 0; i--){
            UARTprintf("%d", (status >> i) & 1);
          }
          UARTprintf("\n\r");*/
        } while(!(status  & 1) && (xTaskGetTickCount() < read_timeout + 10));

        if(!(status  & 1)){
          UARTprintf("Warning - ADC timeout.\n\r");
        }
          data = read_data();
          converted = data/5664672.0;

        /*ADCProcessorTrigger(ADC0_BASE, 0);
    		while(!ADCIntStatus(ADC0_BASE, 0, false))
    		{
    		}
    		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);*/

        integer = (int)converted;
        decimal = ((int)(converted*1000000))%1000000;
        if(decimal < 0){
          //decimal *= -1;
        }
        UARTprintf("ADC: %d.%d\n\r", integer, decimal);

        //
        mswitch_message.value = converted;
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
          } else if ( adc_message.mode == 'N'){
            adc_mode = 'N';

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
  	/*ADCSequenceConfigure(ADC0_BASE, 0, ADC_TRIGGER_PROCESSOR, 0);
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
  	ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);*/

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
