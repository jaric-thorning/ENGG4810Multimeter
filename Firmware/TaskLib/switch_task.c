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

#include "ADC_task.h"

#include "buzzer_task.h"

#include "lcd_task.h"

#define BUTTON3_PIN GPIO_PIN_2
#define BUTTON4_PIN GPIO_PIN_3
#define BUTTON5_PIN GPIO_PIN_3
#define BUTTON6_PIN GPIO_PIN_2

#define BUTTON3_PORT GPIO_PORTF_BASE
#define BUTTON4_PORT GPIO_PORTF_BASE
#define BUTTON5_PORT GPIO_PORTB_BASE
#define BUTTON6_PORT GPIO_PORTB_BASE

#define BUTTON3_PERIPH_GPIO SYSCTL_PERIPH_GPIOF
#define BUTTON4_PERIPH_GPIO SYSCTL_PERIPH_GPIOF
#define BUTTON5_PERIPH_GPIO SYSCTL_PERIPH_GPIOB
#define BUTTON6_PERIPH_GPIO SYSCTL_PERIPH_GPIOB



#define SWITCHTASKSTACKSIZE        128         // Stack size in words

extern xQueueHandle g_pLEDQueue;
extern xSemaphoreHandle g_pUARTSemaphore;

static void
SwitchTask(void *pvParameters)
{
    portTickType ui16LastTime, currentTime;
    uint32_t ui32SwitchDelay = 25;
    uint8_t ui8CurButtonState, ui8PrevButtonState;
    uint8_t ui8Message;

    ui8CurButtonState = ui8PrevButtonState = 0;
    ui16LastTime = xTaskGetTickCount();

    struct mswitch_queue_message mswitch_message;
    struct adc_queue_message adc_message;
    struct buzzer_queue_message buzzer_message;
    struct lcd_queue_message lcd_message;

    uint32_t button3 = 0;
    uint32_t button4 = 0;
    uint32_t button5 = 0;
    uint32_t button6 = 0;

    int button3_time = 0;
    int button4_time = 0;
    int button5_time = 0;
    int button6_time = 0;

    int freq = 1;
    int brightness = 5;

    while(1)
    {
        ui8CurButtonState = ButtonsPoll(0, 0);

        button3 = GPIOPinRead(BUTTON3_PORT,BUTTON3_PIN);
        button4 = GPIOPinRead(BUTTON4_PORT,BUTTON4_PIN);
        button5 = GPIOPinRead(BUTTON5_PORT,BUTTON5_PIN);
        button6 = GPIOPinRead(BUTTON6_PORT,BUTTON6_PIN);

        currentTime = xTaskGetTickCount();
        if((button3 == BUTTON3_PIN) && (button3_time + 200 < currentTime)){
          //UARTprintf("BUTTON3 is HIGH!\n\r");
          button3_time = currentTime;

          mswitch_message.ui32Value = 0; //doesn't matter
          mswitch_message.type = 'M'; //sending M for mode
          mswitch_message.mode = 'I'; //sending I to increment mode

          if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }

        }
        else if((button4 == BUTTON4_PIN) && (button4_time + 200 < currentTime)){
          //UARTprintf("BUTTON4 is HIGH!\n\r");
          button4_time = currentTime;

          mswitch_message.ui32Value = 0; //doesn't matter
          mswitch_message.type = 'R'; //sending M for mode

          if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }

        }
        else if((button5 == BUTTON5_PIN) && (button5_time + 200 < currentTime)){

          brightness = (brightness - 1);
          if(brightness < 0){
            brightness = 5;
          }
          lcd_message.setting = 1;
          lcd_message.brightness = brightness;
          if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }


          /*freq = ((freq + 5) % 50) + 1;
          adc_message.frequency = freq;
          UARTprintf("Changing sampling frequency to: %d\n\r", freq * 100);
          if(xQueueSend(g_pADCQueue, &adc_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }*/
          button5_time = currentTime;
        }
        else if((button6 == BUTTON6_PIN) && (button6_time + 200 < currentTime)){
          buzzer_message.frequency = 1000;
          if(xQueueSend(g_pBuzzerQueue, &buzzer_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }

          button6_time = currentTime;
        }


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

    //External Button Init
    SysCtlPeripheralEnable(BUTTON3_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON3_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON3_PORT, BUTTON3_PIN);

    SysCtlPeripheralEnable(BUTTON4_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON4_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON4_PORT, BUTTON4_PIN);

    SysCtlPeripheralEnable(BUTTON5_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON5_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON5_PORT, BUTTON5_PIN);

    SysCtlPeripheralEnable(BUTTON6_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON6_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON6_PORT, BUTTON6_PIN);







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
