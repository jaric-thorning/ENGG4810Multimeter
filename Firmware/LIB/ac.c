/*
 * ac.c functions for getting AC value
 *
 */

#include <stdint.h>
#include "spi_adc.h"
#include "FreeRTOS.h"
#include "task.h"
#include "utils/uartstdio.h"
#include "bget.h"

#define required_samples 100

uint8_t fast_command = 0b10000111;
uint32_t * samples;

/*float get_max(void){

}

float get_min(void){

}

float get_period(void){

}

float get_rms(void){

}*/
void setup_ac(void){
  samples = bgetz(1000 * 32);
}

double calculate_sqrt(double number){
  double iterative_number = number * 100;
  for(int i = 0; i < 100; i++){
    iterative_number = 0.5 * (iterative_number + number/iterative_number);
  }
  return iterative_number;
}
void collect_samples(void){
  int sample_count = 0;
  int start_time = 0;
  int total_time = 0;
  int read_timeout = 0;
  int integer = 0;
  int decimal = 0;
  uint8_t status;

  uint8_t fast_command = 0b10000111;
  double converted = 0;
  double average = 0;
  double max = 0;
  double min = 1000000;

  int min_sample = 0;
  int max_sample = 0;
  double sum = 0;
  uint32_t data;
  //send_command(fast_command);
  //UARTprintf("Collecting Samples.\n\r");
  //write_byte(0b11000010, 0b1101100); //set adc to continuous mode
  start_time = xTaskGetTickCount();
  while(sample_count < required_samples){
    send_command(fast_command); //Self Calibrate
    //SysCtlDelay(1);
    read_timeout = xTaskGetTickCount();
    do{
      status = read_byte(0xC1); //0b11000001
    } while(!(status  & 1) && (xTaskGetTickCount() < read_timeout + 10));
    if(!(status  & 1)){
      UARTprintf("Warning - ADC timeout.\n\r");
    }

    data = read_data();
    sample_count++;
    converted = (data * 2 - 5664672.0)/5664672.0;

    //UARTprintf("> %d.%d\n\r", (int)converted * 12, (int)(((int)(converted * 12 * 1000))%1000));

    if(converted > 0){
      converted = converted * 1.3;
    } else {
      converted += 0.4 * converted;
    }

    converted *= 12;

    if(converted > max){
      max = converted;
      max_sample = sample_count;
    }

    if(converted < min){
      min = converted;
      min_sample = sample_count;
    }
    sum += (converted * converted);

    /*if( sample_count % 100 == 0){
      UARTprintf(".");
    }*/
    //UARTprintf("%d\n\r", (int)sum);
  }
  //UARTprintf("Samples collected.\n\r");
  //UARTprintf("Control sqrt: %d.%d\n\r", (int)calculate_sqrt(59), (int)((int)(calculate_sqrt(59) * 1000)%1000));
  average = calculate_sqrt(sum/required_samples);
  total_time = xTaskGetTickCount() - start_time;
  int sum_i = (int) (sum);
  int sum_d = (int) ((int)(sum * 1000))%1000;
  int ave_i = (int) (average);
  int ave_d = (int) ((int)(average * 1000))%1000;
  int min_i = (int) (min);
  int min_d = (int) ((int)(min * 1000))%1000;
  int max_i = (int) (max);
  int max_d = (int) ((int)(max * 1000))%1000;

  if(min_d < 0){
    min_d *= -1;
  }
  UARTprintf("sum: %d.%d RMS: %d.%d | min: %d.%d  | max: %d.%d\n\r", sum_i, sum_d, ave_i, ave_d, min_i,
        min_d, max_i, max_d);
  //UARTprintf("Samples collected in %d miliseconds.\n\r", total_time);
  //brel(&samples);
}
