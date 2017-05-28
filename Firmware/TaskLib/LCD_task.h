/*
 *  Header file for LCD_Task
 *
 *
 */



#ifndef __LCD_TASK_H__
#define __LCD_TASK_H__

extern struct lcd_queue_message {
  char mode;
  char button;
  int setting;
  int brightness;
  char type;
  int range;
  int value;
  int decimal;
  int overlimit;
  int negative_value;
} lcd_queue_message;

xQueueHandle g_pLCDQueue;

//*****************************************************************************
//
// Prototypes for the LED task.
//
//*****************************************************************************
extern uint32_t LCDTaskInit(void);




#endif // __LED_TASK_H__
