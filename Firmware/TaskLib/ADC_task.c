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
#include "ac.h"

#define ADCTASKSTACKSIZE        512

#define ADC_ITEM_SIZE           sizeof(struct adc_queue_message)
#define ADC_QUEUE_SIZE          5

#define ADC_TASK_REFRESH_TIME 10
#define RMS_TIMEOUT 1000
#define ADC_DEFAULT_FREQUENCY 500

uint32_t ui32Value;

extern xSemaphoreHandle g_pUARTSemaphore;
extern xSemaphoreHandle g_pSPISemaphore;

static void
ADCTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32ADCRefreshTime;

    ui32ADCRefreshTime = ADC_TASK_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    struct mswitch_queue_message mswitch_message;
    struct adc_queue_message adc_message;

    uint32_t data;
    uint8_t status;
    //uint8_t control;

    char adc_mode = 'N';
    uint8_t slow_command = 0x87; //0b10000111 (using fast command)
    uint8_t fast_command = 0x87; //0b10000111 (currently the same)

    send_command(fast_command); //Self Calibrate

    //write_byte(0b11000010, 0b00000000);

    //command = 0b10000111;
    //send_command(command); //Self Calibrate

    /*int getting_min = 0;
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
    float rms = 0;*/

    //int integer;
    //int decimal;

    //int getting_rms = 1;

    double converted;

    //portTickType period, start_time, start_calc_time, rms_start_time,
    portTickType read_timeout, last_adc_read;

    int frequency_ticks = ADC_DEFAULT_FREQUENCY;

    last_adc_read = xTaskGetTickCount();

    int hasnt_run = 1;
    while(1)
    {
      if(xTaskGetTickCount() > (last_adc_read + frequency_ticks)){
        //Only run if no other SPI action running
        if( xSemaphoreTake(g_pSPISemaphore,portMAX_DELAY) == pdTRUE )
        {
          if(adc_mode == 'R'){
            /*if(hasnt_run){
              hasnt_run = 0;
              collect_samples();
            }*/
            collect_samples();
            //UARTprintf("AC Done.\n\r");
          } else{

            send_command(slow_command); //Self Calibrate

            read_timeout = xTaskGetTickCount();
            do{
              status = read_byte(0xC1); //0b11000001
            } while(!(status  & 1) && (xTaskGetTickCount() < read_timeout + 10));

            if(!(status  & 1)){
              UARTprintf("Warning - ADC timeout.\n\r");
            }
              data = read_data();
              converted = (data * 2 - 5664672.0)/5664672.0;

            /*ADCProcessorTrigger(ADC0_BASE, 0);
        		while(!ADCIntStatus(ADC0_BASE, 0, false))
        		{
        		}
        		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);*/
            /*UARTprintf("BIN: ");
            for(int i = 32; i >= 0; i--){
              UARTprintf("%d", (data >> i) & 1);
            }
            UARTprintf("\n");
            UARTprintf("RAW: %d\n\r", data);
            integer = (int)converted;
            decimal = ((int)(converted*1000000))%1000000;
            if(decimal < 0){
              //decimal *= -1;
            }
            UARTprintf("ADC: %d.%d\n\n\r", integer, decimal);*/

            //
            mswitch_message.value = converted;
            mswitch_message.type = 'V'; //sending V for value


            if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }
          }
          last_adc_read = xTaskGetTickCount();
          //Return SPI usage permission
          xSemaphoreGive(g_pSPISemaphore);
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
              frequency_ticks = adc_message.frequency/2;
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
    //Creat SPI Protection Semaphore
    g_pSPISemaphore = xSemaphoreCreateMutex();

    //SysCtlDelay(10000000);
    g_pADCQueue = xQueueCreate(ADC_QUEUE_SIZE, ADC_ITEM_SIZE);
    //uint8_t status;
    //uint8_t control1;

    spi_adc_init();
    setup_ac();

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
