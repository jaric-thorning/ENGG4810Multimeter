#include <stdbool.h>
#include <stdint.h>
#include "inc/hw_memmap.h"
#include "inc/hw_types.h"
#include "inc/hw_gpio.h"
#include "driverlib/sysctl.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "drivers/buttons.h"
#include "utils/uartstdio.h"
#include "switch_task.h"
#include "led_task.h"
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"
#include "string.h"

#include "LCD_task.h"

#include "stdlib.h"

#include "mswitch_task.h"


#define SWITCHTASKSTACKSIZE        128         // Stack size in words

extern xQueueHandle g_pLEDQueue;
extern xSemaphoreHandle g_pUARTSemaphore;

static void
SwitchTask(void *pvParameters)
{
    portTickType ui16LastTime;
    uint32_t ui32SwitchDelay = 25;
    uint8_t ui8CurButtonState, ui8PrevButtonState;
    uint8_t ui8Message;

    ui8CurButtonState = ui8PrevButtonState = 0;
    ui16LastTime = xTaskGetTickCount();

    struct mswitch_queue_message mswitch_message;

    while(1)
    {
        ui8CurButtonState = ButtonsPoll(0, 0);

        if(ui8CurButtonState != ui8PrevButtonState)
        {
            ui8PrevButtonState = ui8CurButtonState;


            if((ui8CurButtonState & ALL_BUTTONS) != 0)
            {
                if((ui8CurButtonState & ALL_BUTTONS) == LEFT_BUTTON)
                {
                    ui8Message = LEFT_BUTTON;

                    //
                    // Guard UART from concurrent access.
                    //
                    xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
                    UARTprintf("Left Button is pressed.\n");
                    xSemaphoreGive(g_pUARTSemaphore);

                    mswitch_message.ui32Value = 0; //doesn't matter
                    mswitch_message.type = 'M'; //sending M for mode
                    mswitch_message.mode = 'I'; //sending I to increment mode

                    if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
                       pdPASS){
                         UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
                       }

                }
                else if((ui8CurButtonState & ALL_BUTTONS) == RIGHT_BUTTON)
                {
                    ui8Message = RIGHT_BUTTON;

                    //
                    // Guard UART from concurrent access.
                    //
                    xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
                    UARTprintf("Right Button is pressed.\n");
                    xSemaphoreGive(g_pUARTSemaphore);
                }

                if(xQueueSend(g_pLEDQueue, &ui8Message, portMAX_DELAY) !=
                   pdPASS)
                {
                    UARTprintf("\nQueue full. This should never happen.\n");
                    while(1)
                    {
                    }
                }
            }
        }
        vTaskDelayUntil(&ui16LastTime, ui32SwitchDelay / portTICK_RATE_MS);
    }
}

uint32_t
SwitchTaskInit(void)
{

    HWREG(GPIO_PORTF_BASE + GPIO_O_LOCK) = GPIO_LOCK_KEY;
    HWREG(GPIO_PORTF_BASE + GPIO_O_CR) = 0xFF;

    ButtonsInit();

    if(xTaskCreate(SwitchTask, (signed portCHAR *)"Switch",
                   SWITCHTASKSTACKSIZE, NULL, tskIDLE_PRIORITY +
                   PRIORITY_SWITCH_TASK, NULL) != pdTRUE)
    {
        return(1);
    }
    UARTprintf("Switch initiated...\n\r");
    //
    // Success.
    //
    return(0);
}
