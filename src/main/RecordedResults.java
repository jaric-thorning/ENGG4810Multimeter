package main;

import java.util.ArrayList;

/**
 * A class which handles the display of recorded results.
 */
class RecordedResults {

	// The instance of the thread playing back data.
	public static PlaybackData dataPlaybackContainer = null;
	static GuiModel model = new GuiModel();

	/**
	 * Thread handled to quit in GuiView.stop(), and when other data is told to be displayed.
	 */
	public static class PlaybackData implements Runnable {

		private volatile boolean quit = false;

		// The status flag for whether or not the displaying data is paused/unpaused.
		private volatile boolean paused = false;

		private String filename;

		public PlaybackData(String filename) {
			this.filename = filename;
		}

		/**
		 * A private helper function for shutdownRecordedResultsThread(), which handles the method
		 * to stop this thread.
		 */
		private void shutdown() {
			this.quit = true;
		}

		/**
		 * A private helper function for pauseRecordedResultsThread(), which sets the value of the
		 * pause status.
		 * 
		 * @param pause
		 *            the new value of the pause status.
		 */
		private void setPause(boolean pause) {
			this.paused = pause;
		}

		/**
		 * Displays the data loaded from the file at a given speed.
		 */
		@Override
		public synchronized void run() {
			ArrayList<String> multimeterReadingsDataY = new ArrayList<>();
			String multimeterYUnit = "";

			// Reads data from the file
			multimeterReadingsDataY = model.readColumnData(filename, 1);
			multimeterYUnit = model.readColumnData(filename, 2).get(0);

			System.out.println(multimeterYUnit);
			// Displays the data results from the saved file.
			int dataIndex = -1;
			while (!this.quit && ++dataIndex < multimeterReadingsDataY.size()) {
				if (!this.paused) {
					final double yValue = Double
							.parseDouble(multimeterReadingsDataY.get(dataIndex));

					GuiController.instance.recordAndDisplayDummyData(yValue, multimeterYUnit);
				}

				// Delay for visual update.
				try {
					Thread.sleep(((int) GuiController.PER_TIMEFRAME * 1000)
							/ ((int) GuiController.SAMPLES)); // FIXME: 1 sec/samples
					// 1000 milliseconds = 1 sec
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (this.quit) {
				// GuiController.getInstance().resetValues();
			}
			this.quit = true;
		}
	}

	/**
	 * Tells the thread (if it exists) handling the feeding of the recorded results from a file to
	 * stop running.
	 */
	public static void shutdownRecordedResultsThread() {
		if (dataPlaybackContainer != null) {
			dataPlaybackContainer.shutdown();
			System.out.println("SHUTTING DOWN THREAD");
			dataPlaybackContainer = null;
		}
	}

	/**
	 * Tells the thread (if it exists) to pause/unpause the displaying data.
	 * 
	 * @param pause
	 *            the pause status flag. (true = pause, false = unpause).
	 */
	public static void pauseRecordedResultsThread(boolean pause) {
		if (dataPlaybackContainer != null) {
			dataPlaybackContainer.setPause(pause);
			System.out.println("Pause replaying results: " + pause);
		}
	}
}
