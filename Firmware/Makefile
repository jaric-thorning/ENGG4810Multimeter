#
# Multimeter Make File
#

PROJECT=main

#
# Defines the part type that this project uses.
#
PART=TM4C123GH6PM

#
# The base directory for TivaWare.
#
#ROOT=../../..
ROOT=$(HOME)/tiva-c-master

#
# Include the common make definitions.
#
include ${ROOT}/makedefs

#
# Where to find source files that do not live in this directory.
#
VPATH=$(ROOT)/third_party/FreeRTOS/Source/portable/GCC/ARM_CM4F
VPATH+=$(ROOT)/third_party/FreeRTOS/Source/portable/MemMang/
VPATH+=$(ROOT)/third_party/FreeRTOS/Source
VPATH+=$(ROOT)/third_party
VPATH+=$(ROOT)/third_party/bget
VPATH+=$(ROOT)/third_party/fatfs
VPATH+=$(ROOT)/third_party/fatfs/src
VPATH+=$(ROOT)/third_party/fatfs/port
VPATH+=$(ROOT)/third_party/fatfs/option
VPATH+=../drivers
VPATH+=$(ROOT)/utils
VPATH+=./LIB
VPATH+=./TaskLib


#
# Where to find header files that do not live in the source directory.
#
IPATH=.
IPATH+=..
IPATH+=./LIB
IPATH+=./utils
IPATH+=./TaskLib
IPATH+=$(ROOT)/boards/ek-tm4c123gxl
IPATH+=$(ROOT)
IPATH+=$(ROOT)/third_party/FreeRTOS/Source/portable/GCC/ARM_CM4F
IPATH+=$(ROOT)/third_party/FreeRTOS
IPATH+=$(ROOT)/third_party/FreeRTOS/Source/include
IPATH+=$(ROOT)/third_party
IPATH+=$(ROOT)/third_party/bget
IPATH+=$(ROOT)/third_party/fatfs
IPATH+=$(ROOT)/third_party/fatfs/src

#
# The default rule, which causes the FreeRTOS example to be built.
#
all: ${COMPILER}
all: ${COMPILER}/$(PROJECT).axf

#
# The rule to clean out all the build products.
#
clean:
	@rm -rf ${COMPILER} ${wildcard *~}

#
# The rule to create the target directory.
#
${COMPILER}:
	@mkdir -p ${COMPILER}

#
# Rules for building the FreeRTOS example.
#
${COMPILER}/$(PROJECT).axf: ${COMPILER}/buttons.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/$(PROJECT).o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/heap_2.o

${COMPILER}/$(PROJECT).axf: ${COMPILER}/list.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/port.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/queue.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/rgb.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/startup_${COMPILER}.o

${COMPILER}/$(PROJECT).axf: ${COMPILER}/tasks.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/uartstdio.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/ustdlib.o


#------TASKS------#
${COMPILER}/$(PROJECT).axf: ${COMPILER}/led_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/switch_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/comm_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/LCD_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/ADC_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/mswitch_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/sd_task.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/buzzer_task.o

#-----------------#

#--------SD CARD--------#
#${COMPILER}/$(PROJECT).axf: ${COMPILER}/cc932.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/mmc-dk-tm4c123g.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/ff.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/cmdline.o

#--------GENERAL--------#
${COMPILER}/$(PROJECT).axf: ${COMPILER}/bget.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/display.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/uart.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/general_functions.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/sd_card.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/spi_adc.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/menu.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/ac.o
${COMPILER}/$(PROJECT).axf: ${COMPILER}/mswitch_helper.o

${COMPILER}/$(PROJECT).axf: ${ROOT}/driverlib/${COMPILER}/libdriver.a
${COMPILER}/$(PROJECT).axf: $(PROJECT).ld


SCATTERgcc_$(PROJECT)=$(PROJECT).ld
ENTRY_$(PROJECT)=ResetISR
CFLAGSgcc=-DTARGET_IS_TM4C123_RB1

#
# Include the automatically generated dependency files.
#
ifneq (${MAKECMDGOALS},clean)
-include ${wildcard ${COMPILER}/*.d} __dummy__
endif

prog:
	$(HOME)/lm4tools-master/lm4flash/lm4flash -v gcc/$(PROJECT).bin
