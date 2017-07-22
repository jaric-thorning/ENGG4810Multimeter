#ifndef MSWITCH_HELPER_H_   /* Include guard */
#define MSWITCH_HELPER_H_

//Configure UART

extern int next_mode(int current_mode, char requested_update);
extern int trigger_sd_logging(int logging, struct sd_queue_message * sd_message, int filename_count);
extern void record_to_sd(int logging, double integer, double decimal,
  char current_mode_char, struct sd_queue_message * sd_message);
#endif // MSWITCH_HELPER_H_
