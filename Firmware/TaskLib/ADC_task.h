/*
 *  Header file for LCD_Task
 *
 *
 */



#ifndef __ADC_TASK_H__
#define __ADC_TASK_H__

/*extern struct lcd_queue_message {
  char type;
  int range;
  int value;
  int decimal;
};*/

//xQueueHandle g_pLCDQueue;

//*****************************************************************************
//
// Prototypes for the LED task.
//
//*****************************************************************************
extern uint32_t ADCTaskInit(void);




#endif // __LED_TASK_H__
