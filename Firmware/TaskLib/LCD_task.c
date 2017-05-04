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


#include "LCD_task.h"

#include "stdlib.h"
//LCD INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"


#define LCDTASKSTACKSIZE        128

#define LCD_ITEM_SIZE           sizeof(struct lcd_queue_message)
#define LCD_QUEUE_SIZE          5

#define LCD_REFRESH_TIME 10

extern xSemaphoreHandle g_pUARTSemaphore;

static void
LCDTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32LCDRefreshTime;
    struct lcd_queue_message lcd_message2;

    ui32LCDRefreshTime = LCD_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();


    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pLCDQueue, &lcd_message2, 0) == pdPASS)
      {
        xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);

        UARTprintf("[%c: %d.%d]\n\r", lcd_message2.type, lcd_message2.value, lcd_message2.decimal);

        display(lcd_message2.type, lcd_message2.range, lcd_message2.value, lcd_message2.decimal);
        xSemaphoreGive(g_pUARTSemaphore);
      }


      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32LCDRefreshTime / portTICK_RATE_MS);
    }
}

uint32_t
LCDTaskInit(void)
{
    g_pLCDQueue = xQueueCreate(LCD_QUEUE_SIZE, LCD_ITEM_SIZE);

    initLCD();

    printLCD("Starting LCD...");

    if(xTaskCreate(LCDTask, (signed portCHAR *)"LCD", LCDTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_LCD_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("LCD initiated...\n\r");

    //
    // Success.
    //
    return(0);
}
