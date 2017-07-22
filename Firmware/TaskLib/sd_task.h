/*
 *  Header file for SD_Task
 *
 *
 */

#ifndef __SD_TASK_H__
#define __SD_TASK_H__

extern struct sd_queue_message {
  char * filename;
  char * text;
}sd_queue_message;

xQueueHandle g_pSDQueue;

//*****************************************************************************
//
// Prototypes for the SD task.
//
//*****************************************************************************
extern uint32_t SDTaskInit(void);

#endif // __MSWITCH_TASK_H__
