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

#include "bget.h"


#include "LCD_task.h"

#include "stdlib.h"
//LCD INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"

#include "driverlib/pwm.h"


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

    //top_line = (char*)bgetz(16 * sizeof(char));
    //bottom_line = (char*)bgetz(16 * sizeof(char));
    unsigned long period = 5000;

    int brightness_setting = 10;

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pLCDQueue, &lcd_message2, 0) == pdPASS)
      {
        xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);

        if(lcd_message2.setting == 1){
          PWMPulseWidthSet(PWM0_BASE, PWM_OUT_3, period * lcd_message2.brightness/5);;
          PWMOutputState(PWM0_BASE, PWM_OUT_3_BIT, lcd_message2.brightness);
          if(lcd_message2.brightness == 0){
            GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, GPIO_PIN_1);
          }
          else{
            GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, 0);
          }
        }
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

    // Enable the peripherals used by this program.
    SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOB);
    SysCtlPeripheralEnable(SYSCTL_PERIPH_PWM0);  //The Tiva Launchpad has two modules (0 and 1).
    SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOE);
    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOB))
    {
    }

    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_PWM0))
    {
    }
    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOE))
    {
    }

    GPIOPinTypeGPIOOutput(GPIO_PORTE_BASE, GPIO_PIN_1);
    GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, 0);

    GPIOPinConfigure(GPIO_PB5_M0PWM3);
    GPIOPinTypePWM(GPIO_PORTB_BASE, GPIO_PIN_5);


    //Configure PWM Options
    //PWM_GEN_2 Covers M1PWM4 and M1PWM5
    //PWM_GEN_3 Covers M1PWM6 and M1PWM7 See page 207 4/11/13 DriverLib doc
    PWMGenConfigure(PWM0_BASE, PWM_GEN_1, PWM_GEN_MODE_DOWN | PWM_GEN_MODE_NO_SYNC);

    unsigned long period = 5000;
    unsigned long pwmNow = period/10.0;

    //Set the Period (expressed in clock ticks)
    PWMGenPeriodSet(PWM0_BASE, PWM_GEN_1, period);

    //Set PWM duty-50% (Period /2)
    PWMPulseWidthSet(PWM0_BASE, PWM_OUT_3,period);

    // Enable the PWM generator
    PWMGenEnable(PWM0_BASE, PWM_GEN_1);

    // Turn on the Output pins
    //PWMOutputState(PWM0_BASE, PWM_OUT_0_BIT | PWM_OUT_2_BIT | PWM_OUT_4_BIT | PWM_OUT_5_BIT | PWM_OUT_6_BIT | PWM_OUT_7_BIT, true);
    PWMOutputState(PWM0_BASE, PWM_OUT_3_BIT, true);

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
