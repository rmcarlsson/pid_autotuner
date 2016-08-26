package se.trantor.pid_autotuner;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PidATune implements Runnable {

	public double output;

	private boolean isMax, isMin;
	private double setpoint;
	private double noiseBand;
	private int controlType;
	private boolean running;
	private long peak1, peak2;
	private int sampleTime;
	private int nLookBack;
	private int peakType;
	private ArrayList<Double> lastInputs;
	private Map<Integer, Double> peaks;
	private int peakCount;
	private boolean justchanged;
	private double absMax, absMin;
	private double oStep;
	private double outputStart;
	private double Ku, Pu;
	private TemperatureProvider in_t;
	HeaterController heater;
	ExecTerminator terminator;

	private boolean kicking;

	private static final Logger logger = Logger.getLogger(App.class.getName());

	public PidATune(int aSampleTime, TemperatureProvider aTemperatureProvider, HeaterController aHeaterController,
			double aStartPower, double aStep) {

		controlType = 0; // default to PI
		noiseBand = 0.5;
		running = false;
		oStep = aStep;
		SetLookbackSec(aSampleTime);
		in_t = aTemperatureProvider;
		heater = aHeaterController;
		outputStart = aStartPower;
		lastInputs = new ArrayList<Double>();
		peaks = new HashMap<Integer, Double>();
		peakCount = 0;
		kicking = true;
	}

	public void Cancel() {
		running = false;
	}

	public void run() {

		while (kicking) {
			
			try {
				Thread.sleep(TimeUnit.MILLISECONDS.toMillis(sampleTime));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (peakCount > 9 && running) {
				running = false;
				FinishUp();
				return;
			}

			double refVal = in_t.GetTemperature();
			if (!running) { // initialize working variables the first time
							// around
				peakType = 0;
				peakCount = 0;
				justchanged = false;
				absMax = refVal;
				absMin = refVal;
				setpoint = refVal;
				running = true;

				setPower(outputStart + oStep);

			} else {
				if (refVal > absMax)
					absMax = refVal;
				if (refVal < absMin)
					absMin = refVal;
			}

			// oscillate the output base on the input's relation to the setpoint

			if (refVal > setpoint + noiseBand)
				setPower(outputStart - oStep);
			else if (refVal < setpoint - noiseBand)
				setPower(outputStart + oStep);

			isMax = false;
			isMin = false;
			if (lastInputs.size() > 0) {
				isMax = (refVal > Collections.max(lastInputs));
				isMin = (refVal < Collections.min(lastInputs));
			} else {
				isMax = true;
				isMin = true;
			}

			lastInputs.add(0, refVal);

			if (lastInputs.size() > nLookBack)
				lastInputs.subList(nLookBack, (lastInputs.size())).clear();

			if (nLookBack < 9) { // we don't want to trust the maxes or mins
									// until
									// the inputs array has been filled
				continue;
			}

			if (isMax) {
				logger.log(Level.INFO, "Found new max, {0}.", refVal);
				if (peakType == 0)
					peakType = 1;
				if (peakType == -1) {
					peakType = 1;
					justchanged = true;
					peak2 = peak1;
				}
				Date d = new Date();
				peak1 = d.getTime();
				peaks.put(peakCount, refVal);

			} else if (isMin) {
				logger.log(Level.INFO, "Found new min, {0}.", refVal);
				if (peakType == 0)
					peakType = -1;
				if (peakType == 1) {
					peakType = -1;
					peakCount++;
					justchanged = true;
				}

				if (peakCount < 10)
					peaks.put(peakCount, refVal);
			}

			if (justchanged && peakCount > 2) { // we've transitioned. check if
												// we
												// can autotune based on the
												// last
												// peaks
				double avgSeparation = (Math.abs(peaks.get(peakCount - 1) - peaks.get(peakCount - 2))
						+ Math.abs(peaks.get(peakCount - 2) - peaks.get(peakCount - 3))) / 2;
				logger.log(Level.INFO, "Found new avgSeparation, {0}.", avgSeparation);
				if (avgSeparation < 0.05 * (absMax - absMin)) {
					FinishUp();
					running = false;
					return;
				}
			}
			justchanged = false;
		}
	}

	private void setPower(double d) {
		output = d;
		heater.SetPower((int) (output));
	}

	void FinishUp() {
		setPower(0);
		// we can generate tuning parameters!
		Ku = 4 * (2 * oStep) / ((absMax - absMin) * 3.14159);
		Pu = (double) (peak1 - peak2) / 1000;

		logger.log(Level.INFO, MessageFormat.format("Tuning done. Ku {0}, Pu {1}. Kp {2}, Ki {3}, Kd {4}", Ku, Pu,
				GetKp(), GetKi(), GetKd()));

		PrintWriter writer;
		try {
			UUID idOne = UUID.randomUUID();
			String fileName = idOne.toString() + "_params.log";
			writer = new PrintWriter(fileName, "UTF-8");
			writer.format("Tuning done. Ku %f, Pu %f. Kp %f, Ki %f, Kd %f", Ku, Pu, GetKp(), GetKi(), GetKd());
			writer.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.log(Level.INFO, "All done");
		kicking = false;

	}

	public double GetKp() {
		return controlType == 1 ? 0.6 * Ku : 0.4 * Ku;
	}

	public double GetKi() {
		return controlType == 1 ? 1.2 * Ku / Pu : 0.48 * Ku / Pu; // Ki = Kc/Ti
	}

	public double GetKd() {
		return controlType == 1 ? 0.075 * Ku * Pu : 0; // Kd = Kc * Td
	}

	public void SetOutputStep(double Step) {
		oStep = Step;
	}

	double GetOutputStep() {
		return oStep;
	}

	void SetControlType(int Type) // 0=PI, 1=PID
	{
		controlType = Type;
	}

	int GetControlType() {
		return controlType;
	}

	public void SetNoiseBand(double Band) {
		noiseBand = Band;
	}

	public double GetNoiseBand() {
		return noiseBand;
	}

	void SetLookbackSec(int value) {
		if (value < 1)
			value = 1;

		if (value < 25) {
			nLookBack = value * 4;
			sampleTime = 250;
		} else {
			nLookBack = 100;
			sampleTime = value * 10;
		}
	}

	int GetLookbackSec() {
		return nLookBack * sampleTime / 1000;
	}

}
