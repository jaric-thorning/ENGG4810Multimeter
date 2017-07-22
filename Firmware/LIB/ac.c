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

void collect_samples(void){
  int sample_count = 0;
  int start_time = 0;
  int total_time = 0;
  int read_timeout = 0;
  int integer = 0;
  int decimal = 0;
  uint8_t status;
  int required_samples = 100;
  uint8_t fast_command = 0b10000111;
  float converted = 0;
  float average = 0;
  float max = 0;
  float min = 1000000;

  samples = bgetz(required_samples * 32);
  double sum = 0;
  uint32_t data;
  send_command(fast_command);
  UARTprintf("Collecting Samples.\n\r");
  //write_byte(0b11000010, 0b1101100); //set adc to continuous mode


  start_time = xTaskGetTickCount();
  while(sample_count < required_samples){

    send_command(fast_command);
    do{
      status = read_byte(0b11000001);
    } while(!(status  & 1) && (xTaskGetTickCount() < read_timeout + 10));

    data = read_data();
    if(((data * 2 - 5664672.0)/5664672.0) > 0){
      samples[sample_count++] = data;
    }
  }
  total_time = xTaskGetTickCount() - start_time;
  UARTprintf("Samples Collected.\n\r");
  for(int i = 0; i < required_samples; i++){
    converted = (samples[i] * 2 - 5664672.0)/5664672.0;

    integer = (int)converted;

    if(decimal < 0){
      decimal *= -1;
    }
    if(converted > max){
      max = converted;
    }
    if(converted < min){
      min = converted;
    }
    sum += converted;
    average = sum/i;
    UARTprintf("%d> %d  | sum: %d | ave: %d\n\r", i, integer, (int) sum, (int) average);
  }
  UARTprintf("ave: %d | min: %d  | max: %d \n\r", (int) average, (int) min, (int) max);
  UARTprintf("Samples collected in %d miliseconds.\n\r", total_time);
  brel(samples);
}
