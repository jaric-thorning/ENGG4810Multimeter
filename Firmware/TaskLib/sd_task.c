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


#include "sd_task.h"
#include "lcd_task.h"

///#include "xxx.h"
//SD INCLUDES

#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"

#include "sd_card.h"

#include "bget.h"
#include "ADC_task.h"

#define SDTASKSTACKSIZE        512

#define SD_ITEM_SIZE           sizeof(struct sd_queue_message)
#define SD_QUEUE_SIZE          5

#define SD_REFRESH_TIME 500

#define CD 0
#define CS 1
#define DI 2
#define DO 3
#define CLK 4

extern xSemaphoreHandle g_pUARTSemaphore;
extern xSemaphoreHandle g_pSPISemaphore;

void check_range(float value);
void set_shift_pin(int pin, int value);
void set_mode(char mode);
void change_voltage(int voltage);

static void
SDTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32SDRefreshTime;
    struct sd_queue_message sd_message;

    sd_message.filename = (char*)bgetz(64 * sizeof(char));
    sd_message.text = (char*)bgetz(64 * sizeof(char));

    ui32SDRefreshTime = SD_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    int result;

    while(1)
    {
      // Read the next message, if available on queue.
      //Only run if can print output
      if( xSemaphoreTake(g_pUARTSemaphore,portMAX_DELAY) == pdTRUE )
      {
        //only run if not another SPI action running
        if( xSemaphoreTake(g_pSPISemaphore,portMAX_DELAY) == pdTRUE )
        {
          if(xQueueReceive(g_pSDQueue, &sd_message, 0) == pdPASS)
          {
            //UARTprintf("Recieved filename: %s\n\r", sd_message.filename);
            result = append_to_file(sd_message.filename, sd_message.text);
            if(result != 0){
              UARTprintf("FAILED TO POST TO LOG\n\r");
            }
          }
          xSemaphoreGive(g_pSPISemaphore);
        }
     xSemaphoreGive(g_pUARTSemaphore);

    }
      // Wait for the required amount of time.
      vTaskDelayUntil(&ui32WakeTime, ui32SDRefreshTime / portTICK_RATE_MS);
    }
}

uint32_t
SDTaskInit(void)
{

    g_pSDQueue = xQueueCreate(SD_QUEUE_SIZE, SD_ITEM_SIZE);

    initialise_sd_card();

    if(xTaskCreate(SDTask, (signed portCHAR *)"SD", SDTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_SD_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("    SD initiated...\n\r");

    //
    // Success.
    //
    return(0);
}
