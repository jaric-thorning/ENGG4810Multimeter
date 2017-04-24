/*
 *  Header file for SD_Task
 *
 *
 */

#ifndef __SD_TASK_H__
#define __SD_TASK_H__

extern struct sd_queue_message {
  uint32_t ui32Value;
};

xQueueHandle g_pSDQueue;

//*****************************************************************************
//
// Prototypes for the SD task.
//
//*****************************************************************************
extern uint32_t SDTaskInit(void);

#endif // __MSWITCH_TASK_H__
