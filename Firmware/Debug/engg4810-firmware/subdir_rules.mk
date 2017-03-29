################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Each subdirectory must supply rules for building sources it contributes
engg4810-firmware/main.obj: ../engg4810-firmware/main.c $(GEN_OPTS) | $(GEN_HDRS)
	@echo 'Building file: $<'
	@echo 'Invoking: C2000 Compiler'
	"/Applications/ti/ccsv7/tools/compiler/ti-cgt-c2000_16.9.1.LTS/bin/cl2000" --include_path="/Users/jaricthorning/engg4810g32/Firmware" --include_path="/Applications/ti/ccsv7/tools/compiler/ti-cgt-c2000_16.9.1.LTS/include" -g --diag_warning=225 --diag_wrap=off --display_error_number --preproc_with_compile --preproc_dependency="engg4810-firmware/main.d" --obj_directory="engg4810-firmware" $(GEN_OPTS__FLAG) "$<"
	@echo 'Finished building: $<'
	@echo ' '

engg4810-firmware/startup_ccs.obj: ../engg4810-firmware/startup_ccs.c $(GEN_OPTS) | $(GEN_HDRS)
	@echo 'Building file: $<'
	@echo 'Invoking: C2000 Compiler'
	"/Applications/ti/ccsv7/tools/compiler/ti-cgt-c2000_16.9.1.LTS/bin/cl2000" --include_path="/Users/jaricthorning/engg4810g32/Firmware" --include_path="/Applications/ti/ccsv7/tools/compiler/ti-cgt-c2000_16.9.1.LTS/include" -g --diag_warning=225 --diag_wrap=off --display_error_number --preproc_with_compile --preproc_dependency="engg4810-firmware/startup_ccs.d" --obj_directory="engg4810-firmware" $(GEN_OPTS__FLAG) "$<"
	@echo 'Finished building: $<'
	@echo ' '


