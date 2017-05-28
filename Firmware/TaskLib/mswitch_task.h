/*
 *  Header file for MSWITCH_Task
 *
 *
 */



#ifndef __MSWITCH_TASK_H__
#define __MSWITCH_TASK_H__

extern struct mswitch_queue_message {
  char type;
  char mode;
  int frequency;
  float max_value;
  float value;
};

xQueueHandle g_pMSWITCHQueue;

//*****************************************************************************
//
// Prototypes for the MSWITCH task.
//
//*****************************************************************************
extern uint32_t MSWITCHTaskInit(void);




#endif // __MSWITCH_TASK_H__
