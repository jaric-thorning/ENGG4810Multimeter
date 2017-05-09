#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include "inc/hw_memmap.h"
#include "driverlib/fpu.h"
#include "driverlib/gpio.h"
#include "driverlib/interrupt.h"
#include "driverlib/pin_map.h"
#include "driverlib/rom.h"
#include "driverlib/sysctl.h"
#include "driverlib/systick.h"
#include "driverlib/uart.h"
#include "grlib/grlib.h"
#include "utils/cmdline.h"
#include "utils/uartstdio.h"
#include "fatfs/src/ff.h"
#include "fatfs/src/diskio.h"

#define PATH_BUF_SIZE           80
#define CMD_BUF_SIZE            64
//static char g_pcCwdBuf[PATH_BUF_SIZE] = "/";
static char g_pcTmpBuf[PATH_BUF_SIZE];
//static char g_pcCmdBuf[CMD_BUF_SIZE];
static FATFS g_sFatFs;
//static DIR g_sDirObject;
//static FILINFO g_sFileInfo;
static FIL g_sFileObject;

typedef struct
{
    FRESULT iFResult;
    char *pcResultStr;
}
tFResultString;


#define FRESULT_ENTRY(f)        { (f), (#f) }


tFResultString g_psFResultStrings[] =
{
    FRESULT_ENTRY(FR_OK),
    FRESULT_ENTRY(FR_DISK_ERR),
    FRESULT_ENTRY(FR_INT_ERR),
    FRESULT_ENTRY(FR_NOT_READY),
    FRESULT_ENTRY(FR_NO_FILE),
    FRESULT_ENTRY(FR_NO_PATH),
    FRESULT_ENTRY(FR_INVALID_NAME),
    FRESULT_ENTRY(FR_DENIED),
    FRESULT_ENTRY(FR_EXIST),
    FRESULT_ENTRY(FR_INVALID_OBJECT),
    FRESULT_ENTRY(FR_WRITE_PROTECTED),
    FRESULT_ENTRY(FR_INVALID_DRIVE),
    FRESULT_ENTRY(FR_NOT_ENABLED),
    FRESULT_ENTRY(FR_NO_FILESYSTEM),
    FRESULT_ENTRY(FR_MKFS_ABORTED),
    FRESULT_ENTRY(FR_TIMEOUT),
    FRESULT_ENTRY(FR_LOCKED),
    FRESULT_ENTRY(FR_NOT_ENOUGH_CORE),
    FRESULT_ENTRY(FR_TOO_MANY_OPEN_FILES),
    FRESULT_ENTRY(FR_INVALID_PARAMETER),
};

#define NUM_FRESULT_CODES       (sizeof(g_psFResultStrings) /                 \
                                 sizeof(tFResultString))

tContext g_sContext;

//*****************************************************************************
//
// This function returns a string representation of an error code that was
// returned from a function call to FatFs.  It can be used for printing human
// readable error messages.
//
//*****************************************************************************
const char *
StringFromFResult(FRESULT iFResult)
{
    uint_fast8_t ui8Idx;

    //
    // Enter a loop to search the error code table for a matching error code.
    //
    for(ui8Idx = 0; ui8Idx < NUM_FRESULT_CODES; ui8Idx++)
    {
        //
        // If a match is found, then return the string name of the error code.
        //
        if(g_psFResultStrings[ui8Idx].iFResult == iFResult)
        {
            return(g_psFResultStrings[ui8Idx].pcResultStr);
        }
    }

    //
    // At this point no matching code was found, so return a string indicating
    // an unknown error.
    //
    return("UNKNOWN ERROR CODE");
}

//*****************************************************************************
//
// This is the handler for this SysTick interrupt.  FatFs requires a timer tick
// every 10 ms for internal timing purposes.
//
//*****************************************************************************
void
SysTickHandler(void)
{
    //
    // Call the FatFs tick timer.
    //
    disk_timerproc();
}

//*****************************************************************************
//
// The error routine that is called if the driver library encounters an error.
//
//*****************************************************************************
#ifdef DEBUG
void
__error__(char *pcFilename, uint32_t ui32Line)
{
}
#endif

extern void initialise_sd_card(void){
  int nStatus;
  FRESULT iFResult;
  //
  // Enable the peripherals used by this example.
  //
  ROM_SysCtlPeripheralEnable(SYSCTL_PERIPH_SSI0);

  //
  // Configure SysTick for a 100Hz interrupt.  The FatFs driver wants a 10 ms
  // tick.
  //
  ROM_SysTickPeriodSet(ROM_SysCtlClockGet() / 100);
  ROM_SysTickEnable();
  ROM_SysTickIntEnable();

  //
  // Enable Interrupts
  //
  ROM_IntMasterEnable();


  //
  // Mount the file system, using logical disk 0.
  //
  iFResult = f_mount(0, &g_sFatFs);
  if(iFResult != FR_OK)
  {
      UARTprintf("f_mount error: %s\n", StringFromFResult(iFResult));
      return(1);
  }

}

extern int append_to_file(char * filename, char * text){
        FRESULT iFResult;
        int bytes_written;

        // Copy the current path to the temporary buffer so it can be manipulated.
        //
        strcpy(g_pcTmpBuf, "/");

        //
        // Now finally, append the file name to result in a fully specified file.
        //
        strcat(g_pcTmpBuf, filename);

        //
        // Open the file for reading.
        //
        iFResult = f_open(&g_sFileObject, g_pcTmpBuf, FA_WRITE | FA_OPEN_ALWAYS);

        //
        // If there was some problem opening the file, then return an error.
        //
        if(iFResult != FR_OK)
        {
            UARTprintf("COULDN'T OPEN LOG FILE\n\r");
            return iFResult;
        }

        //seek to end of file to append
        iFResult = f_lseek(&g_sFileObject, f_size(&g_sFileObject));

        if (iFResult != FR_OK){
            UARTprintf("COULDN'T MOVE TO END OF FILE\n\r");
            return iFResult;
        }

        //UARTprintf("APPEND WRITE: %s", text);
        iFResult = f_write(&g_sFileObject, text, 64, &bytes_written);
        //
        // If there was some problem writing the file, then return an error.
        //
        if(iFResult != FR_OK)
        {
            UARTprintf("COULDN'T WRITE TO LOG FILE : %d\n\r", iFResult);
            return iFResult;
        }

        //
        // Close the file.
        //
        iFResult = f_close(&g_sFileObject);

        //
        // If there was some problem opening the file, then return an error.
        //
        if(iFResult != FR_OK)
        {
            UARTprintf("COULDN'T CLOSE LOG FILE\n\r");
            return iFResult;
        }

        return 0;
}
