
#ifndef SPI_ADC_H_
#define SPI_ADC_H_


void pulse_sclk(void);
void set_cs(int state);
void set_di(int bit);
void send_command(uint8_t command);
void write_byte(uint8_t c_add, uint8_t byte);
int get_do(void);
uint32_t read_byte(uint8_t address);
uint32_t read_data(void);
void spi_adc_init(void);


#endif /* SPI_ADC_H_ */
