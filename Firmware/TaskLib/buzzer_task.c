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

#include "buzzer_task.h"

//#include "xxx.h"
//Buzzer INCLUDES

#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

#include "display.h"

#include "driverlib/pwm.h"


#define BUZZERTASKSTACKSIZE        128

#define BUZZER_ITEM_SIZE           sizeof(struct buzzer_queue_message)
#define BUZZER_QUEUE_SIZE          5

#define BUZZER_REFRESH_TIME 10

extern xSemaphoreHandle g_pUARTSemaphore;

static void
BuzzerTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32BuzzerRefreshTime;
    struct buzzer_queue_message buzzer_message2;

    ui32BuzzerRefreshTime = BUZZER_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    //top_line = (char*)bgetz(16 * sizeof(char));
    //bottom_line = (char*)bgetz(16 * sizeof(char));

    bool state = false;

    unsigned long period = 5000;
    unsigned long pwmNow = period/10;

    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pBuzzerQueue, &buzzer_message2, 0) == pdPASS)
      {
        xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);

        if(buzzer_message2.sound){
          state = true;
        } else{
          state = false;
        }
        pwmNow = buzzer_message2.frequency/10.0 * period;

        PWMPulseWidthSet(PWM0_BASE, PWM_OUT_2,pwmNow);
        PWMOutputState(PWM0_BASE, PWM_OUT_2_BIT, state);

        xSemaphoreGive(g_pUARTSemaphore);
      }

      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32BuzzerRefreshTime / portTICK_RATE_MS);
    }
}

uint32_t
BuzzerTaskInit(void)
{
  g_pBuzzerQueue = xQueueCreate(BUZZER_QUEUE_SIZE, BUZZER_ITEM_SIZE);

  //Configure PWM Clock to match system
  SysCtlPWMClockSet(SYSCTL_PWMDIV_8);

  // Enable the peripherals used by this program.
  SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOB);
  SysCtlPeripheralEnable(SYSCTL_PERIPH_PWM0);  //The Tiva Launchpad has two modules (0 and 1).

  while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOB))
  {
  }

  while(!SysCtlPeripheralReady(SYSCTL_PERIPH_PWM0))
  {
  }

  GPIOPinConfigure(GPIO_PB4_M0PWM2);
  GPIOPinTypePWM(GPIO_PORTB_BASE, GPIO_PIN_4);


  //Configure PWM Options
  //PWM_GEN_2 Covers M1PWM4 and M1PWM5
  //PWM_GEN_3 Covers M1PWM6 and M1PWM7 See page 207 4/11/13 DriverLib doc
  PWMGenConfigure(PWM0_BASE, PWM_GEN_1, PWM_GEN_MODE_DOWN | PWM_GEN_MODE_NO_SYNC);

  unsigned long period = 5000;
  unsigned long pwmNow = period/10.0;

  //Set the Period (expressed in clock ticks)
  PWMGenPeriodSet(PWM0_BASE, PWM_GEN_1, period);

  //Set PWM duty-50% (Period /2)
  PWMPulseWidthSet(PWM0_BASE, PWM_OUT_2,pwmNow);

  // Enable the PWM generator
  PWMGenEnable(PWM0_BASE, PWM_GEN_1);

  // Turn on the Output pins
  //PWMOutputState(PWM0_BASE, PWM_OUT_0_BIT | PWM_OUT_2_BIT | PWM_OUT_4_BIT | PWM_OUT_5_BIT | PWM_OUT_6_BIT | PWM_OUT_7_BIT, true);
  PWMOutputState(PWM0_BASE, PWM_OUT_2_BIT, 0);

  if(xTaskCreate(BuzzerTask, (signed portCHAR *)"BUZZER", BUZZERTASKSTACKSIZE, NULL,
               tskIDLE_PRIORITY + PRIORITY_BUZZER_TASK, NULL) != pdTRUE)
    {
      return(1);
    }

  UARTprintf("    Buzzer initiated.\n\r");

  //
  // Success.
  //
  return(0);
}
